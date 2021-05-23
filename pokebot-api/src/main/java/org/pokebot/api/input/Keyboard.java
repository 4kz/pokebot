package org.pokebot.api.input;

/**
 * Allows one to send keyboard events and obtain
 * information of the current state of the keyboard.
 */
public interface Keyboard
{
	/**
	 * Simulates that the key is down. It will automatically
	 * go up the next iteration.
	 *
	 * @param keyCode The {@link org.pokebot.api.input.KeyCode} to press down.
	 */
	void setKeyDown(int keyCode);

	/**
	 * Tells if the specified key is down.
	 *
	 * @param keyCode @param keyCode The {@link org.pokebot.api.input.KeyCode} to get the status for.
	 * @return Will return true if the key is down.
	 */
	boolean isKeyDown(int keyCode);

	/**
	 * Function that will be called to fire key change
	 * events into the EventBus.
	 */
	void handleKeyChanges();
}
