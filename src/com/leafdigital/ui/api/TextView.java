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

import java.awt.event.MouseEvent;
import java.io.InputStream;

import org.w3c.dom.*;

import leafchat.core.api.*;

/**
 * Interface for a scrolling text view.
 */
public interface TextView extends Widget
{
	/**
	 * Set type name used to select theme details.
	 * @param themeType Name used for items of this type within theme.
	 */
	public void setThemeType(String themeType);

	/**
	 * Set the default width for the view. Has no effect when the label is
	 * already showing.
	 * @param width Desired width (default 200)
	 */
	public void setDefaultWidth(int width);

	/**
	 * Set the default height for the view. Has no effect when the label is
	 * already showing.
	 * @param height Desired width (default 200)
	 */
	public void setDefaultHeight(int height);

	/**
	 * Removes all text back to empty state.
	 */
	public void clear();

	/** Constant indicating no line limit */
	public final static int LINELIMIT_NONE=0;

	/**
	 * If set, the view will delete lines from the beginning after you add more
	 * than this many lines. (The limit is the number that are guaranteed to be
	 * kept. It actually only deletes lines, in a batch, when you exceed the
	 * limit by a certain amount.)
	 * @param limit Limit or LINELIMIT_NONE
	 */
	public void setLineLimit(int limit);

	/** Selects everything */
	public void selectAll();

	/** @return True if the textview has a selection */
	public boolean hasSelection();

	/** Copies selection to clipboard */
	public void copy();

	/** Sets selection to none */
	public void selectNone();

	/**
	 * Add a paragraph of text.
	 * @param block XML data for new text (will be surrounded with para tag)
	 * @throws GeneralException If the text is not valid data
	 */
	public void addPara(String block) throws GeneralException;

	/**
	 * Add a line of text.
	 * @param line XML data for new text (will be surrounded with line tag)
	 * @throws GeneralException If the text is not valid data
	 */
	public void addLine(String line) throws GeneralException;

	/**
	 * Adds arbitrary text data.
	 * @param xml XML data for new text (will be surrounded with <output> but
	 *   nothing else)
	 * @throws GeneralException If the text is not valid data
	 */
	public void addXML(String xml) throws GeneralException;

	/**
	 * Sets the stylesheet for the textview. (If a stylesheet was already set,
	 * it reverts to default, then sets the new one.)
	 * @param is Stylesheet to add
	 * @throws GeneralException If there's a problem with the sheet format
	 */
	public void setStyleSheet(InputStream is) throws GeneralException;

	/**
	 * Sets the stylesheet for the textview. (If a stylesheet was already set,
	 * it reverts to default, then sets the new one.)
	 * @param styles Stylesheet to add
	 * @throws GeneralException If there's a problem with the sheet format
	 */
	public void setStyleSheet(String styles) throws GeneralException;

	/** Scrolls to end of text */
	public void scrollToEnd();

	/** @return True if currently at end of scroll */
	public boolean isAtEnd();

	/**
	 * Sets callback that happens whenever scroll position changes.
	 * @param callback Callback name
	 * @throws BugException If callback doesn't exist etc.
	 */
	@UICallback
	public void setOnScroll(String callback);

	/**
	 * Sets handler for 'actions' (user clicking on things). If called
	 * multiple times for same tag, second call replaces first.
	 * @param tag Tag that is sensitive to clicks.
	 * @param ah Handler
	 */
	public void setAction(String tag,ActionHandler ah);

	/** Interface that happens when user clicks on things */
	public interface ActionHandler
	{
		/**
		 * Called when a user clicks on the element in question.
		 * @param e Element
		 * @param me Mouse event that resulted in this action
		 * @throws GeneralException If something goes wrong
		 */
		public void action(Element e,MouseEvent me) throws GeneralException;
	}

	/**
	 * Sets handler for menu clicks (right-clicks)
	 * @param mh New handler
	 */
	public void setMenuHandler(MenuHandler mh);

	/** Interface used when user right-clicks */
	public interface MenuHandler
	{
		/**
		 * Should use members of the PopupMenu to add items if required.
		 * @param pm Menu for adding
		 * @param n XML node that was clicked on (may be null if they clicked off text)
		 */
		public void addItems(PopupMenu pm,Node n);
	}

	/**
	 * Marks the current position with a red line below it when new text appears.
	 * You cannot mark position when there is nothing in the window, or when
	 * the view has not yet been formatted for display (sized); doing so has no
	 * effect.
	 */
	public void markPosition();
	/**
	 * Fades out the position marker.
	 * @param opacity New opacity (0=transparent, 255=full)
	 */
	public void fadeMark(int opacity);
	/** Removes the position marker if present */
	public void removeMark();
	/** @return True if position is marked	 */
	public boolean hasMark();

	/**
	 * Enables or disables the 'scrolled-up warning'; a graphical cue that
	 * appears when you're not looking at the bottom of the window.
	 * @param enable True to enable
	 */
	public void setScrolledUpWarning(boolean enable);
}
