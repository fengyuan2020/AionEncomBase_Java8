/*
 * This file is part of aion-lightning <aion-lightning.com>.
 *
 *  aion-lightning is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  aion-lightning is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with aion-lightning.  If not, see <http://www.gnu.org/licenses/>.
 */
package quest.empyrean_crucible;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * @author Kamui
 */
public class _18209ARiftInTheSpaceTwineContinuum extends QuestHandler {

	private static final int questId = 18209;
	public _18209ARiftInTheSpaceTwineContinuum() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(205309).addOnQuestStart(questId);
		qe.registerQuestNpc(205309).addOnTalkEvent(questId);
		qe.registerQuestNpc(217819).addOnKillEvent(questId);
		qe.registerQuestNpc(218185).addOnKillEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == 205309) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 4762);
				else
					return sendQuestStartDialog(env);
			}
		}
		else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 205309) {
				switch (env.getDialog()) {
					case SELECT_REWARD:
						return sendQuestDialog(env, 5);
					default:
						return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}

	@Override
	public boolean onKillEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			int var1 = qs.getQuestVarById(1);
			
			if (var == 0 && var1 < 4)
				return defaultOnKillEvent(env, 217819, 0, 4, 1);
			else if (var == 0 && var1 == 4)
				return defaultOnKillEvent(env, 217819, 0, 1, 0);
			else if (var == 1 && env.getTargetId() == 218185){
				qs.setQuestVarById(2, 1);
				qs.setStatus(QuestStatus.REWARD);
				updateQuestStatus(env);
				return true;
			}				
		}
		return false;
	}
}