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

import com.leafdigital.prefsui.api.PreferencesUI;

import leafchat.core.api.*;

/**
 * Plugin with UI preferences (themes page, etc.)
 */
public class UIPrefsPlugin implements Plugin
{
	private PluginContext context;

	@Override
	public void init(PluginContext pc,PluginLoadReporter plr)
		throws GeneralException
	{
		context=pc;

		PreferencesUI pui=context.getSingleton2(PreferencesUI.class);
		pui.registerPage(this,(new InterfaceStylePage(context,false)).getPage());
		pui.registerWizardPage(this,300,(new InterfaceStylePage(context,true)).getPage());
		pui.registerPage(this,(new ThemesPage(context)).getPage());
		pui.registerWizardPage(this,400,(new WizardThemePage(context)).getPage());
		pui.registerPage(this,(new TextDisplayPage(context)).getPage());
	}

	@Override
	public void close() throws GeneralException
	{
	}

	@Override
	public String toString()
	{
		return "UI preferences plugin";
	}
}
