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

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * @author Cheatkiller
 *
 */
public class _21075FatedHeartbreak extends QuestHandler {

	private final static int questId = 21075;
	private int rewardIndex;
	public _21075FatedHeartbreak() {
		super(questId);
	}

	public void register() {
		qe.registerQuestNpc(799409).addOnQuestStart(questId);
		qe.registerQuestNpc(799409).addOnTalkEvent(questId);
		qe.registerQuestNpc(798392).addOnTalkEvent(questId);
		qe.registerQuestNpc(799410).addOnTalkEvent(questId);
		qe.registerQuestNpc(204138).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 799409) { 
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 4762);
				}
				else {
					return sendQuestStartDialog(env);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.START) {
			if (targetId == 798392) {
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 1011);
				}
				else if (dialog == QuestDialog.STEP_TO_1) {
					giveQuestItem(env, 182207917, 1);
					return defaultCloseDialog(env, 0, 1);
				}
				else if (dialog == QuestDialog.STEP_TO_2) {
					giveQuestItem(env, 182207917, 1);
					return defaultCloseDialog(env, 0, 2);
				}
			}
			else if (targetId == 799410) {
				if (dialog == QuestDialog.START_DIALOG) {
					if (qs.getQuestVarById(0) == 1)
					return sendQuestDialog(env, 1352);
				}
				else if (dialog == QuestDialog.SET_REWARD) {
					rewardIndex = 0;
					removeQuestItem(env, 182207917, 1);
					return defaultCloseDialog(env, 1, 1, true, false);
				}
			}
			else if (targetId == 204138) {
				if (dialog == QuestDialog.START_DIALOG) {
					if (qs.getQuestVarById(0) == 2)
					return sendQuestDialog(env, 1693);
				}
				else if (dialog == QuestDialog.SET_REWARD) {
					rewardIndex = 1;
					removeQuestItem(env, 182207917, 1);
					return defaultCloseDialog(env, 2, 2, true, false);
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 799409) {
				if (dialog == QuestDialog.USE_OBJECT) {
					return sendQuestDialog(env, 10002);
				}
				return sendQuestEndDialog(env, rewardIndex);
			}
		}
		return false;
	}
}