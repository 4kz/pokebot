package org.pokebot.patcher;

import static org.objectweb.asm.Opcodes.ASM9;

public final class Constants
{
	/**
	 * Tells if debugging mode is activated.
	 */
	public static final boolean DEBUG = true;

	/**
	 * Package prefix that is used for the PBO classes.
	 */
	public static final String PACKAGE_PREFIX = "com/pbo/";

	/**
	 * Package prefix that is used for the obfuscated PBO classes.
	 */
	public static final String OBFUSCATED_PREFIX = PACKAGE_PREFIX + "game/client/";

	/**
	 * API version for the ASM library.
	 */
	public static final int ASM_VERSION = ASM9;
}
