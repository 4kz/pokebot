package org.pokebot;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import org.pokebot.api.Client;
import org.pokebot.api.input.Keyboard;
import org.pokebot.api.map.AutoWalker;
import org.pokebot.input.AutoWalkerImpl;
import org.pokebot.input.LwjglKeyboard;
import org.pokebot.pathfinding.AStar;
import org.pokebot.api.map.Pathfinder;

public final class PokebotModule extends AbstractModule
{
	private final Client client;

	public PokebotModule(final Client client)
	{
		this.client = client;
	}

	@Override
	protected void configure()
	{
		bind(EventBus.class).toInstance(new EventBus("main-bus"));
		bind(Client.class).toInstance(client);
		bind(Pathfinder.class).to(AStar.class);
		bind(Keyboard.class).to(LwjglKeyboard.class);
		bind(AutoWalker.class).to(AutoWalkerImpl.class);
	}
}
