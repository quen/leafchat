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

import java.util.*;

import javax.swing.JComponent;

import com.leafdigital.ui.api.*;

/**
 * Implements HorizontalPanel
 */
public class HorizontalPanelImp extends JComponent
{
	/** Spacing between grid squares */
	private int spacing=0;

	/** Border at edge of grid */
	private int border=0;

	/** Keep record of held components */
	private List<InternalWidget> components = new LinkedList<InternalWidget>();

	/** Constructor */
	HorizontalPanelImp()
	{
		setLayout(null);
	}

	@Override
	public void setBounds(int x, int y, int width, int height)
	{
		super.setBounds(x, y, width, height);
		updateLayout();
	}

	@Override
	public void validate()
	{
		super.validate();
		updateLayout();
	}

	/** Move all components to their correct place in new layout */
	private void updateLayout()
	{
		UISingleton.checkSwing();

		int x=border;

		// Go through components laying them out
		synchronized(components)
		{
			for(InternalWidget iw : components)
			{
				if(!iw.isVisible()) continue;

				int thisWidth=iw.getPreferredWidth();
				//int iWidgetHeight=iw.getPreferredHeight(iThisWidth);

				iw.getJComponent().setBounds(x,border,thisWidth,getHeight()-2*border);
				x+=thisWidth;
				x+=spacing;
			}
		}

		repaint();
	}

	/**
	 * @return Interface giving limited public access
	 */
	HorizontalPanel getInterface()
	{
		return externalInterface;
	}

	/** Interface available to public */
	private HorizontalPanel externalInterface=new HorizontalPanelInterface();

	/** Interface available to public */
	class HorizontalPanelInterface extends BasicWidget implements HorizontalPanel,InternalWidget
	{
		@Override
		public int getContentType() { return CONTENT_UNNAMEDSLOTS; }

		@Override
		public JComponent getJComponent()
		{
			return HorizontalPanelImp.this;
		}

		@Override
		public int getPreferredWidth()
		{
			int width=0;
			synchronized(components)
			{
				for(InternalWidget iw : components)
				{
					if(!iw.isVisible()) continue;
					width+=iw.getPreferredWidth();
					width+=spacing;
				}
			}
			width-=spacing; // Compensate for adding last space

			return width+2*border;
		}

		@Override
		public int getPreferredHeight(int width)
		{
			// Find largest component height
			int height=0;
			synchronized(components)
			{
				for(InternalWidget iw : components)
				{
					if(!iw.isVisible()) continue;
					int thisHeight=iw.getPreferredHeight(iw.getPreferredWidth());
					height=Math.max(height,thisHeight);
				}
			}

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
					HorizontalPanelImp.this.spacing=spacing;
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
					HorizontalPanelImp.this.border=border;
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
					HorizontalPanelImp.this.add(iw.getJComponent());
					synchronized(components)
					{
						components.add(iw);
					}
					updateLayout();
				}
			});
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
					HorizontalPanelImp.this.remove(iw.getJComponent());
					synchronized(components)
					{
						components.remove(iw);
					}
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
						HorizontalPanelImp.this.remove(aiw[i].getJComponent());
					}
					updateLayout();
				}
			});
		}
	};

}

