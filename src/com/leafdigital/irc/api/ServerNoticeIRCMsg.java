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

/** Represents a server NOTICE */
public class ServerNoticeIRCMsg extends ServerIRCMsg
{
	/** Text of notice */
	private byte[] text;

	/**
	 * @param sourceServer Server that sent the message
	 * @param target Target of notice
	 * @param text Text of notice
	 */
	public ServerNoticeIRCMsg(String sourceServer,String target,byte[] text)
	{
		super(sourceServer,target);
		this.text=text;
	}

	/** @return Text of notice */
	public byte[] getText() { return text; }

	@Override
	public String toString()
	{
		return super.toString()+
		  "  [ServerNoticeIRCMessage]\n"+
			"  Text: "+IRCMsg.convertISO(getText())+"\n";
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(ServerNoticeIRCMsg.class,"Server notice",
		"Notice sent directly from server.")
	{
		@Override
		protected void listScriptingVariables(Variables v)
		{
			super.listScriptingVariables(v);
			v.add("text",String.class,"String text=msg.convertEncoding(msg.getText());");
		}
	};

}
