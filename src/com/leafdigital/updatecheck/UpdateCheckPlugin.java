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
package com.leafdigital.updatecheck;

import java.io.IOException;
import java.net.*;

import org.w3c.dom.Document;

import util.PlatformUtils;
import util.xml.*;

import com.leafdigital.prefs.api.*;
import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/**
 * Plugin that checks for leafChat updates.
 */
@UIHandler("update")
public class UpdateCheckPlugin implements Plugin
{
	private final static String 	PREF_LASTCHECK="last-check";

	private final static String
	  CHECKURL="http://www.leafdigital.com/software/leafchat/version.xml",
		DOWNLOADURL="http://www.leafdigital.com/software/leafchat/download.html";

	private final static long TWOWEEKS=14L*24L*60L*60L*1000L;

	private PluginContext context;

	private Dialog d;

	/** Label: Version */
	public Label versionUI;

	@Override
	public void init(PluginContext context, PluginLoadReporter reporter) throws GeneralException
	{
		this.context=context;

		context.requestMessages(SystemStateMsg.class,this);
	}

	private class CheckThread extends Thread
	{
		CheckThread()
		{
			super("Update checker");
			start();
		}

		@Override
		public void run()
		{
			try
			{
				URL u=new URL(CHECKURL+"?installed="+SystemVersion.getBuildVersion());
				Document latest=XML.parse(u.openConnection().getInputStream());
				long
					ourVersion=Long.parseLong(SystemVersion.getBuildVersion()),
					availableVersion=Long.parseLong(XML.getRequiredAttribute(latest.getDocumentElement(),"build"));
				final String version=XML.getRequiredAttribute(latest.getDocumentElement(),"version");
				if(availableVersion<=ourVersion)
				{
					context.log("Check successful: This version is current.");
					return;
				}
				context.log("Check successful: Offering new version.");
				UI ui=context.getSingle(UI.class);
				ui.runInThread(new Runnable()
				{
					@Override
					public void run()
					{
						offerUpdate(version);
					}
				});
			}
			catch(IOException e)
			{
				context.log("Check failed: "+e.getMessage());
			}
		}
	}

	private void offerUpdate(String availableVersion)
	{
		UI ui=context.getSingle(UI.class);
		d=ui.createDialog("update", this);
		versionUI.setText("<key>"+XML.esc(availableVersion)+"</key>");
		d.show(null);
	}

	/**
	 * Message: System state change.
	 * @param msg Message
	 * @throws GeneralException
	 */
	public void msg(SystemStateMsg msg) throws GeneralException
	{
		if(msg.getType()!=SystemStateMsg.UIREADY) return;

		Preferences prefs=context.getSingle(Preferences.class);
		PreferencesGroup group=prefs.getGroup(this);

		// Is this a debug version without a proper number in buildVersion?
		// We don't check those.
		try
		{
			Long.parseLong(SystemVersion.getBuildVersion());
		}
		catch(NumberFormatException nfe)
		{
			return;
		}

		// When did we last check?
		long lastCheck=Long.parseLong(group.get(PREF_LASTCHECK,"0"));
		if(lastCheck==0)
		{
			// Never checked. Let's just update the value and come back in two weeks,
			// don't want to pester them when they just installed.
			group.set(PREF_LASTCHECK,System.currentTimeMillis()+"");
			return;
		}
		if(System.currentTimeMillis() < lastCheck + TWOWEEKS)
		{
			// Not time for a check yet
			return;
		}

		// Okay! Let's check
		group.set(PREF_LASTCHECK,System.currentTimeMillis()+"");
		new CheckThread();
	}

	@Override
	public void close() throws GeneralException
	{
	}

	@Override
	public String toString()
	{
	  // Used to display in system log etc.
		return "Update check plugin";
	}

	/**
	 * Action: Download.
	 */
	@UIAction
	public void actionDownload()
	{
		d.close();
		try
		{
			PlatformUtils.showBrowser(new URL(DOWNLOADURL));
		}
		catch(MalformedURLException e)
		{
			// Can't happen
		}
		catch(IOException e)
		{
			UI ui=context.getSingle(UI.class);
			ui.showUserError(null,"Unable to open browser",
				"leafChat was unable to open your Web browser. Please visit the " +
				"download page manually; it is "+DOWNLOADURL);
		}
	}

	/**
	 * Action: Cancel.
	 */
	@UIAction
	public void actionCancel()
	{
		d.close();
	}
}
