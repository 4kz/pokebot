package org.pokebot;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import lombok.SneakyThrows;
import lombok.val;
import org.pokebot.api.event.KeyReleased;
import org.pokebot.api.input.KeyCode;
import org.pokebot.api.input.Keyboard;
import org.pokebot.api.script.AbstractScript;
import org.pokebot.script.AutoFightScript;
import org.pokebot.script.AutoWalkToSpot;
import org.pokebot.script.PopulateMap;

import java.util.HashSet;
import java.util.Set;

@Singleton
public final class ScriptManager
{
	private final EventBus eventBus;

	private final Keyboard keyboard;

	private final Injector injector = Pokebot.getInjector();

	private final Set<AbstractScript> scripts = new HashSet<>();

	@Inject
	public ScriptManager(final EventBus eventBus, final Keyboard keyboard)
	{
		this.eventBus = eventBus;
		this.keyboard = keyboard;
	}

	public void beforeRender(final float delta)
	{
		for (val script : scripts)
		{
			if (script.isBeingTerminated)
			{
				stopScript(script.getClass());
				continue;
			}

			script.beforeRender(delta);
		}
	}

	private void toggleScript(final Class<? extends AbstractScript> scriptClass)
	{
		if (scriptIsStarted(scriptClass))
		{
			stopScript(scriptClass);
		}
		else
		{
			startScript(scriptClass);
		}
	}

	private boolean scriptIsStarted(final Class<? extends AbstractScript> scriptClass)
	{
		for (val script : scripts)
		{
			if (script.getClass().equals(scriptClass))
			{
				return true;
			}
		}

		return false;
	}

	@SneakyThrows
	private void startScript(final Class<? extends AbstractScript> scriptClass)
	{
		if (scriptIsStarted(scriptClass))
		{
			return;
		}

		AbstractScript scriptInst = injector.getInstance(scriptClass);

		injector.injectMembers(scriptInst);
		scriptInst.onStart();
		scripts.add(scriptInst);
		eventBus.register(scriptInst);
	}

	private void stopScript(final Class<? extends AbstractScript> scriptClass)
	{
		val optMatch = scripts.stream()
				.filter(it -> it.getClass().equals(scriptClass))
				.findFirst();

		if (optMatch.isEmpty())
		{
			return;
		}

		val match = optMatch.get();

		eventBus.unregister(match);
		scripts.remove(match);
		match.onStop();
	}

	@Subscribe
	public void onKeyReleased(KeyReleased keyReleased)
	{
		val keyCode = keyReleased.getKeyCode();

		if (keyCode == KeyCode.KEY_NUMPAD1)
		{
			toggleScript(AutoFightScript.class);
		}

		if (keyCode == KeyCode.KEY_NUMPAD2)
		{
			toggleScript(AutoWalkToSpot.class);
		}

		if (keyCode == KeyCode.KEY_NUMPAD3)
		{
			toggleScript(PopulateMap.class);
		}
	}
}
