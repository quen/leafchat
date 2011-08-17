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

import com.leafdigital.ui.api.TextView.ActionHandler;

/**
 * Interface for wrapping (multi-line) labels
 */
public interface Label extends Widget, SupportsBaseGroup, SupportsMacIndent
{
	/**
	 * Set the text.
	 * @param text Text for label in XML format.
	 */
	public void setText(String text);

	/** @return Text of label */
	public String getText();

	/**
	 * Set the default width for the label. Has no effect when the label is
	 * already showing.
	 * @param width Desired width
	 */
	public void setDefaultWidth(int width);

	/**
	 * Sets minimum preferred width for the label.
	 * @param width Desired width
	 */
	public void setMinWidth(int width);

	/**
	 * Intended for field labels. Ensures that all labels in the group have
	 * the same preferred size.
	 * @param group Group identifier
	 */
	public void setWidthGroup(String group);

	/**
	 * @param small If yes, uses a small font.
	 */
	public void setSmall(boolean small);

	/**
	 * Sets handler for 'actions' (user clicking on things). If called
	 * multiple times for same tag, second call replaces first.
	 * @param tag Tag that is sensitive to clicks.
	 * @param ah Handler
	 */
	public void setAction(String tag,ActionHandler ah);
}
