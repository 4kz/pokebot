package org.pokebot.api.map;

import java.util.List;

public interface Pathfinder
{
	/**
	 * Finds the optimal path to reach the goal.
	 *
	 * @param start The starting tile.
	 * @param goal  The tile that has to be reached.
	 * @return A list of all tiles that have to be
	 * traversed to reach the goal. The first item
	 * in the list will be the first tile that has
	 * to be traversed.
	 */
	List<Tile> solve(final Tile start, final Tile goal);
}
