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

/**
 * Interface implemented by all objects that you can add to the user interface.
 * <p>
 * Do not implement this interface to create new widgets. Instead, use
 * UI.newJComponentWrapper. (Widgets must also implement the InternalWidget
 * interface, which is not public and is subject to change.)
 */
public abstract interface Widget
{
	/** @return Widget's ID or null if none */
	public String getID();

	/** @return Widget's owner */
	public WidgetOwner getOwner();

	/**
	 * Sets the widget owner. You might need to call this if creating widgets
	 * yourself, if you want to set up a callback or similar before adding it
	 * to anything.
	 * @param owner Owner
	 */
	public void setOwner(WidgetOwner owner);

	/**
	 * Sets the visibility state of the widget. While invisible, the widget
	 * does not occupy space. Widgets default to visible.
	 * @param visible Visibility state
	 */
	public void setVisible(boolean visible);

	/**
	 * @return True if the widget is currently visible.
	 */
	public boolean isVisible();

	/**
	 * Called to inform the widget when the window that holds it has been closed.
	 */
	public void informClosed();
}
