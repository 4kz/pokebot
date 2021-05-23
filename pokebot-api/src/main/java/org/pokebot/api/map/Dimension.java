package org.pokebot.api.map;

import lombok.Value;

/**
 * A combination of width and height to denote the
 * the size of an object.
 */
@Value
public class Dimension
{
	/**
	 * The width of the object.
	 */
	int width;

	/**
	 * The height of the object.
	 */
	int height;
}
