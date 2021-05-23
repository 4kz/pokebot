package org.pokebot.script;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import lombok.SneakyThrows;
import lombok.val;
import org.pokebot.api.Client;
import org.pokebot.api.entityproperty.Position;
import org.pokebot.api.map.AutoWalker;
import org.pokebot.api.map.Tile;
import org.pokebot.api.script.AbstractScript;
import org.pokebot.api.util.MapUtil;

import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Objects;

public class AutoWalkToSpot extends AbstractScript
{

	/**
	Map containing all the spots for each map to capture pokemon
	*/


	private Reader Reader;

	private Tile goal;
	private final Gson Gson = new Gson();
	private HashMap<Integer, Position> MapOfLocations;

	/**
	* Client to access the API.
	*/
	private final Client client;

	/**
	* AutoWalker required to walk to places.
	*/
	private final AutoWalker autoWalker;

	/**
	* Tells that the script is requested to be stopped.
	*/
	public boolean isBeingTerminated = false;

	/**
	 * Will allow the user to run logic initialization before
	 * any other logic is ran.
	 */

	/**
	 * Current state of the script.
	 */
	private State state = State.WALKING_TO_TILE;

	@SneakyThrows
	public void onStart()
	{

		/**
		 * reads maps.json, converts it and and stores it in the MapOfLocations
		 */
		Reader reader = new FileReader("maps.json");
		Gson gson = new Gson();
		Type type = new TypeToken<HashMap<Integer, Position>>()
		{
		}
		.getType();
		this.MapOfLocations = gson.fromJson(reader, type);


		/**
		 * Gets current map's Id
		 */
		int playerMapId = client.getMapManager().getMapId();

		/**
		 *  goal is the Tile stored in the map of all locations at
		 *  the key that is the current map Id
		 */
		val position = (Position) MapOfLocations.get(playerMapId);

		goal = Tile.fromPosition(position);
	}

	@Inject
	public AutoWalkToSpot(final Client client, final AutoWalker autoWalker)
	{
		this.client = client;
		this.autoWalker = autoWalker;

	}


	/**
	* This is basically the game tick. It will be called before
	* any input is processed and allows the user to handle
	* the script logic.
	*
	* @param delta The time difference since the last
	*			beforeRender function was called.
	*/
	@Override
	public void beforeRender(float delta)
	{
		switch (state)
		{
			case WALKING_TO_TILE:
				val pos = client.getLocalPlayer().getPosition();
				val tile = Tile.fromPosition(pos);
				val collisionLayer = client.getMapManager().getCollisionMain();

				if (client.getMapManager().isBlocked(collisionLayer, goal.getX(), goal.getY()))
				{
					System.out.println("TILE BLOCKED");
					state = State.TILE_IS_BLOCKED;
					beforeRender(delta);
					return;
				}
				if (tile.equals(goal))
				{
					state = State.RUN_IN_GRASS;
					beforeRender(delta);
					return;
				}

				autoWalker.walkTo(goal);
				break;
			case TILE_IS_BLOCKED:
				break;
			case RUN_IN_GRASS:
				val grassTile = MapUtil.closestTile(client, client.getMapManager().getGrass(), true);
				if (grassTile != null)
				{
					autoWalker.walkTo(grassTile);
				}
				break;
		}

		System.out.println("AutoFightScript state: " + state);
	}

	/**
	 * States that this script can be in.
	 */
	private enum State
	{
		WALKING_TO_TILE,
		TILE_IS_BLOCKED,
		RUN_IN_GRASS
	}

	/**
	* Will be called when the script is terminated and allows the
	* user to clean up the potential instances.
	*/
	public void onStop()
	{
	}

	/**
	* Allows the user to terminate the script from within the script itself.
	*/

	@Override
	public boolean equals(final Object o)
	{
		return o.getClass().equals(getClass());
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(getClass());
	}

}
