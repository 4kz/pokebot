package org.pokebot.api.screen;

import org.pokebot.api.MapManager;
import org.pokebot.api.battle.ClientNetworkMessageHandler;
import org.pokebot.api.entity.LocalPlayer;

/**
 * The screen that shows up after the player logged in.
 * It shows the map and the game in itself.
 */
public interface MainGameScreen extends Screen
{
	/**
	 * For a semantic API this is forwarded to the {@link org.pokebot.api.Client}.
	 *
	 * @return A {@link LocalPlayer} instance.
	 */
	LocalPlayer getLocalPlayer();

	/**
	 * For a semantic API this is forwarded to the {@link org.pokebot.api.Client}.
	 *
	 * @return A {@link MapManager} instance.
	 */
	MapManager getMapManager();

	/**
	 * For a semantic API this is forwarded to the {@link org.pokebot.api.Client}.
	 *
	 * @return A {@link ClientNetworkMessageHandler} instance.
	 */
	ClientNetworkMessageHandler getNetworkHandler();
}
