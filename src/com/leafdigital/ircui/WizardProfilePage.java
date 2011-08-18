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

import java.net.InetAddress;

import util.xml.*;

import com.leafdigital.irc.api.IRCPrefs;
import com.leafdigital.net.api.Network;
import com.leafdigital.prefs.api.*;
import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/**
 * Setup wizard page with user's profile (username and nickname).
 */
@UIHandler("wizard-profile")
public class WizardProfilePage
{
	private Page p;
	private PluginContext context;

	/** Edit box: Username */
	public EditBox usernameUI;
	/** Edit box: Nickname */
	public EditBox nicknameUI;
	/** Text view: Preview */
	public TextView previewUI;

	private PreferencesGroup serverPrefs;

	private boolean randomNick;
	private String currentNick;
	private String hostName;

	WizardProfilePage(PluginContext context) throws GeneralException
	{
		this.context=context;
		UI ui = context.getSingle(UI.class);
		p = ui.createPage("wizard-profile", this);
	}

	/**
	 * Action: User switches to this wizard page.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void onSet() throws GeneralException
	{
		Preferences p=context.getSingle(Preferences.class);
		serverPrefs=p.getGroup(p.getPluginOwner("com.leafdigital.irc.IRCPlugin")).getChild("servers");

		hostName=getHostname(context);

		currentNick=serverPrefs.get(IRCPrefs.PREF_DEFAULTNICK,null);
		if(currentNick==null)
		{
			currentNick="lc"+(Math.random()+"").replaceAll("[^0-9]","");
			if(currentNick.length()>9) currentNick=currentNick.substring(0,9);
			PreferencesGroup nickPrefs=serverPrefs.getChild(IRCPrefs.PREFGROUP_NICKS).addAnon();
			nickPrefs.set(IRCPrefs.PREF_NICK,currentNick);
			serverPrefs.set(IRCPrefs.PREF_DEFAULTNICK,currentNick);
			randomNick=true;
		}
		nicknameUI.setValue(currentNick);
		usernameUI.setValue(serverPrefs.get(IRCPrefs.PREF_USER,IRCPrefs.PREFDEFAULT_USER));
		updatePreview();
	}

	private static String cacheHostname;

	static String getHostname(PluginContext context)
	{
		if(cacheHostname==null)
		{
			Network n=context.getSingle(Network.class);
			InetAddress ia=n.getPublicAddress();
			if(ia!=null) cacheHostname=ia.getCanonicalHostName();
			if(cacheHostname==null)
				cacheHostname="your.internet.address";
		}
		return cacheHostname;
	}

	Page getPage()
	{
		return p;
	}

	/**
	 * Action: User focuses nickname field.
	 */
	@UIAction
	public void focusNickname()
	{
		if(randomNick)
		{
			randomNick=false;
			nicknameUI.selectAll();
		}
	}

	/**
	 * Action: User changes nickname
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void changeNickname() throws GeneralException
	{
		if(nicknameUI.getFlag()==EditBox.FLAG_NORMAL)
		{
			PrefsServerPage.changeNick(serverPrefs,currentNick,nicknameUI.getValue());
			currentNick=nicknameUI.getValue();
			updatePreview();
		}
	}

	/**
	 * Action: User focuses username field.
	 */
	@UIAction
	public void focusUsername()
	{
		if(usernameUI.getValue().equals(IRCPrefs.PREFDEFAULT_USER))
			usernameUI.selectAll();
	}

	/**
	 * Action: User changes username
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void changeUsername() throws GeneralException
	{
		if(usernameUI.getFlag()==EditBox.FLAG_NORMAL)
		{
			serverPrefs.set(IRCPrefs.PREF_USER,usernameUI.getValue(),IRCPrefs.PREFDEFAULT_USER);
			updatePreview();
		}
	}

	private void updatePreview() throws GeneralException
	{
		previewUI.clear();
		previewUI.addLine(	ChatWindow.EVENTSYMBOL+"<join>Joined:</join> <nick>"+currentNick+"</nick> ("+
			XML.esc(usernameUI.getValue())+"@"+XML.esc(hostName)+")");
	}
}
