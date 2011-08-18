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

import java.io.*;
import java.net.MalformedURLException;
import java.util.zip.ZipException;

import util.PlatformUtils;
import util.xml.XML;

import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/** Options page where user can select theme. */
@UIHandler("themes")
public class ThemesPage
{
	private PluginContext context;
	private Page p;

	/** UI: Theme list table */
	public Table themesUI;
	/** UI: Name of selected theme */
	public Label themeNameUI;
	/** UI: Description of selected theme */
	public Label themeDescriptionUI;
	/** UI: Authors of selected theme */
	public Label themeAuthorsUI;
	/** UI: Location of selected theme */
	public Label themeLocationUI;
	/** UI: Picture for selected theme */
	public Pic themePicUI;

	/** Current themes */
	private Theme[] themes;

	ThemesPage(PluginContext context)
	{
		this.context = context;

		UI ui = context.getSingle(UI.class);
		p = ui.createPage("themes", this);
	}

	/** @return Page object for adding to dialog */
	public Page getPage()
	{
		return p;
	}

	/**
	 * Checks whether the list of available themes has changed. If it has, redoes
	 * onSet to initialise the list.
	 * @return True if the list changed
	 */
	private boolean checkThemesChange()
	{
		Theme[] oldThemes = themes;
		UI ui=context.getSingle(UI.class);
		themes = ui.getAvailableThemes();

		boolean changed = false;
		if(themes.length != oldThemes.length)
		{
			changed = true;
		}
		else
		{
			for(int i=0; i<themes.length; i++)
			{
				if(!themes[i].equals(oldThemes[i]))
				{
					changed = true;
					break;
				}
			}
		}

		if(changed)
		{
			onSet();
			return true;
		}
		else
		{
			return false;
		}
	}

	/** Callback: Page has been selected in options. */
	@UIAction
	public void onSet()
	{
		UI ui = context.getSingle(UI.class);
		themes = ui.getAvailableThemes();
		Theme current = ui.getTheme();
		themesUI.clear();
		for(int i=0; i<themes.length; i++)
		{
			int index = themesUI.addItem();
			themesUI.setString(index, 0, themes[i].getStringProperty(Theme.META, Theme.META_NAME, "?"));
			if(themes[i] == current)
			{
				themesUI.setBoolean(index, 1, true);
				themesUI.setEditable(index, 1, false);
			}
			else
			{
				themesUI.setBoolean(index, 1, false);
				themesUI.setEditable(index, 1, true);
			}
		}
		selectThemes();
	}

	/** Callback: Change in selected themes. */
	@UIAction
	public void selectThemes()
	{
		if(checkThemesChange())
		{
			return;
		}

		int selected = themesUI.getSelectedIndex();
		if(selected == Table.NONE)
		{
			themeNameUI.setText("(Select theme for information)");
			themeDescriptionUI.setText("");
			themeAuthorsUI.setText("");
			themeLocationUI.setText("");
			themePicUI.setProperty(null);
		}
		else
		{
			Theme t = themes[selected];
			themeNameUI.setText("<strong>"+XML.esc(t.getStringProperty(Theme.META, Theme.META_NAME, "?"))+"</strong>");
			themeAuthorsUI.setText(XML.esc(t.getStringProperty(Theme.META, Theme.META_AUTHORS, "?")));
			themeDescriptionUI.setText(XML.esc(t.getStringProperty(Theme.META, Theme.META_DESCRIPTION, "?")));
			try
			{
				themeLocationUI.setText(XML.esc(t.getLocation().getCanonicalPath()));
			}
			catch(IOException e)
			{
				themeLocationUI.setText("?");
			}
			themePicUI.setThemeFile(t, "theme.png");
		}
	}

	/**
	 * Callback: Change in value in themes table.
	 * @param index Row
	 * @param column Column
	 * @param before Previous value
	 */
	@UIAction
	public void changeThemes(int index, int column, Object before)
	{
		if(checkThemesChange())
		{
			return;
		}

		UI ui = context.getSingle(UI.class);
		Theme current = ui.getTheme();
		for(int i=0; i<themes.length; i++)
		{
			if(current == themes[i])
			{
				// Turn off, and re-enable editing, existing column
				themesUI.setBoolean(i, 1, false);
				themesUI.setEditable(i, 1, true);
			}
		}

		// Set theme
		ui.setTheme(themes[index]);

		// Disable editing for new col
		themesUI.setEditable(index, 1, false);
	}

	/**
	 * Callback: Install button
	 * @throws GeneralException
	 */
	@UIAction
	public void actionInstall() throws GeneralException
	{
		UI ui = context.getSingle(UI.class);
		File selected = ui.showFileSelect(p.getOwner(), "Install theme", false,
			new File(PlatformUtils.getDownloadFolder()), null,
			new String[] {".leafChatTheme"},
			"leafChat themes");
		if(selected != null)
		{
			try
			{
				ui.installUserTheme(selected);
				onSet();
			}
			catch(GeneralException e)
			{
				String suggestion;
				if(e.getCause() instanceof ZipException)
				{
					suggestion="The theme doesn't seem to be a valid .zip file. " +
							"Please compress your themes using a standard zip program. " +
							"The most reliable way to do this is with a command-line program " +
							"such as that available from <url>www.info-zip.org</url>.";
				}
				else
				{
					suggestion="Perhaps the theme " +
						"is invalid? Make sure that the structure of your .zip file is " +
						"identical to the provided themes (e.g. files are included in the " +
						"root of the zip and not in folders). See the help page for more " +
						"information.";
				}

				ui.showUserError(p.getOwner(), "Error loading theme",
					"<para>An error occurred while loading the theme:</para>" +
					"<para><key>"+XML.esc(e.getMessage())+"</key></para>"+
					"<para>"+suggestion+"</para>");
			}
		}
	}

	/**
	 * Callback: Help button
	 */
	@UIAction
	public void actionHelp()
	{
		try
		{
			PlatformUtils.showBrowser(
				(new File("help/themes/index.html")).toURI().toURL());
		}
		catch(MalformedURLException e)
		{
			ErrorMsg.report("Failed to open help", e);
		}
		catch(IOException e)
		{
			ErrorMsg.report(e.getMessage(), e.getCause());
		}
	}
}
