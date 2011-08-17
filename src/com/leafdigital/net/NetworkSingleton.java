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
import java.security.*;
import java.util.*;

import javax.net.ssl.*;

import net.sbbi.upnp.impls.InternetGatewayDevice;

import com.leafdigital.net.api.Network;

import leafchat.core.api.PluginContext;

/** Implements the Network API with support for UPnP and SOCKS proxies */
public class NetworkSingleton implements Network
{
	private PluginContext context;

	private TrustManager[] trustAllCerts = new TrustManager[]
 	{
 		new X509TrustManager()
 		{
 			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers()
 			{
 				return null;
 			}
 			@Override
			public void checkClientTrusted(
 				java.security.cert.X509Certificate[] certs, String authType)
 			{
 			}
 			@Override
			public void checkServerTrusted(
 				java.security.cert.X509Certificate[] certs, String authType)
 			{
 			}
 		}
 	};

	/**
	 * @param context Plugin context
	 */
	public NetworkSingleton(PluginContext context)
	{
		this.context = context;
	}

	@Override
	public Socket connect(String host, int port, int timeout) throws IOException
	{
		return connect(host, port, timeout, SECURE_NONE);
	}

	@Override
	public Socket connect(String host, int port, int timeout, int secureMode)
		throws IOException
	{
		NetPlugin np=(NetPlugin)context.getPlugin();
		if(np.getConnectionType()==NetPlugin.CONNECTION_SOCKS5)
		{
			if(secureMode == SECURE_REQUIRED)
			{
				throw new IOException(
					"Secure connections are not supported via SOCKS proxy");
			}
			SOCKS5Socket s=new SOCKS5Socket(host,port,context,timeout);
			np.setReportedAddress(s.getProxyExternalAddress(),false);
			return s;
		}
		else
		{
			Socket s = null;

			if(secureMode != SECURE_NONE)
			{
				context.logDebug("Attempting secure connection");
				try
				{
					SSLContext sslContext = SSLContext.getInstance("SSL");
					sslContext.init(null, trustAllCerts, new SecureRandom());
					s = sslContext.getSocketFactory().createSocket();

					// Java claims to support Diffie-Hellman key exchange, but does not
					// really, because everyone uses 2048-bit keys but Java only supports
					// up to 1024. This causes negotiation to fail (which is stupid).
					// Java bug 6521495. To work around it, I disable cipher suites that
					// use DH exchange. Connection will still fail if a server ONLY
					// supports DH, but I didn't find any in that position (yet).

					// Note: this code should be removed if Oracle ever fix the Java bug.

					List<String> limited = new LinkedList<String>();
					for(String suite : ((SSLSocket)s).getEnabledCipherSuites())
					{
						if(!suite.contains("_DHE_"))
						{
							limited.add(suite);
						}
					}
					((SSLSocket)s).setEnabledCipherSuites(limited.toArray(new String[limited.size()]));

					s.connect(new InetSocketAddress(host,port),timeout);
					s.setSoTimeout(0);
					((SSLSocket)s).startHandshake();
					if(((SSLSocket)s).getSession().getCipherSuite().equals(
						"SSL_NULL_WITH_NULL_NULL"))
					{
						throw new IOException("Failed secure connection");
					}
					else
					{
						context.logDebug("Secure connection OK");
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
					context.logDebug("Secure connection failed");
					try
					{
						if(s != null)
						{
							s.close();
						}
					}
					catch(IOException e2)
					{
						// Ignore close errors
					}
					s = null;
					if(secureMode == SECURE_REQUIRED)
					{
						throw new IOException(
							"Secure connection requested but not available");
					}

					// Delay avoids server restrictions on fast reconnect
					try
					{
						Thread.sleep(20000);
					}
					catch(InterruptedException ie)
					{
					}
				}
			}

			if(s == null)
			{
				context.logDebug("Using standard (insecure) connection");
				s = new Socket();
				s.connect(new InetSocketAddress(host,port),timeout);
				s.setSoTimeout(0);
			}

			np.setReportedAddress(s.getLocalAddress(),false);
			return s;
		}
	}

	@Override
	public boolean needsListenTarget()
	{
		NetPlugin np=(NetPlugin)context.getPlugin();
		// SOCKS proxy requires listening target
		return np.getConnectionType()==NetPlugin.CONNECTION_SOCKS5;
	}

	@Override
	public Network.Port listen(String remoteHost) throws IOException
	{
		// If not using SOCKS proxy, just do the normal version
		if(!needsListenTarget()) return listen();
		return new SOCKS5Port(remoteHost,context);
	}

	@Override
	public Network.Port listen() throws IOException
	{
		if(needsListenTarget())
		{
			throw new IOException(
				"Cannot open ports via SOCKS proxy without remote target");
		}

		NetPlugin np=(NetPlugin)context.getPlugin();
		if(np.getConnectionType()==NetPlugin.CONNECTION_UPNP)
		{
			InternetGatewayDevice gateway=(np).getUPnPGateway();
			UPnPServerSocket socket=new UPnPServerSocket(context,gateway);
			return new ServerSocketWrapper(socket,
				InetAddress.getByName(socket.getExternalAddress()),
				socket.getExternalPort());
		}
		else
		{
			DirectServerSocket socket=new DirectServerSocket(context);
			return new ServerSocketWrapper(socket,
				InetAddress.getByName(socket.getExternalAddress()),
				socket.getLocalPort());
		}
	}

	@Override
	public void reportPublicAddress(InetAddress ia)
	{
		NetPlugin np=(NetPlugin)context.getPlugin();
		np.setReportedAddress(ia,true);
	}

	@Override
	public InetAddress getPublicAddress()
	{
		NetPlugin np=(NetPlugin)context.getPlugin();
		if(np.getConnectionType()==NetPlugin.CONNECTION_SOCKS5)
			return null;
		else
			return np.getReportedAddress();
	}
}
