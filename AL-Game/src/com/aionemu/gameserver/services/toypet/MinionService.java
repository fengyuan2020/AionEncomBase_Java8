/**
 *  This file is part of Encom.
 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.services.toypet;

import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.controllers.MinionController;
import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.dao.PlayerMinionsDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Minion;
import com.aionemu.gameserver.model.gameobjects.player.MinionCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.minion.MinionBuff;
import com.aionemu.gameserver.model.team2.common.legacy.LootRuleType;
import com.aionemu.gameserver.model.templates.item.ItemUseLimits;
import com.aionemu.gameserver.model.templates.item.actions.AbstractItemAction;
import com.aionemu.gameserver.model.templates.item.actions.ItemActions;
import com.aionemu.gameserver.model.templates.minion.MinionEvolved;
import com.aionemu.gameserver.model.templates.minion.MinionSkill;
import com.aionemu.gameserver.model.templates.minion.MinionTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MINIONS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.restrictions.RestrictionsManager;
import com.aionemu.gameserver.services.SkillLearnService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.knownlist.PlayerAwareKnownList;

/*
 * Reworked by G-Robson26
 * Rework & Test : MATTY
 */

public class MinionService {
	private static List<Integer> minions;
	private MinionBuff minionbuff;
	private Logger log = LoggerFactory.getLogger(MinionService.class);

	public void init() {
		minions = DataManager.MINION_DATA.getAll();
		minionbuff = new MinionBuff();
		log.info("MinionService initialized");
	}

	public void onPlayerLogin(Player player) {
		PacketSendUtility.sendPacket(player, new SM_MINIONS(0, player.getMinionList().getMinions()));
		PacketSendUtility.sendPacket(player, new SM_MINIONS(9, 0));
		PacketSendUtility.sendPacket(player, new SM_MINIONS(11, player.getMinionSkillPoints(), false));
		PacketSendUtility.sendPacket(player, new SM_MINIONS(12));
	}

	public void addMinion(final Player player, final int itemObjId) {
		if (player.getMinionList().getMinions().size() == 200) {
			PacketSendUtility.sendMessage(player, "Max 200 Minion!");
			return;
		}

		final Item item = player.getInventory().getItemByObjId(itemObjId);
		PacketSendUtility.broadcastPacket(player,
				new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), itemObjId, item.getItemId(), 1500, 0), true);

		final ItemUseObserver itemUseObserver = new ItemUseObserver() {

			@Override
			public void abort() {
				player.getController().cancelTask(TaskId.ITEM_USE);
				player.removeItemCoolDown(item.getItemTemplate().getUseLimits().getDelayId());
				PacketSendUtility.sendPacket(player,
						SM_SYSTEM_MESSAGE.STR_ITEM_CANCELED(new DescriptionId(item.getItemTemplate().getNameId())));
				PacketSendUtility.broadcastPacket(player,
						new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), itemObjId, item.getItemId(), 0, 2), true);
				player.getObserveController().removeObserver(this);
			}
		};

		player.getObserveController().attach(itemUseObserver);
		player.getController().addTask(TaskId.ITEM_USE, ThreadPoolManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				player.getObserveController().removeObserver(itemUseObserver);
				player.getController().cancelTask(TaskId.ITEM_USE);
				PacketSendUtility.broadcastPacket(player,
						new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), itemObjId, item.getItemId(), 0, 1), true);

				if (!player.getInventory().decreaseByObjectId(itemObjId, 1)) {
					return;
				}
				int rnd = 0;
				int minionId = 0;
				String grade = "";
				int level = 0;
				String name = "";
				int growthPoint = 0;
				if (!item.getItemTemplate().getMinionTicket()) {
					return;
				}

				MinionTemplate minionTemplate = null;
				// Изменение шансов на создание Миньонов (Уменьшил максимально A грейд, если нужно правте)
				if (item.getItemTemplate().isMinionCashContract()) {
					switch (item.getItemTemplate().getTemplateId()) {
						case 190080017: // Shita (A)
							minionId = 980073;
							break;
						case 190080018: // Grendal (A)
							minionId = 980063;
							break;
						case 190080028: // Abija's (A)
							minionId = 980043;
							break;
						case 190080029: // Hamerun's (A)
							minionId = 980053;
							break;
						case 190080030: // Kerubiel's (A)
							minionId = 980013;
							break;
						case 190080031: // Seiren's (A)
							minionId = 980023;
							break;
						case 190080032: // Steel Rose's (A)
							minionId = 980033;
							break;
						case 190080033: // Kerubian's (C)
							minionId = 980011;
							break;
						case 190080035: // Shita (A)
							minionId = 980073;
							break;
						case 190080036: // Grendal (A)
							minionId = 980063;
							break;
						case 190080008: // Cute Minion Contract
						case 190080013: // Cute Minion Contract
							rnd = Rnd.get(0, 1000);
							minionId = minionId(rnd);
							break;
						default:
							minionId = minions.get(new Random().nextInt(minions.size()));
							break;
					}
				} else {
					switch (item.getItemTemplate().getTemplateId()) {
						case 190089999:
						case 190080005:
						case 190080009: // Lesser Minion Contract
						case 190080010:
						case 190080011:
							rnd = Rnd.get(0, 420);
							while (rnd >= 106 && rnd <= 140) { // Kerubiel A
								rnd = Rnd.get(0, 420);
							}
							minionId = minionId(rnd);
							break;
						case 190080012: // Special Minion Contract
						case 190080006: // Normal Minion Contract
							rnd = Rnd.get(0, 1000);
							minionId = minionId(rnd);
							break;
						case 190080007: // Larger Minion Contract
							rnd = Rnd.get(0, 1000);
							minionId = minionId(rnd);
							break;
						default:
							minionId = minions.get(new Random().nextInt(minions.size()));
							break;
					}
				}

				minionTemplate = DataManager.MINION_DATA.getMinionTemplate(minionId);
				grade = minionTemplate.getGrade();
				level = minionTemplate.getLevel();
				name = minionTemplate.getName();
				growthPoint = minionTemplate.getGrowthPt();

				MinionCommonData addNewMinion = player.getMinionList().addNewMinion(player, minionId, name, grade, level, growthPoint);

				if (addNewMinion != null) {
					PacketSendUtility.sendPacket(player, new SM_MINIONS(1, addNewMinion, 0));
					player.getMinionList().updateMinionsList();
					checkQuest(player, item);
				}
			}
		}, 1500));
	}

	private static void checkQuest(Player player, Item item) {
		switch (player.getRace()) {
		case ELYOS:
			if (player.getQuestStateList().hasQuest(15545) && item.getItemId() == 190080010) {
				QuestState qs = player.getQuestStateList().getQuestState(15545);
				if (qs.getStatus() == QuestStatus.START) {
					qs.setQuestVar(1);
					qs.setStatus(QuestStatus.REWARD);
					PacketSendUtility.sendPacket(player,
							new SM_QUEST_ACTION(15545, qs.getStatus(), qs.getQuestVars().getQuestVars()));
					player.getController().updateNearbyQuests();
				}
			}
			break;
		case ASMODIANS:
			if (player.getQuestStateList().hasQuest(25545) && item.getItemId() == 190080011) {
				QuestState qs = player.getQuestStateList().getQuestState(25545);
				if (qs.getStatus() == QuestStatus.START) {
					qs.setQuestVar(1);
					qs.setStatus(QuestStatus.REWARD);
					PacketSendUtility.sendPacket(player,
							new SM_QUEST_ACTION(25545, qs.getStatus(), qs.getQuestVars().getQuestVars()));
					player.getController().updateNearbyQuests();
				}
			}
			break;
		default:
			break;
		}
	}

	public void spawnMinion(Player player, int minionObjId) {
		MinionCommonData minionCommonData = player.getMinionList().getMinion(minionObjId);
		MinionTemplate minionTemplate = DataManager.MINION_DATA.getMinionTemplate(minionCommonData.getMinionId());
		MinionController controller = new MinionController();
		Minion minion = new Minion(minionTemplate, controller, minionCommonData, player);
		Iterator<MinionSkill> iterator = minionTemplate.getAction().getSkillsCollections().iterator();
		while (iterator.hasNext()) {
			player.getSkillList().addSkill(player, iterator.next().getSkillId(), 1);
		}
		if (player.getMinion() != null) {
			despawnMinion(player, player.getMinionList().getLastUsed());
		}
		minion.setKnownlist(new PlayerAwareKnownList(minion));
		player.setMinion(minion);
		player.getMinionList().setLastUsed(minionObjId);
		minionbuff.apply(player, minionCommonData.getMinionId());
		PacketSendUtility.broadcastPacketAndReceive(player, new SM_MINIONS(5, minionCommonData));
	}

	public void despawnMinion(Player player, int minionObjId) {
		Minion minion = player.getMinion();
		int despawnMinionObjId = 0;
		if (minionObjId == 0) {
			despawnMinionObjId = minion.getObjectId();
		} else {
			despawnMinionObjId = minionObjId;
		}
		MinionCommonData minionCommonData = player.getMinionList().getMinion(despawnMinionObjId);
		Iterator<MinionSkill> iterator = DataManager.MINION_DATA.getMinionTemplate(minionCommonData.getMinionId())
				.getAction().getSkillsCollections().iterator();
		while (iterator.hasNext()) {
			SkillLearnService.removeSkill(player, iterator.next().getSkillId());
		}
		minionCommonData.setIsLooting(false);
		minionCommonData.setIsBuffing(false);
		player.getMinion().getController().delete();
		player.setMinion(null);
		minionbuff.end(player);
		PacketSendUtility.broadcastPacketAndReceive(player, new SM_MINIONS(6, minionCommonData));
	}

	public void growthUpMinion(Player player, int minionObjectId, List<Integer> material) {
		int growthPoint = 0;
		long growthCost = 0;
		String tierGrade = "";
		MinionCommonData playerMinion = player.getMinionList().getMinion(minionObjectId);
		tierGrade = DataManager.MINION_DATA.getMinionTemplate(playerMinion.getMinionId()).getGrade();
		int maxgrowthMax = DataManager.MINION_DATA.getMinionTemplate(playerMinion.getMinionId()).getMaxGrowthValue();
		for (MinionCommonData list : player.getMinionList().getMinions()) {
			for (int matObjt : material) {
				if (list.getObjectId() == matObjt) {
					int minionGrowth = 0;
					if (DataManager.MINION_DATA.getMinionTemplate(list.getMinionId()).getGrade()
							.equalsIgnoreCase(tierGrade)) {
						minionGrowth = DataManager.MINION_DATA.getMinionTemplate(list.getMinionId()).getGrowthPt() * 2;
					} else {
						minionGrowth = DataManager.MINION_DATA.getMinionTemplate(list.getMinionId()).getGrowthPt();
					}
					growthPoint += minionGrowth;
					growthCost += DataManager.MINION_DATA.getMinionTemplate(list.getMinionId()).getGrowthCost();
				}
			}
		}
		if (growthPoint <= 0) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FAMILIAR_GROWTH_MSG_NOTSELECT);
			return;
		}
		if (player.getInventory().getKinah() < growthCost) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FAMILIAR_GROWTH_MSG_NOGOLD);
			return;
		}
		player.getInventory().decreaseKinah(growthCost);
		if (playerMinion.getMinionGrowthPoint() + growthPoint > maxgrowthMax) {
			playerMinion.setMinionGrowthPoint(maxgrowthMax);
		} else {
			playerMinion.setMinionGrowthPoint(playerMinion.getMinionGrowthPoint() + growthPoint);
		}
		DAOManager.getDAO(PlayerMinionsDAO.class).updatePlayerMinionGrowthPoint(player, playerMinion);
		PacketSendUtility.broadcastPacket(player, new SM_MINIONS(7, playerMinion), true);
		for (int matObjt2 : material) {
			deleteMinion(player, matObjt2, true);
		}
		player.getMinionList().updateMinionsList();
	}

	public void evolutionUpMinion(Player player, int minionObjId) {
		MinionCommonData minion = player.getMinionList().getMinion(minionObjId);
		MinionEvolved items = DataManager.MINION_DATA
				.getMinionTemplate(player.getMinionList().getMinion(minionObjId).getMinionId()).getEvolved();
		if (minion.getMinionLevel() >= 4) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FAMILIAR_EVOLVE_MSG_NOEVOLVE);
			return;
		}
		if (player.getInventory().getKinah() < items.getEvolvedCost()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FAMILIAR_EVOLVE_MSG_NOGOLD);
			return;
		}
		if (player.getInventory().getItemCountByItemId(190200000) < items.getEvolvedNum()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FAMILIAR_EVOLVE_MSG_LACK_ITEM);
			return;
		}
		player.getInventory().decreaseKinah(items.getEvolvedCost());
		player.getInventory().decreaseByItemId(190200000, items.getEvolvedNum());
		minion.setMinionId(minion.getMinionId() + 1);
		minion.setMinionLevel(minion.getMinionLevel() + 1);
		minion.setMinionGrowthPoint(0);
		DAOManager.getDAO(PlayerMinionsDAO.class).evolutionMinion(player, minion);
		PacketSendUtility.sendPacket(player, new SM_MINIONS(1, minion, 1));
		player.getMinionList().updateMinionsList();
	}

	public void deleteMinion(Player player, int minionObjId, boolean isMaterial) {
		MinionCommonData minion = player.getMinionList().getMinion(minionObjId);
		if (minion != null) {
			player.getMinionList().deleteMinion(minion.getObjectId());
			PacketSendUtility.broadcastPacket(player, new SM_MINIONS(2, isMaterial, minion), true);
		} else {
			return;
		}
	}

	public void lockMinion(Player player, int minionObjId, int lock) {
		MinionCommonData minion = player.getMinionList().getMinion(minionObjId);
		if (lock == 1) {
			minion.setLock(true);
			DAOManager.getDAO(PlayerMinionsDAO.class).lockMinions(player, minionObjId, 1);
			PacketSendUtility.broadcastPacket(player, new SM_MINIONS(4, minion), true);
		} else {
			minion.setLock(false);
			DAOManager.getDAO(PlayerMinionsDAO.class).lockMinions(player, minionObjId, 0);
			PacketSendUtility.broadcastPacket(player, new SM_MINIONS(4, minion), true);
		}
	}

	public void renameMinion(Player player, int minionObjId, String name) {
		MinionCommonData minion = player.getMinionList().getMinion(minionObjId);
		minion.setName(name);
		DAOManager.getDAO(PlayerMinionsDAO.class).updateMinionName(minion);
		PacketSendUtility.broadcastPacketAndReceive(player, new SM_MINIONS(3, minion));
	}

	public void addMinionSkillPoints(Player player, boolean charge, boolean autoCharge) {
		int maxSkillPoints = 50000;
		int currentSkillPoints = player.getMinionSkillPoints();
		int skillPointsToAdd = maxSkillPoints - currentSkillPoints;
		int price = skillPointsToAdd * 20;
		if (player.getInventory().getKinah() < price) {
			return;
		}
		if (charge) {
			player.getInventory().decreaseKinah(price);
			player.setMinionSkillPoints(maxSkillPoints);
			PacketSendUtility.sendPacket(player, new SM_MINIONS(11, maxSkillPoints, autoCharge));
		} else {
			PacketSendUtility.sendPacket(player, new SM_MINIONS(11, currentSkillPoints, autoCharge));
		}
	}

	public void activateMinionFunction(Player player) {
		long leftTime = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000); // + 30 Days
		System.out.println("Int Timestamp : " + new Timestamp(leftTime));
		if (player.getInventory().tryDecreaseKinah(25000000)) {
			player.getCommonData().setMinionFunctionTime(new Timestamp(leftTime)); // TODO
			PacketSendUtility.sendPacket(player, new SM_MINIONS(9, leftTime));
			PacketSendUtility.sendPacket(player, new SM_MINIONS(12));
		} else {
			return;
		}
	}

	// TODO
	public void addMinionFunctionItems(Player player, int action, int minionObjectId, int itemId, int targetSlot,
			int destinationSlot) {
		if (player.getMinion() == null) {
			return;
		}
		Minion minions = player.getMinion();
		minions.getCommonData().getDopingBag().setItem(itemId, targetSlot);

		if (minions.getCommonData().getDopingBag().getFoodItem() != 0) {
			System.out.println("Minion Bag food:" + minions.getCommonData().getDopingBag().getFoodItem());
		}
		if (minions.getCommonData().getDopingBag().getDrinkItem() != 0) {
			System.out.println("Minion Bag drink:" + minions.getCommonData().getDopingBag().getDrinkItem());
		}
		for (int a : minions.getCommonData().getDopingBag().getScrollsUsed()) {
			System.out.println("Minion Bag scroll:" + a);
		}
		DAOManager.getDAO(PlayerMinionsDAO.class).saveDopingBag(player, minions.getCommonData(),
				minions.getCommonData().getDopingBag());
		PacketSendUtility.broadcastPacket(player, new SM_MINIONS(8, 0, minionObjectId, itemId, targetSlot, 0), true);
	}

	// TODO
	public void buffPlayer(final Player player, final int minionObjectId, int itemId, final int slot) {
		Minion minion = player.getMinion();
		if (minion == null || minion.getCommonData().getDopingBag() == null) {
			return;
		}
		List<Item> items = player.getInventory().getItemsByItemId(itemId);
		Item useItem = items.get(0);
		ItemActions itemActions = useItem.getItemTemplate().getActions();
		ItemUseLimits limit = new ItemUseLimits();
		int useDelay = player.getItemCooldown(useItem.getItemTemplate()) / 3;
		if (useDelay < 3000) {
			useDelay = 3000;
		}
		limit.setDelayId(useItem.getItemTemplate().getUseLimits().getDelayId());
		limit.setDelayTime(useDelay);
		if (player.isItemUseDisabled(limit)) {
			final int useItemId = itemId;
			ThreadPoolManager.getInstance().schedule(new Runnable() {
				@Override
				public void run() {
					PacketSendUtility.broadcastPacket(player, new SM_MINIONS(8, 0, minionObjectId, useItemId, slot, 0),
							true);
				}
			}, useDelay);
			return;
		}
		if (!RestrictionsManager.canUseItem(player, useItem) || player.isProtectionActive()) {
			player.addItemCoolDown(limit.getDelayId(), System.currentTimeMillis() + useDelay, useDelay / 1000);
		} else {
			player.getController().cancelCurrentSkill();
			for (AbstractItemAction itemAction : itemActions.getItemActions()) {
				if (itemAction.canAct(player, useItem, null)) {
					itemAction.act(player, useItem, null);
				}
			}
		}
		PacketSendUtility.broadcastPacket(player, new SM_MINIONS(8, 0, minionObjectId, itemId, slot, 0), true);
		itemId = minion.getCommonData().getDopingBag().getFoodItem();
		long totalDopes = player.getInventory().getItemCountByItemId(itemId);
		itemId = minion.getCommonData().getDopingBag().getDrinkItem();
		totalDopes += player.getInventory().getItemCountByItemId(itemId);
		final int[] scrollBag = minion.getCommonData().getDopingBag().getScrollsUsed();
		for (int i = 0; i < scrollBag.length; ++i) {
			if (scrollBag[i] != 0) {
				totalDopes += player.getInventory().getItemCountByItemId(scrollBag[i]);
			}
		}
		if (totalDopes == 0L) {
			minion.getCommonData().setIsBuffing(false);
			PacketSendUtility.broadcastPacket(player, new SM_MINIONS(8, 0, minionObjectId, itemId, slot, 0), true);
		}
	}

	// TODO
	public void relocateDoping(Player player, int minionObjectId, int targetSlot, int destinationSlot) {
		MinionCommonData minions = player.getMinionList().getMinion(minionObjectId);
		if (minions == null || minions.getDopingBag() == null) {
			return;
		}
		int[] scrollBag = minions.getDopingBag().getScrollsUsed();
		int targetItem = scrollBag[targetSlot - 2];
		if (destinationSlot - 2 > scrollBag.length - 1) {
			minions.getDopingBag().setItem(targetItem, destinationSlot);
			PacketSendUtility.broadcastPacket(player,
					new SM_MINIONS(8, 0, minionObjectId, targetItem, targetSlot, destinationSlot), true);
			minions.getDopingBag().setItem(0, targetSlot);
			PacketSendUtility.broadcastPacket(player, new SM_MINIONS(8, 0, minionObjectId, targetItem, targetSlot, 0),
					true);
		} else {
			minions.getDopingBag().setItem(scrollBag[destinationSlot - 2], targetSlot);
			PacketSendUtility.broadcastPacket(player,
					new SM_MINIONS(8, 0, minionObjectId, scrollBag[destinationSlot - 2], targetSlot, 0), true);
			minions.getDopingBag().setItem(targetItem, destinationSlot);
			PacketSendUtility.broadcastPacket(player,
					new SM_MINIONS(8, 0, minionObjectId, targetItem, 0, destinationSlot), true);
		}
	}

	public void activateLoot(Player player, boolean activate) {
		Minion minion = player.getMinion();
		if (minion == null) {
			return;
		}
		if (!minion.getCommonData().isLooting()) {
			if (activate) {
				if (player.isInTeam()) {
					LootRuleType lootType = player.getLootGroupRules().getLootRule();
					if (lootType == LootRuleType.FREEFORALL) {
						PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LOOTING_PET_MESSAGE03);
						return;
					}
				}
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LOOTING_PET_MESSAGE01);
			}
			minion.getCommonData().setIsLooting(true);
			PacketSendUtility.sendPacket(player, new SM_MINIONS(8, 1, 0, true));
		} else {
			minion.getCommonData().setIsLooting(false);
			PacketSendUtility.sendPacket(player, new SM_MINIONS(8, 1, 0, false));
		}
	}

	public void CombinationMinion(Player player, List<Integer> minionObjIds) {
		log.debug("Minion Combination");

		if (player == null) {
			log.error("CRITICAL ERROR: Player is null!");
			return;
		}
		if (player.getInventory() == null) {
			log.error("CRITICAL ERROR: Player's inventory is null!");
			return;
		}

		long kinah = player.getInventory().getKinah();

		if (kinah < 50000) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1404348, new Object[0]));
			return;
		}

		player.getInventory().decreaseKinah(50000);

		log.debug("MinionObjIds size: " + minionObjIds.size());
		if (minionObjIds.isEmpty()) {
			log.debug("CRITICAL ERROR: MinionObjIds is empty!  Cannot combine minions.");
			return;
		}

		MinionCommonData minion = null;
		int point = 0;
		int level = 0;
		int totalGrowthPoint = 0;
		int validMinionCount = 0;

		for (int minions : minionObjIds) {
			log.debug("Processing minion with ObjId: " + minions);
			minion = player.getMinionList().getMinion(minions);
			if (minion == null) {
				log.debug("CRITICAL ERROR: player.getMinionList().getMinion(" + minions + ") returned null! Skipping this minion.");
				continue;
			}
			point += minion.getMinionGrowthPoint();
			level += minion.getMinionLevel();
			totalGrowthPoint += minion.getMinionGrowthPoint();
			validMinionCount++;
		}

		if (point == 0 && level == 0) {
			log.debug("CRITICAL ERROR: No valid minions found in MinionObjIds! Combination failed.");
			return;
		}

		int averageGrowthPoint = 0;
		if (validMinionCount > 0) {
			averageGrowthPoint = totalGrowthPoint / validMinionCount;
		} else {
			log.debug("CRITICAL ERROR: No valid minions found. Setting averageGrowthPoint to 0.");
		}

		int minionId = 0;
		String name = "";
		String grade = "";
		int levelNewMinion = 0;

		if (minionObjIds.isEmpty()) {
			log.debug("CRITICAL ERROR: MinionObjIds is empty! Cannot get grade.");
			return;
		}

		if (player.getMinionList().getMinion(minionObjIds.get(0)) == null){
			log.debug("CRITICAL ERROR: player.getMinionList().getMinion(" + minionObjIds.get(0) + ") is null!");
			return;
		}
		grade = player.getMinionList().getMinion(minionObjIds.get(0)).getMinionGrade();
		log.debug("Grade of first minion: " + grade); // Проверяем значение

		int rnd = 0;
		if (level > 0) {
			rnd = Rnd.get(0, 200) + ((point / level) / 1000) + (level / 4);
		} else { 
			log.debug("CRITICAL ERROR: Level is zero!  Setting rnd to 0.");
		}
		
		log.debug("Rnd: " + rnd);

		boolean result;
		if (rnd < 125) {
			result = false;
			rnd = Rnd.get(0, 3);
			log.warn("Combination failed. Using grade: " + grade + " and rnd: " + rnd + " to determine minionId.");
			switch (grade) {
				case "D":
					minionId = 980010;
					break;
				case "C":
					minionId = player.getMinionList().getMinion(minionObjIds.get(rnd)).getMinionId();
					break;
				case "B":
					minionId = player.getMinionList().getMinion(minionObjIds.get(rnd)).getMinionId();
					break;
				default:
					log.warn("WARNING: Unknown grade: " + grade + ".  Using default minionId 0.");
					minionId = 0;
					break;
			}
		} else {
			result = true;
			log.warn("Combination succeeded. Using grade: " + grade + " to determine minionId.");
			switch (grade) {
				case "D":
					minionId = minionId(Rnd.get(36, 420));
					break;
				case "C":
					if (player.getMinionList().getMinion(minionObjIds.get(0)).getMinionId() == 980011) {
						minionId = minionId(Rnd.get(141, 700));
					} else {
						minionId = minionId(Rnd.get(421, 700));
					}
					break;
				case "B":
					if (player.getMinionList().getMinion(minionObjIds.get(0)).getMinionId() == 980012) {
						minionId = minionId(Rnd.get(421, 980));
					} else {
						minionId = minionId(Rnd.get(701, 980));
					}
					break;
				default:
					log.warn("WARNING: Unknown grade: " + grade + ".  Using default minionId 0.");
					minionId = 0;
					break;
			}
		}

		if (player.getAccessLevel() > 5) {
			PacketSendUtility.sendMessage(player, (result ? "Success" : "Fail") + " Rnd:" + rnd + " Luck:" + 125);
		}

		MinionTemplate minionTemplate = DataManager.MINION_DATA.getMinionTemplate(minionId);
		if (minionTemplate == null) {
			return;
		}

		grade = minionTemplate.getGrade();
		levelNewMinion = minionTemplate.getLevel();
		name = minionTemplate.getName();
		
		// Логируес код в консоль
		log.info("Creating new minion with ID: " + minionId + ", Name: " + name + ", Grade: " + grade + ", Level: " + levelNewMinion);
		MinionCommonData addNewMinion = player.getMinionList().addNewMinion(player, minionId, name, grade, levelNewMinion, averageGrowthPoint);
		if (addNewMinion == null) {
			return;
		}

		PacketSendUtility.sendPacket(player, new SM_MINIONS(1, addNewMinion, (result ? 2 : 3)));

		for (int minionObjId : minionObjIds) {
			deleteMinion(player, minionObjId, true);
		}
		minionObjIds.clear();
		player.getMinionList().updateMinionsList();
	}

	// Нужно перепроверить, чтобы тут были все миньоны
	private static int minionId(int rnd) {
		if (rnd <= 35) {
			return 980010; // Kerubar D (3.6%)
		} else if (rnd <= 70) {
			return 980011; // Kerubian C (3.5%)
		} else if (rnd <= 105) {
			return 980012; // Kerubiel B (3.5%)
		} else if (rnd == 106) { // Arch Kerubiel A (0.1%)
			return 980013;
		} else if (rnd <= 141) {
			return 980020; // Seiren D (3.5%)
		} else if (rnd <= 176) {
			return 980021; // Seiren C (3.5%)
		} else if (rnd <= 211) {
			return 980022; // Seiren B (3.5%)
		} else if (rnd == 212) { // Seiren A (0.1%)
			return 980023;
		} else if (rnd <= 247) {
			return 980030; // Steel Rose D (3.5%)
		} else if (rnd <= 282) {
			return 980031; // Steel Rose C (3.5%)
		} else if (rnd <= 317) {
			return 980032; // Steel Rose B (3.5%)
		} else if (rnd == 318) { // Steel Rose A (0.1%)
			return 980033;
		} else if (rnd <= 353) {
			return 980040; // Abija D (3.5%)
		} else if (rnd <= 388) {
			return 980041; // Abija C (3.5%)
		} else if (rnd <= 423) {
			return 980042; // Abija B (3.5%)
		} else if (rnd == 424) { // Abija A (0.1%)
			return 980043;
		} else if (rnd <= 459) {
			return 980050; // Hamerun D (3.5%)
		} else if (rnd <= 494) {
			return 980051; // Hamerun C (3.5%)
		} else if (rnd <= 529) {
			return 980052; // Hamerun B (3.5%)
		} else if (rnd == 530) { // Hamerun A (0.1%)
			return 980053;
		} else if (rnd <= 565) {
			return 980060; // Grendal D (3.5%)
		} else if (rnd <= 600) {
			return 980061; // Grendal C (3.5%)
		} else if (rnd <= 635) {
			return 980062; // Grendal B (3.5%)
		} else if (rnd == 636) { // Grendal A (0.1%)
			return 980063;
		} else if (rnd <= 671) {
			return 980070; // Sita D (3.5%)
		} else if (rnd <= 706) {
			return 980071; // Sita C (3.5%)
		} else if (rnd <= 741) {
			return 980072; // Sita B (3.5%)
		} else if (rnd == 742) { // Sita A (0.1%)
			return 980073;
		} else {
			return 980010; // Другое
		}
	}

	public static MinionService getInstance() {
		return SingletonHolder.instance;
	}

	private static class SingletonHolder {

		protected static final MinionService instance = new MinionService();
	}
}