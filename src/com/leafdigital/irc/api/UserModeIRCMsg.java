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

/** User mode change */
public class UserModeIRCMsg extends UserIRCMsg
{
	/** Mode changes */
	private String modes;

	/**
	 * @param source User that is in the message prefix
	 * @param targetNick Target nickname
	 * @param modes Mode changes
	 */
	public UserModeIRCMsg(IRCUserAddress source,String targetNick,String modes)
	{
		super(source,targetNick);
		this.modes=modes;
	}

	/** @return Mode changes */
	public String getModes() { return modes; }

	/** @return All mode letters that have been turned on by this command */
	public String getPositiveModes()
	{
		StringBuffer sb=new StringBuffer();
		boolean on=true; // Default to on?
		for(int i=0;i<modes.length();i++)
		{
			char c=modes.charAt(i);
			if(c=='-')
				on=false;
			else if(c=='+')
				on=true;
			else if(on)
				sb.append(c);
		}
		return sb.toString();
	}

	/** @return All mode letters that have been turned off by this command */
	public String getNegativeModes()
	{
		StringBuffer sb=new StringBuffer();
		boolean on=false;
		for(int i=0;i<modes.length();i++)
		{
			char c=modes.charAt(i);
			if(c=='-')
				on=true;
			else if(c=='+')
				on=false;
			else if(on)
				sb.append(c);
		}
		return sb.toString();
	}

	@Override
	public String toString()
	{
		return super.toString()+
		  "  [UserModeIRCMessage]\n"+
			"  Modes: "+modes+"\n";
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(UserModeIRCMsg.class,"Mode",
		"<para>Indicates that your user modes have changed.</para>" +
		"<small>Mode letters that were turned on (if any) are in " +
		"<key>positiveModes</key>. Mode letters that were turned off (if any) are " +
		"in <key>negativeModes</key></small>.")
	{
		@Override
		protected void listScriptingVariables(Variables v)
		{
			super.listScriptingVariables(v);
			v.add("positiveModes");
			v.add("negativeModes");
		}
	};
}
