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

/** CTCP request sent to a user */
public class UserCTCPRequestIRCMsg extends UserIRCMsg
{
	/** Parameters of message (everything after command + space) */
	private byte[] text;

	/** Command */
	private String request;

	/** True if client has sent a response */
	private boolean responded;

	/**
	 * @param source User that is in the message prefix
	 * @param nick Nickname of target
	 * @param request CTCP request command
	 * @param text Text/parameters of message
	 */
	public UserCTCPRequestIRCMsg(IRCUserAddress source,String nick,String request,byte[] text)
	{
		super(source,nick);

		this.request=request.toUpperCase();
		this.text=text;
	}

	/** @return Text or parameters of message (everything after command); zero-length if none */
	public byte[] getText() { return text; }

	/** @return CTCP request command (upper-case) */
	public String getRequest() { return request; }

	/**
	 * @return True if the client has sent a response (so that it will still
	 * be displayed, this message is not marked handled when a response is sent).
	 */
	public boolean hasResponded() { return responded; }

	/** Indicates that the client has sent a response to this message */
	public void markResponded() { responded=true; }

	@Override
	public String toString()
	{
		return super.toString()+
		  "  [UserCTCPRequestIRCMessage]\n"+
			"  Request: "+request+"\n"+
			"  Params: "+IRCMsg.convertISO(getText())+"\n";
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(UserCTCPRequestIRCMsg.class,"CTCP request",
		"CTCP requests (such as a CTCP PING) sent to you directly.")
	{
		@Override
		protected void listAppropriateFilters(Collection<FilterInfo> list)
		{
			super.listAppropriateFilters(list);
			list.add(CTCPCommandFilter.info);
		}
		@Override
		protected void listScriptingVariables(Variables v)
		{
			super.listScriptingVariables(v);
			v.add("request");
			v.add("params",String.class,"String params=msg.convertEncoding(msg.getText());");
		}
	};
}
