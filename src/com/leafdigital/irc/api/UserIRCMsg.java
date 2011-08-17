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

/** IRC message sent to a user */
public class UserIRCMsg extends UserSourceIRCMsg
{
	/** Target nickname */
	private String targetNick;

	/**
	 * @param source Source user
	 * @param targetNick Target nickname
	 */
	public UserIRCMsg(IRCUserAddress source,String targetNick)
	{
		super(source);
		this.targetNick=targetNick;
	}

	/** @return Target nickname */
	public String getTargetNick() { return targetNick; }

	@Override
	public String toString()
	{
		return super.toString()+
		  "  [UserIRCMessage]\n"+
			"  Target nick: "+getTargetNick()+"\n";
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(UserIRCMsg.class,"Personal",
		"Events related to direct communication with an individual user and " +
		"not a channel.")
	{
	};
}
