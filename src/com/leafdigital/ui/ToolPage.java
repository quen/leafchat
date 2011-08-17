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

import java.awt.*;

import javax.swing.JComponent;

import com.leafdigital.ui.api.*;

/**
 * Toolbar item based on a Page type.
 */
public class ToolPage extends JComponent
{
	private PageTool pt;
	private JComponent c;
	private Page page;

	/**
	 * @param pt Page tool
	 */
	ToolPage(PageTool pt)
	{
		this.pt = pt;
		setLayout(null);
		page = pt.getPage();
		InternalWidget iw=((InternalWidget)page);
		c = iw.getJComponent();
		add(c);
	}

	@Override
	public void setBounds(int x,int y,int width,int height)
	{
		super.setBounds(x,y,width,height);

		// Work out component's desired size
		InternalWidget iw=((InternalWidget)pt.getPage());
		int desiredHeight=iw.getPreferredHeight(width);

		// Centre vertically
		c.setBounds(0,(height-desiredHeight)/2,width,desiredHeight);
	}

	@Override
	public Dimension getPreferredSize()
	{
		InternalWidget iw=((InternalWidget)pt.getPage());
		int width=iw.getPreferredWidth();
		int height=iw.getPreferredHeight(width);
		return new Dimension(width,height);
	}

	/**
	 * @return Page represented by this tool
	 */
	public Page getPage()
	{
		return page;
	}
}
