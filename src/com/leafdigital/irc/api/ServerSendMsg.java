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
 * Communication to be sent to an IRC server.
 * <p>
 * Note: This message is marked handled once it has been sent. Marking it handled
 * beforehand will prevent it being sent.
 */
public class ServerSendMsg extends ServerMsg
{
	/** Line of text */
	private byte[] line;

	/**
	 * @param s Server to send line on
	 * @param line Line of data to send
	 */
	public ServerSendMsg(Server s, byte[] line)
	{
		super(s, NOSEQUENCE);
		this.line = line;
	}

	/** @return Line of data */
	public byte[] getLine()
	{
		return line;
	}

	/** @param line New value for line of data */
	public void setLine(byte[] line)
	{
		this.line = line;
	}

	/**
	 * Information about message for scripting system.
	 */
	public static MessageInfo info = new MessageInfo(ServerSendMsg.class,
		"Send line",
		"<para>Occurs when leafChat is about to send a line of data to the "
		+	"server.</para>"
		+	"<para><small>Line data is handled as byte arrays and can be difficult to "
		+	"process in scripts; the provided 'line' string is converted using "
		+ "ISO-8859-1 encoding regardless of actual encoding. Use "
		+ "<key>msg.getLine()</key> to retrieve the raw data and "
		+ "<key>msg.setLine()</key> to alter the raw data before sending.</small></para>"
		+ "<para><small>Use <key>msg.markHandled();</key> to prevent a line being "
		+	"sent.</small></para>")
	{
		@Override
		protected void listScriptingVariables(Variables v)
		{
			super.listScriptingVariables(v);
			v.add("line", String.class,
				"String line = IRCMsg.convertISO(msg.getLine());");
		}
	};
}
