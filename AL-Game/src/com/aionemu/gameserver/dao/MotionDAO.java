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
package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.motion.Motion;

import java.util.List;

/**
 * @author MrPoke
 * @rework: MATTY
 */
public abstract class MotionDAO implements DAO {

    public abstract List<Motion> loadMotions(Integer playerId);

	public abstract void loadMotionList(Player player);

	public abstract boolean storeMotion(int objectId, Motion motion);

	public abstract boolean updateMotion(int objectId, Motion motion);

	public abstract boolean deleteMotion(int objectId, int motionId);

	@Override
	public String getClassName() {
		return MotionDAO.class.getName();
	}
}