package org.pokebot.api.event;

import lombok.Value;
import org.pokebot.api.screen.Screen;

/**
 * Event that is fired when the game screen changes.
 */
@Value
public class GameScreenChanged
{
	/**
	 * The {@link Screen} that is currently active.
	 */
	Screen screen;
}
