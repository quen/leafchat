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
import util.xml.XML;

import com.leafdigital.irc.api.*;
import com.leafdigital.prefs.api.*;

import leafchat.core.api.*;

/**
 * Watch list monitors users to see who's online.
 */
public class WatchListSingleton implements WatchList
{
	private PluginContext context;

	/** Map from IRCUserAddress -> PreferencesGroup for user's watch list */
	private Map<IRCUserAddress, PreferencesGroup> watchList =
		new HashMap<IRCUserAddress, PreferencesGroup>();

	/** Map from Server -> ServerInfo for all servers we know about */
	private Map<Server, ServerInfo> serverInfo =
		new HashMap<Server, ServerInfo>();

	private BasicMsgOwner watchMsgOwner=new BasicMsgOwner()
	{
		@Override
		public String getFriendlyName()
		{
			return "Watched user messages";
		}

		@Override
		public Class<? extends Msg> getMessageClass()
		{
			return WatchMsg.class;
		}
	};

	private BasicMsgOwner listMsgOwner=new BasicMsgOwner()
	{
		@Override
		public String getFriendlyName()
		{
			return "Watch list change";
		}

		@Override
		public Class<? extends Msg> getMessageClass()
		{
			return WatchListChangeMsg.class;
		}
	};

	/** Information stored per-server */
	private class ServerInfo
	{
		private Server s;

		int serverMax=0;

		/** Map of IRCUserAddress -> Integer (reference count) */
		Map<IRCUserAddress, Integer> tempList =
			new HashMap<IRCUserAddress, Integer>();

		/** Set of IRCUserAddress being WATCHed */
		Set<IRCUserAddress> actualWatch = new HashSet<IRCUserAddress>();

		/** Set of IRCUserAddress that needs ISON */
		Set<IRCUserAddress> ison = new HashSet<IRCUserAddress>();

		/** Map of all lower-case nicks that are being watched in some manner => IRCUserAddress mask */
		Map<String, IRCUserAddress> actualNicks =
			new HashMap<String, IRCUserAddress>();

		/** Time at which we last did ISON */
		long lastIsonTime;
		/** Bulk of last ISON */
		int lastIsonBulk;

		boolean started=false;
		private List<Runnable> backlog = new LinkedList<Runnable>();

		ServerInfo(Server s)
		{
			this.s=s;
		}

		void startup()
		{
			// Check whether server supports WATCH
			try
			{
				String max=s.getISupport("WATCH");
				if(max!=null)
					serverMax=Integer.parseInt(max);
			}
			catch(NumberFormatException nfe)
			{
			}

			// Add everything to WATCH or ISON
			StringBuffer watchCommand=new StringBuffer("WATCH");
			for(Iterator<IRCUserAddress> i=watchList.keySet().iterator();i.hasNext();)
			{
				IRCUserAddress mask = i.next();

				// If still trying WATCH...
				if(actualWatch.size()<serverMax)
				{
					String watchString=mask.toString();
					if(watchString.length()+2+watchCommand.length() > 512) // +2 = " +"
					{
						// Time for a new command, send existing one
						s.sendLine(IRCMsg.constructBytes(watchCommand.toString()));
						watchCommand=new StringBuffer("WATCH");
					}
					watchCommand.append(" +");
					watchCommand.append(watchString);
					actualWatch.add(mask);
				}
				else
				{
					ison.add(mask);
				}
				actualNicks.put(mask.getNick().toLowerCase(),mask);
			}
			if(watchCommand.length()>5)
			{
				s.sendLine(IRCMsg.constructBytes(watchCommand.toString()));
			}
			if(ison.size()>0)
			{
				sendISON();
			}

			started=true;
			for(Iterator<Runnable> i=backlog.iterator();i.hasNext();)
			{
				(i.next()).run();
			}
			backlog=null;
		}

		void addPerm(final IRCUserAddress mask)
		{
			if(!started)
			{
				backlog.add(new Runnable()
				{
					@Override
					public void run()
					{
						addPerm(mask);
					}
				});
				return;
			}

			// If it's in the temp list anyhow, do nothing
			if(tempList.containsKey(mask)) return;

			addSomewhere(mask);
		}

		private void addSomewhere(IRCUserAddress mask)
		{
			if(actualWatch.size()<serverMax)
			{
				actualWatch.add(mask);
				s.sendLine(IRCMsg.constructBytes("WATCH +"+mask.toString()));
			}
			else
			{
				ison.add(mask);
				String isonCommand="ISON "+mask.getNick();
				s.sendLine(IRCMsg.constructBytes(isonCommand)); // So we get immediate notification
				pendingISON.addLast(isonCommand);
			}
			actualNicks.put(mask.getNick().toLowerCase(),mask);
		}

		void removePerm(final IRCUserAddress mask)
		{
			if(!started)
			{
				backlog.add(new Runnable()
				{
					@Override
					public void run()
					{
						removePerm(mask);
					}
				});
				return;
			}

			// If it's in the temp list anyhow, do nothing
			if(tempList.containsKey(mask)) return;

			removeSomewhere(mask);
		}

		private void removeSomewhere(IRCUserAddress mask)
		{
			// Okay, let's see where it is...
			if(actualWatch.remove(mask))
			{
				s.sendLine(IRCMsg.constructBytes("WATCH -"+mask.toString()));
			}
			else
			{
				ison.remove(mask);
			}

			// Get rid of it from the online list too
			online.remove(mask.getNick().toLowerCase());

			// Get rid from the nick list
			actualNicks.remove(mask.getNick().toLowerCase());
		}

		void addTemp(final IRCUserAddress mask)
		{
			if(!started)
			{
				backlog.add(new Runnable()
				{
					@Override
					public void run()
					{
						addTemp(mask);
					}
				});
				return;
			}

			Integer i=tempList.get(mask);
			if(i!=null)
			{
				// Already in temp list, just refcount it up
				tempList.put(mask, i+1);
				return;
			}

			// OK, add it...
			tempList.put(mask, 1);

			// Now, is it in the perm list?
			if(watchList.containsKey(mask))	return;

			// Otherwise better add it
			addSomewhere(mask);
		}

		void removeTemp(final IRCUserAddress mask)
		{
			if(!started)
			{
				backlog.add(new Runnable()
				{
					@Override
					public void run()
					{
						removeTemp(mask);
					}
			  });
				return;
			}

			Integer i=tempList.get(mask);
			if(i==null)
			{
				throw new BugException("Temporary mask "+mask+" not present");
			}
			if(i.intValue()>1)
			{
				tempList.put(mask, i - 1);
				return;
			}

			// OK, remove it...
			tempList.remove(mask);

			// Now, is it in the perm list?
			if(watchList.containsKey(mask))	return;

			// Get rid of it from WATCH/ISON
			removeSomewhere(mask);
		}

		/** List of pending ISON commands */
		private LinkedList<String> pendingISON = new LinkedList<String>();

		private void sendISON()
		{
			if(ison.size()==0) return;

			lastIsonTime=System.currentTimeMillis();
			lastIsonBulk=0;

			// :riga-r.ca.us.dal.net 303 quen :quen blahblah
      // If we don't know reported host (why not?), assume a safe value for its length
			int iHostLength=s.getReportedHost()!=null ? s.getReportedHost().length() : 100;
			int prefixLength=iHostLength+s.getOurNick().length()+8;

			StringBuffer isonCommand = new StringBuffer("ISON");
			for(Iterator<IRCUserAddress> i=ison.iterator();i.hasNext();)
			{
				String nick=i.next().getNick();
				if(nick.length()+1+isonCommand.length() > 510-prefixLength) // 510 for CRLF
				{
					pendingISON.addLast(isonCommand.toString());
					s.sendLine(IRCMsg.constructBytes(isonCommand.toString()));
					lastIsonBulk++;
					isonCommand=new StringBuffer("ISON");
				}
				isonCommand.append(' ');
				isonCommand.append(nick);
			}
			if(isonCommand.length()>4)
			{
				pendingISON.addLast(isonCommand.toString());
				s.sendLine(IRCMsg.constructBytes(isonCommand.toString()));
				lastIsonBulk++;
			}
		}

		/** Called regularly to run timed events */
		void trigger()
		{
			if(ison.size()==0) return;

			// Send ISON every 60 seconds, plus 30 for each extra line (beyond one)
			// of ISON messages
			int seconds=(int)((System.currentTimeMillis()-lastIsonTime)/1000L);
			if(seconds > 30 + lastIsonBulk*30)
				sendISON();
		}

		/**
		 * Map of online nicks. From lowercase version of nick to IRCUserAddress.
		 */
		Map<String, IRCUserAddress> online = new HashMap<String, IRCUserAddress>();

		boolean isKnown(String nick)
		{
			String lcNick=nick.toLowerCase();
			boolean isKnown=online.containsKey(lcNick);
			return isKnown;
		}

		boolean isOnline(String nick)
		{
			String lcNick=nick.toLowerCase();
			boolean isOnline=online.get(lcNick)!=null;
			return isOnline;
		}

		/**
		 * Indicates that the user has been seen online. We don't know whether
		 * the user is of interest to the watch list.
		 * @param ua User address
		 */
		void seenOnline(final IRCUserAddress ua)
		{
			if(!started)
			{
				backlog.add(new Runnable()
				{
					@Override
					public void run()
					{
						seenOnline(ua);
					}
				});
				return;
			}
			// Only send it if they are in the watch list/ISON list
			String lcNick=ua.getNick().toLowerCase();
			IRCUserAddress mask=actualNicks.get(lcNick);
			if(mask!=null && ua.matches(mask))
				markOnline(ua);
		}

		/**
		 * Indicates that the user has been seen to quit. We don't know whether
		 * the user is of interest to the watch list.
		 * @param ua User address
		 */
		void seenOffline(final IRCUserAddress ua)
		{
			if(!started)
			{
				backlog.add(new Runnable()
				{
					@Override
					public void run()
					{
						seenOffline(ua);
					}
				});
				return;
			}
			// Only send it if we previously thought they were online
			String lcNick=ua.getNick().toLowerCase();
			IRCUserAddress mask=actualNicks.get(lcNick);
			if(mask!=null && ua.matches(mask))
				markOffline(ua);
		}

		void markOnline(final IRCUserAddress ua)
		{
			if(!started)
			{
				backlog.add(new Runnable()
				{
					@Override
					public void run()
					{
						markOnline(ua);
					}
				});
				return;
			}

			// Do nothing if it was already marked online
			String lcNick=ua.getNick().toLowerCase();
			boolean checked=online.containsKey(lcNick);
			boolean present=online.get(lcNick)!=null;
			if(present) return;

			// Save in map and send message
			online.put(lcNick,ua);
			watchMsgOwner.getDispatch().dispatchMessageHandleErrors(new OnWatchMsg(s,ua,checked),false);
		}

		void markOffline(final IRCUserAddress ua)
		{
			if(!started)
			{
				backlog.add(new Runnable()
				{
					@Override
					public void run()
					{
						markOffline(ua);
					}
				});
				return;
			}

			// Do nothing if it was already marked offline
			String lcNick=ua.getNick().toLowerCase();
			boolean checked=online.containsKey(lcNick);
			boolean present=online.get(lcNick)!=null;
			if(checked && !present)	return;

			// Mark offline in map and send message
			online.put(lcNick,null);
			watchMsgOwner.getDispatch().dispatchMessageHandleErrors(new OffWatchMsg(s,ua,checked),false);
		}

		boolean handleISON(String found)
		{
			if(pendingISON.isEmpty()) return false;

			// Make map of seeking nicks LC to seeking nicks in requested case
			Map<String, String> seekingNicks=new HashMap<String, String>();
			String[] seekingNicksArray=pendingISON.removeFirst().substring(5).toLowerCase().split(" ");
			for(int i=0;i<seekingNicksArray.length;i++)
			{
				seekingNicks.put(seekingNicksArray[i].toLowerCase(),seekingNicksArray[i]);
			}

			// Map of found nicks, similar
			Map<String, String> foundNicks = new HashMap<String, String>();
			String[] foundNicksArray=found.split(" ");
			for(int i=0;i<foundNicksArray.length;i++)
			{
				foundNicks.put(foundNicksArray[i].toLowerCase(),foundNicksArray[i]);
			}

			// Loop through checking if present. Use the preferred case when reporting.
			for(Iterator<String> i=seekingNicks.keySet().iterator();i.hasNext();)
			{
				String seeking = i.next();
				if(foundNicks.containsKey(seeking))
					markOnline(new IRCUserAddress(foundNicks.get(seeking),"",""));
				else
					markOffline(new IRCUserAddress(seekingNicks.get(seeking),"",""));
			}

			return true;
		}
	}

	/**
	 * @param context Context
	 * @throws GeneralException Error starting up
	 */
	public WatchListSingleton(PluginContext context) throws GeneralException
	{
		this.context = context;

		// Read list from preferences
		Preferences p=context.getSingle(Preferences.class);
		PreferencesGroup pg=p.getGroup(context.getPlugin());
		PreferencesGroup[] ignoreGroups=pg.getChild(IRCPrefs.PREFGROUP_WATCH).getAnon();
		for(int i=0;i<ignoreGroups.length;i++)
		{
			watchList.put(new IRCUserAddress(
				ignoreGroups[i].get(IRCPrefs.PREF_WATCH_NICK),
				ignoreGroups[i].get(IRCPrefs.PREF_WATCH_USER),
				ignoreGroups[i].get(IRCPrefs.PREF_WATCH_HOST)
				),ignoreGroups[i]);
		}

		context.registerMessageOwner(listMsgOwner);
		context.registerMessageOwner(watchMsgOwner);
		context.registerExtraMessageClass(OnWatchMsg.class);
		context.registerExtraMessageClass(OffWatchMsg.class);
		context.requestMessages(UserCommandMsg.class,this,Msg.PRIORITY_NORMAL);
		context.requestMessages(UserCommandListMsg.class,this,Msg.PRIORITY_NORMAL);
		context.requestMessages(ServerConnectionFinishedMsg.class,this,Msg.PRIORITY_EARLY);
		context.requestMessages(ServerConnectedMsg.class,this,Msg.PRIORITY_EARLY);
		context.requestMessages(ServerDisconnectedMsg.class,this,Msg.PRIORITY_EARLY);
		context.requestMessages(NumericIRCMsg.class,this,Msg.PRIORITY_EARLY);
		context.requestMessages(UserSourceIRCMsg.class,this,Msg.PRIORITY_FIRST);

		timedEventID=TimeUtils.addTimedEvent(regularTrigger,15000,true);
	}

	/** Track our timed event */
	private int timedEventID=-1;

	/** Close the timed event so it doesn't hang onto this object */
	void close()
	{
		if(timedEventID!=-1)
			TimeUtils.cancelTimedEvent(timedEventID);
	}

	/** Regular trigger that runs every 15 seconds to do ISON etc */
	private Runnable regularTrigger=new Runnable()
	{
		@Override
		public void run()
		{
			for(Iterator<ServerInfo> i=serverInfo.values().iterator();i.hasNext();)
			{
				i.next().trigger();
			}
			timedEventID=TimeUtils.addTimedEvent(regularTrigger,15000,true);
		}
	};

	/**
	 * Message: Any message from a user (because they're online now).
	 * @param msg Message
	 */
	public void msg(UserSourceIRCMsg msg)
	{
		// If we receive a message from a user and they're offline, that means
		// they must be online now...
		ServerInfo si=serverInfo.get(msg.getServer());
		if(si==null) return;
		if(msg instanceof QuitIRCMsg)
			si.seenOffline(msg.getSourceUser());
		else
			si.seenOnline(msg.getSourceUser());
	}

	/**
	 * Message: WATCH numerics.
	 * @param msg Message
	 */
	public void msg(NumericIRCMsg msg)
	{
		ServerInfo si=serverInfo.get(msg.getServer());
		if(si==null) return;

		switch(msg.getNumeric())
		{
		case NumericIRCMsg.RPL_LOGON:
		case NumericIRCMsg.RPL_NOWON:
			if(msg.getParams().length>3)
			{
				si.markOnline(
					new IRCUserAddress(msg.getParamISO(1),msg.getParamISO(2),msg.getParamISO(3)));
				msg.markHandled();
			}
			break;
		case NumericIRCMsg.RPL_LOGOFF:
		case NumericIRCMsg.RPL_NOWOFF:
			if(msg.getParams().length>3)
			{
				si.markOffline(
					new IRCUserAddress(msg.getParamISO(1),msg.getParamISO(2),msg.getParamISO(3)));
				msg.markHandled();
			}
			break;
		case NumericIRCMsg.RPL_WATCHOFF:
			msg.markHandled();
			break;
		case NumericIRCMsg.RPL_ISON:
			if(msg.getParams().length==2)
			{
				if(si.handleISON(msg.getParamISO(1)))
					msg.markHandled();
			}
			break;
		default:
			return;
		}
	}

	/**
	 * Message: Connected to server.
	 * @param msg Message
	 */
	public void msg(ServerConnectedMsg msg)
	{
		serverInfo.put(msg.getServer(),new ServerInfo(msg.getServer()));
	}

	/**
	 * Message: Complete connection to server.
	 * @param msg Message
	 */
	public void msg(ServerConnectionFinishedMsg msg)
	{
		ServerInfo si=serverInfo.get(msg.getServer());
		si.startup();
	}

	/**
	 * Message: Disconnected from server.
	 * @param msg Message
	 */
	public void msg(ServerDisconnectedMsg msg)
	{
		serverInfo.remove(msg.getServer());
	}

	/**
	 * Message: User command (notify, watch).
	 * @param msg Message
	 */
	public void msg(UserCommandMsg msg)
	{
		if(msg.getCommand()!=null && (msg.getCommand().equals("notify") || msg.getCommand().equals("watch")))
		{
			String[] params=msg.getParams().split(" ");
			if(params.length!=1 || params[0].equals(""))
			{
				msg.error("Incorrect syntax. Use /watch +<key>mask</key> or /watch -<key>mask</key>");
			}
			else
			{
				String mask=params[0];
				boolean add;
				if(mask.startsWith("+"))
				{
					add=true;
					mask=mask.substring(1);
				}
				else if(mask.startsWith("-"))
				{
					add=false;
					mask=mask.substring(1);
				}
				else
				{
					add=true;
				}
				IRCUserAddress ua=new IRCUserAddress(mask,true);
				if(ua.getNick().indexOf('*')!=-1)
				{
					msg.error("Incorrect syntax. /watch masks may not include wildcard in nickname portion");
				}
				else
				{
					if(add)
					{
						if(addMask(ua))
						{
							msg.getMessageDisplay().showInfo("Added to watch list: <key>"+XML.esc(ua.toString())+"</key>");
						}
						else
						{
							msg.getMessageDisplay().showError("Already in watch list: <key>"+XML.esc(ua.toString())+"</key>");
						}
					}
					else
					{
						if(removeMask(ua))
						{
							msg.getMessageDisplay().showInfo("Removed from watch list: <key>"+XML.esc(ua.toString())+"</key>");
						}
						else
						{
							msg.getMessageDisplay().showError("Not in watch list: <key>"+XML.esc(ua.toString())+"</key>");
						}
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
		msg.addCommand(false, "watch", UserCommandListMsg.FREQ_UNCOMMON,
			"/watch <+/-mask>",
			"Start or stop watching a given user mask.");
	}

	@Override
	public synchronized boolean addMask(IRCUserAddress mask)
	{
		if(watchList.containsKey(mask))
			return false;

		Preferences p=context.getSingle(Preferences.class);
		PreferencesGroup newGroup=p.getGroup(context.getPlugin()).getChild(IRCPrefs.PREFGROUP_WATCH).addAnon();
		newGroup.set(IRCPrefs.PREF_WATCH_NICK,mask.getNick());
		newGroup.set(IRCPrefs.PREF_WATCH_USER,mask.getUser());
		newGroup.set(IRCPrefs.PREF_WATCH_HOST,mask.getHost());

		watchList.put(mask,newGroup);
		for(Iterator<ServerInfo> i=serverInfo.values().iterator();i.hasNext();)
		{
			ServerInfo si=i.next();
			si.addPerm(mask);
		}

		listMsgOwner.getDispatch().dispatchMessage(new WatchListChangeMsg(),false);

		return true;
	}

	@Override
	public synchronized boolean removeMask(IRCUserAddress mask)
	{
		if(!watchList.containsKey(mask))
			return false;

		watchList.get(mask).remove();

		watchList.remove(mask);
		for(Iterator<ServerInfo> i=serverInfo.values().iterator();i.hasNext();)
		{
			ServerInfo si=i.next();
			si.removePerm(mask);
		}

		listMsgOwner.getDispatch().dispatchMessage(new WatchListChangeMsg(),false);

		return true;
	}

	@Override
	public synchronized void addTemporaryMask(Server s,IRCUserAddress mask)
	{
		ServerInfo si=serverInfo.get(s);
		if(si==null) throw new BugException("Don't know server "+s);
		si.addTemp(mask);
	}

	@Override
	public synchronized void removeTemporaryMask(Server s,IRCUserAddress mask)
	{
		ServerInfo si=serverInfo.get(s);
		if(si==null) throw new BugException("Don't know server "+s);
		si.removeTemp(mask);
	}

	@Override
	public synchronized IRCUserAddress[] getMasks()
	{
		return watchList.keySet().toArray(new IRCUserAddress[watchList.keySet().size()]);
	}

	@Override
	public synchronized boolean isKnown(Server s,String nick)
	{
		ServerInfo si=serverInfo.get(s);
		if(si==null) throw new BugException("Don't know server "+s);
		return si.isKnown(nick);
	}

	@Override
	public synchronized boolean isOnline(Server s,String nick)
	{
		ServerInfo si=serverInfo.get(s);
		if(si==null) throw new BugException("Don't know server "+s);
		return si.isOnline(nick);
	}
}
