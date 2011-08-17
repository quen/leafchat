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

import leafchat.core.api.*;

/**
 * Access to parts of the IRC user interface, such as chat windows and the like.
 */
public interface IRCUI extends Singleton
{
	/**
	 * Obtains a MessageDisplay object which can be used to add text to the
	 * current window for a given server. If there is no window for that server
	 * then one will be created if text is added (not otherwise).
	 * @param s Server to display messages for (may be null)
	 * @return MessageDisplay that shows messages appropriately for that server
	 */
	public MessageDisplay getMessageDisplay(Server s);

	/**
	 * Creates a new chat window. This window is like a message window but
	 * has no connection to any particular server and does not automatically
	 * display anything. Methods of {@link GeneralChatWindow} must be used to
	 * make the window display things.
	 * @param owner Context that owns window. If this plugin is unloaded, the
	 *   window will automatically close itself
	 * @param h Callback that handles command entry from the window
	 * @param logSource Source for log address. Usually a server address but
	 *   can be something else. Use null to prevent logging
	 * @param logCategory Category of thing for log. Logger.CATEGORY_xx constant
	 *   or custom name
	 * @param logItem Item name for log. For Logger.CATEGORY_USER, must be nick;
	 *   for CATEGORY_CHAN, must be channel. For custom types can be anything
	 *   appropriate
	 * @param availableBytes Number of bytes allowed per line of text (best
	 *   to include a safe margin), used for auto-wrapping text. Applies only to
	 *   text not to /-commands which are auto-limited at 400 odd.
	 * @param ownNick User's own nick for purposes of this window (display own
	 *   messages)
	 * @param target Target's nick (or other identifier), i.e. where text to this
	 *   window goes; will be matched up against showOwnText value
	 * @param startMinimised True if window should be minimised to start with
	 * @return Interface to the window
	 */
	public GeneralChatWindow createGeneralChatWindow(PluginContext owner,
		GeneralChatWindow.Handler h,
		String logSource,String logCategory,String logItem,
		int availableBytes,String ownNick,String target, boolean startMinimised);

}
