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
package leafchat.startup;

import com.leafdigital.audio.AudioPlugin;
import com.leafdigital.dcc.DCCPlugin;
import com.leafdigital.donations.DonationsPlugin;
import com.leafdigital.encryption.EncryptionPlugin;
import com.leafdigital.highlighter.HighlighterPlugin;
import com.leafdigital.idle.IdlePlugin;
import com.leafdigital.irc.IRCPlugin;
import com.leafdigital.ircui.IRCUIPlugin;
import com.leafdigital.logs.LogsPlugin;
import com.leafdigital.monitor.MonitorPlugin;
import com.leafdigital.net.NetPlugin;
import com.leafdigital.notification.NotificationPlugin;
import com.leafdigital.prefs.PrefsPlugin;
import com.leafdigital.prefsui.PrefsUIPlugin;
import com.leafdigital.scripting.ScriptingPlugin;
import com.leafdigital.ui.UIPlugin;
import com.leafdigital.uiprefs.UIPrefsPlugin;
import com.leafdigital.updatecheck.UpdateCheckPlugin;

import leafchat.core.*;
import leafchat.core.api.GeneralException;

/**
 * Special version of startup that assumes all classes are available on system
 * classloader
 */
public class IDEStartupHandler extends StartupHandler
{
	/**
	 * Main method.
	 * @param args Ignored
	 * @throws Exception Any startup error
	 */
	public static void main(String[] args) throws Exception
	{
		SystemLogSingleton.useConsole=true;
		StartupHandler.initStatics(true);
		new IDEStartupHandler();
	}

	@Override
	protected void classloaderInit(PluginManager pm)
	{
	}

	@Override
	protected void pluginInit(PluginManager pm) throws GeneralException
	{
		pm.fakePluginInit(new PrefsPlugin(),ss);
		pm.fakePluginInit(new UIPlugin(),ss);
		pm.fakePluginInit(new LogsPlugin(),ss);
		pm.fakePluginInit(new PrefsUIPlugin(),ss);
		pm.fakePluginInit(new NetPlugin(),ss);
		pm.fakePluginInit(new IRCPlugin(),ss);
		pm.fakePluginInit(new AudioPlugin(),ss);
		pm.fakePluginInit(new NotificationPlugin(),ss);
		pm.fakePluginInit(new IdlePlugin(),ss);
		pm.fakePluginInit(new IRCUIPlugin(),ss);
		pm.fakePluginInit(new DCCPlugin(),ss);
		pm.fakePluginInit(new UIPrefsPlugin(),ss);
		pm.fakePluginInit(new ScriptingPlugin(),ss);
		pm.fakePluginInit(new DonationsPlugin(),ss);
		pm.fakePluginInit(new UpdateCheckPlugin(),ss);
		pm.fakePluginInit(new EncryptionPlugin(),ss);
		pm.fakePluginInit(new MonitorPlugin(),ss);
		pm.fakePluginInit(new HighlighterPlugin(),ss);
	}

}
