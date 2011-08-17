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

import leafchat.core.api.*;

/**
 * Singleton that runs IRC /-commands. Commands are implemented via plugins
 * which listen for {@link UserCommandMsg}.
 */
public interface Commands extends Singleton
{
	/**
	 * Carries out an IRC command.
	 * @param command Command string
	 * @param contextServer Context server that command applies to
	 * @param contextNick Context nickname (may be null)
	 * @param contextChan Context channel (may be null)
	 * @param md Display to use for response/error messages
	 * @param variables If true, replaces variables in command (note: this is
	 *   currently not implemented and has no effect)
	 */
	public void doCommand(String command,
		Server contextServer,IRCUserAddress contextNick,String contextChan,
		MessageDisplay md,boolean variables);

	/**
	 * Returns true if the character (presumably first character of an IRC typed
	 * line) is a command character. Usually command character is / but users
	 * can add others.
	 * @param c Character in question
	 * @return True if command, false otherwise
	 */
	public boolean isCommandCharacter(char c);
}
