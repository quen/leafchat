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
import java.util.regex.*;

import org.w3c.dom.*;

import util.*;
import util.xml.*;

import com.leafdigital.irc.api.*;
import com.leafdigital.irc.api.Server.StatusPrefix;
import com.leafdigital.ircui.api.IRCActionListMsg;
import com.leafdigital.logs.api.Logger;
import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/**
 * Chat windows based on an IRC server.
 */
public abstract class ServerChatWindow extends ChatWindow
{
	private Server s;
	private boolean connected=true;

	/** Panel used to display 'away' button. */
	public HorizontalPanel awayUI;

	protected Server getServer() { return s; }
	protected boolean isConnected() { return connected; }

	int requestIDServerDisconnected=-1;

	private final String
		INTERNALACTION_RECONNECT="Reconnect",
	 	INTERNALACTION_JOIN="Join";

	/** Special class that changes the window title when the server name changes. */
	public class TitleChanger
	{
		/**
		 * Server message: updates the title.
		 * @param msg Message
		 */
		public void msg(ServerMsg msg)
		{
			setTitle();
		}
	}
	private Object oTitleChanger=new TitleChanger();

	/** Class that handles the display or otherwise of the 'away' button. */
	public class AwayHandler
	{
		/**
		 * IRC message: numeric (used for away, unaway).
		 * @param msg Message
		 */
		public void msg(NumericIRCMsg msg)
		{
			switch(msg.getNumeric())
			{
			case NumericIRCMsg.RPL_NOWAWAY:
				awayUI.setVisible(true);
				break;
			case NumericIRCMsg.RPL_UNAWAY:
				awayUI.setVisible(false);
				break;
			}
		}
	}
	private Object awayHandler=new AwayHandler();

	/**
	 * Constructs a server-based chat window.
	 * @param context Plugin context
	 * @param s Server
	 * @param xmlFile Name of xml file within ircui package, excluding ".xml"
	 * @param showNow If true, shows before exiting constructor
	 * @param visible If true, appears popped-up, otherwise minimised
	 * @throws GeneralException
	 */
	public ServerChatWindow(PluginContext context,Server s,String xmlFile,
		boolean showNow,boolean visible) throws GeneralException
	{
		super(context, xmlFile, showNow, visible);

		// Start listening and setup server
		context.requestMessages(ServerConnectionFinishedMsg.class,this,null,Msg.PRIORITY_EARLY);
		initServer(s,true);

		// Redo title when server state changes
		context.requestMessages(ServerMsg.class,oTitleChanger,null,Msg.PRIORITY_EARLY);

		// Tell spare window it should close
		if(!(this instanceof SpareWindow))
		{
			((IRCUIPlugin)context.getPlugin()).gotNonSpareWindow(s);
		}
	}

	@Override
	protected void internalAction(Element e) throws GeneralException
	{
		String text=XML.getText(e);
		if(text.equals(INTERNALACTION_RECONNECT))
		{
			if(s.isConnected()) return;
			if(getPluginContext().getSingleton2(UI.class).showQuestion(getWindow(),"Reconnect",
				"You can reconnect to server "+oldServerHost+", port "+oldServerPort+".",
				UI.BUTTON_YES|UI.BUTTON_CANCEL,"Reconnect",null,null,UI.BUTTON_YES)==
				UI.BUTTON_YES)
			{
				((IRCUIPlugin)getPluginContext().getPlugin()).reconnect(
					s.getReportedOrConnectedHost(),s.getConnectedPort());
			}
		}
		if(text.equals(INTERNALACTION_JOIN))
		{
			if(!s.isConnected()) return;
			try
			{
				String chan=XML.getText(XML.getChild(e.getParentNode(),"chan"));
				s.sendLine(IRCMsg.constructBytes("JOIN "+chan));
			}
			catch(XMLException e1)
			{
				return;
			}
		}
	}

	/**
	 * Initialises the window to handle a particular server. Used on construct
	 * and if the server changes.
	 * @param s Server, or null if disconnect
	 * @param firstTime True if this is initial setup
	 */
	private void initServer(Server s,boolean firstTime)
	{
		if(s!=null)	this.s=s;

		// Unrequest existing messages.
		if(requestIDServerDisconnected!=-1)
		{
			getPluginContext().unrequestMessages(ServerDisconnectedMsg.class,this,requestIDServerDisconnected);
			requestIDServerDisconnected=-1;
		}
		getPluginContext().unrequestMessages(null,awayHandler,PluginContext.ALLREQUESTS);

		// Request new ones
		if(s!=null)
		{
			requestIDServerDisconnected=getPluginContext().requestMessages(
				ServerDisconnectedMsg.class,this,new ServerFilter(s),Msg.PRIORITY_EARLY);

			if(awayUI!=null) getPluginContext().requestMessages(
				NumericIRCMsg.class,awayHandler,new ServerFilter(s),Msg.PRIORITY_EARLY);
		}

		// Set up other variables
		connected=s!=null;
		if(commandUI!=null) commandUI.setEnabled(s!=null);

		if(!firstTime) initServerInner(s,false);

		// Tell spare window it should close
		if(s!=null && !(this instanceof SpareWindow))
		{
			((IRCUIPlugin)getPluginContext().getPlugin()).gotNonSpareWindow(s);
		}
	}

	/**
	 * Overridable equivalent of initServer. Not called automatically on first
	 * run, so must be called from constructor of subclasses.
	 * @param s Server, or null if disconnect
	 * @param firstTime True if this is initial setup
	 */
	protected void initServerInner(Server s,boolean firstTime)
	{
	}

	/** Should set window title. Called whenever server connections change. Default does nothing. */
	protected void setTitle()
	{
	}

	@Override
	public void windowClosed() throws GeneralException
	{
		getPluginContext().unrequestMessages(null,oTitleChanger,PluginContext.ALLREQUESTS);
		getPluginContext().unrequestMessages(null,awayHandler,PluginContext.ALLREQUESTS);
		super.windowClosed();
	}

	private String oldServerHost;
	private int oldServerPort;

	/**
	 * Server message: disconnected. Used to display text.
	 * @param msg Message
	 * @throws GeneralException
	 */
	public void msg(ServerDisconnectedMsg msg) throws GeneralException
	{
		if(s==null)
		{
			return;
		}
		oldServerHost=s.getReportedOrConnectedHost();
		oldServerPort=s.getConnectedPort();
		addLine("[Disconnected] (<internalaction>"+INTERNALACTION_RECONNECT+"</internalaction>)");
		initServer(null,false);
	}

	/**
	 * Server message: fully connected. Used to re-init with the new server.
	 * @param msg Message
	 * @throws GeneralException
	 */
	public void msg(ServerConnectionFinishedMsg msg) throws GeneralException
	{
		if(connected || s==null)
		{
			return;
		}

		// OK, if we're disconnected and reconnect to same server or network, set
		// things up again
		if(msg.getServer().getReportedOrConnectedHost().equals(
			s.getReportedOrConnectedHost()))
		{
			addLine("[Reconnected]");
			initServer(msg.getServer(),false);
		}
		else
		{
			String previousNetwork=s.getPreferences().getAnonParent().get(IRCPrefs.PREF_NETWORK,null);

			if(previousNetwork!=null && previousNetwork.equals(
				msg.getServer().getPreferences().getAnonParent().get(IRCPrefs.PREF_NETWORK,null)))
			{
				addLine("[Reconnected]");
				initServer(msg.getServer(),false);
			}
		}
	}

	/** Handler for the special 'boxed' multi-line message display. */
	public class CurrentBox implements Runnable
	{
		BoxedMessage bm;
		NumericIRCMsg start,end;
		List<NumericIRCMsg> middle = new LinkedList<NumericIRCMsg>();
		long lastValid;
		private int request=-1;

		CurrentBox(NumericIRCMsg start,BoxedMessage bm)
		{
			if(bm.hasHeading())
				this.start=start;
			else
				middle.add(start);
			this.bm=bm;
			if(!bm.isEndKnown())
			{
				// If the end is not known, we request all messages and stop after
				// we get anything that doesn't match.
				request=getPluginContext().requestMessages(
					IRCMsg.class,this,new ServerFilter(getServer()),Msg.PRIORITY_FIRST);

				// Also set a timer and end it 1 second after receiving the last
				lastValid=System.currentTimeMillis();
				TimeUtils.addTimedEvent(this,1000,true);
			}
		}

		@Override
		public void run()
		{
			if(System.currentTimeMillis() - lastValid > 1000)
				endIt();
			else
				TimeUtils.addTimedEvent(this,1000,true);
		}

		/**
		 * Handles any IRC message to possibly cancel the box.
		 * @param msg Message
		 */
		public void msg(IRCMsg msg)
		{
			if(!(msg instanceof NumericIRCMsg) || !bm.matchesMiddle((NumericIRCMsg)msg))
				endIt();
		}

		private void endIt()
		{
			if(currentBox==this)
			{
				bm.display(this,ServerChatWindow.this);
				currentBox=null;
			}
			if(request!=-1)
				getPluginContext().unrequestMessages(IRCMsg.class,this,request);
		}

		boolean check(NumericIRCMsg nim)
		{
			if(bm.matchesEnd(nim))
			{
				end=nim;
				endIt();
				return true;
			}
			else if(bm.matchesMiddle(nim))
			{
				lastValid=System.currentTimeMillis();
				middle.add(nim);
				return true;
			}
			else
			{
				endIt();
				return false;
			}
		}
	}
	private CurrentBox currentBox=null;

	private static class BoxedMessage
	{
		String tag;
		int start=0,end=0;
		int[] middle;

		boolean isEndKnown()
		{
			return end!=0;
		}

		BoxedMessage(String tag,int start,int end)
		{
			this.tag=tag;
			this.start=start;
			this.end=end;
		}
		BoxedMessage(String tag,int[] middle)
		{
			this.tag=tag;
			this.middle=middle;
		}

		boolean hasHeading()
		{
			return true;
		}

		boolean matchesStart(NumericIRCMsg nim)
		{
			if(middle!=null)
				return matchesMiddle(nim);
			else
				return nim.getNumeric()==start;
		}
		boolean matchesEnd(NumericIRCMsg nim)
		{
			if(middle!=null)
				return false;
			else
				return nim.getNumeric()==end;
		}
		boolean matchesMiddle(NumericIRCMsg nim)
		{
			if(middle!=null)
			{
				for(int i=0;i<middle.length;i++)
				{
					if(middle[i]==nim.getNumeric()) return true;
				}
				return false;
			}
			else
				return true;
		}

		String getText(ServerChatWindow cw,NumericIRCMsg nim)
		{
			return "<line>"+esc(getNumericText(nim))+"</line>";
		}

		void display(CurrentBox cb,ServerChatWindow cw)
		{
			StringBuffer xml=new StringBuffer();
			xml.append("<"+tag+"><box>");
			if(hasHeading()) xml.append("<boxstart>"+getText(cw,cb.start).replaceAll("</?line>","")+"</boxstart>");
			for(NumericIRCMsg numeric : cb.middle)
			{
				xml.append(getText(cw, numeric));
			}
			xml.append("</box></"+tag+">");
			cw.addLine(xml.toString(),true,null,true);
		}
	}

	private static class BoxedMessageWhois extends BoxedMessage
	{
		BoxedMessageWhois()
		{
			super("whois",NumericIRCMsg.RPL_WHOISUSER,NumericIRCMsg.RPL_ENDOFWHOIS);
		}
		@Override
		String getText(ServerChatWindow cw,NumericIRCMsg m)
		{
			switch(m.getNumeric())
			{
			case NumericIRCMsg.RPL_WHOISUSER:
				if(m.getParams().length==6)
				{
					IRCUserAddress ua=new IRCUserAddress(m.getParamISO(1),m.getParamISO(2),m.getParamISO(3));
					return "<line><nick>"+esc(ua.getNick())+"</nick> ("+esc(ua.getUser())+"@"+
						esc(ua.getHost())+") - "+esc(m.convertEncoding(m.getParams()[5]))+"</line>";
				}
				break;
			case NumericIRCMsg.RPL_WHOISSERVER:
				if(m.getParams().length==4)
				{
					return "<line>Connected to <key>"+esc(m.getParamISO(2))+"</key> - "+esc(m.getParamISO(3))+"</line>";
				}
				break;
			case NumericIRCMsg.RPL_WHOISIDLE:
				String value=null;
				if(m.getParams().length>=4 && m.isParamInteger(2)) // RFC1459 standard version
				{
					long ms=1000L*Long.parseLong(m.getParamISO(2));
					if(ms>60000)
						value="<line>Idle since <key>"+displayTime(System.currentTimeMillis()-ms)+"</key> (<key>"+
							StringUtils.displayMilliseconds(ms)+"</key>)</line>";
					else
						value="<line>Idle for <key>"+StringUtils.displayMilliseconds(ms)+"</key></line>";
				}
				if(m.getParams().length==5 && m.isParamInteger(3)) // Extended version with signon time
				{
					value+="<line>Signed on at <key>"+displayTime(Long.parseLong(m.getParamISO(3))*1000L)+"</key></line>";
				}
				if(value!=null) return value;
				break;
			case NumericIRCMsg.RPL_WHOISREGNICK: // Nonstandard, used on sorcery.net esper.net dal.net
				if(m.getParams().length==3)
				{
					return "<line>"+esc(StringUtils.capitalise(m.getParamISO(2))).replaceAll(
						"(identified|registered)","<key>$1</key>")+"</line>";
				}
				break;
			case NumericIRCMsg.RPL_WHOISOPERATOR:
				if(m.getParams().length==3)
				{
					return "<line>"+esc(StringUtils.capitalise(m.getParamISO(2))).replaceAll(
						"(operator)","<key>$1</key>")+"</line>";
				}
				break;
			case NumericIRCMsg.RPL_WHOISMASKED:
				if(m.getParams().length==3)
				{
					return "<line>"+esc(StringUtils.capitalise(m.getParamISO(2))).replaceAll(
						"([a-z0-9]+(\\.[a-z0-9]+)+)","<key>$1</key>")+"</line>";
				}
				break;
			case NumericIRCMsg.RPL_WHOISHOST:
				if(m.getParams().length==3)
				{
					return "<line>"+esc(StringUtils.capitalise(m.getParamISO(2))).replaceAll(
						" ([^ ]+@[^ ]+) "," <key>$1</key> ")+"</line>";
				}
				break;
			case NumericIRCMsg.RPL_WHOISCHANNELS:
				if(m.getParams().length>=3)
				{
					StringBuffer sb=new StringBuffer("<line>In channels:");
					StatusPrefix[] prefixes=cw.s.getPrefix();
					for(int param=2;param<m.getParams().length;param++)
					{
						String chan=m.getParamISO(param);
						if(chan.length()==0) continue;
						char c=chan.charAt(0);
						boolean found=false;
						for(int prefix=0;prefix<prefixes.length;prefix++)
						{
							if(prefixes[prefix].getPrefix()==c)
								found=true;
						}
						if(found)
						{
							sb.append(" "+esc(c+"")+"<chan>"+esc(chan.substring(1))+"</chan>");
						}
						else
						{
							sb.append(" <chan>"+esc(chan)+"</chan>");
						}
					}
					sb.append("</line>");
					return sb.toString();
				}
				break;
			default:
				// Nothing
				break;
			}
			return super.getText(cw,m);
		}
	}

	private static class BoxedMessageWho extends BoxedMessage
	{
		BoxedMessageWho()
		{
			super("who",NumericIRCMsg.RPL_WHOREPLY,NumericIRCMsg.RPL_ENDOFWHO);
		}
		@Override
		boolean hasHeading()
		{
			return false;
		}
		@Override
		String getText(ServerChatWindow cw,NumericIRCMsg m)
		{
			switch(m.getNumeric())
			{
			case NumericIRCMsg.RPL_WHOREPLY:
				// <channel> <user> <host> <server> <nick> <H|G>[*][@|+] :<hopcount> <real name>

				if(m.getParams().length>=7)
				{
					IRCUserAddress ua=new IRCUserAddress(m.getParamISO(5),m.getParamISO(2),m.getParamISO(3));
					String last=m.convertEncoding(m.getParams()[7]);
					String hopCount="?";
					if(last.matches("[0-9]+ .*"))
					{
						hopCount=last.substring(0,last.indexOf(' '));
						last=last.substring(last.indexOf(' ')+1);
					}
					String flags=m.getParamISO(6);
					String away=flags.startsWith("H") ? "Here" : "Away";
					flags=flags.substring(1);

					String chanPart=esc(m.getParamISO(1));
					if(chanPart.equals("*"))
						chanPart="";
					else
						chanPart="<chan>"+chanPart+"</chan> ";

					return "<line>"+chanPart+"<nick>"+esc(ua.getNick())+"</nick> ("+esc(ua.getUser())+"@"+
						esc(ua.getHost())+") <key>"+away+"</key>, mode <key>"+XML.esc(flags)+"</key>, <key>"+hopCount+ "</key> hops - "+
						esc(last)+"</line>";
				}
				break;
			default:
				// Nothing
				break;
			}
			return super.getText(cw,m);
		}
	}

	private static class BoxedMessageLusers extends BoxedMessage
	{
		BoxedMessageLusers()
		{
			super("lusers",new int[]
     	{
				NumericIRCMsg.RPL_LUSERCHANNELS,
				NumericIRCMsg.RPL_LUSERCLIENT,
				NumericIRCMsg.RPL_LUSERME,
				NumericIRCMsg.RPL_LUSEROP,
				NumericIRCMsg.RPL_LUSERUNKNOWN	,
				NumericIRCMsg.RPL_GLOBALUSERS,
				NumericIRCMsg.RPL_LOCALUSERS,
				NumericIRCMsg.RPL_STATSCONN
			});
		}

		@Override
		boolean hasHeading()
		{
			return false;
		}

		@Override
		String getText(ServerChatWindow cw,NumericIRCMsg nim)
		{
			StringBuffer params=new StringBuffer();
			int startParam=1;
			// These two messages duplicate the numbers in the text, so
			// for those, skip the numeric parameters
			if(nim.getNumeric()==NumericIRCMsg.RPL_LOCALUSERS ||
				nim.getNumeric()==NumericIRCMsg.RPL_GLOBALUSERS)
				startParam=nim.getParams().length-1;
			for(int param=startParam;param<nim.getParams().length;param++)
			{
				if(param!=startParam) params.append(' ');
				params.append(nim.getParamISO(param));
			}
			return "<line>"+esc(params.toString()).replaceAll("([0-9]+)","<key>$1</key>")+"</line>";
		}
	}

	private static class BoxedMessageLinks extends BoxedMessage
	{
		private static Pattern REGEX_HOPCOUNT = Pattern.compile("([0-9]{1,9})(?: (.*))?");
		BoxedMessageLinks()
		{
			super("links", NumericIRCMsg.RPL_LINKS, NumericIRCMsg.RPL_ENDOFLINKS);
		}

		@Override
		boolean hasHeading()
		{
			return false;
		}

		@Override
		String getText(ServerChatWindow cw, NumericIRCMsg msg)
		{
			int count = msg.getParams().length;
			if( count >= 4)
			{
				String server = msg.getParamISO(1);
				String countAndInfo = msg.convertEncoding(msg.getParams()[count-1]);
				Matcher m = REGEX_HOPCOUNT.matcher(countAndInfo);
				if(m.matches())
				{
					int hops = Integer.parseInt(m.group(1));
					String info = m.group(2);
					if(info == null)
					{
						info = "";
					}

					// Indent servers past 0 hops
					StringBuffer out = new StringBuffer("<line>");
					for(int i=0; i<hops; i++)
					{
						out.append("&#160;&#160;");
						if(i == hops-1)
						{
							out.append("&#x21b3;");
						}
					}
					out.append("<server>" + esc(server) + "</server>");
					if(info.length() > 0)
					{
						out.append(" " + info);
					}
					out.append("</line>");
					return out.toString();
				}
			}
			return "";
		}
	}

	private static final BoxedMessage[] BOXEDMESSAGES=
	{
		new BoxedMessage("motd",NumericIRCMsg.RPL_MOTDSTART,NumericIRCMsg.RPL_ENDOFMOTD),
		new BoxedMessageWhois(),
		new BoxedMessageLusers(),
		new BoxedMessageWho(),
		new BoxedMessageLinks()
	};

	/**
	 * Watch message. Displays info.
	 * @param msg Message
	 */
	public void handleWatchMsg(WatchMsg msg)
	{
		msg.markHandled();

		boolean on=(msg instanceof OnWatchMsg);
		String description = on
		  ? (description=msg.isRealChange() ? "logged <key>on</key>" : "is <key>on</key>line")
		  	: (description=msg.isRealChange() ? "logged <key>off</key>" : null);
		if(description==null)
			return; // Don't display those or it would be tedious on login

		IRCUserAddress ua=msg.getUserAddress();
		String host = ua.getHost().length()==0 ? ""
			:	"("+esc(ua.getUser())+"@"+esc(ua.getHost())+") ";

		addLine(EVENTSYMBOL+"<nick>"+esc(msg.getNick())+"</nick> "+host+description);
	}

	/**
	 * Matches part of the reply to USERHOST messages, except that you have to
	 * add a space to the end before applying this (makes it simpler to handle
	 * different numbers of replies). Groups: 1=nick, 2=ircop *, 3=+/- away,
	 * 4=username, 5=host
	 */
	private final static Pattern RPL_USERHOST_REGEX = Pattern.compile(
		"([^ *]+)(\\*)?=([+-])(?:([^@ ]+)@)?(\\S*)\\s+");

	/**
	 * Unhandled message. Provides standard display for many message types.
	 * @param msg Message
	 * @throws GeneralException
	 */
	public void handleUnhandledMsg(IRCMsg msg) throws GeneralException
	{
		if(msg instanceof NumericIRCMsg)
		{
			NumericIRCMsg numeric=(NumericIRCMsg)msg;
			if(currentBox==null)
			{
				for(int i=0;i<BOXEDMESSAGES.length;i++)
				{
					if(BOXEDMESSAGES[i].matchesStart(numeric))
					{
						currentBox=new CurrentBox(numeric,BOXEDMESSAGES[i]);
						msg.markHandled();
						return;
					}
				}
			}
			else
			{
				if(currentBox.check(numeric))
				{
					msg.markHandled();
					return;
				}
				else // Try again
				{
					handleUnhandledMsg(msg);
					return;
				}
			}

			boolean done=false;
			switch(numeric.getNumeric())
			{
			case NumericIRCMsg.RPL_WELCOME:
				if(numeric.getParams().length>1)
				{
					addLine("<line>"+esc(numeric.getParamISO(1)).replaceAll(
						"([^ !]+)!([^ @]+)@([^ ]+)","<key>$1</key>!<key>$2</key>@<key>$3</key>")+"</line>");
					done=true;
				}
				break;
			case NumericIRCMsg.RPL_YOURHOST:
				if(numeric.getParams().length>1)
				{
					addLine("<line>"+esc(numeric.getParamISO(1)).replaceAll(
						"host is ([^ ,]+)","host is <key>$1</key>").replaceAll(
							"version ([^ ]+)$","version <key>$1</key>")+"</line>");
					done=true;
				}
				break;
			case NumericIRCMsg.RPL_CREATED:
				if(numeric.getParams().length>1)
				{
					addLine("<line>"+esc(numeric.getParamISO(1)).replaceAll(
						"was created (.+) at (.+)$","was created <key>$1</key> at <key>$2</key>")+"</line>");
					done=true;
				}
				break;
			case NumericIRCMsg.RPL_MYINFO: // Don't display, it's not very interesting
				done=true;
				break;
			// Just display these
			case NumericIRCMsg.RPL_NOWAWAY :
			case NumericIRCMsg.RPL_UNAWAY :
				addLine("<line>"+EVENTSYMBOL+esc(numeric.getParamISO(1))+"</line>");
				done=true;
				break;
			case NumericIRCMsg.RPL_VERSION :
				if(numeric.getParams().length > 2)
				{
					String version = numeric.getParamISO(1);
					if(version.endsWith("."))
					{
						version = version.substring(0, version.length() - 1);
					}
					String extraVersion = "";
					for(int i=3; i<numeric.getParams().length; i++)
					{
						if(extraVersion.length() != 0)
						{
							extraVersion += " ";
						}
						extraVersion += numeric.getParamISO(i);
					}
					addLine("<line><server>" + esc(numeric.getParamISO(2))
						+ "</server> running server version <key>"
						+ version + "</key> " + extraVersion + "</line>");
					done = true;
				}
				break;
			case NumericIRCMsg.RPL_TIME :
				if(numeric.getParams().length > 1)
				{
					String server = esc(numeric.getParamISO(1));
					String time = esc(numeric.getParamISO(2));
					addLine("<line><server>" + server
						+ "</server> " + time + "</line>");
					done = true;
				}
				break;
			case NumericIRCMsg.RPL_USERHOST:
				if(numeric.getParams().length >= 2)
				{
					Matcher m = RPL_USERHOST_REGEX.matcher(numeric.getParamISO(1) + " ");
					StringBuffer out = new StringBuffer("<line>" + EVENTSYMBOL + " ");
					boolean first = true;
					while(m.find())
					{
						if(first)
						{
							first = false;
						}
						else
						{
							out.append("; ");
						}
						out.append("<nick>" + esc(m.group(1)) + "</nick>");
						if("*".equals(m.group(2)))
						{
							out.append(" (<key>IRC operator</key>)");
						}
						out.append(" <key>" + esc(m.group(4)) + "</key>@<key>"
							+ esc(m.group(5)) + "</key>");
						if(m.group(3).equals("-"))
						{
							out.append(" (away)");
						}
						else
						{
							out.append(" (present)");
						}
					}
					if(first)
					{
						out.append("No result from /userhost");
					}
					out.append("</line>");
					addLine(out.toString());
					done = true;
				}
				break;
			}
			if(!done)
			{
				addLine("<unhandled>"+esc(getNumericText(numeric))+"</unhandled>");
			}
		}
		else if(msg instanceof UserNoticeIRCMsg)
		{
			UserNoticeIRCMsg unim=(UserNoticeIRCMsg)msg;
			IRCUserAddress ua=unim.getSourceUser();
			String sXML="-<nick>"+esc(ua.getNick())+"</nick>- "+esc(unim.convertEncoding(unim.getText()));
			addLine(sXML);
			getPluginContext().getSingleton2(Logger.class).log(
				getServer().getReportedOrConnectedHost(),Logger.CATEGORY_USER,ua.getNick(),"notice",sXML);
		}
		else if(msg instanceof ServerNoticeIRCMsg)
		{
			ServerNoticeIRCMsg snim=(ServerNoticeIRCMsg)msg;
			addLine("-<server>"+esc(snim.getSourceServer())+"</server>- "+esc(IRCMsg.convertISO(snim.getText())));
		}
		else if(msg instanceof UserModeIRCMsg)
		{
			UserModeIRCMsg umim=(UserModeIRCMsg)msg;
			addLine(EVENTSYMBOL+"<nick>"+esc(umim.getTargetNick())+"</nick> user mode set to: <key>"+esc(umim.getModes())+"</key>");
		}
		else if(msg instanceof NickIRCMsg)
		{
			NickIRCMsg nim=(NickIRCMsg)msg;
			addLine(EVENTSYMBOL+"<nick>"+esc(nim.getSourceUser().getNick())+
				"</nick> <nickchange>changes nick to</nickchange> <nick>"+esc(nim.getNewNick())+
				"</nick>");
		}
		else if(msg instanceof InviteIRCMsg)
		{
			InviteIRCMsg iim=(InviteIRCMsg)msg;
			addLine(EVENTSYMBOL+"<nick>"+esc(iim.getSourceUser().getNick())+
				"</nick> ("+esc(iim.getSourceUser().getUser())+"@"+esc(iim.getSourceUser().getHost())+") " +
				"invites you to join <chan>"+esc(iim.getChannel())+"</chan> (<internalaction>"+INTERNALACTION_JOIN+"</internalaction>)");
		}
		else if(msg instanceof UserCTCPRequestIRCMsg)
		{
			UserCTCPRequestIRCMsg request=(UserCTCPRequestIRCMsg)msg;
			String extra="";
			if(request.getText().length>0)
			{
				String text=request.convertEncoding(request.getText());
				if(request.getRequest().equals("PING") && text.matches("[0-9]{1,10}"))
				{
			 	  SimpleDateFormat sdf=new SimpleDateFormat("EEE, d MMMMM yyyy HH:mm:ss");
					text=sdf.format(new Date(Long.parseLong(text)*1000));
				}
			  extra=": "+esc(text);
			}
			addLine(EVENTSYMBOL+"<nick>"+esc(request.getSourceUser().getNick())+
				"</nick> sent CTCP <key>"+request.getRequest()+"</key>"+extra);
		}
		else if(msg instanceof UserCTCPResponseIRCMsg)
		{
			UserCTCPResponseIRCMsg response=(UserCTCPResponseIRCMsg)msg;
			String extra="";
			if(response.getText().length>0)
			{
				String text=response.convertEncoding(response.getText());
				if(response.getRequest().equals("PING") && text.matches("[0-9]{1,10}"))
				{
					long seconds=System.currentTimeMillis()/1000L - Long.parseLong(text);
					text=seconds+(seconds==1 ? " second" : " seconds");
				}
			  extra=": "+esc(text);
			}
			addLine(EVENTSYMBOL+"<nick>"+esc(response.getSourceUser().getNick())+
				"</nick> responded to CTCP <key>"+response.getRequest()+"</key>"+extra);
		}
		else if(msg instanceof ErrorIRCMsg)
		{
			byte[][] params = msg.getParams();
			addLine(EVENTSYMBOL + "<server>" + esc(msg.getServer().getReportedHost())
				+ "</server> " + esc(msg.convertEncoding(params[params.length - 1])));
		}
		else
		{
			addLine("<unhandled>"+esc(msg.convertEncoding(msg.getLine()))+"</unhandled>");
		}
		msg.markHandled();
	}

	protected String ifMessage(IRCMsg m,byte[] abText)
	{
		if(abText==null) return "";
		return ": "+m.convertEncoding(abText);
	}

	/**
	 * IRC message: quit. Displays quit.
	 * @param msg Message
	 * @throws GeneralException
	 */
	public void msg(QuitIRCMsg msg) throws GeneralException
	{
		// Don't check if it's been handled because we display in multiple places.

		IRCUserAddress ua=msg.getSourceUser();
		addLine(EVENTSYMBOL+"<nick>"+esc(ua.getNick())+"</nick> <quit>has quit IRC</quit>"+
			esc(ifMessage(msg,msg.getMessage())),"quit");

		msg.markHandled();
	}

	/**
	 * IRC message: nick. Displays nick.
	 * @param msg Message
	 * @throws GeneralException
	 */
	public void msg(NickIRCMsg msg) throws GeneralException
	{
		// DOn't check if it's been handled because we display in multiple places.

		addLine(EVENTSYMBOL+"<nick>"+esc(msg.getSourceUser().getNick())+
			"</nick> <nickchange>changes nick to</nickchange> <nick>"+esc(msg.getNewNick())+
			"</nick>","nick");

		msg.markHandled();
	}

	@Override
	public void addItems(PopupMenu pm,Node n)
	{
		if(!connected) return;

		String nick=null,chan=null;
		if(n!=null)
		{
			while(n!=null && !(n instanceof Element))
			{
				n=n.getParentNode();
			}
			Element e=(Element)n;
			if(e.getTagName().equals("nick"))
			{
				nick=XML.getText(e);
			}
			else if(e.getTagName().equals("chan"))
			{
				chan=XML.getText(e);
			}
		}

		IRCActionListMsg menuContext=new IRCActionListMsg(s,getContextChannel(),getContextNick(),
			chan,nick!=null ? new String[] {nick} : null);
		((IRCUIPlugin)getPluginContext().getPlugin()).getActionListOwner().fillMenu(menuContext,pm);
	}

	@Override
	protected String getOwnNick()
	{
		String ourNick = s==null ? null : s.getOurNick();
		return ourNick == null ? "[Not connected]" : ourNick;
	}

	private static String getNumericText(NumericIRCMsg nim)
	{
		StringBuffer display=new StringBuffer();
		for(int i=1;i<nim.getParams().length;i++)
		{
			if(i>1) display.append(" ");
			display.append(nim.getParamISO(i));
		}
		return display.toString();
	}

	@Override
	protected String getLogSource()
	{
		return 	getServer().getReportedOrConnectedHost();
	}

	/**
	 * Callback: User clicks Away button.
	 * @throws GeneralException
	 */
	@UIAction
	public void actionAway() throws GeneralException
	{
		Commands c=getPluginContext().getSingleton2(Commands.class);
		doCommand(c,"/away");
	}
}
