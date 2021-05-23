package org.pokebot.input;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.val;
import org.pokebot.api.Client;
import org.pokebot.api.input.KeyCode;
import org.pokebot.api.input.Keyboard;
import org.pokebot.api.map.AutoWalker;
import org.pokebot.api.map.Direction;
import org.pokebot.api.map.Pathfinder;
import org.pokebot.api.map.Tile;

@Singleton
public final class AutoWalkerImpl implements AutoWalker
{
	@Inject
	private Client client;

	@Inject
	private Keyboard keyboard;

	@Inject
	private Pathfinder pathfinder;

	@Override
	public State walkTo(final Tile tile)
	{
		val start = Tile.fromPosition(client.getLocalPlayer().getPosition());

		val path = pathfinder.solve(start, tile);

		if (path == null)
		{
			return State.NO_PATH;
		}

		if (path.isEmpty())
		{
			return State.REACHED_GOAL;
		}

		val direction = Direction.getDirection(start, path.get(0));

		if (direction != null)
		{
			switch (direction)
			{
				case UP:
					keyboard.setKeyDown(KeyCode.KEY_W);
					break;
				case DOWN:
					keyboard.setKeyDown(KeyCode.KEY_S);
					break;
				case LEFT:
					keyboard.setKeyDown(KeyCode.KEY_A);
					break;
				case RIGHT:
					keyboard.setKeyDown(KeyCode.KEY_D);
					break;
			}
		}
		return State.WALKING;
	}
}
