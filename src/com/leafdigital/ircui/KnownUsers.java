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
package com.leafdigital.ircui;

import java.text.SimpleDateFormat;
import java.util.*;

import util.StringUtils;
import util.xml.XML;

import com.leafdigital.irc.api.*;

import leafchat.core.api.*;

/**
 * Class tracks a list of known users so that we can display information about
 * other channels they're in, etc.
 */
public class KnownUsers
{
	private final static long EXPIRY = 12*60*60*1000; // 12 hours

	/** Information about a particular user in a particular channel */
	private class UserChannel
	{
		/**
		 * Nicks that this user currently is operating under in the channel. Note
		 * that it's possible the same user/host combination has multiple nicks.
		 */
		HashSet<String> currentNicks = new HashSet<String>();

		/**
		 * Previously-used nicks (map of nick -> Long of time when the nick ceased
		 * being current)
		 */
		HashMap<String, Long> pastNicks = new HashMap<String, Long>();
	}

	/** Map of information about users */
	private HashMap<Server, Map<String, Map<String, UserChannel>>> users =
		new HashMap<Server, Map<String, Map<String, UserChannel>>>();

	KnownUsers(PluginContext context)
	{
		context.requestMessages(ServerDisconnectedMsg.class,this,Msg.PRIORITY_EARLY);
		context.requestMessages(MinuteMsg.class,this,new MinuteFilter(60));
	}

	private Map<String, UserChannel> findUser(Server s,IRCUserAddress ua)
	{
		Map<String, Map<String, UserChannel>> server = users.get(s);
		if(server==null)
		{
			server = new HashMap<String, Map<String, UserChannel>>();
			users.put(s,server);
		}
		String key = ua.getUser() + "@" + ua.getHost();
		Map<String, UserChannel> user = server.get(key);
		if(user==null)
		{
			user = new HashMap<String, UserChannel>();
			server.put(key,user);
		}
		return user;
	}

	/**
	 * Should be called when somebody (else) joins a channel.
	 * @param s Server
	 * @param chan Channel
	 * @param ua Joiner
	 * @return Either null or a string to add with extra information
	 */
	synchronized String chanJoin(Server s,String chan,IRCUserAddress ua)
	{
		String info;
		Map<String, UserChannel> user = findUser(s, ua);
		UserChannel c=user.get(chan);
		if(c==null)
		{
			// Weren't in this channel recently. Are they in any other current ones?
			LinkedList<String> current = new LinkedList<String>(),
				previous = new LinkedList<String>();
			for(Map.Entry<String, UserChannel> entry : user.entrySet())
			{
				String otherChan = entry.getKey();
				UserChannel otherChanDetails = entry.getValue();
				if(otherChanDetails.currentNicks.isEmpty())
				{
					if(otherChanDetails.pastNicks.containsKey(ua.getNick()))
					{
						// Previously in other channel with same nick
						previous.add(XML.esc(otherChan));
					}
					else
					{
						// Previously in other channel with different nick
						previous.add(XML.esc(otherChan) + " ("
							+ getPastNicks(otherChanDetails.pastNicks, ua.getNick()) + ")");
					}
				}
				else
				{
					if(otherChanDetails.currentNicks.contains(ua.getNick()))
					{
						// Currently in other channel with same nick
						current.add(XML.esc(otherChan));
					}
					else
					{
						// Currently in other channel with different nick
						current.add(XML.esc(otherChan)+" (as <nick>"
							+ XML.esc(otherChanDetails.currentNicks.iterator().next())
							+ "</nick>)");
					}
				}
			}

			if(!current.isEmpty()) // Show current channels if available
			{
				info="<knownuser><nick>"+XML.esc(ua.getNick())+"</nick> is also in "+
				  StringUtils.formatList(current.toArray(new String[0]))+"</knownuser>";
			}
			else if(!previous.isEmpty()) // Otherwise show previous channels
			{
				info="<knownuser><nick>"+XML.esc(ua.getNick())+"</nick> was previously in "+
			  	StringUtils.formatList(previous.toArray(new String[0]))+"</knownuser>";
			}
			else // No information about them at all
				info=null;

			c=new UserChannel();
			user.put(chan,c);
		}
		else
		{
			// OK, they've been here before. Are they still here?
			if(!c.currentNicks.isEmpty())
			{
				String[] list=new String[c.currentNicks.size()];
				int count=0;
				for(String nick : c.currentNicks)
				{
					list[count++]="<nick>"+XML.esc(nick)+"</nick>";
				}
				info="<knownuser><nick>"+XML.esc(ua.getNick())+"</nick> is also here as "+
					StringUtils.formatList(list)+"</knownuser>";
			}
			else
			{
				// Not here now, let's find the most recent
				Map.Entry<String, Long> me = getMostRecentNick(c.pastNicks);
				String recentNick = me.getKey();
				long recentTime = me.getValue().longValue();
				info="<knownuser><nick>"+XML.esc(ua.getNick())+"</nick> was previously here at <key>"+
					(new SimpleDateFormat("HH:mm")).format(new Date(recentTime))+"</key>";
				if(!recentNick.equals(ua.getNick()))
				{
					info += getPastNicks(c.pastNicks, ua.getNick());
				}
				info+="</knownuser>";
			}
		}

		c.currentNicks.add(ua.getNick());
		return info;
	}

	private static String getPastNicks(Map<String, Long> pastNicks,
		String currentNick)
	{
		// Prepare set sorted by time
		TreeSet<Map.Entry<String, Long>> entries =
			new TreeSet<Map.Entry<String, Long>>(
				new Comparator<Map.Entry<String, Long>>()
		{
			@Override
			public int compare(Map.Entry<String, Long> arg0,
				Map.Entry<String, Long> arg1)
			{
				long
					time0 = arg0.getValue().longValue(),
					time1 = arg1.getValue().longValue();
				return (int)(time1-time0);
			}
		});

		// Add all entries except those with same nick as current to the set
		for(Map.Entry<String, Long> me : pastNicks.entrySet())
		{
			if(!me.getKey().toString().equalsIgnoreCase(currentNick))
			{
				entries.add(me);
			}
		}

		if(entries.isEmpty())
		{
			return "";
		}

		String[] nicks = new String[entries.size()];
		int count = 0;
		for(Map.Entry<String, Long> me : entries)
		{
			nicks[count] = "<nick>" + XML.esc(me.getKey()) + "</nick>";
		}
		return " as " + StringUtils.formatList(nicks, 5);
	}

	private static Map.Entry<String, Long> getMostRecentNick(
		Map<String, Long> pastNicks)
	{
		Map.Entry<String, Long> best=null;
		for(Map.Entry<String, Long> me : pastNicks.entrySet())
		{
			long entryTime=me.getValue().longValue();
			if(best==null || entryTime>best.getValue().longValue())
			{
				best = me;
			}
		}
		return best;
	}

	/**
	 * Called to inform about text in a channel.
	 * @param s Server
	 * @param chan Channel
	 * @param ua User
	 */
	public synchronized void chanText(Server s,String chan,IRCUserAddress ua)
	{
		Map<String, UserChannel> user = findUser(s,ua);
		UserChannel c = user.get(chan);
		if(c==null)
		{
			c = new UserChannel();
			user.put(chan,c);
		}
		c.currentNicks.add(ua.getNick());
	}

	/**
	 * Called to inform about a kick in a channel.
	 * @param s Server
	 * @param chan Channel
	 * @param nick User
	 */
	public synchronized void chanKick(Server s,String chan,String nick)
	{
		// Find user
		Map<String, Map<String, UserChannel>> server = users.get(s);
		if(server==null) return;
		for(Map.Entry<String, Map<String, UserChannel>> entry : server.entrySet())
		{
			Map<String, UserChannel> user = entry.getValue();
			UserChannel c=user.get(chan);
			if(c!=null && c.currentNicks.contains(nick))
			{
				// Do the same as for part
				c.pastNicks.put(nick,new Long(System.currentTimeMillis()));
				c.currentNicks.remove(nick);
				return;
			}
		}
	}

	/**
	 * Called to inform about a part in a channel.
	 * @param s Server
	 * @param chan Channel
	 * @param ua User
	 */
	public synchronized void chanPart(Server s,String chan,IRCUserAddress ua)
	{
		Map<String, UserChannel> user = findUser(s,ua);
		UserChannel c=user.get(chan);
		if(c==null)
		{
			c = new UserChannel();
			user.put(chan,c);
		}
		c.pastNicks.put(ua.getNick(),new Long(System.currentTimeMillis()));
		c.currentNicks.remove(ua.getNick());
	}

	/**
	 * Called to inform about a quit in a channel.
	 * @param s Server
	 * @param chan Channel
	 * @param ua User
	 */
	public synchronized void chanQuit(Server s,String chan,IRCUserAddress ua)
	{
		// Actually does the same as part
		chanPart(s,chan,ua);
	}

	/**
	 * Called to inform about nick change in a channel.
	 * @param s Server
	 * @param chan Channel
	 * @param ua User
	 * @param newNick New nickname
	 */
	public synchronized void chanNick(Server s,String chan,IRCUserAddress ua,String newNick)
	{
		Map<String, UserChannel> user = findUser(s,ua);
		UserChannel c=user.get(chan);
		if(c==null)
		{
			c = new UserChannel();
			user.put(chan, c);
		}
		c.pastNicks.put(ua.getNick(),new Long(System.currentTimeMillis()));
		c.currentNicks.remove(ua.getNick());
		c.currentNicks.add(newNick);
	}

	/**
	 * Must be called when user parts from a channel or is kicked from it, etc.
	 * @param s Server
	 * @param chan Channel
	 */
	synchronized void wePart(Server s, String chan)
	{
		Map<String, Map<String, UserChannel>> server = users.get(s);
		if(server==null) return;

		for(Map<String, UserChannel> user : server.values())
		{
			user.remove(chan);
		}
	}

	/**
	 * Message: Server is disconnected.
	 * @param msg
	 */
	synchronized public void msg(ServerDisconnectedMsg msg)
	{
		users.remove(msg.getServer());
	}

	/**
	 * Message: periodic housekeeping per-minute call.
	 * @param msg
	 */
	synchronized public void msg(MinuteMsg msg)
	{
		long now=System.currentTimeMillis();
		for(Iterator<Map<String, Map<String, UserChannel>>> i =
			users.values().iterator(); i.hasNext();)
		{
			Map<String, Map<String, UserChannel>> server = i.next();
			for(Iterator<Map<String, UserChannel>> j =
				server.values().iterator(); j.hasNext();)
			{
				Map<String, UserChannel> user = j.next();
				for(Iterator<UserChannel> k = user.values().iterator(); k.hasNext();)
				{
					UserChannel uc =k.next();
					if(!uc.currentNicks.isEmpty())
					{
						continue;
					}
					for(Iterator<Long> l = uc.pastNicks.values().iterator(); l.hasNext();)
					{
						long time= l.next().longValue();
						if(now-time > EXPIRY)
						{
							l.remove();
						}
					}
					if(uc.pastNicks.isEmpty())
					{
						k.remove();
					}
				}
				if(user.isEmpty())
				{
					j.remove();
				}
			}
			if(server.isEmpty())
			{
				i.remove();
			}
		}
	}
}
