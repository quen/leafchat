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

import com.leafdigital.ui.api.*;

import leafchat.core.api.BugException;

/** Basic widget wrapper for a JComponent */
public class JComponentWrapper extends BasicWidget implements Widget, InternalWidget
{
	/** Wrapped component */
	private JComponent c;

	@Override
	public int getContentType() { return CONTENT_NONE; }

	/**
	 * Construct wrapping the given JComponent.
	 * @param c Component to wrap
	 */
	JComponentWrapper(JComponent c)
	{
		this.c=c;
	}

	@Override
	public JComponent getJComponent()
	{
		return c;
	}

	@Override
	public int getPreferredWidth()
	{
		UISingleton.checkSwing();
		if(c instanceof SizeInfo)
			return ((SizeInfo)c).getPreferredWidth();
		else
			return c.getPreferredSize().width;
	}

	@Override
	public int getPreferredHeight(int iWidth)
	{
		UISingleton.checkSwing();
		if(c instanceof SizeInfo)
			return ((SizeInfo)c).getPreferredHeight(iWidth);
		else
			return c.getPreferredSize().height;
	}

	@Override
	public void addXMLChild(String sSlotName, Widget wChild)
	{
		throw new BugException("Components cannot be added to JComponentWrapper");
	}
}
