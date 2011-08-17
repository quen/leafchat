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

/** Nickname change */
public class NickIRCMsg extends UserSourceIRCMsg
{
	/** New nick */
	private String nick;

	/**
	 * @param source Source user (with old nick)
	 * @param nick New nick
	 */
	public NickIRCMsg(IRCUserAddress source,String nick)
	{
		super(source);
		this.nick=nick;
	}

	/** @return New nick */
	public String getNewNick() { return nick; }

	@Override
	public String toString()
	{
		return super.toString()+
		  "  [NickIRCMessage]\n"+
			"  New nick: "+getNewNick()+"\n";
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(NickIRCMsg.class,"Nick change",
		"<para>A user has changed their nickname.</para>" +
		"<small><key>sourceNick</key> is the old nick. The new one is " +
		"<key>newNick</key>.</small>")
	{
		@Override
		protected void listScriptingVariables(Variables v)
		{
			super.listScriptingVariables(v);
			v.add("newNick");
		}
	};

}
