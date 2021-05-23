package org.pokebot.api.map;

/**
 * The direction that an entity can move in.
 */
public enum Direction
{
	UP,
	DOWN,
	LEFT,
	RIGHT;

	/**
	 * Gets the direction that has to be traversed
	 * in order to reach the goal.
	 *
	 * @param start The starting title.
	 * @param goal  The destination tile.
	 * @return Direction to reach the goal.
	 */
	public static Direction getDirection(final Tile start, final Tile goal)
	{
		if (start.getX() < goal.getX())
		{
			return RIGHT;
		}
		else if (start.getX() > goal.getX())
		{
			return LEFT;
		}
		else if (start.getY() < goal.getY())
		{
			return UP;
		}
		else if (start.getY() > goal.getY())
		{
			return DOWN;
		}
		else
		{
			return null;
		}
	}
}
