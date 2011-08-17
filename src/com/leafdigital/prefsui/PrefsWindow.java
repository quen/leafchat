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
package com.leafdigital.prefsui;

import com.leafdigital.ui.api.*;

import leafchat.core.api.GeneralException;

/**
 * Preferences window.
 */
@UIHandler("options")
public class PrefsWindow
{
  private PrefsTool pt;
  private Window w;

  /**
   * UI: Preference category list.
   */
  public ListBox categoriesUI;
  /**
   * Page: Actual preferences for category.
   */
  public Page categoryPageUI;

  /**
   * @param pt Tool button
   * @throws GeneralException
   */
	public PrefsWindow(PrefsTool pt) throws GeneralException
	{
		this.pt=pt;

		UI u=pt.getContext().getSingleton2(UI.class);
		w=u.createWindow("options", this);
		w.setRemember("tool","prefs");

		Page[] pages=pt.getPlugin().getPages();
		for(int i=0;i<pages.length;i++)
		{
			categoriesUI.addItem(pages[i].getTitle(),pages[i]);
		}

		w.show(false);
	}

	/**
	 * Action: Change category.
	 * @throws GeneralException
	 */
	@UIAction
	public void selectionCategories() throws GeneralException
	{
		Page selected=(Page)categoriesUI.getSelectedData();
		if(selected!=null)
		{
			categoryPageUI.setContents(selected);
		}
	}

	void close()
	{
		w.close();
	}

	void activate()
	{
		w.activate();
	}

	/**
	 * Action: Window closed.
	 */
	@UIAction
	public void windowClosed()
	{
		pt.closed();
	}

	/**
	 * Action: Wizard button.
	 */
	@UIAction
	public void actionWizard()
	{
		w.close();
		pt.getPlugin().showWizard();
	}
}
