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
package com.leafdigital.idle;

import util.PlatformUtils;

import com.leafdigital.irc.api.IRCPrefs;
import com.leafdigital.prefs.api.*;
import com.leafdigital.ui.api.*;

import leafchat.core.api.PluginContext;

/**
 * Preferences options for idle system.
 */
@UIHandler("idle")
public class IdlePage
{
	private PluginContext pc;
	private Page p;

	/**
	 * UI: old version.
	 */
	public Label oldVersionUI;

	/**
	 * UI: enable idle.
	 */
	public CheckBox enableUI;
	/**
	 * UI: auto-cancel.
	 */
	public CheckBox autoCancelUI;
	/**
	 * UI: cross-server idle.
	 */
	public CheckBox multiserverUI;
	/**
	 * UI: count non-idle with messages.
	 */
	public RadioButton actionMessagesUI;
	/**
	 * UI: count non-idle with mouse moves.
	 */
	public RadioButton actionMouseUI;
	/**
	 * UI: how many minutes before idle.
	 */
	public EditBox minutesUI;

	IdlePage(PluginContext pc)
	{
		this.pc=pc;
		UI ui = pc.getSingleton2(UI.class);
		p = ui.createPage("idle", this);
	}

	/**
	 * Action: user clicks multi-server option.
	 */
	@UIAction
	public void changeMultiserver()
	{
		Preferences p=pc.getSingleton2(Preferences.class);
		PreferencesGroup ircPrefs=p.getGroup(p.getPluginOwner("com.leafdigital.irc.IRCPlugin"));
		ircPrefs.set(IRCPrefs.PREF_AWAYMULTISERVER,
			p.fromBoolean(multiserverUI.isChecked()),
			IRCPrefs.PREFDEFAULT_AWAYMULTISERVER);
	}

	/**
	 * Action: user changes other settings.
	 */
	@UIAction
	public void changeSettings()
	{
		Preferences p=pc.getSingleton2(Preferences.class);
		PreferencesGroup pg=p.getGroup(pc.getPlugin());
		boolean isJava15=PlatformUtils.isJavaVersionAtLeast(1,5);

		minutesUI.setEnabled(enableUI.isChecked());
		actionMessagesUI.setEnabled(enableUI.isChecked());
		actionMouseUI.setEnabled(enableUI.isChecked() &&
			isJava15);
		autoCancelUI.setEnabled(enableUI.isChecked());
		oldVersionUI.setVisible(!isJava15);

		pg.set(IdlePlugin.PREF_AUTOAWAY,p.fromBoolean(enableUI.isChecked()),
			IdlePlugin.PREFDEFAULT_AUTOAWAY);
		if(minutesUI.getFlag()==EditBox.FLAG_NORMAL)
		  pg.set(IdlePlugin.PREF_IDLETIME,minutesUI.getValue(),
		  	IdlePlugin.PREFDEFAULT_IDLETIME);
		pg.set(IdlePlugin.PREF_ACTIVE,
			actionMouseUI.isSelected()
			  ? IdlePlugin.PREFVALUE_ACTIVE_MOUSE
			  :	IdlePlugin.PREFVALUE_ACTIVE_COMMAND,
			  IdlePlugin.PREFDEFAULT_ACTIVE);
		pg.set(IdlePlugin.PREF_AUTOUNAWAY,p.fromBoolean(enableUI.isChecked()),
			IdlePlugin.PREFDEFAULT_AUTOUNAWAY);
	}

	/**
	 * Action: enter prefs page.
	 */
	@UIAction
	public void onSet()
	{
		Preferences p=pc.getSingleton2(Preferences.class);
		PreferencesGroup pg=p.getGroup(pc.getPlugin());
		enableUI.setChecked(p.toBoolean(
			pg.get(IdlePlugin.PREF_AUTOAWAY,IdlePlugin.PREFDEFAULT_AUTOAWAY)));
		autoCancelUI.setChecked(p.toBoolean(
			pg.get(IdlePlugin.PREF_AUTOUNAWAY,IdlePlugin.PREFDEFAULT_AUTOUNAWAY)));
		minutesUI.setValue(
			pg.get(IdlePlugin.PREF_IDLETIME,IdlePlugin.PREFDEFAULT_IDLETIME));
		if(pg.get(IdlePlugin.PREF_ACTIVE,IdlePlugin.PREFDEFAULT_ACTIVE).equals(
			IdlePlugin.PREFVALUE_ACTIVE_MOUSE) && PlatformUtils.isJavaVersionAtLeast(1,5))
			actionMouseUI.setSelected();
		else
			actionMessagesUI.setSelected();
		changeSettings();

		PreferencesGroup ircPrefs=p.getGroup(p.getPluginOwner("com.leafdigital.irc.IRCPlugin"));
		multiserverUI.setChecked(
			p.toBoolean(ircPrefs.get(IRCPrefs.PREF_AWAYMULTISERVER,IRCPrefs.PREFDEFAULT_AWAYMULTISERVER)));
	}

	Page getPage()
	{
		return p;
	}
}
