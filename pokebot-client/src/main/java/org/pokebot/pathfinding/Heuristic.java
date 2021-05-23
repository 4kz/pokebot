package org.pokebot.pathfinding;

import org.pokebot.api.map.Tile;

public interface Heuristic
{
	int distance(final Tile a, final Tile b);
}
