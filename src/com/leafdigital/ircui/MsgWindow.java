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

import java.util.*;

import com.leafdigital.idle.api.Idle;
import com.leafdigital.irc.api.*;
import com.leafdigital.logs.api.Logger;
import com.leafdigital.notification.api.Notification;
import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/** Message window */
@UIHandler("msgwindow")
public class MsgWindow extends ServerChatWindow
{
	private String nick;
	private IRCUserAddress lastAddress;
	private boolean maskAdded;

	/** List of existing message windows */
	private static List<MsgWindow> lExisting = new LinkedList<MsgWindow>();

	/**
	 * Finds existing message window, if there is one.
	 * @param s Server
	 * @param sNick Nickname
	 * @return Window or null if none
	 */
	static MsgWindow find(Server s, String sNick)
	{
		for(MsgWindow mw : lExisting)
		{
			if(mw.getServer()==s && mw.nick==sNick)
			{
				return mw;
			}
		}
		return null;
	}

	private boolean firstMessage = false;

	/**
	 * @param pc Plugin context for messages etc
	 * @param uim IRC message that we're responding to
	 * @param bVisible True if window should appear, false to minimise
	 * @throws GeneralException
	 */
	public MsgWindow(PluginContext pc, UserIRCMsg uim, boolean bVisible) throws GeneralException
	{
		this(pc, uim.getServer(), uim.getSourceUser().getNick(), bVisible, true);

		// Dispatch msg to appropriate msg() method
		firstMessage = true;
		pc.dispatchMsgToTarget(uim, this);

		lExisting.add(this);
	}

	@Override
	protected void reportActualMessage(String title, String text)
	{
		if(firstMessage)
		{
			getPluginContext().getSingle(Notification.class).notify(
				IRCUIPlugin.NOTIFICATION_NEWWINDOW, title,
				text + "\n\n(New window opened)");

			firstMessage = false;
		}
		else
		{
		 super.reportActualMessage(title, text);
		}
	}

	@Override
	@UIAction
	public void windowClosed() throws GeneralException
	{
		// Remove watchlist
		if(isConnected() && maskAdded)
		{
			getPluginContext().getSingle(WatchList.class).
				removeTemporaryMask(getServer(), new IRCUserAddress(nick, true));
			maskAdded = false;
		}

		super.windowClosed();
		lExisting.remove(this);
	}

	/**
	 * Set once we know what the user's status is. Once status is known, we
	 * start displaying messages about it.
	 */
	private boolean statusKnown=false;

	@Override
	public String toString()
	{
		String server = getServer()==null
			? "unknown server"
			: getServer().getReportedOrConnectedHost();
		return "[MsgWindow: " + nick + " on " + server + "]";
	}

	/**
	 * @param pc Context
	 * @param s Server
	 * @param nick Nickname
	 * @param visible True if window should be visible
	 * @throws GeneralException
	 */
	public MsgWindow(PluginContext pc, Server s, String nick, boolean visible) throws GeneralException
	{
		this(pc, s, nick, visible, false);
	}

	private int requestUser, requestQuit, requestNick;

	private void requestMessages(boolean removeOld)
	{
		if(removeOld)
		{
			getPluginContext().unrequestMessages(UserIRCMsg.class, this, requestUser);
			getPluginContext().unrequestMessages(QuitIRCMsg.class, this, requestQuit);
			getPluginContext().unrequestMessages(NickIRCMsg.class, this, requestNick);
		}

		NickAndServerFilter usf = new NickAndServerFilter(getServer(), nick);
		requestUser = getPluginContext().requestMessages(UserIRCMsg.class, this, usf, Msg.PRIORITY_NORMAL);
		requestQuit = getPluginContext().requestMessages(QuitIRCMsg.class, this, usf, Msg.PRIORITY_NORMAL);
		requestNick = getPluginContext().requestMessages(NickIRCMsg.class, this, usf, Msg.PRIORITY_NORMAL);
	}

	@Override
	protected void initServerInner(Server s, boolean firstTime)
	{
		if(s != null)
		{
			requestMessages(!firstTime);
			if(!firstTime) 	// Can't do it here first time 'cause nick is not set
			{
				getPluginContext().getSingle(WatchList.class).
					addTemporaryMask(s, new IRCUserAddress(nick, true));
				maskAdded = true;
			}
		}
	}

	/**
	 * @param pc Context
	 * @param s Server
	 * @param nick Nick
	 * @param visible Visible
	 * @param definitelyOnline True if they're definitely online (b/c we just
	 *  received a message)
	 * @throws GeneralException
	 */
	public MsgWindow(PluginContext pc, Server s, String nick, boolean visible, boolean definitelyOnline) throws GeneralException
	{
		super(pc, s, "msgwindow", false, visible);
		this.nick = nick;

		setTitle();

		// Start listening
		initServerInner(s, true);
		pc.requestMessages(WatchMsg.class, this, null, Msg.PRIORITY_EARLY);

		// Watch this user
		WatchList wl = pc.getSingle(WatchList.class);
		wl.addTemporaryMask(s, new IRCUserAddress(nick, true));
		maskAdded = true;
		if(!(definitelyOnline || (!wl.isKnown(s, nick) || wl.isOnline(s, nick))))
		{
			addLine(EVENTSYMBOL+"<nick>"+esc(nick)+"</nick> is <key>not online</key>");
		}
		statusKnown=definitelyOnline || wl.isKnown(s, nick);

		// Show window
		getWindow().setRemember("message", nick);
		getCommandEdit().setRemember("message", nick);
		((IRCUIPlugin)getPluginContext().getPlugin()).informShown(this);
		getWindow().show(!visible);
	}

	/**
	 * Watch message: user logged on/off.
	 * @param wm Messge
	 */
	public void msg(WatchMsg wm)
	{
		if(!wm.getNick().equalsIgnoreCase(nick) || !(wm.getServer()==getServer()))
		{
			return;
		}

		boolean on = wm instanceof OnWatchMsg;
		EditBox command = (EditBox)getWindow().getWidget("command");

		if(on && !command.isEnabled())
		{
			if(statusKnown)
			{
				addLine(EVENTSYMBOL + "<nick>" + esc(nick) + "</nick> logged <key>on</key>");
			}
			statusKnown=true;
		}
		else if(!on && command.isEnabled())
		{
			if(statusKnown)
			{
				addLine(EVENTSYMBOL + "<nick>" + esc(nick) + "</nick> logged <key>off</key>");
			}
			else
			{
				addLine(EVENTSYMBOL + "<nick>" + esc(nick) + "</nick> is <key>not online</key>");
			}
			statusKnown=true;
		}

		wm.markHandled();
	}

	@Override
	protected void setTitle()
	{
		getWindow().setTitle(nick + " (" + getServer().getCurrentShortName() + ")");
	}

	@Override
	protected void doCommand(Commands c, String line) throws GeneralException
	{
		getPluginContext().getSingle(Idle.class).userAwake(
			line.equals("/away") ? Idle.AWAKE_UNAWAY : Idle.AWAKE_COMMAND);

		IRCUserAddress contextUser;
		if(lastAddress != null && lastAddress.getNick().equals(nick))
		{
			contextUser = lastAddress;
		}
		else
		{
			contextUser = new IRCUserAddress(nick, false);
		}
		c.doCommand(line, getServer(), contextUser, null, this, true);
	}

	/**
	 * Server message: standard message.
	 * @param m Message
	 * @throws GeneralException
	 */
	public void msg(UserMessageIRCMsg m) throws GeneralException
	{
		if(m.isHandled())
		{
			return;
		}

		IRCUserAddress ua = m.getSourceUser();
		lastAddress = ua;
		addLine("&lt;<nick>" + esc(ua.getNick()) + "</nick>&gt; "
			+ esc(m.convertEncoding(m.getText())), "msg");
		reportActualMessage(nick, m.convertEncoding(m.getText()));
		m.markHandled();
	}

	/**
	 * Server message: action.
	 * @param m Message
	 * @throws GeneralException
	 */
	public void msg(UserActionIRCMsg m) throws GeneralException
	{
		if(m.isHandled())
		{
			return;
		}

		IRCUserAddress ua = m.getSourceUser();
		lastAddress = ua;
		addLine(ACTIONSYMBOL + "<nick>" + esc(ua.getNick()) + "</nick> "
			+ esc(m.convertEncoding(m.getText())), "action");
		reportActualMessage(nick, ACTIONSYMBOL + ua.getNick() + " "
			+ m.convertEncoding(m.getText()));
		m.markHandled();
	}

	@Override
	public void msg(NickIRCMsg m) throws GeneralException
	{
		WatchList wl = getPluginContext().getSingle(WatchList.class);
		if(maskAdded)
		{
			wl.removeTemporaryMask(getServer(), new IRCUserAddress(nick, true));
			maskAdded = false;
		}
		nick = m.getNewNick();
		wl.addTemporaryMask(getServer(), new IRCUserAddress(nick, true));
		maskAdded = true;
		getWindow().setTitle(nick + " (" + getServer() + ")");
		getWindow().setRemember("message", nick);
		getCommandEdit().setRemember("message", nick);
		requestMessages(true);
		super.msg(m);
	}

	/**
	 * Server message: other user message (shouldn't be called)
	 * @param m Message
	 */
	public void msg(UserIRCMsg m)
	{
		// Called for types we don't handle specifically, like UserNotice and CTCP
		// which both ought to go to current window
	}

	@Override
	protected boolean isUs(String sTarget)
	{
		return sTarget.equalsIgnoreCase(nick);
	}

	@Override
	protected int getAvailableBytes() throws GeneralException
	{
		if(nick==null)
		{
			return 400;
		}

		// RFC limit is 512 per line including CRLF. The system will send to other users:
		// :nick!user@host PRIVMSG <target> :<message><CRLF>
		// So there are 499 characters available for the prefix+nick+message
		// Nicks etc. are always converted using ISO one byte per character.
		return 499
			-nick.length()
			-getServer().getApproxPrefixLength();
	}

	@Override
	protected String getLogCategory()
	{
		return Logger.CATEGORY_USER;
	}

	@Override
	protected String getLogItem()
	{
		return nick;
	}

	@Override
	public void fillTabCompletionList(TabCompletionList options)
	{
		options.add(nick, true);
	}

	@Override
	protected boolean displayTimeStamps()
	{
		return true;
	}

	@Override
	protected String getContextNick()
	{
		return nick;
	}
}
