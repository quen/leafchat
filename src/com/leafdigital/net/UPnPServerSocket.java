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

import net.sbbi.upnp.impls.InternetGatewayDevice;
import net.sbbi.upnp.messages.UPNPResponseException;

/**
 * UPnP implementation of listening socket.
 */
public class UPnPServerSocket extends ServerSocket
{
	private int externalPort=-1;
	private InternetGatewayDevice gateway;
	private PluginContext context;

	UPnPServerSocket(PluginContext context,InternetGatewayDevice gateway) throws IOException
	{
		// Create and bind socket on any free port
		super(0);
		this.gateway=gateway;
		this.context=context;

		// Try random external ports
    String localHostIP=InetAddress.getLocalHost().getHostAddress();
		for(int attempt=0;attempt<10;attempt++)
		{
			externalPort=(int)(Math.random()*50000)+5000;
			try
			{
				if(gateway.addPortMapping("leafChat listening port",null,
					getLocalPort(),externalPort,localHostIP,0,"TCP"))
					break;
			}
			catch(IOException e)
			{
				throw new UPnPException("Error communicating with UPnP gateway",e);
			}
			catch(UPNPResponseException e)
			{
				throw new UPnPException("Error setting up listening port with UPnP gateway",e);
			}
		}
		context.log("UPnP: reserved external port "+externalPort);
		if(externalPort==-1)
			throw new UPnPException("Unable to reserve listening port with UPnP gateway");
	}

	int getExternalPort()
	{
		return externalPort;
	}

	String getExternalAddress() throws UPnPException
	{
		try
		{
			return gateway.getExternalIPAddress();
		}
		catch(UPNPResponseException e)
		{
			throw new UPnPException("Error obtaining external address from UPnP gateway",e);
		}
		catch(IOException e)
		{
			throw new UPnPException("Error communicating with UPnP gateway",e);
		}
	}

	@Override
	public void close() throws IOException
	{
		super.close();
		try
		{
			gateway.deletePortMapping(null,externalPort,"TCP");
			context.log("UPnP: released external port "+externalPort);
		}
		catch(UPNPResponseException e)
		{
			context.log("Error deleting UPnP port mapping",e);
		}
	}

}
