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
package quest.gelkmaros;

import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * @author VladimirZ
 */
public class _21036DeliveryofAetherSample extends QuestHandler {

	private final static int questId = 21036;
	public _21036DeliveryofAetherSample() {
		super(questId);
	}

	@Override
	public void register() {
		int[] npcs = {799258, 799238, 798713, 799239};
		for (int npc : npcs)
			qe.registerQuestNpc(npc).addOnTalkEvent(questId);
		qe.registerQuestNpc(799258).addOnQuestStart(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		QuestState qs = env.getPlayer().getQuestStateList().getQuestState(questId);
		if (env.getTargetId() == 799258) {
			if (qs == null || qs.getStatus() == QuestStatus.NONE) {
				switch (env.getDialog()) {
					case START_DIALOG:
					   return sendQuestDialog(env, 1011);
                    case ASK_ACCEPTION: {
                       return sendQuestDialog(env, 4);
                    }   
				    case ACCEPT_QUEST: {
					   return sendQuestStartDialog(env, 182207832, 1);
				    } 
                }
			}
		} if (qs == null)
			return false;
		if (qs.getStatus() == QuestStatus.START) {
		int var = qs.getQuestVarById(0);
			if (env.getTargetId() == 799238) {
				switch (env.getDialog()) {
					case START_DIALOG:
						if (var == 0)
							return sendQuestDialog(env, 1352);
					case STEP_TO_1:
						return defaultCloseDialog(env, 0, 1);
				}
			}
			else if (env.getTargetId() == 798713) {
				switch (env.getDialog()) {
					case START_DIALOG:
						if (var == 1)
							return sendQuestDialog(env, 1693);
					case STEP_TO_2:
						qs.setQuestVarById(0, var + 1);
					    qs.setStatus(QuestStatus.REWARD);
					    updateQuestStatus(env);
                    return closeDialogWindow(env);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (env.getTargetId() == 799239) {
				switch (env.getDialog()) {
					case START_DIALOG:
					return sendQuestDialog(env, 2375);
				} 
					return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}