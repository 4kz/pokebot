package org.pokebot.api;

import org.pokebot.api.battle.ClientNetworkMessageHandler;
import org.pokebot.api.entity.LocalPlayer;
import org.pokebot.api.screen.Screen;

public interface Client
{
	Screen getCurrentScreen();

	LocalPlayer getLocalPlayer();

	MapManager getMapManager();

	ClientNetworkMessageHandler getNetworkHandler();
}
