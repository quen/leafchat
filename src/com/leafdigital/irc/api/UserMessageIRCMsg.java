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

/** Message sent to a user */
public class UserMessageIRCMsg extends UserIRCMsg
{
	/** Actual text of message */
	private byte[] text;

	/**
	 * @param source User that is in the message prefix
	 * @param targetNick Target nickname
	 * @param text Text of message
	 */
	public UserMessageIRCMsg(IRCUserAddress source,String targetNick,byte[] text)
	{
		super(source,targetNick);
		this.text=text;
	}

	/** @return Actual text of message */
	public byte[] getText() { return text; }

	@Override
	public String toString()
	{
		return super.toString()+
		  "  [UserMessageIRCMessage]\n"+
			"  Text: "+IRCMsg.convertISO(getText())+"\n";
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(UserMessageIRCMsg.class,"Message",
		"A private message (ordinary text).")
	{
		@Override
		protected void listScriptingVariables(Variables v)
		{
			super.listScriptingVariables(v);
			v.add("text",String.class,"String text=msg.convertEncoding(msg.getText());");
		}
	};
}
