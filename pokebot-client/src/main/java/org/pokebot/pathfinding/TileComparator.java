package org.pokebot.pathfinding;

import lombok.val;
import org.pokebot.api.map.Tile;

import java.util.Comparator;
import java.util.HashMap;

public final class TileComparator implements Comparator<Tile>
{
	private final HashMap<Tile, Integer> map;

	public TileComparator(final HashMap<Tile, Integer> map)
	{
		this.map = map;
	}

	@Override
	public int compare(final Tile o1, final Tile o2)
	{
		val o1f = map.get(o1);
		val o2f = map.get(o2);
		return o1f.compareTo(o2f);
	}
}
