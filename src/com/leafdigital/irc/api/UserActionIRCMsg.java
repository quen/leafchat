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

/** Action message */
public class UserActionIRCMsg extends UserCTCPRequestIRCMsg
{
	/**
	 * @param source User that is in the message prefix
	 * @param nick Channel name
	 * @param text Text
	 */
	public UserActionIRCMsg(IRCUserAddress source,String nick,byte[] text)
	{
		super(source,nick,"ACTION",text);
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(UserActionIRCMsg.class,"Action",
		"An action (/me) in a private message conversation.")
	{
		@Override
		protected void listAppropriateFilters(Collection<FilterInfo> list)
		{
			super.listAppropriateFilters(list);
			list.remove(CTCPCommandFilter.info);
		}
		@Override
		protected void listScriptingVariables(Variables v)
		{
			super.listScriptingVariables(v);
			v.remove("request");
			v.remove("params");
			v.add("text",String.class,"String text=msg.convertEncoding(msg.getText());");
		}
	};
}
