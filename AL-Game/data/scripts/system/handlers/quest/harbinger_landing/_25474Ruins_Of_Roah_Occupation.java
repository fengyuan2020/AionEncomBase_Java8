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
package quest.harbinger_landing;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/****/
/** Author Ghostfur & Unknown (Aion-Unique)
/****/

public class _25474Ruins_Of_Roah_Occupation extends QuestHandler {

    private final static int questId = 25474;
    public _25474Ruins_Of_Roah_Occupation() {
        super(questId);
    }
	
    public void register() {
        qe.registerQuestNpc(805815).addOnQuestStart(questId);
        qe.registerQuestNpc(805815).addOnTalkEvent(questId);
		qe.registerQuestNpc(883148).addOnKillEvent(questId);
		qe.registerQuestNpc(883160).addOnKillEvent(questId);
		qe.registerQuestNpc(883172).addOnKillEvent(questId);
		qe.registerQuestNpc(883016).addOnKillEvent(questId);
		qe.registerQuestNpc(883022).addOnKillEvent(questId);
		qe.registerQuestNpc(883028).addOnKillEvent(questId);
		qe.registerQuestNpc(883150).addOnKillEvent(questId);
		qe.registerQuestNpc(883162).addOnKillEvent(questId);
		qe.registerQuestNpc(883174).addOnKillEvent(questId);
		qe.registerQuestNpc(883018).addOnKillEvent(questId);
		qe.registerQuestNpc(883024).addOnKillEvent(questId);
		qe.registerQuestNpc(883030).addOnKillEvent(questId);
    }
	
    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
            if (targetId == 805815) {
                if (env.getDialog() == QuestDialog.START_DIALOG) {
                    return sendQuestDialog(env, 4762);
                } else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 805815) {
				if (env.getDialogId() == 1352) {
					return sendQuestDialog(env, 5);
				} else {
					return sendQuestEndDialog(env);
				}
			}
		}
        return false;
    }
	
    public boolean onKillEvent(QuestEnv env) {
        Player player = env.getPlayer();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            switch (env.getTargetId()) {
				case 883148:
				case 883160:
				case 883172:
				case 883016:
				case 883022:
				case 883028:
				case 883150:
				case 883162:
				case 883174:
				case 883018:
				case 883024:
				case 883030:
                if (qs.getQuestVarById(1) < 1) {
					qs.setQuestVarById(1, qs.getQuestVarById(1) + 1);
					updateQuestStatus(env);
				} if (qs.getQuestVarById(1) >= 1) {
					qs.setQuestVarById(0, 1);
					qs.setStatus(QuestStatus.REWARD);
					updateQuestStatus(env);
				}
            }
        }
        return false;
    }
}