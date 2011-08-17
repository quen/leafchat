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
package com.leafdigital.dcc;

import java.io.*;
import java.net.*;
import java.util.*;

import util.PlatformUtils;
import util.xml.*;

import com.leafdigital.irc.api.*;
import com.leafdigital.ircui.api.*;
import com.leafdigital.net.api.Network;
import com.leafdigital.notification.api.NotificationListMsg;
import com.leafdigital.prefs.api.Preferences;
import com.leafdigital.prefsui.api.PreferencesUI;
import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/**
 * Main plugin class for DCC handling.
 */
@UIHandler("dccprefs")
public class DCCPlugin implements Plugin
{
	private PluginContext context;
	private TransfersWindow tw;
	final static String NOTIFICATION_TRANSFERCOMPLETE="File transfer complete";

	@Override
	public void init(PluginContext pc, PluginLoadReporter plr) throws GeneralException
	{
		this.context=pc;

		new DCCCommands(pc);

		pc.requestMessages(UserCTCPRequestIRCMsg.class,this);
		pc.requestMessages(NotificationListMsg.class,this);

		UI ui=pc.getSingleton2(UI.class);
		Page p = ui.createPage("dccprefs", this);

		downloadLocationUI.setText(getDownloadFolder().toString());

		PreferencesUI preferencesUI=pc.getSingleton2(PreferencesUI.class);
		preferencesUI.registerPage(pc.getPlugin(),p);

		pc.requestMessages(IRCActionListMsg.class,this);
	}

	/**
	 * Message: Notification list. (Adds 'DCC transfer finished' to the list of
	 * things that users can be notified about.)
	 * @param msg Message
	 */
	public void msg(NotificationListMsg msg)
	{
		msg.addType(NOTIFICATION_TRANSFERCOMPLETE, true);
	}

	/**
	 * Message: CTCP request. Handles DCC SEND, DCC CHAT.
	 * @param msg
	 */
	public void msg(UserCTCPRequestIRCMsg msg)
	{
		if(!msg.getRequest().equals("DCC")) return;
		byte[][] params=IRCMsg.splitBytes(msg.getText());
		if(params.length<1) return;
		String command=IRCMsg.convertISO(params[0]).toUpperCase();

		if((command.equals("SEND") || command.equals("CHAT")) && params.length>=4)
		{
			// << :Frog!frog@95913ea5.mant.adsl.78b9d041.com.hmsk PRIVMSG frog :DCC SEND grass1.jpg 4294967295 0 106538 2
			// http://en.wikipedia.org/wiki/Direct_Client-to-Client#Reverse_.2F_Firewall_DCC
			try
			{
				long addressNum=Long.parseLong(IRCMsg.convertISO(params[2]));
				int port=Integer.parseInt(IRCMsg.convertISO(params[3]));
				InetAddress address=InetAddress.getByAddress(
					new byte[] {
						(byte)((addressNum>>24)&0xff),
						(byte)((addressNum>>16)&0xff),
						(byte)((addressNum>>8)&0xff),
						(byte)(addressNum&0xff)});

				if(command.equals("SEND"))
				{
					context.logDebug("Received DCC SEND: "+msg.getLineISO());
					long size=params.length==4 ? TransferProgress.SIZE_UNKNOWN :
						Long.parseLong(IRCMsg.convertISO(params[4]));
					new FileAcceptWindow(context,msg.getServer(),msg.getSourceUser(),address,port,params[1],size,msg);
					msg.markHandled();
				}
				else if(command.equals("CHAT") && IRCMsg.convertISO(params[1]).equalsIgnoreCase("chat"))
				{
					context.logDebug("Received DCC CHAT: "+msg.getLineISO());
					new DCCChatWindow(context,msg.getServer(),msg.getSourceUser().getNick(),
						address,port);
					msg.markHandled();
				}
				else
				{
					// TODO Possibly support other DCC protocols
				}
			}
			catch(NumberFormatException nfe)
			{
				return;
			}
			catch(UnknownHostException uhe)
			{
				// Can't happen
				throw new Error(uhe);
			}
		}
	}

	/**
	 * Message: IRC action list. (Adds menu items for DCC Send and DCC Chat when
	 * you click on a single user.)
	 * @param msg Message
	 */
	public void msg(IRCActionListMsg msg)
	{
		if(msg.hasSingleNick() && msg.notUs())
		{
			msg.addIRCAction(new NickAction(context,"DCC: Send a file to "+msg.getSingleNick()+"...",IRCAction.CATEGORY_USER,100,
				"/dccsend %%NICK%%"));
			msg.addIRCAction(new NickAction(context,"DCC: Chat directly with "+msg.getSingleNick(),IRCAction.CATEGORY_USER,110,
				"/dccchat %%NICK%%"));
		}
	}

	final static String
		PREF_DOWNLOADFOLDER="download-folder";

	File getDownloadFolder()
	{
		Preferences p=context.getSingleton2(Preferences.class);
		return new File(
			p.getGroup(context.getPlugin()).get(PREF_DOWNLOADFOLDER,
			PlatformUtils.getDownloadFolder()));
	}

	@Override
	public void close() throws GeneralException
	{
		// TODO Ought to cancel all transfers etc. here
	}

	@Override
	public String toString()
	{
		return "DCC plugin";
	}

	void transfersWindowClosed()
	{
		tw=null;
	}

	synchronized void startDownload(String nick,InetAddress address,int port,File target,File targetPartial,long size,long resumePos)
	{
		if(tw==null)
		{
			tw=new TransfersWindow(context);
		}
		TransferProgress tp=new TransferProgress(tw,context,false,nick,target.getName(),size);
		new Downloader(context,tp,address,port,target,targetPartial,resumePos,size);
	}
	synchronized void startListen(Server s,String nick,File source)
	{
		if(tw==null)
		{
			tw=new TransfersWindow(context);
		}
		TransferProgress tp=new TransferProgress(tw,context,true,nick,source.getName(),source.length());
		new Uploader(context,tp,s,nick,source);
	}
	synchronized void startChat(Server s,String nick)
	{
		new DCCChatWindow(context,s,nick);
	}

	// Prefs page

	/**
	 * Label: Download location.
	 */
	public Label downloadLocationUI;

	/**
	 * Action: User clicks to choose download location.
	 */
	@UIAction
	public void actionDownloadLocation()
	{
		UI ui=context.getSingleton2(UI.class);
		File f=ui.showFolderSelect(null,"Choose download folder", getDownloadFolder());
		if(f==null) return;
		Preferences p=context.getSingleton2(Preferences.class);
		p.getGroup(context.getPlugin()).set(PREF_DOWNLOADFOLDER,f.getAbsolutePath(),
			PlatformUtils.getDesktopFolder());
		downloadLocationUI.setText(XML.esc(getDownloadFolder().toString()));
	}

	/** Map of nickname -> address as IP string */
	private Map<String, String> dccAddress = new HashMap<String, String>();

	/**
	 * Remembers a user's address for use with DCC via a proxy.
	 * @param nick Nickname
	 * @param address Address as IP string
	 */
	void setDCCAddress(String nick,String address)
	{
		synchronized(dccAddress)
		{
			dccAddress.put(nick.toLowerCase(),address);
		}
	}

	/**
	 * @param nick Nickname
	 * @return Previously-set IP address or null if none
	 */
	String getDCCAddress(String nick)
	{
		synchronized(dccAddress)
		{
			return dccAddress.get(nick.toLowerCase());
		}
	}

	/**
	 * DCC represents IP addresses as integers, for some godforesaken reason.
	 * @param ia Internet address
	 * @return Stupid number representing address
	 * @throws GeneralException If the address is IPv6 (not supported)
	 */
	long getStupidIPNumber(InetAddress ia) throws GeneralException
	{
		long ipnumber;
		if(ia instanceof Inet6Address && !((Inet6Address)ia).isIPv4CompatibleAddress())
		{
			throw new GeneralException("DCC does not support IPv6");
		}
		byte[] addr=ia.getAddress();
		int
			b1=addr[addr.length-4],b2=addr[addr.length-3],
			b3=addr[addr.length-2],b4=addr[addr.length-1];

		ipnumber=
			(b4 & 0xff)  |
			((b3<<8) & 0xff00) |
			((b2<<16) & 0xff0000) |
			((b1<<24) & 0xff000000);
		return ipnumber;
	}

	/**
	 * @param nick Target nickname (used only when going via proxy)
	 * @return Suitable listening port for DCC
	 * @throws GeneralException Any error
	 */
	Network.Port getDCCListenPort(String nick) throws GeneralException
	{
		Network n=context.getSingleton2(Network.class);

		// Listen
		Network.Port p;
		try
		{
			// Get address if needed
			if(n.needsListenTarget())
			{
				String address=getDCCAddress(nick);
				if(address==null)
				{
					throw new GeneralException("Must use /dccaddress when using proxy");
				}
				p=n.listen(address);
			}
			else
			{
				p=n.listen();
			}
		}
		catch(IOException e)
		{
			throw new GeneralException("Error preparing port: "+e.getMessage());
		}
		return p;
	}

}
