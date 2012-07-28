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
package com.leafdigital.ui.api;

import org.w3c.dom.Element;

import leafchat.core.api.BugException;

/**
 * This interface represents the contents of a single Page within the
 * system.
 */
public interface Page extends WidgetOwner, Widget, WidgetParent
{
	/**
	 * Sets the page's title; may be used in various ways depending on where the
	 * Page is placed.
	 * @param title New title text
	 */
	public void setTitle(String title);

	/** @return Page title */
	public String getTitle();

	/**
	 * Clears existing contents and sets the contents of the page to a single
	 * widget.
	 * @param w Desired widget
	 */
	public void setContents(Widget w);

	/**
	 * Clears existing contents and sets the contents of the page based on an
	 * XML document.
	 * @param e XML element; must follow the appropriate format
	 * @throws BugException If there are format problems with the XML
	 */
	public void setContents(Element e);

	/**
	 * Sets a callback which will get called if this page is set within another
	 * one by calling setContents on the other page. Can be used to do things
	 * just before the page appears.
	 * @param callback Callback method name
	 */
	@UICallback
	public void setOnSet(String callback);
}
