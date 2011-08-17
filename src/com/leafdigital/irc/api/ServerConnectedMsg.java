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

/** Sent once a socket connection has been opened to an IRC server */
public class ServerConnectedMsg extends ServerMsg
{
	/**
	 * @param s Server that is now connected
	 * @param sequence Message sequence
	 */
	public ServerConnectedMsg(Server s,int sequence)
	{
		super(s,sequence);
	}

	/**
	 * Information about message for scripting system.
	 */
	public static MessageInfo info=new MessageInfo(ServerConnectedMsg.class,
		"Connected",
		"<para>Event sent when leafChat becomes connected to a server.</para>"
		+	"<small>This event occurs immediately on connection, when IRC commands "
		+ "cannot yet be executed.</small>")
	{
		@Override
		public boolean allowScripting()
		{
			return false;
		}
	};
}
