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

import java.awt.Image;
import java.util.*;

import com.leafdigital.irc.api.IRCPrefs;
import com.leafdigital.prefs.api.PreferencesGroup;
import com.leafdigital.ui.api.*;
import com.leafdigital.ui.api.TreeBox.Item;

import leafchat.core.api.PluginContext;

class PrefsServerItem implements TreeBox.Item
{
	private PreferencesGroup group;
	private PrefsServerItem parent;
	private PrefsServerItem[] children;
	private PluginContext context;

	boolean isRoot() { return parent == null; }
	boolean isNetwork() { return group.exists("network"); }
	boolean isServer() { return group.exists("host"); }

	/**
	 * @return True if this server is a redirector
	 */
	boolean isRedirect()
	{
		return group.exists(IRCPrefs.PREF_REDIRECTOR);
	}

	@Override
	public boolean isLeaf()
	{
		return isServer();
	}

	PreferencesGroup getGroup() { return group; }

	PrefsServerItem(PreferencesGroup group, PrefsServerItem parent, PluginContext context)
	{
		this.group = group;
		this.parent = parent;
		this.context = context;
		PreferencesGroup[] groupAnon = group.getAnon();
		children=new PrefsServerItem[groupAnon.length];
		for(int i=0; i<groupAnon.length; i++)
		{
			children[i] = new PrefsServerItem(groupAnon[i], this, context);
		}
	}

	/**
	 * Finds the PrefsServerItem that corresponds to a given preferences group.
	 * @param pg Desired group
	 * @return Corresponding item or null if none
	 */
	PrefsServerItem find(PreferencesGroup pg)
	{
		if(this.group==pg) return this;
		for(int i=0;i<children.length;i++)
		{
			PrefsServerItem result=children[i].find(pg);
			if(result!=null) return result;
		}
		return null;
	}

	void remove()
	{
		group.remove();
		LinkedList<PrefsServerItem> parentKids =
			new LinkedList<PrefsServerItem>(Arrays.asList(parent.children));
		parentKids.remove(this);
		parent.children = parentKids.toArray(new PrefsServerItem[parentKids.size()]);
		parent=null;
	}

	void add(PrefsServerItem newChild,int newPos)
	{
		group.addAnon(newChild.group,newPos);
		newChild.parent=this;
		LinkedList<PrefsServerItem> kids =
			new LinkedList<PrefsServerItem>(Arrays.asList(children));
		kids.add(newPos,newChild);
		children = kids.toArray(new PrefsServerItem[kids.size()]);
	}

	void addItemOnly(PrefsServerItem newChild)
	{
		LinkedList<PrefsServerItem> kids =
			new LinkedList<PrefsServerItem>(Arrays.asList(children));
		kids.add(newChild);
		children = kids.toArray(new PrefsServerItem[kids.size()]);
	}

	void moveTo(PrefsServerItem newParent,int newPos)
	{
		remove();
		newParent.add(this,newPos);
	}

	@Override
	public TreeBox.Item getParent()
	{
		return parent;
	}

	@Override
	public String getText()
	{
		if(isRoot())
			return "Global settings";
		else
			return group.get("host",group.get("network","<unknown>"));
	}

	@Override
	public Image getIcon()
	{
		return context.getSingle(UI.class).getTheme().getImageProperty("serverTree",
			isNetwork() ? "network" : (isRedirect() ? "redirect" : "server"), true,
				null, null);
	}

	@Override
	public Item[] getChildren()
	{
		return children;
	}

	@Override
	public String toString()
	{
		return getText();
	}
}