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

import javax.net.ssl.*;

import org.w3c.dom.Document;

import util.xml.XML;

import net.sbbi.upnp.impls.InternetGatewayDevice;
import net.sbbi.upnp.messages.UPNPResponseException;

import com.leafdigital.net.api.Network;
import com.leafdigital.prefs.api.*;
import com.leafdigital.prefsui.api.PreferencesUI;

import leafchat.core.api.*;

/** Provides support for network connections via proxies etc */
public class NetPlugin implements Plugin
{
	private static final String URL_IPCHECK="https://live.leafdigital.com/leafchat-remote/ip.jsp";
	private static final String HOST_LOCALCHECK="live.leafdigital.com";

	private final static String PREF_CONNECTIONTYPE="connection-type";
	private final static String PREFVALUE_CONNECTIONTYPE_UPNP="upnp";
	private final static String PREFVALUE_CONNECTIONTYPE_MANUAL="manual";
	private final static String PREFVALUE_CONNECTIONTYPE_SOCKS5="socks5";
	final static int CONNECTION_UPNP=1,CONNECTION_MANUAL=2,CONNECTION_SOCKS5=3;

	private final static String PREF_SOCKSHOST="socks-host";
	private final static String PREF_SOCKSPORT="socks-port";
	private final static String PREFDEFAULT_SOCKSPORT="1080";
	private final static String PREF_SOCKSUSERNAME="socks-username";
	private final static String PREF_SOCKSPASSWORD="socks-password";

	// IP address details
	private static final String PREF_PUBLICADDRESS="public-address";
	private static final String PREF_REPORTEDVERSION="reported-version";
	private static final String
		PREF_LISTENPORTMIN="listen-port-min",
	  PREFDEFAULT_LISTENPORTMIN="50631";
	private static final String
		PREF_LISTENPORTMAX="listen-port-max",
	  PREFDEFAULT_LISTENPORTMAX="50639";

	private PluginContext context;
	private InternetGatewayDevice gateway=null;
	private boolean checkedGateway=false;

	private Preferences prefs;
	private PreferencesGroup group;

	private InetAddress reportedAddress;

	@Override
	public void init(PluginContext context, PluginLoadReporter status) throws GeneralException
	{
		this.context=context;

		prefs=context.getSingleton2(Preferences.class);
		group=prefs.getGroup(prefs.getPluginOwner(context.getPlugin()));

		context.registerSingleton(Network.class,new NetworkSingleton(context));
		PreferencesUI pui=context.getSingleton2(PreferencesUI.class);
		pui.registerPage(this,(new ConnectionPage(context)).getPage());

		// If UPnP is not configured or is set to yes, see if we have UPnP device
		int type=getConnectionType();
		if(type==0 || type==CONNECTION_UPNP)
		{
			(new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					if(getUPnPGateway()!=null)
						group.set(PREF_CONNECTIONTYPE,PREFVALUE_CONNECTIONTYPE_UPNP);
					else
						group.set(PREF_CONNECTIONTYPE,PREFVALUE_CONNECTIONTYPE_MANUAL);
				}
			},"leafChat UPnP check thread")).start();
		}


		// Once per version, report the version alongside checking the public IP
		// address.
		String currentVersion=SystemVersion.getBuildVersion();
		String reportedVersion=group.get(PREF_REPORTEDVERSION,"");
		if(!reportedVersion.equals(currentVersion))
		{
			status.reportProgress("Checking public IP address");
			context.log("Checking public IP address");
			String publicAddress=getPublicAddress(currentVersion);
			context.log("Public IP: "+(publicAddress==null ? "<unknown>" : publicAddress));
			if(publicAddress!=null)
			{
				// OK, now we can check if we're behind a router
				status.reportProgress("Checking local IP address");
				context.log("Checking local IP address");
				if(checkBehindRouter(publicAddress)) {
					group.set(PREF_PUBLICADDRESS,publicAddress);
				}
			}
			group.set(PREF_REPORTEDVERSION,currentVersion);
		}
	}

	/** @return SOCKS server address */
	String getSOCKSHost()
	{
		return group.get(PREF_SOCKSHOST,"");
	}
	/** @return SOCKS server port */
	int getSOCKSPort()
	{
		return Integer.parseInt(group.get(PREF_SOCKSPORT,PREFDEFAULT_SOCKSPORT));
	}
	/** @return SOCKS username */
	String getSOCKSUsername()
	{
		return group.get(PREF_SOCKSUSERNAME,"");
	}
	/** @return SOCKS password */
	String getSOCKSPassword()
	{
		return group.get(PREF_SOCKSPASSWORD,"");
	}
	/** @param host Host of SOCKS server	 */
	void setSOCKSHost(String host)
	{
		group.set(PREF_SOCKSHOST,host,"");
	}
	/** @param port Port of SOCKS server	 */
	void setSOCKSPort(int port)
	{
		group.set(PREF_SOCKSPORT,port+"",PREFDEFAULT_SOCKSPORT);
	}
	/** @param username Username	 */
	void setSOCKSUsername(String username)
	{
		group.set(PREF_SOCKSUSERNAME,username,"");
	}
	/** @param password Password	 */
	void setSOCKSPassword(String password)
	{
		group.set(PREF_SOCKSPASSWORD,password,"");
	}

	/** @return The connection type as CONNECTION_xx constant */
	int getConnectionType()
	{
		String type=group.get(PREF_CONNECTIONTYPE,"");
		if(type.equals(PREFVALUE_CONNECTIONTYPE_UPNP))
			return CONNECTION_UPNP;
		if(type.equals(PREFVALUE_CONNECTIONTYPE_MANUAL))
			return CONNECTION_MANUAL;
		if(type.equals(PREFVALUE_CONNECTIONTYPE_SOCKS5))
			return CONNECTION_SOCKS5;
		return 0;
	}

	/**
	 * Sets connection type and stores in preferences.
	 * @param type CONNECTION_xx constant
	 */
	void setConnectionType(int type)
	{
		switch(type)
		{
		case CONNECTION_UPNP:
			group.set(PREF_CONNECTIONTYPE,PREFVALUE_CONNECTIONTYPE_UPNP);
			break;
		case CONNECTION_MANUAL:
			group.set(PREF_CONNECTIONTYPE,PREFVALUE_CONNECTIONTYPE_MANUAL);
			break;
		case CONNECTION_SOCKS5:
			group.set(PREF_CONNECTIONTYPE,PREFVALUE_CONNECTIONTYPE_SOCKS5);
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Attempts to enable UPnP and sets the preference to the relevant value.
	 */
	private synchronized void findGateway()
	{
		checkedGateway=true;
	  try
	  {
	  	  long start=System.currentTimeMillis();
	    InternetGatewayDevice[] IGDs = InternetGatewayDevice.getDevices(-1);
	    long done=System.currentTimeMillis();
	    String time=" (discovery took "+(done-start)+" ms)";
	    if(IGDs!=null)
	    {
	    		// Yay, got one
	    		gateway=IGDs[0];
    	  		context.log("Found UPnP gateway: "+gateway.getIGDRootDevice().getModelName()+time);
    	  		try
    	  		{
    	    		setReportedAddress(InetAddress.getByName(gateway.getExternalIPAddress()),true);
    	  		}
    	  		catch(IOException e)
    	  		{
    	  		}
				catch(UPNPResponseException e)
				{
				}
  				return;
	    }
	    else
	    {
	    		context.log("No UPnP gateway"+time);
	    }
	  }
	  catch (IOException e)
	  {
	  		context.log("Error searching for UPnP gateway",e);
	  }
	  gateway=null;
	}

	/** @return UPnP device if available, otherwise null */
	synchronized InternetGatewayDevice getUPnPGateway()
	{
		if(!checkedGateway) findGateway();
		return gateway;
	}

	/**
	 * Makes a connection to the Web server on live.leafdigital.com and checks
	 * whether the local source address is the same as the public reported one.
	 * @param expectedAddress Known public address
	 * @return True if user is behind router
	 */
	private boolean checkBehindRouter(String expectedAddress)
	{
		try
		{
			Socket s=new Socket();
			s.setSoTimeout(10000);
			s.connect(new InetSocketAddress(HOST_LOCALCHECK,80));
			String gotAddress=s.getLocalAddress().getHostAddress();
			boolean behindRouter=!gotAddress.equals(expectedAddress);
			if(!behindRouter)	setReportedAddress(s.getLocalAddress(),false);
			return behindRouter;
		}
		catch(Exception e)
		{
			return false; // Er, don't know
		}
	}

	/**
	 * Obtains public IP address of this computer via a Web service on
	 * live.leafdigital.com.
	 * @param currentVersion Software version. Added to end of URL.
	 * @return IP address as string
	 */
	private String getPublicAddress(String currentVersion)
	{
		try
		{
			// This uses HTTPS because it's likely to get a direct connection
			// and none of this proxy bull.
			URL u=new URL(URL_IPCHECK+"?version="+currentVersion);

			TrustManager[] trustAllCerts = new TrustManager[]
      {
				new X509TrustManager()
				{
					@Override
					public java.security.cert.X509Certificate[] getAcceptedIssuers()
					{
						return null;
					}

					@Override
					public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType)
					{
					}

					@Override
					public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType)
					{
					}
				}
			};

			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());

			SSLSocketFactory before=HttpsURLConnection.getDefaultSSLSocketFactory();
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			HttpsURLConnection connection=(HttpsURLConnection)u.openConnection();
			HttpsURLConnection.setDefaultSSLSocketFactory(before);

			Document d=XML.parse(connection.getInputStream());
			return XML.getChildText(d.getDocumentElement(),"address");
		}
		catch(Exception e)
		{
			return null;
		}
	}

	/** @return The address user has set in preferences */
	String getManualPublicAddress()
	{
		return group.get(PREF_PUBLICADDRESS,null);
	}

	/**
	 * Set public address manually.
	 * @param address New address or null for none
	 */
	void setManualPublicAddress(String address)
	{
		if(address==null)
			group.unset(PREF_PUBLICADDRESS);
		else
			group.set(PREF_PUBLICADDRESS,address);
	}

	/**
	 * Sets reported address. The address will only actually be set if the
	 * parameter is a valid public address.
	 * @param ia Address as local end of any connection
	 * @param replace If true, replaces any existing address
	 */
	void setReportedAddress(InetAddress ia,boolean replace)
	{
		if((reportedAddress==null || replace) && isValidAddress(ia)) reportedAddress=ia;
	}

	/**
	 * @param ia Internet address being checked
	 * @return True if address is a valid public address
	 */
	static boolean isValidAddress(InetAddress ia)
	{
		return !(ia.isLoopbackAddress() || ia.isLinkLocalAddress() || ia.isSiteLocalAddress());
	}

	/**
	 * @return The address that has been reported as a local end to connections
	 *   from this system, if any
	 */
	InetAddress getReportedAddress()
	{
		return reportedAddress;
	}

	int getListenPortMin()
	{
		return Integer.parseInt(group.get(PREF_LISTENPORTMIN,PREFDEFAULT_LISTENPORTMIN));
	}
	int getListenPortMax()
	{
		return Integer.parseInt(group.get(PREF_LISTENPORTMAX,PREFDEFAULT_LISTENPORTMAX));
	}
	void setListenPorts(int min,int max)
	{
		group.set(PREF_LISTENPORTMIN,min+"");
		group.set(PREF_LISTENPORTMAX,max+"");
	}

	@Override
	public void close() throws GeneralException
	{
	}

	@Override
	public String toString()
	{
		return "Network plugin";
	}
}
