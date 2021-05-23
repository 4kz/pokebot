package org.pokebot.patcher.deob;

import kotlinx.metadata.jvm.KotlinClassMetadata;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.pokebot.patcher.asm.ClassPool;
import org.pokebot.patcher.asm.PoolModifier;
import org.pokebot.patcher.asm.wrappers.ClassFile;
import org.pokebot.patcher.util.JarResult;
import org.pokebot.patcher.util.Util;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public final class SourceBasedClassRefactor implements PoolModifier
{
	private final Map<String, String> refactorMap = new HashMap<>();

	private ClassPool classPool;

	@Override
	public ClassPool run(final ClassPool classPool)
	{
		val startTime = System.currentTimeMillis();

		this.classPool = classPool;

		buildRefactorMap();
		refactor();

		val elapsedTime = System.currentTimeMillis() - startTime;
		log.info("Refactored " + refactorMap.size() + " class names " + elapsedTime + " ms.");
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
					val newName = refactorMap.get(internalName);
					if (newName != null)
					{
						internalName = newName;
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
		handleOuterClasses();
		handleInnerClasses();
	}

	private void handleInnerClasses()
	{
		for (val cf : classPool)
		{
			if (!cf.inObfuscatedPackage()
					|| !cf.name.contains("$")
					|| !outerIsObfuscated(cf.name))
			{
				continue;
			}

			val outer = cf.name.substring(0, cf.name.indexOf('$'));
			val newName = refactorMap.get(outer);

			if (newName == null)
			{
				continue;
			}

			val inner = cf.name.substring(cf.name.indexOf('$'));
			refactorMap.put(cf.name, newName + inner);
		}
	}

	private String getKotlinName(final ClassFile cf)
	{
		val kmClass = ((KotlinClassMetadata.Class) cf.getKotlinMetadata()).toKmClass();
		var name = kmClass.getName();

		if (name.startsWith(".ktx"))
		{
			return null;
		}

		if (name.startsWith(".com"))
		{
			name = name.substring(1);
		}

		if (!Util.getPackage(cf.name).equals(Util.getPackage(name)))
		{
			log.error("A package mismatch occurred whilst refactoring a kotlin class.");
			return null;
		}

		name = name.replace('.', '$');

		return name;
	}

	private void handleOuterClasses()
	{
		val encounterMap = new HashMap<String, Integer>();
		var noSourceCounter = 0;

		for (val cf : classPool)
		{
			if (!cf.inObfuscatedPackage()
					|| cf.name.contains("$")
					|| !outerIsObfuscated(cf.name))
			{
				continue;
			}

			if (cf.getKotlinMetadata() != null && cf.getKotlinMetadata() instanceof KotlinClassMetadata.Class)
			{
				val kotlinName = getKotlinName(cf);
				if (kotlinName != null)
				{
					refactorMap.put(cf.name, kotlinName + "Kt");
					continue;
				}
			}

			val packageName = Util.getPackage(cf.name);

			if (cf.sourceFile == null)
			{
				val newName = packageName + "Class" + noSourceCounter;
				refactorMap.put(cf.name, newName);
				noSourceCounter++;
				continue;
			}

			var lastIdx = cf.sourceFile.lastIndexOf('.');
			if (lastIdx == -1)
			{
				lastIdx = cf.sourceFile.length();
			}

			val sourceName = cf.sourceFile.substring(0, lastIdx);
			val newName = packageName + sourceName;

			val suffix = encounterMap.merge(newName, 1, Integer::sum) - 1;

			if (suffix == 0)
			{
				refactorMap.put(cf.name, newName);
			}
			else
			{
				refactorMap.put(cf.name, newName + suffix);
			}
		}
	}

	private boolean outerIsObfuscated(final String className)
	{
		return getOuterClass(className).length() < 3;
	}

	private String getOuterClass(final String className)
	{
		var lastIdx = className.indexOf('$');

		if (lastIdx == -1)
		{
			lastIdx = className.length();
		}

		return className.substring(className.lastIndexOf('/') + 1, lastIdx);
	}
}
