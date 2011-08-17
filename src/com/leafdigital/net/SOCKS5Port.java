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

import com.leafdigital.net.api.Network.Port;

import leafchat.core.api.PluginContext;

/** SOCKS5 implementation of listening port */
public class SOCKS5Port extends SOCKS5Base implements Port
{
	private InetAddress publicAddress;
	private int publicPort;

	private InetAddress connectedAddress;

	/**
	 * @param remoteHost Remote address that can connect
	 * @param context Plugin context
	 * @throws IOException Error creating port
	 */
	public SOCKS5Port(String remoteHost,PluginContext context) throws IOException
	{
		super(context);

		boolean ok=false;
		try
		{
			output.write(5); // Version 5
			output.write(2); // 'Bind'
			output.write(0); // Reserved
			writeAddress(remoteHost,0); // Hopefully we can send port 0 at least!
			output.flush();

			input.read(); // Version number, ignored
			int reply=input.read();
			if(reply!=0) 	throw new SOCKS5Exception(reply);
			input.read(); // Reserved byte, ignored
			InetSocketAddress result=readAddress(); // Bind address
			publicAddress=result.getAddress();
			publicPort=result.getPort();

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

	@Override
	public Socket accept() throws IOException
	{
		return accept(0);
	}

	@Override
	public Socket accept(int timeout) throws IOException
	{
		if(connectedAddress!=null) throw new IOException("May not call accept() twice from SOCKS5 port");
		setSoTimeout(timeout);
		try
		{
			input.read(); // Version number (ignored)
			int reply=input.read();
			if(reply!=0) 	throw new SOCKS5Exception(reply);
			input.read(); // Reserved byte, ignored
			InetSocketAddress result=readAddress(); // Bind address
			connectedAddress=result.getAddress();
			return this;
		}
		catch(SocketTimeoutException e)
		{
			throw e;
		}
		catch(IOException e)
		{
			throw new SOCKS5Exception("Error communicating with proxy, perhaps a listening port timeout",e);
		}
	}

	@Override
	public void close() throws IOException
	{
		// Don't really close the socket! This is just supposed to close the
		// 'listening' socket, and there isn't one, so it does nothing.
	}

	@Override
	public InetAddress getInetAddress()
	{
		return connectedAddress!=null ? connectedAddress : super.getInetAddress();
	}

	@Override
	public InetAddress getPublicAddress()
	{
		return publicAddress;
	}

	@Override
	public int getPublicPort()
	{
		return publicPort;
	}
}
