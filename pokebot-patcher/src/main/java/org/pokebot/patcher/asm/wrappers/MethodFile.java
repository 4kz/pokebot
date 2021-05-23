package org.pokebot.patcher.asm.wrappers;

import org.objectweb.asm.tree.MethodNode;
import org.pokebot.patcher.Constants;

/**
 * An extension of {@link MethodNode} that stores additional
 * data and has utility functions.
 */
public final class MethodFile extends MethodNode
{
	public MethodFile(
			final int access,
			final String name,
			final String descriptor,
			final String signature,
			final String[] exceptions)
	{
		super(Constants.ASM_VERSION, access, name, descriptor, signature, exceptions);
	}
}
