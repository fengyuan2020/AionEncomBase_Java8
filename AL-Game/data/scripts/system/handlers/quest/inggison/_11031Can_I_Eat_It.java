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
package quest.inggison;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * @author VladimirZ
 */
public class _11031Can_I_Eat_It extends QuestHandler {

	private final static int questId = 11031;
	public _11031Can_I_Eat_It() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(798959).addOnQuestStart(questId);
		qe.registerQuestNpc(798959).addOnTalkEvent(questId);
		qe.registerQuestItem(182206724, questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		final QuestState qs = player.getQuestStateList().getQuestState(questId);
		int targetId = env.getTargetId();
		QuestDialog dialog = env.getDialog();
		if (targetId == 798959) {
			if (qs == null || qs.getStatus() == QuestStatus.NONE) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 4762);
				else
					return sendQuestStartDialog(env);
			}
        }
		if (qs == null)
			return false;
		if (qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);
			switch (env.getTargetId()) {
				case 798959:
					switch (env.getDialog()) {
						case START_DIALOG:
							if (var == 0)
								return sendQuestDialog(env, 1011);
							else if (var == 1)
								return sendQuestDialog(env, 1352);
						case CHECK_COLLECTED_ITEMS:
							if (var == 0) {
								if (QuestService.collectItemCheck(env, true)) {
									qs.setQuestVarById(0, var + 1);
									updateQuestStatus(env);
								return sendQuestDialog(env, 10000);
							} else 
								return sendQuestDialog(env, 10001);
							}
						case STEP_TO_2:
							if (var == 1) {
								giveQuestItem(env, 182206724, 1);
								qs.setQuestVarById(0, var + 1);
								updateQuestStatus(env);
				                return closeDialogWindow(env);
						}
						return true;
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD) {
			if (env.getTargetId() == 798959) {
				if (env.getDialog() == QuestDialog.START_DIALOG)
					return sendQuestDialog(env, 10002);
				else if (env.getDialogId() == QuestDialog.SELECT_REWARD.id())
					return sendQuestDialog(env, 5);
				else
					return sendQuestEndDialog(env);
			}
			return false;
		}
		return false;
	}

	@Override
    public HandlerResult onItemUseEvent(final QuestEnv env, Item item) {
        final Player player = env.getPlayer();
        final QuestState qs = player.getQuestStateList().getQuestState(questId);
        if (qs != null && qs.getStatus() == QuestStatus.START) {
            int var = qs.getQuestVarById(0);
			if (var == 2) {
                return HandlerResult.fromBoolean(useQuestItem(env, item, 2, 3, true));
            }
        }
        return HandlerResult.FAILED;
    }
}