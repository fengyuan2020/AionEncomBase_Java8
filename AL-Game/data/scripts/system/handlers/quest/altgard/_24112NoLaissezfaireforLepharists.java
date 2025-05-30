/**
 * This file is part of Aion-Lightning <aion-lightning.org>.
 *
 *  Aion-Lightning is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Aion-Lightning is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details. *
 *  You should have received a copy of the GNU General Public License
 *  along with Aion-Lightning.
 *  If not, see <http://www.gnu.org/licenses/>.
 */
package quest.altgard;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * @Author Majka Ajural. correct DainAvenger
 */

public class _24112NoLaissezfaireforLepharists extends QuestHandler {

	private final static int questId = 24112;
	public _24112NoLaissezfaireforLepharists() {
		super(questId);
	}
	
    @Override
	public void register() {
		qe.registerQuestNpc(203631).addOnQuestStart(questId);
		qe.registerQuestNpc(832821).addOnTalkEvent(questId);
		qe.registerQuestNpc(210510).addOnKillEvent(questId);
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc) {
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		}
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
		    if (targetId == 203631) { // Nokir
				switch (env.getDialog()) {
					case START_DIALOG:
						return sendQuestDialog(env, 1011);
					case ACCEPT_QUEST_SIMPLE:
					if (QuestService.startQuest(env)) {
						qs = player.getQuestStateList().getQuestState(questId);
					    qs.setQuestVarById(5, 1);
						updateQuestStatus(env);
				        return closeDialogWindow(env);
                    }
					case REFUSE_QUEST_SIMPLE:
				        return closeDialogWindow(env);
				}
			}
		}
		if (qs == null || qs.getStatus() == QuestStatus.START) {
	        if (targetId == 832821) { // Brodir
				switch (env.getDialog()) {
					case START_DIALOG:
						if (qs.getQuestVarById(0) == 0) { 
							return sendQuestDialog(env, 1352);
						}
						else if (qs.getQuestVarById(0) == 1) {
							return sendQuestDialog(env, 2375);
						}
					case STEP_TO_1:
						qs.setQuestVarById(5, 0);
						qs.setQuestVarById(0, 0);
						updateQuestStatus(env);
				        return closeDialogWindow(env);
					case SELECT_REWARD:
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						return sendQuestEndDialog(env);
                    }
				}
			}
            if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
	            if (targetId == 832821) { // Brodir
				    return sendQuestEndDialog(env);
			}
		}
		return false;
	}
	
	@Override
	public boolean onKillEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		if (qs.getQuestVarById(0) == 0 && targetId == 210510) {
			qs.setQuestVarById(0, 1);
			updateQuestStatus(env);
			return true;
		}
		return false;
	}
}	