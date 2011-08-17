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

/** Interface for widgets that support the BaseGroup function */
public interface SupportsMacIndent
{
	/**
	 * Add indent to match up with edit boxes.
	 */
	public final static String TYPE_EDIT = "edit";
	/**
	 * Legacy value so that old XML files using the "y/n" notation work.
	 */
	public final static String TYPE_EDIT_LEGACY = "y";
	/**
	 * Add indent to match up with buttons.
	 */
	public final static String TYPE_BUTTON = "button";
	/**
	 * Do not add indent (this is default).
	 */
	public final static String TYPE_NONE = "n";

	/**
	 * On Mac OS X, most interactive control types gain an extra indent which
	 * allows the system space to display the blue focus ring around the control.
	 * Labels are not interactive so do not have the extra indent. When a label
	 * should line up horizontally with such a control, this option can be used
	 * to add an extra left indent on Mac only.
	 * <p>
	 * @param macIndent True to add extra indent on Mac
	 * @deprecated Please use the more specific {@link #setMacIndent(String)}.
	 */
	public void setMacIndent(boolean macIndent);

	/**
	 * On Mac OS X, most interactive control types gain an extra indent which
	 * allows the system space to display the blue focus ring around the control.
	 * Labels are not interactive so do not have the extra indent. When a label
	 * should line up horizontally with such a control, this option can be used
	 * to add an extra left indent on Mac only.
	 * @param type TYPE_xx constant (usually "edit" or "button" or "n")
	 * @throws IllegalArgumentException If the type is unknown
	 */
	public void setMacIndent(String type) throws IllegalArgumentException;
}
