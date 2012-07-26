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
package com.leafdigital.ircui;

import java.util.*;

import util.xml.*;

import com.leafdigital.irc.api.*;
import com.leafdigital.ircui.api.*;
import com.leafdigital.ircui.api.GeneralChatWindow.Handler;
import com.leafdigital.notification.api.*;
import com.leafdigital.prefs.api.*;
import com.leafdigital.prefsui.api.PreferencesUI;
import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/** IRC user-interface plugin. */
public class IRCUIPlugin implements Plugin,IRCUI,DefaultMessageDisplay
{
	/** Turn this on to make it fake connection on run */
	final static boolean USEFAKESERVER="y".equals(System.getProperty("leafchat.fakeserver"));
	final static String
		NOTIFICATION_DEIDLE="New message after long idle period",
		NOTIFICATION_NEWWINDOW="New message window appears",
		NOTIFICATION_DISCONNECTED="Server disconnected",
		NOTIFICATION_WINDOWMINIMIZED="New message when window is minimized",
		NOTIFICATION_APPLICATIONINACTIVE="New message when application is inactive";
	final static String
	  PREF_CLOSESPAREWINDOWS="close-spare-windows",
	  PREFDEFAULT_CLOSESPAREWINDOWS="t";

	/** Will not autoreconnect again to same server within 2 minutes */
	private final static long AUTO_RECONNECT_FREQUENCY = 120 * 1000;

	private PluginContext context;
	private ConnectTool ct;
	private JoinTool jt;
	private EntryTool et;
	private ActionListOwner alo;
	private KnownUsers ku;

	private static class ReconnectInfo
	{
		String host;
		int port;
		long time;

		private ReconnectInfo(String host, int port)
		{
			this.host = host;
			this.port = port;
			this.time = System.currentTimeMillis();
		}
	}

	private HashMap<ServerDisconnectedMsg, ReconnectInfo> autoReconnects =
		new HashMap<ServerDisconnectedMsg, ReconnectInfo>();

	@Override
	public void init(PluginContext context, PluginLoadReporter plr) throws GeneralException
	{
		this.context=context;

		if(USEFAKESERVER)
			new FakeServer();

		context.getSingle(Connections.class).setDefaultMessageDisplay(this);

		ct=new ConnectTool(context,this);
		UI ui=context.getSingle(UI.class);
		ui.registerTool(ct);
		jt=new JoinTool(context);
		ui.registerTool(jt);
		et=new EntryTool(context);
		ui.registerTool(et);

		context.requestMessages(IRCMsg.class,this,Msg.PRIORITY_LATE);
		context.requestMessages(ServerConnectionFinishedMsg.class,this);
		context.requestMessages(ServerConnectedMsg.class, this);
		context.requestMessages(WatchMsg.class,this,Msg.PRIORITY_LATE);
		context.requestMessages(ServerRearrangeMsg.class,this,Msg.PRIORITY_FIRST);
		context.requestMessages(SystemStateMsg.class,this);
		context.requestMessages(MinuteMsg.class, this);

		context.registerSingleton(IRCUI.class,this);

		alo=new ActionListOwner(context);

		PreferencesUI preferencesUI=context.getSingle(PreferencesUI.class);
		preferencesUI.registerPage(this,(new IgnoreListPage(context)).getPage());
		preferencesUI.registerPage(this,(new WatchListPage(context)).getPage());
		preferencesUI.registerPage(this,(new EncodingPage(context)).getPage());
		preferencesUI.registerPage(this,(new PrefsMiscPage(context)).getPage());
		preferencesUI.registerPage(this,(new CommandListPage(context)).getPage());
		preferencesUI.registerWizardPage(this,100,(new WizardProfilePage(context)).getPage());
		preferencesUI.registerWizardPage(this,200,(new WizardPersonalPage(context)).getPage());

		new UICommands(context);

		context.requestMessages(IRCActionListMsg.class,this);

		ku=new KnownUsers(context);

		context.requestMessages(NotificationListMsg.class,this);
		context.requestMessages(ServerDisconnectedMsg.class,this);
	}

	/**
	 * Notification message: called to get list of notification types.
	 * @param msg Message
	 */
	public void msg(NotificationListMsg msg)
	{
		msg.addType(NOTIFICATION_DEIDLE,true);
		msg.addType(NOTIFICATION_DISCONNECTED,true);
		msg.addType(NOTIFICATION_NEWWINDOW,true);
		msg.addType(NOTIFICATION_WINDOWMINIMIZED,true);
		msg.addType(NOTIFICATION_APPLICATIONINACTIVE,false);
	}

	/**
	 * Server message: server is disconnected.
	 * @param msg Message
	 */
	public void msg(ServerDisconnectedMsg msg)
	{
		// Notify unless it was caused by user/system quit request
		if(!msg.getServer().wasQuitRequested())
			context.getSingle(Notification.class).notify(
				NOTIFICATION_DISCONNECTED,"Disconnected: "+msg.getServer().getReportedOrConnectedHost(),"");
	}

	void reconnect(String host,int port) throws GeneralException
	{
		ct.directConnect(host,port);
	}

	private LinkedList<JoinRequest> joinRequests = null;

	private static class JoinRequest
	{
		/**
		 * Join requests time-out after 2 minutes so it doesn't randomly join
		 * a channel when you connect 2 hours later.
		 */
		private final static long TIMEOUT=120000;

		private PreferencesGroup server;
		private String name, key, host;
		private long time;
		public JoinRequest(PreferencesGroup server,String name,String key)
		{
			this.server=server;
			this.name=name;
			this.key=key;
			time=System.currentTimeMillis();
		}
		public JoinRequest(String host, String name, String key)
		{
			this.host = host;
			this.name = name;
			this.key = key;
			time = System.currentTimeMillis();
		}
		boolean timedOut()
		{
			return System.currentTimeMillis()-time > TIMEOUT;
		}
	}

	/**
	 * Adds a join request for a specific channel and hostname, but does not
	 * actually initiate connect.
	 * @param host Hostname
	 * @param channel Channel name
	 * @param key Channel key or empty string if none
	 */
	void addJoinRequest(String host, String channel, String key)
	{
		synchronized(this)
		{
			if(joinRequests==null) joinRequests = new LinkedList<JoinRequest>();
			joinRequests.add(new JoinRequest(host, channel, key));
		}
	}

	/**
	 * Connects to a server and joins a channel.
	 * @param server Preferences entry referring to server or network
	 * @param name Name of channel
	 * @param key Key (password) or empty string if none
	 * @throws GeneralException
	 */
	void connectAndJoin(PreferencesGroup server,String name,String key) throws GeneralException
	{
		ct.directConnect(server);
		synchronized(this)
		{
			if(joinRequests==null) joinRequests = new LinkedList<JoinRequest>();
			joinRequests.add(new JoinRequest(server,name,key));
		}
	}

	@Override
	public void close() throws GeneralException
	{
		context.getSingle(Connections.class).setDefaultMessageDisplay(null);
	}

	@Override
	public String toString()
	{
		return "IRC UI plugin";
	}

	/**
	 * Message: Connected. Used to turn off auto-join if needed.
	 * @param msg Message
	 */
	public void msg(ServerConnectedMsg msg)
	{
		JoinRequest request = getMatchingJoinRequest(msg.getServer(), false);
		if(request != null)
		{
			msg.getServer().suppressAutoJoin();
		}
	}

	private JoinRequest getMatchingJoinRequest(Server server, boolean remove)
	{
		synchronized(this)
		{
			if(joinRequests!=null)
			{
				for(Iterator<JoinRequest> i=joinRequests.iterator(); i.hasNext();)
				{
					JoinRequest request=i.next();
					if(request.timedOut())
					{
						i.remove();
						continue;
					}
					if(request.server != null)
					{
						// Join request specified relating to a preferences item
						PreferencesGroup pg = server.getPreferences();
						if(pg==request.server || pg.getAnonParent()==request.server)
						{
							if(remove)
							{
								i.remove();
							}
							return request;
					  }
					}
					else
					{
						// Join request specified by hostname
						if(server.getConnectedHost().equals(request.host))
						{
							if(remove)
							{
								i.remove();
							}
							return request;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Message: Connection finished. Used to join channels requested by
	 * {@link #connectAndJoin(PreferencesGroup, String, String)}.
	 * @param msg Message
	 * @throws GeneralException
	 */
	public void msg(ServerConnectionFinishedMsg msg) throws GeneralException
	{
		JoinRequest request = getMatchingJoinRequest(msg.getServer(), true);
		if(request != null)
		{
			msg.getServer().sendLine(IRCMsg.constructBytes("JOIN " +
				request.name + (request.key.length()>0 ? " " + request.key : "")));
		}
	}

	/**
	 * Message: join. Used to open new channel window.
	 * @param msg Message
	 * @throws GeneralException
	 */
	public void msg(JoinIRCMsg msg) throws GeneralException
	{
		if(!msg.isHandled())
			new ChanWindow(context,msg);
	}

	/**
	 * Message: user message. Used to open new message window.
	 * @param msg Message
	 * @throws GeneralException
	 */
	public void msg(UserMessageIRCMsg msg) throws GeneralException
	{
		if(!msg.isHandled())
			openNewMessageWindow(msg);
	}

	/**
	 * Message: user action. Used to open new message window.
	 * @param msg Message
	 * @throws GeneralException
	 */
	public void msg(UserActionIRCMsg msg) throws GeneralException
	{
		if(!msg.isHandled())
			openNewMessageWindow(msg);
	}

	private void openNewMessageWindow(UserIRCMsg msg)
		throws GeneralException
	{
		new MsgWindow(context,msg,false);
	}

	/**
	 * General IRC message. Used to ensure message is displayed in available
	 * window.
	 * @param msg Message
	 * @throws GeneralException
	 */
	public void msg(IRCMsg msg) throws GeneralException
	{
		// Messages that are supposed not to be displayed
		if(msg instanceof PartIRCMsg)
		{
			msg.markHandled();
		}
		// Unhandled messages
		if(!msg.isHandled())
		{
			getWindow(msg.getServer()).handleUnhandledMsg(msg);
		}
	}

	/**
	 * Message: watch message. Used to ensure message is displayed.
	 * @param msg Message
	 * @throws GeneralException
	 */
	public void msg(WatchMsg msg) throws GeneralException
	{
		if(!msg.isHandled())
		{
			getWindow(msg.getServer()).handleWatchMsg(msg);
		}
	}

	/**
	 * Message: server rearrange. Used to offer the choice of placing a new
	 * server in a network.
	 * @param msg Message
	 * @throws GeneralException
	 */
	public void msg(ServerRearrangeMsg msg) throws GeneralException
	{
		UI u=context.getSingle(UI.class);
		int result=u.showQuestion(null,
			"Server organisation",msg.getText(),UI.BUTTON_YES|UI.BUTTON_NO,
			msg.getButtonConfirm(),msg.getButtonOther(),null,UI.BUTTON_YES);
		if(result==UI.BUTTON_YES)
			msg.confirm();
		else
			msg.reject();

		msg.markStopped();
	}

	/** List of windows, most recently active first */
	private LinkedList<ChatWindow> activeChatWindows =
		new LinkedList<ChatWindow>();

	/**
	 * Obtains the most recent window for a given server.
	 * @param s Server (may be null)
	 * @return Window or null if no windows for that server
	 */
	synchronized ServerChatWindow getRecentWindow(Server s)
	{
		for(ChatWindow cw : activeChatWindows)
		{
			if((cw instanceof ServerChatWindow)
				&& ((ServerChatWindow)cw).getServer()==s)
			{
				return (ServerChatWindow)cw;
			}
		}
		return null;
	}

	/**
	 * Obtains any recent window (regardless of server)
	 * @return Window
	 */
	synchronized ChatWindow getRecentWindow()
	{
		if(activeChatWindows.isEmpty())
			return null;
		else
			return activeChatWindows.getFirst();
	}

	/**
	 * Called when a window is closed.
	 * @param cw Closed window
	 */
	void informClosed(ChatWindow cw)
	{
		activeChatWindows.remove(cw);
	}

	/**
	 * Called when a window is made active. Keeps a list of active windows
	 * so we know the current window for a server.
	 * @param cw Window
	 */
	void informActive(ChatWindow cw)
	{
		activeChatWindows.remove(cw);
		activeChatWindows.addFirst(cw);
	}

	/**
	 * Called when a window is shown, just to make sure it's in the list of
	 * windows even if it is never made active.
	 * @param cw Window
	 */
	void informShown(ChatWindow cw)
	{
		activeChatWindows.remove(cw);
		// Last, because it's never been active
		activeChatWindows.addLast(cw);
	}

	EditBox.TabCompletion newTabCompletion(ChatWindow first)
	{
		return new NickChanCompletion(first);
	}

	private class NickChanCompletion implements EditBox.TabCompletion
	{
		private ChatWindow first;

		NickChanCompletion(ChatWindow first)
		{
			this.first=first;
		}

		@Override
		public String[] complete(String partial, boolean atStart)
		{
			TabCompletionList tcl = new TabCompletionList(partial, atStart);

			// Do the specified chat window
			if(first != null)
			{
				first.fillTabCompletionList(tcl);
			}

			// Do all other windows
			for(ChatWindow cw : activeChatWindows)
			{
				if(cw == first)
				{
					continue;
				}
				cw.fillTabCompletionList(tcl);
			}

			// Include favourite channels
			try
			{
				JoinTool.ChannelInfo[] favourites = JoinTool.getFavouriteChannels(context);
				for(int i=0; i<favourites.length; i++)
				{
					tcl.add(favourites[i].name, false);
				}
			}
			catch(GeneralException ge)
			{
				ErrorMsg.report("Error obtaining favourite channels for tab-completion list", ge);
			}

			// Include commands
			if(atStart && partial.startsWith("/"))
			{
				Server server = null;
				if(first != null && (first instanceof ServerChatWindow))
				{
					server = ((ServerChatWindow)first).getServer();
				}
				UserCommandListMsg m = new UserCommandListMsg(server);
				try
				{
					context.dispatchExternalMessage(UserCommandListMsg.class, m, true);
				}
				catch(GeneralException e)
				{
					ErrorMsg.report("Error obtaining command list for tab completion", e);
				}
				for(String command : m.getCommands())
				{
					tcl.add("/" + command + " ", false);
				}
			}

			return tcl.getResult();
		}
	}

	private Map<Server, SpareWindow> spareWindows =
		new HashMap<Server, SpareWindow>();

	synchronized void spareWindowClosed(Server s)
	{
		spareWindows.remove(s);
	}

	synchronized void gotNonSpareWindow(Server s)
	{
		SpareWindow w=spareWindows.get(s);
		if(w!=null) w.gotOtherWindow();
	}

	synchronized boolean hasNonSpareWindow(Server s)
	{
		for(ChatWindow cw : activeChatWindows)
		{
			if((cw instanceof ServerChatWindow) && !(cw instanceof SpareWindow) &&
				((ServerChatWindow)cw).getServer()==s) return true;
		}
		return false;
  }

	synchronized ServerChatWindow getWindow(Server s) throws GeneralException
	{
		ServerChatWindow scw = getRecentWindow(s);
		if(scw == null)
		{
			SpareWindow spare = new SpareWindow(context, s);
			spareWindows.put(s, spare);
			scw = spare;
			informActive(scw);
				// Should get called anyway but maybe this will be too
				// late, if we request the same spare window twice very quickly
			  // (appears to affect external windows only)
		}
		return scw;
	}

	@Override
	public MessageDisplay getMessageDisplay(final Server s)
	{
		return new MessageDisplay()
		{
			private ChatWindow cw=null;

			private ChatWindow getChatWindow()
			{
				if(cw!=null && !cw.getWindow().isClosed())
				{
					return cw;
				}
				try
				{
					cw=getWindow(s);
				}
				catch(GeneralException ge)
				{
					ErrorMsg.report("Error obtaining default window",ge);
				}
				return cw;
			}

			@Override
			public void showInfo(String message)
			{
				getChatWindow().showInfo(message);
			}

			@Override
			public void showError(String message)
			{
				getChatWindow().showError(message);
			}

			@Override
			public void showOwnText(int type,String target,String text)
			{
				getChatWindow().showOwnText(type,target,text);
			}

			@Override
			public void clear()
			{
				getChatWindow().clear();
			}
		};
	}

	ActionListOwner getActionListOwner()
	{
		return alo;
	}

	/**
	 * Actionlist message: used to add nickname actions to menus.
	 * @param m Message
	 */
	public void msg(IRCActionListMsg m)
	{
		if(m.hasSingleNick())
		{
			boolean us = !m.notUs();

			m.addIRCAction(new NickAction(context,
				"Chat privately with " + (us ? "yourself" : m.getSingleNick()),
				IRCAction.CATEGORY_USER, 10, "/query %%NICK%%"));
			m.addIRCAction(new NickAction(context,
				"Get info about " + (us ? "yourself" : m.getSingleNick()),
				IRCAction.CATEGORY_USER, 20, "/whois %%NICK%%"));
			if(!us)
			{
				m.addIRCAction(new NickAction(context,
					"Block messages from " + m.getSingleNick(), IRCAction.CATEGORY_USER, 1000,
					"/ignore %%NICK%%"));
			}
		}
	}

	/**
	 * Quit confirmation dialog that knows about connected servers.
	 */
	@UIHandler("quitconfirm")
	public class QuitConfirm
	{
		QuitConfirm(SystemStateMsg m,Server[] connected) throws GeneralException
		{
			objectToQuit=false;
			UI ui=context.getSingle(UI.class);
			quitConfirm = ui.createDialog("quitconfirm", this);

			StringBuffer list=new StringBuffer();
			for(int i=0;i<connected.length;i++)
			{
				list.append("<line>"+XML.esc(connected[i].getReportedOrConnectedHost())+"</line>");
			}
			serverListUI.setText(list.toString());

			quitConfirm.show(null);

			if(objectToQuit) m.markStopped();
		}
		private Dialog quitConfirm;
		/** Label listing server names */
		public Label serverListUI;
		private boolean objectToQuit;

		/** Action: User chooses to quit. */
		@UIAction
		public void actionQuit()
		{
			quitConfirm.close();
		}

		/** Action: User cancels quit. */
		@UIAction
		public void actionCancel()
		{
			quitConfirm.close();
			objectToQuit=true;
		}
	}

	/**
	 * System state message. Used to warn about quit causing disconnect.
	 * @param msg Message
	 * @throws GeneralException
	 */
	public void msg(SystemStateMsg msg) throws GeneralException
	{
		if(msg.getType()==SystemStateMsg.REQUESTSHUTDOWN)
		{
			Connections c=context.getSingle(Connections.class);
			Server[] connected=c.getConnected();
			if(connected.length==0)	return; // Not connected, so quit is ok

			new QuitConfirm(msg,connected);
		}
	}

	@Override
	public GeneralChatWindow createGeneralChatWindow(PluginContext owner,
		Handler h, String logSource, String logCategory, String logItem,
		int availableBytes, String ownNick, String target, boolean startMinimised)
	{
		try
		{
			PluginChatWindow pcw=new PluginChatWindow(context,
				owner, h, logSource, logCategory, logItem, availableBytes, ownNick, target,startMinimised);
			informActive(pcw);
			return pcw;
		}
		catch(GeneralException e)
		{
			throw new BugException(e);
		}
	}

	KnownUsers getKnownUsers()
	{
		return ku;
	}

	/**
	 * Minute message: used to clear out old reconnect info to make sure it
	 * doesn't cause a memory leak.
	 * @param msg Message
	 */
	public void msg(MinuteMsg msg)
	{
		long threshold = System.currentTimeMillis() - AUTO_RECONNECT_FREQUENCY;
		for(Iterator<ReconnectInfo> i = autoReconnects.values().iterator(); i.hasNext(); )
		{
			ReconnectInfo info = i.next();
			if(info.time < threshold)
			{
				i.remove();
			}
		}
	}

	/**
	 * Called when a ServerDisconnectedMsg is received. As this is passed on by
	 * ServerChatWindow, it may be called multiple times for a single disconnected
	 * message; the actual reconnect should only happen once. However, the
	 * return value will still be true if a reconnect is occuring for this
	 * message, even if it was already triggered.
	 * @param msg Message
	 * @return True if autoreconnect is happening for this disconnect event
	 * @throws GeneralException Any error during reconnect
	 */
	public boolean considerAutoReconnect(ServerDisconnectedMsg msg) throws GeneralException
	{
		// Check auto-reconnect is turned on
		Preferences prefs = context.getSingle(Preferences.class);
		PreferencesGroup group = prefs.getGroup(prefs.getPluginOwner(
			IRCPrefs.IRCPLUGIN_CLASS));
		if(!prefs.toBoolean(group.get(
			IRCPrefs.PREF_AUTORECONNECT, IRCPrefs.PREFDEFAULT_AUTORECONNECT)))
		{
			return false;
		}

		// Did we already do a reconnect for this message?
		if(autoReconnects.containsKey(msg))
		{
			return true;
		}

		// See if we already have a connection to the same network
		String network = msg.getServer().getPreferences().getAnonHierarchical(
			IRCPrefs.PREF_NETWORK, null);
		if(network != null)
		{
			Connections c = context.getSingle(Connections.class);
			for(Server connected : c.getConnected())
			{
				String other = connected.getPreferences().getAnonHierarchical(
					IRCPrefs.PREF_NETWORK, null);
				if(network.equals(other))
				{
					return false;
				}
			}
		}

		// Get server and port
		String host = msg.getServer().getConnectedHost();
		int port = msg.getServer().getConnectedPort();
		for(ReconnectInfo info : autoReconnects.values())
		{
			if(info.host.equals(host) && info.port == port)
			{
				return false;
			}
		}

		// If somebody marks the message handled, don't do anything
		if(msg.isHandled())
		{
			return false;
		}

		// Trigger reconnect, mark handled, and add to list
		autoReconnects.put(msg, new ReconnectInfo(host, port));
		reconnect(host, port);
		msg.markHandled();
		return true;
	}
}
