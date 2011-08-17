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
 * Interface for checkboxes.
 */
public interface CheckBox extends Widget,SupportsBaseGroup
{
	/**
	 * Set the button's text label.
	 * @param text Text for label
	 */
	public void setLabel(String text);

	/**
	 * Checks or unchecks button.
	 * @param checked New checked value
	 */
	public void setChecked(boolean checked);

	/**
	 * @return True if button is checked
	 */
	public boolean isChecked();

	/**
	 * Enables/disables the checkbox.
	 * @param enabled New enabled value
	 */
	public void setEnabled(boolean enabled);

	/**
	 * Sets the change method called when button is clicked.
	 * @param callback Name of method
	 * @throws BugException If method doesn't exist etc.
	 */
	@UICallback
	public void setOnChange(String callback);
}
