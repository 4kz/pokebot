package org.pokebot.api.map;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.pokebot.api.entityproperty.Position;

/**
 * Represents a position in tile form. A tile
 * in turn is the building block of a map. Collision
 * is also calculated around tiles.
 */
@Data
@AllArgsConstructor
public final class Tile
{
	/**
	 * X-coordinate of the tile.
	 */
	int x;

	/**
	 * Y-coordinate of the tile.
	 */
	int y;

	/**
	 * Returns the tile that best matches the given position.
	 *
	 * @param position Position to return the tile for.
	 * @return A tile object with appropriate x and y coordinates.
	 */
	public static Tile fromPosition(final Position position)
	{
		return new Tile(Math.round(position.getX() / 32f), Math.round(position.getY() / 32f));
	}
}
