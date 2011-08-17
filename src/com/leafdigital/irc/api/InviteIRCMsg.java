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

/** Invite message */
public class InviteIRCMsg extends UserIRCMsg
{
	/** Channel being invited to */
	private String channel;

	/**
	 * @param source User that is in the message prefix
	 * @param targetNick Target nickname
	 * @param channel Channel being invited to
	 */
	public InviteIRCMsg(IRCUserAddress source,String targetNick,String channel)
	{
		super(source,targetNick);
		this.channel=channel;
	}

	/** @return Channel being invited to */
	public String getChannel() { return channel; }

	@Override
	public String toString()
	{
		return super.toString()+
		  "  [InviteIRCMessage]\n"+
			"  Channel: "+getChannel()+"\n";
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(InviteIRCMsg.class,"Invite",
		"Sent when another user invites you to a channel.")
	{
		@Override
		protected void listScriptingVariables(Variables v)
		{
			super.listScriptingVariables(v);
			v.add("channel");
		}
	};
}
