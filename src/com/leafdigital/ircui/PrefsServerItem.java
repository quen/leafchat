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

import java.awt.Image;
import java.util.*;

import com.leafdigital.prefs.api.PreferencesGroup;
import com.leafdigital.ui.api.TreeBox;
import com.leafdigital.ui.api.TreeBox.Item;

class PrefsServerItem implements TreeBox.Item
{
	private PreferencesGroup pg;
	private PrefsServerItem parent;
	private PrefsServerItem[] children;

	boolean isRoot() { return parent==null; }
	boolean isNetwork() { return pg.exists("network"); }
	boolean isServer() { return pg.exists("host"); }

	@Override
	public boolean isLeaf()
	{
		return isServer();
	}

	PreferencesGroup getGroup() { return pg; }

	PrefsServerItem(PreferencesGroup pg,PrefsServerItem parent)
	{
		this.pg=pg;
		this.parent=parent;
		PreferencesGroup[] apg=pg.getAnon();
		children=new PrefsServerItem[apg.length];
		for(int i=0;i<apg.length;i++)
		{
			children[i]=new PrefsServerItem(apg[i],this);
		}
	}

	/**
	 * Finds the PrefsServerItem that corresponds to a given preferences group.
	 * @param pg Desired group
	 * @return Corresponding item or null if none
	 */
	PrefsServerItem find(PreferencesGroup pg)
	{
		if(this.pg==pg) return this;
		for(int i=0;i<children.length;i++)
		{
			PrefsServerItem result=children[i].find(pg);
			if(result!=null) return result;
		}
		return null;
	}

	void remove()
	{
		pg.remove();
		LinkedList<PrefsServerItem> parentKids =
			new LinkedList<PrefsServerItem>(Arrays.asList(parent.children));
		parentKids.remove(this);
		parent.children = parentKids.toArray(new PrefsServerItem[parentKids.size()]);
		parent=null;
	}

	void add(PrefsServerItem newChild,int newPos)
	{
		pg.addAnon(newChild.pg,newPos);
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
			return pg.get("host",pg.get("network","<unknown>"));
	}

	@Override
	public Image getIcon()
	{
		// TODO Maybe put an icon there
		return null;
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