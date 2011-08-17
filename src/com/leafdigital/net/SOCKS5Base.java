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

/**
 * Shared code in a socket for communicating with a SOCKS5 server.
 */
public abstract class SOCKS5Base extends Socket
{
	protected InputStream input;
	protected OutputStream output;

	/**
	 * Constructs server connection, sets timeout to 10 seconds, and leaves it
	 * ready to send commands.
	 * @param context Context (for SOCKS details and logging)
	 * @throws IOException If there is any error
	 */
	public SOCKS5Base(PluginContext context) throws IOException
	{
		NetPlugin np=(NetPlugin)context.getPlugin();
		String proxyHost=np.getSOCKSHost();
		int proxyPort=np.getSOCKSPort();
		String username=np.getSOCKSUsername(),password=np.getSOCKSPassword();
		try
		{
			connect(new InetSocketAddress(proxyHost,proxyPort));
			input=super.getInputStream();
			output=super.getOutputStream();
		}
		catch(IOException e)
		{
			throw new SOCKS5Exception("Error connecting to proxy. Check proxy address and port are correct.",e);
		}

		setSoTimeout(10000);

		boolean ok=false;
		try
		{
			// Init and method select
			output.write(5); // Version 5
			output.write(2); // 2 methods
			output.write(0); // No authentication
			output.write(2); // Username/password
			output.flush();

			int version=input.read(); // Ignore
			context.logDebug("Connected to SOCKS5 proxy "+proxyHost+":"+proxyPort+", version "+version);
			int method=input.read();
			if(method==255 || (method!=0 && method!=2))
			{
				throw new SOCKS5Exception("Proxy requires an authentication method leafChat does not support. leafChat cannot use this proxy.");
			}

			// Method-specific negotiation
			if(method==2)
			{
				output.write(1);
				output.write(username.length());
				output.write(username.getBytes("ISO-8859-1"));
				output.write(password.length());
				output.write(password.getBytes("ISO-8859-1"));
				output.flush();
				input.read(); // Version
				int status=input.read();
				if(status!=0)
				{
					throw new SOCKS5Exception("Proxy did not accept username/password combination. Check these are correct.");
				}
			}
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
	public InputStream getInputStream()
	{
		return input;
	}

	@Override
	public OutputStream getOutputStream()
	{
		return output;
	}

	/**
	 * Writes an address to the SOCKS connection in domain-name format (including
	 * sending the type field).
	 * @param host Host name
	 * @param port Port
	 * @throws IOException
	 */
	protected void writeAddress(String host,int port) throws IOException
	{
		output.write(3); // Domain name
		// Actual name
		output.write(host.length());
		output.write(host.getBytes("ISO-8859-1"));
		// Port in network byte order
		int port1=port>>8,port2=port&0xff;
		output.write(port1);
		output.write(port2);
	}

	void readFully(byte[] buffer) throws IOException
	{
		int pos=0;
		while(pos<buffer.length)
		{
			int read=input.read(buffer,pos,buffer.length-pos);
			if(read<1) throw new IOException("Unexpected EOF");
			pos+=read;
		}
	}

	protected InetSocketAddress readAddress() throws IOException
	{
		// Address
		int addressType=input.read();
		InetAddress address;
		switch(addressType)
		{
		case 1: // IPv4
			byte[] ip4=new byte[4];
			readFully(ip4);
			address=Inet4Address.getByAddress(ip4);
			break;

		case 3: // Domain name
			int size=input.read();
			byte[] domain=new byte[size];
			readFully(domain);
			address=InetAddress.getByName(new String(domain,"US-ASCII"));
			break;

		case 4: // IPv6
			byte[] ip6=new byte[16];
			readFully(ip6);
			address=Inet6Address.getByAddress(ip6);
			break;

		default:
			throw new SOCKS5Exception("Unexpected address type in SOCKS server response: "+addressType);
		}

		// Port
		int port=( input.read() << 8 ) | input.read();

		return new InetSocketAddress(address,port);
	}

}