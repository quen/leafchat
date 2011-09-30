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

import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;

import org.w3c.dom.Element;

import util.*;

import com.leafdigital.idle.api.Idle;
import com.leafdigital.irc.api.*;
import com.leafdigital.ircui.api.*;
import com.leafdigital.logs.api.Logger;
import com.leafdigital.ui.api.*;
import com.leafdigital.ui.api.TextView.ActionHandler;

import leafchat.core.api.*;

/** Channel window */
@UIHandler("chanwindow")
public class ChanWindow extends ServerChatWindow
{
	private String chan;
	private Map<String, NickInfo> nickInfo = new HashMap<String, NickInfo>();

	private ListBox nameList;
	private ModeDisplay modes;

	/** Split panel bar */
  public SplitPanel splitUI;

  /** Topic label */
  public Label topicUI;

	private char ownStatus=0;

	// A-Za-z0-9- are official DNS characters. * is used on some networks to hide server details
	private final static Pattern SPLIT=Pattern.compile("([A-Za-z0-9*-]+(?:\\.[A-Za-z0-9*-]+){2,}) ([A-Za-z0-9*-]+(?:\\.[A-Za-z0-9*-.]+){2,})");

	private int timerBold=-1;
	private final static int BOLDTIMER_PERIOD=30*1000; // Make names non-bold every 30 seconds
	private final static int BOLD_LENGTH=15*60*1000; // Once bold, names stay bold for 15 minutes

	private int timerWho=-1;
	private final static int WHOTIMER_STANDARD=180*1000; // Does /who every 3 minutes usually
	private final static int WHOTIMER_INCREMENT=60*1000; // But waits an extra 1 minute
	private final static int WHOTIMER_NAMES=20; // For every 20 names

	// If an error occurs during creation, this can cause problems because the
	// window-close callback still gets called.
	private boolean fullyCreated=false;

	private void setOwnStatus(char prefix)
	{
		ownStatus=prefix;
		modes.updateOpStatus(hasAtLeastPrefix('@'));
		updateTopicBar();
	}

	/**
	 * @param prefix A channel mode prefix such as @
	 * @return True if the current user has at least the specified channel mode prefix
	 */
  public boolean hasAtLeastPrefix(char prefix)
  {
  	  return getServer().isPrefixAtLeast(ownStatus,prefix);
  }

	/**
	 * @param pc Plugin context for messages etc
	 * @param jim Join message that we're responding to
	 * @throws GeneralException
	 */
	public ChanWindow(PluginContext pc,JoinIRCMsg jim) throws GeneralException
	{
		super(pc, jim.getServer(), "chanwindow", false, true);
		this.chan=jim.getChannel();

		setTitle();
		nameList=(ListBox)getWindow().getWidget("names");

		Server s=getServer();

		modes=new ModeDisplay(pc,s,getWindow(),chan);
		VerticalPanel vp=(VerticalPanel)getWindow().getWidget("controls");
		if(!PlatformUtils.isMac()) vp.setBorder(4);
		UI ui=pc.getSingle(UI.class);
		vp.add(ui.newJComponentWrapper(modes));

		updateTopicBar();
		topicUI.setAction("url",new ActionHandler()
		{
			@Override
			public void action(Element e, MouseEvent me) throws GeneralException
			{
				new TopicChangeDialog();
			}
		});

		// Request messages
		initServerInner(s,true);

		// And for actions too
		pc.requestMessages(IRCActionListMsg.class,this);

		// Run the join message so it gets displayed
		msg(jim);

		// Send the mode request
		s.sendLine(IRCMsg.constructBytes("MODE "+chan));

		// Show the window
		getWindow().setRemember("channel",chan);
		getCommandEdit().setRemember("channel", chan);
		String extraRemember=getWindow().getExtraRemember();
		if(extraRemember!=null && extraRemember.matches("[0-9]+"))
		{
			int divider=Integer.parseInt(extraRemember);
			splitUI.setSplitSize(divider);
		}
		((IRCUIPlugin)getPluginContext().getPlugin()).informShown(this);
		getWindow().show(false);
		fullyCreated=true;

		// Set up bold timer
		triggerBoldTimer();
	}

	private void triggerBoldTimer()
	{
		timerBold=TimeUtils.addTimedEvent(new Runnable()
		{
			@Override
			public void run()
			{
				boldTimer();
			}
		}, BOLDTIMER_PERIOD,true);
	}

	private void boldTimer()
	{
		long now=System.currentTimeMillis();
		for(Iterator<NickInfo> i=nickInfo.values().iterator();i.hasNext();)
		{
			NickInfo ni=i.next();
			if(now-ni.lastMessage > BOLD_LENGTH)
			{
				nameList.setBold(ni.getNameInList(),false);
			}
		}
		triggerBoldTimer();
	}

	private void triggerWhoTimer()
	{
		timerWho=TimeUtils.addTimedEvent(new Runnable()
		{
			@Override
			public void run()
			{
				whoTimer();
			}
		}, WHOTIMER_STANDARD + (nameList.getItems().length / WHOTIMER_NAMES)
			*WHOTIMER_INCREMENT, true);
	}

	/**
	 * Track which message our WHO is; -1 indicates we're not waiting for one,
	 * 0 = it's the next one, etc.
	 */
	private int whoID;

	private void whoTimer()
	{
		if(getServer().isConnected())
		{
			whoID=getServer().sendServerRequest(IRCMsg.constructBytes("WHO "+chan));
			triggerWhoTimer();
		}
	}

	private void whoReply(NumericIRCMsg m)
	{
		if(m.getParams().length>6)
		{
			// Remember user/host
			IRCUserAddress ua=new IRCUserAddress(m.getParamISO(5),m.getParamISO(2),m.getParamISO(3));
			updateRecords(ua);

			// Update away status
			boolean away=m.getParamISO(6).startsWith("G");
			NickInfo ni=nickInfo.get(ua.getNick());
			if(ni!=null) nameList.setFaint(ni.getNameInList(),away);
		}
	}

	private int requestIDChan,requestIDQuit,requestIDNumeric,requestIDNick;

	@Override
	protected void initServerInner(Server s, boolean firstTime)
	{
		PluginContext pc=getPluginContext();

		if(!firstTime)
		{
			pc.unrequestMessages(ChanIRCMsg.class,this,requestIDChan);
			pc.unrequestMessages(QuitIRCMsg.class,this,requestIDQuit);
			pc.unrequestMessages(NumericIRCMsg.class,this,requestIDNumeric);
			pc.unrequestMessages(NickIRCMsg.class,this,requestIDNick);

			if(s!=null)
			{
				modes.changeServer(s);
				String keyPart = "";
				if(modes.hasMode('k'))
				{
					keyPart = " " + modes.getModeValue('k');
				}
				s.sendLine(IRCMsg.constructBytes("JOIN " + chan + keyPart));
			}
		}
		if(s==null)
		{
			// Clear names list
			nameList.clear();
			nickInfo.clear();

			// Forget who ID and stop requesting more
			if(timerWho!=-1)
			{
				TimeUtils.cancelTimedEvent(timerWho);
				timerWho=-1;
			}
			whoID=0;
		}

		requestIDChan=pc.requestMessages(ChanIRCMsg.class,this,new ChanAndServerFilter(s,chan),Msg.PRIORITY_NORMAL);
		requestIDQuit=pc.requestMessages(QuitIRCMsg.class,this,new ChanSourceFilter(),Msg.PRIORITY_NORMAL);
		requestIDNumeric=pc.requestMessages(NumericIRCMsg.class,this,new ServerFilter(s),Msg.PRIORITY_NORMAL);
		requestIDNick=pc.requestMessages(NickIRCMsg.class,this,new ChanSourceFilter(),Msg.PRIORITY_NORMAL);
	}

	@Override
	protected void setTitle()
	{
		getWindow().setTitle(chan+" ("+getServer().getCurrentShortName()+")");
	}

	class ChanSourceFilter extends ServerFilter
	{
		ChanSourceFilter()
		{
			super(getServer());
		}

		@Override
		public boolean accept(Msg m)
		{
			return super.accept(m) &&
				nickInfo.containsKey(((UserSourceIRCMsg)m).getSourceUser().getNick());
		}
	}

	@Override
	protected int getAvailableBytes() throws GeneralException
	{
		if(chan==null) return 400;
		// RFC limit is 512 per line including CRLF. The system will send to other users:
		// :nick!user@host PRIVMSG <channel> :<message><CRLF>
		// So there are 499 characters available for the prefix + channel + message
		// Channels etc. are always converted using ISO one byte per character.
		return 499
		  -chan.length()
		  -getServer().getApproxPrefixLength();
	}

	@Override
	protected void doCommand(Commands c,String line) throws GeneralException
	{
		getPluginContext().getSingle(Idle.class).userAwake(
			line.equals("/away") ? Idle.AWAKE_UNAWAY : Idle.AWAKE_COMMAND);
		c.doCommand(line,getServer(),null,chan,this,true);
	}

	@Override
	protected String getLogCategory()
	{
		return Logger.CATEGORY_CHAN;
	}

	@Override
	protected String getLogItem()
	{
		return chan;
	}

	private boolean kicked=false;

	@UIAction
	@Override
	public void windowClosed() throws GeneralException
	{
		if(fullyCreated)
		{
			getWindow().setExtraRemember(splitUI.getSplitSize()+"");
		}
		if(isConnected() && !kicked)
		{
			getServer().sendLine(IRCMsg.constructBytes("PART "+chan));
			((IRCUIPlugin)getPluginContext().getPlugin()).getKnownUsers().wePart(getServer(),chan);
		}
		if(timerBold!=-1) TimeUtils.cancelTimedEvent(timerBold);
		if(timerWho!=-1) TimeUtils.cancelTimedEvent(timerWho);
		super.windowClosed();
	}

	/**
	 * Server message: join.
	 * @param jim Message
	 * @throws GeneralException
	 */
	public void msg(JoinIRCMsg jim) throws GeneralException
	{
		listAddName(jim.getSourceUser().getNick(),false,jim.getSourceUser());
		IRCUserAddress ua=jim.getSourceUser();
		String extra=null;
		if(ua.getNick().equalsIgnoreCase(getServer().getOurNick()))
		{
			if(kicked)
			{
				kicked = false;
				commandUI.setEnabled(true);
			}
		}
		else
		{
			extra=((IRCUIPlugin)getPluginContext().getPlugin()).getKnownUsers().chanJoin(jim.getServer(),chan,ua);
		}
		if(jim.isHandled()) return;

		if(splitManager==null || !splitManager.rejoin(ua.getNick()))
		{
			addLine(EVENTSYMBOL+"<join>Joined:</join> <nick>"+esc(ua.getNick())+"</nick> ("+esc(ua.getUser())+"@"+esc(ua.getHost())+")","join");
			if(extra!=null) addLine(extra);
		}

		jim.markHandled();
	}

	/**
	 * Server message: kick.
	 * @param m Message
	 * @throws GeneralException
	 */
	public void msg(KickIRCMsg m) throws GeneralException
	{
		listRemoveName(m.getVictim());

		// Handle kick if it was us who got kicked
		if(m.getVictim().equalsIgnoreCase(getServer().getOurNick()))
		{
			((IRCUIPlugin)getPluginContext().getPlugin()).getKnownUsers().wePart(getServer(),chan);
			commandUI.setEnabled(false);
			kicked=true;
		}
		else
		{
			((IRCUIPlugin)getPluginContext().getPlugin()).getKnownUsers().chanKick(getServer(),chan,m.getVictim());
		}

		if(m.isHandled()) return;

		addLine(EVENTSYMBOL+"<kick>Kicked:</kick> <nick>"+esc(m.getVictim())+"</nick> (by <nick>"+esc(m.getSourceUser().getNick())+
			"</nick>"+esc(ifMessage(m,m.getText()))+")","kick");
		m.markHandled();
	}

	/**
	 * Server message: part.
	 * @param m Message
	 * @throws GeneralException
	 */
	public void msg(PartIRCMsg m) throws GeneralException
	{
		IRCUserAddress ua=m.getSourceUser();
		listRemoveName(ua.getNick());

		((IRCUIPlugin)getPluginContext().getPlugin()).getKnownUsers().chanPart(getServer(),chan,ua);

		if(m.isHandled()) return;

		addLine(EVENTSYMBOL+"<part>Left:</part> <nick>"+esc(ua.getNick())+"</nick> ("+esc(ua.getUser())+"@"+esc(ua.getHost())+")"+
			esc(ifMessage(m,m.getText())),"part");
		m.markHandled();
	}

	/**
	 * Server message: normal message.
	 * @param m Message
	 * @throws GeneralException
	 */
	public void msg(ChanMessageIRCMsg m) throws GeneralException
	{
		updateRecords(m.getSourceUser());
		((IRCUIPlugin)getPluginContext().getPlugin()).getKnownUsers().chanText(getServer(),chan,m.getSourceUser());
		listUpdateRecent(m.getSourceUser().getNick());
		if(m.isHandled()) return;

		IRCUserAddress ua=m.getSourceUser();
		if(splitManager!=null) splitManager.forceRejoinDisplay(ua.getNick());
		addLine("&lt;<nick>"+esc(ua.getNick())+"</nick>&gt; "+esc(m.convertEncoding(m.getText())),"msg");
		reportActualMessage(chan,"<"+ua.getNick()+"> "+m.convertEncoding(m.getText()));
		m.markHandled();
	}

	/**
	 * Server message: action.
	 * @param m Message
	 * @throws GeneralException
	 */
	public void msg(ChanActionIRCMsg m) throws GeneralException
	{
		updateRecords(m.getSourceUser());
		((IRCUIPlugin)getPluginContext().getPlugin()).getKnownUsers().chanText(getServer(),chan,m.getSourceUser());
		listUpdateRecent(m.getSourceUser().getNick());
		if(m.isHandled()) return;
		IRCUserAddress ua=m.getSourceUser();
		if(splitManager!=null) splitManager.forceRejoinDisplay(ua.getNick());
		addLine(ACTIONSYMBOL+"<nick>"+esc(ua.getNick())+"</nick> "+esc(m.convertEncoding(m.getText())),"action");
		reportActualMessage(chan,ACTIONSYMBOL+ua.getNick()+" "+m.convertEncoding(m.getText()));
		m.markHandled();
	}

	/**
	 * Server message: CTCP request.
	 * @param m Message
	 * @throws GeneralException
	 */
	public void msg(ChanCTCPRequestIRCMsg m) throws GeneralException
	{
		updateRecords(m.getSourceUser());
		if(m.isHandled()) return;
		IRCUserAddress ua=m.getSourceUser();
		addLine("[<nick>"+esc(ua.getNick())+"</nick> CTCP <ctcp>"+esc(m.getRequest())+"</ctcp>] "+esc(m.convertEncoding(m.getText())),"ctcp");
		m.markHandled();
	}

	/**
	 * Server message: channel mode.
	 * @param m Message
	 * @throws GeneralException
	 */
	public void msg(ChanModeIRCMsg m) throws GeneralException
	{
		updateRecords(m.getSourceUser());

		if(m.getSourceUser()==null)
		{
			// Actually numeric RPL_CHANNELMODEIS, so clear existing modes
			modes.clearModes();
		}

		// Handle internal mode storage
		ChanModeIRCMsg.ModeChange[] changes=m.getChanges();
		Server.StatusPrefix[] statusPrefixes=getServer().getPrefix();
		for(int change=0;change<changes.length;change++)
		{
			char mode=changes[change].getMode();
			int type=getServer().getChanModeType(mode);

			switch(type)
			{
			case Server.CHANMODE_USERSTATUS:
				// Handle status changes
				for(int prefix=0;prefix<statusPrefixes.length;prefix++)
				{
					if(mode==statusPrefixes[prefix].getMode())
					{
						changeMode(changes[change].getParam(),statusPrefixes[prefix].getPrefix(),changes[change].isPositive());
						break;
					}
				}
				break;

			case Server.CHANMODE_ALWAYSPARAM:
			case Server.CHANMODE_SETPARAM:
				if(changes[change].isPositive())
					modes.addMode(mode,changes[change].getParam());
				else
					modes.removeMode(mode);
				break;

			case Server.CHANMODE_NOPARAM:
				if(changes[change].isPositive())
					modes.addMode(mode,null);
				else
					modes.removeMode(mode);
				break;

			default:
				break;
			}
		}

		updateTopicBar(); // In case +t changed
		if(m.isHandled()) return;

		if(m.getSourceUser()!=null)
		{
			IRCUserAddress ua=m.getSourceUser();
			StringBuffer sbModes=new StringBuffer();
			sbModes.append(m.getModes());
			for(int i=0;i<m.getModeParams().length;i++)
				sbModes.append(" "+m.getModeParams()[i]);
			addLine(EVENTSYMBOL+"<nick>"+esc(ua.getNick())+"</nick> <mode>changed mode:</mode> "+esc(sbModes.toString()),"mode");
		}
		m.markHandled();
	}

	/**
	 * Server message: channel notice.
	 * @param m Message
	 * @throws GeneralException
	 */
	public void msg(ChanNoticeIRCMsg m) throws GeneralException
	{
		updateRecords(m.getSourceUser());
		((IRCUIPlugin)getPluginContext().getPlugin()).getKnownUsers().chanText(getServer(),chan,m.getSourceUser());
		listUpdateRecent(m.getSourceUser().getNick());
		if(m.isHandled()) return;
		IRCUserAddress ua=m.getSourceUser();
		addLine("["+(m.getStatus()==0?"":""+esc(""+m.getStatus()))+
			esc(m.getChannel())+"] -<nick>"+esc(ua.getNick())+"</nick>- "+esc(m.convertEncoding(m.getText())),"notice");
		reportActualMessage(chan,
			"["+(m.getStatus()==0?"":""+m.getStatus())+
			m.getChannel()+"] -"+ua.getNick()+"- "+m.convertEncoding(m.getText()));
		m.markHandled();
	}

	/** Split manager. When this is non-null, certain information needs to be fed to it */
	private SplitManager splitManager=null;

	/** Information about a particular split between two servers */
	private static class SplitInfo
	{
		long splitAt;
		Set<String> splitNotDisplayed = new TreeSet<String>();
		Set<String> splitDisplayed = new HashSet<String>();
		Set<String> rejoinNotDisplayed = new TreeSet<String>();
	}

	/** Split manager handles display of split events */
	private class SplitManager implements Runnable
	{
		/** Map of server,server -> SplitInfo */
		HashMap<String, SplitInfo> servers = new HashMap<String, SplitInfo>();

		SplitManager()
		{
			// Update every second
			TimeUtils.addTimedEvent(this,1000,true);
		}

		/**
		 * Called whenever somebody is split.
		 * @param server1 Server
		 * @param server2 Other server
		 * @param nick Nickname
		 */
		void add(String server1,String server2,String nick)
		{
			String key=server1+","+server2;
			SplitInfo info=servers.get(key);
			if(info==null)
			{
				addLine(EVENTSYMBOL+"Network split between <key>"+server1+"</key> and <key>"+server2+"</key>","split");
				info=new SplitInfo();
				info.splitAt=System.currentTimeMillis();
				servers.put(key,info);
			}
			info.splitNotDisplayed.add(nick);
		}

		/**
		 * Called whenever somebody joins if the split manager is active.
		 * Allows for lists of joiners to be grouped rather than displayed as
		 * normal joins.
		 * @param nick Nick in question
		 * @return True if this a rejoin and should not be displayed
		 */
		boolean rejoin(String nick)
		{
			// Is this rejoining person a splitee?
			boolean splitee=false;
			SplitInfo info=null;
			for(Iterator<SplitInfo> i=servers.values().iterator();i.hasNext();)
			{
				info=i.next();
				if(info.splitDisplayed.contains(nick) || info.splitNotDisplayed.contains(nick))
				{
					splitee=true;
					break;
				}
			}
			if(!splitee) return false;

			if(info.splitNotDisplayed.contains(nick))
			{
				// Better do display now so that we show they've split and the rejoin
				// makes sense
				doDisplay(true);
			}
			info.splitDisplayed.remove(nick);
			info.rejoinNotDisplayed.add(nick);

			return true;
		}

		/**
		 * Called whenever somebody says something if the split manager is active.
		 * Basically ensures that the 'rejoin' display with their name appears before
		 * anything they might say.
		 * @param nick Nick in question
		 */
		void forceRejoinDisplay(String nick)
		{
			// Is this rejoining person a splitee?
			SplitInfo info=null;
			for(Iterator<SplitInfo> i=servers.values().iterator();i.hasNext();)
			{
				info=i.next();
				if(info.rejoinNotDisplayed.contains(nick))
				{
					doDisplay(false);
					return;
				}
			}
		}

		void doDisplay(boolean splitOnly)
		{
			for(SplitInfo info : servers.values())
			{
				if(!info.splitNotDisplayed.isEmpty())
				{
					StringBuffer sb=new StringBuffer(EVENTSYMBOL+"<part>Split:</part>");
					for(String nick : info.splitNotDisplayed)
					{
						info.splitDisplayed.add(nick);
						sb.append(" <nick>"+esc(nick)+"</nick>");
					}
					info.splitNotDisplayed.clear();
					addLine(sb.toString(),"split");
				}
				if(!splitOnly && !info.rejoinNotDisplayed.isEmpty())
				{
					StringBuffer sb=new StringBuffer(EVENTSYMBOL+"<join>Rejoined:</join>");
					for(String nick : info.rejoinNotDisplayed)
					{
						sb.append(" <nick>"+esc(nick)+"</nick>");
					}
					info.rejoinNotDisplayed.clear();
					addLine(sb.toString(),"split");
				}
			}
		}

		@Override
		public void run()
		{
			// Got anything new to display?
			doDisplay(false);

			// Check if we should continue checking
			long now=System.currentTimeMillis();
			boolean keepGoing=false;
			for(SplitInfo info : servers.values())
			{
				if(
					// If there are any splitters left and the split happened less than
					// 10 minutes ago, continue
					(info.splitDisplayed.size()>1 && now-info.splitAt<10*60*1000) ||
					// If the split happened less than 30 seconds ago, continue
					now-info.splitAt<30*1000)
				{
					keepGoing=true;
					break;
				}
			}
			if(keepGoing)
				TimeUtils.addTimedEvent(this,1000,true);
			else
				splitManager=null;
		}
	}

	@Override
	public void msg(QuitIRCMsg msg) throws GeneralException
	{
		listRemoveName(msg.getSourceUser().getNick());
		((IRCUIPlugin)getPluginContext().getPlugin()).getKnownUsers().chanQuit(getServer(),chan,msg.getSourceUser());

		// Split detect
		String message=msg.convertEncoding(msg.getMessage());
		Matcher m=SPLIT.matcher(message);
		if(m.matches())
		{
			if(splitManager==null)
			{
				splitManager=new SplitManager();
			}
			splitManager.add(m.group(1),m.group(2),msg.getSourceUser().getNick());
			msg.markHandled();
		}
		else
		{
			// Display quit
			super.msg(msg);
		}
	}

	@Override
	public void msg(NickIRCMsg m) throws GeneralException
	{
		listChangeName(m.getSourceUser().getNick(),m.getNewNick());
		((IRCUIPlugin)getPluginContext().getPlugin()).getKnownUsers().chanNick(getServer(),chan,m.getSourceUser(),m.getNewNick());
		updateRecords(m.getSourceUser());
		super.msg(m);
	}

	/**
	 * Server message: numeric.
	 * @param m Message
	 * @throws GeneralException
	 */
	public void msg(NumericIRCMsg m) throws GeneralException
	{
		switch(m.getNumeric())
		{
			case NumericIRCMsg.RPL_NAMREPLY:
				// :dream.esper.net 353 quentesting1 = #quentesting :@quentesting1
				if(m.getParams().length<4 || !m.getParamISO(2).equalsIgnoreCase(chan))
					return;
				String[] asNames=m.getParamISO(3).split(" ");
				for(int i=0;i<asNames.length;i++)
				{
					String sName=asNames[i];
					listAddName(sName,true,null);
				}
				m.markHandled();
				// Start doing /who
				if(timerWho==-1) whoTimer();
				break;

			case NumericIRCMsg.RPL_ENDOFNAMES:
				// :dream.esper.net 366 quentesting1 #quentesting :End of /NAMES list.
				if(m.getParams().length<2 || !m.getParamISO(1).equalsIgnoreCase(chan))
					return;
				m.markHandled();
				break;

			case NumericIRCMsg.RPL_TOPIC:
				// :discworld.esper.net 332 quentesting2 #quentesting :My happy fun topic
				if(m.getParams().length<3 || !m.getParamISO(1).equalsIgnoreCase(chan))
					return;

				setTopic(m.getParams()[2],null,m);

				m.markHandled();
				break;

			case NumericIRCMsg.RPL_TOPICWHOTIME:
				// :iuturna.sorcery.net 333 quen #quentesting quen 1137247214
				if(m.getParams().length<4 || !m.getParamISO(1).equalsIgnoreCase(chan))
					return;

				int iUnixTime;
				try
				{
					iUnixTime=Integer.parseInt(m.getParamISO(3));
				}
				catch(NumberFormatException nfe)
				{
					// Ignore, we'll display as an unrecognised server message.
					return;
				}

				// The nick is sometimes a full address
				IRCUserAddress ua=new IRCUserAddress(m.getParamISO(2),false);
				addLine(EVENTSYMBOL+"<topic>Set by</topic> <nick>"+esc(ua.getNick())+"</nick> at "+
					formatTime(iUnixTime),"topictime");

				m.markHandled();
				break;
			case NumericIRCMsg.RPL_CHANNELMODEIS:
				// :naylean.draconic.com 324 quentesting1 #quentesting +pntcl 30
				if(m.getParams().length<2 || !m.getParamISO(1).equalsIgnoreCase(chan))
					return;

				ChanModeIRCMsg cmim=(ChanModeIRCMsg)m.getSimilar();
				if(cmim!=null)
				{
					msg(cmim);
					m.markHandled();
				}
				break;
			case NumericIRCMsg.RPL_CREATIONTIME:
				// :naylean.draconic.com 329 quentesting1 #quentesting 1155726202
				if(m.getParams().length<3 || !m.getParamISO(1).equalsIgnoreCase(chan))
					return;
				try
				{
					iUnixTime=Integer.parseInt(m.getParamISO(2));
				}
				catch(NumberFormatException nfe)
				{
					// Ignore, we'll display as an unrecognised server message.
					return;
				}

				addLine(EVENTSYMBOL+"<chaninfo>Channel opened</chaninfo> at "+
					formatTime(iUnixTime),"chanopened");

				m.markHandled();
				break;
			case NumericIRCMsg.RPL_ENDOFWHO:
				if(m.getResponseID()==whoID) m.markHandled();
				break;
			case NumericIRCMsg.RPL_WHOREPLY:
				if(m.getResponseID()==whoID)
				{
					whoReply(m);
					m.markHandled();
				}
				break;
			case NumericIRCMsg.RPL_NOWAWAY:
			{
				NickInfo ni=nickInfo.get(getServer().getOurNick());
				if(ni!=null) nameList.setFaint(ni.getNameInList(),true);
				break;
			}
			case NumericIRCMsg.RPL_UNAWAY:
			{
				NickInfo ni=nickInfo.get(getServer().getOurNick());
				if(ni!=null) nameList.setFaint(ni.getNameInList(),false);
				break;
			}
		}
	}

	/**
	 * Server message: topic.
	 * @param m Message
	 * @throws GeneralException
	 */
	public void msg(TopicIRCMsg m) throws GeneralException
	{
		setTopic(m.getTopic(),m.getSourceUser(),m);
		m.markHandled();
	}

	private static String formatTime(int iUnixTime)
	{
		Calendar cNow=Calendar.getInstance(),cThen=Calendar.getInstance();
		cThen.setTimeInMillis(iUnixTime * 1000L);

		SimpleDateFormat sdf;

		if(cNow.get(Calendar.DAY_OF_YEAR) == cThen.get(Calendar.DAY_OF_YEAR) &&
			cNow.get(Calendar.YEAR) == cThen.get(Calendar.YEAR) )
		{ // Today
			sdf=new SimpleDateFormat("HH:mm 'today'");
		}
		else
		{
			// Check up to a week back (but not hitting this same day name)
			cNow.set(Calendar.HOUR,0);
			cNow.set(Calendar.MINUTE,0);
			cNow.set(Calendar.SECOND,0);
			cNow.set(Calendar.MILLISECOND,0);
			cNow.add(Calendar.DATE,-6);
			if(!cThen.before(cNow))
			{ // This week
				sdf=new SimpleDateFormat("HH:mm 'on' EEEE");
			}
			else
			{ // Use full date format
				sdf=new SimpleDateFormat(
					"HH:mm 'on' EEEE dd MMMM yyyy");
			}
		}
		return sdf.format(cThen.getTime());
	}

	/**
	 * @param sName Name to add to list
	 * @param bMayHavePrefix If name might be prefixed
	 * @param ua Full user address or null if not known
	 * @throws BugException Error in xml
	 */
	private void listAddName(String sName,boolean bMayHavePrefix,IRCUserAddress ua)
	{
		if(sName.length()==0) return;

		// Parse off prefix to build data entry and add to set
		NickInfo ni=new NickInfo();
		ni.sName=sName;
		ni.ua=ua;
		if(bMayHavePrefix)
		{
			char cMaybePrefix=sName.charAt(0);
			Server.StatusPrefix[] asp=getServer().getPrefix();
			for(int iPrefix=0;iPrefix<asp.length;iPrefix++)
			{
				if(asp[iPrefix].getPrefix() == cMaybePrefix)
				{
					ni.cPrefix=cMaybePrefix;
					ni.sName=sName.substring(1);
					break;
				}
			}
		}

		// See if name's already present
		NickInfo niOld=nickInfo.get(ni.sName);
		if(niOld!=null)
			nameList.removeItem(niOld.getNameInList());

		// Put into map (replacing old one if there was one)
		nickInfo.put(ni.sName,ni);
		if(ni.sName.equals(getServer().getOurNick()))
			setOwnStatus(ni.cPrefix);

		// Add to listbox
		nameList.addItem(ni.getNameInList());
	}

	/** Information stored about a person in namelist */
	private static class NickInfo
	{
		String sName;
		IRCUserAddress ua; // May be null if not known yet
		char cPrefix=0;
		long lastMessage=0;
		String getNameInList() { return (cPrefix!=0 ? cPrefix+sName : sName); }
	}

	/**
	 * @param name Name to remove from list
	 * @throws BugException Error in xml
	 */
	private void listRemoveName(String name)
	{
		// Find in map
		NickInfo ni=nickInfo.get(name);
		if(ni==null)
		{
			getPluginContext().log("Warning: attempt to remove absent nick from channel "+chan+": "+name);
			return;
		}

		// Remove from listbox
		nameList.removeItem(ni.getNameInList());

		// Remove from set
		nickInfo.remove(name);
	}

	private void listChangeName(String before,String after)
	{
		// Find in map
		NickInfo ni=nickInfo.get(before);
		if(ni==null) throw new BugException("Nick not found: "+before);

		// Remove from listbox & set
		nameList.removeItem(ni.getNameInList());
		nickInfo.remove(before);

		// Add back to set & listbox
		ni.sName=after;
		nickInfo.put(after,ni);
		nameList.addItem(ni.getNameInList());
		if(System.currentTimeMillis() - ni.lastMessage < BOLD_LENGTH)
			nameList.setBold(ni.getNameInList(),true);
	}

	private void listUpdateRecent(String name)
	{
		// Find in map
		NickInfo ni=nickInfo.get(name);
		if(ni==null) return; // Ignore missing nicks

		// Mark bold in list
		ni.lastMessage=System.currentTimeMillis();
		nameList.setBold(ni.getNameInList(),true);
	}

	private void changeMode(String nick,char prefix,boolean on)
	{
		// Find in map
		NickInfo ni=nickInfo.get(nick);
		if(ni==null)
		{
			getPluginContext().log("Warning: attempt to change mode for missing nick "+chan+": "+nick);
			return;
		}

		// Remove from listbox
		nameList.removeItem(ni.getNameInList());

		// Change mode
		if(on)
			ni.cPrefix=prefix;
		else if(ni.cPrefix==prefix)
			ni.cPrefix=0;

		if(ni.sName.equals(getServer().getOurNick()))
			setOwnStatus(ni.cPrefix);

		// Add back to list
		nameList.addItem(ni.getNameInList());
	}

	private String currentTopic="";

	private void setTopic(byte[] topicBytes,IRCUserAddress setter,IRCMsg encodingReference) throws GeneralException
	{
		String topic=encodingReference.convertEncoding(topicBytes);
		if(setter!=null)
			addLine(EVENTSYMBOL+"<topic>Topic set</topic> by <nick>"+setter.getNick()+"</nick>: "+esc(topic),"topicset");
		else
			addLine(EVENTSYMBOL+"<topic>Topic</topic>: "+esc(topic),"topic");
		currentTopic=topic;
		updateTopicBar();
	}

	private void updateTopicBar()
	{
		String text;
		if(currentTopic.length()==0)
		{
			text="No topic.";
		}
		else
		{
			text="<strong>Topic</strong>: "+processColours(esc(currentTopic));
		}
		topicUI.setText(text+
			((hasAtLeastPrefix('@') || !modes.hasMode('t'))?" (<url>change</url>)" : ""));
	}

	/**
	 * Dialog used to change the topic.
	 */
	@UIHandler("changetopic")
	public class TopicChangeDialog
	{
		private Dialog d;

		/** Edit box for topic. */
		public EditBox topicUI;
		/** Button to change. */
		public Button changeUI;

		TopicChangeDialog() throws GeneralException
		{
			UI ui=getPluginContext().getSingle(UI.class);
			d=ui.createDialog("changetopic", this);

			topicUI.setValue(currentTopic);
			changeValue();

			d.show(getWindow());
		}

		/** Callback: Cancel button. */
		@UIAction
		public void actionCancel()
		{
			d.close();
		}

		/** Callback: Text change in edit box. */
		@UIAction
		public void changeValue()
		{
			// Get encoding
			IRCEncoding encoding=getPluginContext().getSingle(IRCEncoding.class);
			int bytes=encoding.getEncoding(getServer(),chan,null).convertOutgoing(
				topicUI.getValue()).length;
			changeUI.setEnabled(bytes<=getServer().getMaxTopicLength());
		}

		/**
		 * Callback: Change button.
		 * @throws GeneralException
		 */
		@UIAction
		public void actionChange() throws GeneralException
		{
			doCommand(getPluginContext().getSingle(Commands.class),
				"/topic "+chan+" "+topicUI.getValue());
			d.close();
		}
	}

	/**
	 * Server message: general channel message (not used).
	 * @param m Message
	 */
	public void msg(ChanIRCMsg m)
	{
		// Never called, but required because we requested the generic type rather than
		// making 6 individual requests. This is kind of OK technologically because
		// maybe we add another type later.
	}

	@Override
	protected boolean isUs(String sTarget)
	{
		return sTarget.equalsIgnoreCase(chan);
	}

	@Override
	public void fillTabCompletionList(TabCompletionList options)
	{
		options.add(chan,false);
		for(String name : nickInfo.keySet())
		{
			options.add(name,true);
		}
	}

	@Override
	protected boolean displayTimeStamps()
	{
		return true;
	}

	@Override
	protected String getContextChannel()
	{
		return chan;
	}

	/**
	 * Obtains menu actions for the popup menu on channel names list.
	 * @param pm Menu to add actions to
	 */
	@UIAction
	public void menuNames(PopupMenu pm)
	{
		// Get selected nicks
		String[] selectedNicks=nameList.getMultiSelected();
		Server.StatusPrefix[] asp=getServer().getPrefix();
		for(int i=0;i<selectedNicks.length;i++)
		{
			// Get rid of prefix if any match
			for(int iPrefix=0;iPrefix<asp.length;iPrefix++)
			{
				if(asp[iPrefix].getPrefix() == selectedNicks[i].charAt(0))
				{
					selectedNicks[i]=selectedNicks[i].substring(1);
					break; // No point checking other prefixes
				}
			}
		}

		// Add in actions on them from across plugin(s)
		IRCActionListMsg context=new IRCActionListMsg(getServer(),
			chan,null,
			null,selectedNicks);
		((IRCUIPlugin)getPluginContext().getPlugin()).getActionListOwner().fillMenu(
			context,pm);
	}

	/**
	 * Called when we know for sure a user's address, so that we can add it to
	 * their record if needed.
	 * @param ua Address
	 */
	private void updateRecords(IRCUserAddress ua)
	{
		if(ua==null) return;
		NickInfo info=nickInfo.get(ua.getNick());
		if(info==null) return;
		if(info.ua==null)
			info.ua=ua;
	}

	/**
	 * Adds default channel actions for menus
	 * @param m Message that will receive data
	 */
	public void msg(IRCActionListMsg m)
	{
		if(!chan.equalsIgnoreCase(m.getContextChannel())) return;

		if(m.notUs() && m.hasSelectedNicks() && hasAtLeastPrefix('@'))
		{
			String[] selected=m.getSelectedNicks();
			String name=selected.length==1 ? selected[0] :
				selected.length+" people";

			m.addIRCAction(new NickAction(getPluginContext(),
				"Kick "+name+" out of "+chan,IRCAction.CATEGORY_USERCHAN,200,
				"/kick "+chan+" %%NICK%%"));

			m.addIRCAction(new NickAction(getPluginContext(),
				"Ban " + name + " from "+chan, IRCAction.CATEGORY_USERCHAN, 300,
				"/ban " + chan + " %%NICK%%"));

			// Do any of them already have ops?
			boolean anyOps=false,anyVoice=false,allOps=true,allVoice=true,fail=false;
			for(int i=0;i<selected.length;i++)
			{
				NickInfo info=nickInfo.get(selected[i]);
				if(info==null)
				{
					fail=true;
					break;
				}

				if(getServer().isPrefixAtLeast(info.cPrefix,'@'))
					anyOps=true;
				else
					allOps=false;
				if(getServer().isPrefixAtLeast(info.cPrefix,'+'))
					anyVoice=true;
				else
					allVoice=false;
			}

			if(!fail)
			{
				if(!anyOps)
					m.addIRCAction(new ModeAction(
						"Give "+name+" ops (@) on "+chan,100,true,'o'));
				if(!anyVoice)
					m.addIRCAction(new ModeAction(
						"Give "+name+" voice (+) on "+chan,110,true,'v'));
				if(allOps)
					m.addIRCAction(new ModeAction(
						"Remove ops (@) from "+name+" on "+chan,200,false,'o'));
				if(allVoice)
					m.addIRCAction(new ModeAction(
						"Remove voice (+) from "+name+" on "+chan,210,false,'v'));
			}
		}
	}

	class ModeAction extends AbstractIRCAction
	{
		private boolean plus;
		private char letter;

		/**
	 	 * @param name Menu name
		 * @param order Order within category
		 * @param plus True for +mode, false for -mode
		 * @param letter Letter being added/removed
		 */
		public ModeAction(String name,int order,boolean plus,char letter)
		{
			super(name,IRCAction.CATEGORY_USERCHAN,order);
			this.plus=plus;
			this.letter=letter;
		}

		@Override
		public void run(Server s,String contextChannel,String contextNick,
			String selectedChannel,String[] selectedNicks,MessageDisplay caller)
		{
			// Find
			int numModes=s.getMaxModeParams();

			Commands c=getPluginContext().getSingle(Commands.class);
			LinkedList<String> l = new LinkedList<String>(Arrays.asList(selectedNicks));
			while(!l.isEmpty())
			{
				String letters="",names="";
				for(int i=0;i<numModes && !l.isEmpty();i++)
				{
					letters+=letter;
					if(names.length()!=0)
					{
						names+=" ";
					}
					names += l.removeFirst();
				}
				c.doCommand(
					"/mode "+chan+" "+(plus?"+" : "-")+letters+" "+names,
					s,contextNick==null?null:new IRCUserAddress(contextNick,false),contextChannel,caller,false);
			}
		}
	}

	@Override
	public void showOwnText(int type,String target,String text)
	{
		if(type==MessageDisplay.TYPE_MSG || type==MessageDisplay.TYPE_ACTION)
			listUpdateRecent(getOwnNick());
		super.showOwnText(type,target,text);
	}
}
