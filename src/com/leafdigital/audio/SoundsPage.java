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
package com.leafdigital.audio;

import java.io.*;

import util.*;
import util.xml.*;

import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/**
 * Preferences options for audio system.
 */
@UIHandler("sounds")
public class SoundsPage
{
	private PluginContext context;
	private Page p;

	/**
	 * UI: list of system sounds.
	 */
	public ListBox systemUI;
	/**
	 * UI: list of user sounds.
	 */
	public ListBox userUI;

	/**
	 * UI: play system sound button.
	 */
	public Button systemPlayUI;
	/**
	 * UI: play user sound button.
	 */
	public Button userPlayUI;

	/**
	 * UI: label with folder information.
	 */
	public Label folderUI;
	/**
	 * UI: folder open button.
	 */
	public Button openUI;

	SoundsPage(PluginContext context)
	{
		this.context = context;
		UI ui = context.getSingle(UI.class);
		p = ui.createPage("sounds", this);
	}

	/**
	 * Action: enter prefs page.
	 * @throws GeneralException Any error with sounds folders
	 */
	@UIAction
	public void onSet() throws GeneralException
	{
		String label = folderUI.getText();
		if(label.contains("%%FOLDER"))
		{
			AudioPlugin plugin = (AudioPlugin)context.getPlugin();
			try
			{
				label = label.replace("%%FOLDER%%",
					XML.esc(plugin.getSoundsFolder(false).getCanonicalPath()));
			}
			catch(IOException e)
			{
				throw new GeneralException("Error getting sounds folder location", e);
			}
			folderUI.setText(label);
		}
		if(!PlatformUtils.isJavaVersionAtLeast(1, 6))
		{
			openUI.setVisible(false);
		}

		fillLists();
		selectLists();
	}

	private void fillLists()
	{
		AudioPlugin plugin = (AudioPlugin)context.getPlugin();

		fillList(systemUI, plugin.getSoundsFolder(true));
		fillList(userUI, plugin.getSoundsFolder(false));
	}

	private void fillList(ListBox listBox, File folder)
	{
		String selectedBefore = listBox.getSelected();

		listBox.clear();
		File[] files = IOUtils.listFiles(folder);
		for(int i=0; i<files.length; i++)
		{
			String name = files[i].getName();
			if(!name.endsWith(".ogg"))
			{
				continue;
			}
			name = name.substring(0, name.length()-4);
			listBox.addItem(name);
			if(name.equals(selectedBefore))
			{
				listBox.setSelected(name, true);
			}
		}
	}

	/**
	 * Action: list selection changed.
	 */
	@UIAction
	public void selectLists()
	{
		boolean selectedSystem = systemUI.getSelected() != null;
		systemPlayUI.setEnabled(selectedSystem);

		boolean selectedUser = userUI.getSelected() != null;
		userPlayUI.setEnabled(selectedUser);
	}

	/**
	 * Action: play system sound.
	 * @throws GeneralException Error playing sound
	 */
	@UIAction
	public void actionSystemPlay() throws GeneralException
	{
		playSound(true);
	}

	/**
	 * Action: play user sound.
	 * @throws GeneralException Error playing sound
	 */
	@UIAction
	public void actionUserPlay() throws GeneralException
	{
		playSound(false);
	}

	private void playSound(boolean system) throws GeneralException
	{
		AudioPlugin plugin = (AudioPlugin)context.getPlugin();

		File file = new File(plugin.getSoundsFolder(system),
			(system ? systemUI : userUI).getSelected() + ".ogg");
		if(!file.exists())
		{
			actionRefresh();
			return;
		}

		plugin.play(file);
	}

	/**
	 * Action: refresh lists from disk.
	 */
	@UIAction
	public void actionRefresh()
	{
		fillLists();
		selectLists();
	}

	/**
	 * Action: open user sounds folder.
	 */
	@UIAction
	public void actionOpen()
	{
		AudioPlugin plugin = (AudioPlugin)context.getPlugin();
		PlatformUtils.systemOpen(plugin.getSoundsFolder(false));
	}

	Page getPage()
	{
		return p;
	}
}
