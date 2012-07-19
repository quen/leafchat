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

Copyright 2012 Samuel Marshall.
*/
package com.leafdigital.ui;

import java.awt.Image;

/**
 * Interface implemented by the different types of container for a
 * FrameContents.
 */
interface FrameHolder
{
	/**
	 * Set up this window based on settings of another one.
	 * @param other Other window we're copying
	 */
	public void initialiseFrom(FrameHolder other);

	/**
	 * @param title Title for window titlebar
	 */
	public void setTitle(String title);
	/** @return Title for window titlebar */
	public String getTitle();

	/**
	 * @param i Icon for window top-left
	 */
	public void setIcon(Image i);
	/** @return Icon for window top-left */
	public Image getIcon();

	/**
	 * @param resizable True if window may be resized
	 */
	public void setResizable(boolean resizable);
	/** @return True if window may be resized */
	public boolean isResizable();

	/**
	 * @param closable True if window may be closed
	 */
	public void setClosable(boolean closable);
	/** @return True if window may be closed */
	public boolean isClosable();

	/** Called when something happens in the window that users should notice */
	public void attention();

	/**
	 * Gets the time at which {@link #attention()} was most recently called.
	 * The intention is that this can be used to find out how recently something
	 * was attentioned.
	 * @return Time of last attention or 0 if not currently showing attention
	 */
	public long getAttentionTime();

	/** Called when X box is clicked */
	public void handleClose();

	/** Call to place focus in the frame */
	public void focusFrame();

	public void handleMinimize();

	/** @return True if window is minimised */
	public boolean isMinimized();

	/** Detaches this frame holder from its contents and gets rid of it */
	public void killSilently();

	/** @return True if window is active */
	public boolean isActive();

	/** @return True if window is hidden */
	public boolean isHidden();
}
