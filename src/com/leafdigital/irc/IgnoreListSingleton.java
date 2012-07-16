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

import util.xml.XML;

import com.leafdigital.irc.api.*;
import com.leafdigital.prefs.api.*;

import leafchat.core.api.*;

/**
 * Manages ignore list and SILENCE commands.
 */
public class IgnoreListSingleton implements IgnoreList,MsgOwner
{
	private PluginContext context;
	private Map<IRCUserAddress, PreferencesGroup> ignore =
		new HashMap<IRCUserAddress, PreferencesGroup>();

	/**
	 * @param pc Context
	 * @throws GeneralException
	 */
	public IgnoreListSingleton(PluginContext pc) throws GeneralException
	{
		this.context=pc;

		// Read list from preferences
		Preferences p=pc.getSingle(Preferences.class);
		PreferencesGroup pg=p.getGroup(pc.getPlugin());
		PreferencesGroup[] ignoreGroups=pg.getChild(IRCPrefs.PREFGROUP_IGNORE).getAnon();
		for(int i=0;i<ignoreGroups.length;i++)
		{
			ignore.put(new IRCUserAddress(
				ignoreGroups[i].get(IRCPrefs.PREF_IGNORE_NICK),
				ignoreGroups[i].get(IRCPrefs.PREF_IGNORE_USER),
				ignoreGroups[i].get(IRCPrefs.PREF_IGNORE_HOST)
				),ignoreGroups[i]);
		}

		pc.registerMessageOwner(this);
		pc.requestMessages(UserSourceIRCMsg.class,this,Msg.PRIORITY_EARLY);
		pc.requestMessages(UserCommandMsg.class,this,Msg.PRIORITY_NORMAL);
		pc.requestMessages(UserCommandListMsg.class,this,Msg.PRIORITY_NORMAL);
		pc.requestMessages(SilenceIRCMsg.class,this,Msg.PRIORITY_EARLY);
	}

	/**
	 * Message: SILENCE response.
	 * @param msg Message
	 */
	public void msg(SilenceIRCMsg msg)
	{
		System.out.println(msg.toString());
		msg.markHandled();
	}

	/**
	 * Message: User command (used to track 'ignore').
	 * @param msg Message
	 */
	public void msg(UserCommandMsg msg)
	{
		if(msg.getCommand()!=null && msg.getCommand().equals("ignore"))
		{
			String[] params=msg.getParams().split(" ");
			if(params.length<1 ||
				(params.length==1 && (params[0].length()==0 || params[0].equals("-r"))) ||
				params.length>2 || (params.length==2 && !params[0].equals("-r")))
			{
				msg.error("Incorrect syntax. Use /ignore <key>mask</key> or /ignore -r <key>mask</key>");
			}
			else
			{
				if(params.length==2)
				{
					IRCUserAddress ua=new IRCUserAddress(params[1],true);
					if(removeMask(ua))
					{
						msg.getMessageDisplay().showInfo("Removed from ignore list: <key>"+XML.esc(ua.toString())+"</key>");
					}
					else
					{
						msg.getMessageDisplay().showError("Not in ignore list: <key>"+XML.esc(ua.toString())+"</key>");
					}
				}
				else
				{
					IRCUserAddress ua=new IRCUserAddress(params[0],true);
					if(addMask(ua))
					{
						msg.getMessageDisplay().showInfo("Added to ignore list: <key>"+XML.esc(ua.toString())+"</key>");
					}
					else
					{
						msg.getMessageDisplay().showError("Already in ignore list: <key>"+XML.esc(ua.toString())+"</key>");
					}
				}
			}

			msg.markHandled();
		}
	}

	/**
	 * Message: Listing available commands.
	 * @param msg Message
	 */
	public void msg(UserCommandListMsg msg)
	{
		msg.addCommand(false, "ignore", UserCommandListMsg.FREQ_COMMON,
			"/ignore [-r] <mask>",
			"Ignore somebody based on a user mask; or stop ignoring a mask with -r");
	}

	@Override
	public synchronized boolean addMask(IRCUserAddress mask)
	{
		if(ignore.containsKey(mask))
			return false;

		Preferences p=context.getSingle(Preferences.class);
		PreferencesGroup newGroup=p.getGroup(context.getPlugin()).getChild(IRCPrefs.PREFGROUP_IGNORE).addAnon();
		newGroup.set(IRCPrefs.PREF_IGNORE_NICK,mask.getNick());
		newGroup.set(IRCPrefs.PREF_IGNORE_USER,mask.getUser());
		newGroup.set(IRCPrefs.PREF_IGNORE_HOST,mask.getHost());

		ignore.put(mask,newGroup);
		md.dispatchMessage(new IgnoreListChangeMsg(),false);

		return true;
	}

	@Override
	public synchronized boolean removeMask(IRCUserAddress mask)
	{
		if(!ignore.containsKey(mask))
			return false;

		ignore.get(mask).remove();

		ignore.remove(mask);

		Server[] connected=context.getSingle(Connections.class).getConnected();
		for(int i=0;i<connected.length;i++)
		{
			connected[i].unsilence(mask.toString());
		}

		md.dispatchMessage(new IgnoreListChangeMsg(),false);

		return true;
	}

	@Override
	public synchronized IRCUserAddress[] getMasks()
	{
		return ignore.keySet().toArray(new IRCUserAddress[ignore.keySet().size()]);
	}

	/**
	 * Message: Any received message from any user (to see if it needs to be
	 * silenced).
	 * @param msg Message
	 */
	public void msg(UserSourceIRCMsg msg)
	{
		for(IRCUserAddress pattern : ignore.keySet())
		{
			if(msg.getSourceUser().matches(pattern))
			{
				msg.markHandled(); // System will process as needed, but it won't get displayed
				msg.getServer().silence(pattern.toString());
				break;
			}
		}
	}

	// Message owner
	private MessageDispatch md;

	@Override
	public boolean allowExternalDispatch(Msg m)
	{
		return false;
	}

	@Override
	public String getFriendlyName()
	{
		return "Ignore list change";
	}

	@Override
	public Class<? extends Msg> getMessageClass()
	{
		return IgnoreListChangeMsg.class;
	}

	@Override
	public void init(MessageDispatch md)
	{
		this.md=md;
	}

	@Override
	public void manualDispatch(Msg m)
	{
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
}
