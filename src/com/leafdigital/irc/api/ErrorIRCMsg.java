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

/** IRC 'error' message that is received when the server drops connection. */
public class ErrorIRCMsg extends IRCMsg
{
	/** Message */
	private String message;

	/**
	 * @param message Error message
	 */
	public ErrorIRCMsg(String message)
	{
		this.message=message;
	}

	/** @return Error message */
	public String getMessage() { return message; }

	@Override
	public String toString()
	{
		return super.toString()+
		  "  [ErrorIRCMsg]\n"+
			"  Message: "+getMessage()+"\n";
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(ErrorIRCMsg.class,"Error",
		"The server sends this event when you quit or are otherwise disconnected.")
	{
		@Override
		protected void listScriptingVariables(Variables v)
		{
			super.listScriptingVariables(v);
			v.add("message");
		}
	};
}
