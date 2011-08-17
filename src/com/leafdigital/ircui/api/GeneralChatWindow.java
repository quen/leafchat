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

import org.w3c.dom.Element;

import com.leafdigital.irc.api.*;

import leafchat.core.api.GeneralException;

/**
 * A chat window. Created with {@link IRCUI#createGeneralChatWindow(leafchat.core.api.PluginContext, com.leafdigital.ircui.api.GeneralChatWindow.Handler, String, String, String, int, String, String, boolean)}.
 */
public interface GeneralChatWindow
{
	/** Callbacks from the window */
	public interface Handler
	{
		/**
		 * User has entered a command line.
		 * @param c Commands object
		 * @param line Line entered
		 * @throws GeneralException Any error
		 */
		public void doCommand(Commands c,String line) throws GeneralException;

		/**
		 * User has closed the window. Discard references to it.
		 * @throws GeneralException Any error
		 */
		public void windowClosed() throws GeneralException;

		/**
		 * User has clicked on an <internalaction> link. (This can do nothing if
		 * you aren't going to output any such link.)
		 * @param e XML element representing the link
		 * @throws GeneralException Any error
		 */
		public void internalAction(Element e) throws GeneralException;
	}

	/**
	 * Enables or disables the edit box in the window.
	 * @param enabled True to enable, false to disable
	 */
	public void setEnabled(boolean enabled);

	/**
	 * Adds a line of arbitrary XML text to the display (not logged).
	 * @param xml XML format data; if intending plain text, remember to escape it
	 */
	public void addLine(String xml);

	/**
	 * Adds a line of arbitrary XML text to the display and to the system logs.
	 * @param xml XML format data; if intending plain text, remember to escape it
	 * @param logType Log event type identifier.
	 */
	public void addLine(String xml,String logType);

	/**
	 * Shows text from our user. May be directed at the target in this window
	 * or somebody else. Text is automatically logged (if enabled).
	 * @param type MessageDisplay.TYPE_xx constant
	 * @param target Target name (if this matches the window target name,
	 *   it'll show appropriately)
	 * @param text Text to display
	 */
	public void showOwnText(int type,String target,String text);

	/**
	 * Sets this window's default target name.
	 * @param target Nickname of target
	 */
	public void setTarget(String target);

	/**
	 * Shows text from another user in this window. Text is automatically logged
	 * (if enabled).
	 * @param type MessageDisplay.TYPE_xx constant
	 * @param nick Source nickname
	 * @param text Text to display
	 */
	public void showRemoteText(int type,String nick,String text);

	/**
	 * Sets the window title.
	 * @param title New title
	 */
	public void setTitle(String title);

	/** @return MessageDisplay object for window */
	public MessageDisplay getMessageDisplay();
}
