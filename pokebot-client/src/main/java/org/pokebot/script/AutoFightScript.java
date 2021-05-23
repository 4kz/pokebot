package org.pokebot.script;

import com.google.inject.Inject;
import lombok.val;
import org.pokebot.api.Client;
import org.pokebot.api.map.AutoWalker;
import org.pokebot.api.map.Tile;
import org.pokebot.api.script.AbstractScript;
import org.pokebot.api.util.MapUtil;

/**
 * Auto Fight Script that will train your Pokémon
 * in the forrest.
 */
public class AutoFightScript extends AbstractScript
{
	/**
	 * Client to access the API.
	 */
	private final Client client;

	/**
	 * AutoWalker required to walk to places.
	 */
	private final AutoWalker autoWalker;

	/**
	 * Current state of the script.
	 */
	private State state = State.WALKING_TO_HEALER;

	/**
	 * Tile that is adjacent to the Aroma Lady. This
	 * tile has to be walked to in order to interact
	 * with the healer.
	 */
	private static final Tile AROMA_LADY_ADJACENT_TILE = new Tile(86, 82);

	@Inject
	public AutoFightScript(final Client client, final AutoWalker autoWalker)
	{
		this.client = client;
		this.autoWalker = autoWalker;
	}

	@Override
	public void beforeRender(float delta)
	{
		switch (state)
		{
			case WALKING_TO_HEALER:
				val pos = client.getLocalPlayer().getPosition();
				val tile = Tile.fromPosition(pos);

				if (tile.equals(AROMA_LADY_ADJACENT_TILE))
				{
					state = State.RUN_IN_GRASS; // TODO: Change to OPEN_HEAL_DIALOG later.
					// Call itself so that it doesn't waste a tick to process the new state.
					beforeRender(delta);
					return;
				}

				autoWalker.walkTo(AROMA_LADY_ADJACENT_TILE);
				break;
			case OPEN_HEAL_DIALOG:
				// TODO: Add logic to the API to handle dialogs. Use that logic in here.
				break;
			case RUN_IN_GRASS:
				val grassTile = MapUtil.closestTile(client, client.getMapManager().getGrass(), true);
				autoWalker.walkTo(grassTile);
				break;
		}

		System.out.println("AutoFightScript state: " + state);
	}

	/**
	 * States that this script can be in.
	 */
	private enum State
	{
		WALKING_TO_HEALER,
		OPEN_HEAL_DIALOG,
		RUN_IN_GRASS
	}
}
