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

import java.util.HashSet;

import com.leafdigital.irc.api.IRCPrefs;
import com.leafdigital.prefs.api.*;
import com.leafdigital.ui.api.*;
import com.leafdigital.ui.api.TreeBox.Item;

import leafchat.core.api.*;

/**
 * Server preferences page.
 */
@UIHandler({"prefs-server", "addserver"})
public class PrefsServerPage implements TreeBox.DragSingleSelectionHandler,IRCPrefs
{
	private Page pThis;

	/** UI: Server selector tree view. */
	public TreeBox servertreeUI;

	/** UI: Add server. */
	public Button addUI;

	/** UI: Delete server. */
	public Button deleteUI;

	/** UI: Label warning about global password setting. */
	public Label passwordGlobalWarningUI;

	/** UI: Auto-identify checkbox. */
	public CheckBox autoIdentifyUI;

	/** UI: Identify command. */
	public EditBox identifyCommandUI;

	/**
	 * UI: Server password.
	 */
	public EditBox passwordUI;
	/**
	 * UI: Port range.
	 */
	public EditBox portRangeUI;

	/** UI: Security option. */
	public Dropdown securityUI;
	/** UI: Block containing security option */
	public BorderPanel securitySectionUI;

	private Dialog addDialog;
	/** UI: Add network: Name */
	public EditBox addNetworkNameUI;
	/** UI: Add network: Suffix */
	public EditBox addNetworkSuffixUI;
	/** UI: Add server: Address */
	public EditBox addServerAddressUI;
	/** UI: Add: Add button */
	public Button addAddUI;
	private boolean networkNameChanged=false;
	/** UI: Add: Tabs */
	public TabPanel addTabsUI;

	private PluginContext context;

	PrefsServerPage(PluginContext pc) throws GeneralException
	{
		UI u=pc.getSingle(UI.class);
		pThis = u.createPage("prefs-server", this);

		context=pc;

		Preferences p=pc.getSingle(Preferences.class);
		PreferencesGroup pg=p.getGroup(p.getPluginOwner(IRCPrefs.IRCPLUGIN_CLASS)).getChild("servers");
		piRoot=new PrefsServerItem(pg,null);

		verifyPrefs(pg,null);

		// Fill security options
		securityUI.addValue(PREF_SECUREMODE_NONE, "Unencrypted connection");
		securityUI.addValue(PREF_SECUREMODE_OPTIONAL, "Auto-detect");
		securityUI.addValue(PREF_SECUREMODE_REQUIRED, "Secure SSL connection");

		servertreeUI.setHandler(this);
		servertreeUI.select(getRoot());
		selected(getRoot());
	}

	/**
	 * Makes sure that preferences are valid. If you have no nicknames in root,
	 * it makes one and sets it default; if you have any DEFAULTNICK settings
	 * that don't match a NICK setting, these are removed.
	 * @param pg Group to start with
	 * @param parentNicks Nicks that are already present in parent groups
	 */
	private void verifyPrefs(PreferencesGroup pg, HashSet<String> parentNicks)
	{
		// Get list of nicks valid within this group
		HashSet<String> hereNicks =
			parentNicks != null
			? new HashSet<String>(parentNicks)
			: new HashSet<String>();
		PreferencesGroup[] apgNicks=pg.getChild(PREFGROUP_NICKS).getAnon();
		for(int iNick=0;iNick<apgNicks.length;iNick++)
		{
			hereNicks.add(apgNicks[iNick].get(PREF_NICK));
		}

		// If this group's default nick exists and isn't equal to one of the nicks,
		// get rid of it. (This should only really be necessary for those upgrading
		// from pre-alpha 5 as it shouldn't be possible from now on.)
		String defaultNick=pg.get(PREF_DEFAULTNICK,null);
		if(defaultNick!=null && !hereNicks.contains(defaultNick))
		{
		  pg.unset(PREF_DEFAULTNICK);
		  defaultNick=null;
		}

		// Special handling for root
		if(parentNicks==null)
		{
			// If root doesn't have any nicknames, add a random one
			if(hereNicks.isEmpty())
			{
				String nick="lc"+(Math.random()+"").replaceAll("[^0-9]","");
				if(nick.length()>9) nick=nick.substring(0,9);
				PreferencesGroup newNick=pg.getChild(PREFGROUP_NICKS).addAnon();
				newNick.set(PREF_NICK,nick);
				hereNicks.add(nick);
			}
			if(defaultNick==null)
			{
				defaultNick=hereNicks.iterator().next();
				pg.set(PREF_DEFAULTNICK,defaultNick);
			}
			// To help those updating from pre-Alpha 5, clear any passwords set at
			// root level as these can no longer be set
			for(int i=0;i<apgNicks.length;i++)
			{
				apgNicks[i].unset(PREF_PASSWORD);
			}
		}

		// Recurse to children
		PreferencesGroup[] apgChildren=pg.getAnon();
		for(int iChild=0;iChild<apgChildren.length;iChild++)
		{
			verifyPrefs(apgChildren[iChild],hereNicks);
		}
	}

	Page getPage() { return pThis; }

	PrefsServerItem piRoot;

	@Override
	public Item getRoot()
	{
		return piRoot;
	}

	@Override
	public boolean isRootDisplayed()
	{
		return true;
	}

	private PreferencesGroup selectedGroup;
	private PrefsServerItem selectedItem;

	private final static int COLUMN_NICK=0,COLUMN_PASSWORD=1,COLUMN_DEFAULT=2;

	@Override
	public void selected(Item i)
	{
		try
		{
			PrefsServerItem pi=(PrefsServerItem)i;
			selectedGroup=pi==null ? null : pi.getGroup();
			selectedItem=pi;

			Table tNick=(Table)pThis.getWidget("nicknames");
			EditBox ebUser=(EditBox)pThis.getWidget("serveruser");
			EditBox ebRealName=(EditBox)pThis.getWidget("serverrealname");
			EditBox ebQuitMessage=(EditBox)pThis.getWidget("serverquitmessage");

			// Clear nick table
			while(tNick.getNumItems()>0)
			{
				tNick.removeItem(0);
			}

			deleteUI.setEnabled(selectedGroup!=null && selectedGroup!=piRoot.getGroup());
			addUI.setEnabled(selectedGroup!=null);

			if(selectedGroup==null)
			{
				ebUser.setValue("");
				ebUser.setEnabled(false);
				ebRealName.setValue("");
				ebRealName.setEnabled(false);
				ebQuitMessage.setValue("");
				ebQuitMessage.setEnabled(false);
				identifyCommandUI.setValue("");
				identifyCommandUI.setEnabled(false);
				autoIdentifyUI.setEnabled(false);
				passwordUI.setValue("");
				passwordUI.setEnabled(false);
				portRangeUI.setValue("");
				portRangeUI.setEnabled(false);
				securitySectionUI.setVisible(false);
			}
			else
			{
				fillNickTable(tNick,selectedGroup,true);
				passwordGlobalWarningUI.setVisible(i==piRoot);

				ebUser.setValue(
					selectedGroup.getAnonHierarchical(PREF_USER,PREFDEFAULT_USER)
					);
				ebUser.setEnabled(true);
				changeUser();

				ebRealName.setValue(
					selectedGroup.getAnonHierarchical(PREF_REALNAME,PREFDEFAULT_REALNAME)
					);
				ebRealName.setEnabled(true);
				changeRealName();

				ebQuitMessage.setValue(
					selectedGroup.getAnonHierarchical(PREF_QUITMESSAGE,PREFDEFAULT_QUITMESSAGE)
					);
				ebQuitMessage.setEnabled(true);
				changeQuitMessage();

				identifyCommandUI.setValue(
					selectedGroup.getAnonHierarchical(PREF_IDENTIFYPATTERN,PREFDEFAULT_IDENTIFYPATTERN)
					);
				identifyCommandUI.setEnabled(true);
				changeIdentifyCommand();

				autoIdentifyUI.setChecked(
					selectedGroup.getAnonHierarchical(PREF_AUTOIDENTIFY,"y").equals("y"));
				autoIdentifyUI.setEnabled(true);
				changeAutoIdentify();

				passwordUI.setValue(
					selectedGroup.getAnonHierarchical(PREF_SERVERPASSWORD,""));
				passwordUI.setEnabled(true);
				changePassword();

				portRangeUI.setValue(
					selectedGroup.getAnonHierarchical(PREF_PORTRANGE,""));
				portRangeUI.setEnabled(true);
				changePortRange();

				if(pi.isServer())
				{
					securitySectionUI.setVisible(true);
					String secureMode = selectedGroup.get(PREF_SECUREMODE, PREFDEFAULT_SECUREMODE);
					securityUI.setSelected(secureMode);
				}
				else
				{
					securitySectionUI.setVisible(false);
				}
			}
		}
		catch(GeneralException ge)
		{
			throw new Error("bugger",ge);
		}
	}

	private void fillNickTable(Table t,PreferencesGroup pg,boolean bCurrent)
	{
		// Do parent first
		PreferencesGroup pgParent=pg.getAnonParent();
		if(pgParent!=null) 	fillNickTable(t,pgParent,false);

		// Add entries from this group
		PreferencesGroup[] apgNicks=pg.getChild(PREFGROUP_NICKS).getAnon();

		entryloop: for(int iEntry=0;iEntry<apgNicks.length;iEntry++)
		{
			String
				sNick=apgNicks[iEntry].get(PREF_NICK),
				sPassword=apgNicks[iEntry].get(PREF_PASSWORD,"");

			// See if row already exists
			for(int iRow=0;iRow<t.getNumItems();iRow++)
			{
				if(t.getString(iRow,COLUMN_NICK).equals(sNick))
				{
					// Just set the password
					t.setString(iRow,COLUMN_PASSWORD,sPassword);
					if(bCurrent) t.setDim(iRow,COLUMN_PASSWORD,false);

					// Bail to next entry
					continue entryloop;
				}
			}

			// OK, need new row
			int iNewRow=t.addItem();
			t.setString(iNewRow,COLUMN_NICK,sNick);
			t.setString(iNewRow,COLUMN_PASSWORD,sPassword);
			t.setEditable(iNewRow,COLUMN_PASSWORD,selectedItem!=piRoot);
			if(!bCurrent)
			{
				t.setEditable(iNewRow,COLUMN_NICK,false);
				t.setDim(iNewRow,COLUMN_PASSWORD,true);
			}
		}

		// If this is the current group, set the default column too
		if(bCurrent)
		{
			// Get hierarchical default
			String sDefault=pg.getAnonHierarchical(PREF_DEFAULTNICK,null);

			// Find default and set it
			boolean bFound=false;
			for(int iRow=0;iRow<t.getNumItems();iRow++)
			{
				if(t.getString(iRow,COLUMN_NICK).equals(sDefault))
				{
					t.setBoolean(iRow,COLUMN_DEFAULT,true);
					bFound=true;
				}
			}

			// If no default, use the first one
			if(!bFound && t.getNumItems()>0) t.setBoolean(0,COLUMN_DEFAULT,true);

			// Add extra row for people to type new ones
			addNickTableRow();
		}
	}

	/**
	 * Action: Nickname selected.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void nicknamesSelect() throws GeneralException
	{
		Table t=(Table)pThis.getWidget("nicknames");
		Button bDelete=(Button)pThis.getWidget("nicknamesdelete");
		int iRow=t.getSelectedIndex();
		if(iRow==Table.NONE)
		{
			bDelete.setEnabled(false);
		}
		else
		{
			bDelete.setEnabled(
				t.isEditable(iRow,COLUMN_NICK) && !t.isOverwrite(iRow,COLUMN_NICK) &&
				t.getNumItems()>2); // 2 because of the 'add new nick' thing
		}
	}

	/**
	 * Action: Nickname deleted.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void nicknamesDelete() throws GeneralException
	{
		// Get table
		Table t=(Table)pThis.getWidget("nicknames");
		int iIndex=t.getSelectedIndex();

		// Delete from prefs
		deleteNick(selectedGroup,t.getString(iIndex,COLUMN_NICK));

		// Delete from table
		if(t.getBoolean(iIndex,COLUMN_DEFAULT))
		{
			// Something else is default now, set it
			String defaultNick=selectedGroup.getAnonHierarchical(PREF_DEFAULTNICK);
			for(int i=0;i<t.getNumItems();i++)
			{
				if(t.getString(i,COLUMN_NICK).equals(defaultNick))
				{
					t.setBoolean(i,COLUMN_DEFAULT,true);
					break;
				}
			}
		}
		// Remove item from table
		t.removeItem(iIndex);
	}

	private void deleteNick(PreferencesGroup pg,String sNick)
	{
		PreferencesGroup[] apgNicks=pg.getChild(PREFGROUP_NICKS).getAnon();

		// Delete in this group if present
		String firstOtherNick=null;
		for(int iNick=0;iNick<apgNicks.length;iNick++)
		{
			if(apgNicks[iNick].get(PREF_NICK).equals(sNick))
				apgNicks[iNick].remove();
			else if(firstOtherNick==null)
				firstOtherNick=apgNicks[iNick].get(PREF_NICK);
		}
		String defaultNick=pg.get(PREF_DEFAULTNICK,null);
		if(defaultNick!=null && defaultNick.equals(sNick))
		{
			if(pg.getAnonParent()==null)
			{
				// Root must have default nick
				pg.set(PREF_DEFAULTNICK,firstOtherNick);
			}
			else
			{
				// Use parent's default nick
				pg.unset(PREF_DEFAULTNICK);
			}
		}


		// Delete from all child groups
		PreferencesGroup[] apgChildren=pg.getAnon();
		for(int iChild=0;iChild<apgChildren.length;iChild++)
		{
			deleteNick(apgChildren[iChild],sNick);
		}
	}

	/**
	 * Action: Nickname option changed.
	 * @param index Row
	 * @param col Column
	 * @param before Previous value
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void nicknamesChange(int index,int col,Object before) throws GeneralException
	{
		Table t=(Table)pThis.getWidget("nicknames");
		PreferencesGroup pgParent=selectedGroup.getAnonParent();
		switch(col)
		{
		case COLUMN_NICK: // Nickname
		{
			if(index==t.getNumItems()-1) // Last row = adding item
			{
				// Store in prefs
				PreferencesGroup pgNew=selectedGroup.getChild(PREFGROUP_NICKS).addAnon();
				pgNew.set(PREF_NICK,t.getString(index,col));

				// Update this row to make it 'normal'
				t.setOverwrite(index,COLUMN_NICK,false);
				t.setEditable(index,COLUMN_PASSWORD,pgParent!=null);
				t.setEditable(index,COLUMN_DEFAULT,true);

				// Add new row for adding more nicks
				addNickTableRow();
				nicknamesSelect(); // Selected row might be deletable
			}
			else // Changing existing nick
			{
				changeNick(selectedGroup,(String)before,t.getString(index,col));
			}
		} break;
		case COLUMN_PASSWORD: // Password
		{
			PreferencesGroup[] apgNicks=selectedGroup.getChild(PREFGROUP_NICKS).getAnon();
			String sNick=t.getString(index,COLUMN_NICK),sPassword=t.getString(index,COLUMN_PASSWORD);

			boolean bSameAsParent=pgParent!=null && sPassword.equals(getNickPassword(pgParent,sNick));

			// Edit in this group if we've got it
			boolean bFound=false;
			for(int iNick=0;iNick<apgNicks.length;iNick++)
			{
				if(apgNicks[iNick].get(PREF_NICK).equals(sNick))
				{
					bFound=true;
					// Does that password match one from a parent? If so, don't store it
					// separately. Also delete if it's blank now
					if(bSameAsParent)
					{
						apgNicks[iNick].remove();
					}
					else if(pgParent==null && sPassword.equals(""))
					{
						apgNicks[iNick].unset(PREF_PASSWORD);
					}
					else
					{
						apgNicks[iNick].set(PREF_PASSWORD,sPassword);
					}
					break;
				}
			}

			// Not got it? Add new record
			if(!bFound && !sPassword.equals(""))
			{
				PreferencesGroup pgNew=selectedGroup.getChild(PREFGROUP_NICKS).addAnon();
				pgNew.set(PREF_NICK,sNick);
				pgNew.set(PREF_PASSWORD,sPassword);
			}

			resolvePasswords(selectedGroup,sNick,sPassword,true);

			t.setDim(index,col,bSameAsParent);
		} break;
		case COLUMN_DEFAULT: // Default
		{
			// Don't let them turn the top one off (sigh, these should be radios)
			if(!t.getBoolean(index,col))
			{
				t.setBoolean(index,col,true);
			}
			else
			{
				// Turn all the others off
				for(int iItem=0;iItem<t.getNumItems();iItem++)
				{
					if(iItem!=index) t.setBoolean(iItem,col,false);
				}
				// Set in prefs
				String sNick=t.getString(index,COLUMN_NICK);
				if(pgParent==null)
				{
					selectedGroup.set(PREF_DEFAULTNICK,sNick);
				}
				else if (sNick.equals(pgParent.getAnonHierarchical(PREF_DEFAULTNICK,null)))
				{
					selectedGroup.unset(PREF_DEFAULTNICK);
				}
				else
				{
					selectedGroup.set(PREF_DEFAULTNICK,sNick);
				}
			}
		} break;
		default:
			assert false;
		}
	}

	/**
	 * Get rid of any entries that refer to this nick but have the same password.
	 * @param pg Group
	 * @param nick Nickname
	 * @param password Password
	 * @param childrenOnly True to apply only to children not this group itself
	 * @throws BugException System bug
	 */
	private void resolvePasswords(PreferencesGroup pg,String nick,String password,boolean childrenOnly)
	  throws BugException
	{
		if(!childrenOnly)
		{
			// Apply change to this group
			PreferencesGroup[] apgNicks=pg.getChild(PREFGROUP_NICKS).getAnon();
			for(int iNick=0;iNick<apgNicks.length;iNick++)
			{
				if(apgNicks[iNick].get(PREF_NICK).equals(nick))
				{
					if(apgNicks[iNick].get(PREF_PASSWORD,"").equals(password))
						apgNicks[iNick].remove();
					break;
				}
			}
		}

		// Apply change to children
		PreferencesGroup[] apgChildren=pg.getAnon();
		for(int iChild=0;iChild<apgChildren.length;iChild++)
		{
			resolvePasswords(apgChildren[iChild],nick,password,false);
		}
	}

	/**
	 * Obtains nickname password starting from a particular group (checking its parents too).
	 * @param pg Group to start from
	 * @param nick Nickname in question
	 * @return Password or "" if none
	 * @throws BugException
	 */
	private String getNickPassword(PreferencesGroup pg,String nick)
	{
		PreferencesGroup[] apgNicks=pg.getChild(PREFGROUP_NICKS).getAnon();

		// Edit in this group if we've got it
		for(int iNick=0;iNick<apgNicks.length;iNick++)
		{
			if(apgNicks[iNick].get(PREF_NICK).equals(nick))
			{
				String sPassword=apgNicks[iNick].get(PREF_PASSWORD,"");
				return sPassword;
			}
		}

		// OK try parent
		PreferencesGroup pgParent=pg.getAnonParent();
		if(pgParent==null)
			return "";
		else
			return getNickPassword(pgParent,nick);
	}

	/**
	 * Changes a nickname in preferences, including all child references to it.
	 * @param pg Root group
	 * @param sBefore Previous nick
	 * @param sAfter New nick
	 * @throws GeneralException Any error
	 */
	static void changeNick(PreferencesGroup pg,String sBefore,String sAfter) throws GeneralException
	{
		// Apply change to nick if stored in this group
		PreferencesGroup[] apgNicks=pg.getChild(PREFGROUP_NICKS).getAnon();
		for(int iNick=0;iNick<apgNicks.length;iNick++)
		{
			if(apgNicks[iNick].get(PREF_NICK).equals(sBefore))
			{
				apgNicks[iNick].set(PREF_NICK,sAfter);
				break;
			}
		}

		// If this group's default nick is the 'before' one, change it
		if(pg.get(PREF_DEFAULTNICK,"").equals(sBefore))
			pg.set(PREF_DEFAULTNICK,sAfter);

		// Apply change to children
		PreferencesGroup[] apgChildren=pg.getAnon();
		for(int iChild=0;iChild<apgChildren.length;iChild++)
		{
			changeNick(apgChildren[iChild],sBefore,sAfter);
		}
	}

	/**
	 * User makes change to nickname or password field.
	 * @param index Index in table
	 * @param col Column
	 * @param value New value
	 * @param ec Editing control interface
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void nicknamesEditing(int index,int col,String value,Table.EditingControl ec) throws GeneralException
	{
		Table t=(Table)pThis.getWidget("nicknames");
		switch(col)
		{
		case COLUMN_NICK:
		{
			// Check nick is one of the permitted patterns
			if(!value.matches("[A-Za-z_][A-Za-z0-9-_\\[\\]\\\\`^{}]*"))
			{
				ec.markError();
				return;
			}

			// Check it's not the same as another nick
			for(int iRow=0;iRow<t.getNumItems();iRow++)
			{
				if(iRow!=index && t.getString(iRow,COLUMN_NICK).equals(value))
				{
					ec.markError();
					return;
				}
			}
		} break;
		case COLUMN_PASSWORD:
		{
			if(!value.matches("\\S*"))
			{
				ec.markError();
				return;
			}
			PreferencesGroup pgParent=selectedGroup.getAnonParent();
			if(pgParent!=null && getNickPassword(pgParent,t.getString(index,COLUMN_NICK)).equals(value))
			{
				ec.markDim();
				return;
			}
		}
		default:
			assert false;
		}
	}

	private final static String ADDNICKTEXT="(Add new...)";

	private void addNickTableRow()
	{
		Table tNick=(Table)pThis.getWidget("nicknames");
		int iNew=tNick.addItem();
		tNick.setString(iNew,COLUMN_NICK,ADDNICKTEXT);
		tNick.setOverwrite(iNew,COLUMN_NICK,true);
		tNick.setEditable(iNew,COLUMN_PASSWORD,false);
		tNick.setEditable(iNew,COLUMN_DEFAULT,false);
	}

//	public void changeNick() throws GeneralException
//	{
//		EditBox ebNick=(EditBox)pThis.getWidget("servernick");
//		String sValue=ebNick.getValue();
//
//		String sParentValue=pgSelected.getAnonHierarchical(PREF_NICK,PREFDEFAULT_NICK,false);
//
//		if(sValue.equals(sParentValue))
//		{
//			pgSelected.unset(PREF_NICK);
//			ebNick.setFlag(EditBox.FLAG_DIM);
//		}
//		else if(sValue.matches("[A-Za-z][]A-Za-z0-9\\[\\\\`^{}-]*")) // From RFC sect 2.3.1
//		{
//			pgSelected.set(PREF_NICK,sValue);
//			ebNick.setFlag(EditBox.FLAG_NORMAL);
//		}
//		else
//		{
//			// Don't save
//			ebNick.setFlag(EditBox.FLAG_ERROR);
//		}
//	}

	/**
	 * Action: Username changed.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void changeUser() throws GeneralException
	{
		EditBox eb=(EditBox)pThis.getWidget("serveruser");
		String sValue=eb.getValue();
		String sParentValue=selectedGroup.getAnonHierarchical(PREF_USER,PREFDEFAULT_USER,false);

		if(sValue.equals(sParentValue))
		{
			selectedGroup.unset(PREF_USER);
			eb.setFlag(EditBox.FLAG_DIM);
		}
		else if(sValue.matches("[^ \r\n\0]+")) // From RFC sect 2.3.1
		{
			selectedGroup.set(PREF_USER,sValue);
			eb.setFlag(EditBox.FLAG_NORMAL);
		}
		else
		{
			// Don't save
			eb.setFlag(EditBox.FLAG_ERROR);
		}
	}

	/**
	 * Action: Realname changed.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void changeRealName() throws GeneralException
	{
		EditBox eb=(EditBox)pThis.getWidget("serverrealname");
		String sValue=eb.getValue();
		String sParentValue=selectedGroup.getAnonHierarchical(PREF_REALNAME,PREFDEFAULT_REALNAME,false);

		if(sValue.equals(sParentValue))
		{
			selectedGroup.unset(PREF_REALNAME);
			eb.setFlag(EditBox.FLAG_DIM);
		}
		else if(sValue.length()>0)
		{
			selectedGroup.set(PREF_REALNAME,sValue);
			eb.setFlag(EditBox.FLAG_NORMAL);
		}
		else
		{
			// Don't save
			eb.setFlag(EditBox.FLAG_ERROR);
		}
	}

	/**
	 * Action: Quit message changed.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void changeQuitMessage() throws GeneralException
	{
		EditBox eb=(EditBox)pThis.getWidget("serverquitmessage");
		String sValue=eb.getValue();
		String sParentValue=selectedGroup.getAnonHierarchical(PREF_QUITMESSAGE,PREFDEFAULT_QUITMESSAGE,false);

		if(sValue.equals(sParentValue))
		{
			selectedGroup.unset(PREF_QUITMESSAGE);
			eb.setFlag(EditBox.FLAG_DIM);
		}
		else
		{
			selectedGroup.set(PREF_QUITMESSAGE,sValue);
			eb.setFlag(EditBox.FLAG_NORMAL);
		}
	}

	/**
	 * Action: Identify command changed.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void changeIdentifyCommand() throws GeneralException
	{
		String sValue=identifyCommandUI.getValue();
		String sParentValue=selectedGroup.getAnonHierarchical(PREF_IDENTIFYPATTERN,PREFDEFAULT_IDENTIFYPATTERN,false);

		if(sValue.equals(sParentValue))
		{
			selectedGroup.unset(PREF_IDENTIFYPATTERN);
			identifyCommandUI.setFlag(EditBox.FLAG_DIM);
		}
		else if(sValue.matches("/[a-z]+.*"))
		{
			selectedGroup.set(PREF_IDENTIFYPATTERN,sValue);
			identifyCommandUI.setFlag(EditBox.FLAG_NORMAL);
		}
		else
		{
			// Don't save
			identifyCommandUI.setFlag(EditBox.FLAG_ERROR);
		}
	}

	/**
	 * Action: Auto-identify option changed.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void changeAutoIdentify() throws GeneralException
	{
		String sValue=autoIdentifyUI.isChecked() ? "y" : "n";
		String sParentValue=selectedGroup.getAnonHierarchical(IRCPrefs.PREF_AUTOIDENTIFY,"y",false);

		if(sValue.equals(sParentValue))
		{
			selectedGroup.unset(IRCPrefs.PREF_AUTOIDENTIFY);
		}
		else
		{
			selectedGroup.set(IRCPrefs.PREF_AUTOIDENTIFY,sValue);
		}

		identifyCommandUI.setEnabled(autoIdentifyUI.isChecked());
	}

	@Override
	public boolean canDrag(Item i)
	{
		return i!=piRoot;
	}

	@Override
	public boolean canDragTo(Item moving,Item parent,int parentPos)
	{
		if(((PrefsServerItem)moving).isNetwork())
		{
			// Can't drag networks into networks
			return parent==piRoot;
		}
		else
		{
			return true;
		}
	}

	@Override
	public void dragTo(Item moving,Item parent,int parentPos)
	{
		((PrefsServerItem)moving).moveTo((PrefsServerItem)parent,parentPos);
		servertreeUI.update();
		selected(moving);
	}

	/**
	 * Action: Add button.
	 */
	@UIAction
	public void actionAdd()
	{
		UI ui = context.getSingle(UI.class);
		addDialog = ui.createDialog("addserver", this);
		addDialog.show(pThis.getOwner());
	}

	/**
	 * Action: User clicks Add within Add dialog.
	 */
	@UIAction
	public void actionAddAdd()
	{
		PrefsServerItem newItem;
		if(addTabsUI.getDisplayed().equals("addNetworkPage"))
		{
			PreferencesGroup newNetwork=piRoot.getGroup().addAnon();
			newNetwork.set(IRCPrefs.PREF_NETWORK,addNetworkNameUI.getValue());
			newNetwork.set(IRCPrefs.PREF_NETWORKSUFFIX,addNetworkSuffixUI.getValue());
			newNetwork.set(IRCPrefs.PREF_HANDADDED,"yes");
			newItem = new PrefsServerItem(newNetwork,piRoot);
			piRoot.addItemOnly(newItem);
		}
		else
		{
			PrefsServerItem parent=selectedItem;
			while(parent!=null && parent.isServer()) parent=(PrefsServerItem)parent.getParent();
			if(parent==null) parent=piRoot;

			PreferencesGroup newServer=parent.getGroup().addAnon();
			newServer.set(IRCPrefs.PREF_HOST,addServerAddressUI.getValue());
			newServer.set(IRCPrefs.PREF_HANDADDED,"yes");
			newItem = new PrefsServerItem(newServer,parent);
			parent.addItemOnly(newItem);
		}
		servertreeUI.update();
		servertreeUI.select(newItem);
		selected(newItem);

		addDialog.close();
	}

	/**
	 * Action: User clicks Cancel within Add dialog.
	 */
	@UIAction
	public void actionAddCancel()
	{
		addDialog.close();
	}

	private void enableAddButton()
	{
		if(addTabsUI.getDisplayed().equals("addNetworkPage"))
		{
			addAddUI.setEnabled(
				addNetworkSuffixUI.getFlag()==EditBox.FLAG_NORMAL &&
				addNetworkNameUI.getValue().length()>0);
		}
		else
		{
			addAddUI.setEnabled(addServerAddressUI.getFlag()==EditBox.FLAG_NORMAL &&
				addServerAddressUI.getValue().length()>0);
		}
	}

	/**
	 * Action: User changes tabs in Add dialog.
	 */
	@UIAction
	public void changeAddTabs()
	{
		enableAddButton();
	}

	/**
	 * Action: User changes network suffix in Add dialog.
	 */
	@UIAction
	public void changeAddNetworkSuffix()
	{
		String text=addNetworkSuffixUI.getValue();
		if(text.matches("([a-z0-9-][a-z0-9.-]*)?"))
		{
			if(!networkNameChanged || addNetworkNameUI.getValue().length()==0)
			{
				addNetworkNameUI.setValue(text);
				networkNameChanged=false;
			}
			addNetworkSuffixUI.setFlag(EditBox.FLAG_NORMAL);
		}
		else
		{
			addNetworkSuffixUI.setFlag(EditBox.FLAG_ERROR);
		}
		enableAddButton();
	}

	/**
	 * Action: User changes network name in Add dialog.
	 */
	@UIAction
	public void changeAddNetworkName()
	{
		networkNameChanged=true;
	}

	/**
	 * Action: User changes server address in Add dialog.
	 */
	@UIAction
	public void changeAddServerAddress()
	{
		String text=addServerAddressUI.getValue();
		if(text.matches("[a-z0-9.-]*"))
		{
			addServerAddressUI.setFlag(EditBox.FLAG_NORMAL);
		}
		else
		{
			addServerAddressUI.setFlag(EditBox.FLAG_ERROR);
		}
		enableAddButton();
	}

	/**
	 * Action: User closes Add dialog.
	 */
	@UIAction
	public void closedAdd()
	{
		addDialog=null;
	}

	/**
	 * Action: User clicks Delete.
	 */
	@UIAction
	public void actionDelete()
	{
		String network=selectedGroup.get(IRCPrefs.PREF_NETWORK,null);
		String server=selectedGroup.get(IRCPrefs.PREF_HOST,null);
		UI ui=context.getSingle(UI.class);
		if(ui.showQuestion(pThis,"Confirm delete",
			network!=null ?
				"Are you sure you want to delete preferences for the network "+network+
				", including all servers within it?"
				: "Are you sure you want to delete preferences for the server "+server+"?",
			UI.BUTTON_YES|UI.BUTTON_CANCEL,null,null,null,UI.BUTTON_CANCEL)==UI.BUTTON_YES)
		{
			selectedItem.remove();
			servertreeUI.update();
			servertreeUI.select(piRoot);
			selected(piRoot);
		}
	}

	/**
	 * Action: User changes port range.
	 */
	@UIAction
	public void changePortRange()
	{
		String sValue=portRangeUI.getValue();
		String sParentValue=selectedGroup.getAnonHierarchical(PREF_PORTRANGE,PREFDEFAULT_PORTRANGE,false);

		if(sValue.equals(sParentValue))
		{
			selectedGroup.unset(PREF_PORTRANGE);
			portRangeUI.setFlag(EditBox.FLAG_DIM);
		}
		else if(sValue.matches("[0-9]+(-[0-9]+)?"))
		{
			selectedGroup.set(PREF_PORTRANGE,sValue);
			portRangeUI.setFlag(EditBox.FLAG_NORMAL);
		}
		else
		{
			portRangeUI.setFlag(EditBox.FLAG_ERROR);
		}
	}

	/**
	 * Action: User changes server password.
	 */
	@UIAction
	public void changePassword()
	{
		String sValue=passwordUI.getValue();
		String sParentValue=selectedGroup.getAnonHierarchical(PREF_SERVERPASSWORD,PREFDEFAULT_SERVERPASSWORD,false);

		if(sValue.equals(sParentValue))
		{
			selectedGroup.unset(PREF_SERVERPASSWORD);
			passwordUI.setFlag(EditBox.FLAG_DIM);
		}
		else if(sValue.matches("[^ ]*"))
		{
			selectedGroup.set(PREF_SERVERPASSWORD,sValue);
			passwordUI.setFlag(EditBox.FLAG_NORMAL);
		}
		else
		{
			passwordUI.setFlag(EditBox.FLAG_ERROR);
		}
	}

	/**
	 * Action: User changes security option.
	 */
	@UIAction
	public void changeSecurity()
	{
		String value = (String)securityUI.getSelected();
		selectedGroup.set(PREF_SECUREMODE, value);
	}
}
