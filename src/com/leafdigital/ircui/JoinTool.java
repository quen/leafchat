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

import util.xml.XML;

import com.leafdigital.irc.api.*;
import com.leafdigital.prefs.api.*;
import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/** Implements the Join toolbar icon and dialog. */
@UIHandler("join")
public class JoinTool implements SimpleTool
{
	private static final String PATTERN_KEYWORD="[\\x21-\\x2b\\x2d-\\xff]*";
	private PluginContext context;

	/** UI: Button to join favourite channel(s) */
	public Button joinFavouritesUI;

	/** UI: Favourites table */
	public Table favouritesUI;

	/** Join window */
	private Window joinWindow = null;

	/**
	 * @param context Context
	 */
	public JoinTool(PluginContext context)
	{
		this.context=context;
	}

	@Override
	public void removed()
	{
		if(joinWindow!=null) joinWindow.close();
	}

	@Override
	public String getLabel()
	{
		return "Join";
	}

	@Override
	public String getThemeType()
	{
		return "joinButton";
	}

	@Override
	public int getDefaultPosition()
	{
		return 110;
	}

	@Override
	public void clicked() throws GeneralException
	{
		if(joinWindow == null)
		{
			UI u=context.getSingleton2(UI.class);
			joinWindow = u.createWindow("join", this);
			initWindow();
			joinWindow.show(false);
		}
		else
		{
			joinWindow.activate();
		}
	}

	static class ChannelInfo implements Comparable<ChannelInfo>
	{
		PreferencesGroup server,channel;
		String name,network,host,key;
		boolean autoJoin;

		@Override
		public int compareTo(ChannelInfo o)
		{
			ChannelInfo other=o;
			int i=name.compareTo(other.name);
			if(i!=0) return i;
			i=network.compareTo(other.network);
			if(i!=0) return i;
			i=host.compareTo(other.host);
			if(i!=0) return i;
			i=key.compareTo(other.key);
			if(i!=0) return i;
			return (autoJoin?1 : 0) - (other.autoJoin ? 1 : 0);
		}

		@Override
		public boolean equals(Object obj)
		{
			if(obj==null || !(obj instanceof ChannelInfo)) return false;
			ChannelInfo other=(ChannelInfo)obj;
			return other.name.equals(name) &&
				network.equals(other.network) &&
				host.equals(other.host) &&
			  other.key.equals(key) && other.autoJoin==autoJoin;
		}

		public ChannelInfo(Preferences p,PreferencesGroup server,PreferencesGroup channel)
		  throws GeneralException
		{
			this.server=server;
			this.channel=channel;
			name=channel.get(IRCPrefs.PREF_NAME);
			network=server.get(IRCPrefs.PREF_NETWORK,"");
			host=server.get(IRCPrefs.PREF_HOST,"");
			autoJoin=p.toBoolean(channel.get(IRCPrefs.PREF_AUTOJOIN));
			key=channel.get(IRCPrefs.PREF_KEY);
		}
	}

	private static void addChannels(Preferences p, PreferencesGroup pg,
		Set<ChannelInfo> channelSet) throws GeneralException
	{
		// Loop through all channels
		PreferencesGroup[] channels =
			pg.getChild(IRCPrefs.PREFGROUP_CHANNELS).getAnon();
		for(int i=0; i<channels.length; i++)
		{
			channelSet.add(new ChannelInfo(p, pg, channels[i]));
		}
	}

	static ChannelInfo[] getFavouriteChannels(PluginContext context) throws GeneralException
	{
		// Fill favourites table
		Preferences p=context.getSingleton2(Preferences.class);
		PreferencesGroup[] level1Prefs=p.getGroup(
			p.getPluginOwner("com.leafdigital.irc.IRCPlugin")).getChild(
				IRCPrefs.PREFGROUP_SERVERS).getAnon();
		TreeSet<ChannelInfo> channelSet = new TreeSet<ChannelInfo>();
		for(int level1=0;level1<level1Prefs.length;level1++)
		{
			// Got any channels?
			addChannels(p,level1Prefs[level1],channelSet);
			PreferencesGroup[] level2Prefs=level1Prefs[level1].getAnon();
			for(int level2=0;level2<level2Prefs.length;level2++)
			{
				addChannels(p,level2Prefs[level2],channelSet);
			}
		}

		return channelSet.toArray(new ChannelInfo[channelSet.size()]);
	}

	private ChannelInfo[] channels;

	private void initWindow() throws GeneralException
	{
		joinWindow.setRemember("tool","join");
		channels=getFavouriteChannels(context);

		if(channels.length>0)
		{
			((TabPanel)joinWindow.getWidget("tabs")).display("favouritesPage");
		}

		// Get table
		for(int i=0; i<channels.length; i++)
		{
			ChannelInfo ci = channels[i];
			int row = favouritesUI.addItem();
			favouritesUI.setString(row,0,ci.name);
			favouritesUI.setString(row,1,ci.network+ci.host);
			favouritesUI.setBoolean(row,2,ci.autoJoin);
			favouritesUI.setString(row,3,ci.key);
		}

		// Start listening for server changes
		context.requestMessages(ServerDisconnectedMsg.class,this,null,Msg.PRIORITY_NORMAL);
		context.requestMessages(ServerConnectionFinishedMsg.class,this,null,Msg.PRIORITY_NORMAL);

		// Update with current servers
		updateServers();
	}

	/**
	 * Callback: Window closed.
	 * @throws GeneralException
	 */
	@UIAction
	public void windowClosed() throws GeneralException
	{
		context.unrequestMessages(null,this,PluginContext.ALLREQUESTS);
		joinWindow = null;
		joinFavouritesUI = null;
		favouritesUI = null;
	}

	/**
	 * Message: Possible server connect/disconnect.
	 * @param msg Message
	 * @throws GeneralException
	 */
	public void msg(ServerMsg msg) throws GeneralException
	{
		updateServers();
	}

	/**
	 * Callback: Join button (first screen).
	 * @throws GeneralException
	 */
	@UIAction
	public void actionJoin() throws GeneralException
	{
		// Because this can get called by pressing return too
		if(!((Button)joinWindow.getWidget("join")).isEnabled()) return;

		Dropdown serverCombo=(Dropdown)joinWindow.getWidget("server");
		Server s=(Server)serverCombo.getSelected();

		String
			channel=((EditBox)joinWindow.getWidget("channel")).getValue(),
		  key=((EditBox)joinWindow.getWidget("keyword")).getValue();

		boolean
			rememberKeyword=((CheckBox)joinWindow.getWidget("savekeyword")).isChecked();

		// Don't save entry (at all) if they chose not to remember the keyword
		if(rememberKeyword || key.length()==0)
		{
			Preferences p=context.getSingleton2(Preferences.class);

			// Obtain prefs for server, or network if it belongs to one
			PreferencesGroup pg=s.getPreferences().getAnonParent();
			if(pg.get(IRCPrefs.PREF_NETWORK,null)==null)
			  pg=s.getPreferences();

			// Get channels group
			PreferencesGroup channels=pg.getChild(IRCPrefs.PREFGROUP_CHANNELS);
			PreferencesGroup[] existing=channels.getAnon();
			boolean found=false;
			for(int i=0;i<existing.length;i++)
			{
				String name=existing[i].get(IRCPrefs.PREF_NAME);
				if(name.equalsIgnoreCase(channel))
				{
					// OK already there, save key...
					existing[i].set(IRCPrefs.PREF_KEY,key);
					found=true;
					break;
				}
			}
			if(!found)
			{
				PreferencesGroup newChan=channels.addAnon();
				newChan.set(IRCPrefs.PREF_NAME,channel);
				newChan.set(IRCPrefs.PREF_KEY,key);
				newChan.set(IRCPrefs.PREF_AUTOJOIN,p.fromBoolean(false));
			}
		}

		// Send join command and close window
		sendJoin(s,channel,key);
		joinWindow.close();
	}

	private void sendJoin(Server s,String channel,String key)
	{
		s.sendLine(IRCMsg.constructBytes("JOIN "+channel+(key.length()>0 ? " "+key:"")));
	}

	/**
	 * Callback: Update button enable/disabled (first screen).
	 * @throws GeneralException
	 */
	@UIAction
	public void enableButtons() throws GeneralException
	{
		Dropdown serverCombo=(Dropdown)joinWindow.getWidget("server");

		String
			channel=((EditBox)joinWindow.getWidget("channel")).getValue(),
		  keyword=((EditBox)joinWindow.getWidget("keyword")).getValue();

		Server s=(Server)serverCombo.getSelected();

		((Button)joinWindow.getWidget("join")).setEnabled(
			s!=null
			&& channel.matches(".[\\x21-\\x2b\\x2d-\\xff]{1,199}") // Any character except controls, space, comma. Up to 200 letters long
			&& s.getChanTypes().indexOf(channel.charAt(0))!=-1
			&& keyword.matches(PATTERN_KEYWORD)
		);

		((CheckBox)joinWindow.getWidget("savekeyword")).setEnabled(
			keyword.length()!=0);
	}

	void updateServers() throws GeneralException
	{
		// Get connected servers
		Connections c=context.getSingleton2(Connections.class);
		Server[] servers=c.getConnected();

		// Get current value
		Dropdown serverCombo=(Dropdown)joinWindow.getWidget("server");
		Dropdown server2Combo=(Dropdown)joinWindow.getWidget("server2");
		Server
			before=(Server)serverCombo.getSelected(),
			before2=(Server)server2Combo.getSelected();

		// Update list
		serverCombo.clear();
		server2Combo.clear();
		for(int i=0;i<servers.length;i++)
		{
			if(!servers[i].isConnectionFinished()) return;
			serverCombo.addValue(servers[i],servers[i].getReportedOrConnectedHost());
			server2Combo.addValue(servers[i],servers[i].getReportedOrConnectedHost());
			if(servers[i]==before)
				serverCombo.setSelected(servers[i]);
			if(servers[i]==before2)
				server2Combo.setSelected(servers[i]);
		}

		// Hide/show it
		serverCombo.setVisible(servers.length>1);
		server2Combo.setVisible(servers.length>1);

		// Update favourites list
		for(int index=0; index<channels.length; index++)
		{
			ChannelInfo ci=channels[index];

			boolean match=false;
			for(int server=0;server<servers.length;server++)
			{
				PreferencesGroup pg=servers[server].getPreferences();
				if(pg==ci.server || pg.getAnonParent()==ci.server)
				{
					match=true;
					break;
				}
			}

			favouritesUI.setDim(index, 1, !match);
		}

		// Fix buttons
		enableButtons();
		server2Changed();
	}

	/**
	 * Callback: Table change.
	 * @param index Row
	 * @param col Column
	 * @param oBefore Previous value of data
	 * @throws GeneralException
	 */
	@UIAction
	public void favouritesChange(int index,int col,Object oBefore) throws GeneralException
	{
		Preferences p=context.getSingleton2(Preferences.class);
		ChannelInfo ci=channels[index];
		switch(col)
		{
		case 2: // Autojoin
			ci.autoJoin = favouritesUI.getBoolean(index, col);
			ci.channel.set(IRCPrefs.PREF_AUTOJOIN,p.fromBoolean(ci.autoJoin));
		  break;
		case 3: // Key
			ci.key = favouritesUI.getString(index, col);
			ci.channel.set(IRCPrefs.PREF_KEY,ci.key);
		  break;
	  default:
		  	assert(false);
		}
	}

	/**
	 * Callback: Table in process of editing.
	 * @param index Row
	 * @param col Column
	 * @param value Current value
	 * @param ec Editing control for reporting errors
	 * @throws GeneralException
	 */
	@UIAction
	public void favouritesEditing(int index,int col,String value,Table.EditingControl ec) throws GeneralException
	{
		switch(col)
		{
		case 3 : // Keyword
			// Check keyword matches permitted pattern
			if(!value.matches(PATTERN_KEYWORD))	ec.markError();
			break;
		default:
			assert(false);
		}
	}

	/**
	 * Callback: Change to select in favourites window.
	 * @throws GeneralException
	 */
	@UIAction
	public void favouritesSelect() throws GeneralException
	{
		int[] selected = favouritesUI.getSelectedIndices();
		((Button)joinWindow.getWidget("delete")).setEnabled(selected.length>0);
		joinFavouritesUI.setEnabled(selected.length>0);
	}

	/**
	 * Callback: Join selected favourites.
	 * @throws GeneralException
	 */
	@UIAction
	public void favouritesJoin() throws GeneralException
	{
		// Because this can get called by pressing return or double-clicking too
		if(!joinFavouritesUI.isEnabled())
		{
			return;
		}

		int[] selected = favouritesUI.getSelectedIndices();
		boolean ok = true;
		for(int i=0; i<selected.length; i++)
		{
			ok &= join(channels[selected[i]]);
		}
		if(ok)
		{
			joinWindow.close();
		}
	}

	/**
	 * Joins a channel on the first appropriate (matching host/network) connected
	 * server.
	 * @param ci Channel info
	 * @return True if it joined something, false if it didn't
	 * @throws GeneralException
	 */
	private boolean join(ChannelInfo ci) throws GeneralException
	{
		// Get connected servers
		Connections c=context.getSingleton2(Connections.class);
		Server[] servers=c.getConnected();

		// Find server for this channel
		for(int server=0;server<servers.length;server++)
		{
			PreferencesGroup pg=servers[server].getPreferences();
			if(pg==ci.server || pg.getAnonParent()==ci.server)
			{
				sendJoin(servers[server],ci.name,ci.key);
				return true;
			}
		}

		int confirm=context.getSingleton2(UI.class).showQuestion(joinWindow,
			"Confirm connect",
			"You are not connected to an appropriate server for channel <key>"+
			  XML.esc(ci.name)+"</key>. Connect to <key>"+(ci.network+ci.host)+
			  "</key> now?",UI.BUTTON_YES|UI.BUTTON_CANCEL,"Connect",null,null,
			  UI.BUTTON_YES);
		if(confirm==UI.BUTTON_YES)
		{
			((IRCUIPlugin)context.getPlugin()).connectAndJoin(ci.server,ci.name,ci.key);
			return true;
		}

		return false;
	}

	/**
	 * Callback: Delete favourite(s).
	 * @throws GeneralException
	 */
	@UIAction
	public void actionDelete() throws GeneralException
	{
		int[] deadRows = favouritesUI.getSelectedIndices();
		Arrays.sort(deadRows);
		LinkedList<ChannelInfo> channelList =
			new LinkedList<ChannelInfo>(Arrays.asList(channels));
		for(int i=deadRows.length-1;i>=0;i--)
		{
			favouritesUI.removeItem(deadRows[i]);
			ChannelInfo ci=channelList.get(deadRows[i]);
			ci.channel.remove();
			channelList.remove(deadRows[i]);
		}
		channels = channelList.toArray(new ChannelInfo[channelList.size()]);
	}

	/**
	 * Callback: Server dropdown changed.
	 * @throws GeneralException
	 */
	@UIAction
	public void server2Changed() throws GeneralException
	{
		ChoicePanel cp=(ChoicePanel)joinWindow.getWidget("listchoice");
		Dropdown server2=(Dropdown)joinWindow.getWidget("server2");

		if(server2.getSelected()==null)
			cp.display("noserver");
		else
		{
			Server s=(Server)server2.getSelected();

			String eList=s.getISupport("ELIST");
			if(eList!=null && eList.toUpperCase().indexOf('U')!=-1)
			{
				Table t=(Table)joinWindow.getWidget("searchresults");
				if(searchServer!=s)
					t.clear();
				cp.display("ok");
			}
			else
			{
				cp.display("nosupport");
			}
		}
	}

	private boolean searching=false;
	private int searchRequest;
	private Server searchServer;

	/**
	 * Callback: User changed min users for search.
	 * @throws GeneralException
	 */
	@UIAction
	public void changeMinUsers() throws GeneralException
	{
		EditBox minUsers=(EditBox)joinWindow.getWidget("minusers");
		boolean ok=minUsers.getValue().matches("[1-9][0-9]{0,2}");
		((Button)joinWindow.getWidget("search")).setEnabled(ok && !searching);
		minUsers.setFlag(ok ? EditBox.FLAG_NORMAL : EditBox.FLAG_ERROR);
	}

	/**
	 * Callback: Search button.
	 * @throws GeneralException
	 */
	@UIAction
	public void actionSearch() throws GeneralException
	{
		Button searchButton=(Button)joinWindow.getWidget("search");
		if(!searchButton.isEnabled()) return;

		searchButton.setEnabled(false);
		searching=true;
		Table t=(Table)joinWindow.getWidget("searchresults");
		t.clear();
		selectSearch();

		Server s=(Server)((Dropdown)joinWindow.getWidget("server2")).getSelected();
		searchServer=s;
		String minUsers=((EditBox)joinWindow.getWidget("minusers")).getValue();

		searchRequest=context.requestMessages(NumericIRCMsg.class,this,new ServerFilter(s),Msg.PRIORITY_EARLY);

		s.sendLine(IRCMsg.constructBytes("LIST >"+minUsers));
	}

	private final static String EATCOLOURS="(\\x03[0-9]{1,2}(,[0-9]{1,2})?)|[\\x00-\\x1f]";

	/**
	 * Message: Numeric (looking for channel list).
	 * @param msg Message
	 * @throws GeneralException
	 */
	public void msg(NumericIRCMsg msg) throws GeneralException
	{
		switch(msg.getNumeric())
		{
		case NumericIRCMsg.RPL_LISTSTART:
			msg.markHandled();
			break;
		case NumericIRCMsg.RPL_LISTEND:
			searching=false;
			context.unrequestMessages(null,this,searchRequest);
			changeMinUsers(); // Enable button again
			msg.markHandled();
			break;
		case NumericIRCMsg.RPL_LIST:
			if(msg.getParams().length==4)
			{
				String name=msg.getParamISO(1);
				if(!name.equals("*")) // DALnet puts out these, I dunno what it is, maybe a count of those not in a chan?
				{
					Table t=(Table)joinWindow.getWidget("searchresults");
					int index=t.addItem();
					t.setString(index,0,name);
					t.setString(index,1,msg.getParamISO(2));
					t.setString(index,2,msg.getParamISO(3).replaceAll(EATCOLOURS,""));
				}
				msg.markHandled();
			}
			break;
		default:
		}
	}

	/**
	 * Callback: Search result selected.
	 * @throws GeneralException
	 */
	@UIAction
	public void selectSearch() throws GeneralException
	{
		Table t=(Table)joinWindow.getWidget("searchresults");
		int[] selected=t.getSelectedIndices();
		((Button)joinWindow.getWidget("joinSearch")).setEnabled(selected.length>0);
	}

	/**
	 * Callback: Join button (search page).
	 * @throws GeneralException
	 */
	@UIAction
	public void actionJoinSearch() throws GeneralException
	{
		// Because this can get called by pressing return or double-clicking too
		if(!((Button)joinWindow.getWidget("joinSearch")).isEnabled()) return;

		Table t=(Table)joinWindow.getWidget("searchresults");
		int[] selected=t.getSelectedIndices();
		for(int i=0;i<selected.length;i++)
		{
			sendJoin(searchServer,t.getString(selected[i],0),"");
		}

		joinWindow.close();
	}
}
