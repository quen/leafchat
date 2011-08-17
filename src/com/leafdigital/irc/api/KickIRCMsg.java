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

/** Somebody is kicked from channel */
public class KickIRCMsg extends ChanIRCMsg
{
	/** Victim of kick */
	private String victim;

	/** Text of message */
	private byte[] text;

	/**
	 * @param source User that is in the message prefix
	 * @param channel Channel name
	 * @param victim Victim of kick message
	 * @param text Text of kick message (null if none)
	 */
	public KickIRCMsg(IRCUserAddress source,String channel,String victim,byte[] text)
	{
		super(source,channel);
		this.victim=victim;
		this.text=text;
	}

	/** @return Nickname of victim */
	public String getVictim() { return victim; }

	/** @return Text of message (null if none) */
	public byte[] getText() { return text; }

	@Override
	public String toString()
	{
		return super.toString()+
		  "  [KickIRCMessage]\n"+
			"  Victim: "+getVictim()+"\n"+
			"  Text: "+IRCMsg.convertISO(getText())+"\n";
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(KickIRCMsg.class,"Kick",
		"<para>A user has been kicked off the channel.</para>" +
		"<small>The source user is the one who did the kicking. The one " +
		"kicked is <key>victim</key>.</small>")
	{
		@Override
		protected void listScriptingVariables(Variables v)
		{
			super.listScriptingVariables(v);
			v.add("victim");
		}
	};

}
