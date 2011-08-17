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

import java.awt.Image;
import java.awt.event.*;
import java.awt.image.BufferedImage;

import javax.swing.*;

import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/** Button for a SimpleTool */
public class ToolButton extends JButton implements ThemeListener
{
	/**
	 * Constructs placeholder button, for establishing preferred height only.
	 * @param ui UI singleton
	 */
	ToolButton(UISingleton ui)
	{
		this(ui,"Test",null,null);
	}

	private String themeType;
	private SimpleTool st;

	/**
	 * Constructs a simple tool's button
	 * @param ui UI singleton
	 * @param sLabel Text label
	 * @param themeType Theme type
	 * @param st Simple tool
	 */
	ToolButton(UISingleton ui,String sLabel,String themeType,SimpleTool st)
	{
		super(sLabel);
		setOpaque(false);
		this.themeType=themeType;
		this.st=st;

		Theme t=ui.getTheme();
		updateTheme(t);
		ui.informThemeListener(this);

		setHorizontalTextPosition(CENTER);
		setHorizontalAlignment(CENTER);
		setVerticalTextPosition(BOTTOM);

		if(st!=null)
		{
			addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent ae)
				{
					try
					{
						ToolButton.this.st.clicked();
					}
					catch (GeneralException e)
					{
						ErrorMsg.report(
						  "An error occurred when the tool button was clicked",e);
					}
				}
			});
		}
	}

	@Override
	public void updateTheme(Theme t)
	{
		Class<?> defaultReference = (st==null) ? null : st.getClass();

		Image normal=themeType==null ? null
			: t.getImageProperty(themeType,"normal",true,defaultReference,themeType+".normal.png");
		if(normal==null)
			normal=new BufferedImage(48,48,BufferedImage.TYPE_INT_RGB);
		setIcon(new ImageIcon(normal));

		if(themeType!=null)
		{
			Image hover=t.getImageProperty(themeType,"hover",true,defaultReference,themeType+".hover.png");
			if(hover!=null)
				setRolloverIcon(new ImageIcon(hover));
			Image pressed=t.getImageProperty(themeType,"pressed",true,defaultReference,themeType+".pressed.png");
			if(pressed!=null)
				setPressedIcon(new ImageIcon(pressed));
		}

		if(!t.getBooleanProperty(Theme.META,"buttonLabels",true))
			setText(null);
		if(!t.getBooleanProperty(Theme.META,"buttonBorders",true))
		{
			setBorderPainted(false);
			setContentAreaFilled(false);
		}
	}
}
