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

/** Notice sent to a channel */
public class ChanNoticeIRCMsg extends ChanIRCMsg
{
	/** Actual text of message */
	private byte[] text;

	/** Status character */
	private char status;

	/**
	 * @param source User that is in the message prefix
	 * @param channel Channel name
	 * @param status Status character e.g. + if this message goes only to voice/ops,
	 *   0 for none
	 * @param text Text of message
	 */
	public ChanNoticeIRCMsg(IRCUserAddress source,String channel,
		char status,byte[] text)
	{
		super(source,channel);
		this.text=text;
		this.status=status;
	}

	/** @return Actual text of message */
	public byte[] getText() { return text; }

	/** @return Status character; 0 usually, '+' for voices message, etc. */
	public char getStatus() { return status; }

	@Override
	public String toString()
	{
		return super.toString()+
		  "  [ChanNoticeIRCMessage]\n"+
			"  Text: "+IRCMsg.convertISO(getText())+"\n"+
			"  Status: "+(getStatus()==0 ? "<normal>" : ""+getStatus())+"\n";
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(ChanNoticeIRCMsg.class,"Notice",
		"<para>A channel notice, sent to everyone on the channel or to people of " +
		"a particular status (e.g. @).</para>" +
		"<small>The <key>status</key> variable is blank for ordinary notices.</small>")
	{
		@Override
		protected void listScriptingVariables(Variables v)
		{
			super.listScriptingVariables(v);
			v.add("text",String.class,"String text=msg.convertEncoding(msg.getText());");
			v.add("status",String.class,"String status=c.getStatus()==0?\"\":c.getStatus()+\"\";");
		}
	};

}
