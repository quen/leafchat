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

import java.util.*;

import javax.swing.JComponent;

import com.leafdigital.ui.api.*;

/**
 * Implements VerticalPanel
 */
public class VerticalPanelImp extends JComponent
{
	/** Spacing between grid squares */
	private int spacing=0;

	/** Border at edge of grid */
	private int border=0;

	/** Keep record of held components */
	private List<InternalWidget> components = new LinkedList<InternalWidget>();

	/** Constructor */
	VerticalPanelImp()
	{
		setLayout(null);
	}

	@Override
	public void setBounds(int x, int y, int width, int height)
	{
		super.setBounds(x, y, width, height);
		updateLayout();
	}

	/** Move all components to their correct place in new layout */
	private void updateLayout()
	{
		UISingleton.checkSwing();

		// Get available width
		int iWidth=getWidth()-border*2;
		int iY=border;

		// Go through components laying them out
		synchronized(components)
		{
			for(InternalWidget iw : components)
			{
				if(!iw.isVisible()) continue;

				int iWidgetHeight=iw.getPreferredHeight(iWidth);

				iw.getJComponent().setBounds(border,iY,iWidth,iWidgetHeight);
				iY+=iWidgetHeight;
				iY+=spacing;
			}
		}
		repaint();
	}

	/** @return Interface giving limited public access */
	VerticalPanel getInterface()
	{
		return externalInterface;
	}

	/** Interface available to public */
	private VerticalPanel externalInterface=new VerticalPanelInterface();

	/** Interface available to public */
	class VerticalPanelInterface extends BasicWidget implements VerticalPanel,InternalWidget
	{
		@Override
		public int getContentType() { return CONTENT_UNNAMEDSLOTS; }

		@Override
		public JComponent getJComponent()
		{
			return VerticalPanelImp.this;
		}

		@Override
		public int getPreferredWidth()
		{
			// Find largest component width
			int width=0;
			synchronized(components)
			{
				for(InternalWidget iw : components)
				{
					if(!iw.isVisible()) continue;
					width=Math.max(width,iw.getPreferredWidth());
				}
			}

			return width+2*border;
		}

		@Override
		public int getPreferredHeight(int width)
		{
			if(width==0) return 0;

			int height=0;
			synchronized(components)
			{
				for(InternalWidget iw : components)
				{
					if(!iw.isVisible()) continue;
					height+=iw.getPreferredHeight(width-2*border);
					height+=spacing;
				}
			}
			height-=spacing; // Compensate for adding last space

			return height+2*border;
		}

		@Override
		public void setSpacing(final int spacing)
		{
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					VerticalPanelImp.this.spacing=spacing;
					revalidate();
					updateLayout();
				}
			});
		}

		@Override
		public void setBorder(final int border)
		{
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					VerticalPanelImp.this.border=border;
					revalidate();
					updateLayout();
				}
			});
		}

		@Override
		public void addXMLChild(String slotName, Widget child)
		{
			add(child);
		}

		@Override
		public void add(Widget w)
		{
			final InternalWidget iw=(InternalWidget)w;
			iw.setParent(this);
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					VerticalPanelImp.this.add(iw.getJComponent());
					synchronized(components)
					{
						components.add(iw);
					}
					revalidate();
					updateLayout();
				}
			});
		}

		@Override
		public Widget[] getWidgets()
		{
			List<Widget> all = new LinkedList<Widget>();
			synchronized(components)
			{
				for(InternalWidget w : components)
				{
					if(w != null)
					{
						all.add(w);
					}
				}
			}
			return all.toArray(new Widget[all.size()]);
		}

		@Override
		public void remove(Widget w)
		{
			final InternalWidget iw=(InternalWidget)w;
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					VerticalPanelImp.this.remove(iw.getJComponent());
					synchronized(components)
					{
						components.remove(iw);
					}
					revalidate();
					updateLayout();
				}
			});
		}

		@Override
		public void removeAll()
		{
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					InternalWidget[] aiw;
					synchronized(components)
					{
						aiw=components.toArray(new InternalWidget[components.size()]);
						components.clear();
					}
					for(int i=0;i<aiw.length;i++)
					{
						VerticalPanelImp.this.remove(aiw[i].getJComponent());
					}
					revalidate();
					updateLayout();
				}
			});
		}

		@Override
		public void redoLayout()
		{
			updateLayout();
			super.redoLayout();
		}
	};

}