/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

public class GeoDataConfig {

	/**
	 * Geodata enable
	 */
	@Property(key = "gameserver.geodata.enable", defaultValue = "false")
	public static boolean GEO_ENABLE;

	/**
	 * Enable canSee checks using geodata.
	 */
	@Property(key = "gameserver.geodata.cansee.enable", defaultValue = "true")
	public static boolean CANSEE_ENABLE;

	/**
	 * Enable Fear skill using geodata.
	 */
	@Property(key = "gameserver.geodata.fear.enable", defaultValue = "true")
	public static boolean FEAR_ENABLE;

	/**
	 * Enable Geo checks during npc movement (prevent flying mobs)
	 */
	@Property(key = "gameserver.geo.npc.move", defaultValue = "false")
	public static boolean GEO_NPC_MOVE;

	/**
	 * Enable npc checks aggro target visibility range (canSee)
	 */
	@Property(key = "gameserver.geo.npc.aggro", defaultValue = "false")
	public static boolean GEO_NPC_AGGRO;

	/**
	 * Enable geo materials using skills
	 */
	@Property(key = "gameserver.geo.materials.enable", defaultValue = "false")
	public static boolean GEO_MATERIALS_ENABLE;

	/**
	 * Show collision zone name and skill id
	 */
	@Property(key = "gameserver.geo.materials.showdetails", defaultValue = "false")
	public static boolean GEO_MATERIALS_SHOWDETAILS;

	/**
	 * Enable geo shields
	 */
	@Property(key = "gameserver.geo.shields.enable", defaultValue = "false")
	public static boolean GEO_SHIELDS_ENABLE;

	/**
	 * Enable geo doors
	 */
	@Property(key = "gameserver.geo.doors.enable", defaultValue = "false")
	public static boolean GEO_DOORS_ENABLE;

	/**
	 * Object factory for geodata primitives enabled
	 */
	@Property(key = "gameserver.geodata.objectfactory.enabled", defaultValue = "true")
	public static boolean GEO_OBJECT_FACTORY_ENABLE;

	/**
	 * If you use monon2 geo data for your server
	 */
	@Property(key = "gameserver.geodata.monon2.in.use", defaultValue = "fasle")
	public static boolean GEO_MONONO2_IN_USE;
	@Property(key = "gameserver.geo.nav.pathfinding.enable", defaultValue = "false")
	public static boolean GEO_NAV_ENABLE;
	
	/**
     * Maximum number of triangles to consider for a straight line path.
     */
	@Property(key = "gameserver.geo.nav.max.straight.line.triangles", defaultValue = "50")
    public static int GEO_NAV_MAX_STRAIGHT_LINE_TRIANGLES;
	
    /**
     * A value used when attempting to pathfind to a target that is not on the Nav Mesh.
     */
    @Property(key = "gameserver.geo.nav.arbitrary.small.value", defaultValue = "1")
    public static float GEO_NAV_ARBITRARY_SMALL_VALUE;

    /**
     * A value used when retracing or opening the list of nodes to create a pathway corridor.
     */
    @Property(key = "gameserver.geo.nav.arbitrary.large.value", defaultValue = "1500")
    public static int GEO_NAV_ARBITRARY_LARGE_VALUE;

    /**
     * A percentage of pathCost to add onto the basic path cost calculation
     * if the next node is moving away from the target node.
     */
    @Property(key = "gameserver.geo.nav.path.weight", defaultValue = "0.2")
    public static float GEO_NAV_PATH_WEIGHT;

    /**
     * A multiplier for {@link NavHeapNode#targetDist}. When the target distance is estimated,
     * it will be multiplied by this value. This is to give nodes that are closer to the target
     * a higher priority than nodes that are further away.
     */
    @Property(key = "gameserver.geo.nav.target.weight", defaultValue = "20")
    public static float GEO_NAV_TARGET_WEIGHT;
	
}