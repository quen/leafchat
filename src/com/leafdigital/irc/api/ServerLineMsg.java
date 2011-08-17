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
 * Represents the lowest level of IRC communication: a line received from the
 * server.
 */
public class ServerLineMsg extends ServerMsg
{
	/** Line received from server */
	private byte[] line;

	/**
	 * @param s Server that sent this line
	 * @param line Line of text
	 * @param sequence Message sequence
	 */
	public ServerLineMsg(Server s,byte [] line,int sequence)
	{
		super(s,sequence);
		this.line=line;
	}

	/** @return Line of text that was received (not including CRLF) */
	public byte[] getLine()
	{
		return line;
	}

	/**
	 * Information about message for scripting system.
	 */
	public static MessageInfo info = new MessageInfo(ServerLineMsg.class,
		"Received line",
		"<para>Occurs whenever leafChat receives a line of data from the "
		+	"server.</para>"
		+	"<para><small>Line data is handled as byte arrays and can be difficult to "
		+	"process in scripts; the provided 'line' string is converted using "
		+ "ISO-8859-1 encoding regardless of actual encoding. Use "
		+ "<key>msg.getLine()</key> to retrieve the raw data.</small></para>"
		+ "<para><small>Use <key>msg.markHandled();</key> to prevent leafChat "
		+	"processing the line and generating other appropriate events."
		+	"</small></para>")
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
