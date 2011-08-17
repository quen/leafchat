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
package com.leafdigital.ircui.api;

import java.util.*;

import com.leafdigital.irc.api.*;

import leafchat.core.api.*;

/**
 * Message that is sent when building a list of actions, for example when somebody
 * right-clicks on a nickname or the like. Receivers should call
 * {@link #addIRCAction(IRCAction)} if they have a possible action that
 * applies in that context.
 */
public class IRCActionListMsg extends Msg
{
	private LinkedList<IRCAction> list = new LinkedList<IRCAction>();
	private String[] selectedNicks;
	private String selectedChannel;
	private String contextNick,contextChannel;
	private Server server;

	/**
	 * @param server Server of window this message is for
	 * @param contextChannel Context channel
	 * @param contextNick Context nickname
	 * @param selectedChannel Channel of window this message is for; null if it's not a chan
	 * @param selectedNicks Nicknames this message is for; null if none
	 */
	public IRCActionListMsg(Server server,
		String contextChannel, String contextNick,
		String selectedChannel, String[] selectedNicks)
	{
		super();
		this.server=server;
		this.selectedChannel=selectedChannel;
		this.selectedNicks=selectedNicks;
		this.contextChannel=contextChannel;
		this.contextNick=contextNick;
	}

	/**
	 * For use by whoever despatched the message, after its despatch.
	 * @return All the actions that were added
	 */
	public Collection<IRCAction> getIRCActions()
	{
		return list;
	}

	/**
	 * Adds an action to the list.
	 * @param a Action
	 */
	public void addIRCAction(IRCAction a)
	{
		list.add(a);
	}

	/** @return True if the action list refers to a nickname (either selected or context) */
	public boolean hasSingleNick()
	{
		return (selectedNicks!=null && selectedNicks.length==1) ||
		  (selectedNicks==null && contextNick!=null);
	}
	/** @return True if a single nickname, or any of multiple nicknames, is not
	 * the current user's */
	public boolean notUs()
	{
		String us=getServer().getOurNick();
		if(hasSingleNick())
		{
			return !us.equals(getSingleNick());
		}
		else
		{
			if(selectedNicks!=null)
			{
				for(int i=0;i<selectedNicks.length;i++)
				{
					if(selectedNicks[i].equals(us)) return false;
				}
			}
			else
			{
				if(us.equals(getContextNick())) return false;
			}
			return true;
		}
	}

	/** @return True if there are a number of selected (not context) nicknames */
	public boolean hasSelectedNicks()
	{
		return (selectedNicks!=null && selectedNicks.length>=1);
	}
	/** @return Single nickname */
	public String getSingleNick()
	{
		if(selectedNicks!=null)
			return selectedNicks[0];
		else
			return contextNick;
	}
	/** @return Array of nicknames */
	public String[] getSelectedNicks()
	{
		return selectedNicks;
	}
	/** @return Context nick (e.g. in a message window) */
	public String getContextNick()
	{
		return contextNick;
	}
	/** @return True if the action list refers to a channel */
	public boolean hasChannel()
	{
		return selectedChannel!=null || contextChannel!=null;
	}
	/** @return Channel (either selected or context) */
	public String getChannel()
	{
		return selectedChannel!=null ? selectedChannel : contextChannel;
	}
	/** @return Context channel (e.g. in a channel window) */
	public String getContextChannel()
	{
		return contextChannel;
	}
	/** @return Selected channel e.g. if clicked on */
	public String getSelectedChannel()
	{
		return selectedChannel;
	}
	/** @return Server */
	public Server getServer()
	{
		return server;
	}
}
