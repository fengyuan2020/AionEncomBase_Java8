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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with aion-unique. If not, see <http://www.gnu.org/licenses/>.
 */
package quest.ishalgen;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * @author Mr. Poke
 */
public class _2114TheInsectProblem extends QuestHandler {

	private final static int questId = 2114;
	public _2114TheInsectProblem() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203533).addOnQuestStart(questId);
		qe.registerQuestNpc(203533).addOnTalkEvent(questId);
		qe.registerQuestNpc(210734).addOnKillEvent(questId);
		qe.registerQuestNpc(210380).addOnKillEvent(questId);
		qe.registerQuestNpc(210381).addOnKillEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (targetId == 203533) {
		    if (qs == null || qs.getStatus() == QuestStatus.NONE) {
				switch (env.getDialog()) {
					case START_DIALOG:
						return sendQuestDialog(env, 1011);
					case STEP_TO_1:
						if (QuestService.startQuest(env)) {
							qs = player.getQuestStateList().getQuestState(questId);
							qs.setQuestVar(1);
							updateQuestStatus(env);
							PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(env.getVisibleObject().getObjectId(), 10));
							return true;
						}
					case STEP_TO_2:
						if (QuestService.startQuest(env)) {
							qs = player.getQuestStateList().getQuestState(questId);
							qs.setQuestVar(11);
							updateQuestStatus(env);
							PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(env.getVisibleObject().getObjectId(), 10));
							return true;
						}
				}
			}
			else if (qs.getStatus() == QuestStatus.REWARD) {
				int var = qs.getQuestVarById(0);
				switch (env.getDialog()) {
					case USE_OBJECT:
					if (var == 10)
						return sendQuestDialog(env, 2034);
					else if (var == 20)
						return sendQuestDialog(env, 2375);
					case SELECT_REWARD: {
					if (var == 10)
						return sendQuestDialog(env, 5);
					else if (var == 20)
						return sendQuestDialog(env, 6);
                    }
					case SELECT_NO_REWARD:
					if (QuestService.finishQuest(env, var / 10 - 1)) {
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(env.getVisibleObject().getObjectId(), 10));
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean onKillEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		if (qs == null || qs.getStatus() == QuestStatus.START) {
		int var = qs.getQuestVarById(0);
		switch (targetId) {
			case 210734:
				if (var >= 1 && var < 10) {
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
					updateQuestStatus(env);
					return true;
				}
				else if (var == 10) {
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					return true;
				}
			case 210380:
			case 210381:
				if (var >= 11 && var < 20) {
					qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
					updateQuestStatus(env);
					return true;
				}
				else if (var == 20) {
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
					return true;
				}
            }
		}
		return false;
	}
}