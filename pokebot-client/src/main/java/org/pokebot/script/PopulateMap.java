package org.pokebot.script;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import lombok.SneakyThrows;
import org.pokebot.api.Client;
import org.pokebot.api.entityproperty.Position;
import org.pokebot.api.map.AutoWalker;
import org.pokebot.api.map.Tile;
import org.pokebot.api.script.AbstractScript;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Objects;

public class PopulateMap extends AbstractScript
{

	/**
	 Map containing all the spots for each map to capture pokemon
	 */


	private Reader Reader;
	private Tile goal;
	private Writer Writer;
	private final Gson Gson = new Gson();
	private HashMap<Integer, Position> MapOfLocations;

	/**
	 * Client to access the API.
	 */
	private final Client client;

	/**
	 * AutoWalker required to walk to places.
	 */

	/**
	 * Tells that the script is requested to be stopped.
	 */
	public boolean isBeingTerminated = false;

	/**
	 * Will allow the user to run logic initialization before
	 * any other logic is ran.
	 */


	@SneakyThrows
	public void onStart()
	{
		MapOfLocations = new HashMap<Integer, Position>();

		Type type = new TypeToken<HashMap<Integer, Position>>()
		{
		}
				.getType();

		/**
		 * getting map Id
		 */
		int playerMapId = client.getMapManager().getMapId();
		System.out.println("PlayermapID:" + playerMapId);

		/**
		 * getting map of player locations from json
		 */
		Reader reader = new FileReader("maps.json");

		this.MapOfLocations = this.Gson.fromJson(reader, type);
		System.out.println("Location:" +  MapOfLocations);

		/**
		 * put at map id key the player current position
		 */
		this.MapOfLocations.put(playerMapId, client.getLocalPlayer().getPosition());
		System.out.println("MapOfLocations:" + MapOfLocations);

		/**
		 * Write to json the new map of player locations
		 */

		String json = this.Gson.toJson(MapOfLocations);
		System.out.println("Json" + json);

		Writer writer = new FileWriter("maps.json");
		writer.append(json);
		writer.close();

	}

	@Inject
	public PopulateMap(final Client client, final AutoWalker autoWalker)
	{
		this.client = client;

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
	}

	/**
	 * States that this script can be in.
	 */

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
