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
package admincommands;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CUSTOM_PACKET;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CUSTOM_PACKET.PacketElementType;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.utils.PacketSendUtility;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
/**
 * Send packet in raw format.
 * 
 * @author Luno
 * @author Aquanox
 */
public class Raw extends AdminCommand {

	private static final File ROOT = new File("data/packets/");

	private static final Logger logger = LoggerFactory.getLogger(Raw.class);

	public Raw() {
		super("raw");
	}

	@Override
	public void execute(Player admin, String... params) {
		if (params.length != 1) {
			PacketSendUtility.sendMessage(admin, "Usage: //raw [name]");
			return;
		}
	
		File file = new File(ROOT, params[0] + ".txt");
	
		if (!file.exists() || !file.canRead()) {
			PacketSendUtility.sendMessage(admin, "Wrong file selected.");
			return;
		}
	
		try {
			// 使用JDK 8的Files类读取文件 | Using JDK 8's Files class to read the file
			List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
	
			SM_CUSTOM_PACKET packet = null;
			PacketSendUtility.sendMessage(admin, "lines "+lines.size());
			boolean init = false;
			for (int r = 0 ; r< lines.size(); r++){
				String row = lines.get(r);
				String[] tokens = row.substring(0, 48).trim().split(" ");
				int len = tokens.length;
				
				for (int i = 0; i < len; i++) {
					if (!init) {
						if (i == 1){
						packet = new SM_CUSTOM_PACKET(Integer.decode("0x"+tokens[i]+tokens[i-1]));
						init = true;
						}
					}
					else if ( r > 0 || i > 4){
						packet.addElement(PacketElementType.C, "0x" + tokens[i]);
					}
				}
			}
			if (packet != null){
				PacketSendUtility.sendMessage(admin, "Packet send..");
				PacketSendUtility.sendPacket(admin, packet);
			}
		}
		catch (Exception e) {
			PacketSendUtility.sendMessage(admin, "An error has occurred.");
			logger.warn("IO Error.", e);
		}
	}

	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Usage: //raw [name]");
	}
}