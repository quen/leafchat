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
package com.leafdigital.ui.api;

import java.awt.Image;

/** Like listbox but contains a tree of items */
public interface TreeBox extends Widget
{
	/**
	 * Sets the handler used to build the tree of items.
	 * @param h Handler
	 */
	public void setHandler(Handler h);

	/** Call if the Handler's list of items might have changed */
	public void update();

	/**
	 * Selects the given item. Handler.selected() will not be called.
	 * @param i Item to select (null for none)
	 */
	public void select(Item i);

	/**
	 * Selects the given items. Handler.selected() will not be called.
	 * @param items Items to select (empty list for none)
	 */
	public void select(Item[] items);

	/**
	 * Sets width of box (if not called, will use preferred size).
	 * @param width Desired width
	 */
	public void setWidth(int width);

	/**
	 * Sets height of box (if not called, will use preferred size).
	 * @param height Desired height
	 */
	public void setHeight(int height);

	/** Interface implemented by users to handle the tree */
	public abstract interface Handler
	{
		/** @return Top-level items */
		Item getRoot();

		/** @return True if root item should be actually displayed */
		boolean isRootDisplayed();
	}

	/** Interface to implement if you need to know about selection */
	public interface SingleSelectionHandler extends Handler
	{
		/**
		 * Called when selection changes.
		 * @param i New selected item (or null if none)
		 */
		void selected(Item i);
	}

	/**
	 * Interface to implement if you need to know about selection and dragging
	 */
	public interface DragSingleSelectionHandler extends SingleSelectionHandler
	{
		/**
		 * @param i Item to drag
		 * @return True if the given item can be dragged
		 */
		boolean canDrag(Item i);

		/**
		 * @param moving Item being dragged
		 * @param parent New parent for item
		 * @param position New index to insert at within parent. This index
		 *   accounts for the assumed removal of the item being dragged first.
		 * @return True if item can be dragged there or false to give X cursor
		 */
		boolean canDragTo(Item moving,Item parent,int position);

		/**
		 * @param moving Item that's been dragged
		 * @param parent New parent for item
		 * @param position New index to insert at within parent. This index
		 *   accounts for the assumed removal of the item being dragged first.
		 */
		void dragTo(Item moving,Item parent,int position);
	}

	/** Interface to implement if you need to know about selection & allow multiple */
	public interface MultiSelectionHandler extends Handler
	{
		/**
		 * Called when selection changes.
		 * @param items New selected items
		 */
		void selected(Item[] items);
	}

	/**
	 * Interface implemented by users to pass on information about the tree
	 * of options.
	 */
	public interface Item
	{
		/** @return Text of an item */
		String getText();
		/** @return Icon for an item or null if none */
		Image getIcon();
		/** @return Children of this item */
		Item[] getChildren();
		/** @return Parent of this item */
		Item getParent();
		/** @return True if this is a leaf node */
		boolean isLeaf();
	}
}
