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

/** A wrappable label */
public class SpacerImp extends JComponent
{
	private int width=4,height=4;

	Spacer getInterface() { return outsideInterface; }

	SpacerInterface outsideInterface=new SpacerInterface();

	class SpacerInterface extends BasicWidget implements Spacer, InternalWidget
	{
		@Override
		public int getContentType() { return CONTENT_NONE; }

		@Override
		public JComponent getJComponent()
		{
			return SpacerImp.this;
		}

		@Override
		public int getPreferredWidth()
		{
			return width;
		}

		@Override
		public int getPreferredHeight(int iWidth)
		{
			return height;
		}

		@Override
		public void addXMLChild(String sSlotName, Widget wChild)
		{
			throw new BugException("Spacers cannot contain children");
		}

		@Override
		public void setHeight(int height)
		{
			SpacerImp.this.height=height;
			revalidate();
		}

		@Override
		public void setWidth(int width)
		{
			SpacerImp.this.width=width;
			revalidate();
		}
	}
}
