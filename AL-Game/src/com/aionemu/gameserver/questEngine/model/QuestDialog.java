/*

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
package com.aionemu.gameserver.questEngine.model;

public enum QuestDialog {
	NULL(0), USE_OBJECT(-1), SELECTED_QUEST_REWARD1(8), SELECTED_QUEST_REWARD2(9), SELECTED_QUEST_REWARD3(10),
	SELECTED_QUEST_REWARD4(11), SELECTED_QUEST_REWARD5(12), SELECTED_QUEST_REWARD6(13), SELECTED_QUEST_REWARD7(14),
	SELECTED_QUEST_REWARD8(15), SELECTED_QUEST_REWARD9(16), SELECTED_QUEST_REWARD10(17), SELECTED_QUEST_REWARD11(18),
	SELECTED_QUEST_REWARD12(19), SELECTED_QUEST_REWARD13(20), SELECTED_QUEST_REWARD14(21), SELECTED_QUEST_REWARD15(22),
	SELECT_NO_REWARD(23), // 4.3
	NO_RIGHTS(27), START_DIALOG(31), // 4.3
	CHECK_COLLECTED_ITEMS(39), // 4.3

	ACCEPT_QUEST(1002), REFUSE_QUEST(1003), REFUSE_QUEST_2(1004), ASK_ACCEPTION(1007), FINISH_DIALOG(1008),
	SELECT_REWARD(1009),

	ACCEPT_QUEST_SIMPLE(20000), REFUSE_QUEST_SIMPLE(20001), CHECK_COLLECTED_ITEMS_SIMPLE(20002), SETPRO_NEXT(20003),
	CHECK_AP(20004), CHECK_GOLD(20005),

	SELECT_ACTION_1011(1011), SELECT_ACTION_1012(1012), SELECT_ACTION_1013(1013), SELECT_ACTION_1014(1014),
	SELECT_ACTION_1097(1097), SELECT_ACTION_1182(1182), SELECT_ACTION_1352(1352), SELECT_ACTION_1353(1353),
	SELECT_ACTION_1354(1354), SELECT_ACTION_1355(1355), SELECT_ACTION_1356(1356), SELECT_ACTION_1375(1375),
    SELECT_ACTION_1396(1396), SELECT_ACTION_1438(1438), SELECT_ACTION_1439(1439), SELECT_ACTION_1609(1609),
    SELECT_ACTION_1693(1693), SELECT_ACTION_1694(1694), SELECT_ACTION_1695(1695), SELECT_ACTION_1696(1696),
    SELECT_ACTION_1697(1697), SELECT_ACTION_1779(1779), SELECT_ACTION_1780(1780), SELECT_ACTION_1864(1864),
    SELECT_ACTION_1865(1865), SELECT_ACTION_1949(1949), SELECT_ACTION_1950(1950), SELECT_ACTION_2034(2034),
    SELECT_ACTION_2035(2035), SELECT_ACTION_2036(2036), SELECT_ACTION_2037(2037), SELECT_ACTION_2038(2038),
    SELECT_ACTION_2292(2292), SELECT_ACTION_2375(2375), SELECT_ACTION_2376(2376), SELECT_ACTION_2377(2377),
    SELECT_ACTION_2378(2378), SELECT_ACTION_2379(2379), SELECT_ACTION_2461(2461), SELECT_ACTION_2546(2546),
    SELECT_ACTION_2716(2716), SELECT_ACTION_2717(2717), SELECT_ACTION_2718(2718), SELECT_ACTION_2720(2720),
    SELECT_ACTION_3058(3058), SELECT_ACTION_3143(3143), SELECT_ACTION_3399(3399), SELECT_ACTION_3400(3400),
    SELECT_ACTION_3739(3739), SELECT_ACTION_3740(3740), SELECT_ACTION_3741(3741), SELECT_ACTION_4081(4081),
    SELECT_ACTION_4166(4166), SELECT_ACTION_4763(4763), SELECT_ACTION_6501(6501), SELECT_ACTION_6503(6503),
    SELECT_ACTION_6842(6842), SELECT_ACTION_6844(6844), SELECT_ACTION_7183(7183), SELECT_ACTION_7524(7524),

	STEP_TO_1(10000), STEP_TO_2(10001), STEP_TO_3(10002), STEP_TO_4(10003), STEP_TO_5(10004), STEP_TO_6(10005),
	STEP_TO_7(10006), STEP_TO_8(10007), STEP_TO_9(10008), STEP_TO_10(10009), STEP_TO_11(10010), STEP_TO_12(10011),
	STEP_TO_13(10012), STEP_TO_14(10013), STEP_TO_15(10014), STEP_TO_16(10015), STEP_TO_17(10016), STEP_TO_18(10017),
	STEP_TO_19(10018), STEP_TO_20(10019), STEP_TO_21(10020), STEP_TO_30(10029), STEP_TO_31(10030), STEP_TO_40(10039),
	STEP_TO_41(10040), SET_REWARD(10255),

	EXCHANGE_COIN(59); // 4.3

	private int id;

	private QuestDialog(int id) {
		this.id = id;
	}

	public int id() {
		return id;
	}
}