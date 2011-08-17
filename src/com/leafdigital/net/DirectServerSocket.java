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
package com.leafdigital.net;

import java.io.IOException;
import java.net.*;

import leafchat.core.api.PluginContext;

/**
 * Server socket that listens for connections directly on the current computer
 * and knows its local address.
 */
public class DirectServerSocket extends ServerSocket
{
	private String externalAddress;

	/**
	 * @param context Plugin context
	 * @throws IOException Error creating socket
	 */
	public DirectServerSocket(PluginContext context) throws IOException
	{
		NetPlugin np=(NetPlugin)context.getPlugin();

		// Select port from list
		for(int port=np.getListenPortMin();port<=np.getListenPortMax();port++)
		{
			try
			{
				bind(new InetSocketAddress(port));
				break;
			}
			catch(IOException e)
			{
				// If we've run out of attempts, throw the error, otherwise try next
				if(port==np.getListenPortMax())
				{
					IOException e2=new IOException("No ports available");
					e2.initCause(e);
					throw e;
				}
			}
		}

		// Select address...

		// 1. Do we have a reported address that is useful?
		InetAddress ia=np.getReportedAddress();
		if(ia!=null)
		{
			externalAddress=ia.getHostAddress();
		}
		else
		{
			// 2. Has the user manually set one?
			externalAddress = np.getManualPublicAddress();
			if(externalAddress==null)
			{
				// 3. Get the local address
				ia=InetAddress.getLocalHost();
				if(!NetPlugin.isValidAddress(ia))
					throw new IOException("Unable to determine external IP address. Please set address manually in Connections page.");
				externalAddress=ia.getHostAddress();
			}
		}
	}

	/**
	 * @return External address
	 */
	public String getExternalAddress()
	{
		return externalAddress;
	}
}
