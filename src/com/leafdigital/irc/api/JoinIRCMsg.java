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

/** JOIN to a channel */
public class JoinIRCMsg extends ChanIRCMsg
{
	/**
	 * @param source User that is in the message prefix
	 * @param channel Channel name
	 */
	public JoinIRCMsg(IRCUserAddress source,String channel)
	{
		super(source,channel);
	}

	@Override
	public String toString()
	{
		return super.toString()+
		  "  [JoinIRCMessage]\n";
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(JoinIRCMsg.class,"Join",
		"A user has joined the channel.")
	{
		@Override
		protected void listScriptingVariables(Variables v)
		{
			super.listScriptingVariables(v);
			v.add("isYou",Boolean.class,"boolean isYou=msg.getSourceUser().getNick().equalsIgnoreCase(msg.getServer().getOurNick());");
		}
	};
}
