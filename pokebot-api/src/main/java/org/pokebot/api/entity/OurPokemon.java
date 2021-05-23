package org.pokebot.api.entity;

import org.pokebot.api.entityproperty.PokemonMove;

import java.util.List;

public interface OurPokemon extends Pokemon
{
	List<PokemonMove> getMoves();

	int getExperience();

	int getExperienceForNextLevel();

	int getSlotNumber();

	int getEvolutionPokedexNumber();

	String getNature();

	String getCaughtPlace();

	String getOriginalOwner();

	String getAbility();

	int getLevel();

	int getCurrentHp();

	int getHappiness();
}
