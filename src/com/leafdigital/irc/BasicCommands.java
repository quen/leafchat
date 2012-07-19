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

import java.io.*;
import java.util.*;
import java.util.regex.*;

import util.xml.XML;

import com.leafdigital.irc.api.*;
import com.leafdigital.prefs.api.*;

import leafchat.core.api.*;

/** Class for handling basic command messages */
public class BasicCommands
{
	private static final Set<String> oneParamThenText =
		new HashSet<String>(Arrays.asList(new String[]
	{
		"squit","part","topic","kill"
	}));

	private static final Set<String> twoParamsThenText =
		new HashSet<String>(Arrays.asList(new String[]
	{
		"kick"
	}));

	private PluginContext context;

	BasicCommands(PluginContext pc)
	{
		this.context=pc;
	}

	void handle(UserCommandListMsg msg) throws GeneralException
	{
		// Commands that are handled specifically in this file.
		msg.addCommand(true, "say", UserCommandListMsg.FREQ_OBSCURE,
			"/say <message>", "Say something in the current channel/message.");
		msg.addCommand(true, "msg", UserCommandListMsg.FREQ_COMMON,
			"/msg <nick> <message>", "Send private message to the named user");
		msg.addCommand(true, "me", UserCommandListMsg.FREQ_COMMON,
			"/me <action text>", "Send action message in the current " +
			"channel/message");
		msg.addCommand(true, "raw", UserCommandListMsg.FREQ_UNCOMMON,
			"/raw <command>", "Send a raw IRC command to the server");
		msg.addCommand(true, "quote", UserCommandListMsg.FREQ_UNCOMMON,
			"/quote <command>", "Send a raw IRC command to the server");
		msg.addCommand(true, "ctcp", UserCommandListMsg.FREQ_UNCOMMON,
			"/ctcp <nick> <command> [params]", "Send a CTCP command to the " +
			"named user");
		msg.addCommand(true, "ctcpreply", UserCommandListMsg.FREQ_OBSCURE,
			"/ctcpreply <nick> <command> [params]", "Send a CTCP reply to the " +
			"named user");
		msg.addCommand(true, "nick", UserCommandListMsg.FREQ_COMMON,
			"/nick <new nick>", "Change to a new nickname");
		msg.addCommand(true, "quit", UserCommandListMsg.FREQ_COMMON,
			"/quit [message]", "Close IRC connection to current server, leaving an " +
			"optional message in your channels");
		msg.addCommand(true, "aquit", UserCommandListMsg.FREQ_COMMON,
			"/aquit [message]", "Close IRC connection to all servers, leaving " +
			"optional message on channels");
		msg.addCommand(false, "clear", UserCommandListMsg.FREQ_COMMON,
			"/clear", "Clear the current scrollback window");
		msg.addCommand(false, "echo", UserCommandListMsg.FREQ_OBSCURE,
			"/echo <message>", "<key>Scripting:</key> Display the message in current window");
		msg.addCommand(true, "join", UserCommandListMsg.FREQ_COMMON,
			"/join <channel> [key]", "Join the named channel (include a key if " +
			"required by the channel)");
		msg.addCommand(true, "away", UserCommandListMsg.FREQ_COMMON,
			"/away [message]", "Mark yourself away, with specified message (use " +
			"with no message when you come back)");
		msg.addCommand(true, "ban", UserCommandListMsg.FREQ_COMMON,
			"/ban [channel] <nick>", "Ban a user from a channel; if no channel " +
			"specified, uses current channel");
		msg.addCommand(true, "kick", UserCommandListMsg.FREQ_COMMON,
			"/kick [channel] <nick>",	"Kick a user from a channel; if no channel " +
			"specified, uses current channel");
		msg.addCommand(true, "squit", UserCommandListMsg.FREQ_OBSCURE,
			"/squit <server> <comment>", "<key>IRCop:</key> Disconnect a remote server");
		msg.addCommand(true, "part", UserCommandListMsg.FREQ_COMMON,
			"/part <channel>", "Leave the named channel");
		msg.addCommand(true, "topic", UserCommandListMsg.FREQ_COMMON,
			"/topic <channel> [new topic]", "View or (if topic specified) change " +
			"the topic for a channel");
		msg.addCommand(true, "kill", UserCommandListMsg.FREQ_OBSCURE,
			"/kill <nick> <comment>",	"<key>IRCop:</key> Cause user to be disconnected from " +
			"the server, with supplied message");

		// Other commands from RFC1459 that can be typed by user
		msg.addCommand(true, "oper", UserCommandListMsg.FREQ_OBSCURE,
			"/oper <user> <password>", "<key>IRCop:</key> Enable operator privileges");
		msg.addCommand(true, "mode", UserCommandListMsg.FREQ_COMMON,
			"/mode <channel/nick> <flags> [params]", "Set channel or nickname modes");
		msg.addCommand(true, "names", UserCommandListMsg.FREQ_UNCOMMON,
			"/names <channel>", "<key>Low-level:</key> List all nicknames in a channel");
		msg.addCommand(true, "list", UserCommandListMsg.FREQ_UNCOMMON,
			"/list", "List channels on a server (note: most servers support extra " +
			"parameters, without which this command is pretty useless)");
		msg.addCommand(true, "invite", UserCommandListMsg.FREQ_UNCOMMON,
			"/invite <nick> <channel>", "Invite somebody to a channel");
		msg.addCommand(true, "version", UserCommandListMsg.FREQ_UNCOMMON,
			"/version [server]", "Show server version (by default, for current " +
			"server)");
		msg.addCommand(true, "stats", UserCommandListMsg.FREQ_OBSCURE,
			"/stats [query] [server]", "<key>IRCop:</key> Returns server information");
		msg.addCommand(true, "links", UserCommandListMsg.FREQ_UNCOMMON,
			"/links [server] [server mask]", "List all servers known by the " +
			"current/specified server (matching the mask, if supplied)");
		msg.addCommand(true, "time", UserCommandListMsg.FREQ_UNCOMMON,
			"/time [server]", "Show local time from current or specified server");
		msg.addCommand(true, "connect", UserCommandListMsg.FREQ_OBSCURE,
			"/connect <target> [port] [existing]", "<key>IRCop:</key> Cause an existing " +
			"server to connect to the target server");
		msg.addCommand(true, "trace", UserCommandListMsg.FREQ_OBSCURE,
			"/trace <server>", "Show route to named server");
		msg.addCommand(true, "admin", UserCommandListMsg.FREQ_OBSCURE,
			"/admin [server]", "Show information about administrator of current " +
			"or named server");
		msg.addCommand(true, "info", UserCommandListMsg.FREQ_OBSCURE,
			"/info [server]", "Show miscellaneous information about current or " +
			"named server");
		msg.addCommand(true, "privmsg", UserCommandListMsg.FREQ_OBSCURE,
			"/privmsg <channel/nick> <text>",	"<key>Low-level:</key> Send ordinary text to " +
			"channel or nickname");
		msg.addCommand(true, "notice", UserCommandListMsg.FREQ_COMMON,
			"/notice <nick> <text>", "Send a notice (message which doesn't appear " +
			"in its own window); some servers also let you send notices to " +
			"channels or other groups");
		msg.addCommand(true, "who", UserCommandListMsg.FREQ_COMMON,
			"/who <channel/other> [o]", "List all users in current channel " +
			"(optional 'o' flag restricts to ops)");
		msg.addCommand(true, "whois", UserCommandListMsg.FREQ_COMMON,
			"/whois [server] <nick>",	"Display information about user");
		msg.addCommand(true, "whowas", UserCommandListMsg.FREQ_UNCOMMON,
			"/whowas <nick> [count] [server]", "Display information about a user " +
			"who was previously corrected");
		msg.addCommand(true, "pong", UserCommandListMsg.FREQ_OBSCURE,
			"/pong <param> [param]", "<key>Low-level:</key> Reply to a server PING message");
		msg.addCommand(true, "rehash", UserCommandListMsg.FREQ_OBSCURE,
			"/rehash", "<key>IRCop:</key> Make server reload its configuration file");
		msg.addCommand(true, "restart", UserCommandListMsg.FREQ_OBSCURE,
			"/restart", "<key>IRCop:</key> Restart server");
		msg.addCommand(true, "users", UserCommandListMsg.FREQ_OBSCURE,
			"/users [server]", "List users on the given or current server " +
			"(normally disabled)");
		msg.addCommand(true, "userhost", UserCommandListMsg.FREQ_UNCOMMON,
			"/userhost <nickname> [nickname]*",	"Show username and host for each " +
			"nickname");
		msg.addCommand(true, "ison", UserCommandListMsg.FREQ_UNCOMMON,
			"/ison <nickname> [nickname]*", "<key>Low-level:</key> List which of the given " +
			"users are on IRC");
	}

	void handle(UserCommandMsg msg) throws GeneralException
	{
		if(msg.isHandled())
		{
			return;
		}
		String command=msg.getCommand();
		if(command==null||command.equals("say"))
		{
			say(msg);
		}
		else if(command.equals("msg"))
		{
			msg(msg);
		}
		else if(command.equals("me"))
		{
			me(msg);
		}
		else if(command.equals("raw")||command.equals("quote"))
		{
			raw(msg);
		}
		else if(command.equals("ctcp"))
		{
			ctcp(msg);
		}
		else if(command.equals("ctcpreply"))
		{
			ctcpreply(msg);
		}
		else if(command.equals("nick"))
		{
			nick(msg);
		}
		else if(command.equals("quit"))
		{
			quit(msg);
		}
		else if(command.equals("aquit"))
		{
			aquit(msg);
		}
		else if(command.equals("silence"))
		{
			silence(msg);
		}
		else if(command.equals("clear"))
		{
			clear(msg);
		}
		else if(command.equals("echo"))
		{
			echo(msg);
		}
		else if(command.equals("join"))
		{
			join(msg);
		}
		else if(command.equals("away"))
		{
			away(msg);
		}
		else if(command.equals("ban"))
		{
			ban(msg);
		}
		else if(oneParamThenText.contains(command))
		{
			generic(msg,1);
		}
		else if(twoParamsThenText.contains(command))
		{
			generic(msg,2);
		}
	}

	private void silence(UserCommandMsg msg) throws GeneralException
	{
		msg.error(
			"Please use <key>/ignore</key>, not /silence. leafChat automatically uses "
			+ "/silence on ignored users whenever the server supports it.");
		msg.markHandled();
	}

	private void clear(UserCommandMsg msg) throws GeneralException
	{
		msg.getMessageDisplay().clear();
		msg.markHandled();
	}

	private void echo(UserCommandMsg msg) throws GeneralException
	{
		msg.getMessageDisplay().showInfo(XML.esc(msg.getParams()));
		msg.markHandled();
	}

	byte[] convertEncoding(String text, Server s, String chan, IRCUserAddress user)
	{
		// Get encoding
		IRCEncoding ie = context.getSingle(IRCEncoding.class);
		IRCEncoding.EncodingInfo ei = ie.getEncoding(s, chan, user);
		return ei.convertOutgoing(text);
	}

	byte[] convertEncoding(String text, Server s, String chanOrNick)
	{
		if(s.getChanTypes().indexOf(chanOrNick.charAt(0)) != -1)
		{
			// Channel
			return convertEncoding(text,s,chanOrNick,null);
		}
		else
		{
			// Nick
			return convertEncoding(text,s,null,new IRCUserAddress(chanOrNick,false));
		}
	}

	private void say(UserCommandMsg msg) throws GeneralException
	{
		if(msg.getServer()==null
			|| (msg.getContextChan()==null && msg.getContextUser()==null))
		{
			msg.error("You can only talk in channel or message windows.");
			return;
		}
		if(!msg.getServer().isConnected())
		{
			msg.error("Not connected.");
			return;
		}
		if(!msg.getParams().equals(""))
		{
			String target;
			if(msg.getContextChan() != null)
			{
				target = msg.getContextChan();
			}
			else
			{
				target = msg.getContextUser().getNick();
			}
			msg.getServer().sendLine(
				IRCMsg.constructBytes("PRIVMSG " + target + " :", convertEncoding(
					msg.getParams(), msg.getServer(), msg.getContextChan(),
					msg.getContextUser())));
			msg.getMessageDisplay().showOwnText(MessageDisplay.TYPE_MSG, target,
				msg.getParams());
		}
		msg.markHandled();
	}

	private void me(UserCommandMsg msg) throws GeneralException
	{
		if(msg.getServer()==null
			|| (msg.getContextChan()==null && msg.getContextUser()==null))
		{
			msg.error("You can only talk in channel or message windows.");
			return;
		}
		if(!msg.getServer().isConnected())
		{
			msg.error("Not connected.");
			return;
		}
		String target;
		if(msg.getContextChan() != null)
		{
			target = msg.getContextChan();
		}
		else
		{
			target = msg.getContextUser().getNick();
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try
		{
			out.write(("PRIVMSG " + target + " :\u0001ACTION ").getBytes(
				"ISO-8859-1"));
			out.write(convertEncoding(msg.getParams(), msg.getServer(),
				msg.getContextChan(), msg.getContextUser()));
			out.write(1);
		}
		catch(IOException e)
		{
			throw new BugException("Oh come on.");
		}
		msg.getServer().sendLine(out.toByteArray());
		msg.getMessageDisplay().showOwnText(MessageDisplay.TYPE_ACTION, target,
			msg.getParams());
		msg.markHandled();
	}

	private void msg(UserCommandMsg msg) throws GeneralException
	{
		if(!checkConnected(msg))
		{
			return;
		}
		int space = msg.getParams().indexOf(' ');
		if(space==-1 || space==msg.getParams().length()-1)
		{
			msg.error(
				"Incorrect syntax. Use: /msg &lt;nickname or channel> &lt;text>");
			msg.markHandled();
			return;
		}
		String
			target = msg.getParams().substring(0,space),
			message=msg.getParams().substring(space+1);
		msg.getServer().sendLine(
			IRCMsg.constructBytes("PRIVMSG " + target + " :",
				convertEncoding(message, msg.getServer(), target)));
		msg.getMessageDisplay().showOwnText(MessageDisplay.TYPE_MSG, target,
			message);
		msg.markHandled();
	}

	private void raw(UserCommandMsg msg) throws GeneralException
	{
		if(!checkConnected(msg))
		{
			return;
		}

		msg.getServer().sendLine(IRCMsg.constructBytes("",
				convertEncoding(msg.getParams(), msg.getServer(),	null, null)));
		msg.markHandled();
	}

	private void join(UserCommandMsg msg) throws GeneralException
	{
		if(!checkConnected(msg))
		{
			return;
		}

		// Send command
		msg.getServer().sendLine(IRCMsg.constructBytes(
			"JOIN ", convertEncoding(msg.getParams(), msg.getServer(), null, null)));

		// Automatically remember channel in favourites (but not if key is
		// supplied)
		String[] params = msg.getParams().split(" +", 2);
		if(params.length == 1)
		{
			// Note, this code is basically the same as code in JoinTool.java
			String channel = params[0];

			Preferences p = context.getSingle(Preferences.class);

			// Obtain prefs for server, or network if it belongs to one
			PreferencesGroup pg = msg.getServer().getPreferences().getAnonParent();
			if(pg.get(IRCPrefs.PREF_NETWORK, null) == null)
			{
			  pg=msg.getServer().getPreferences();
			}

			// Get channels group
			PreferencesGroup channels = pg.getChild(IRCPrefs.PREFGROUP_CHANNELS);
			PreferencesGroup[] existing = channels.getAnon();
			boolean found = false;
			for(int i=0; i<existing.length; i++)
			{
				String name = existing[i].get(IRCPrefs.PREF_NAME);
				if(name.equalsIgnoreCase(channel))
				{
					found = true;
					break;
				}
			}
			if(!found)
			{
				PreferencesGroup newChan = channels.addAnon();
				newChan.set(IRCPrefs.PREF_NAME, channel);
				newChan.set(IRCPrefs.PREF_KEY, "");
				newChan.set(IRCPrefs.PREF_AUTOJOIN, p.fromBoolean(false));
			}
		}
	}

	private boolean checkConnected(UserCommandMsg msg) throws GeneralException
	{
		if(msg.getServer() == null)
		{
			msg.error("You must send this command to a specified server.");
			return false;
		}
		if(!msg.getServer().isConnected())
		{
			msg.error("Not connected.");
			return false;
		}
		return true;
	}

	private void ctcp(UserCommandMsg msg) throws GeneralException
	{
		if(!checkConnected(msg))
		{
			return;
		}

		String[] params = msg.getParams().split(" ", 3);
		if(params.length<2 || params[0].equals("") || params[1].equals(""))
		{
			msg.error("Syntax: /ctcp &lt;target> &lt;command> [params]");
			return;
		}
		if(params.length==2 && params[1].equalsIgnoreCase("ping"))
		{
			String[] pingWithTime = new String[3];
			pingWithTime[0] = params[0];
			pingWithTime[1] = "PING";
			pingWithTime[2] = (System.currentTimeMillis()/1000L)+"";
			params = pingWithTime;
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try
		{
			baos.write(("PRIVMSG " + params[0] + " :\u0001" +
				params[1].toUpperCase()).getBytes("ISO-8859-1"));
			if(params.length >= 3)
			{
				baos.write(convertEncoding(
					" " + params[2], msg.getServer(), params[0]));
			}
			baos.write(1);
		}
		catch(IOException e)
		{
			throw new BugException("Oh come on.");
		}
		msg.getServer().sendLine(baos.toByteArray());
		msg.markHandled();
	}

	private void ctcpreply(UserCommandMsg msg) throws GeneralException
	{
		if(!checkConnected(msg))
		{
			return;
		}

		String[] params = msg.getParams().split(" ", 3);
		if(params.length<2 || params[0].equals("") || params[1].equals(""))
		{
			msg.error("Syntax: /ctcpreply &lt;target> &lt;command> [params]");
			return;
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try
		{
			baos.write(("NOTICE " + params[0] + " :\u0001" +
				params[1].toUpperCase()).getBytes("ISO-8859-1"));
			if(params.length >= 3)
			{
				baos.write(convertEncoding(params[2], msg.getServer(), params[0]));
			}
			baos.write(1);
		}
		catch(IOException e)
		{
			throw new BugException("Oh come on.");
		}
		msg.getServer().sendLine(baos.toByteArray());
		msg.markHandled();
	}

	private void away(UserCommandMsg msg) throws GeneralException
	{
		Preferences p = context.getSingle(Preferences.class);
		PreferencesGroup pg = p.getGroup(context.getPlugin());
		boolean awayMultiServer = p.toBoolean(pg.get(
			IRCPrefs.PREF_AWAYMULTISERVER, IRCPrefs.PREFDEFAULT_AWAYMULTISERVER));

		if(awayMultiServer)
		{
			Connections c = context.getSingle(Connections.class);
			Server[] servers = c.getConnected();
			for(int i=0; i<servers.length; i++)
			{
				byte[] command;
				if(msg.getParams().length() > 0)
				{
					command = IRCMsg.constructBytes("AWAY :",
						convertEncoding(msg.getParams(), servers[i], null, null));
				}
				else
				{
					command = IRCMsg.constructBytes("AWAY");
				}
				servers[i].sendLine(command);
			}
		}
		else
		{
			generic(msg, 0);
		}

		msg.markHandled();
	}

	private void generic(UserCommandMsg msg, int numParams) throws GeneralException
	{
		if(!checkConnected(msg))
		{
			return;
		}

		String[] params = msg.getParams().split(" +", numParams+1);
		if(params.length < numParams)
		{
			msg.error("Syntax: /" + msg.getCommand().toLowerCase()
				+ " requires at least " + numParams + " parameters");
			return;
		}
		String command = msg.getCommand().toUpperCase();
		for(int i=0; i<numParams; i++)
		{
			command += " " + params[i];
		}
		if(params.length > numParams)
		{
			msg.getServer().sendLine(IRCMsg.constructBytes(command + " :",
				convertEncoding(params[numParams], msg.getServer(), null, null)));
		}
		else
		{
			msg.getServer().sendLine(IRCMsg.constructBytes(command));
		}
		msg.markHandled();
	}

	private void quit(UserCommandMsg msg) throws GeneralException
	{
		if(!checkConnected(msg))
		{
			return;
		}

		String message = msg.getParams();
		if(message.equals(""))
		{
			message = msg.getServer().getQuitMessage();
		}
		msg.getServer().sendLine(
			IRCMsg.constructBytes("QUIT :", convertEncoding(message, msg.getServer(),
				null, null)));
		msg.markHandled();
	}

	private void aquit(UserCommandMsg msg) throws GeneralException
	{
		Connections connections =
			context.getSingle(Connections.class);
		Server[] connected = connections.getConnected();
		if(connected.length == 0)
		{
			msg.error("Not currently connected to any servers");
			return;
		}
		String message = msg.getParams();
		if(message.equals(""))
		{
			message = msg.getServer().getQuitMessage();
		}
		byte[] quitLine = IRCMsg.constructBytes("QUIT :",
			convertEncoding(message, msg.getServer(),	null, null));
		for(int i=0; i<connected.length; i++)
		{
			connected[i].sendLine(quitLine);
		}
		msg.markHandled();
	}

	private void nick(UserCommandMsg msg) throws GeneralException
	{
		if(!checkConnected(msg))
		{
			return;
		}

		String nick = msg.getParams();
		if(nick.matches("\\S+"))
		{
			msg.getServer().sendLine(IRCMsg.constructBytes("NICK "+nick));

			((ServerConnection)msg.getServer()).identify(nick,
				Server.IDENTIFYEVENT_NICK);
			msg.markHandled();
		}
		else
		{
			msg.error("Syntax: /nick &lt;name>");
		}
	}

	private static class BanRequest
	{
		private final static long TIMEOUT = 30000L;

		private Server server;
		private String chan, nick;
		private long time;

		private BanRequest(Server server, String chan, String nick)
		{
			this.server = server;
			this.chan = chan;
			this.nick = nick;
			this.time = System.currentTimeMillis();
		}
	}

	private List<BanRequest> banRequests = new LinkedList<BanRequest>();
	private int banNumericRequestId = -1;

	private final static Pattern CHANNEL_NICK_REGEX = Pattern.compile(
		"^([#+&]\\S+) (\\S+)$");
	private final static Pattern NICK_REGEX = Pattern.compile(
		"^(\\S+)$");

	private void ban(UserCommandMsg msg) throws GeneralException
	{
		if(!checkConnected(msg))
		{
			return;
		}

		String nick = null, chan = null;
		Matcher m = CHANNEL_NICK_REGEX.matcher(msg.getParams());
		if(m.matches())
		{
			chan = m.group(1);
			nick = m.group(2);
		}
		else if(msg.getContextChan() != null)
		{
			m = NICK_REGEX.matcher(msg.getParams());
			if(m.matches())
			{
				chan = msg.getContextChan();
			}
			nick = msg.getParams();
		}

		if(chan != null)
		{
			BanRequest request = new BanRequest(msg.getServer(), chan, nick);
			synchronized(banRequests)
			{
				if(banRequests.isEmpty())
				{
					banNumericRequestId = context.requestMessages(NumericIRCMsg.class, this,
						new NumericFilter(NumericIRCMsg.RPL_USERHOST), Msg.PRIORITY_FIRST);
				}
				banRequests.add(request);
			}
			msg.getServer().sendLine(IRCMsg.constructBytes("USERHOST " + request.nick));
			msg.markHandled();
		}
		else
		{
			msg.error("Syntax: /ban &lt;channel&gt; &lt;name>");
		}
	}

	private final static Pattern SINGLE_USERHOST_REGEX = Pattern.compile(
		"^(\\S+)\\*?=[+-]([^@ ]+\\@)?(\\S*)\\s*$");

	/**
	 * Numeric IRC messages: the ban command relies on RPL_USERHOST.
	 * @param msg Message
	 */
	public void msg(NumericIRCMsg msg)
	{
		// Check message format
		Matcher m = SINGLE_USERHOST_REGEX.matcher(msg.getParamISO(1));
		if(!m.matches())
		{
			// Maybe somebody else doing a userhost for more than one person
			return;
		}
		String nick = m.group(1), host = m.group(3);

		long now = System.currentTimeMillis();
		synchronized(banRequests)
		{
			// Loop through all outstanding ban request
			for(Iterator<BanRequest> i=banRequests.iterator(); i.hasNext();)
			{
				BanRequest request = i.next();

				// If timeout expired, cancel request
				if(request.time + BanRequest.TIMEOUT < now)
				{
					i.remove();
					continue;
				}

				// If it matched
				if(msg.getServer() == request.server && nick.equalsIgnoreCase(request.nick))
				{
					msg.getServer().sendLine(IRCMsg.constructBytes("MODE " + request.chan
						+ " +b *!*@" + host));

					msg.markHandled();
					i.remove();
					break;
				}
			}
			if(banRequests.isEmpty())
			{
				context.unrequestMessages(NumericIRCMsg.class, this, banNumericRequestId);
				banNumericRequestId = -1;
			}
		}
	}
}
