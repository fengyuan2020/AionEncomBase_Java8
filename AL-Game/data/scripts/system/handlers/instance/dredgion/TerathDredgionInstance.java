/*
 * This file is part of Encom.
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
package instance.dredgion;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.configs.main.GroupConfig;
import com.aionemu.gameserver.configs.main.RateConfig;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.actions.PlayerActions;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.DredgionReward;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.playerreward.DredgionPlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.InstancePlayerReward;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.AutoGroupService;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import javolution.util.FastList;
import org.apache.commons.lang.mutable.MutableInt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author (Encom)
 * @rework MATTY
**/

@InstanceID(300440000)
public class TerathDredgionInstance extends GeneralInstanceHandler
{
	private int bulkhead;
	private int secretCache;
	private int surkanaKills;
	private long instanceTime;
	private Map<Integer, StaticDoor> doors;
	protected DredgionReward dredgionReward;
	private float loosingGroupMultiplier = 1;
	private boolean isInstanceDestroyed = false;
	protected AtomicBoolean isInstanceStarted = new AtomicBoolean(false);
	private final FastList<Future<?>> terathTask = FastList.newInstance();
	
	protected DredgionPlayerReward getPlayerReward(Player player) {
		Integer object = player.getObjectId();
		if (dredgionReward.getPlayerReward(object) == null) {
			addPlayerToReward(player);
		}
		return (DredgionPlayerReward) dredgionReward.getPlayerReward(object);
	}
	
	protected void captureRoom(Race race, int roomId) {
		dredgionReward.getDredgionRoomById(roomId).captureRoom(race);
	}
	
	private void addPlayerToReward(Player player) {
		dredgionReward.addPlayerReward(new DredgionPlayerReward(player.getObjectId()));
	}
	
	private boolean containPlayer(Integer object) {
		return dredgionReward.containPlayer(object);
	}
	
	public void onDropRegistered(Npc npc) {
		Set<DropItem> dropItems = DropRegistrationService.getInstance().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		int index = dropItems.size() + 1;
		switch (npcId) {
			case 219264: //Captain Anusa.
				for (Player player: instance.getPlayersInside()) {
					if (player.isOnline()) {
						dropItems.add(DropRegistrationService.getInstance().regDropItem(index++, player.getObjectId(), npcId, 188053789, 1)); //Major Stigma Support Bundle.
						dropItems.add(DropRegistrationService.getInstance().regDropItem(index++, player.getObjectId(), npcId, 188052582, 1)); //Dragon's Conquerer Mark Box.
						dropItems.add(DropRegistrationService.getInstance().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //Tempering Solution Chest.
					}
				}
			break;
			case 219255: //Supervisor Chitan.
			case 219256: //Chief Navigator Adhari.
			case 219257: //Assistant Navigator Kurta.
			case 219258: //Vice Gun Captain Faniran.
			case 219259: //Gun Captain Pahala.
			case 219260: //Vice Air Captain Lukar.
			case 219261: //Air Captain Misalus.
			case 219262: //Engineer Tapisha.
			case 219263: //First Mate Kamital.
			case 219265: //Bosun Kuchuran.
			case 219267: //Supply Captain Marahane.
			case 219268: //Quartermaster Gashar.
			case 219270: //Enforcer Udara.
			case 219286: //Auditor Zhantri.
				for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(DropRegistrationService.getInstance().regDropItem(index++, player.getObjectId(), npcId, 188052582, 1)); //Dragon's Conquerer Mark Box.
						dropItems.add(DropRegistrationService.getInstance().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //Tempering Solution Chest.
					}
				}
			break;
			case 219266: //Archivist Davorkar.
			case 219271: //Master At Arms Vandukar.
				for (Player player: instance.getPlayersInside()) {
				    dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 123001270, 1)); //Ksanat's Belt.
					if (player.isOnline()) {
						dropItems.add(DropRegistrationService.getInstance().regDropItem(index++, player.getObjectId(), npcId, 188052582, 1)); //Dragon's Conquerer Mark Box.
						dropItems.add(DropRegistrationService.getInstance().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //Tempering Solution Chest.
					}
				}
			break;
		   /**
			* Obtain the Captain’s Key by killing Gatekeeper Payad.
			* The Captain’s Key opens the door to the Captain’s Cabin.
			*/
			case 219269: //Gatekeeper Payad.
				for (Player player: instance.getPlayersInside()) {
				    dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 185000117, 1)); //Captain's Cabin Passage Key.
					dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, 185000189, 1)); //Secret Cache Key.
					if (player.isOnline()) {
						dropItems.add(DropRegistrationService.getInstance().regDropItem(index++, player.getObjectId(), npcId, 188052582, 1)); //Dragon's Conquerer Mark Box.
						dropItems.add(DropRegistrationService.getInstance().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //Tempering Solution Chest.
					}
				}
			break;
		}
	}
	
	private void onDieSurkan(Npc npc, Player mostPlayerDamage, int points) {
		Race race = mostPlayerDamage.getRace();
		captureRoom(race, npc.getNpcId() + 14 - 701454); //Cabin Power Surkana.
		for (Player player: instance.getPlayersInside()) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400199, new DescriptionId(race.equals(Race.ASMODIANS) ? 1800483 : 1800481), new DescriptionId(npc.getObjectTemplate().getNameId() * 2 + 1)));
		} if (++surkanaKills == 5) {
            //Captain Anusa has appeared in the Captain's Cabin.
			sendMsgByRace(1401416, Race.PC_ALL, 0);
			spawn(219264, 485.47916f, 812.4957f, 416.68475f, (byte) 31); //Captain Anusa.
        }
		getPlayerReward(mostPlayerDamage).captureZone();
		updateScore(mostPlayerDamage, npc, points, false);
		npc.getController().onDelete();
	}
	
	protected void startInstanceTask() {
		instanceTime = System.currentTimeMillis();
		terathTask.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				openFirstDoors();
				//The bulkhead has been activated and the passage between the First Armory and Gravity Control has been sealed.
				sendMsgByRace(1400604, Race.PC_ALL, 5000);
				//The bulkhead has been activated and the passage between the Second Armory and Gravity Control has been sealed.
				sendMsgByRace(1400605, Race.PC_ALL, 10000);
				dredgionReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
				sendPacket();
				switch (Rnd.get(1, 2)) {
					case 1:
					    spawn(219255, 415.2769f, 282.0216f, 409.7311f, (byte) 118); //Supervisor Chitan.
					break;
					case 2:
					    spawn(219255, 556.53534f, 279.2918f, 409.7311f, (byte) 33); //Supervisor Chitan.
					break;
				} switch (Rnd.get(1, 2)) {
					case 1:
						spawn(219263, 485.25455f, 877.04614f, 405.01407f, (byte) 90); //First Mate Kamital.
					break;
					case 2:
					    spawn(219286, 485.25455f, 877.04614f, 405.01407f, (byte) 90); //Auditor Zhantri.
					break;
				}
			}
		}, 60000));
	   /**
		* Terath Dredgion Teleportation Devices:
		* There are numerous teleportation devices located inside the Terath Dredgion.
		* These teleportation devices allow players to teleport to different areas of the Dredgion with ease.
		* Central Teleporter: This teleporter activates 10 minutes after the Instanced Dungeon has begun.
		*/
		terathTask.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				//A teleport device has been activated in the Emergency Exit.
				sendMsgByRace(1401424, Race.PC_ALL, 0);
				spawn(730558, 415.07663f, 173.85265f, 432.53436f, (byte) 0, 34); //Port Midship Teleporter.
				spawn(730559, 554.83081f, 173.87158f, 432.52448f, (byte) 0, 9); //Starboard Midship Teleporter.
			}
		}, 600000));
		/**
		* Enforcer Udara:
		* Location: Gravity Control
		* Time Elapsed: 15 Minutes
		* Valor: 1,000 Points
		*/
		terathTask.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				//Enforcer Udara has appeared in the Gravity Control Room.
				sendMsgByRace(1401417, Race.PC_ALL, 0);
				spawn(219270, 485.4811f, 313.925f, 403.71857f, (byte) 36); //Enforcer Udara.
			}
		}, 900000));
		terathTask.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				if (!dredgionReward.isRewarded()) {
					Race winningRace = dredgionReward.getWinningRaceByScore();
					stopInstance(winningRace);
				}
			}
		}, 3600000));
	}
	
	@Override
	public void onDie(Npc npc) {
		int point = 0;
		Player mostPlayerDamage = npc.getAggroList().getMostPlayerDamage();
        if (mostPlayerDamage == null) {
            return;
        }
		Race race = mostPlayerDamage.getRace();
		switch (npc.getObjectTemplate().getTemplateId()) {
		   /**
			* There are six weapons locker located near the Terath Dredgion entrance, and each chest awards 100 points if destroyed. 
			* These locker are also related to Quests for both Elyos and Asmodians. 
			*/
		    case 701439: //Weapons Locker.
				point = 100;
				despawnNpc(npc);
            break;
		   /**
			* The Surkana:
			* Destroy Surkana in each room can obtain a higher score.
			* 2. When you add monsters to attack Surkana is around 20m range. First, it is safe to be cleaned up monsters.
			* 3. When you destroy a race that destroyed Surkana is displayed on the map. It is through you can guess the path of the opposing faction.
			* 4. Captain Room Teleport appeared to be destroyed 5 Surkana.
			*/
			case 701441: //Armory Maintenance Surkana.
			case 701442: //Armory Maintenance Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage, 500);
			break;
			case 701443: //Gravity Control Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage, 900);
			break;
			case 701444: //Port Thrust Control Surkana.
			case 701445: //Starboard Thrust Control Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage, 1100);
			break;
			case 701446: //Cannon Control Surkana.
			case 701447: //Cannon Control Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage, 800);
			break;
			case 701448: //Drop Authority Surkana.
			case 701449: //Drop Authority Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage, 600);
			break;
			case 701450: //Weapons Charge Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage, 700);
			break;
			case 701451: //Flywheel Surkana.
			case 701452: //Flywheel Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage, 500);
			break;
			case 701453: //Bridge Power Surkana .
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage, 700);
			break;
			case 701454: //Cabin Power Surkana.
				despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage, 1100);
			break;
		   /**
			* Captain’s Cabin Passage:
			* There are paths to the left and right of the Captain’s Cabin’s on the second floor, but the doors are blocked.
			* These doors cannot be demolished, and can only be opened with a key dropped by a specific Named Monster.
			* Groups desiring the Captain’s Cabin Passage Key will need to defeat "Master At Arms Vandukar" in the center of the Dredgion.
			* Only one Group can loot the key.
			* The Captain’s Cabin Teleport Device is located just beyond the Barracks, and can make reaching Captain Anusa much easier.
			*/
			case 219271: //Master At Arms Vandukar.
				if (race.equals(Race.ELYOS)) {
				   //A teleport device has been activated in the Captain's Cabin.
				   sendMsgByRace(1401419, Race.ELYOS, 0);
				   spawn(730562, 473.62231f, 761.99506f, 388.66f, (byte) 0, 33); //Elyos Captain's Cabin Teleporter.
				} else if (race.equals(Race.ASMODIANS)) {
				   //A teleport device has been activated in the Captain's Cabin.
				   sendMsgByRace(1401419, Race.ASMODIANS, 0);
				   spawn(730563, 496.52225f, 761.99506f, 388.66f, (byte) 0, 186); //Asmodian Captain's Cabin Teleporter.
				}
				point = 1000;
            break;
		   /**
			* Supply Room Teleporter:
			* This teleporter activates after the destruction of the Teleporter Generator in the Barracks.
			*/
			case 730570: //Port Teleporter Generator.
                despawnNpc(npc);
				//A teleport device has been activated in the Supplies Storage Room.
				sendMsgByRace(1401415, Race.PC_ALL, 0);
				spawn(730560, 397.11661f, 184.29782f, 432.8032f, (byte) 0, 42); //Port Supply Room Teleporter.
            break;
			case 730571: //Starboard Teleporter Generator.
                despawnNpc(npc);
				//A second teleport device has been activated in the Supplies Storage room.
				sendMsgByRace(1401418, Race.PC_ALL, 0);
				spawn(730561, 572.10443f, 185.23933f, 432.56024f, (byte) 0, 10); //Starboard Supply Room Teleporter.
            break;
		   /**
			* Defense Shield Generator:
			* When the Defense Shield Generator on the Weapons Deck or Lower Weapons deck is demolished, a shield appears in Ready Room 1 or 2.
			* This shield blocks access to the center of the Terath Dredgion.
			* The Ready Room is the shortest route to the center of the Dredgion, and the quickest route to the opposing race’s area.
			* Different tactics can be used in this area to maximize the Group’s accumulation of points.
			* For example, if one Group decides to destroy the opposing Group’s Shield Generator, it will make it difficult for the opposing Group to reach the center of the Dredgion.
			* In some cases, it might wiser for one Group to destroy their own Defense Shield Generator, and delay engagement with the opposing race in order to accumulate more points.
			*/
			case 730566: //Portside Defense Shield.
			case 730567: //Starboard Defense Shield.
				despawnNpc(npc);
			break;
			case 730572: //Portside Defense Shield Generator.
				despawnNpc(npc);
				//The Portside Defense Shield has been generated in Ready Room 1.
				sendMsgByRace(1400226, Race.PC_ALL, 0);
				spawn(730566, 448.39151f, 493.64182f, 394.13174f, (byte) 0, 12);  // spawn barier
			break;
			case 730573: //Starboard Defense Shield Generator.
				despawnNpc(npc);
				//The Starboard Defense Shield has been generated in Ready Room 2.
				sendMsgByRace(1400227, Race.PC_ALL, 0);
				spawn(730567, 520.87555f, 493.40115f, 394.43292f, (byte) 0, 133);  // spawn barier
			break;
		   /**
			* The Bulkhead:
			* These shields are activated by the Terath Sentinel when first encountered at the beginning of the battle.
			* These shields block the entrance from the Armories to Gravity Control, and can be demolished with attacks, but also have a significant amount of health.
			* Groups often opt to move around the shields instead of demolishing them.
			* It’s worth noting that after a certain amount of time has passed, Enforcer Udara spawns in the Gravity Control room, and gives 1,000 points when defeated.
			* There is also a chance that Bosun Kuchuran, a Hero grade Named Monster, will spawn.
			* Bosun Kuchuran has a chance to drop Fabled and Heroic accessories. 
			*/
			case 730574: //Port Bulkhead.
			case 730575: //Starboard Bulkhead.
				bulkhead++;
				if (bulkhead == 2) {
					spawn(219265, 456.3946f, 319.65912f, 402.69315f, (byte) 28); //Bosun Kuchuran.
				}
				despawnNpc(npc);
			break;
			case 219256: //Chief Navigator Adhari.
			case 219257: //Assistant Navigator Kurta.
			case 219258: //Vice Gun Captain Faniran.
			case 219259: //Gun Captain Pahala.
			case 219260: //Vice Air Captain Lukar.
			case 219261: //Air Captain Misalus.
			case 219262: //Engineer Tapisha.
			case 219266: //Archivist Davorkar.
			case 219267: //Supply Captain Marahane.
			case 219268: //Quartermaster Gashar.
			    secretCache++;
				if (secretCache == 6) {
				    //A Dredgion Treasure Chest has appeared in the Drop Zone!
					sendMsgByRace(1401421, Race.PC_ALL, 0);
					spawn(701455, 482.82455f, 496.16556f, 397.28323f, (byte) 92); //Dredgion Opportunity Bundle.
				}
				point = 200;
            break;
			case 219263: //First Mate Kamital.
			case 219286: //Auditor Zhantri.
			    point = 500;
            break;
			case 219255: //Supervisor Chitan.
			case 219265: //Bosun Kuchuran.
			case 219269: //Gatekeeper Payad.
			case 219270: //Enforcer Udara.
				point = 1000;
			break;
			case 219264: //Captain Anusa.
				point = 1000;
				ThreadPoolManager.getInstance().schedule(new Runnable() {
				    @Override
					public void run() {
						if (!dredgionReward.isRewarded()) {
							Race winningRace = dredgionReward.getWinningRaceByScore();
							stopInstance(winningRace);
						}
					}
				}, 30000);
			break;
		}
		updateScore(mostPlayerDamage, npc, point, false);
	}
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
	
	protected void openFirstDoors() {
		openDoor(4);
		openDoor(173);
	}
	
	@Override
	public void onEnterInstance(final Player player) {
		if (!containPlayer(player.getObjectId())) {
			addPlayerToReward(player);
		}
		sendPacket();
	}
	
	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		dredgionReward = new DredgionReward(mapId, instanceId);
		dredgionReward.setInstanceScoreType(InstanceScoreType.PREPARING);
		doors = instance.getDoors();
		startInstanceTask();
	}
	
	protected void stopInstance(Race race) {
		stopInstanceTask();
		dredgionReward.setWinningRace(race);
		dredgionReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		doReward();
		sendPacket();
	}
	
	public void doReward() {
		for (Player player : instance.getPlayersInside()) {
			InstancePlayerReward playerReward = getPlayerReward(player);
			float abyssPoint = playerReward.getPoints() * RateConfig.DREDGION_REWARD_RATE;
			if (player.getRace().equals(dredgionReward.getWinningRace())) {
				abyssPoint += dredgionReward.getWinnerPoints();
			} else {
				abyssPoint += dredgionReward.getLooserPoints();
			}
			AbyssPointsService.addAp(player, (int) abyssPoint);
			QuestEnv env = new QuestEnv(null, player, 0, 0);
			QuestEngine.getInstance().onDredgionReward(env);
		}
		for (Npc npc : instance.getNpcs()) {
			npc.getController().onDelete();
		}
		ThreadPoolManager.getInstance().schedule(new Runnable() {
			@Override
			public void run() {
				if (!isInstanceDestroyed) {
					for (Player player : instance.getPlayersInside()) {
						if (PlayerActions.isAlreadyDead(player)) {
							PlayerReviveService.duelRevive(player);
						}
						onExitInstance(player);
					}
					AutoGroupService.getInstance().unRegisterInstance(instanceId);
				}
			}
		}, 120000);
	}
	
	private int getTime() {
		long result = System.currentTimeMillis() - instanceTime;
		if (result < 60000) {
			return (int) (60000 - result);
		} else if (result < 3600000) {
			return (int) (3600000 - (result - 60000));
		}
		return 0;
	}
	
	@Override
    public boolean onReviveEvent(Player player) {
		player.getGameStats().updateStatsAndSpeedVisually();
		PlayerReviveService.revive(player, 100, 100, false, 0);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
		PacketSendUtility.sendPacket(player, new SM_QUESTION_WINDOW(SM_QUESTION_WINDOW.STR_INSTANT_DUNGEON_RESURRECT, 0, 0));
        dredgionReward.portToPosition(player);
		return true;
    }
	
	@Override
	public boolean onDie(Player player, Creature lastAttacker) {
		int points = 60;
		PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.DIE, 0, player.equals(lastAttacker) ? 0 : lastAttacker.getObjectId()), true);
        PacketSendUtility.sendPacket(player, new SM_DIE(player.haveSelfRezEffect(), false, 0, 8));
		if (lastAttacker instanceof Player) {
			if (lastAttacker.getRace() != player.getRace()) {
				InstancePlayerReward playerReward = getPlayerReward(player);
				if (getPointsByRace(lastAttacker.getRace()).compareTo(getPointsByRace(player.getRace())) < 0) {
					points *= loosingGroupMultiplier;
				} else if (loosingGroupMultiplier == 10 || playerReward.getPoints() == 0) {
					points = 0;
				}
			    updateScore((Player) lastAttacker, player, points, true);
			}
		}
		updateScore(player, player, -points, false);
		return true;
	}
	
	private MutableInt getPointsByRace(Race race) {
		return dredgionReward.getPointsByRace(race);
	}
	
	private void addPointsByRace(Race race, int points) {
		dredgionReward.addPointsByRace(race, points);
	}
	
	private void addPointToPlayer(Player player, int points) {
		getPlayerReward(player).addPoints(points);
	}
	
	private void addPvPKillToPlayer(Player player) {
		getPlayerReward(player).addPvPKillToPlayer();
	}
	
	private void addBalaurKillToPlayer(Player player) {
		getPlayerReward(player).addMonsterKillToPlayer();
	}
	
	protected void updateScore(Player player, Creature target, int points, boolean pvpKill) {
		if (points == 0) {
			return;
		}
		addPointsByRace(player.getRace(), points);
		List<Player> playersToGainScore = new ArrayList<Player>();
		if (target != null && player.isInGroup2()) {
			for (Player member : player.getPlayerGroup2().getOnlineMembers()) {
				if (member.getLifeStats().isAlreadyDead()) {
					continue;
				} if (MathUtil.isIn3dRange(member, target, GroupConfig.GROUP_MAX_DISTANCE)) {
					playersToGainScore.add(member);
				}
			}
		} else {
			playersToGainScore.add(player);
		}
		for (Player playerToGainScore : playersToGainScore) {
			addPointToPlayer(playerToGainScore, points / playersToGainScore.size());
			if (target instanceof Npc) {
				PacketSendUtility.sendPacket(playerToGainScore, new SM_SYSTEM_MESSAGE(1400237, new DescriptionId(((Npc) target).getObjectTemplate().getNameId() * 2 + 1), points));
			} else if (target instanceof Player) {
				PacketSendUtility.sendPacket(playerToGainScore, new SM_SYSTEM_MESSAGE(1400237, target.getName(), points));
			}
		}
		int pointDifference = getPointsByRace(Race.ASMODIANS).intValue() - (getPointsByRace(Race.ELYOS)).intValue();
		if (pointDifference < 0) {
			pointDifference *= -1;
		} if (pointDifference >= 3000) {
			loosingGroupMultiplier = 10;
		} else if (pointDifference >= 1000) {
			loosingGroupMultiplier = 1.5f;
		} else {
			loosingGroupMultiplier = 1;
		} if (pvpKill && points > 0) {
			addPvPKillToPlayer(player);
		} else if (target instanceof Npc && ((Npc) target).getRace().equals(Race.DRAKAN)) {
			addBalaurKillToPlayer(player);
		}
		sendPacket();
	}
	
	@Override
	public void onInstanceDestroy() {
		isInstanceDestroyed = true;
		dredgionReward.clear();
		stopInstanceTask();
		doors.clear();
	}
	
	protected void openDoor(int doorId) {
		StaticDoor door = doors.get(doorId);
		if (door != null) {
			door.setOpen(true);
		}
	}
	
	private void sendPacket() {
		instance.doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(getTime(), dredgionReward, instance.getPlayersInside()));
			}
		});
	}
	
	protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time) {
        sp(npcId, x, y, z, h, 0, time, 0, null);
    }
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final int msg, final Race race) {
        sp(npcId, x, y, z, h, 0, time, msg, race);
    }
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int entityId, final int time, final int msg, final Race race) {
        terathTask.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                if (!isInstanceDestroyed) {
                    spawn(npcId, x, y, z, h, entityId);
                    if (msg > 0) {
                        sendMsgByRace(msg, race, 0);
                    }
                }
            }
        }, time));
    }
	
    protected void sp(final int npcId, final float x, final float y, final float z, final byte h, final int time, final String walkerId) {
        terathTask.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                if (!isInstanceDestroyed) {
                    Npc npc = (Npc) spawn(npcId, x, y, z, h);
                    npc.getSpawn().setWalkerId(walkerId);
                    WalkManager.startWalking((NpcAI2) npc.getAi2());
                }
            }
        }, time));
    }
	
    protected void sendMsgByRace(final int msg, final Race race, int time) {
        terathTask.add(ThreadPoolManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                instance.doOnAllPlayers(new Visitor<Player>() {
                    @Override
                    public void visit(Player player) {
                        if (player.getRace().equals(race) || race.equals(Race.PC_ALL)) {
                            PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(msg));
                        }
                    }
                });
            }
        }, time));
    }
	
	private void sendMsg(final String str) {
		instance.doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendWhiteMessageOnCenter(player, str);
			}
		});
	}
	
	private void stopInstanceTask() {
        for (FastList.Node<Future<?>> n = terathTask.head(), end = terathTask.tail(); (n = n.getNext()) != end; ) {
            if (n.getValue() != null) {
                n.getValue().cancel(true);
            }
        }
    }
	
	@Override
	public InstanceReward<?> getInstanceReward() {
		return dredgionReward;
	}
	
	@Override
    public void onExitInstance(Player player) {
        TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
    }
	
	@Override
    public void onLeaveInstance(Player player) {
        stopInstanceTask();
		//"Player Name" has left the battle.
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400255, player.getName()));
        if (player.isInGroup2()) {
            PlayerGroupService.removePlayer(player);
        }
    }
}