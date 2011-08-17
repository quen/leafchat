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

import java.util.Collection;

import leafchat.core.api.*;

/** Any communication from or to an IRC server */
public abstract class ServerMsg extends Msg
{
	/** Message sequence name used for all server messages and related messages */
	final static String SEQUENCE="ServerMsg";
	/** Constant indicating message has no sequence */
	final static int NOSEQUENCE=-1;

	/**
	 * @param s Server that sent the message (or to which it will be sent)
	 * @param sequence Sequence number (or -1 if this is a 'send' message)
	 */
	public ServerMsg(Server s,int sequence)
	{
		this.s=s;
		if(sequence!=NOSEQUENCE) setSequence(SEQUENCE,sequence);
	}

	/** Server that sent the message (or to which it will be sent) */
	private Server s;

	/** @return Server that sent the message (or to which it will be sent) */
	public Server getServer()
	{
		return s;
	}

	/**
	 * Information about message for scripting system.
	 */
	public static MessageInfo info=new MessageInfo(ServerMsg.class,
		"Server status",
		"<para>Low-level events relating to servers.</para>")
	{
		@Override
		public String getContextInit()
		{
			return "registerContext(msg.getServer(), null, null, null);";
		}
		@Override
		public boolean allowScripting()
		{
			return true;
		}

		@Override
		protected void listAppropriateFilters(Collection<FilterInfo> list)
		{
			super.listAppropriateFilters(list);
			list.add(ServerFilter.info);
		}

		@Override
		protected void listScriptingVariables(Variables v)
		{
			super.listScriptingVariables(v);
			v.add("server", Server.class,
				"Server server = msg.getServer();");
		}
	};
}
