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
package com.leafdigital.prefs;

import com.leafdigital.prefs.api.Preferences;

import leafchat.core.api.*;

/** Plugin provides preferences support */
public class PrefsPlugin implements Plugin
{
	private PluginContext pc;

	/* (non-Javadoc)
	 * @see leafchat.core.api.Plugin#init(leafchat.core.api.PluginContext)
	 */
	@Override
	public void init(PluginContext pc, PluginLoadReporter plr) throws GeneralException
	{
		this.pc=pc;

		// Register preferences implementation as messageowner and as singleton
		PreferencesImp pi=new PreferencesImp(pc);
		pc.registerMessageOwner(pi);
		pc.registerSingleton(Preferences.class,pi);
	}

	/* (non-Javadoc)
	 * @see leafchat.core.api.Plugin#close()
	 */
	@Override
	public void close() throws GeneralException
	{
		// Make sure any preferences changes are saved
		((PreferencesImp)pc.getSingle(Preferences.class)).close();
	}

	@Override
	public String toString()
	{
		return "Preferences plugin";
	}

}
