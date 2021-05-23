package org.pokebot.api.entity;

import org.pokebot.api.playerproperty.AdminLevel;
import org.pokebot.api.playerproperty.Title;

public interface Player extends Movable
{
	String getName();

	Title getTitle();

	AdminLevel getAdminLevel();
}
