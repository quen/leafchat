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
package com.leafdigital.irc;

import java.util.*;

import util.TimeUtils;

import com.leafdigital.irc.api.*;
import com.leafdigital.prefs.api.PreferencesGroup;

import leafchat.core.api.*;

/** Implementor of Connections interface to manage list of connected servers */
public class ConnectionsSingleton implements Connections,MsgOwner
{
	private MessageDispatch dispatch;
	private List<ServerConnection> servers = new LinkedList<ServerConnection>();
	private Plugin p;
	private PluginContext context;
	PluginContext getPluginContext() { return context; }
	Plugin getPlugin() { return p; }
	private DefaultMessageDisplay dmd;

	private int messageSequence=0;

	/**
	 * Obtains the default message display handler (which must be set outside
	 * the plugin).
	 * @return The default message display handler
	 * @throws IllegalStateException If it hasn't been set yet
	 */
	public DefaultMessageDisplay getDefaultMessageDisplay() throws IllegalStateException
	{
		if(dmd==null) throw new IllegalStateException("Default message display not set");
		return dmd;
	}

	ConnectionsSingleton(PluginContext context,Plugin p)	 throws GeneralException
	{
		this.context=context;
		this.p=p;
		context.registerMessageOwner(this);
	}

	@Override
	public Server newServer()
	{
		return new ServerConnection(this,context);
	}

	void closeAll()
	{
		ServerConnection[] serverArray;
		synchronized(servers)
		{
			serverArray = servers.toArray(new ServerConnection[servers.size()]);
		}
		for(int i=0;i<serverArray.length;i++)
		{
			serverArray[i].disconnectGracefully();
		}
	}

	synchronized void informConnected(final ServerConnection sc)
	{
		servers.add(sc);
		ServerConnectedMsg scm=new ServerConnectedMsg(sc,messageSequence++);
		dispatch.dispatchMessageHandleErrors(scm,false);

		// Start connection-finished-idle timer.
		final int
			CHECKEVERY=250, // Check every 250ms...
		  AFTERMOTD=1000, // If there is 1000ms idle after the MOTD...
		  ANYWAY=5000;    // ...or 5000ms pause after we have got
		  // the nickname at least (should be in first numeric after accepting
		  // nick), then we send the ConnectionFinished.

		TimeUtils.addTimedEvent(new Runnable()
		{
			@Override
			public void run()
			{
				synchronized(ConnectionsSingleton.this)
				{
					// If it was disconnected, abandon this process
					if(!servers.contains(sc)) return;

					long lastLine=sc.getLastLineTime();
					if(lastLine!=0) // As long as we've received at least *some* line
					{
						long now=System.currentTimeMillis();
						int 	actualDelay=(int)(now-lastLine);

						if( !sc.deferConnectionFinished() &&
							( (sc.hasGotEndOfMOTD() && actualDelay >AFTERMOTD) ||
							(sc.getOurNick()!=null && actualDelay>ANYWAY)))
						{
							ServerConnectionFinishedMsg scfm=new ServerConnectionFinishedMsg(sc,messageSequence++);
							dispatch.dispatchMessageHandleErrors(scfm,false);
							return;
						}
					}

					// Try again a little later
					TimeUtils.addTimedEvent(this,CHECKEVERY,true);
				}
			}
		},CHECKEVERY,true);
	}

	void informDisconnected(ServerConnection sc, Throwable t)
	{
		int thisSequence;
		synchronized(this)
		{
			servers.remove(sc);
			thisSequence = messageSequence;
			messageSequence++;
		}
		NetworkException ne = (t==null || (t instanceof NetworkException))
			? (NetworkException)t
			: new NetworkException(t);
		ServerDisconnectedMsg sdm = new ServerDisconnectedMsg(sc, ne, thisSequence,
			sc.wasQuitRequested());
		dispatch.dispatchMessageHandleErrors(sdm, false);
	}

	void informLine(ServerConnection sc,byte[] abLine)
	{
		int thisSequence;
		synchronized(this)
		{
			thisSequence=messageSequence;
			messageSequence++;
		}
		ServerLineMsg slm=new ServerLineMsg(sc,abLine,thisSequence);
		dispatch.dispatchMessageHandleErrors(slm,false);
	}

	void informSend(ServerConnection sc,byte[] abLine)
	{
		ServerSendMsg ssm=new ServerSendMsg(sc,abLine);
		dispatch.dispatchMessageHandleErrors(ssm,false);
	}

	int informRearrange(ServerRearrangeMsg srm)
	{
		int thisSequence;
		synchronized(this)
		{
			thisSequence=messageSequence;
			messageSequence++;
		}
		srm.updateSequence(thisSequence);
		dispatch.dispatchMessageHandleErrors(srm,true);
		return srm.getResult();
	}

	// MessageOwner

	@Override
	public void init(MessageDispatch mdp)
	{
		this.dispatch=mdp;
	}

	@Override
	public String getFriendlyName()
	{
		return "Low-level server messages";
	}

	@Override
	public Class<? extends Msg> getMessageClass()
	{
		return ServerMsg.class;
	}

	@Override
	public boolean registerTarget(Object oTarget, Class<? extends Msg> cMessage,
		MessageFilter mf,int iRequestID,int iPriority)
	{
		return true;
	}

	@Override
	public void unregisterTarget(Object oTarget,int iRequestID)
	{
	}

	@Override
	public void manualDispatch(Msg m)
	{
	}

	@Override
	public boolean allowExternalDispatch(Msg m)
	{
		return false;
	}

	@Override
	public Server getNumbered(int index) throws ArrayIndexOutOfBoundsException
	{
		if(index-1 < servers.size() && index-1 > 0)
			return servers.get(index-1);
		else
			throw new ArrayIndexOutOfBoundsException("No connected server at index "+index);
	}

	@Override
	public Server[] getConnected()
	{
		return servers.toArray(new Server[servers.size()]);
	}

	/**
	 * Obtains short name for a server. The shortname identifies a server uniquely
	 * with regard to other servers, i.e. if you only have one connection to a
	 * network, it'll be the network name.
	 * @param s Server
	 * @return Suitable name
	 */
	public synchronized String getShortName(Server s)
	{
		String
			host=s.getReportedOrConnectedHost(),
			fullHost=host+":"+s.getConnectedPort();

		if(host.indexOf(':')!=-1) host=host.substring(0,host.indexOf(':'));

		String network=s.getPreferences().getAnonParent().get(IRCPrefs.PREF_NETWORK,null);

		boolean networkOK=network!=null,skipPortOK=true,skipNumberOK=true;
		boolean gotUsYet=false;
		int number=1;

		for(Server compare : servers)
		{
			String compareHost=compare.getReportedOrConnectedHost();
			String compareFullHost=compareHost+":"+compare.getConnectedPort();
			if(compare==s)
			{
				gotUsYet=true;
				continue;
			}
			if(!gotUsYet && compareFullHost.equals(fullHost)) number++;
			PreferencesGroup pg=compare.getPreferences().getAnonParent();
			String sCompareNetwork=pg==null ? null : pg.get(IRCPrefs.PREF_NETWORK,null);
			if(networkOK && sCompareNetwork!=null && sCompareNetwork.equals(network)) networkOK=false;
			if(skipPortOK && compareHost.equals(host)) skipPortOK=false;
			if(skipNumberOK && compareFullHost.equals(fullHost)) skipNumberOK=false;
		}

		if(networkOK)
			return network;
		else if(skipPortOK)
			return host;
		else if(skipNumberOK)
			return fullHost;
		else
			return fullHost+" \u2013 "+number;
	}

	@Override
	public void setDefaultMessageDisplay(DefaultMessageDisplay dmd)
	{
		this.dmd=dmd;
	}
}
