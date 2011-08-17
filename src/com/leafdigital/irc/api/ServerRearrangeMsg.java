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
import util.xml.XML;

/**
 * Sent to inform the UI part that the server preferences could be rearranged if
 * user confirms it. IRC UI plugin will display a dialog in response to this
 * message.
 */
public class ServerRearrangeMsg extends ServerMsg
{
	/** Text of question for user and of buttons */
	private String text, buttonConfirm, buttonOther;

	/** User response, if any */
	private int result=NONE;

	/** No user response yet */
	public final static int NONE = 0;

	/** User confirms inclusion */
	public final static int CONFIRM = 1;

	/** User rejects inclusion */
	public final static int REJECT = 2;

	/**
	 * Constructor called if a new network would be created.
	 * @param s Server that sent this line
	 * @param host Hostname of server
	 * @param network Name of newly-created network
	 * @param other Other server that would be added alongside
	 */
	public ServerRearrangeMsg(Server s, String host, String network, String other)
	{
		super(s, NOSEQUENCE);

		text = "<para>This server reports its identity as <strong>" + XML.esc(host) + "</strong>.</para>" +
		  "<para>It probably belongs with " +
		  "<strong>" + XML.esc(other) + "</strong> in the <strong>" + XML.esc(network) + "</strong> network.</para>" +
			"<para>If you don't think these servers belong in the same network, it is important " +
			"that you do not include them together; including them could make your nickname password vulnerable.</para>";
		buttonConfirm = "Include";
		buttonOther = "Do not include";
	}

	/**
	 * Constructor called if server would be added to existing network
	 * @param s Server that sent this line
	 * @param host Hostname of server
	 * @param network Name of network for addition
	 */
	public ServerRearrangeMsg(Server s, String host, String network)
	{
		super(s, NOSEQUENCE);

		text = "<para>This server reports its identity as <strong>" + XML.esc(host) + "</strong>.</para>" +
			"<para>It probably belongs in the <strong>" + XML.esc(network) + "</strong> network.</para>"+
			"<para>If you don't think this server belongs in that network, it is important " +
			"that you do not include it; including it could make your nickname password vulnerable.</para>";
		buttonConfirm = "Include";
		buttonOther = "Do not include";
	}

	/** @return Text (XML) for dialog */
	public String getText()	{	return text;	}

	/** @return Text for confirm button */
	public String getButtonConfirm()	{	return buttonConfirm;	}
	/** @return Text for other button */
	public String getButtonOther()	{	return buttonOther;	}

	/** Call to indicate that user clicked 'yes' to approve this change */
	public void confirm()
	{
		result = CONFIRM;
	}

	/** Call to indicate user rejected change */
	public void reject()
	{
		result = REJECT;
	}

	/** @return User's response */
	public int getResult()
	{
		return result;
	}

	/**
	 * Used internally to correct the sequence number.
	 * @param sequence Right sequence number
	 */
	public void updateSequence(int sequence)
	{
		setSequence(SEQUENCE, sequence);
	}

	/**
	 * Information about message for scripting system.
	 */
	public static MessageInfo info = new MessageInfo(ServerRearrangeMsg.class,
		"Rearrange",
		"<para>Event sent when the system suggests rearranging server preferences "
		+ "to place it inside a network in the tree.</para<")
	{
		@Override
		public boolean allowScripting()
		{
			return false;
		}
	};
}
