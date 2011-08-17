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

/**
 * Message sent when user types a /-command or ordinary text
 */
public class UserCommandMsg extends Msg
{
	/** IRC server (null if none) */
	private Server server;
	/** Context channel (null if none) */
	private String contextChan;
	/** Context user (null if none) */
	private IRCUserAddress contextUser;
	/** UI object representing context where message was typed */
	private MessageDisplay md;
	/** Slash-command name (null if none) */
	private String command;
	/** Parameters of command (everything after the command and space) */
	private String params;

	/**
	 * @param server Server for command (null if none)
	 * @param command Slash-command (null if none)
	 * @param params Parameters to command (empty string if none)
	 * @param contextChan Context chan (or null)
	 * @param contextUser Context nick/user (or null)
	 * @param md Context UI object (e.g. a channel window or something)
	 *   which will be used to generate error messages resulting directly from
	 *   this command.
	 */
	public UserCommandMsg(
		Server server, String command, String params,
		String contextChan, IRCUserAddress contextUser,
		MessageDisplay md)
	{
		if(command!=null)
		{
			command=command.toLowerCase();
		}
		this.command = command;
		this.params = params;
		this.server = server;
		this.contextChan = contextChan;
		this.contextUser = contextUser;
		if(md == null)
		{
			throw new NullPointerException("Message display may not be null");
		}
		this.md = md;
	}

	/**
	 * Call when there is an error in what the user's typed. Stops message
	 * processing and displays the error.
	 * @param s Text of error
	 */
	public void error(String s)
	{
		markStopped();
		md.showError(s);
	}

	/** @return Message display for window command was issued in */
	public MessageDisplay getMessageDisplay()
	{
		return md;
	}

	/** @return Server that command should run on (null if none) */
	public Server getServer()
	{
		return server;
	}
	/** @return Command in lower-case, or null if none */
	public String getCommand()
	{
		return command;
	}
	/** @return Parameters, or empty string if none */
	public String getParams()
	{
		return params;
	}
	/** @return Context chan (null if none) */
	public String getContextChan()
	{
		return contextChan;
	}
	/** @return Context user (null if none) */
	public IRCUserAddress getContextUser()
	{
		return contextUser;
	}

	/**
	 * Message information for scripting system.
	 */
	public static MessageInfo info=new MessageInfo(UserCommandMsg.class,
		"Typed command",
		"<para>Event sent when the user types a /-command into any window.</para>"
		+ "<small>It's usually easier to handle this by creating a "
		+ "<key>Command</key> script item rather than an <key>Event</key>.</small>")
	{
		@Override
		public String getContextInit()
		{
			return "registerContext(msg.getServer(),msg.getContextUser(),msg.getContextChan(),msg.getMessageDisplay());";
		}
		@Override
		public boolean allowScripting()
		{
			return true;
		}
		@Override
		protected void listScriptingVariables(Variables v)
		{
			super.listScriptingVariables(v);
			v.add("command");
			v.add("params");
		}
		@Override
		protected void listAppropriateFilters(Collection<FilterInfo> list)
		{
			super.listAppropriateFilters(list);
			list.add(CommandFilter.info);
		}
	};
}
