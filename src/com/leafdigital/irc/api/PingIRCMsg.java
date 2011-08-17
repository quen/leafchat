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

/** IRC ping message */
public class PingIRCMsg extends IRCMsg
{
	/** Ping code */
	private String code;

	/**
	 * @param code Ping code (null if none)
	 */
	public PingIRCMsg(String code)
	{
		this.code=code;
	}

	/** @return Ping code (null if none) */
	public String getCode() { return code; }

	@Override
	public String toString()
	{
		return super.toString()+
		  "  [PingIRCMessage]\n"+
			"  Code: "+getCode()+"\n";
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(PingIRCMsg.class,"Server ping",
		"The server sends this event periodically if data hasn't been received for a while.")
	{
	};
}
