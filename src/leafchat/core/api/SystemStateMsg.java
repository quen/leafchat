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

Copyright 2012 Samuel Marshall.
*/
package leafchat.core.api;

import leafchat.startup.StartupHandler;

/** Message sent when leafChat has loaded all plugins and is ready to start up */
public class SystemStateMsg extends Msg
{
	/** Type: sent when all plugins have been loaded */
	public final static int PLUGINSLOADED=0;

	/** Type: sent when application is shutting down, before plugins are closed */
	public final static int SHUTDOWN=1;

	/** Type: sent when application UI has started, just before splash screen goes */
	public final static int UIREADY=2;

	/** Type: sent when license dialog is being displayed */
	public final static int LICENSEDIALOG = 4;

	/**
	 * Type: sent when user has done something that causes the app to quit, to give
	 * a chance to ask if they're sure.
	 */
	public final static int REQUESTSHUTDOWN=3;

	private int type;

	/**
	 * @param type Type constant
	 */
	public SystemStateMsg(int type)
	{
		this.type=type;
	}

	/**
	 * @return Type constant
	 */
	public int getType()
	{
		return type;
	}

	/**
	 * Sends the REQUESTSHUTDOWN and SHUTDOWN messages.
	 * @return False if user cancelled shutdown. (Otherwise doesn't return.)
	 */
	public static boolean sendShutdown()
	{
		if(!StartupHandler.sendRequestShutdownMsg())
			return false;
		StartupHandler.sendShutdownMsg(true);
		return true; // Doesn't get here
	}
}
