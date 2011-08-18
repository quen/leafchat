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
package com.leafdigital.monitor;

import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/**
 * Monitor output window.
 */
@UIHandler("monitorwindow")
public class MonitorWindow
{
	/** Text view: monitor display */
	public TextView viewUI;
	/** Check box: auto scrolling */
	public CheckBox scrollUI;

	private MonitorPlugin plugin;
	private Window w;

	MonitorWindow(PluginContext context)
	{
		plugin=(MonitorPlugin)context.getPlugin();
		w=context.getSingle(UI.class).createWindow("monitorwindow", this);
		w.show(false);
	}

	void addLine(String xml)
	{
		try
		{
			viewUI.addLine(xml);
			if(scrollUI.isChecked()) viewUI.scrollToEnd();
		}
		catch(GeneralException e)
		{
			throw new BugException("Error adding line to monitor window",e);
		}
	}

	void focus()
	{
		w.activate();
	}

	void close()
	{
		w.close();
	}

	/**
	 * Action: Window closed.
	 */
	@UIAction
	public void windowClosed()
	{
		plugin.windowClosed();
	}
}

