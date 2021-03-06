/**
 *	rscplus
 *
 *	This file is part of rscplus.
 *
 *	rscplus is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	rscplus is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with rscplus.  If not, see <http://www.gnu.org/licenses/>.
 *
 *	Authors: see <https://github.com/OrN/rscplus>
 */

package Game;

import Client.Logger;
import Client.Settings;
import Client.TwitchIRC;
import java.applet.Applet;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class Client
{
	public static void init()
	{
		for(int i = 0; i < strings.length; i++)
		{
			if(strings[i].contains("Oh dear! You are dead..."))
			{
				strings[i] = "YOU DIED";
			}
		}

		handler_mouse = new MouseHandler();
		handler_keyboard = new KeyboardHandler();

		Applet applet = Game.getInstance().getApplet();
		applet.addMouseListener(handler_mouse);
		applet.addMouseMotionListener(handler_mouse);
		applet.addMouseWheelListener(handler_mouse);
		applet.addKeyListener(handler_keyboard);
		applet.setFocusTraversalKeysEnabled(false);

		if(Settings.DEBUG)
			dumpStrings();

		// Initialize login
		init_login();

		// Skip first login screen and don't wipe user info
		login_screen = 2;
	}

	public static void update()
	{
		if(state == STATE_GAME)
		{
			// Process XP drops
			boolean dropXP = (xpdrop_state[SKILL_HP] > 0.0f);
			for(int i = 0; i < xpdrop_state.length; i++)
			{
				float xpGain = getXP(i) - xpdrop_state[i];
				xpdrop_state[i] += xpGain;

				if(xpGain > 0.0f && dropXP)
				{
					xpdrop_handler.add("+" + xpGain + " (" + skill_name[i] + ")");

					if(i == SKILL_HP && xpbar.currentSkill != -1)
						continue;

					xpbar.setSkill(i);
				}
			}
		}
	}

	public static void init_login()
	{
		for(int i = 0; i < xpdrop_state.length; i++)
			xpdrop_state[i] = 0.0f;

		Camera.init();
		state = STATE_LOGIN;

		twitch.disconnect();

		setLoginMessage("Please enter your username and password", "");
	}

	public static void init_game()
	{
		Camera.init();
		combat_style = Settings.COMBAT_STYLE;
		state = STATE_GAME;

		if(TwitchIRC.isUsing())
			twitch.connect();

		// Patch game data
		Item.patchItemNames();
	}

	public static String processChatCommand(String line)
	{
		if(TwitchIRC.isUsing() && line.startsWith("/"))
		{
			String message = line.substring(1, line.length());
			String messageArray[] = message.split(" ");

			message = processClientChatCommand(message);

			if(messageArray[0] != null && messageArray[0].equals("me") && messageArray.length > 1)
			{
				message = message.substring(3, message.length());
				twitch.sendEmote(message, true);
			}
			else
			{
				twitch.sendMessage(message, true);
			}
			return "::";
		}

		line = processClientChatCommand(line);
		line = processClientCommand(line);

		return line;
	}

	public static String processPrivateCommand(String line)
	{
		line = processClientChatCommand(line);
		return line;
	}

	private static String processClientCommand(String line)
	{
		if(line.startsWith("::"))
		{
			String commandArray[] = line.substring(2, line.length()).toLowerCase().split(" ");
			String command = commandArray[0];

			if(command.equals("toggleroofs"))
				Settings.toggleHideRoofs();
			else if(command.equals("togglecombat"))
				Settings.toggleCombatMenu();
			else if(command.equals("togglehitbox"))
				Settings.toggleShowHitbox();
			else if(command.equals("togglefatigue"))
				Settings.toggleFatigueAlert();
			else if(command.equals("toggletwitch"))
				Settings.toggleTwitchHide();
			else if(command.equals("toggleplayerinfo"))
				Settings.toggleShowPlayerInfo();
			else if(command.equals("togglenpcinfo"))
				Settings.toggleShowNPCInfo();
			else if(command.equals("toggleiteminfo"))
				Settings.toggleShowItemInfo();
			else if(command.equals("screenshot"))
				Renderer.takeScreenshot();
			else if(command.equals("debug"))
				Settings.toggleDebug();
			else if(command.equals("fov") && commandArray.length > 1)
				Camera.setFoV(Integer.parseInt(commandArray[1]));

			if(commandArray[0] != null)

			return "::";
		}

		return line;
	}

	private static String processClientChatCommand(String line)
	{
		if(line.startsWith("::"))
		{
			String command = line.substring(2, line.length()).toLowerCase();

			if(command.equals("total"))
				return "My Total Level is " + getTotalLevel() + " (" + getTotalXP() + " xp).";

			for(int i = 0; i < 18; i++)
			{
				if(command.equals(skill_name[i].toLowerCase()))
					return "My " + skill_name[i] + " level is " + base_level[i] + " (" + getXP(i) + " xp).";
			}
		}

		return line;
	}

	public static void displayMessage(String message, int chat_type)
	{
		if(Client.state != Client.STATE_GAME || Reflection.displayMessage == null)
			return;

		try
		{
			boolean accessible = Reflection.displayMessage.isAccessible();
			Reflection.displayMessage.setAccessible(true);
			Reflection.displayMessage.invoke(Client.instance, false, null, 0, message, chat_type, 0, null, null);
			Reflection.displayMessage.setAccessible(accessible);
		} catch(Exception e) {}
	}

	public static void setLoginMessage(String line1, String line2)
	{
		if(Reflection.setLoginText == null)
			return;

		try
		{
			boolean accessible = Reflection.setLoginText.isAccessible();
			Reflection.setLoginText.setAccessible(true);
			Reflection.setLoginText.invoke(Client.instance, (byte)-49, line2, line1);
			Reflection.setLoginText.setAccessible(accessible);
		} catch(Exception e) {}
	}

	// All messages added to chat are routed here
	public static void messageHook(String username, String message, int type)
	{
		if(type == 0)
		{
			if(username == null && message != null && message.contains("The spell fails! You may try again in 20 seconds"))
				magic_timer = Renderer.time + 21000L;
		}

		if(type == Client.CHAT_PRIVATE)
		{
			if(username != null)
				lastpm_username = username;
		}

		System.out.println(username + ": " + message + " (" + type + ")");
	}

	public static void drawNPC(int x, int y, int width, int height, String name)
	{
		// ILOAD 6 is index
		npc_list.add(new NPC(x, y, width, height, name, NPC.TYPE_MOB));
	}

	public static void drawPlayer(int x, int y, int width, int height, String name)
	{
		npc_list.add(new NPC(x, y, width, height, name, NPC.TYPE_PLAYER));
	}

	public static void drawItem(int x, int y, int width, int height, int id)
	{
		item_list.add(new Item(x, y, width, height, id));
	}

	public static float getXPforLevel(int level)
	{
		float xp = 0.0f;
		for(int x = 1; x < level; x++)
			xp += Math.floor(x + 300 * Math.pow(2, x / 7.0f)) / 4.0f;
		return xp;
	}

	public static float getXPUntilLevel(int skill)
	{
		float xpNextLevel = getXPforLevel(base_level[skill] + 1);
		return xpNextLevel - getXP(skill);
	}

	public static int getLevel(int i)
	{
		return current_level[i];
	}

	public static int getTotalLevel()
	{
		int total = 0;
		for(int i = 0; i < 18; i++)
			total += Client.base_level[i];
		return total;
	}

	public static float getTotalXP()
	{
		float xp = 0;
		for(int i = 0; i < 18; i++)
			xp += getXP(i);
		return xp;
	}

	public static int getBaseLevel(int i)
	{
		return base_level[i];
	}

	public static float getXP(int i)
	{
		return (float)xp[i] / 4.0f;
	}

	public static int getFatigue()
	{
		return (fatigue * 100 / 750);
	}

	public static boolean isFriend(String name)
	{
		for(int i = 0; i < friends_count; i++)
		{
			if(friends[i] != null && friends[i].equals(name))
				return true;
		}

		return false;
	}

	public static boolean isInterfaceOpen()
	{
		return (show_bank || show_shop || show_welcome || show_trade || show_tradeconfirm || show_duel || show_duelconfirm ||
			show_report != 0 || show_friends != 0 || show_sleeping);
	}

	private static void dumpStrings()
	{
		BufferedWriter writer = null;

		try
		{
			File file = new File(Settings.Dir.DUMP + "/strings.dump");
			writer = new BufferedWriter(new FileWriter(file));

			writer.write("Client:\n\n");
			for(int i = 0; i < strings.length; i++)
				writer.write(i + ": " + strings[i] + "\n");

			writer.close();
		}
		catch(Exception e)
		{
			try { writer.close(); } catch(Exception e2) {}
		}
	}

	public static List<NPC> npc_list = new ArrayList<NPC>();
	public static List<Item> item_list = new ArrayList<Item>();

	public static final int SKILL_ATTACK = 0;
	public static final int SKILL_DEFENSE = 1;
	public static final int SKILL_STRENGTH = 2;
	public static final int SKILL_HP = 3;
	public static final int SKILL_RANGED = 4;
	public static final int SKILL_PRAYER = 5;
	public static final int SKILL_MAGIC = 6;
	public static final int SKILL_COOKING = 7;
	public static final int SKILL_WOODCUT = 8;
	public static final int SKILL_FLETCHING = 9;
	public static final int SKILL_FISHING = 10;
	public static final int SKILL_FIREMAKING = 11;
	public static final int SKILL_CRAFTING = 12;
	public static final int SKILL_SMITHING = 13;
	public static final int SKILL_MINING = 14;
	public static final int SKILL_HERBLAW = 15;
	public static final int SKILL_AGILITY = 16;
	public static final int SKILL_THIEVING = 17;

	public static final int STATE_LOGIN = 1;
	public static final int STATE_GAME = 2;

	public static final int MENU_NONE = 0;
	public static final int MENU_INVENTORY = 1;
	public static final int MENU_MINIMAP = 2;
	public static final int MENU_STATS = 3;
	public static final int MENU_FRIENDS = 4;
	public static final int MENU_SETTINGS = 5;

	public static final int CHAT_NONE = 0;
	public static final int CHAT_PRIVATE = 1;
	public static final int CHAT_QUEST = 3;
	public static final int CHAT_CHAT = 4;

	public static final int COMBAT_CONTROLLED = 0;
	public static final int COMBAT_AGGRESSIVE = 1;
	public static final int COMBAT_ACCURATE = 2;
	public static final int COMBAT_DEFENSIVE = 3;

	public static int state = STATE_LOGIN;

	public static int inventory_count;
	public static long magic_timer = 0L;

	public static boolean show_bank;
	public static boolean show_duel;
	public static boolean show_duelconfirm;
	public static int show_friends;
	public static int show_menu;
	public static boolean show_questionmenu;
	public static int show_report;
	public static boolean show_shop;
	public static boolean show_sleeping;
	public static boolean show_trade;
	public static boolean show_tradeconfirm;
	public static boolean show_welcome;

	public static int inventory_items[];

	public static int fatigue;
	public static int current_level[];
	public static int base_level[];
	public static int xp[];
	public static String skill_name[];
	public static int combat_style;

	public static int friends_count;
	public static String friends[];

	public static String pm_username;
	public static String pm_text;
	public static String pm_enteredText;
	public static String lastpm_username = null;

	public static int login_screen;
	public static String username_login;

	public static String strings[];

	public static XPDropHandler xpdrop_handler = new XPDropHandler();
	public static XPBar xpbar = new XPBar();

	// Game's client instance
	public static Object instance;

	private static TwitchIRC twitch = new TwitchIRC();
	private static MouseHandler handler_mouse;
	private static KeyboardHandler handler_keyboard;
	private static float xpdrop_state[] = new float[18];
}
