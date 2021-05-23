package org.pokebot.pathfinding;

import org.pokebot.api.map.Tile;

public final class ManhattanDistance implements Heuristic
{
	@Override
	public int distance(final Tile a, final Tile b)
	{
		return Math.abs(b.getX() - a.getX()) + Math.abs(b.getY() - a.getY());
	}
}
