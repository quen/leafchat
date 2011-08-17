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

/** IRC message that has a server as prefix */
public class ServerIRCMsg extends IRCMsg
{
	/** Server that sent the message */
	private String sourceServer;
	/** Target of notice */
	private String target;
	/**
	 * If this message is a response to a tracked server request, this field
	 * contains the ID of the request.
	 */
	private int responseID=Server.UNTRACKED_REQUEST;

	/**
	 * @param sourceServer Server that sent the message
	 * @param target Target of notice
	 */
	public ServerIRCMsg(String sourceServer,String target)
	{
		this.sourceServer=sourceServer;
		this.target=target;
	}

	/**
	 * Used after construction to set the ID of the response (if applicable)
	 * @param responseID ID of response
	 */
	public void setResponseID(int responseID) { this.responseID=responseID; }

	/**
	 * @return Response ID or Server.UNTRACKED_REQUEST
	 */
	public int getResponseID() { return responseID; }

	/** @return Server that sent the message */
	public String getSourceServer() { return sourceServer; }
	/** @return Target of notice */
	public String getTarget() { return target; }

	@Override
	public String toString()
	{
		return super.toString()+
		  "  [ServerIRCMessage]\n"+
			"  Source server: "+getSourceServer()+"\n"+
			"  Target: "+getTarget()+"\n";
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(ServerIRCMsg.class,"Server",
		"Notices sent by the server.")
	{
	};
}
