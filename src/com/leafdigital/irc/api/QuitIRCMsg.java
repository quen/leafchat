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

/** User quit */
public class QuitIRCMsg extends UserSourceIRCMsg
{
	/** Quit message */
	private byte[] message;

	/**
	 * @param source Source user
	 * @param message Quit message
	 */
	public QuitIRCMsg(IRCUserAddress source,byte[] message)
	{
		super(source);
		this.message=message;
	}

	/** @return Quit message or null if none */
	public byte[] getMessage() { return message; }

	@Override
	public String toString()
	{
		return super.toString()+
		  "  [QuitIRCMessage]\n"+
			"  Message: "+IRCMsg.convertISO(getMessage())+"\n";
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(QuitIRCMsg.class,"Quit",
		"<para>A user has quit IRC.</para>" +
		"<small>This message is sent only for users who are on the same " +
		"channel as you. You don't receive it for users you're only chatting " +
		"privately with.</small>")
	{
		@Override
		protected void listScriptingVariables(Variables v)
		{
			super.listScriptingVariables(v);
			v.add("message",String.class,"String message=msg.convertEncoding(msg.getMessage());");
		}
	};

}
