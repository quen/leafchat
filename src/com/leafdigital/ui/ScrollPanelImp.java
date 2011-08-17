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

import java.awt.Dimension;

import javax.swing.*;
import javax.swing.border.Border;

import com.leafdigital.ui.api.*;

/**
 * Implements ScrollPanel.
 */
public class ScrollPanelImp extends JScrollPane
{
	private boolean outline=true;
	private Border originalBorder;
	private Border emptyBorder=null;
	private Dimension preferredSize=new Dimension(300,400);

	private BorderPanelImp bpi;

	/** Constructor */
	ScrollPanelImp()
	{
		super(VERTICAL_SCROLLBAR_ALWAYS,HORIZONTAL_SCROLLBAR_NEVER);
		bpi=new BorderPanelImp(); // Use an internal container because BorderPanel supports all the Swing bits
		setViewportView(bpi);
		originalBorder=getBorder();
	}

	/** @return Interface giving limited public access */
	ScrollPanel getInterface()
	{
		return externalInterface;
	}

	@Override
	public Dimension getPreferredSize()
	{
		return preferredSize;
	}

	/** Interface available to public */
	private ScrollPanel externalInterface=new ScrollPanelInterface();

	/** Interface available to public */
	class ScrollPanelInterface extends BasicWidget implements ScrollPanel,InternalWidget
	{
		@Override
		public int getContentType() { return CONTENT_SINGLE; }

		@Override
		public JComponent getJComponent()
		{
			return ScrollPanelImp.this;
		}

		@Override
		public int getPreferredWidth()
		{
			return ScrollPanelImp.this.getPreferredSize().width;
		}

		@Override
		public int getPreferredHeight(int width)
		{
			return ScrollPanelImp.this.getPreferredSize().height;
		}

		@Override
		public void setBorder(final int border)
		{
			if(border==0)
				emptyBorder=null;
			else
				emptyBorder=BorderFactory.createEmptyBorder(border,border,border,border);
			updateBorder();
		}

		@Override
		public void addXMLChild(String slotName, Widget child)
		{
			set(child);
		}

		@Override
		public void set(Widget w)
		{
			final InternalWidget iw=(InternalWidget)w;
			bpi.getInterface().set(BorderPanel.NORTH,iw);
			if(iw!=null) iw.setParent(this);
		}

		@Override
		public void remove(Widget w)
		{
			set(null);
		}

		@Override
		public void removeAll()
		{
			set(null);
		}

		@Override
		public void setPreferredSize(int width,int height)
		{
			preferredSize=new Dimension(width,height);
		}

		@Override
		public void setScrollAmount(int amount)
		{
			ScrollPanelImp.this.getVerticalScrollBar().setUnitIncrement(amount);
		}

		@Override
		public void setOutline(boolean outline)
		{
			ScrollPanelImp.this.outline=outline;
			updateBorder();
		}

		@Override
		public void redoLayout()
		{
			// This actually just causes the scrollbar to update, since the scrollpane
			// itself will not change size.
			revalidate();
		}
	};

	private void updateBorder()
	{
		UISingleton.runInSwing(new Runnable()
		{
			@Override
			public void run()
			{
				if(outline && emptyBorder!=null)
					setBorder(BorderFactory.createCompoundBorder(emptyBorder,originalBorder));
				else if(emptyBorder!=null)
					setBorder(emptyBorder);
				else if(outline)
					setBorder(originalBorder);
				else
					setBorder(null);
			}
		});
	}

	@Override
	public void validate()
	{
		bpi.validate();
		super.validate();
	}
}

