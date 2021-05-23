package org.pokebot.api.util;

import lombok.val;
import org.pokebot.api.Client;
import org.pokebot.api.map.Tile;
import org.pokebot.api.map.Dimension;

public final class MapUtil
{
	public static Dimension getMapDimension(final Client client)
	{
		val map = client.getMapManager().getMap();
		return client.getMapManager().getDimension(map);
	}

	public static boolean tileIsBlocked(final Client client, final int xTile, final int yTile)
	{
		return tileIsCollidingWithMain(client, xTile, yTile) || tileHasNpc(client, xTile, yTile);
	}

	public static Tile closestTile(final Client client, final Object layer, final boolean skipOwnTile)
	{
		val playerPos = client.getLocalPlayer().getPosition();
		val playerTile = Tile.fromPosition(playerPos);

		val dimensions = getMapDimension(client);

		var closestDist = Integer.MAX_VALUE;
		Tile closestTile = null;
		for (int x = 0; x < dimensions.getWidth(); x++)
		{
			for (int y = 0; y < dimensions.getHeight(); y++)
			{
				if (skipOwnTile && playerTile.getX() == x && playerTile.getY() == y)
				{
					continue;
				}

				val exists = client.getMapManager().tileExists(layer, x, y);

				if (!exists)
				{
					continue;
				}

				val dist = Math.abs(playerTile.getX() - x) + Math.abs(playerTile.getY() - y);

				if (dist < closestDist)
				{
					closestTile = new Tile(x, y);
					closestDist = dist;
				}
			}
		}
		return closestTile;
	}

	private static boolean tileIsCollidingWithMain(final Client client, final int xTile, final int yTile)
	{
		val layer = client.getMapManager().getCollisionMain();
		return client.getMapManager().isBlocked(layer, xTile, yTile);
	}

	private static boolean tileHasNpc(final Client client, final int xTile, final int yTile)
	{
		for (val npc : client.getMapManager().getNpcs())
		{
			val tile = Tile.fromPosition(npc.getPosition());
			if (tile.getX() == xTile && tile.getY() == yTile)
			{
				return true;
			}
		}
		return false;
	}
}
