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
 * Preferesnces page: watch list.
 */
@UIHandler({"watchlist", "addwatchmask"})
public class WatchListPage
{
	private PluginContext pc;
	private Page p;

	// Page UI elements

	/** Button: Delete entry */
	public Button deleteUI;
	/** Table: List of watch entries */
	public Table watchListUI;

	// Dialog UI elements

	/** Button: Add mask */
	public Button addMaskUI;
	/** Editbox: Nickname */
	public EditBox nickUI;
	/** Editbox: Username */
	public EditBox userUI;
	/** Editbox: Hostname */
	public EditBox hostUI;

	WatchListPage(PluginContext pc)
	{
		this.pc=pc;
		UI ui = pc.getSingleton2(UI.class);
		p = ui.createPage("watchlist", this);
		pc.requestMessages(WatchListChangeMsg.class,this,Msg.PRIORITY_FIRST);
		msg(null);
	}

	private final static int COL_NICK=0,COL_USER=1,COL_HOST=2;

	/**
	 * Message: Watch list has changed.
	 * @param msg Message
	 */
	public void msg(WatchListChangeMsg msg)
	{
		IRCUserAddress selectedMask=null;
		int selectedIndex=watchListUI.getSelectedIndex();
		if(selectedIndex!=Table.NONE)
		{
			selectedMask=new IRCUserAddress(
				watchListUI.getString(selectedIndex,COL_NICK),
				watchListUI.getString(selectedIndex,COL_USER),
				watchListUI.getString(selectedIndex,COL_HOST));
		}
		watchListUI.clear();

		WatchList il=pc.getSingleton2(WatchList.class);
		IRCUserAddress[] masks=il.getMasks();
		Arrays.sort(masks);
		for(int i=0;i<masks.length;i++)
		{
			int newRow=watchListUI.addItem();
			watchListUI.setString(newRow,COL_NICK,masks[i].getNick());
			watchListUI.setString(newRow,COL_USER,masks[i].getUser());
			watchListUI.setString(newRow,COL_HOST,masks[i].getHost());

			if(masks[i].equals(selectedMask))
				watchListUI.setSelectedIndex(newRow);
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
		UI ui = pc.getSingleton2(UI.class);
		addMask = ui.createDialog("addwatchmask", this);
		addMask.show(p);
	}

	/**
	 * Action: Remove button.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void actionRemove() throws GeneralException
	{
		int row=watchListUI.getSelectedIndex();
		IRCUserAddress selectedMask=new IRCUserAddress(
			watchListUI.getString(row,COL_NICK),
			watchListUI.getString(row,COL_USER),
			watchListUI.getString(row,COL_HOST));

		WatchList il=pc.getSingleton2(WatchList.class);
		il.removeMask(selectedMask);
	}

	/**
	 * Action: Watch list selection changed.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void selectWatchList() throws GeneralException
	{
		deleteUI.setEnabled(watchListUI.getSelectedIndex()!=Table.NONE);
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
	private final static String SAFECHARACTERSNOSTAR="[^!@* ]+";

	/**
	 * Action: Text changed in any of the edit fields.
	 */
	@UIAction
	public void changeText()
	{
		boolean ok=true;
		if(nickUI.getValue().matches(SAFECHARACTERSNOSTAR) )
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
	 * Action: User clicks to add the mask.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void actionAddMask() throws GeneralException
	{
		if(!addMaskUI.isEnabled()) return;

		IRCUserAddress newMask=new IRCUserAddress(
			nickUI.getValue(),userUI.getValue(),hostUI.getValue());

		WatchList il=pc.getSingleton2(WatchList.class);
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
