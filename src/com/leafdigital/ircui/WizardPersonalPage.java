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

import util.xml.*;

import com.leafdigital.irc.api.IRCPrefs;
import com.leafdigital.prefs.api.*;
import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/**
 * Setup wizard page that contains user personal details.
 */
@UIHandler("wizard-personal")
public class WizardPersonalPage
{
	private Page p;
	private PluginContext context;

	/** Edit box: quit message */
	public EditBox quitUI;
	/** Edit box: real name */
	public EditBox realnameUI;
	/** Text view: quit message preview */
	public TextView previewQuitUI;
	/** Text view: real name preview */
	public TextView previewRealnameUI;
	/** Label: Info about realname */
	public Label realnameInfoUI;

	private String nickname,username,hostname;

	private PreferencesGroup serverPrefs;

	WizardPersonalPage(PluginContext context) throws GeneralException
	{
		this.context=context;
		UI ui = context.getSingleton2(UI.class);
		p = ui.createPage("wizard-personal", this);

		Preferences p=context.getSingleton2(Preferences.class);
		serverPrefs=p.getGroup(p.getPluginOwner("com.leafdigital.irc.IRCPlugin")).getChild("servers");

		quitUI.setValue(serverPrefs.get(IRCPrefs.PREF_QUITMESSAGE,IRCPrefs.PREFDEFAULT_QUITMESSAGE));
		realnameUI.setValue(serverPrefs.get(IRCPrefs.PREF_REALNAME,IRCPrefs.PREFDEFAULT_REALNAME));
	}

	/**
	 * Action: User has selected this page.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void onSet() throws GeneralException
	{
		nickname=serverPrefs.get(IRCPrefs.PREF_DEFAULTNICK);
		username=serverPrefs.get(IRCPrefs.PREF_USER,IRCPrefs.PREFDEFAULT_USER);
		hostname=WizardProfilePage.getHostname(context);
		realnameInfoUI.setText("	This appears when somebody types <strong>/whois "+nickname+"</strong>.");
		updatePreviewQuit();
		updatePreviewRealname();
	}

	Page getPage()
	{
		return p;
	}

	/**
	 * Action: User focused realname field.
	 */
	@UIAction
	public void focusRealname()
	{
		if(realnameUI.getValue().equals(IRCPrefs.PREFDEFAULT_REALNAME))
			realnameUI.selectAll();
	}

	/**
	 * Action: User changed realname.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void changeRealname() throws GeneralException
	{
		if(realnameUI.getFlag()==EditBox.FLAG_NORMAL)
		{
			serverPrefs.set(IRCPrefs.PREF_REALNAME,realnameUI.getValue(),IRCPrefs.PREFDEFAULT_REALNAME);
			updatePreviewRealname();
		}
	}

	/**
	 * Action: User focused quit message field.
	 */
	@UIAction
	public void focusQuit()
	{
		if(quitUI.getValue().equals(IRCPrefs.PREFDEFAULT_QUITMESSAGE))
			quitUI.selectAll();
	}

	/**
	 * Action: User changed quit message.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void changeQuit() throws GeneralException
	{
		if(quitUI.getFlag()==EditBox.FLAG_NORMAL)
		{
			serverPrefs.set(IRCPrefs.PREF_QUITMESSAGE,quitUI.getValue(),IRCPrefs.PREFDEFAULT_QUITMESSAGE);
			updatePreviewQuit();
		}
	}

	private void updatePreviewRealname() throws GeneralException
	{
		previewRealnameUI.clear();
		previewRealnameUI.addLine(
			"<nick>"+XML.esc(nickname)+"</nick> ("+XML.esc(username)+"@"+
			XML.esc(hostname)+") - "+XML.esc(realnameUI.getValue()));
	}
	private void updatePreviewQuit() throws GeneralException
	{
		previewQuitUI.clear();
		previewQuitUI.addLine(	ChatWindow.EVENTSYMBOL+"<nick>"+XML.esc(nickname)+
			"</nick> <quit>has quit IRC</quit>"+(quitUI.getValue().length()==0 ? "" :
				": "+XML.esc(quitUI.getValue())));
	}
}
