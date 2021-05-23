package org.pokebot.api.entity;

import java.util.List;

public interface LocalPlayer extends Player
{
	List<OurPokemon> getPokemon();
}
