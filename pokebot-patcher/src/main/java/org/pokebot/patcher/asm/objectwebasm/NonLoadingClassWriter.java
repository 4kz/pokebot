package org.pokebot.patcher.asm.objectwebasm;

import lombok.val;
import org.objectweb.asm.ClassWriter;
import org.pokebot.patcher.asm.wrappers.ClassFile;
import org.pokebot.patcher.util.JarResult;

/**
 * A variant of the {@link ClassWriter} that does not
 * require all classes to be loaded into the JVM. Instead
 * it will get the super and interface information based
 * on the {@link JarResult}.
 */
public final class NonLoadingClassWriter extends ClassWriter
{
	private final JarResult jarResult;

	public NonLoadingClassWriter(final int flags, final JarResult jarResult)
	{
		super(flags);

		this.jarResult = jarResult;
	}

	@Override
	protected String getCommonSuperClass(final String type1, final String type2)
	{
		if (type1.equals("java/lang/Object")
				|| type2.equals("java/lang/Object")
				|| type1.startsWith("org/pokebot")
				|| type2.startsWith("org/pokebot"))
		{
			return "java/lang/Object";
		}

		val cf1 = jarResult.findClass(type1);
		val cf2 = jarResult.findClass(type2);

		if (cf1 == null && cf2 == null)
		{
			try
			{
				return super.getCommonSuperClass(type1, type2);
			}
			catch (RuntimeException e)
			{
				return "java/lang/Object";
			}
		}

		if (cf1 != null && cf2 != null)
		{
			if (!(cf1.isInterface() || cf2.isInterface()))
				for (ClassFile c = cf1; c != null; c = c.getSuper())
					for (ClassFile c2 = cf2; c2 != null; c2 = c2.getSuper())
						if (c == c2)
							return c.name;

			return "java/lang/Object";
		}

		ClassFile found;
		String other;

		if (cf1 == null)
		{
			found = cf2;
			other = type1;
		}
		else
		{
			found = cf1;
			other = type2;
		}

		ClassFile prev = null;

		for (ClassFile c = found; c != null; c = c.getSuper())
			if ((prev = c).superName.equals(other))
				return other;

		return super.getCommonSuperClass(prev.superName, other);
	}
}
