package org.pokebot.api.map;

public interface AutoWalker
{
	enum State
	{
		WALKING,
		REACHED_GOAL,
		NO_PATH
	}

	State walkTo(Tile tile);
}
