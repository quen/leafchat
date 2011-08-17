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
package com.leafdigital.ui;

import java.awt.*;
import java.util.*;

import javax.swing.JComponent;

/** Methods for everything which can go in a BaseGroup */
public interface BaseGroup
{
	/**
	 * @return natural baseline in pixels (if no offset)
	 */
	public int getBaseline();

	/**
	 * Called when the group is re-evaluated to update the additional offset on
	 * the control.
	 * @param topOffset New offset in pixels (0..)
	 */
	public void setTopOffset(int topOffset);

	/** @return InternalWidgetOwner for this item */
	public InternalWidgetOwner getInternalWidgetOwner();

	/** Static methods for handling group */
	static class Updater
	{
		/**
		 * Add an item to group.
		 * @param item Item
		 * @param group Group name
		 */
		public static void addToGroup(BaseGroup item,String group)
		{
			Set<BaseGroup> s=getGroup(item,group);
			synchronized(s)
			{
				s.add(item);
				updateGroup(item,group);
			}
		}

		/**
		 * Obtains the appropriate group from the item (actually its owner) and groupname.
		 * @param item Item; doesn't have to belong to group yet
		 * @param group Groupname
		 * @return Group set
		 */
		private static Set<BaseGroup> getGroup(BaseGroup item,String group)
		{
			return item.getInternalWidgetOwner().getArbitraryGroup("base\n"+group);
		}

		/**
		 * Remove an item from group.
		 * @param item Item
		 * @param group Group name
		 */
		public static void removeFromGroup(BaseGroup item,String group)
		{
			Set<BaseGroup> s=getGroup(item,group);
			synchronized(s)
			{
				s.remove(item);
				updateGroup(item,group);
			}
		}

		/**
		 * Updates all items in a group.
		 * @param item Any item from group
		 * @param group Group name
		 */
		public static void updateGroup(BaseGroup item,String group)
		{
			Set<BaseGroup> s = getGroup(item,group);
			synchronized(s)
			{
				int maxBaseline=0;
				for(BaseGroup otherItem : s)
				{
					maxBaseline = Math.max(maxBaseline, otherItem.getBaseline());
				}
				for(BaseGroup otherItem : s)
				{
					otherItem.setTopOffset(maxBaseline-otherItem.getBaseline());
				}
			}
		}
	}

	/**
	 * Not sure what this is for...
	 */
	static class Debug
	{
		private static boolean DEBUG=false;

		public static void paint(Graphics g,BaseGroup bg,int topOffset)
		{
			if(!DEBUG) return;
			JComponent c=(JComponent)bg;
			g.setColor(Color.red);
			g.drawLine(0,bg.getBaseline()+topOffset,c.getWidth(),bg.getBaseline()+topOffset);
			if(topOffset>0)
			{
				g.setColor(Color.blue);
				g.drawLine(0,bg.getBaseline(),c.getWidth(),bg.getBaseline());
			}

		}
	}


}
