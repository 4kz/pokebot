package org.pokebot.patcher.util;

import lombok.val;

import java.util.Arrays;
import java.util.List;

public final class Util
{
	public static <T> List<T> add(final List<T> list, final T element)
	{
		List<T> newList = list == null ? MutableList.of(1) : list;
		newList.add(element);
		return newList;
	}

	public static String getPackage(final String className)
	{
		return className.substring(0, className.lastIndexOf('/') + 1);
	}

	public static String lowerFirst(final String s)
	{
		return s.substring(0, 1).toLowerCase() + s.substring(1);
	}

	public static String upperFirst(final String s)
	{
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	public static String toSlash(final String name)
	{
		return name.replace('.', '/');
	}

	/**
	 * Turns an {@link Object} into an array of the generic
	 * input type. If the cast cannot succeed it will return null
	 * instead.
	 *
	 * @param listObject The object that should be a list or an instance
	 *                   of list. Moreover it should contain the generic
	 *                   object as list value.
	 * @param out        The generic that the list will be cast to. E.g.
	 *                   a String cast will require: new String[0]
	 * @return An array with the list contents.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] listObjectToArray(Object listObject, T[] out)
	{
		if (!(listObject instanceof List))
		{
			return null;
		}

		val list = (List<?>) listObject;

		if (list.size() < 1)
		{
			return Arrays.copyOf(out, 0);
		}

		if (list.get(0).getClass() != out.getClass().getComponentType())
		{
			return null;
		}

		return (T[]) Arrays.copyOf(list.toArray(), list.size(), out.getClass());
	}

	/**
	 * Turns an {@link Object} into an array of the primitive
	 * integer type. If the cast cannot succeed it will return null
	 * instead. Since primitives cannot be used as generics, this
	 * method is added on top of the generic conversion method.
	 *
	 * @param listObject The object that should be a list or an instance
	 *                   of list. Moreover it should contain a {@link Integer}
	 *                   object as list value.
	 * @return A primitive int array with the list contents.
	 */
	public static int[] listObjectToIntArray(Object listObject)
	{
		if (!(listObject instanceof List))
		{
			return null;
		}

		val list = (List<?>) listObject;

		if (list.size() < 1)
		{
			return new int[0];
		}

		if (list.get(0).getClass() != Integer.class)
		{
			return null;
		}

		return list.stream().mapToInt(i -> (Integer) i).toArray();
	}
}
