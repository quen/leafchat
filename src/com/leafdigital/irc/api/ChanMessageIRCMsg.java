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

/** Normal message sent to a channel */
public class ChanMessageIRCMsg extends ChanIRCMsg
{
	/** Actual text of message */
	private byte[] text;

	/**
	 * @param source User that is in the message prefix
	 * @param channel Channel name
	 * @param text Text of message
	 */
	public ChanMessageIRCMsg(IRCUserAddress source,String channel,byte[] text)
	{
		super(source,channel);
		this.text=text;
	}

	/** @return Actual text of message */
	public byte[] getText() { return text; }

	@Override
	public String toString()
	{
		return super.toString()+
		  "  [ChanMessageIRCMessage]\n"+
			"  Text: "+IRCMsg.convertISO(getText())+"\n";
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(ChanMessageIRCMsg.class,"Message",
		"<para>A channel message (ordinary text).</para>" +
		"<small>The IRC server does not send back your own messages, so you can't " +
		"use this to detect things you said yourself.</small>")
	{
		@Override
		protected void listScriptingVariables(Variables v)
		{
			super.listScriptingVariables(v);
			v.add("text",String.class,"String text=msg.convertEncoding(msg.getText());");
		}
	};

}
