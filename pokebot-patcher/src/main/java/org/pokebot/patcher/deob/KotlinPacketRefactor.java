package org.pokebot.patcher.deob;

import kotlinx.metadata.jvm.KotlinClassMetadata;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.pokebot.patcher.asm.wrappers.ClassFile;
import org.pokebot.patcher.asm.ClassPool;
import org.pokebot.patcher.asm.PoolModifier;
import org.pokebot.patcher.util.JarResult;
import org.pokebot.patcher.util.Util;

import java.util.HashMap;
import java.util.Map;

/**
 * Refactors the packets name to their Kotlin counterparts based
 * on the metadata.
 */
@Slf4j
public final class KotlinPacketRefactor implements PoolModifier
{
	private ClassPool classPool;

	private final Map<String, String> refactorMap = new HashMap<>();

	@Override
	public ClassPool run(final ClassPool classPool)
	{
		val startTime = System.currentTimeMillis();

		this.classPool = classPool;

		buildRefactorMap();
		refactor();

		val elapsedTime = System.currentTimeMillis() - startTime;
		log.info("Refactored " + refactorMap.size() + " packets in " + elapsedTime + " ms.");
		return this.classPool;
	}

	private void refactor()
	{
		val remappedCp = new ClassPool();
		JarResult jarResult = null;

		for (val cf : classPool)
		{
			if (jarResult == null)
			{
				jarResult = new JarResult(remappedCp, cf.getJarResult().getDepPool());
			}
			val copy = new ClassFile(jarResult);
			val remapper = new ClassRemapper(copy, new Remapper()
			{
				@Override
				public String map(String internalName)
				{
					val thisPackage = Util.getPackage(internalName);
					val match = refactorMap.get(thisPackage);
					if (match != null)
					{
						internalName = internalName.replace(thisPackage, match);
					}
					return super.map(internalName);
				}
			});
			cf.accept(remapper);
			remappedCp.add(copy);
		}

		classPool = remappedCp;
	}

	private void buildRefactorMap()
	{
		for (val cf : classPool)
		{
			if (!cf.inObfuscatedPackage() || cf.getKotlinMetadata() == null)
			{
				continue;
			}

			val metadata = cf.getKotlinMetadata();

			if (!(metadata instanceof KotlinClassMetadata.Class))
			{
				continue;
			}

			val kmClass = ((KotlinClassMetadata.Class) metadata).toKmClass();
			var name = kmClass.getName();

			if (name.startsWith(".ktx"))
			{
				continue;
			}

			if (name.startsWith(".com"))
			{
				name = name.substring(1);
			}

			val obfuscatedPackage = Util.getPackage(cf.name);
			val kotlinPackage = Util.getPackage(name);

			if (obfuscatedPackage.equals(kotlinPackage))
			{
				continue;
			}

			val obfuscatedDepth = obfuscatedPackage.split("/").length;
			val kotlinDepth = kotlinPackage.split("/").length;

			if (obfuscatedDepth != kotlinDepth)
			{
				continue;
			}

			val v = refactorMap.putIfAbsent(obfuscatedPackage, kotlinPackage);
			if (v != null && !v.equals(kotlinPackage))
			{
				log.error("Unable to determine the right package as multiple different packages are assigned.");
			}
		}
	}
}
