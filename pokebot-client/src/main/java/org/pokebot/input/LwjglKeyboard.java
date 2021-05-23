package org.pokebot.input;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.val;
import org.pokebot.api.event.KeyPressed;
import org.pokebot.api.event.KeyReleased;
import org.pokebot.api.input.KeyCode;
import org.pokebot.api.input.Keyboard;

import java.nio.ByteBuffer;

/**
 * A {@link Keyboard} implementation that modifies and accesses
 * the keyDownBuffer. This is most likely as low-level as possible
 * with reflection or injection.
 */
@Singleton
public final class LwjglKeyboard implements Keyboard
{
	@Inject
	private EventBus eventBus;

	/**
	 * Array required to keep track of key changes.
	 */
	private boolean[] keyDown = new boolean[KeyCode.KEY_SLEEP + 1];

	/**
	 * Buffer that will be obtained from the Lwjql Keyboard class.
	 */
	private final ByteBuffer keyDownBuffer;

	/**
	 * Creates a new LwjglKeyboard instance. It will obtain the keyDownBuffer
	 * field from the Lwjgl library that GDX uses in turn.
	 */
	public LwjglKeyboard()
	{
		try
		{
			val keyboardClass = Class.forName("org.lwjgl.input.Keyboard");
			val keyDownBufferField = keyboardClass.getDeclaredField("keyDownBuffer");
			keyDownBufferField.setAccessible(true);
			keyDownBuffer = (ByteBuffer) keyDownBufferField.get(null);
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Unable to get the LWJGL keyDownBuffer field");
		}
	}

	/**
	 * Function that will be called to fire key change
	 * events into the EventBus.
	 */
	public void handleKeyChanges()
	{
		for (var i = 0; i < keyDown.length; i++)
		{
			val keyIsDown = isKeyDown(i);

			if (keyIsDown && !keyDown[i])
			{
				eventBus.post(new KeyPressed(i));
				keyDown[i] = true;
			}
			else if (!keyIsDown && keyDown[i])
			{
				eventBus.post(new KeyReleased(i));
				keyDown[i] =  false;
			}
		}
	}

	/**
	 * Simulates that the key is down. It will automatically
	 * go up the next iteration as this buffer is modified
	 * by the Lwjgl library.
	 *
	 * @param keyCode The {@link org.pokebot.api.input.KeyCode} to press down.
	 */
	@Override
	public void setKeyDown(final int keyCode)
	{
		keyDownBuffer.put(keyCode, (byte) 1);
	}

	/**
	 * Tells if the specified key is down.
	 *
	 * @param keyCode @param keyCode The {@link org.pokebot.api.input.KeyCode} to get the status for.
	 * @return Will return true if the key is down.
	 */
	@Override
	public boolean isKeyDown(final int keyCode)
	{
		return keyDownBuffer.get(keyCode) != 0;
	}
}
