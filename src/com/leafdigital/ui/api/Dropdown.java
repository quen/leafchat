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
 * Interface for combo boxes.
 */
public interface Dropdown extends Widget,SupportsBaseGroup
{
	/** @return Selected value's key or null if none selected */
	public Object getSelected();

	/**
	 * Selects the given item.
	 * @param key Key of item for selection
	 */
	public void setSelected(Object key);

	/** Clears the list */
	public void clear();

	/**
	 * Adds a new value to the list
	 * @param key Object to add as key (may be anything)
	 * @param value Corresponding string to display
	 */
	public void addValue(Object key,String value);

	/**
	 * Sets callback method used whenever selection changes.
	 * @param sCallback Name of callback method
	 */
	@UICallback
	public void setOnChange(String sCallback);

	/**
	 * Enables/disables the control.
	 * @param enabled New enabled value
	 */
	public void setEnabled(boolean enabled);

	/**
	 * @return True if control is enabled
	 */
	public boolean isEnabled();
}
