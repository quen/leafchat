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

import com.leafdigital.ui.api.SimpleTool;

import leafchat.core.api.*;

/**
 * Tool (toolbar button) for opening Prefs window.
 */
public class PrefsTool implements SimpleTool
{
	private PluginContext context;

	/**
	 * @param context Context
	 * @throws GeneralException
	 */
	public PrefsTool(PluginContext context) throws GeneralException
	{
		this.context=context;
	}

	@Override
	public void removed()
	{
		if(pw!=null) pw.close();
	}

	PrefsUIPlugin getPlugin() { return (PrefsUIPlugin)context.getPlugin(); }

	PluginContext getContext() { return context; }

	@Override
	public String getLabel()
	{
		return "Options";
	}

	@Override
	public String getThemeType()
	{
		return "optionsButton";
	}

	@Override
	public int getDefaultPosition()
	{
		return 200;
	}

	private PrefsWindow pw=null;

	@Override
	public void clicked() throws GeneralException
	{
		if(pw==null)
		{
			pw=new PrefsWindow(this);
		}
		else
			pw.activate();
	}

	/**
	 * Called by window when closed.
	 */
	public void closed()
	{
		pw=null;
	}

	void closeWindow()
	{
		if(pw!=null) pw.close();
	}
}
