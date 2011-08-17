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
package com.leafdigital.uiprefs;

import util.PlatformUtils;

import com.leafdigital.prefs.api.*;
import com.leafdigital.ui.api.*;

import leafchat.core.api.PluginContext;

/** Prefs page for user to choose an interface style */
@UIHandler({"interfacestyle", "wizard-interfacestyle"})
public class InterfaceStylePage
{
  private Page p;
  private PluginContext context;

  /** UI: Classic style button */
	public RadioButton classicUI;
  /** UI: Tabs style button */
	public RadioButton tabsUI;
  /** UI: Separate window style button */
	public RadioButton separateUI;

	/** UI: Minimise to tray option */
	public CheckBox trayMinimiseUI;

  Page getPage()
  {
		return p;
  }

  /** Page selected */
	@UIAction
  public void onSet()
  {
		UI ui = context.getSingleton2(UI.class);
		switch(ui.getUIStyle())
		{
		case UI.UISTYLE_SINGLEWINDOW :
			classicUI.setSelected();
			break;
		case UI.UISTYLE_MULTIWINDOW:
			separateUI.setSelected();
			break;
		case UI.UISTYLE_TABBED:
			tabsUI.setSelected();
			break;
		}

		if(PlatformUtils.isMac())
		{
			trayMinimiseUI.setVisible(false);
		}
		else if(ui.getUIStyle() == UI.UISTYLE_MULTIWINDOW)
		{
			trayMinimiseUI.setEnabled(false);
		}
		else
		{
			Preferences p = context.getSingleton2(Preferences.class);
			PreferencesGroup pg = p.getGroup(p.getPluginOwner("com.leafdigital.ui.UIPlugin"));
			trayMinimiseUI.setChecked("t".equals(
				pg.get(UIPrefs.PREF_MINIMISE_TO_TRAY, UIPrefs.PREFDEFAULT_MINIMISE_TO_TRAY)));
		}
  }

	InterfaceStylePage(PluginContext context, boolean wizard)
	{
		this.context=context;
		UI ui = context.getSingleton2(UI.class);
		p = ui.createPage(
			wizard ? "wizard-interfacestyle"  : "interfacestyle", this);
	}

	/** UI: Interface radio button clicked */
	@UIAction
	public void actionInterface()
	{
		UI ui = context.getSingleton2(UI.class);
		if(classicUI.isSelected())
		{
			ui.setUIStyle(UI.UISTYLE_SINGLEWINDOW);
			trayMinimiseUI.setEnabled(true);
		}
		else if(separateUI.isSelected())
		{
			ui.setUIStyle(UI.UISTYLE_MULTIWINDOW);
			trayMinimiseUI.setEnabled(false);
			Preferences p = context.getSingleton2(Preferences.class);
			PreferencesGroup pg = p.getGroup(p.getPluginOwner("com.leafdigital.ui.UIPlugin"));
			pg.set(UIPrefs.PREF_MINIMISE_TO_TRAY, "f", UIPrefs.PREFDEFAULT_MINIMISE_TO_TRAY);
		}
		else if(tabsUI.isSelected())
		{
			ui.setUIStyle(UI.UISTYLE_TABBED);
			trayMinimiseUI.setEnabled(true);
		}
	}

	/** UI: Tray minimise option changed */
	@UIAction
	public void changeTrayMinimise()
	{
		Preferences p = context.getSingleton2(Preferences.class);
		PreferencesGroup pg = p.getGroup(p.getPluginOwner("com.leafdigital.ui.UIPlugin"));
		pg.set(UIPrefs.PREF_MINIMISE_TO_TRAY, trayMinimiseUI.isChecked() ? "t" : "f",
			UIPrefs.PREFDEFAULT_MINIMISE_TO_TRAY);
	}
}
