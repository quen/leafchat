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
 * Sent once an IRC connection is fully connected (currently this happens after
 * we get the 'end of MOTD' message, once the server stops sending stuff)
 */
public class ServerConnectionFinishedMsg extends ServerMsg
{
	/**
	 * @param s Server that is now fully connected
	 * @param sequence Message sequence
	 */
	public ServerConnectionFinishedMsg(Server s,int sequence)
	{
		super(s,sequence);
	}

	/**
	 * Information about message for scripting system.
	 */
	public static MessageInfo info=new MessageInfo(ServerConnectionFinishedMsg.class,
		"Connection finished",
		"<para>Event sent when leafChat has successfully connected to an IRC "
		+	"server and is ready to send commands.</para>")
	{
	};
}
