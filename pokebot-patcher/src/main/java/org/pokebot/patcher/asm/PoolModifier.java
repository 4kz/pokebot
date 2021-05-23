package org.pokebot.patcher.asm;

/**
 * Every class that will modify the {@link ClassPool}
 * should implement this {@link PoolModifier} interface.
 */
public interface PoolModifier
{
	ClassPool run(final ClassPool classPool);
}
