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
 * @author dta3000
 */
public class _11012PracticalNursing extends QuestHandler {

	private final static int questId = 11012;
	public _11012PracticalNursing() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(799071).addOnQuestStart(questId);
		qe.registerQuestNpc(799071).addOnTalkEvent(questId);
		qe.registerQuestNpc(799072).addOnTalkEvent(questId);
		qe.registerQuestNpc(799073).addOnTalkEvent(questId);
		qe.registerQuestNpc(799074).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
		    if (targetId == 799071) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				}
				else if (env.getDialogId() == 1007) {
					return sendQuestDialog(env, 4);
				}
				else if (env.getDialogId() == 1002) {
					return sendQuestStartDialog(env, 182206715, 3);
				}
			}
		}
		if (qs == null)
			return false;
		if (qs.getStatus() == QuestStatus.START) {
		int var = qs.getQuestVarById(0);
			if (targetId == 799072 && var == 0) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				}
				else if (env.getDialog() == QuestDialog.STEP_TO_1) {
					qs.setQuestVar(++var);
					updateQuestStatus(env);
					removeQuestItem(env, 182206715, 1);
				    return closeDialogWindow(env);
				}
			}
			else if (targetId == 799073 && var == 1) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1352);
				}
				else if (env.getDialog() == QuestDialog.STEP_TO_2) {
					qs.setQuestVar(++var);
					updateQuestStatus(env);
					removeQuestItem(env, 182206715, 1);
				    return closeDialogWindow(env);
				}
			}
			else if (targetId == 799074 && var == 2) {
				if (env.getDialog() == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1693);
				}
				else if (env.getDialog() == QuestDialog.STEP_TO_3) {
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					removeQuestItem(env, 182206715, 1);
				    return closeDialogWindow(env);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 799071) {
				if (env.getDialog() == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 10002);
				}
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}