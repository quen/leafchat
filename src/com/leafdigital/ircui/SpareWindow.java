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

import util.TimeUtils;
import util.xml.XML;

import com.leafdigital.irc.api.*;
import com.leafdigital.prefs.api.*;
import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/** Spare window that pops up when there are no existing windows for that server */
@UIHandler("sparewindow")
public class SpareWindow extends ServerChatWindow
{
	/** Checkbox user can turn on to automatically close these windows. */
	public CheckBox autoCloseUI;
	private PreferencesGroup group;
	private PluginContext context;
	private int autoCloseID=-1;
	private boolean cancelled=false;

	private final static int CLOSEDELAY=5000;

	/**
	 * @param context Plugin context for messages etc
	 * @param s Server this window is for
	 * @throws GeneralException
	 */
	public SpareWindow(PluginContext context,Server s) throws GeneralException
	{
		super(context, s, "sparewindow", true, true);
		this.context=context;
		setTitle();

		// When this window is opened during server connection, add initial
		// information about connection security
		if(s!=null && !s.isConnectionFinished())
		{
			addLine("Connected to <server>" +
					XML.esc(s.getConnectedIpAddress()) + "</server> using " +
					(s.isSecureConnection() ? "a <key>secure</key> connection."
						: "an <key>unencrypted</key> connection."));
		}

		addLine("<box><line>This window appears when connecting or when no other windows are available for "+
			"the relevant server. You can close it after connecting.</line></box>");
		clearMark();

		Preferences p=context.getSingle(Preferences.class);
		group=p.getGroup(context.getPlugin());
		autoCloseUI.setChecked(
			p.toBoolean(group.get(IRCUIPlugin.PREF_CLOSESPAREWINDOWS,
				IRCUIPlugin.PREFDEFAULT_CLOSESPAREWINDOWS)));

		context.requestMessages(PreferencesChangeMsg.class,this);
	}

	/** Callback: user has turned on/off the autoclose checkbox. */
	@UIAction
	public synchronized void changeAutoClose()
	{
		group.set(IRCUIPlugin.PREF_CLOSESPAREWINDOWS,
			group.getPreferences().fromBoolean(autoCloseUI.isChecked()));
		if(!autoCloseUI.isChecked() && autoCloseID!=-1)
		{
			TimeUtils.cancelTimedEvent(autoCloseID);
			autoCloseID=-1;
		}
		if(autoCloseUI.isChecked() && autoCloseID==-1)
		{
			if(((IRCUIPlugin)getPluginContext().getPlugin()).hasNonSpareWindow(getServer()))
				gotOtherWindow();
		}
	}

	/**
	 * Message: preferences have changed. Used to synchronize the 'close window'
	 * option.
	 * @param msg Message
	 */
	public void msg(PreferencesChangeMsg msg)
	{
		if(msg.getGroup()==group && msg.getName().equals(IRCUIPlugin.PREF_CLOSESPAREWINDOWS))
		{
			autoCloseUI.setChecked(
				group.getPreferences().toBoolean(group.get(IRCUIPlugin.PREF_CLOSESPAREWINDOWS,
					IRCUIPlugin.PREFDEFAULT_CLOSESPAREWINDOWS)));
		}
	}

	synchronized void gotOtherWindow()
	{
		if(autoCloseUI.isChecked() && !cancelled && autoCloseID==-1)
		{
			addLine("<box><line>This window will close automatically in "+
				(CLOSEDELAY/1000)+" seconds.</line></box>");
			autoCloseID=TimeUtils.addTimedEvent(new Runnable()
			{
				@Override
				public void run()
				{
					getWindow().close();
				}
			},CLOSEDELAY,true);
		}
	}

	@Override
	protected void setTitle()
	{
		if(getServer() == null)
		{
			getWindow().setTitle("Spare window (no server)");
		}
		else
		{
			getWindow().setTitle("Spare window ("+getServer().getReportedOrConnectedHost()+")");
		}
	}

	@Override
	@UIAction
	synchronized public void windowClosed() throws GeneralException
	{
		super.windowClosed();
		((IRCUIPlugin)context.getPlugin()).spareWindowClosed(getServer());
		if(autoCloseID!=-1)
		{
			TimeUtils.cancelTimedEvent(autoCloseID);
			autoCloseID=-1;
		}
	}

	@Override
	synchronized public void actionOnActive() throws GeneralException
	{
		super.actionOnActive();
		if(autoCloseID!=-1)
		{
			TimeUtils.cancelTimedEvent(autoCloseID);
			autoCloseID=-1;
			addLine("<line>Close cancelled. This window will now remain open until you close it manually.</line>");
			cancelled=true;
		}
	}

	@Override
	protected void doCommand(Commands c,String sLine) throws GeneralException
	{
		// Should never be called...
		assert false;
	}
	@Override
	protected int getAvailableBytes() throws GeneralException
	{
		// Should never be called...
		assert false;
		return -1;
	}

	@Override
	protected boolean isUs(String sTarget)
	{
		return false;
	}

	@Override
	protected String getLogCategory()
	{
		assert false;
		return null;
	}

	@Override
	protected String getLogItem()
	{
		assert false;
		return null;
	}

}
