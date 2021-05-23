package org.pokebot.patcher.asm;

import lombok.val;
import org.objectweb.asm.Type;
import org.pokebot.patcher.asm.wrappers.ClassFile;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * Represents a pool of {@link ClassFile} that can
 * be accessed through a map.
 */
public final class ClassPool implements Iterable<ClassFile>
{
	/**
	 * Internal map that stores the {@link ClassFile}.
	 */
	private final HashMap<String, ClassFile> map = new HashMap<>();

	/**
	 * Adds a {@link ClassFile} to the pool.
	 *
	 * @param classFile The {@link ClassFile} to add the pool.
	 */
	public void add(final ClassFile classFile)
	{
		map.put(classFile.name, classFile);
	}

	/**
	 * Searches for the {@link ClassFile} that matches
	 * the name. If the name is a descriptor it will
	 * convert the name accordingly. For method
	 * descriptors it will mean that the return type is
	 * used to identify the class.
	 *
	 * @param className Name to return a {@link ClassFile} for.
	 * @return The {@link ClassFile} that matches the name.
	 */
	public ClassFile findClass(String className)
	{
		if (className.startsWith("("))
		{
			className = Type.getReturnType(className).getDescriptor();
		}

		if (className.startsWith("L") && className.endsWith(";"))
		{
			className = className.substring(1, className.length() - 1);
		}

		return map.get(className);
	}

	public ClassFile findClassByInterface(final Class<?> inter)
	{
		for (val cf : this)
		{
			if (cf.implementsInterface(inter))
			{
				return cf;
			}
		}
		return null;
	}

	@Override
	public Iterator<ClassFile> iterator()
	{
		return map.values().iterator();
	}

	@Override
	public void forEach(final Consumer<? super ClassFile> action)
	{
		map.values().forEach(action);
	}

	@Override
	public Spliterator<ClassFile> spliterator()
	{
		return map.values().spliterator();
	}
}
