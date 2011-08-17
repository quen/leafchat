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

import java.util.Collection;

import leafchat.core.api.*;

/** IRC message sent to a channel */
public class ChanIRCMsg extends UserSourceIRCMsg
{
	/** Channel */
	private String channel;

	/**
	 * @param source User that is in the message prefix
	 * @param channel Channel name
	 */
	public ChanIRCMsg(IRCUserAddress source,String channel)
	{
		super(source);
		this.channel=channel;
	}

	/** @return Channel name */
	public String getChannel() { return channel; }

	@Override
	public String toString()
	{
		return super.toString()+
		  "  [ChanIRCMessage]\n"+
			"  Channel: "+getChannel()+"\n";
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(ChanIRCMsg.class,"Channel",
		"All events related to a specific channel.")
	{
		@Override
		public String getContextInit()
		{
			return "registerContext(msg.getServer(),msg.getSourceUser(),msg.getChannel(),null);";
		}
		@Override
		protected void listAppropriateFilters(Collection<FilterInfo> list)
		{
			super.listAppropriateFilters(list);
			list.add(ChanAndServerFilter.info);
			list.add(ChanFilter.info);
		}
		@Override
		protected void listScriptingVariables(Variables v)
		{
			super.listScriptingVariables(v);
			v.add("channel");
		}
	};
}
