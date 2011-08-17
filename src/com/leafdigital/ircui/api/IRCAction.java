/*
This file is part of leafdigital leafChat.

leafChat is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

leafChat is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with leafChat. If not, see <http://www.gnu.org/licenses/>.

Copyright 2011 Samuel Marshall.
*/
package com.leafdigital.ircui.api;

import com.leafdigital.irc.api.*;

/**
 * Interface to be implemented by code that provides a particular action.
 * @see IRCActionListMsg
 */
public interface IRCAction
{
	/** Predefined category: user-related. */
	public final static int CATEGORY_USER=100;
	/** Predefined category: user and channel-related. */
	public final static int CATEGORY_USERCHAN=200;
	/** Predefined category: channel-related. */
	public final static int CATEGORY_CHAN=300;

	/**
	 * Obtains the category, used for ordering and separating items in the list.
	 * @return Category of action; a CATEGORY_xx constant or new constant
	 *   selected for plugin.
	 */
	public int getCategory();

	/**
	 * Obtains the ordering within the category. Used to place items at particular
	 * positions. When defining new items, try to leave gaps (e.g. put the numbers
	 * 10 apart) so that there's room for plugins to insert other options later.
	 * @return Order within category (lowest first)
	 */
	public int getOrder();

	/**
	 * @return Name of action, used for display on the list
	 */
	public String getName();

	/**
	 * Called when the action should actually run.
	 * @param s Server
	 * @param contextChannel Context channel (the one the user's action happened in)
	 * @param contextNick Context nick (the one the user's action happened in)
	 * @param selectedChannel Selected channel (one which was actively chosen)
	 * @param selectedNicks Selected nicks (which were actively chosen)
	 * @param caller Reference to window that should be used to display messages
	 *   if needed
	 */
	public void run(Server s,String contextChannel,String contextNick,
		String selectedChannel,String[] selectedNicks,MessageDisplay caller);
}
