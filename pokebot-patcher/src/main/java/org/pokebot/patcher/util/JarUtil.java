package org.pokebot.patcher.util;

import lombok.val;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.pokebot.patcher.asm.wrappers.ClassFile;
import org.pokebot.patcher.asm.ClassPool;
import org.pokebot.patcher.asm.objectwebasm.NonLoadingClassWriter;

import java.io.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

/**
 * This class contains utility functions to load and save jar files.
 */
public final class JarUtil
{
	/**
	 * Loads a jar into a {@link JarResult}.
	 *
	 * @param file          The file that has to be loaded into a result.
	 * @param packagePrefix Package prefix that tells how the
	 *                      dependency and package pool should be separated.
	 * @return A {@link JarResult} will be returned.
	 * @throws IOException Error that occurs if the file cannot be read.
	 */
	public static JarResult loadJar(final File file, final String packagePrefix) throws IOException
	{
		JarResult jarResult = new JarResult(new ClassPool(), new ClassPool());
		try (val jis = new JarInputStream(new BufferedInputStream(new FileInputStream(file))))
		{
			JarEntry entry;
			while ((entry = jis.getNextJarEntry()) != null)
			{
				if (!entry.getName().endsWith(".class"))
				{
					continue;
				}

				val cf = new ClassFile(jarResult);
				val cr = new ClassReader(jis);

				if (entry.getName().startsWith(packagePrefix))
				{
					cr.accept(cf, ClassReader.SKIP_FRAMES);
					jarResult.getPackagePool().add(cf);
				}
				else
				{
					cr.accept(cf, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
					jarResult.getDepPool().add(cf);
				}

				jis.closeEntry();
			}
		}
		return jarResult;
	}

	/**
	 * Saves a {@link JarResult} into a file. Only the
	 * {@link JarResult#getPackagePool()} will be saved.
	 * The {@link JarResult#getDepPool()} is required
	 * for the {@link NonLoadingClassWriter}.
	 *
	 * @param file File to save this {@link JarResult} to.
	 * @param jarResult The {@link JarResult} to save.
	 * @throws IOException Exception when the file cannot be saved.
	 */
	public static void saveJar(final File file, final JarResult jarResult) throws IOException
	{
		try (val jos = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(file))))
		{
			for (val cf : jarResult.getPackagePool())
			{
				jos.putNextEntry(new JarEntry(cf.getJarName()));

				val cw = new NonLoadingClassWriter(ClassWriter.COMPUTE_FRAMES, jarResult);
				cf.accept(cw);
				jos.write(cw.toByteArray());

				jos.closeEntry();
			}
		}
	}
}
