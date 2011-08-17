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

/** Shows the current/changed topic on channel */
public class TopicIRCMsg extends ChanIRCMsg
{
	/** Text of topic */
	private byte[] topic;

	/**
	 * @param source User that is in the message prefix
	 * @param channel Channel name
	 * @param topic Text of topic
	 */
	public TopicIRCMsg(IRCUserAddress source,String channel,byte[] topic)
	{
		super(source,channel);
		this.topic=topic;
	}

	/** @return Text of topic */
	public byte[] getTopic() { return topic; }

	@Override
	public String toString()
	{
		return super.toString()+
		  "  [TopicIRCMessage]\n"+
			"  Text: "+IRCMsg.convertISO(getTopic())+"\n";
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(TopicIRCMsg.class,"Topic",
		"Channel topic (sent when you enter and when it changes).")
	{
		@Override
		protected void listScriptingVariables(Variables v)
		{
			super.listScriptingVariables(v);
			v.add("topic",String.class,"String topic=msg.convertEncoding(msg.getTopic());");
		}
	};

}
