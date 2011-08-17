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

import java.awt.Image;

import org.w3c.dom.Element;

import leafchat.core.api.BugException;

/**
 * This interface represents the contents of a single Window within the
 * system. 'Windows' may be operating-system level windows, subwindows, or
 * even areas in tabs depending on the user's chosen interface style.
 */
public interface Window extends WidgetOwner
{
	/**
	 * Sets the window's title; used in title bars and switchbar.
	 * @param title New title text
	 */
	public void setTitle(String title);

	/** @return Window's current title */
	public String getTitle();

	/**
	 * Sets the window's icon; used in title bars and switchbar.
	 * @param icon Image to use for icon (can be any size, but should be
	 *   square; will be resized to necessary size; must be fully loaded,
	 *   for example a BufferedImage or obtained via lc.util.loadImage)
	 */
	public void setIcon(Image icon);

	/**
	 * Call when something important has happened in this window and the
	 * user should be visually notified (if it isn't on top).
	 */
	public void attention();

	/**
	 * Sets whether the window can clear attention if the user activates it or
	 * otherwise makes it visible. Set false to indicate that a window might
	 * not actually be displaying the information that attention() was supposed
	 * to highlight.
	 * @param canClearAttention True if attention will be cleared, false if it won't
	 */
	public void setCanClearAttention(boolean canClearAttention);

	/** @return The 'can clear attention' flag */
	public boolean getCanClearAttention();

	/**
	 * Set the window's initial size. This size is used when the window is shown,
	 * unless the system has remembered a size for the window in preferences.
	 * <p>
	 * Calling this after the window has been shown has no effect. If you choose
	 * a size lower than the minimum, the minimum will be used.
	 * @param width Initial width
	 * @param height Initial height
	 */
	public void setInitialSize(int width,int height);

	/**
	 * Set whether the user can resize the window. The default is true.
	 * @param resizable True if user can resize window.
	 */
	public void setResizable(boolean resizable);

	/**
	 * Sets whether the user can close the window. The default is true.
	 * @param closeable True if user can close window.
	 */
	public void setClosable(boolean closeable);

	/**
	 * Set the minimum size for this window. The system imposes its own
	 * minimum, so if you choose a very low minimum size it will be ignored.
	 * @param minWidth
	 * @param minHeight
	 */
	public void setMinSize(int minWidth,int minHeight);

	/**
	 * Set details used to remember the window position. If you don't call this
	 * method, the window position will not be remembered from one run to the
	 * next.
	 * @param category Category of window (must follow the rules for preferences
	 *   tokens, i.e. must start with a letter, all characters must be letters,
	 *   digits, - or _)
	 * @param id ID of window (should be unique within category; may be null
	 *   if there is never more than one window of that category; can include
	 *   any characters)
	 */
	public void setRemember(String category,String id);

	/**
	 * Store additional text that will be remembered with the window position.
	 * @param text Extra text
	 */
	public void setExtraRemember(String text);
	/**
	 * @return Extra text that was remembered with the window position, or null
	 * if none.
	 */
	public String getExtraRemember();

	/**
	 * Call when the window is ready to show. Nothing will be displayed until
	 * you call this method.
	 * @param minimised If true, initially shows in minimised state
	 */
	public void show(boolean minimised);

	/**
	 * Clears existing contents and sets the contents of the window to a single
	 * widget.
	 * @param w Desired widget
	 */
	public void setContents(Widget w);

	/**
	 * Clears existing contents and sets the contents of the window based on an
	 * XML document.
	 * @param e XML element; must follow the appropriate format
	 * @throws BugException If there are format problems with the XML
	 */
	public void setContents(Element e);

	/**
	 * Closes the window. This is a permanent operation; once closed, the window
	 * may not be shown again.
	 */
	public void close();

	/** @return True if window has been closed */
	public boolean isClosed();

	/**
	 * Minimises the window. (Does nothing if it's already minimised.)
	 */
	public void minimize();

	/**
	 * Activates the window (brings it to front and sets keyboard focus).
	 */
	public void activate();

	/** @return True if window is currently active */
	public boolean isActive();

	/**
	 * Sets the action method called when window is closed.
	 * @param callback Name of method
	 * @throws BugException If method doesn't exist etc.
	 */
	@UICallback
	public void setOnClosed(String callback);

	/**
	 * Sets the action method called when window is about to be closed. If you
	 * set this, you must manually call close() after receiving this callback;
	 * the window won't close automatically.
	 * @param callback Name of method
	 * @throws BugException If method doesn't exist etc.
	 */
	@UICallback
	public void setOnClosing(String callback);

	/**
	 * Sets the action method called when window becomes active (current).
	 * @param callback Name of method
	 * @throws BugException If method doesn't exist etc.
	 */
	@UICallback
	public void setOnActive(String callback);

	/**
	 * @return True if window is currently hidden (e.g. a tab that is not current).
	 */
	public boolean isHidden();

	/**
	 * @return True if window is currently minimized.
	 */
	public boolean isMinimized();
}
