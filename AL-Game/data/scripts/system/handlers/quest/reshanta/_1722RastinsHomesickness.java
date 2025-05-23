/*
 * This file is part of aion-unique <aion-unique.org>.
 *
 * aion-unique is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * aion-unique is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with aion-unique.  If not, see <http://www.gnu.org/licenses/>.
 */
package quest.reshanta;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * @author Rhys2002
 */
public class _1722RastinsHomesickness extends QuestHandler {

	private final static int questId = 1722;
	private final static int[] npc_ids = { 278547, 278560, 278517, 278544, 278532, 278539, 278524, 278555, 278567 };
	public _1722RastinsHomesickness() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(278547).addOnQuestStart(questId);
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
		    if (targetId == 278547) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 4762);
				else
					return sendQuestStartDialog(env);
			}
		}
		if (qs == null)
			return false;
		if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 278547)
				removeQuestItem(env, 182202101, 1);
			return sendQuestEndDialog(env);
		}
		else if (qs.getStatus() == QuestStatus.START) {
		int var = qs.getQuestVarById(0);
		if (targetId == 278560) {
			switch (env.getDialog()) {
				case START_DIALOG:
					if (var == 0)
						return sendQuestDialog(env, 1011);
				case STEP_TO_1:
					if (var == 0) {
						qs.setQuestVarById(0, var + 1);
						updateQuestStatus(env);
            		    return closeDialogWindow(env);
					}
			}
		}
		else if (targetId == 278517) {
			switch (env.getDialog()) {
				case START_DIALOG:
					if (var == 1)
						return sendQuestDialog(env, 1352);
				case STEP_TO_2:
					if (var == 1) {
						qs.setQuestVarById(0, var + 1);
						updateQuestStatus(env);
            		    return closeDialogWindow(env);
					}
			}
		}
		else if (targetId == 278544) {
			switch (env.getDialog()) {
				case START_DIALOG:
					if (var == 2)
						return sendQuestDialog(env, 1693);
				case SELECT_REWARD:
					if (var == 2) {
						qs.setQuestVarById(0, var + 1);
						updateQuestStatus(env);
            		    return closeDialogWindow(env);
					}
			}
		}
		else if (targetId == 278532) {
			switch (env.getDialog()) {
				case START_DIALOG:
					if (var == 3)
						return sendQuestDialog(env, 2034);
				case STEP_TO_4:
					if (var == 3) {
						qs.setQuestVarById(0, var + 1);
						updateQuestStatus(env);
            		    return closeDialogWindow(env);
					}
			}
		}
		else if (targetId == 278539) {
			switch (env.getDialog()) {
				case START_DIALOG:
					if (var == 4)
						return sendQuestDialog(env, 2375);
				case STEP_TO_5:
					if (var == 4) {
						qs.setQuestVarById(0, var + 1);
						updateQuestStatus(env);
            		    return closeDialogWindow(env);
					}
			}
		}
		else if (targetId == 278524) {
			switch (env.getDialog()) {
				case START_DIALOG:
					if (var == 5)
						return sendQuestDialog(env, 2716);
				case STEP_TO_6:
					if (var == 5) {
						qs.setQuestVarById(0, var + 1);
						updateQuestStatus(env);
            		    return closeDialogWindow(env);
					}
			}
		}
		else if (targetId == 278555) {
			switch (env.getDialog()) {
				case START_DIALOG:
					if (var == 6)
						return sendQuestDialog(env, 3057);
				case STEP_TO_7:
					if (var == 6) {
						qs.setQuestVarById(0, var + 1);
						updateQuestStatus(env);
            		    return closeDialogWindow(env);
					}
			}
		}
		else if (targetId == 278567) {
			switch (env.getDialog()) {
				case START_DIALOG:
					if (var == 7)
						return sendQuestDialog(env, 3398);
				case SET_REWARD:
					if (var == 7) {
						giveQuestItem(env, 182202101, 1);
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
            		    return closeDialogWindow(env);
					}
			   }
		    }
		}
		return false;
	}
}
