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

import leafchat.core.api.MessageInfo;

/**
 * Message sent when a 'watched' user comes online
 */
public class OnWatchMsg extends WatchMsg
{
	/**
	 * @param s Server
	 * @param ua User address
	 * @param realChange True if this was a real change in their status rather
	 *   than just us adding the entry
	 */
	public OnWatchMsg(Server s,IRCUserAddress ua,boolean realChange)
	{
		super(s,ua, realChange);
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(OnWatchMsg.class,"User online",
		"<para>A user is marked as online.</para>" +
		"<small>This event applies only to watched users. It occurs either when a " +
		"user just came online, or when they were just added to the watch list. " +
		"You can tell which by checking msg.isRealChange().</small>")
	{
	};
}
