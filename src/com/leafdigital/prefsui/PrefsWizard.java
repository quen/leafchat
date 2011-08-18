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

import leafchat.core.api.PluginContext;

/** Class that handles the wizard dialog. */
@UIHandler("wizard")
public class PrefsWizard
{
	private PluginContext context;
	private Page[] pages;
	private int index=0;

	/** Button: Back */
	public Button backUI;
	/** Button: Next */
	public Button nextUI;
	/** Button: Close */
	public Button closeUI;
	/** Page: current page */
	public Page currentPageUI;
	/** Label: Progress */
	public Label progressUI;

	private Dialog d;

	/**
	 * Construct and display the dialog.
	 * @param context Context
	 * @param p List of pages for dialog
	 */
	public PrefsWizard(PluginContext context,Page[] p)
	{
		this.context=context;
		this.pages=p;

		UI u=context.getSingle(UI.class);
		d = u.createDialog("wizard", this);
		updatePage();
		d.show(null);
	}

	/** Close the dialog */
	@UIAction
	public void close()
	{
		d.close();
	}

	/**
	 * Action: Back.
	 */
	@UIAction
	public void actionBack()
	{
		index--;
		updatePage();
	}

	/**
	 * Action: Next.
	 */
	@UIAction
	public void actionNext()
	{
		index++;
		updatePage();
	}

	private void updatePage()
	{
		currentPageUI.setContents(pages[index]);
		backUI.setEnabled(index>0);
		boolean atEnd=index==pages.length-1;
		nextUI.setEnabled(!atEnd);
		nextUI.setDefault(!atEnd);
		closeUI.setLabel(atEnd ? "Finish" : "Close");
		closeUI.setDefault(atEnd);
		progressUI.setText("<right>"+(index+1)+" of "+(pages.length)+"</right>");
	}

	/**
	 * Action: Window closed.
	 */
	@UIAction
	public void windowClosed()
	{
		// Inform plugin there's no wizard any more
		((PrefsUIPlugin)context.getPlugin()).wizardClosed();
	}

}
