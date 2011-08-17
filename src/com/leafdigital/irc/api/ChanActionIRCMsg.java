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

/** Action message (/me) */
public class ChanActionIRCMsg extends ChanCTCPRequestIRCMsg
{
	/**
	 * @param source User that is in the message prefix
	 * @param channel Channel name
	 * @param text Text
	 */
	public ChanActionIRCMsg(IRCUserAddress source,String channel,byte[] text)
	{
		super(source,channel,"ACTION",text);
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(ChanActionIRCMsg.class,"Action",
		"<para>A channel action (/me).</para>" +
		"<small>The IRC server does not send back your own messages, so you can't " +
		"use this to detect things you said yourself.</small>")
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
