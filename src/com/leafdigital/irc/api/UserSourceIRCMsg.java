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

/** IRC message that has a user as prefix */
public class UserSourceIRCMsg extends IRCMsg
{
	/** User that is in the message prefix */
	private IRCUserAddress source;

	/**
	 * @param source User that is in the message prefix
	 */
	public UserSourceIRCMsg(IRCUserAddress source)
	{
		this.source=source;
	}

	/** @return User that is in the message prefix */
	public IRCUserAddress getSourceUser() { return source; }

	@Override
	public String toString()
	{
		return super.toString()+
		  "  [UserSourceIRCMessage]\n"+
			"  Source user: "+source+"\n";
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(UserSourceIRCMsg.class,"From user",
		"All events that come from a named user.")
	{
		@Override
		public String getContextInit()
		{
			return "registerContext(msg.getServer(),msg.getSourceUser(),null,null);";
		}
		@Override
		protected void listAppropriateFilters(Collection<FilterInfo> list)
		{
			super.listAppropriateFilters(list);
			list.add(NickAndServerFilter.info);
			list.add(UserFilter.info);
			list.add(NickFilter.info);
		}
		@Override
		protected void listScriptingVariables(Variables v)
		{
			super.listScriptingVariables(v);
			v.add("sourceNick",String.class,"String sourceNick=msg.getSourceUser().getNick();");
			v.add("sourceUser",String.class,"String sourceUser=msg.getSourceUser().getUser();");
			v.add("sourceHost",String.class,"String sourceHost=msg.getSourceUser().getHost();");
		}
	};
}
