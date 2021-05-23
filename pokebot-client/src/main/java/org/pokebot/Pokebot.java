package org.pokebot;

import com.google.common.eventbus.EventBus;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import lombok.Getter;
import org.pokebot.api.Client;
import org.pokebot.api.input.Keyboard;
import org.pokebot.api.map.AutoWalker;
import org.pokebot.api.screen.MainGameScreen;

@Singleton
public final class Pokebot
{
	@Getter
	private static Injector injector;

	@Inject
	private Client client;

	@Inject
	private EventBus eventBus;

	@Inject
	private Keyboard keyboard;

	@Inject
	private AutoWalker autoWalker;

	private ScriptManager scriptManager;

	public static void init(final Client client)
	{
		injector = Guice.createInjector(new PokebotModule(client));

		injector.getInstance(Pokebot.class).start();
	}

	public void start()
	{
		System.out.println("Started Pokebot...");

		eventBus.register(this);
	}

	/**
	 * Will be called before any rendering is done.
	 *
	 * @param delta Time difference in seconds.
	 */
	public void beforeRender(final float delta)
	{
		if (!(client.getCurrentScreen() instanceof MainGameScreen))
		{
			return;
		}

		keyboard.handleKeyChanges();

		if (scriptManager == null)
		{
			scriptManager = injector.getInstance(ScriptManager.class);
			eventBus.register(scriptManager);
			injector.injectMembers(scriptManager);
		}

		scriptManager.beforeRender(delta);
	}
}
