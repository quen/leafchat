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
 * Interface for single-line edit boxes.
 * @see EditArea
 */
public interface EditBox extends Widget,SupportsBaseGroup
{
	/** @return Entered/selected value as string */
	public String getValue();

	/** @return Multi-line value as string array (only splits lines if wrap is turned on) */
	public String[] getValueLines();

	/** @param s New text */
	public void setValue(String s);

	/**
	 * @param useFontSettings If true, uses user-selected font rather than OS default
	 */
	public void setUseFontSettings(boolean useFontSettings);

	/**
	 * Sets a linked textview which will be handle some keyboard input from
	 * this control. (E.g. copy here will do copy in textview.)
	 * @param id ID of textview
	 * @throws BugException If the textview doesn't exist or hasn't been constructed
	 *   yet
	 */
	public void setTextView(String id);

	/**
	 * Sets the action method called when user presses Return.
	 * @param callback Name of method
	 * @throws BugException If method doesn't exist etc.
	 */
	@UICallback
	public void setOnEnter(String callback);

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

	/**
	 * Sets the action method called when multiple lines are pasted. If you
	 * don't set this, multiline pastes are disallowed. The method must have
	 * a single string parameter.
	 * @param callback Name of method or null for none.
	 */
	@UICallback
	public void setOnMultiLine(String callback);

	/**
	 * Sets details used to remember the command history between sessions. If
	 * this is set, the edit box will load its command history, and later save it
	 * into preferences.
	 * @param category Category of editbox (must follow the rules for preferences
	 *   tokens, i.e. must start with a letter, all characters must be letters,
	 *   digits, - or _)
	 * @param memoryId ID of editbox (should be unique within category; may be null
	 *   if there is never more than one window of that category; can include
	 *   any characters; does not have to match the 'real' ID assigned to the box)
	 */
	public void setRemember(String category, String memoryId);

	/**
	 * Sets the display to indicate a particular situation.
	 * @param flag One of the FLAG_xx constants
	 */
	public void setFlag(int flag);

	/** @return Current flag state FLAG_xx */
	public int getFlag();

	/**
	 * Sets a required regular expression which is applied to automatically
	 * set state to FLAG_ERROR or FLAG_NORMAL. This automatic behaviour occurs
	 * before the OnChange callback.
	 * @param require Regular expression following Java conventions e.g. "[0-9]+"
	 */
	public void setRequire(String require);

	/** @param enabled True to enable, false to disable */
	public void setEnabled(boolean enabled);

	/** @return True if enabled */
	public boolean isEnabled();

	/** Focuses this edit */
	public void focus();

	/**
	 * Sets the maximum number of bytes (UTF-8).
	 * @param max Max characters per line or LINEBYTES_NOLIMIT (default)
	 */
	public void setLineBytes(int max);

	/**
	 * Sets line wrapping (doesn't actually wrap, just indicates where lines would wrap)
	 * @param allowWrap If true, allows word-wrapping to a second line (default false)
	 */
	public void setLineWrap(boolean allowWrap);

	/**
	 * Sets preferred width
	 * @param width Width in pixels.
	 */
	public void setWidth(int width);

	/** If no limit on line characters */
	public final static int LINEBYTES_NOLIMIT=-1;

	/** Display normal text */
	public final static int FLAG_NORMAL=0;
	/** Display dim text, indicating editfield value is presently unimportant */
	public final static int FLAG_DIM=1;
	/** Display text in error colour, indicating that value must be corrected */
	public final static int FLAG_ERROR=2;

	/**
	 * Enables/disables tab-completion handling.
	 * @param tc Tab completion handler, or null for none
	 */
	public void setTabCompletion(TabCompletion tc);

	/**
	 * Handler interface to be implemented by anything that provides completion.
	 */
	public interface TabCompletion
	{
		/**
		 * Called the first time the user presses Tab in a particular situation.
		 * @param partial Beginning of a word
		 * @param atStart True if user is at the start of the edit box
		 * @return List of possible words, most likely first
		 */
		public String[] complete(String partial,boolean atStart);
	}

	/** Selects everything */
	public void selectAll();
}
