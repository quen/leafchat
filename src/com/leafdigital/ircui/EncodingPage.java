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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import com.leafdigital.irc.api.*;
import com.leafdigital.prefs.api.*;
import com.leafdigital.ui.api.*;
import com.leafdigital.ui.api.TreeBox.Item;

import leafchat.core.api.PluginContext;

/**
 * Preferences page containing character encoding options.
 */
@UIHandler({"encoding", "addencodingoverride"})
public class EncodingPage implements TreeBox.SingleSelectionHandler
{
	private PluginContext context;
	private Page p;

	/** Dropdown: incoming encoding. */
	public Dropdown incomingUI;
	/** Dropdown: outgoing encoding. */
	public Dropdown outgoingUI;
	/** Checkbox: default to UTF-8. */
	public CheckBox utf8UI;
	/** Table: list of overrides. */
	public Table overridesUI;
	/** Button: Remove override. */
	public Button removeUI;
	/** Button: Edit override. */
	public Button editUI;

	private Dialog addDialog;

	/** Tab panel: tabs in Add dialog */
	public TabPanel addTabsUI;
	/** Button: Add button in dialog */
	public Button addAddUI;

	/** Editbox: Nickname for override. */
	public EditBox addNickUI;
	/** Editbox: Username for override. */
	public EditBox addUserUI;
	/** Editbox: Hostname for override. */
	public EditBox addHostUI;
	/** Editbox: Channel name for override. */
	public EditBox addChannelUI;

	/** Dropdown: Incoming encoding for override. */
	public Dropdown addIncomingUI;
	/** Dropdown: Outgoing encoding for override. */
	public Dropdown addOutgoingUI;
	/** Checkbox: UTF-8 default for override. */
	public CheckBox addUTF8UI;
	/** Treebox: Server for override */
	public TreeBox addServerTreeUI;

	private boolean addEditMode;
	private String addEditType,addEditName;
	private PreferencesGroup addEditGroup;

	EncodingPage(PluginContext pc)
	{
		this.context=pc;
		UI ui=pc.getSingle(UI.class);

		p = ui.createPage("encoding", this);
		fillDropdown(incomingUI,false);
		fillDropdown(outgoingUI,true);

		Preferences p=pc.getSingle(Preferences.class);
		PreferencesGroup encoding=getMainPrefs();
		incomingUI.setSelected(encoding.get(IRCPrefs.PREF_ENCODING,IRCPrefs.PREFDEFAULT_ENCODING));
	  outgoingUI.setSelected(encoding.get(IRCPrefs.PREF_OUTGOING,IRCPrefs.PREFDEFAULT_OUTGOING));
	  utf8UI.setChecked(p.toBoolean(encoding.get(IRCPrefs.PREF_UTF8,IRCPrefs.PREFDEFAULT_UTF8)));
	  fillOverrides();
	}

	private void fillDropdown(Dropdown d,boolean outgoing)
	{
		for(String charset : Charset.availableCharsets().keySet())
		{
			if(outgoing)
			{
				try
				{
					"frog".getBytes(charset);
				}
				catch(UnsupportedOperationException e)
				{
					continue;
				}
				catch(UnsupportedEncodingException e)
				{
					continue;
				}
			}
			d.addValue(charset,charset);
		}
	}

	Page getPage()
	{
		return p;
	}

	/**
	 * Action: User clicks add override button.
	 */
	@UIAction
	public void actionAdd()
	{
		openAddDialog(false);
	}

	private void openAddDialog(boolean edit)
	{
		UI ui=context.getSingle(UI.class);

		addDialog = ui.createDialog("addencodingoverride", this);
		addEditMode=edit;

		Preferences prefs=context.getSingle(Preferences.class);
		PreferencesGroup encoding=getMainPrefs();

		// Fill and set up encoding selection
		fillDropdown(addIncomingUI,false);
		fillDropdown(addOutgoingUI,true);

		// Fill server list
		PreferencesGroup pg=prefs.
			getGroup("Plugin_com.leafdigital.irc.IRCPlugin").getChild("servers");
		rootItem = new PrefsServerItem(pg, null, context);
		addServerTreeUI.setHandler(this);

		// Set up defaults or existing values
		if(edit)
		{
			int index=overridesUI.getSelectedIndex();
			String context=overridesUI.getString(index,COL_CONTEXT);
			addEditName=overridesUI.getString(index,COL_NAME);
			addEditType=context;
			if(context.equals(CONTEXT_CHANNEL))
			{
				PreferencesGroup[] chans=encoding.getChild(IRCPrefs.PREFGROUP_BYCHAN).getAnon();
				for(int i=0;i<chans.length;i++)
				{
					if(chans[i].get(IRCPrefs.PREF_BYCHAN_NAME).equals(addEditName))
					{
						addEditGroup=chans[i];
						addIncomingUI.setSelected(addEditGroup.get(IRCPrefs.PREF_ENCODING));
						addOutgoingUI.setSelected(addEditGroup.get(IRCPrefs.PREF_OUTGOING));
						addUTF8UI.setChecked(pg.getPreferences().toBoolean(addEditGroup.get(IRCPrefs.PREF_UTF8)));
						addTabsUI.display("addChannelPage");
						addChannelUI.setValue(addEditName);
						break;
					}
				}
			}
			else if(context.equals(CONTEXT_USER))
			{
				PreferencesGroup[] users=encoding.getChild(IRCPrefs.PREFGROUP_BYUSER).getAnon();
				for(int i=0;i<users.length;i++)
				{
					if(users[i].get(IRCPrefs.PREF_BYUSER_MASK).equals(addEditName))
					{
						addEditGroup=users[i];
						addIncomingUI.setSelected(addEditGroup.get(IRCPrefs.PREF_ENCODING));
						addOutgoingUI.setSelected(addEditGroup.get(IRCPrefs.PREF_OUTGOING));
						addUTF8UI.setChecked(pg.getPreferences().toBoolean(addEditGroup.get(IRCPrefs.PREF_UTF8)));
						addTabsUI.display("addUserPage");
						IRCUserAddress mask=new IRCUserAddress(addEditName,true);
						addNickUI.setValue(mask.getNick());
						addUserUI.setValue(mask.getUser());
						addHostUI.setValue(mask.getHost());
						break;
					}
				}
			}
			else
			{
				initEditFromServerOverride(
					prefs.getGroup("Plugin_com.leafdigital.irc.IRCPlugin").getChild("servers"),
					context.equals(CONTEXT_NETWORK),addEditName);
			}
			addAddUI.setLabel("Save changes");
		}
		else
		{
			addIncomingUI.setSelected(incomingUI.getSelected());
			addOutgoingUI.setSelected(outgoingUI.getSelected());
			addUTF8UI.setChecked(utf8UI.isChecked());
			selectedServer=null;
		}

		addDialog.show(p.getOwner());
	}

	private boolean isEditSame()
	{
		String
			incoming=(String)addIncomingUI.getSelected(),
			outgoing=(String)addOutgoingUI.getSelected(),
			utf8=addEditGroup.getPreferences().fromBoolean(addUTF8UI.isChecked());

		if(!(addEditGroup.get(IRCPrefs.PREF_ENCODING).equals(incoming) &&
			addEditGroup.get(IRCPrefs.PREF_OUTGOING).equals(outgoing) &&
			addEditGroup.get(IRCPrefs.PREF_UTF8).equals(utf8)))
			return false;

		if(addEditType.equals(CONTEXT_CHANNEL))
		{
			return
				addTabsUI.getDisplayed().equals("addChannelPage") &&
				addChannelUI.getValue().equals(addEditName);
		}
		else if(addEditType.equals(CONTEXT_USER))
		{
			IRCUserAddress mask=new IRCUserAddress(
				addNickUI.getValue(),addUserUI.getValue(),addHostUI.getValue());
			return
  			addTabsUI.getDisplayed().equals("addUserPage") &&
  			mask.toString().equals(addEditName);
		}
		else
		{
			boolean network=addEditGroup.get(IRCPrefs.PREF_HOST,null)==null;
			String selectedName=network ? addEditGroup.get(IRCPrefs.PREF_NETWORK) :
				addEditGroup.get(IRCPrefs.PREF_HOST);
			return
  			addTabsUI.getDisplayed().equals("addServerPage") &&
	  		selectedName.equals(addEditName);
		}
	}

	/**
	 * Action: User clicks edit override button.
	 */
	@UIAction
	public void actionEdit()
	{
		openAddDialog(true);
	}

	/**
	 * Action: User clicks remove override button.
	 */
	@UIAction
	public void actionRemove()
	{
		Preferences prefs=context.getSingle(Preferences.class);
		PreferencesGroup encoding=getMainPrefs();

		int index=overridesUI.getSelectedIndex();
		String context=overridesUI.getString(index,COL_CONTEXT);
		String name=overridesUI.getString(index,COL_NAME);

		if(context.equals(CONTEXT_CHANNEL))
		{
			PreferencesGroup[] chans=encoding.getChild(IRCPrefs.PREFGROUP_BYCHAN).getAnon();
			for(int i=0;i<chans.length;i++)
			{
				if(chans[i].get(IRCPrefs.PREF_BYCHAN_NAME).equals(name))
					chans[i].remove();
			}
		}
		else if(context.equals(CONTEXT_USER))
		{
			PreferencesGroup[] users=encoding.getChild(IRCPrefs.PREFGROUP_BYUSER).getAnon();
			for(int i=0;i<users.length;i++)
			{
				if(users[i].get(IRCPrefs.PREF_BYUSER_MASK).equals(name))
					users[i].remove();
			}
		}
		else
		{
			removeServerOverride(
				prefs.getGroup("Plugin_com.leafdigital.irc.IRCPlugin").getChild("servers"),
				context.equals(CONTEXT_NETWORK),name);
		}
		fillOverrides();
	}

	private void initEditFromServerOverride(PreferencesGroup pg,boolean network,String name)
	{
		// Check this server
		if(
			(network && name.equals(pg.get(IRCPrefs.PREF_NETWORK,null))) ||
			(!network && name.equals(pg.get(IRCPrefs.PREF_HOST,null)))
			)
		{
			addIncomingUI.setSelected(pg.get(IRCPrefs.PREF_ENCODING));
			addOutgoingUI.setSelected(pg.get(IRCPrefs.PREF_OUTGOING));
			addUTF8UI.setChecked(pg.getPreferences().toBoolean(pg.get(IRCPrefs.PREF_UTF8)));
			addServerTreeUI.select(rootItem.find(pg));
			addTabsUI.display("addServerPage");
			addEditGroup=pg;
			return;
		}

		// Check all child servers
		PreferencesGroup[] children=pg.getAnon();
		for(int i=0;i<children.length;i++)
		{
			initEditFromServerOverride(children[i],network,name);
		}
	}

	private void removeServerOverride(PreferencesGroup pg,boolean network,String name)
	{
		// Check this server
		if(
			(network && name.equals(pg.get(IRCPrefs.PREF_NETWORK,null))) ||
			(!network && name.equals(pg.get(IRCPrefs.PREF_HOST,null)))
			)
		{
			pg.unset(IRCPrefs.PREF_ENCODING);
			pg.unset(IRCPrefs.PREF_UTF8);
			pg.unset(IRCPrefs.PREF_OUTGOING);
		}

		// Check all child servers
		PreferencesGroup[] children=pg.getAnon();
		for(int i=0;i<children.length;i++)
		{
			removeServerOverride(children[i],network,name);
		}
	}

	/**
	 * Action: User selects an override from table.
	 */
	@UIAction
	public void selectOverrides()
	{
		boolean gotSomething=overridesUI.getSelectedIndex()!=Table.NONE;
		removeUI.setEnabled(gotSomething);
		editUI.setEnabled(gotSomething);
	}

	/**
	 * Action: Add dialog losed.
	 */
	@UIAction
	public void closedAdd()
	{
		addDialog=null;
	}

	/**
	 * Action: focus nickname field.
	 */
	@UIAction
	public void focusAddNick()
	{
		if(addNickUI.getValue().equals("*")) addNickUI.selectAll();
	}

	/**
	 * Action: focus username field.
	 */
	@UIAction
	public void focusAddUser()
	{
		if(addUserUI.getValue().equals("*")) addUserUI.selectAll();
	}

	/**
	 * Action: focus hostname field.
	 */
	@UIAction
	public void focusAddHost()
	{
		if(addHostUI.getValue().equals("*")) addHostUI.selectAll();
	}

	private final static String SAFECHARACTERS="[^!@ ]+";

	/** Check whether add button should now be enabled */
	@UIAction
	public void changeAdd()
	{
		String selectedTab=addTabsUI.getDisplayed();
		boolean canAdd=false;
		if(selectedTab.equals("addServerPage"))
		{
			canAdd=selectedServer!=null;
		}
		else if(selectedTab.equals("addChannelPage"))
		{
			String value=addChannelUI.getValue();
			canAdd=value.matches("[^A-Za-z0-9].+");
			addChannelUI.setFlag((canAdd || value.equals(""))?EditBox.FLAG_NORMAL : EditBox.FLAG_ERROR);
		}
		else if(selectedTab.equals("addUserPage"))
		{
			canAdd=true;
			if(addNickUI.getValue().matches(SAFECHARACTERS) )
			{
				addNickUI.setFlag(EditBox.FLAG_NORMAL);
			}
			else
			{
				addNickUI.setFlag(EditBox.FLAG_ERROR);
				canAdd=false;
			}
			if(addUserUI.getValue().matches(SAFECHARACTERS))
			{
				addUserUI.setFlag(EditBox.FLAG_NORMAL);
			}
			else
			{
				addUserUI.setFlag(EditBox.FLAG_ERROR);
				canAdd=false;
			}
			if(addHostUI.getValue().matches(SAFECHARACTERS))
			{
				addHostUI.setFlag(EditBox.FLAG_NORMAL);
			}
			else
			{
				addHostUI.setFlag(EditBox.FLAG_ERROR);
				canAdd=false;
			}
			if(addNickUI.getValue().equals("*") &&
				addUserUI.getValue().equals("*") &&
				addHostUI.getValue().equals("*"))
			{
				canAdd=false;
			}
		}
		if(addEditMode && canAdd)	canAdd=!isEditSame();
		addAddUI.setEnabled(canAdd);
	}

	/**
	 * Action: Cancel add dialog.
	 */
	@UIAction
	public void actionAddCancel()
	{
		addDialog.close();
	}

	/**
	 * Action: Confirm add (add button in dialog).
	 */
	@UIAction
	public void actionAddAdd()
	{
		// Get rid of old one if editing
		if(addEditMode) actionRemove();

		// Prepare preferences
		Preferences p=context.getSingle(Preferences.class);
		PreferencesGroup encoding=getMainPrefs();

		// Get encoding details
		String
			incoming=(String)addIncomingUI.getSelected(),
			outgoing=(String)addOutgoingUI.getSelected();
		String utf8=p.fromBoolean(addUTF8UI.isChecked());
		String selectedTab=addTabsUI.getDisplayed();

		if(selectedTab.equals("addServerPage"))
		{
			selectedServer.set(IRCPrefs.PREF_ENCODING,incoming);
			selectedServer.set(IRCPrefs.PREF_OUTGOING,outgoing);
			selectedServer.set(IRCPrefs.PREF_UTF8,utf8);
		}
		else if(selectedTab.equals("addChannelPage"))
		{
			PreferencesGroup pg=encoding.getChild(IRCPrefs.PREFGROUP_BYCHAN).addAnon();
			pg.set(IRCPrefs.PREF_BYCHAN_NAME,addChannelUI.getValue());
			pg.set(IRCPrefs.PREF_BYCHAN_ENCODING,incoming);
			pg.set(IRCPrefs.PREF_BYCHAN_OUTGOING,outgoing);
			pg.set(IRCPrefs.PREF_BYCHAN_UTF8,utf8);
		}
		else if(selectedTab.equals("addUserPage"))
		{
			PreferencesGroup pg=encoding.getChild(IRCPrefs.PREFGROUP_BYUSER).addAnon();
			pg.set(IRCPrefs.PREF_BYUSER_MASK,addNickUI.getValue()+"!"+addUserUI.getValue()+"@"+addHostUI.getValue());
			pg.set(IRCPrefs.PREF_BYUSER_ENCODING,incoming);
			pg.set(IRCPrefs.PREF_BYUSER_OUTGOING,outgoing);
			pg.set(IRCPrefs.PREF_BYUSER_UTF8,utf8);
		}

		addDialog.close();
		fillOverrides();
	}

	private final static int COL_CONTEXT=0,COL_NAME=1,COL_RECEIVED=2,COL_UTF8=3,COL_SENT=4;
	private final static String CONTEXT_NETWORK="Net",CONTEXT_SERVER="Server",CONTEXT_CHANNEL="Chan",CONTEXT_USER="User";

	private void fillServerOverrides(PreferencesGroup pg)
	{
		Preferences p=context.getSingle(Preferences.class);

		// Check this server
		String serverEncoding=pg.get(IRCPrefs.PREF_ENCODING,null);
		if(serverEncoding!=null)
		{
			int index=overridesUI.addItem();
			boolean network=pg.get(IRCPrefs.PREF_HOST,null)==null;
			overridesUI.setString(index,COL_CONTEXT,network ? CONTEXT_NETWORK : CONTEXT_SERVER);
			overridesUI.setString(index,COL_NAME,network ? pg.get(IRCPrefs.PREF_NETWORK) : pg.get(IRCPrefs.PREF_HOST));
			overridesUI.setString(index,COL_RECEIVED,serverEncoding);
			overridesUI.setBoolean(index,COL_UTF8,p.toBoolean(pg.get(IRCPrefs.PREF_UTF8)));
			overridesUI.setString(index,COL_SENT,pg.get(IRCPrefs.PREF_OUTGOING));
		}

		// Check all child servers
		PreferencesGroup[] children=pg.getAnon();
		for(int i=0;i<children.length;i++)
		{
			fillServerOverrides(children[i]);
		}
	}

	/** Update the overrides table */
	private void fillOverrides()
	{
		Preferences p=context.getSingle(Preferences.class);
		PreferencesGroup encoding=getMainPrefs();

		overridesUI.clear();
		fillServerOverrides(p.getGroup("Plugin_com.leafdigital.irc.IRCPlugin").getChild("servers"));

		PreferencesGroup[] users=encoding.getChild(IRCPrefs.PREFGROUP_BYUSER).getAnon();
		for(int i=0;i<users.length;i++)
		{
			int index=overridesUI.addItem();
			overridesUI.setString(index,COL_CONTEXT,CONTEXT_USER);
			overridesUI.setString(index,COL_NAME,users[i].get(IRCPrefs.PREF_BYUSER_MASK));
			overridesUI.setString(index,COL_RECEIVED,users[i].get(IRCPrefs.PREF_BYUSER_ENCODING));
			overridesUI.setBoolean(index,COL_UTF8,p.toBoolean(users[i].get(IRCPrefs.PREF_BYUSER_UTF8)));
			overridesUI.setString(index,COL_SENT,users[i].get(IRCPrefs.PREF_BYUSER_OUTGOING));
		}
		PreferencesGroup[] chans=encoding.getChild(IRCPrefs.PREFGROUP_BYCHAN).getAnon();
		for(int i=0;i<chans.length;i++)
		{
			int index=overridesUI.addItem();
			overridesUI.setString(index,COL_CONTEXT,CONTEXT_CHANNEL);
			overridesUI.setString(index,COL_NAME,chans[i].get(IRCPrefs.PREF_BYCHAN_NAME));
			overridesUI.setString(index,COL_RECEIVED,chans[i].get(IRCPrefs.PREF_BYCHAN_ENCODING));
			overridesUI.setBoolean(index,COL_UTF8,p.toBoolean(chans[i].get(IRCPrefs.PREF_BYCHAN_UTF8)));
			overridesUI.setString(index,COL_SENT,chans[i].get(IRCPrefs.PREF_BYCHAN_OUTGOING));
		}
	}

	private PreferencesGroup getMainPrefs()
	{
		Preferences p=context.getSingle(Preferences.class);
		return p.getGroup(
			p.getPluginOwner("com.leafdigital.irc.IRCPlugin")).
			getChild(IRCPrefs.PREFGROUP_ENCODING);
	}

	/**
	 * Action: Change incoming encoding dropdown.
	 */
	@UIAction
	public void selectionChangeIncoming()
	{
		getMainPrefs().set(IRCPrefs.PREF_ENCODING,
			(String)incomingUI.getSelected(),IRCPrefs.PREFDEFAULT_ENCODING);
	}

	/**
	 * Action: Change default UTF-8 dropdown.
	 */
	@UIAction
	public void changeUTF8()
	{
		Preferences p=context.getSingle(Preferences.class);
		getMainPrefs().set(IRCPrefs.PREF_UTF8,
			p.fromBoolean(utf8UI.isChecked()),IRCPrefs.PREFDEFAULT_UTF8);
	}

	/**
	 * Action: Change outgoing encoding dropdown.
	 */
	@UIAction
	public void selectionChangeOutgoing()
	{
		getMainPrefs().set(IRCPrefs.PREF_OUTGOING,
			(String)outgoingUI.getSelected(),IRCPrefs.PREFDEFAULT_OUTGOING);
	}

	PreferencesGroup selectedServer;

	@Override
	public void selected(Item i)
	{
		if(i==null)
			selectedServer=null;
		else
			selectedServer=((PrefsServerItem)i).getGroup();
		changeAdd();
	}

	private PrefsServerItem rootItem;

	@Override
	public Item getRoot()
	{
		return rootItem;
	}

	@Override
	public boolean isRootDisplayed()
	{
		return false;
	}
}
