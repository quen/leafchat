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

import java.io.*;
import java.net.*;

import leafchat.core.api.PluginContext;

/** A socket wrapper that includes SOCKS5 connection negotiation */
public class SOCKS5Socket extends SOCKS5Base
{
	private InetAddress proxyExternalAddress;

	/**
	 * @param host Remote host
	 * @param port Remote port
	 * @param context Plugin context
	 * @param timeout Timeout (ms)
	 * @throws IOException Error connecting
	 */
	public SOCKS5Socket(String host,int port,PluginContext context,int timeout)
	  throws IOException
	{
		super(context);

		boolean ok=false;
		try
		{
			output.write(5); // Version 5
			output.write(1); // 'Connect'
			output.write(0); // Reserved
			writeAddress(host,port);
			output.flush();

			// Allow a bit longer for connection
			setSoTimeout(timeout);

			input.read(); // Version number, ignored
			int reply=input.read();
			if(reply!=0)
				throw new SOCKS5Exception(reply);
			input.read(); // Reserved byte, ignored
			// Bind address
			proxyExternalAddress=readAddress().getAddress();

			// And we should be good to go!
			setSoTimeout(0);
			ok=true;
		}
		catch(SocketTimeoutException e)
		{
			throw new SOCKS5Exception("Timeout when communicating with proxy. Check proxy address and port are correct.",e);
		}
		catch(IOException e)
		{
			if(e instanceof SOCKS5Exception) throw e;
			throw new SOCKS5Exception("Error communicating with proxy. Check proxy address and port are correct.",e);
		}
		finally
		{
			if(!ok) close();
		}
	}

	/** @return Address that the proxy reported as its external address */
	InetAddress getProxyExternalAddress()
	{
		return proxyExternalAddress;
	}
}
