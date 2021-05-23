package org.pokebot.api;

import org.pokebot.api.entity.EnemyPlayer;
import org.pokebot.api.entity.Npc;
import org.pokebot.api.map.Dimension;

import java.util.List;

public interface MapManager
{
	int getMapId();

	Object getMap();

	Dimension getDimension(Object map);

	List<EnemyPlayer> getPlayers();

	List<Npc> getNpcs();

	boolean isBlocked(Object layer, int xTile, int yTile);

	boolean tileExists(Object layer, int xTile, int yTile);

	Object getCollisionMain();

	Object getGrass();

	Object getSurf();

	Object getBridgeEntryLayer();

	Object getBridgeExitLayer();

	Object getBridgeOverCollisionLayer();

	Object getBridgeUnderCollisionLayer();

	Object getBridgeLayer();

	Object getLedgeLeft();

	Object getLedgeRight();

	Object getLedgeUp();

	Object getLedgeDown();

	Object getBikeLedge();

	Object getRockClimb();

	Object getWaterfall();
}
