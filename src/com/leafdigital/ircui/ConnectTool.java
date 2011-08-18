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
import java.util.regex.*;

import javax.swing.SwingUtilities;

import util.xml.*;

import com.leafdigital.irc.api.*;
import com.leafdigital.prefs.api.*;
import com.leafdigital.ui.api.*;
import com.leafdigital.ui.api.TreeBox.Item;

import leafchat.core.api.*;

/** Connect icon and dialog. */
@UIHandler("connect")
public class ConnectTool implements SimpleTool, TreeBox.MultiSelectionHandler
{
	private static final String PREFS_SINGLE="single";
	private static final String PREFS_LIST="list";
	private static final String PREFS_LASTCONNECT="lastconnect";
	private static final String PREFS_AUTOSHOW="show-connect";
	private static final String PREFS_ANDCONNECT="and-connect";
	private PluginContext context;
	private IRCUIPlugin plugin;
	private Window window=null;

	/** Checkbox controlling whether dialog shows on launch */
	public CheckBox autoShowUI;
	/** Checkbox controlling whether to automatically connect on launch too */
	public CheckBox andConnectUI;
	/** Server tree-list */
	public TreeBox serverselectUI;
	/** Tab panel */
	public TabPanel tabsUI;
	/** Button that you can click while connecting to return to the dialog */
	public Button backUI;
	/** Connect button */
	public Button connectUI;
	/** Server name box */
	public EditBox serverUI;
	/** Disconnect list */
	public ListBox disconnectlistUI;
	/** Disconnect button */
	public Button disconnectUI;

	/** UI: Choice panel for switching page */
	public ChoicePanel choiceUI;

	private String directHost;
	private int directPort;
	private PreferencesGroup directServer;

	private boolean direct=false;

	private boolean autoShow=false;

	private PrefsServerPage ps;

	private Item[] selectedServers=null;

	/**
	 * @param pc Context
	 * @param iup Plugin
	 * @throws GeneralException
	 */
	public ConnectTool(PluginContext pc,IRCUIPlugin iup) throws GeneralException
	{
		this.context=pc;
		this.plugin=iup;
		pc.requestMessages(SystemStateMsg.class,this);
	}

	@Override
	public void removed()
	{
		if(window!=null) window.close();
	}

	/**
	 * System message: Used to automatically pull up the dialog on launch.
	 * @param msg Message
	 * @throws GeneralException
	 */
	public void msg(SystemStateMsg msg) throws GeneralException
	{
		if(msg.getType()==SystemStateMsg.UIREADY)
		{
			Preferences p=context.getSingle(Preferences.class);
			if(IRCUIPlugin.USEFAKESERVER || p.toBoolean(p.getGroup(plugin).get(PREFS_AUTOSHOW,"f")))
			{
				try
				{
					autoShow=true;
				  clicked();
				}
				finally
				{
					autoShow=false;
				}
			}
		}
	}

	@Override
	public String getLabel()
	{
		return "Connect";
	}

	@Override
	public String getThemeType()
	{
		return "connectButton";
	}

	@Override
	public int getDefaultPosition()
	{
		return 100;
	}

	@Override
	public void clicked() throws GeneralException
	{
		if(retrieveWindow(false))
		{
			Preferences p=context.getSingle(Preferences.class);
			if(IRCUIPlugin.USEFAKESERVER ||
				(autoShow && p.toBoolean(p.getGroup(plugin).get(PREFS_ANDCONNECT,"f"))
					&& connectUI.isEnabled())) actionConnect();
		}
	}

	/**
	 * Either creates a new window or makes sure the existing one is active.
	 * @param direct True if window is being opened for a direct connection
	 * @return True if the window is on its UI page; false if it's on the
	 *   'connecting' page and shouldn't be interrupted
	 * @throws GeneralException
	 */
	private boolean retrieveWindow(boolean direct) throws GeneralException
	{
		if(window == null)
		{
			UI u = context.getSingle(UI.class);
			window = u.createWindow("connect", this);
			initWindow(direct);
			window.show(false);
		}
		else
		{
			if(direct)
			{
				makeDirect();
			}
			window.activate();

			// Don't do anything if currently connecting
			if(!choiceUI.getDisplayed().equals("ui")) return false;
		}
		return true;
	}

	void directConnect(String host,int port) throws GeneralException
	{
		if(retrieveWindow(true))
		{
			directHost=host;
			directPort=port;
			directServer=null;
			actionConnect();
		}
	}

	void directConnect(PreferencesGroup server) throws GeneralException
	{
		if(retrieveWindow(true))
		{
			directHost=null;
			directPort=0;
			directServer=server;
			actionConnect();
		}
	}

	/**
	 * When a connect window already exists, turns it into 'direct' mode to use
	 * for a programmatic connection.
	 * @throws GeneralException
	 */
	private void makeDirect() throws GeneralException
	{
		this.direct=true;
		backUI.setVisible(false);
	}

	private void initWindow(boolean direct) throws GeneralException
	{
		this.direct=direct;
		window.setRemember("tool","connect");
		if(direct)
		{
			backUI.setVisible(false);
			return;
		}

		ps=new PrefsServerPage(context);
		TabPanel tp=(TabPanel)window.getWidget("tabs");
		tp.add(ps.getPage());

	  serverselectUI.setHandler(this);

		Preferences p=context.getSingle(Preferences.class);
		PreferencesGroup pg=p.getGroup(plugin).getChild(PREFS_LASTCONNECT);
		PreferencesGroup[] apgList=pg.getChild(PREFS_LIST).getAnon();
		if(apgList.length==0)
		{
			PreferencesGroup pgSingle=pg.getChild(PREFS_SINGLE);
			if(pgSingle.exists("host") && pgSingle.exists("port"))
			{
				String sHost=pgSingle.get("host");
				int iPort=p.toInt(pgSingle.get("port"));
				String server = sHost + (iPort==6667 ? "" : ":"+iPort);
				if(pgSingle.exists("channel"))
				{
					server = "irc://" + server + "/" + pgSingle.get("channel");
					if(pgSingle.exists("key"))
					{
						server += "?" + pgSingle.get("key");
					}
				}

				serverUI.setValue(server);
			}
		}
		else
		{
			List<TreeBox.Item> l = new LinkedList<TreeBox.Item>();
			findItems(l,apgList,(PrefsServerItem)getRoot());
			TreeBox.Item[] selected = l.toArray(new TreeBox.Item[l.size()]);
			serverselectUI.select(selected);
			selected(selected);
		}

		autoShowUI.setChecked(p.toBoolean(p.getGroup(plugin).get(PREFS_AUTOSHOW,"f")));
		andConnectUI.setEnabled(autoShowUI.isChecked());
		andConnectUI.setChecked(p.toBoolean(p.getGroup(plugin).get(PREFS_ANDCONNECT,"f")));

		// Currently-connected servers
		Connections c=context.getSingle(Connections.class);
		Server[] as=c.getConnected();
		for(int i=0;i<as.length;i++)
		{
			String sHost=as[i].getReportedHost();
			if(sHost==null) sHost=as[i].getConnectedHost();
			disconnectlistUI.addItem(sHost);
		}

		changeAddress();
	}

	private void findItems(List<TreeBox.Item> l, PreferencesGroup[] apgList,
		PrefsServerItem psi) throws GeneralException
	{
		if(psi.isServer())
		{
			for(int i=0;i<apgList.length;i++)
			{
				if(apgList[i].get("host","").equals(psi.getGroup().get("host")))
				{
					l.add(psi);
					break;
				}
			}
		}
		else if(psi.isNetwork())
		{
			for(int i=0;i<apgList.length;i++)
			{
				if(apgList[i].get("network","").equals(psi.getGroup().get("network")))
				{
					l.add(psi);
					break;
				}
			}
		}

		PrefsServerItem[] apsi=(PrefsServerItem[])psi.getChildren();
		for(int i=0;i<apsi.length;i++)
		{
			findItems(l,apgList,apsi[i]);
		}
	}

	/** Callback: Cancel button */
	@UIAction
	public void actionCancel()
	{
		cancelConnect();
		// This check shouldn't be necessary, but some users hit NPEs at this point
		if(window != null)
		{
			window.close();
		}
	}

	/** Callback: Window closed */
	@UIAction
	public void windowClosed()
	{
		window = null;
		autoShowUI = null;
		andConnectUI = null;
		serverselectUI = null;
		tabsUI = null;
		backUI = null;
		connectUI = null;
		serverUI = null;
		choiceUI = null;
		disconnectlistUI = null;
		disconnectUI = null;
	}

	private static class ServerInfo
	{
		ServerInfo()
		{
		}
		ServerInfo(PreferencesGroup pg)
		{
			this.pg=pg;
			host=pg.get(IRCPrefs.PREF_HOST);
			String portRange=pg.getAnonHierarchical(IRCPrefs.PREF_PORTRANGE,
				IRCPrefs.PREFDEFAULT_PORTRANGE);
			failureRun=pg.getPreferences().toInt(pg.get(IRCPrefs.PREF_FAILURES,"0"));
			try
			{
				int dash=portRange.indexOf('-');
				if(dash==-1)
				{
					portMin=Integer.parseInt(portRange);
					portMax=portMin;
				}
				else
				{
					portMin=Integer.parseInt(portRange.substring(0,dash));
					portMax=Integer.parseInt(portRange.substring(dash+1));
				}
				if(portMin>portMax)
				{
					portMax=portMin;
				}
			}
			catch(NumberFormatException e)
			{
				portMin=6667;
				portMax=6667;
			}
		}

		void markError()
		{
			if(pg!=null)
			{
				pg.set(IRCPrefs.PREF_FAILURES,pg.getPreferences().fromInt(failureRun+1));
			}
		}

		void markOK()
		{
			if(pg!=null)
			{
				pg.unset(IRCPrefs.PREF_FAILURES);
			}
		}

		/** Single server */
		String host=null;

		/** Port */
		int portMin,portMax;

		private PreferencesGroup pg;

		/**
		 * Failure run size (1 = last attempt was failure, 2 = last two attempts were
		 * failures, etc.)
		 */
		int failureRun=0;

		/** If we are supposed to try a sequence of servers in order, they go in here */
		ServerInfo[] oneOf=null;
	}

	private final static Pattern REGEX_IRCURL = Pattern.compile(
		"^irc://([a-z0-9-.]+)(?::([0-9]{1,5}))?(?:/(?:([#&+]?)([^?]+)(?:\\?(.+))?)?)?$",
		Pattern.CASE_INSENSITIVE);
	private final static Pattern REGEX_SERVERNAME = Pattern.compile(
		"^([a-z0-9-.]+)(?:[: ]([0-9]{1,5}))?$", Pattern.CASE_INSENSITIVE);

	/**
	 * Callback: Connect button.
	 * @throws GeneralException
	 */
	@UIAction
	public void actionConnect() throws GeneralException
	{
		List<ServerInfo> connectTo = new LinkedList<ServerInfo>();
		if(direct)
		{
			ServerInfo si=new ServerInfo();
			if(directHost!=null)
			{
				// Connect to specified server
				si.host=directHost;
				si.portMin=directPort;
				si.portMax=directPort;
				connectTo.add(si);
			}
			else
			{
				// Connect to server/net from preferences
				addSelectedServer(connectTo,null,new PrefsServerItem(directServer,null));
			}
		}
		else
		{
			Preferences p=context.getSingle(Preferences.class);
			PreferencesGroup pg=p.getGroup(plugin).getChild(PREFS_LASTCONNECT);
			PreferencesGroup pgList=pg.getChild(PREFS_LIST);
			pgList.clearAnon();

			if(selectedServers!=null && selectedServers.length>0 && !IRCUIPlugin.USEFAKESERVER)
			{
				for(int i=0;i<selectedServers.length;i++)
				{
					PrefsServerItem psi=(PrefsServerItem)selectedServers[i];
					addSelectedServer(connectTo,pgList,psi);
				}
			}
			else
			{
				// Set up server info with default port
				ServerInfo si = new ServerInfo();
				si.portMin = 6667;
				si.portMax = 6667;
				String channel = null;
				String key = null;

				// Check input
				String input = serverUI.getValue().trim();
				Matcher m = REGEX_IRCURL.matcher(input);
				if(IRCUIPlugin.USEFAKESERVER)
				{
					// Fake (local) server for debugging
					si.host="localhost";
				}
				else if(m.matches())
				{
					// IRC URL
					si.host = m.group(1);
					if(m.group(2) != null && m.group(2).length()>0)
					{
						si.portMin = Integer.parseInt(m.group(2));
						si.portMax = si.portMin;
					}

					if(m.group(4) != null && m.group(4).length()>0)
					{
						String prefix = "#";
						if(m.group(3) != null && m.group(3).length()>0)
						{
							prefix = m.group(3);
						}
						channel = prefix + m.group(4);
						key = m.group(5); // May be null
						if(key == null)
						{
							key = "";
						}
						plugin.addJoinRequest(si.host, channel, key);
					}
				}
				else
				{
					// Server name/port
					m = REGEX_SERVERNAME.matcher(input);
					if(!m.matches())
					{
						throw new GeneralException("Invalid input '" + input
							+ "' (was supposed to be detected earlier)");
					}
					si.host = m.group(1);

					if(m.group(2) != null && m.group(2).length()>0)
					{
						si.portMin = Integer.parseInt(m.group(2));
						si.portMax = si.portMin;
					}
				}

				PreferencesGroup pgSingle=pg.getChild(PREFS_SINGLE);
				pgSingle.set("host",si.host);
				pgSingle.set("port",""+si.portMin);
				if(channel != null)
				{
					pgSingle.set("channel", channel);
					if(key != null && key.length() > 0)
					{
						pgSingle.set("key", key);
					}
					else
					{
						pgSingle.unset("key");
					}
				}
				else
				{
					pgSingle.unset("channel");
					pgSingle.unset("key");
				}

				connectTo.add(si);
			}
			connectUI.setEnabled(false);
		}
		choiceUI.display("connecting");

		final ServerInfo[] connectArray = connectTo.toArray(new ServerInfo[connectTo.size()]);
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					connecting=true;
					connect(connectArray);
				}
				finally
				{
					connecting=false;
				}
			}
		}).start();
	}

	/**
	 * Adds a selected server item to the connectionlist
	 * @param connectTo
	 * @param pgList
	 * @param psi
	 */
	private void addSelectedServer(List<ServerInfo> connectTo,
		PreferencesGroup pgList, PrefsServerItem psi)
	{
		if(psi.isServer())
		{
			ServerInfo si=new ServerInfo(psi.getGroup());

			if(pgList!=null)
			{
				PreferencesGroup pgNew=pgList.addAnon();
				pgNew.set("host",si.host);
			}

			connectTo.add(si);
		}
		else if(psi.isNetwork())
		{
			if(pgList!=null)
			{
				PreferencesGroup pgNew=pgList.addAnon();
				pgNew.set("network",psi.getGroup().get(IRCPrefs.PREF_NETWORK));
			}

			ServerInfo si=new ServerInfo();
			si.oneOf=new ServerInfo[psi.getChildren().length];
			for(int child=0;child<si.oneOf.length;child++)
			{
				PrefsServerItem childItem=(PrefsServerItem)psi.getChildren()[child];
				si.oneOf[child]=new ServerInfo(childItem.getGroup());
			}

			// Shuffle list
			Arrays.sort(si.oneOf,new Comparator<ServerInfo>()
			{
				private Map<ServerInfo, Double> values = new HashMap<ServerInfo, Double>();
				private double getValue(ServerInfo o)
				{
					Double d = values.get(o);
					if(d==null)
					{
						// Make it less likely to pick the server by adding 0.33 to its
						// random number for each recent failure, up to a total 0.99 added,
						// which should make it pretty unlikely to come first.
						int failures=o.failureRun;
						failures=Math.min(failures,3);
						d=new Double(Math.random()+0.33*failures);
						values.put(o,d);
					}
					return d.doubleValue();
				}
				@Override
				public int compare(ServerInfo o1, ServerInfo o2)
				{
					return getValue(o1) > getValue(o2) ? 1 : -1;
				}
			});

			connectTo.add(si);
		}
	}


	private boolean connecting=false,cancelling=false;

	private void cancelConnect()
	{
		// A bit lame
		cancelling=true;
		while(connecting)
		{
			try
			{
				Thread.sleep(250);
			}
			catch(InterruptedException ie)
			{
			}
		}
		cancelling=false;
	}

	/**
	 * Callback: Back button.
	 * @throws GeneralException
	 */
	@UIAction
	public void actionBack() throws GeneralException
	{
		cancelConnect();

		choiceUI.display("ui");
		((Button)window.getWidget("connect")).setEnabled(true);
	}

	private void addTextInSwing(final String xml)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					if(window==null) return;
					TextView tv=(TextView)window.getWidget("log");
					tv.addXML(xml);
				}
				catch(GeneralException e)
				{
					ErrorMsg.report("Error adding text to connect log",e);
				}
			}
		});
	}

	private void connect(ServerInfo[] servers)
	{
		boolean anyErrors=false;
		outer: for(int i=0;i<servers.length;i++)
		{
			if(servers[i].oneOf==null)
			{
				ServerInfo thisServer=servers[i];
				int thisPort=(int)(Math.random()*(thisServer.portMax-thisServer.portMin+1))+thisServer.portMin;
				try
				{
					addTextInSwing("<connect><line>Connecting to <key>"+
						XML.esc(thisServer.host)+":"+thisPort+"</key>...</line></connect>");
					connect(thisServer.host,thisPort);
					if(cancelling) return;
					thisServer.markOK();
				}
				catch(GeneralException e)
				{
					anyErrors=true;
					thisServer.markError();
					addTextInSwing("<connect><indent><line>Connection failed! <error>"+
						XML.esc(e.getMessage())+"</error></line></indent></connect>");
				}
			}
			else
			{
				for(int j=0;j<servers[i].oneOf.length;j++)
				{
					ServerInfo thisServer=servers[i].oneOf[j];
					int thisPort=(int)(Math.random()*(thisServer.portMax-thisServer.portMin+1))+thisServer.portMin;
					try
					{
						addTextInSwing("<connect><line>Connecting to <key>"+
							XML.esc(thisServer.host)+":"+thisPort+"</key>...</line></connect>");
						connect(thisServer.host,thisPort);
						if(cancelling) return;
						thisServer.markOK();
						continue outer; // If we connected to one, all good! on to the next one
					}
					catch(GeneralException e)
					{
						thisServer.markError();
						addTextInSwing("<connect><indent><line>Connection failed! <error>"+
							XML.esc(e.getMessage())+"</error></line></indent></connect>");
					}
				}
				anyErrors=true; // Failed to connect to any of them...
			}
		}

		if(!anyErrors)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					window.close();
				}
			});
		}
	}

	private void connect(String sServer,int iPort) throws GeneralException
	{
		Server s=context.getSingle(Connections.class).newServer();
		synchronized(s)
		{
			s.beginConnect(sServer,iPort,new Server.ConnectionProgress()
			{
				@Override
				public void progress(String text)
				{
					addTextInSwing("<connect><indent><line>"+text+"</line></indent></connect>");
				}
			});
			while(true)
			{
				if(s.isConnected())
				{
					return;
				}
				if(s.getError()!=null)
				{
					throw s.getError();
				}
				try
				{
					s.wait();
				}
				catch(InterruptedException e)
				{
				}
			}
		}
	}

	/**
	 * Callback: Server selected for disconnect.
	 * @throws GeneralException
	 */
	@UIAction
	public void selectionDisconnect() throws GeneralException
	{
		disconnectUI.setEnabled(disconnectlistUI.getMultiSelected().length > 0);
	}

	/**
	 * Callback: Disconnect button.
	 * @throws GeneralException
	 */
	@UIAction
	public void actionDisconnect() throws GeneralException
	{
		String[] hosts = disconnectlistUI.getMultiSelected();
		if(hosts.length==0) return;

		Connections c = context.getSingle(Connections.class);
		Server[] connected = c.getConnected();

		for(int disconnect=0; disconnect<hosts.length; disconnect++)
		{
			// Find server
			for(int server=0; server<connected.length; server++)
			{
				String host = connected[server].getReportedHost();
				if(host==null)
				{
					host = connected[server].getConnectedHost();
				}
				if(host.equals(hosts[disconnect]))
				{
					connected[server].disconnectGracefully();
				}
			}
		}

		window.close();
	}

	/**
	 * Callback: Auto-show option changed.
	 * @throws GeneralException
	 */
	@UIAction
	public void changeAutoShow() throws GeneralException
	{
		Preferences p=context.getSingle(Preferences.class);
		p.getGroup(plugin).set(PREFS_AUTOSHOW,p.fromBoolean(autoShowUI.isChecked()));
		andConnectUI.setEnabled(autoShowUI.isChecked());
	}

	/**
	 * Callback: Auto-connect option changed.
	 * @throws GeneralException
	 */
	@UIAction
	public void changeAndConnect() throws GeneralException
	{
		Preferences p=context.getSingle(Preferences.class);
		p.getGroup(plugin).set(PREFS_ANDCONNECT,p.fromBoolean(andConnectUI.isChecked()));
	}

	@Override
	public void selected(Item[] ai)
	{
		selectedServers=ai;
		if(ai!=null && ai.length>0)
		{
			serverUI.setFlag(EditBox.FLAG_DIM);
		}
		else
		{
			serverUI.setFlag(EditBox.FLAG_NORMAL);
		}
		updateOK();
	}

	/**
	 * Callback: Address changed.
	 */
	@UIAction
	public void changeAddress()
	{
		// If user types in text to the address initially, that counts the same
		// as focusing in terms of making that be the default.
		if(serverUI.getValue().length()>0)
			focusAddress();
		updateOK();
	}

	private boolean checkServerValid()
	{
		boolean valid = REGEX_IRCURL.matcher(serverUI.getValue()).matches() ||
			REGEX_SERVERNAME.matcher(serverUI.getValue()).matches();
		serverUI.setFlag(valid ? EditBox.FLAG_NORMAL : EditBox.FLAG_ERROR);
		return valid;
	}

	private void updateOK()
	{
		// Enable connect button if a server is selected from list, or typed in box
		connectUI.setEnabled((selectedServers!=null && selectedServers.length>0) ||
			(serverUI.getFlag()!=EditBox.FLAG_DIM && checkServerValid()));
	}

	/**
	 * Callback: Address edit focused.
	 */
	@UIAction
	public void focusAddress()
	{
		// Deselect list so that address is used
		serverselectUI.select((Item)null);
		selected((Item[])null);
		checkServerValid();
	}

	@Override
	public Item getRoot()
	{
		return ps.getRoot();
	}

	@Override
	public boolean isRootDisplayed()
	{
		return false;
	}

	/**
	 * Callback: Tab changed.
	 */
	@UIAction
	public void changeTabs()
	{
		if("connectPage".equals(tabsUI.getDisplayed()))
			serverselectUI.update();
	}
}
