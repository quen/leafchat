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

/**
 * Interface for list boxes.
 */
public interface ListBox extends Widget, SupportsMacIndent
{
	/** @return Selected value as string, null if none */
	public String getSelected();

	/** @return Selected value data, null if none */
	public Object getSelectedData();

	/** @return Selected values as string array */
	public String[] getMultiSelected();

	/** @return Selected value data */
	public Object[] getMultiSelectedData();

	/** @param b If true, allows multiple selection */
	public void setMultiSelect(boolean b);

	/** @param b If true, sorts list */
	public void setSort(boolean b);

	/** @param s New item to add */
	public void addItem(String s);

	/**
	 *  @param s New item to add
	 *  @param data Associated data
	 */
	public void addItem(String s,Object data);

	/** @param s Item to remove */
	public void removeItem(String s);

	/** @param data Data of item to remove */
	public void removeData(Object data);

	/**
	 * Changes item selection (SelectionChange events won't be sent)
	 * @param s Item to select
	 * @param selected True to select, false to deselect
	 */
	public void setSelected(String s,boolean selected);

	/**
	 * Changes item selection (SelectionChange events won't be sent)
	 * @param data Item to select
	 * @param selected True to select, false to deselect
	 */
	public void setSelectedData(Object data,boolean selected);

  /** Remove all items */
	public void clear();

	/** Deselect everything */
	public void clearSelection();

	/** @return List of all strings in box */
	public String[] getItems();

	/**
	 * @param items Class to use for array
	 * @return List of all data items in box
	 */
	public<C> C[] getData(Class<C> items);

	/**
	 * Sets callback method used whenever selection changes.
	 * @param callback Name of callback method
	 */
	@UICallback
	public void setOnChange(String callback);

	/**
	 * Sets callback method used when somebody double-clicks.
	 * @param callback Name of callback method or null to disable
	 */
	@UICallback
	public void setOnAction(String callback);

	/**
	 * Sets width of list.
	 * @param width Desired width
	 */
	public void setWidth(int width);

	/**
	 * Enables/disables the widget.
	 * @param enabled New enabled value
	 */
	public void setEnabled(boolean enabled);

	/**
	 * @return True if widget is enabled
	 */
	public boolean isEnabled();

	/**
	 * Sets handler for menu clicks (right-clicks). Method signature:
	 * callback(PopupMenu pm). Method should either add options to the popup menu
	 * or do nothing (in which case menu won't be displayed).
	 * @param callback Name of callback method or null to disable
	 */
	@UICallback
	public void setOnMenu(String callback);

	/**
	 * @param useFontSettings If true, uses user-selected font rather than OS default
	 */
	public void setUseFontSettings(boolean useFontSettings);

	/**
	 * Marks an item bold or not-bold.
	 * @param s Item to mark
	 * @param bold True for bold, false for not
	 */
	public void setBold(String s,boolean bold);

	/**
	 * Marks an item faint or not-faint.
	 * @param s Item to mark
	 * @param faint True for faint, false for not
	 */
	public void setFaint(String s,boolean faint);
}
