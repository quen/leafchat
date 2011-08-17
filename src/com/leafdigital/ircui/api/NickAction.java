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

import com.leafdigital.irc.api.*;

import leafchat.core.api.PluginContext;

/** Convenience class for IRCActions that take a single nickname or multiple
 * selected nicknames. */
public class NickAction extends AbstractIRCAction implements IRCAction
{
	private String command;
	private PluginContext pc;

	/**
	 * @param pc Context
 	 * @param name Menu name
	 * @param category Category constant
	 * @param order Order within category
	 * @param command IRC command to run, with %%NICK%% for nickname, for each name
	 */
	public NickAction(PluginContext pc,String name,int category,int order,String command)
	{
		super(name,category,order);
		this.command=command;
		this.pc=pc;
	}

	@Override
	public void run(Server s,String contextChannel,String contextNick,
		String selectedChannel,String[] selectedNicks,MessageDisplay caller)
	{
		String[] selected=selectedNicks==null ? new String[] {contextNick} : selectedNicks;
		Commands c=pc.getSingleton2(Commands.class);

		for(int i=0;i<selected.length;i++)
		{
			String nick=selected[i];
			c.doCommand(
				command.replaceAll("%%NICK%%",nick),
				s,contextNick==null?null:new IRCUserAddress(contextNick,false),contextChannel,caller,false);
		}
	}
}
