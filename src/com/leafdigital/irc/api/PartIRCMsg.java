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

/** Somebody leaves a channel */
public class PartIRCMsg extends ChanIRCMsg
{
	/** Text of message */
	private byte[] text;

	/**
	 * @param source User that is in the message prefix
	 * @param channel Channel name
	 * @param text Text of part message (null if none)
	 */
	public PartIRCMsg(IRCUserAddress source,String channel,byte[] text)
	{
		super(source,channel);
		this.text=text;
	}

	/** @return Text of message (null if none) */
	public byte[] getText() { return text; }

	@Override
	public String toString()
	{
		return super.toString()+
		  "  [PartIRCMessage]\n"+
			"  Text: "+IRCMsg.convertISO(getText())+"\n";
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(PartIRCMsg.class,"Part",
		"A user has left the channel.")
	{
	};
}
