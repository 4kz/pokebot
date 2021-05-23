package org.pokebot.pathfinding;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.val;
import org.pokebot.api.Client;
import org.pokebot.api.map.Pathfinder;
import org.pokebot.api.map.Tile;
import org.pokebot.api.util.MapUtil;

import java.util.*;

/**
 * A {@link Pathfinder} implementation that solves the
 * optimal path using the A* algorithm. Since the game
 * uses 4-way movement it will use the Manhattan distance
 * heuristic.
 */
@Singleton
public final class AStar implements Pathfinder
{
	/**
	 * The client is required to get map information (collision, dimensions).
	 */
	@Inject
	private Client client;

	/**
	 * Heuristic used to solve the problem with.
	 */
	private static final Heuristic manhattanDistance = new ManhattanDistance();

	/**
	 * Map that links a {@link Tile} to its given fScore (gScore + hScore).
	 * The hScore is the distance to the end calculated by the heuristic.
	 */
	private final HashMap<Tile, Integer> fScore = new HashMap<>();

	/**
	 * All tiles that are enlisted, sorted by their fScore.
	 */
	private final PriorityQueue<Tile> openSet = new PriorityQueue<>(new TileComparator(fScore));

	/**
	 * Map that keeps track of the optimal path.
	 */
	private final HashMap<Tile, Tile> cameFrom = new HashMap<>();

	/**
	 * Map that links a {@link Tile} to its given gScore (cost from starting point to the tile).
	 */
	private final HashMap<Tile, Integer> gScore = new HashMap<>();

	public List<Tile> emptyList = List.of();

	/**
	 * Reconstructs the path that was used to reach
	 * the goal.
	 *
	 * @param cameFrom The tile map that kept track of
	 *                 the path.
	 * @param current  The goal and tile that was traversed last.
	 * @return The optimal path.
	 */
	private List<Tile> reconstructPath(Map<Tile, Tile> cameFrom, Tile current)
	{
		val totalPath = new ArrayList<Tile>();
		totalPath.add(current);
		while (cameFrom.containsKey(current))
		{
			current = cameFrom.get(current);
			totalPath.add(0, current);
		}
		return totalPath;
	}

	/**
	 * Obtains all neighbors based on the current map dimensions
	 * and 4-way movement.
	 *
	 * @param tile The tile to find the neighbors for.
	 * @return A list of tiles that are adjacent to the
	 * given tile.
	 */
	private List<Tile> neighbors(final Tile tile)
	{
		val list = new ArrayList<Tile>();

		val dimension = MapUtil.getMapDimension(client);

		if (tile.getX() - 1 > -1)
		{
			list.add(new Tile(tile.getX() - 1, tile.getY()));
		}
		if (tile.getY() - 1 > -1)
		{
			list.add(new Tile(tile.getX(), tile.getY() - 1));
		}
		if (tile.getX() + 1 < dimension.getWidth())
		{
			list.add(new Tile(tile.getX() + 1, tile.getY()));
		}
		if (tile.getY() + 1 < dimension.getHeight())
		{
			list.add(new Tile(tile.getX(), tile.getY() + 1));
		}

		return list;
	}

	public List<Tile> solve(final Tile start, final Tile goal)
	{
		return solve(start, goal, manhattanDistance);
	}

	private List<Tile> solve(final Tile start, final Tile goal, final Heuristic h)
	{
		if (start.equals(goal))
		{
			return emptyList;
		}

		fScore.clear();
		fScore.put(start, h.distance(start, goal));

		openSet.clear();
		openSet.add(start);

		cameFrom.clear();

		gScore.clear();
		gScore.put(start, 0);

		while (!openSet.isEmpty())
		{
			val current = openSet.poll();
			if (current.equals(goal))
			{
				return reconstructPath(cameFrom, current);
			}

			openSet.remove(current);
			for (val neighbor : neighbors(current))
			{
				if (MapUtil.tileIsBlocked(client, neighbor.getX(), neighbor.getY()))
				{
					continue;
				}

				val tentativeGScore = gScore.get(current) + 1;

				if (tentativeGScore < gScore.getOrDefault(neighbor, Integer.MAX_VALUE))
				{
					if (!current.equals(start))
					{
						cameFrom.put(neighbor, current);
					}
					gScore.put(neighbor, tentativeGScore);
					fScore.put(neighbor, gScore.get(neighbor) + h.distance(neighbor, goal));
					if (!openSet.contains(neighbor))
					{
						openSet.add(neighbor);
					}
				}
			}
		}
		return null;
	}
}
