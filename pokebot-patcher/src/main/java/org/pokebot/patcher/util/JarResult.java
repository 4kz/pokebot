package org.pokebot.patcher.util;

import lombok.Value;
import lombok.val;
import org.pokebot.patcher.asm.wrappers.ClassFile;
import org.pokebot.patcher.asm.ClassPool;

import java.util.List;

/**
 * Result obtained from the JarUtil that contains
 * one pool that is the specified package and one pool
 * that is the dependency pool.
 */
@Value
public class JarResult
{
	/**
	 * Pool that contains the content of the specified package.
	 */
	ClassPool packagePool;

	/**
	 * Pool that contains the dependencies of the jar.
	 */
	ClassPool depPool;

	/**
	 * Finds a {@link ClassFile} in the {@link #packagePool} and
	 * {@link #depPool}.
	 *
	 * @param className Name to search for.
	 * @return A {@link ClassFile} that matches the given name or null.
	 */
	public ClassFile findClass(final String className)
	{
		val classPools = List.of(packagePool, depPool);
		for (val cp : classPools)
		{
			val match = cp.findClass(className);
			if (match != null)
			{
				return match;
			}
		}
		return null;
	}
}
