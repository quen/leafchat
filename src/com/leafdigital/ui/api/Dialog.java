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

import org.w3c.dom.Element;

import leafchat.core.api.BugException;

/**
 * This interface represents the contents of a single Dialog within the
 * system. (Dialogs are similar to Windows, but modal i.e. user has to
 * dismiss the dialog before they do anything else. For this reason they
 * should be used sparingly.)
 */
public interface Dialog extends WidgetOwner
{
	/**
	 * Sets the dialog's title; used in title bar
	 * @param title New title text
	 */
	public void setTitle(String title);

	/**
	 * Set the dialog's initial size. This size is used when the dialog is shown.
	 * <p>
	 * Calling this after the dialog has been shown has no effect. If you choose
	 * a size lower than the minimum, the minimum will be used.
	 * @param width Initial width
	 * @param height Initial height
	 */
	public void setInitialSize(int width,int height);

	/**
	 * Set whether the user can resize the dialog. The default is false.
	 * @param resizable True if user can resize window
	 */
	public void setResizable(boolean resizable);

	/**
	 * Set whether the user can close the dialog by clicking the X button. The
	 * default is true.
	 * @param closeable True if user can close window
	 */
	public void setCloseable(boolean closeable);

	/**
	 * Set the minimum size for this dialog. The system imposes its own
	 * minimum, so if you choose a very low minimum size it will be ignored.
	 * @param minWidth
	 * @param minHeight
	 */
	public void setMinSize(int minWidth,int minHeight);

	/**
	 * Call when the dialog is ready to show. Nothing will be displayed until
	 * you call this method.
	 * <p>
	 * Unlike windows, your application is responsible for closing the dialog
	 * using close(). The user will not be able to access other windows until
	 * the dialog is hidden.
	 * @param parent The dialog is positioned relative to this. Null is OK.
	 */
	public void show(WidgetOwner parent);

	/**
	 * Clears existing contents and sets the contents of the dialog to a single
	 * widget.
	 * @param w Desired widget
	 */
	public void setContents(Widget w);

	/**
	 * Clears existing contents and sets the contents of the dialog based on an
	 * XML document.
	 * @param e XML element; must follow the appropriate format
	 * @throws BugException If there are format problems with the XML
	 */
	public void setContents(Element e);

	/**
	 * Closes the dialog. This is a permanent operation; once closed, the window
	 * may not be shown again.
	 * <p>
	 * After this call, the application will resume as normal.
	 */
	public void close();

	/**
	 * Sets the action method called when dialog is closed.
	 * @param callback Name of method
	 * @throws BugException If method doesn't exist etc.
	 */
	@UICallback
	public void setOnClosed(String callback);
}
