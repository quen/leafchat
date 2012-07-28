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

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import com.leafdigital.ui.api.*;

import leafchat.core.api.BugException;

/**
 * Implements ChoicePanel.
 */
public class ChoicePanelImp extends JPanel
{
	/** Keep record of held components */
	private List<InternalWidget> components = new LinkedList<InternalWidget>();

	/** Border setting */
	private int border;

	/** Card layout */
	private CardLayout cl;

	/**
	 * Constructor.
	 */
	public ChoicePanelImp()
	{
		cl=new CardLayout();
		setLayout(cl);
		setOpaque(false);
	}

	/** @return Interface giving limited public access */
	ChoicePanel getInterface()
	{
		return externalInterface;
	}

	/** Interface available to public */
	private ChoicePanel externalInterface=new ChoicePanelInterface();

	/** Interface available to public */
	class ChoicePanelInterface extends BasicWidget implements ChoicePanel,InternalWidget
	{
		private String current;

		@Override
		public int getContentType() { return CONTENT_UNNAMEDSLOTS; }

		@Override
		public JComponent getJComponent()
		{
			return ChoicePanelImp.this;
		}

		@Override
		public int getPreferredWidth()
		{
			// Find largest component width
			int iWidth=0;
			synchronized(components)
			{
				for(InternalWidget iw : components)
				{
					iWidth=Math.max(iWidth,iw.getPreferredWidth());
				}
			}

			return iWidth+2*border;
		}

		@Override
		public int getPreferredHeight(int width)
		{
			if(width==0) return 0;

			int iHeight=0;
			synchronized(components)
			{
				for(InternalWidget iw : components)
				{
					iHeight=Math.max(iHeight,iw.getPreferredHeight(width-2*border));
				}
			}

			return iHeight+2*border;
		}

		@Override
		public void setBorder(final int border)
		{
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					ChoicePanelImp.this.border=border;
					ChoicePanelImp.this.setBorder(BorderFactory.createEmptyBorder(
						border,border,border,border));
				}
			});
		}

		@Override
		public void addXMLChild(String slotName, Widget child)
		{
			if(!(child instanceof Page)) throw new BugException(
			  "<ChoicePanel> may only contain <Page>");

			add((Page)child);
		}

		@Override
		public void add(final Page p)
		{
			((InternalWidget)p).setParent(this);
			final PageImp.PageInterface pi=(PageImp.PageInterface)p;
			if(current==null)
			{
				current = pi.getID();
			}
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					ChoicePanelImp.this.add(pi.getJComponent(),pi.getID());
					synchronized(components)
					{
						components.add(pi);
					}
					repaint();
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
					ChoicePanelImp.this.remove(iw.getJComponent());
					synchronized(components)
					{
						components.remove(iw);
					}
					repaint();
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
						ChoicePanelImp.this.remove(aiw[i].getJComponent());
					}
					repaint();
				}
			});
		}

		@Override
		public void display(String id)
		{
			current=id;
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					cl.show(ChoicePanelImp.this,current);
				}
			});
		}

		@Override
		public String getDisplayed()
		{
			return current;
		}
	}
}

