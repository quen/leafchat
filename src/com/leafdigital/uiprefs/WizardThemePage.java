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

import com.leafdigital.ui.api.*;

import leafchat.core.api.PluginContext;

/**
 * Wizard page with theme option.
 */
@UIHandler("wizard-theme")
public class WizardThemePage
{
	private PluginContext context;
	private Page p;

	/**
	 * Pic: Leaves theme
	 */
	public Pic leavesPicUI;
	/**
	 * Pic: standard theme
	 */
	public Pic sharedPicUI;
	/**
	 * Radio button: leaves
	 */
	public RadioButton leavesUI;
	/**
	 * Radio button: standard
	 */
	public RadioButton sharedUI;

	WizardThemePage(PluginContext context)
	{
		this.context=context;

		UI ui=context.getSingle(UI.class);
		p=ui.createPage("wizard-theme", this);
	}

	/**
	 * Action: Page shown.
	 */
	@UIAction
	public void onSet()
	{
		UI ui=context.getSingle(UI.class);
		Theme[] available=ui.getAvailableThemes();
		for(int i=0;i<available.length;i++)
		{
			if(available[i].getLocation().getName().equals("leaves.leafChatTheme"))
			{
				leavesPicUI.setThemeFile(available[i],"theme.png");
				if(available[i]==ui.getTheme())
					leavesUI.setSelected();
			}
			if(available[i].getLocation().getName().equals("shared.leafChatTheme"))
			{
				sharedPicUI.setThemeFile(available[i],"theme.png");
				if(available[i]==ui.getTheme())
					sharedUI.setSelected();
			}
		}
	}

	/**
	 * @return Page
	 */
	public Page getPage()
	{
		return p;
	}

	/**
	 * Action: Choose leaves theme.
	 */
	@UIAction
	public void actionLeaves()
	{
		setTheme("leaves");
	}

	/**
	 * Action: Choose standard theme.
	 */
	@UIAction
	public void actionShared()
	{
		setTheme("shared");
	}

	private void setTheme(String name)
	{
		UI ui=context.getSingle(UI.class);
		Theme[] available=ui.getAvailableThemes();
		for(int i=0;i<available.length;i++)
		{
			if(available[i].getLocation().getName().equals(name+".leafChatTheme"))
			{
				ui.setTheme(available[i]);
			}
		}
	}
}
