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

import java.util.*;

import util.xml.XML;

import leafchat.core.api.Msg;

/**
 * Message sent to get a list of available / commands.
 */
public class UserCommandListMsg extends Msg
{
	/** IRC server (null if none) */
	private Server server = null;

	/** True if retrieving all commands. */
	private boolean all = false;

	/** List of all supported commands. */
	private SortedSet<CommandDetails> list = new TreeSet<CommandDetails>();

	/**
	 * Command is frequently typed by ordinary users.
	 */
	public final static int FREQ_COMMON = 100;
	/**
	 * Command may be typed by ordinary users, but is used less frequently.
	 */
	public final static int FREQ_UNCOMMON = 200;
	/**
	 * Command is used only by IRC operators, is normally handled internally
	 * by clients, or is normally used only when scripting.
	 */
	public final static int FREQ_OBSCURE = 300;

	private static class CommandDetails implements Comparable<CommandDetails>
	{
		private String command;
		private int frequency;
		private String description;

		private CommandDetails(String command, int frequency, String description)
		{
			this.command = command;
			this.frequency = frequency;
			this.description = description;
		}

		@Override
		public int compareTo(CommandDetails other)
		{
			if(frequency < other.frequency)
			{
				return -1;
			}
			else if(frequency > other.frequency)
			{
				return 1;
			}

			return command.compareTo(other.command);
		}

		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof CommandDetails))
			{
				return false;
			}
			CommandDetails other = (CommandDetails)o;
			return other.frequency == frequency && other.command.equals(command);
		}

		@Override
		public int hashCode()
		{
			return command.hashCode() ^ frequency;
		}
	}

	/**
	 * Used to retrieve commands that are appropriate for current server.
	 * @param server Server for command (null if none)
	 */
	public UserCommandListMsg(Server server)
	{
		this.server = server;
	}

	/**
	 * Used to retrieve all commands, regardless of whether currently connected
	 * to a server or not.
	 */
	public UserCommandListMsg()
	{
		all = true;
	}

	/** @return Server that command should run on (null if none) */
	public Server getServer()
	{
		return server;
	}

	/**
	 * For use by whoever despatched the message, after its despatch. Returns
	 * all the commands that were added (as strings without leading /,
	 * in common-first order).
	 * @return All the commands
	 */
	public String[] getCommands()
	{
		String[] commands = new String[list.size()];
		int i = 0;
		for(CommandDetails d : list)
		{
			commands[i++] = d.command;
		}
		return commands;
	}

	/**
	 * For user by whoever despatched the message, after its despatch. Returns
	 * the description of a given command, or null if it isn't known. The
	 * description is in XML format ready for output.
	 * @param command Command (must be lower-case with no slash)
	 * @return Description or null if none
	 */
	public String getDescription(String command)
	{
		for(CommandDetails d : list)
		{
			if(d.command.equals(command))
			{
				return d.description;
			}
		}
		return null;
	}

	/**
	 * Adds a supported command to the list. The frequency value relates to the
	 * chance of an ordinary user typing this command.
	 * @param onServer True if command only applies when connected to a server
	 * @param command Command (not including /)
	 * @param freq Frequency (FREQ_xx constant)
	 * @param example Example command (plain text)
	 * @param explanation Explanation of what the example command does (XML)
	 */
	public void addCommand(boolean onServer, String command, int freq, String example,
		String explanation)
	{
		// Don't add command if it requires a server connection and we don't have one.
		if (onServer && !all && server == null)
		{
			return;
		}
		StringBuilder out = new StringBuilder("<command><example>");
		example = XML.esc(example).replaceFirst("^(/[^ ]+)", "<name>$1</name>");
		out.append(example);
		out.append("</example><description>");
		out.append(explanation);
		out.append("</description></command>");
		list.add(new CommandDetails(command, freq,out.toString()));
	}
}
