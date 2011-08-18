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
package com.leafdigital.ircui;

import java.awt.EventQueue;

import javax.swing.SwingUtilities;

import com.leafdigital.idle.api.Idle;
import com.leafdigital.irc.api.*;
import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/** Tool that appears in top bar to allow users to manually type IRC commands. */
@UIHandler("entry")
public class EntryTool implements PageTool
{
	private PluginContext context;
	private Page p;

	/** UI: Command box */
	public EditBox commandUI;
	/** UI: Server dropdown */
	public Dropdown serverUI;
	/** UI: Close button */
	public Pic closeUI;

	/**
	 * @param context Plugin context
	 */
	public EntryTool(PluginContext context)
	{
		this.context = context;

		Runnable r=new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					init();
				}
				catch(GeneralException e)
				{
					ErrorMsg.report("Error setting up command-entry tool", e);
				}
			}
		};
		if(EventQueue.isDispatchThread())
		{
			r.run();
		}
		else
		{
			SwingUtilities.invokeLater(r);
		}
	}

	private void init() throws GeneralException
	{
		UI ui = context.getSingle(UI.class);

		p = ui.createPage("entry", this);

		// Start listening for server changes
		context.requestMessages(ServerMsg.class, this, null, Msg.PRIORITY_NORMAL);

		commandUI.setTabCompletion(((IRCUIPlugin)context.getPlugin()).newTabCompletion(null));
		commandUI.setRemember("tool", "entry");

		updateServers();
	}

	@Override
	public void removed()
	{
		context.unrequestMessages(null, this, PluginContext.ALLREQUESTS);
	}

	@Override
	public Page getPage()
	{
		while(p == null)
		{
			try
			{
				Thread.sleep(100);
			}
			catch(InterruptedException ie)
			{
			}
		}
		return p;
	}

	@Override
	public int getDefaultPosition()
	{
		return 10000;
	}

	/**
	 * Server message: changes server dropdown.
	 * @param msg Message
	 * @throws GeneralException
	 */
	public void msg(ServerMsg msg) throws GeneralException
	{
		updateServers();
	}

	private void updateServers() throws GeneralException
	{
		// Get connected servers
		Connections c = context.getSingle(Connections.class);
		Server[] servers = c.getConnected();

		// Get current value
		Server before = (Server)serverUI.getSelected();

		// Update list
		serverUI.clear();
		for(int i=0; i<servers.length; i++)
		{
			serverUI.addValue(servers[i], servers[i].getReportedOrConnectedHost());
			if(servers[i] == before)
			{
				serverUI.setSelected(servers[i]);
			}
		}
		if(servers.length == 0)
		{
			serverUI.addValue(null, "Not connected");
		}

		// Enable/disable server dropdown
		serverUI.setEnabled(servers.length > 1);

		// Show/hide X button
		closeUI.setVisible(servers.length > 0);

		// May affect sizing of toolbar
		context.getSingle(UI.class).resizeToolbar();
	}

	/**
	 * UI action: User presses Return in command box.
	 * @throws GeneralException
	 */
	@UIAction
	public void enterCommand() throws GeneralException
	{
		String command = commandUI.getValue();
		if(command.length() == 0)
		{
			return;
		}

		Server s = (Server)serverUI.getSelected();
		Commands c = context.getSingle(Commands.class);

		commandUI.setValue("");

		IRCUIPlugin ip = (IRCUIPlugin)context.getPlugin();
		context.getSingle(Idle.class).userAwake(Idle.AWAKE_COMMAND);
		c.doCommand(command, s, null, null, ip.getMessageDisplay(s), true);
	}

	/**
	 * UI action: User clicks Close box.
	 * @throws GeneralException
	 */
	@UIAction
	public void actionClose() throws GeneralException
	{
		// Disconnect from server
		Server s = (Server)serverUI.getSelected();
		Commands c = context.getSingle(Commands.class);
		IRCUIPlugin ip = (IRCUIPlugin)context.getPlugin();
		c.doCommand("/quit", s, null, null, ip.getMessageDisplay(s), true);
	}
}
