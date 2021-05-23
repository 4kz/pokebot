package org.pokebot.api.playerproperty;

public interface Title
{
	String getName();

	int getId();

	String getDescription(String key);

	TitleBenefits getTitleBenefits();
}
