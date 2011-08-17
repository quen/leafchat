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

/** Notice sent to a channel */
public class ChanCTCPRequestIRCMsg extends ChanIRCMsg
{
	/** Parameters of message (everything after command + space) */
	private byte[] text;

	/** Command */
	private String request;

	/**
	 * @param source User that is in the message prefix
	 * @param channel Channel name
	 * @param request CTCP request command
	 * @param text Text/parameters of message
	 */
	public ChanCTCPRequestIRCMsg(IRCUserAddress source,String channel,String request,byte[] text)
	{
		super(source,channel);

		this.request=request;
		this.text=text;
	}

	/** @return Text or parameters of message (everything after command) */
	public byte[] getText() { return text; }

	/** @return CTCP request command */
	public String getRequest() { return request; }

	@Override
	public String toString()
	{
		return super.toString()+
		  "  [ChanCTCPRequestIRCMessage]\n"+
			"  Request: "+request+"\n"+
			"  Params: "+IRCMsg.convertISO(getText())+"\n";
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(ChanCTCPRequestIRCMsg.class,"CTCP request",
		"CTCP requests (such as a CTCP PING) sent to an entire channel.")
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
