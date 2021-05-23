package org.pokebot.patcher.util;

import lombok.val;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MutableList
{
	public static <E> List<E> of(int capacity)
	{
		return new ArrayList<>(capacity);
	}

	public static <E> List<E> of()
	{
		return new ArrayList<>();
	}

	@SafeVarargs
	public static <E> List<E> of(E... elements)
	{
		val list = new ArrayList<E>(elements.length);
		Collections.addAll(list, elements);
		return list;
	}
}
