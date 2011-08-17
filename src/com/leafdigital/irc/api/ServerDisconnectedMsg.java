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

/** Sent once a socket connection has been lost to an IRC server */
public class ServerDisconnectedMsg extends ServerMsg
{
	/** Exception that caused disconnect */
	private NetworkException ne;

	/** True if disconnect was requested by user */
	private boolean requested;

	/**
	 * @param s Server that is now disconnected
	 * @param ne Exception that caused disconnect (null if none)
	 * @param sequence Message sequence
	 * @param requested True if disconnect was requested by user
	 */
	public ServerDisconnectedMsg(Server s, NetworkException ne, int sequence,
		boolean requested)
	{
		super(s, sequence);
		this.ne = ne;
		this.requested = requested;
	}

	/** @return Exception that caused disconnect (null if none) */
	public NetworkException getException()
	{
		return ne;
	}

	/**
	 * @return True if disconnect was requested by user via /quit or similar
	 */
	public boolean isRequested()
	{
		return requested;
	}

	/**
	 * Information about message for scripting system.
	 */
	public static MessageInfo info = new MessageInfo(ServerDisconnectedMsg.class,
		"Disconnected",
		"<para>Event sent when leafChat becomes disconnected from a server.</para>"
		+	"<small>The <key>requested</key> variable is true if the disconnect was "
		+ "caused by a user request such as typing /quit.</small>")
	{
		@Override
		protected void listScriptingVariables(Variables v)
		{
			super.listScriptingVariables(v);
			v.add("requested", boolean.class, "boolean requested = msg.isRequested();");
		}
	};
}
