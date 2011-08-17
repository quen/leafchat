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

/** Represents messages which could not be recognised */
public class UnknownIRCMsg extends IRCMsg
{
	/** Reason why parsing failed */
	private String failReason;

	/** @param failReason Reason why parsing failed */
	public UnknownIRCMsg(String failReason)
	{
		this.failReason=failReason;
	}

	/** @return Reason why parsing failed */
	public String getFailReason()
	{
		return failReason;
	}

	/** @return Entire content of failed line */
	public String getFailedLine()
	{
		return getLineISO();
	}

	@Override
	public String toString()
	{
		return super.toString()
		+ "  [UnknownIRCMessage]\n"
		+ "  Fail reason: " + getFailReason() + "\n";
	}

	/** Info for scripting */
	public static MessageInfo info = new MessageInfo(UnknownIRCMsg.class, "Unknown data",
		"leafChat generates this event when it receives a line of data from the server which it doesn't understand.")
	{
		@Override
		protected void listScriptingVariables(Variables v)
		{
			super.listScriptingVariables(v);
			v.add("failReason");
			v.add("failedLine");
		}
	};
}
