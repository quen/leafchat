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
package com.leafdigital.ui;

import javax.swing.JComponent;

import org.w3c.dom.Element;

import com.leafdigital.ui.api.*;

import leafchat.core.api.BugException;

/** All widgets actually implement this interface too */
interface InternalWidget extends Widget
{
	final static int CONTENT_NONE=0,CONTENT_NAMEDSLOTS=1,CONTENT_UNNAMEDSLOTS=2,CONTENT_SINGLE=3;

	/**
	 * Called to set the widget's ID if it has one.
	 * @param sID ID for widget
	 */
	public void setID(String sID);

	/**
	 * Called to inform widget of owner UI.
	 * @param uis UI singleton that created this widet.
	 */
	public void setUI(UISingleton uis);

	/**
	 * Called to inform widget of its owner
	 * @param wo Widget owner
	 */
	@Override
	public void setOwner(WidgetOwner wo);

	/**
	 * This method is called during XML construction to add children. Every
	 * child element within the widgetname element counts as a slot; things
	 * within that are created as widgets then passed to this method.
	 * @param sSlotName Name of XML tag
	 * @param wChild Child widget
	 * @throws BugException If the slot name is invalid, etc.
	 */
	public void addXMLChild(String sSlotName,Widget wChild);

	/**
	 * @return Swing JComponent that handles painting and activity.
	 */
	public JComponent getJComponent();

	/**
	 * @return Preferred width of component.
	 */
	public int getPreferredWidth();

	/**
	 * @param iWidth Intended width (if this does not affect the component's
	 *   height, it can be ignored)
	 * @return Appropriate height for component, at that width
	 */
	public int getPreferredHeight(int iWidth);

	/**
	 * @return Array of strings corresponding to child tags that are processed
	 *   by the widget itself, not the system
	 */
	public String[] getReservedChildren();

	/**
	 * @return A CONTENT_xx property
	 */
	public int getContentType();

	/**
	 * Called to initialise the XML children.
	 * @param aeReserved Each element matching a reserved name
	 * @throws BugException Any error with the data
	 */
	public void setReservedData(Element[] aeReserved);

	/**
	 * If this widget has a parent widget, call this method to set it.
	 * @param parent Parent (null for none)
	 */
	public void setParent(InternalWidget parent);

	/**
	 * @return The value set by setParent (null if none set)
	 */
	public InternalWidget getParent();

	/** Call to update layout when some component changed */
	public void redoLayout();
}
