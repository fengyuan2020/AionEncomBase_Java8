/**
 * This file is part of aion-engine <aion-engine.com>
 *
 *  aion-engine is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  aion-engine is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with aion-engine.  If not, see <http://www.gnu.org/licenses/>.
 */
package quest.inggison;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * @author Leunam
 */
public class _11105WifesNagging extends QuestHandler {

	private final static int questId = 11105;
	private final static int[] npc_ids = {798948, 798984, 798980};
	public _11105WifesNagging() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(798948).addOnQuestStart(questId);
		for (int npc_id : npc_ids)
			qe.registerQuestNpc(npc_id).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
		    if (targetId == 798948) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 1011);
				else if (env.getDialogId() == 1007) {
					return sendQuestDialog(env, 4);
				}
				else if (env.getDialogId() == 1002) {
					return sendQuestStartDialog(env, 182206778, 1);
				}
			}
		}
		if (qs == null)
			return false;
		else if (qs.getStatus() == QuestStatus.START) {
		int var = qs.getQuestVarById(0);
		if (targetId == 798984) {
			switch (env.getDialog()) {
				case START_DIALOG:
					if (var == 0)
						return sendQuestDialog(env, 1352);
				case STEP_TO_1:
					if (var == 0) {
						removeQuestItem(env, 182206778, 1);
						giveQuestItem(env, 182206779, 1);
						qs.setQuestVarById(0, var + 1);
						updateQuestStatus(env);
				        return closeDialogWindow(env);
					}
			}
		}
		else if (targetId == 798980) {
			switch (env.getDialog()) {
				case START_DIALOG:
					if (var == 1)
						return sendQuestDialog(env, 1693);
				case STEP_TO_2:
					if (var == 1) {
						removeQuestItem(env, 182206779, 1);
						qs.setQuestVarById(0, var + 1);
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
				        return closeDialogWindow(env);
					}
		        }
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798948) {
				if (env.getDialog() == QuestDialog.USE_OBJECT)
					return sendQuestDialog(env, 2375);
				else if (env.getDialogId() == 1009)
					return sendQuestDialog(env, 5);
				else
					return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}