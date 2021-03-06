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
package com.leafdigital.irc;

import java.net.*;
import java.util.regex.*;

import com.leafdigital.irc.api.*;
import com.leafdigital.net.api.Network;
import com.leafdigital.prefs.api.*;

import leafchat.core.api.*;

/** Provides support for connection to IRC servers */
public class IRCPlugin implements Plugin, IRCPrefs
{
	private PluginContext context;
	private IgnoreListSingleton ignoreList;
	private WatchListSingleton watchList;
	private IRCEncodingSingleton ircEncoding;
	private BasicCommands basicCommands;

	IgnoreListSingleton getIgnoreListSingleton() { return ignoreList; }

	@Override
	public void init(PluginContext context, PluginLoadReporter plr) throws GeneralException
	{
		this.context = context;

		new CommandListOwner(context);
		context.registerSingleton(Connections.class, new ConnectionsSingleton(context, this));
		context.registerSingleton(Commands.class, new CommandsSingleton(context));

		new IRCMessageParser(context);

		ignoreList = new IgnoreListSingleton(context);
		context.registerSingleton(IgnoreList.class, ignoreList);
		watchList = new WatchListSingleton(context);
		context.registerSingleton(WatchList.class, watchList);
		ircEncoding = new IRCEncodingSingleton(context);
		context.registerSingleton(IRCEncoding.class, ircEncoding);

		basicCommands = new BasicCommands(context);

		updateOldIdentifyPrefs();
		context.requestMessages(PingIRCMsg.class, this);
		context.requestMessages(UserCTCPRequestIRCMsg.class, new CTCPHandler(context), Msg.PRIORITY_FIRST);
		context.requestMessages(UserCommandMsg.class, this, Msg.PRIORITY_LAST);
		context.requestMessages(UserCommandListMsg.class, this, Msg.PRIORITY_LAST);
		context.requestMessages(ServerConnectedMsg.class, this, Msg.PRIORITY_FIRST);
		context.requestMessages(NumericIRCMsg.class, this);
	}

	/** Pattern to find IRC-style address mask from welcome message */
	private final static Pattern SPOTADDRESS=Pattern.compile(
		"^(?:.*\\s)?\\S+!\\S+@(\\S+).*$");

	/**
	 * Handle numerics to get welcome message which may inclue local IP address.
	 * @param m Message
	 */
	public void msg(NumericIRCMsg m)
	{
		// See if we can get the user's local address from welcome message, if
		// so then tell the network plugin
		if(m.getNumeric()==NumericIRCMsg.RPL_WELCOME)
		{
			String message=m.getParamISO(m.getParams().length-1);
			Matcher check=SPOTADDRESS.matcher(message);
			if(check.matches())
			{
				try
				{
					// Resolve our address
					Network n=context.getSingle(Network.class);
					n.reportPublicAddress(InetAddress.getByName(check.group(1)));
				}
				catch(UnknownHostException e)
				{
					// Ignore
				}
			}
		}
	}

	/**
	 * Respond to server PING with PONG.
	 * @param m Message
	 */
	public void msg(PingIRCMsg m)
	{
		m.getServer().sendLine(
			IRCMsg.constructBytes("PONG" + (m.getCode()!=null? " :" + m.getCode() : "")));
		m.markHandled();
	}

	/**
	 * Fallback command handling that passes unknown commands directly to server.
	 * @param m Message
	 * @throws GeneralException
	 */
	public void msg(UserCommandMsg m) throws GeneralException
	{
		basicCommands.handle(m);
		if(!(m.isHandled() || m.isStopped()))
		{
			// Fallback handling
			if(m.getServer()!=null && m.getCommand()!=null)
			{
				if(!m.getServer().isConnected())
				{
					m.error("Not connected.");
				}
				else // Default just passes on to server
				{
					m.getServer().sendLine(IRCMsg.constructBytes(
						m.getCommand().toUpperCase() + " ",
						basicCommands.convertEncoding(m.getParams(), m.getServer(), null, null)));
				}
			}
			else
			{
				m.error("Don't know how to handle this command.");
			}
		}
	}

	/**
	 * Fallback command listing that lists basic commands and standard IRC commands.
	 * @param m Message
	 * @throws GeneralException
	 */
	public void msg(UserCommandListMsg m) throws GeneralException
	{
		basicCommands.handle(m);
	}

	/**
	 * Sends password, nickname, and user in response to server connection.
	 * @param m Message
	 * @throws GeneralException
	 */
	public void msg(ServerConnectedMsg m) throws GeneralException
	{
		Server s = m.getServer();

		// Send PASS line
		String pass = s.getServerPassword();
		if(pass != null)
		{
			s.sendLine(IRCMsg.constructBytes("PASS " + pass));
		}

		// Send NICK line
		String nick = s.getDefaultNick();
		if(nick != null)
		{
			s.sendLine(IRCMsg.constructBytes("NICK " + nick));
		}

		// Send USER line
		String
			user = s.getPreferences().getAnonHierarchical(PREF_USER, PREFDEFAULT_USER),
			realName = s.getPreferences().getAnonHierarchical(PREF_REALNAME, PREFDEFAULT_REALNAME);
		if(user != null)
		{
			s.sendLine(IRCMsg.constructBytes("USER " + user + " 0 * :",
				basicCommands.convertEncoding(realName, s, null, null)));
		}
	}

	@Override
	public void close() throws GeneralException
	{
		watchList.close(); // Need to stop its timed event, though
		((ConnectionsSingleton)context.getSingle(Connections.class)).closeAll();
	}

	@Override
	public String toString()
	{
		return "IRC plugin";
	}

	/**
	 * Updates the format of identify command preferences, which changed in 2.2.
	 * @throws GeneralException
	 */
	private void updateOldIdentifyPrefs() throws GeneralException
	{
		Preferences p=context.getSingle(Preferences.class);
		PreferencesGroup root=p.getGroup(p.getPluginOwner(this));
		updateOldIdentifyPrefs(root.getChild(PREFGROUP_SERVERS));
	}

	private void updateOldIdentifyPrefs(PreferencesGroup group)
	{
		String old=group.get(PREF_IDENTIFYCOMMAND,null);
		if(old!=null)
		{
			group.set(PREF_IDENTIFYPATTERN,old.trim()+" ${password}");
			group.unset(PREF_IDENTIFYCOMMAND);
		}

		PreferencesGroup[] children=group.getAnon();
		for(int i=0;i<children.length;i++)
		{
			updateOldIdentifyPrefs(children[i]);
		}
	}
}
