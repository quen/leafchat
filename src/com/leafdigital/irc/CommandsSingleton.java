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

import java.util.regex.*;

import util.StringUtils;

import com.leafdigital.irc.api.*;
import com.leafdigital.prefs.api.*;

import leafchat.core.api.*;

/** Implementor of Commands interface to manage user-typed command handlers */
public class CommandsSingleton implements Commands,MsgOwner
{
	private MessageDispatch mdp;
	private PluginContext pc;
	PluginContext getPluginContext() { return pc; }

	CommandsSingleton(PluginContext pc)
	{
		this.pc=pc;
		pc.registerMessageOwner(this);
	}

	// MessageOwner

	@Override
	public void init(MessageDispatch mdp)
	{
		this.mdp=mdp;
	}

	@Override
	public String getFriendlyName()
	{
		return "User-typed commands";
	}

	@Override
	public Class<? extends Msg> getMessageClass()
	{
		return UserCommandMsg.class;
	}

	@Override
	public boolean registerTarget(Object oTarget, Class<? extends Msg> cMessage,
		MessageFilter mf, int iRequestID, int iPriority)
	{
		return true;
	}

	@Override
	public void unregisterTarget(Object oTarget, int iRequestID)
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

	/**
	 * Splits a command into the following components:
	 * 1. Optional server index prefix e.g. /2/msg (the /2 bit)
	 * 2. Actual command
	 * 3. Params of command
	 */
	private final static String COMMANDSPLIT=
		"^(%[0-9]+(?=%))?(?:%([A-Za-z0-9]+)(?:\\ )*)?(.*)$";

	private static String regexpRangeQuote(String s)
	{
		if(s.length()==0) return "";
		char c=s.charAt(0);
		switch(c)
		{
		case '\\':
		case '-':
			return "\\"+c;
		default:
			return ""+c;
		}
	}

	@Override
	public boolean isCommandCharacter(char c)
	{
		Preferences p=pc.getSingleton2(Preferences.class);
		PreferencesGroup pg=p.getGroup(pc.getPlugin());
		String extra=pg.get(IRCPrefs.PREF_EXTRACOMMANDCHAR,IRCPrefs.PREFDEFAULT_EXTRACOMMANDCHAR);

		return (c=='/' || extra.indexOf(c)!=-1);
	}

	@Override
	public void doCommand(String command,
		Server contextServer,IRCUserAddress contextUser,String contextChan,MessageDisplay md,
		boolean variables)
	{
		// Set up regular expression matcher to handle extra command character if provided
		Preferences p=pc.getSingleton2(Preferences.class);
		PreferencesGroup pg=p.getGroup(pc.getPlugin());
		String commandCharacters="[/"+regexpRangeQuote(
			pg.get(IRCPrefs.PREF_EXTRACOMMANDCHAR,IRCPrefs.PREFDEFAULT_EXTRACOMMANDCHAR))+
			"]";

		Matcher m=Pattern.compile(StringUtils.replace(COMMANDSPLIT,"%",commandCharacters),Pattern.DOTALL).matcher(command);
		if(!m.matches()) throw new BugException("Command regexp didn't match: "+command);

		// Handle server index
		String serverIndex=m.group(1);
		Server resolvedServer=contextServer;
		if(serverIndex!=null && !serverIndex.equals(""))
		{
			// Get connections singleton
			Connections c=pc.getSingleton2(Connections.class);
			try
			{
				resolvedServer=c.getNumbered(Integer.parseInt(serverIndex.substring(1)));
			}
			catch(ArrayIndexOutOfBoundsException aioobe)
			{
				// Send off an error display already
				md.showError("No connected server has index: <key>"+serverIndex.substring(1)+"</key>");
				return;
			}
		}

		// Set up other parts of resolved command
		String resolvedCommand=m.group(2);
		if(resolvedCommand!=null && resolvedCommand.equals("")) resolvedCommand=null;
		if(resolvedCommand!=null) resolvedCommand=resolvedCommand.toLowerCase();
		String resolvedParams=m.group(3);

		// TODO Replace variables

		// Construct message and dispatch it
		mdp.dispatchMessage(
			new UserCommandMsg(resolvedServer,resolvedCommand,resolvedParams,
				contextChan,contextUser,md),
			true);
	}
}
