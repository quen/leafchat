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
package com.leafdigital.donations;

import java.io.IOException;
import java.net.*;

import util.PlatformUtils;

import com.leafdigital.prefs.api.*;
import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/**
 * Plugin that handles donation reminders.
 */
@UIHandler({"beg", "didyou"})
public class DonationsPlugin implements Plugin
{
	private final static String
		PREF_NOTDONATEDVERSION="notdonated-version",
		PREF_NOTDONATEDTIME="notdonated-time",
		PREF_NOTDONATEDUSES="notdonated-uses",
		PREF_DONATEDVERSION="donated-version",
		PREF_DONATEDTIME="donated-time",
		PREF_HIDEPROMPTUNTIL="hide-prompt-until",
		PREF_CHECKDONATION="check-donation";

	private final static String DONATEURL="http://www.leafdigital.com/software/leafchat/donate.html";

	private final static long YEAR=365L*24L*60L*60L*1000L;
	private final static long THREEMONTHS=91L*24L*60L*60L*1000L;
	private final static long TWOMONTHS=60L*24L*60L*60L*1000L;

	private PluginContext context;

	private Dialog d;

	/** Donation message. */
	public Label messageUI;

	@Override
	public void init(PluginContext context, PluginLoadReporter reporter) throws GeneralException
	{
		this.context=context;

		context.requestMessages(SystemStateMsg.class,this,Msg.PRIORITY_EARLY);
	}

	/**
	 * Message: System state. Used to display dialog if needed.
	 * @param msg Message
	 * @throws GeneralException
	 */
	public void msg(SystemStateMsg msg) throws GeneralException
	{
		if(msg.getType()!=SystemStateMsg.UIREADY) return;

		UI ui=context.getSingle(UI.class);
		Preferences prefs=context.getSingle(Preferences.class);
		PreferencesGroup group=prefs.getGroup(this);

		// Do we need to check a donation?
		if(group.get(PREF_CHECKDONATION,"").equals("y"))
		{
			group.unset(PREF_CHECKDONATION);

			d = ui.createDialog("didyou", this);
			d.show(null);
			return;
		}

		// Have we donated for the current version? If so do nothing
		if(group.get(PREF_DONATEDVERSION,"").equals(SystemVersion.getBuildVersion()))
			return;

		// OK, did we already store the not-donated version id?
		if(group.get(PREF_NOTDONATEDVERSION,null)==null)
		{
			// Nope. Store it now
			group.set(PREF_NOTDONATEDVERSION,SystemVersion.getBuildVersion());
			group.set(PREF_NOTDONATEDTIME,System.currentTimeMillis()+"");
			group.set(PREF_NOTDONATEDUSES,"1");
			return;
		}
		// Count uses of undonated version
		int undonatedUses=prefs.toInt(group.get(PREF_NOTDONATEDUSES));
		group.set(PREF_NOTDONATEDUSES,prefs.fromInt(undonatedUses+1));

		// If they have donated, we don't ask for a year at least
		String donatedTime=group.get(PREF_DONATEDTIME,null);
		if(donatedTime!=null && System.currentTimeMillis() < Long.parseLong(donatedTime) + YEAR)
			return;

		// We also don't ask until three months and 30 usages since the undonated
		// version (or first run)
		long undonatedTime=Long.parseLong(group.get(PREF_NOTDONATEDTIME));
		if(System.currentTimeMillis() < undonatedTime+ THREEMONTHS)
			return;
		if(undonatedUses<30)
			return;

		// If they told us to piss off, let's take note
		long hidePromptUntil=Long.parseLong(group.get(PREF_HIDEPROMPTUNTIL,"0"));
		if(System.currentTimeMillis()<hidePromptUntil)
			return;
		group.unset(PREF_HIDEPROMPTUNTIL);

		// Okay, now show the dialog
		long undonatedDays=(System.currentTimeMillis()-undonatedTime)/(24*60*60*1000);
		d = ui.createDialog("beg", this);
		if(donatedTime==null)
		{
			messageUI.setText(
				"<para>leafChat is free for noncommercial use, but if you like it, "+
				"please consider making a donation.</para>"+
				"<para>A lot of work went into this program and it would be nice to "+
				"pay for it (think of it like busking). You've "+
				"used leafChat <key>"+undonatedUses+"</key> times over <key>"+
				undonatedDays+"</key> days, so I hope you like it.</para>"+
				"<para>You can donate via PayPal or using any credit card. Click the "+
				"<strong>Donate</strong> button to open the relevant Web page.</para>"+
				"<para>If you don't want to donate now, no problem. Just click "+
				"<strong>Maybe later</strong> and this box will go away for a couple "+
				"of months.</para>"+
				"<small>If you already made a donation without going through the "+
				"program interface, click Donate anyway; you can just close the page "+
				"again.</small>"
				);
		}
		else
		{
			messageUI.setText(
				"<para>Thanks for previously donating to show your appreciation for " +
				"leafChat.</para>"+
				"<para>That was over a year ago. Since then you've updated to at least one new version and  "+
				"have used that new version <key>"+undonatedUses+"</key> times over <key>"+
				undonatedDays+"</key> days.</para>"+
				"<para>If you think you've got enough value out of the updates that "+
				"another donation would be fair, then please click <strong>Donate</strong> "+
				"now. Otherwise? No worries, just click <strong>Maybe later</strong>.</para>"+
				"<small>If you made a donation recently without going through the "+
				"program interface, click Donate anyway; you can just close the page "+
				"again.</small>"
				);
		}
		d.show(null);
	}

	@Override
	public void close() throws GeneralException
	{
	  // Perform any cleanup necessary when your plugin is closed, such as closing
	  // files. (Requests made on the context object are automatically cleaned
	  // up, so you may be able to leave this blank.)
	}

	@Override
	public String toString()
	{
	  // Used to display in system log etc.
		return "Donation prompts plugin";
	}

	/** Action: Donate button. */
	@UIAction
	public void actionDonate()
	{
		d.close();
		Preferences prefs=context.getSingle(Preferences.class);
		PreferencesGroup group=prefs.getGroup(this);
		group.set(PREF_CHECKDONATION,"y");
		try
		{
			PlatformUtils.showBrowser(new URL(DONATEURL));
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
				"donation page manually; it is "+DONATEURL);
		}
	}

	/** Action: Yes, I donated. */
	@UIAction
	public void actionYep()
	{
		d.close();
		Preferences prefs=context.getSingle(Preferences.class);
		PreferencesGroup group=prefs.getGroup(this);
		group.set(PREF_DONATEDTIME,System.currentTimeMillis()+"");
		group.set(PREF_DONATEDVERSION,SystemVersion.getBuildVersion());
		group.unset(PREF_NOTDONATEDTIME);
		group.unset(PREF_NOTDONATEDUSES);
		group.unset(PREF_NOTDONATEDVERSION);
	}

	/** Action: Cancel dialog. */
	@UIAction
	public void actionCancel()
	{
		d.close();
	}

	/** Action: Maybe later (button). */
	@UIAction
	public void actionLater()
	{
		d.close();
		Preferences prefs=context.getSingle(Preferences.class);
		PreferencesGroup group=prefs.getGroup(this);
		group.set(PREF_HIDEPROMPTUNTIL,(System.currentTimeMillis()+TWOMONTHS)+"");
	}
}
