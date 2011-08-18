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

import com.leafdigital.irc.api.IRCPrefs;
import com.leafdigital.prefs.api.*;
import com.leafdigital.ui.api.*;

import leafchat.core.api.PluginContext;

/**
 * Preferences page for miscellaneous IRC settings.
 */
@UIHandler("prefs-misc")
public class PrefsMiscPage
{
	private Page p;

	/** Editbox: extra command characters for if / is hard to type */
	public EditBox extraCommandUI;
	/** Checkbox: enable frequent pings if there are disconnect problems. */
	public CheckBox frequentPingsUI;

	private Preferences prefs;
	private PreferencesGroup group;

	PrefsMiscPage(PluginContext context)
	{
		UI ui=context.getSingle(UI.class);
		p = ui.createPage("prefs-misc", this);

		prefs=context.getSingle(Preferences.class);
		group=prefs.getGroup(prefs.getPluginOwner(IRCPrefs.IRCPLUGIN_CLASS));

		extraCommandUI.setValue(
			group.get(IRCPrefs.PREF_EXTRACOMMANDCHAR,IRCPrefs.PREFDEFAULT_EXTRACOMMANDCHAR));
		frequentPingsUI.setChecked(prefs.toBoolean(
			group.get(IRCPrefs.PREF_FREQUENTPINGS,IRCPrefs.PREFDEFAULT_FREQUENTPINGS)));
	}

	Page getPage()
	{
		return p;
	}

	/**
	 * Action: User changes extra command characters option.
	 */
	@UIAction
	public void changeExtraCommand()
	{
		if(extraCommandUI.getFlag()==EditBox.FLAG_NORMAL)
		{
			group.set(IRCPrefs.PREF_EXTRACOMMANDCHAR,extraCommandUI.getValue(),
				IRCPrefs.PREFDEFAULT_EXTRACOMMANDCHAR);
		}
	}

	/**
	 * Action: User changes frequent pings options.
	 */
	@UIAction
	public void changeFrequentPings()
	{
		group.set(IRCPrefs.PREF_FREQUENTPINGS,prefs.fromBoolean(frequentPingsUI.isChecked()),
			IRCPrefs.PREFDEFAULT_FREQUENTPINGS);
	}
}
