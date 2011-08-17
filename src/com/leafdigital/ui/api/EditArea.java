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
 * Interface for multi-line edit boxes.
 * @see EditBox
 */
public interface EditArea extends Widget,SupportsBaseGroup
{
	/** @return Entered value as string with line breaks */
	public String getValue();

	/** @param s New text including possible \n */
	public void setValue(String s);

	/**
	 * Sets the action method called when user types or does anything else that
	 * changes the value.
	 * @param callback Name of method
	 * @throws BugException If method doesn't exist etc.
	 */
	@UICallback
	public void setOnChange(String callback);

	/**
	 * Sets the action method called when the control is focused.
	 * @param callback Name of method
	 * @throws BugException If method doesn't exist etc.
	 */
	@UICallback
	public void setOnFocus(String callback);

	/** @param enabled True to enable, false to disable */
	public void setEnabled(boolean enabled);

	/** @return True if enabled */
	public boolean isEnabled();

	/** Focuses this edit */
	public void focus();

	/**
	 * Sets preferred width
	 * @param width Width in pixels.
	 */
	public void setWidth(int width);

	/**
	 * Sets preferred height
	 * @param height Height in pixels
	 */
	public void setHeight(int height);

	/**
	 * Turns on the autostretch feature. If this is set, the edit area
	 * automatically changes its size based on the content, instead of offering
	 * scrollbars.
	 * @param autoStretch
	 */
	public void setAutoStretch(boolean autoStretch);

	/**
	 * Highlights specified lines. The lines remain highlighted only until
	 * the user edits something.
	 * @param lines Array of 0-based line numbers (so first line is 0, not 1)
	 * to highlight, or null for none
	 */
	public void highlightErrorLines(int[] lines);

	/** Selects everything */
	public void selectAll();
}
