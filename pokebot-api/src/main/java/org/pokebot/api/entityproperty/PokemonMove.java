package org.pokebot.api.entityproperty;

public interface PokemonMove
{
	int getId();

	int getPp();

	int getMaxPp();

	String getName();

	int getSlotNumber();

	int getMaxPpUseCounter();
}
