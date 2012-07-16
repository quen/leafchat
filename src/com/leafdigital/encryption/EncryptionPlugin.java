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
package com.leafdigital.encryption;

import java.util.*;
import java.util.regex.*;

import util.xml.XML;

import com.leafdigital.irc.api.*;
import com.leafdigital.ircui.api.*;
import com.leafdigital.ui.api.UI;

import leafchat.core.api.*;

/**
 * Encryption plugin. Used for encrypted chat via custom CTCP messages.
 */
public class EncryptionPlugin implements Plugin
{
	static final String ALGORITHM_CIPHER="TripleDES";
	static final String ALGORITHM_KEYAGREEMENT="DH";

	private PluginContext context;

	/** Map of Server => String [LC nick] => EncryptionState */
	private Map<Server, Map<String, EncryptedWindow>> servers = null;

	@Override
	public void init(PluginContext context, PluginLoadReporter reporter) throws GeneralException
	{
		this.context=context;

		context.requestMessages(UserCTCPRequestIRCMsg.class,this);
		context.requestMessages(UserCTCPResponseIRCMsg.class,this);
		context.requestMessages(UserCommandMsg.class,this);
		context.requestMessages(UserCommandListMsg.class,this);
		context.requestMessages(IRCActionListMsg.class,this);
	}

	final static String CTCP_TEXT="ENCRYPTEDTEXT",CTCP_NICK="ENCRYPTEDNICKCHANGE",
		CTCP_INIT="ENCRYPTEDSTART",CTCP_END="ENCRYPTEDSTOP",CTCP_INFO="ENCRYPTEDINFO";

	/**
	 * Message: User command (used to handle /encryptedquery).
	 * @param msg Message
	 */
	public synchronized void msg(UserCommandMsg msg)
	{
		if("encryptedquery".equals(msg.getCommand()))
		{
			// Check they've entered only a single nick
			if(!msg.getParams().matches("[^ ]+"))
			{
				msg.getMessageDisplay().showError("Syntax: /encryptedquery PersonYouWantToTalkTo");
				msg.markHandled();
				return;
			}
			String nick=msg.getParams();

			// Check they aren't trying to talk to themselves
			if(nick.equalsIgnoreCase(msg.getServer().getOurNick()))
			{
				msg.getMessageDisplay().showError("You cannot start an encrypted chat with yourself.");
				msg.markHandled();
				return;
			}

			if(getWindow(msg.getServer(),nick)!=null)
			{
				msg.getMessageDisplay().showError("An encrypted chat window is already open to that nick. Close the window if you want to start again.");
				msg.markHandled();
				return;
			}

			// Open the window
			EncryptedWindow w=new EncryptedWindow(context,msg.getServer(),nick);
			addSession(msg.getServer(),nick,w);
			w.initLocal();
			msg.markHandled();
		}
	}

	/**
	 * Message: Listing available commands.
	 * @param msg Message
	 */
	public void msg(UserCommandListMsg msg)
	{
		msg.addCommand(true, "encryptedquery", UserCommandListMsg.FREQ_UNCOMMON,
			"/encryptedquery <nick>",
			"Open a securely-encrypted chat window with the named user");
	}

	/**
	 * Message: IRC action list (used to add encrypted chat option to menu when
	 * you click on a single user).
	 * @param msg Message
	 */
	public synchronized void msg(IRCActionListMsg msg)
	{
		if(!msg.hasSingleNick()) return;
		final String nick=msg.getSingleNick();
		if(nick.equalsIgnoreCase(msg.getServer().getOurNick())) return;
		if(getWindow(msg.getServer(),nick)!=null) return;

		msg.addIRCAction(new IRCAction()
		{
			@Override
			public int getCategory()
			{
				return CATEGORY_USER;
			}

			@Override
			public String getName()
			{
				return "Encrypted chat with "+nick;
			}

			@Override
			public int getOrder()
			{
				return 700;
			}

			@Override
			public void run(Server s,String contextChannel,String contextNick,String selectedChannel,String[] selectedNicks,MessageDisplay caller)
			{
				if(context.getSingle(UI.class).showOptionalQuestion(
					"encryption-requires-lc2-warning",null,"Encrypted chat requires leafChat 2",
					"Encrypted chat only works if the other person is using leafChat 2. " +
					"If you haven't checked, please make sure that they are leafChat " +
					"users before continuing.",UI.BUTTON_YES|UI.BUTTON_CANCEL,
					"Start encrypted chat",null,null,UI.BUTTON_CANCEL)==UI.BUTTON_YES)
				{
					context.getSingle(Commands.class).doCommand(
						"/encryptedquery "+nick,s,null,null,caller,false);
				}
			}
		});
	}

	private synchronized void addSession(Server s, String nick, EncryptedWindow w)
	{
		if(servers==null) servers = new HashMap<Server, Map<String, EncryptedWindow>>();
		Map<String, EncryptedWindow> sessions = servers.get(s);
		if(sessions==null)
		{
			sessions = new HashMap<String, EncryptedWindow>();
			servers.put(s,sessions);
		}
		sessions.put(nick.toLowerCase(),w);
	}

	synchronized void killSession(Server s, EncryptedWindow w)
	{
		if(servers==null) return;
		Map<String, EncryptedWindow> sessions = servers.get(s);
		if(sessions==null) return;
		for(Iterator<EncryptedWindow> i=sessions.values().iterator(); i.hasNext(); )
		{
			if(i.next()==w)
			{
				i.remove();
			}
		}
		if(sessions.isEmpty()) servers.remove(s);
		if(servers.isEmpty()) servers=null;
	}

	private static final Pattern KEYPARTS=Pattern.compile("([0-9]+)/([0-9]+)");

	/**
	 * Message: CTCP request. Used to detect the CTCP messages for encrypted
	 * chat.
	 * @param msg Message
	 */
	public void msg(UserCTCPRequestIRCMsg msg)
	{
		String nick=msg.getSourceUser().getNick();
		EncryptedWindow w=getWindow(msg.getServer(),nick);
		if(msg.getRequest().equals(CTCP_INIT))
		{
			String[] params=IRCMsg.convertISO(msg.getText()).split(" ",3);
			Matcher m=KEYPARTS.matcher(params[1]);

			if(params.length!=3 || !m.matches())
			{
				getMessageDisplay(msg.getServer()).showError(
					"Received encrypted-message request from <nick>"+XML.esc(msg.getSourceUser().getNick())+
					"</nick> in invalid format.");
				msg.markHandled();
				return;
			}

			int
				partNum=Integer.parseInt(m.group(1)),
				partTotal=Integer.parseInt(m.group(2));

			if(!params[0].equals("DH/TripleDES"))
			{
				if(partNum==1)
				{
					getMessageDisplay(msg.getServer()).showError(
						"Received encrypted-message request from <nick>"+XML.esc(msg.getSourceUser().getNick())+
						"</nick> with unsupported encryption format <key>"+XML.esc(params[0])+"</key>.");
					msg.getServer().sendLine(IRCMsg.constructBytes("NOTICE " +nick+" :\u0001ENCRYPTEDFORMATUNSUPPORTED "+
						params[0]+"\u0001"));
				}

				msg.markHandled();
				return;
			}

			if(partNum==1)
			{
				w=new EncryptedWindow(context,msg.getServer(),nick);
				addSession(msg.getServer(),nick,w);
			}
			else if(w==null)
			{
				getMessageDisplay(msg.getServer()).showError(
					"Received encrypted-message request from <nick>"+XML.esc(msg.getSourceUser().getNick())+
					"</nick> in invalid format.");
				msg.markHandled();
				return;
			}
			w.initRemote(params[2],partNum,partTotal);
			msg.markHandled();
			return;
		}
		if(msg.getRequest().equals(CTCP_NICK))
		{
			String[] params=IRCMsg.convertISO(msg.getText()).split(" ",3);
			if(params.length!=1)
			{
				getMessageDisplay(msg.getServer()).showError(
					"Received encrypted nick change from <nick>"+msg.getSourceUser().getNick()+
					"</nick> in invalid format.");
				msg.markHandled();
				return;
			}

			w=getWindow(msg.getServer(),params[0]);
			if(w==null)
			{
				getMessageDisplay(msg.getServer()).showError(
					"Received encrypted nick change from <nick>"+msg.getSourceUser().getNick()+
					"</nick> for window that no longer exists.");
				msg.markHandled();
				return;
			}

			w.nick(nick);
			Map<String, EncryptedWindow> nicks = servers.get(msg.getServer());
			nicks.remove(params[0].toLowerCase());
			nicks.put(nick,w);
			msg.markHandled();
			return;
		}

		if(msg.getRequest().equals(CTCP_TEXT))
		{
			String[] params=IRCMsg.convertISO(msg.getText()).split(" ",3);
			if(params.length!=2 || !params[0].matches("[AS]"))
			{
				getMessageDisplay(msg.getServer()).showError(
					"Received encrypted text from <nick>"+msg.getSourceUser().getNick()+
					"</nick> in invalid format.");
				msg.markHandled();
				return;
			}

			if(w==null)
			{
				getMessageDisplay(msg.getServer()).showError(
					"Received encrypted text from <nick>"+msg.getSourceUser().getNick()+
					"</nick> for window that no longer exists.");
			}
			else
			{
				w.text(params[0].equals("A"),params[1]);
			}
			msg.markHandled();
			return;
		}
		if(msg.getRequest().equals(CTCP_END))
		{
			// Just ignore if we get one of these after window was already closed
			if(w!=null)	w.end();
			msg.markHandled();
			return;
		}
		if(msg.getRequest().equals(CTCP_INFO))
		{
			msg.markHandled();
			return;
		}
	}

	private MessageDisplay getMessageDisplay(Server s)
	{
		return context.getSingle(IRCUI.class).getMessageDisplay(s);
	}

	/**
	 * Message: CTCP response. Used to handle responses to encrypted chat.
	 * @param msg Message
	 */
	public void msg(UserCTCPResponseIRCMsg msg)
	{
		if(msg.getRequest().equals(CTCP_INIT))
		{
			String nick=msg.getSourceUser().getNick();
			String[] params=IRCMsg.convertISO(msg.getText()).split(" ",3);
			Matcher m=KEYPARTS.matcher(params[0]);
			if(params.length!=2 || !m.matches())
			{
				getMessageDisplay(msg.getServer()).showError(
					"Received encrypted-message confirmation from <nick>"+XML.esc(msg.getSourceUser().getNick())+
					"</nick> in invalid format.");
				msg.markHandled();
				return;
			}

			EncryptedWindow w=getWindow(msg.getServer(),nick);
			if(w==null)
			{
				getMessageDisplay(msg.getServer()).showError(
					"Received encrypted text from <nick>"+msg.getSourceUser().getNick()+
					"</nick> for window that no longer exists.");
			}
			else
			{
				int
					partNum=Integer.parseInt(m.group(1)),
					partTotal=Integer.parseInt(m.group(2));
				w.finishInit(params[1],partNum,partTotal);
			}

			msg.markHandled();
		}
	}

	private synchronized EncryptedWindow getWindow(Server s,String nick)
	{
		if(servers==null) return null;
		Map<String, EncryptedWindow> sessions = servers.get(s);
		if(sessions==null) return null;
		return sessions.get(nick.toLowerCase());
	}

	@Override
	public void close() throws GeneralException
	{
	}

	@Override
	public String toString()
	{
	  // Used to display in system log etc.
		return "Encryption plugin";
	}
}
