/*
 * =====================================================================================*
 * This file is part of Aion-Unique (Aion-Unique Home Software Development)             *
 * Aion-Unique Development is a closed Aion Project that use Old Aion Project Base      *
 * Like Aion-Lightning, Aion-Engine, Aion-Core, Aion-Extreme, Aion-NextGen, ArchSoft,   *
 * Aion-Ger, U3J, Encom And other Aion project, All Credit Content                      *
 * That they make is belong to them/Copyright is belong to them. And All new Content    *
 * that Aion-Unique make the copyright is belong to Aion-Unique                         *
 * You may have agreement with Aion-Unique Development, before use this Engine/Source   *
 * You have agree with all of Term of Services agreement with Aion-Unique Development   *
 * =====================================================================================*
 */
package quest.esoterrace;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/****/
/** Author Ghostfur & Unknown (Aion-Unique)
/****/
public class _27540The_Drana_Experiment extends QuestHandler {

    private final static int questId = 27540;
    public _27540The_Drana_Experiment() {
        super(questId);
    }
	
    public void register() {
		qe.registerOnEnterWorld(questId);
        qe.registerQuestNpc(799558).addOnQuestStart(questId); //Luigir.
        qe.registerQuestNpc(799558).addOnTalkEvent(questId); //Luigir.
		qe.registerQuestNpc(799563).addOnTalkEvent(questId); //Nepion.
		qe.registerQuestNpc(217185).addOnKillEvent(questId); //Dalia Charlands.
		qe.registerQuestNpc(217195).addOnKillEvent(questId); //Captain Murugan.
    }
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 799558) {
                switch (env.getDialog()) {
                    case START_DIALOG: {
                        return sendQuestDialog(env, 4762);
					} case ACCEPT_QUEST:
					case ACCEPT_QUEST_SIMPLE: {
						return sendQuestStartDialog(env);
					} case REFUSE_QUEST_SIMPLE: {
				        return closeDialogWindow(env);
					}
                }
			}
		}
		if (qs == null || qs.getStatus() == QuestStatus.START) {
			if (targetId == 799563) { //Nepion.
                switch (env.getDialog()) {
                    case START_DIALOG: {
                        if (qs.getQuestVarById(0) == 1) {
                            return sendQuestDialog(env, 1352);
                        }
					} case SELECT_ACTION_1353: {
						if (qs.getQuestVarById(0) == 1) {
							playQuestMovie(env, 473);
							return sendQuestDialog(env, 1353);
						}
					} case STEP_TO_2: {
						changeQuestStep(env, 1, 2, false);
						return closeDialogWindow(env);
					}
				}
            } else if (targetId == 799553) { //Luigir.
                switch (env.getDialog()) {
                    case START_DIALOG: {
                        if (qs.getQuestVarById(0) == 4) {
                            return sendQuestDialog(env, 2375);
                        }
					} case SELECT_ACTION_2376: {
						if (qs.getQuestVarById(0) == 4) {
							return sendQuestDialog(env, 2376);
						}
					} case CHECK_COLLECTED_ITEMS: {
					    if (QuestService.collectItemCheck(env, true)) {
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(env);
							return sendQuestDialog(env, 10000);
						} else {
							return sendQuestDialog(env, 10001);
						}
					}
				}
            }
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 799558) { //Luigir.
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 10002);
				} else if (env.getDialog() == QuestDialog.SELECT_REWARD) {
					return sendQuestDialog(env, 5);
				} else {
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}
	
	@Override
    public boolean onEnterWorldEvent(QuestEnv env) {
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
			if (player.getWorldId() == 300250000) {
				changeQuestStep(env, 0, 1, false);
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean onKillEvent(QuestEnv env) {
		final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			if (qs.getQuestVarById(0) == 2) { //Dalia Charlands.
				return defaultOnKillEvent(env, 217185, 2, 3);
			} else if (qs.getQuestVarById(0) == 3) { //Captain Murugan.
				return defaultOnKillEvent(env, 217195, 3, 4);
			}
		}
		return false;
	}
}