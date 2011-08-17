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
 * Interface for radio buttons.
 */
public interface RadioButton extends Widget,SupportsBaseGroup
{
	/**
	 * Set the button's text label.
	 * @param text Text for label
	 */
	public void setLabel(String text);

	/**
	 * Sets the group which the button belongs to. Within the same group, only
	 * a single button is selected.
	 * @param group Group
	 */
	public void setGroup(String group);

	/** @return True if this button is selected */
	public boolean isSelected();

	/** Marks this button as selected, deselecting others in the group. */
	public void setSelected();

	/**
	 * Sets the action method called when button is clicked.
	 * @param callback Name of method
	 * @throws BugException If method doesn't exist etc.
	 */
	@UICallback
	public void setOnAction(String callback);

	/** @param enabled True to enable, false to disable */
	public void setEnabled(boolean enabled);

	/** @return True if enabled */
	public boolean isEnabled();

	/**
	 * Intended for field labels. Ensures that all labels in the group have
	 * the same preferred size.
	 * @param group Group identifier
	 */
	public void setWidthGroup(String group);
}
