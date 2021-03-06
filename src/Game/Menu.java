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
import java.lang.reflect.Array;
import java.lang.reflect.Field;

public class Menu
{
	public static void resize()
	{
		if(chat_menu != null && Reflection.menuX != null)
		{
			try
			{
				boolean accessibleX = Reflection.menuX.isAccessible();
				boolean accessibleY = Reflection.menuY.isAccessible();
				Reflection.menuX.setAccessible(true);
				Reflection.menuY.setAccessible(true);

				int[] y = (int[])Reflection.menuY.get(chat_menu);
				y[chat_input] = Renderer.height_client - (334 - 324);
				y[chat_type1] = Renderer.height_client - (334 - 269);
				y[chat_type2] = Renderer.height_client - (334 - 269);
				y[chat_type3] = Renderer.height_client - (334 - 269);
				Reflection.menuY.set(chat_menu, y);

				int[] x = (int[])Reflection.menuX.get(quest_menu);
				x[quest_handle] = Renderer.width - 199;
				Reflection.menuX.set(quest_menu, x);

				x = (int[])Reflection.menuX.get(friend_menu);
				x[friend_handle] = Renderer.width - 199;
				Reflection.menuX.set(friend_menu, x);

				x = (int[])Reflection.menuX.get(spell_menu);
				x[spell_handle] = Renderer.width - 199;
				Reflection.menuX.set(spell_menu, x);

				Reflection.menuX.setAccessible(accessibleX);
				Reflection.menuY.setAccessible(accessibleY);
			}
			catch(Exception e) {}
		}
	}

	public static boolean switchList(Object menu)
	{
		// Spell menu scroll persistence
		if(menu == spell_menu)
		{
			try
			{
				boolean accessibleScroll = Reflection.menuScroll.isAccessible();
				Reflection.menuScroll.setAccessible(true);

				int[] scroll = (int[])Reflection.menuScroll.get(spell_menu);

				// Swap scroll values
				spell_swap_scroll[spell_swap_idx] = scroll[spell_handle];
				spell_swap_idx ^= 1;
				scroll[spell_handle] = spell_swap_scroll[spell_swap_idx];

				Reflection.menuScroll.set(spell_menu, scroll);
				Reflection.menuScroll.setAccessible(accessibleScroll);

				return false;
			}
			catch(Exception e) {}
		}

		return true;
	}

	private static int spell_swap_idx = 0;
	private static int spell_swap_scroll[] = { 0, 0 };

	public static Object chat_menu;
	public static int chat_type1;
	public static int chat_type2;
	public static int chat_type3;
	public static int chat_input;

	public static Object quest_menu;
	public static int quest_handle;

	public static Object friend_menu;
	public static int friend_handle;

	public static Object spell_menu;
	public static int spell_handle;
}
