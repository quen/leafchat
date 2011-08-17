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
package com.leafdigital.irc.api;

/**
 * Interface implemented by things that can display error messages from
 * commands. This is how commands show messages to the current window; command
 * calls include a MessageDisplay reference.
 */
public interface MessageDisplay
{
	/** Text sent to server as PRIVMSG */
	public final static int TYPE_MSG=1;

	/** Text sent to server as NOTICE */
	public final static int TYPE_NOTICE=2;

	/** Text sent to server as /me */
	public final static int TYPE_ACTION=3;

	/**
	 * Shows an error message.
	 * @param message Text of message
	 */
	public void showError(String message);

	/**
	 * Shows some generic information.
	 * @param message Text of message
	 */
	public void showInfo(String message);

	/**
	 * Shows text that has been sent to server (but will not be responded to).
	 * @param type An OWNTEXT_xx constant
	 * @param target Target of message/notice/etc
	 * @param text Text of message/notice/etc
	 */
	public void showOwnText(int type,String target,String text);

	/** Clears the window where the message was typed. */
	public void clear();
}
