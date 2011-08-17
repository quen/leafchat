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

import leafchat.core.api.BugException;

/**
 * A Panel with any number of slots (all named 'tab' in XML). Components are
 * laid out in separate tabs. Components should be Pages; it uses their title
 * for the tab names.
 */
public interface TabPanel extends Panel
{
	/**
	 * Adds a page to the end.
	 * @param p New page
	 */
	public void add(Page p);

	/**
	 * Brings a particular page to the front.
	 * @param id ID of desired page
	 * @throws BugException If id doesn't exist or isn't a page etc
	 */
	public void display(String id) throws BugException;

	/**
	 * Sets the callback called when the user clicks on a tab. (Not called
	 * for manual sets.)
	 * @param callback Callback
	 */
	@UICallback
	public void setOnChange(String callback);

	/** @return ID of displayed page or null if it doesn't have one */
	public String getDisplayed();
}
