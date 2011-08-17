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

import com.apple.eawt.*;
import com.apple.eawt.AppEvent.QuitEvent;

/**
 * Special initialiser for Mac platform only. This is the new version
 * that supports Apple Java since:
 * Java for Mac OS X 10.6 Update 3, Java for Mac OS X 10.5 Update 8
 */
public class MacPlatformNew
{
	private static boolean inThread;

	static
	{
		Application a=Application.getApplication();
		a.setQuitHandler(new QuitHandler()
		{
			@Override
			public void handleQuitRequestWith(QuitEvent event, QuitResponse response)
			{
				// Doc says this can be called multiple times before responding to the
				// first one. Let's only start the process once.
				synchronized(MacPlatformNew.class)
				{
					if(inThread)
					{
						return;
					}
					inThread = true;
				}
				try
				{
					if(!StartupHandler.sendRequestShutdownMsg())
					{
						// User cancelled
						response.cancelQuit();
					}
					else
					{
						StartupHandler.sendShutdownMsg(false);
						response.performQuit();
					}
				}
				finally
				{
					synchronized(MacPlatformNew.class)
					{
						inThread = false;
					}
				}
			}
		});
	}

}
