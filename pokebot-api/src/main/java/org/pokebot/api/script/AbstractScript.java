package org.pokebot.api.script;

import java.io.IOException;
import java.util.Objects;

/**
 * The {@link AbstractScript} is a class that should be extended
 * by every script.
 */
public abstract class AbstractScript
{
	/**
	 * Tells that the script is requested to be stopped.
	 */
	public boolean isBeingTerminated = false;

	/**
	 * Will allow the user to run logic initialization before
	 * any other logic is ran.
	 */
	public void onStart() throws IOException
	{
	}

	/**
	 * This is basically the game tick. It will be called before
	 * any input is processed and allows the user to handle
	 * the script logic.
	 *
	 * @param delta The time difference since the last
	 *              beforeRender function was called.
	 */
	public void beforeRender(final float delta)
	{
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
	public final void terminateScript()
	{
		isBeingTerminated = true;
	}

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
