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

import java.util.Arrays;

import com.leafdigital.irc.api.*;
import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/**
 * Preferences page for controlling the ignore list.
 */
@UIHandler({"ignorelist", "addignoremask"})
public class IgnoreListPage
{
	private PluginContext pc;
	private Page p;

	// Page UI elements
	/** Button: Delete */
	public Button deleteUI;
	/** Table: Ignore list */
	public Table ignoreListUI;

	// Dialog UI elements
	/** Button: Add mask */
	public Button addMaskUI;
	/** Editbox: Nickname */
	public EditBox nickUI;
	/** Editbox: Username */
	public EditBox userUI;
	/** Editbox: Hostname */
	public EditBox hostUI;

	IgnoreListPage(PluginContext pc)
	{
		this.pc=pc;
		UI ui=pc.getSingle(UI.class);
		p = ui.createPage("ignorelist", this);
		pc.requestMessages(IgnoreListChangeMsg.class,this,Msg.PRIORITY_FIRST);
		msg(null);
	}

	private final static int COL_NICK=0,COL_USER=1,COL_HOST=2;

	/**
	 * Message: Ignore list has changed.
	 * @param msg Message
	 */
	public void msg(IgnoreListChangeMsg msg)
	{
		IRCUserAddress selectedMask=null;
		int selectedIndex=ignoreListUI.getSelectedIndex();
		if(selectedIndex!=Table.NONE)
		{
			selectedMask=new IRCUserAddress(
				ignoreListUI.getString(selectedIndex,COL_NICK),
				ignoreListUI.getString(selectedIndex,COL_USER),
				ignoreListUI.getString(selectedIndex,COL_HOST));
		}
		ignoreListUI.clear();

		IgnoreList il=pc.getSingle(IgnoreList.class);
		IRCUserAddress[] masks=il.getMasks();
		Arrays.sort(masks);
		for(int i=0;i<masks.length;i++)
		{
			int newRow=ignoreListUI.addItem();
			ignoreListUI.setString(newRow,COL_NICK,masks[i].getNick());
			ignoreListUI.setString(newRow,COL_USER,masks[i].getUser());
			ignoreListUI.setString(newRow,COL_HOST,masks[i].getHost());

			if(masks[i].equals(selectedMask))
				ignoreListUI.setSelectedIndex(newRow);
		}
	}

	Page getPage()
	{
		return p;
	}

	private Dialog addMask;

	/**
	 * Action: Add button.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void actionAdd() throws GeneralException
	{
		UI ui=pc.getSingle(UI.class);
		addMask = ui.createDialog("addignoremask", this);
		addMask.show(p);
	}

	/**
	 * Action: Remove button.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void actionRemove() throws GeneralException
	{
		int row=ignoreListUI.getSelectedIndex();
		IRCUserAddress selectedMask=new IRCUserAddress(
			ignoreListUI.getString(row,COL_NICK),
			ignoreListUI.getString(row,COL_USER),
			ignoreListUI.getString(row,COL_HOST));

		IgnoreList il=pc.getSingle(IgnoreList.class);
		il.removeMask(selectedMask);
	}

	/**
	 * Action: Ignore list selection change.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void selectIgnoreList() throws GeneralException
	{
		deleteUI.setEnabled(ignoreListUI.getSelectedIndex()!=Table.NONE);
	}

	// Mask dialog methods

	/**
	 * Action: Dialog closed.
	 */
	@UIAction
	public void closedAddMask()
	{
		addMask=null;
	}

	/**
	 * Action: Cancel button.
	 */
	@UIAction
	public void actionCancel()
	{
		addMask.close();
	}

	private final static String SAFECHARACTERS="[^!@ ]+";

	/**
	 * Action: Text changed (any edit box).
	 */
	@UIAction
	public void changeText()
	{
		boolean ok=true;
		if(nickUI.getValue().matches(SAFECHARACTERS))
		{
			nickUI.setFlag(EditBox.FLAG_NORMAL);
		}
		else
		{
			nickUI.setFlag(EditBox.FLAG_ERROR);
			ok=false;
		}
		if(userUI.getValue().matches(SAFECHARACTERS))
		{
			userUI.setFlag(EditBox.FLAG_NORMAL);
		}
		else
		{
			userUI.setFlag(EditBox.FLAG_ERROR);
			ok=false;
		}
		if(hostUI.getValue().matches(SAFECHARACTERS))
		{
			hostUI.setFlag(EditBox.FLAG_NORMAL);
		}
		else
		{
			hostUI.setFlag(EditBox.FLAG_ERROR);
			ok=false;
		}
		addMaskUI.setEnabled(ok);
	}

	/**
	 * Action: Add mask button clicked.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void actionAddMask() throws GeneralException
	{
		if(!addMaskUI.isEnabled()) return;

		IRCUserAddress newMask=new IRCUserAddress(
			nickUI.getValue(),userUI.getValue(),hostUI.getValue());

		IgnoreList il=pc.getSingle(IgnoreList.class);
		il.addMask(newMask);

		addMask.close();
	}

	/**
	 * Action: Nick box focused.
	 */
	@UIAction
	public void focusNick()
	{
		if(nickUI.getValue().equals("*")) nickUI.selectAll();
	}

	/**
	 * Action: User box focused.
	 */
	@UIAction
	public void focusUser()
	{
		if(userUI.getValue().equals("*")) userUI.selectAll();
	}

	/**
	 * Action: Host box focused.
	 */
	@UIAction
	public void focusHost()
	{
		if(hostUI.getValue().equals("*")) hostUI.selectAll();
	}
}
