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
package quest.gelkmaros_armor;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestDialog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

public class _21054MissionofDestiny extends QuestHandler {

	private final static int questId = 21054;
	public _21054MissionofDestiny() {
		super(questId);
	}
	
	@Override
	public void register() {
		qe.registerQuestNpc(799318).addOnQuestStart(questId); //Batalrion
		qe.registerQuestNpc(799318).addOnTalkEvent(questId); //Batalrion
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestDialog dialog = env.getDialog();
		int targetId = env.getTargetId();
		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (targetId == 799318) { //Batalrion
				switch (dialog) {
					case START_DIALOG:
						return sendQuestDialog(env, 1011);
					case SELECT_ACTION_1012: {
						return sendQuestDialog(env, 1012);
					} case ASK_ACCEPTION: {
						return sendQuestDialog(env, 4);
					} case ACCEPT_QUEST: {
						return sendQuestStartDialog(env);
					} case REFUSE_QUEST: {
						return sendQuestDialog(env, 1004);
					}
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.START) {
			if (targetId == 799318) { //Batalrion
				if (dialog == QuestDialog.START_DIALOG) {
					return sendQuestDialog(env, 2375);
				} else if (dialog == QuestDialog.CHECK_COLLECTED_ITEMS) {
					long itemCount = player.getInventory().getItemCountByItemId(182207842);
					if (player.getInventory().tryDecreaseKinah(150000) && itemCount > 29) {
						player.getInventory().decreaseByItemId(182207842, 30);
						changeQuestStep(env, 0, 0, true);
						return sendQuestDialog(env, 5);
					} else
						return sendQuestDialog(env, 2716);
				}
			}
		} else if (qs == null || qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 799318) { //Batalrion
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}