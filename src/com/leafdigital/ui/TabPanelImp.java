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

import java.awt.Dimension;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/**
 * Implements TabPanel.
 */
public class TabPanelImp extends JTabbedPane implements PageImp.PageTitleOwner
{
	/** Keep record of held components */
	private List<InternalWidget> components = new LinkedList<InternalWidget>();

	/** Border setting */
	private int border;

	/** Listener called on change */
	private String onChange;

	private boolean disableEvents;

	private static int widthOffset,heightOffset=-1;

	/** Constructor */
	TabPanelImp()
	{
		// Calculate how much size the tab panel bits take up
		if(heightOffset==-1)
		{
			JPanel p=new JPanel();
			p.setPreferredSize(new Dimension(100,100));
			JTabbedPane tp=new JTabbedPane();
			tp.addTab("My tab",p);
			heightOffset=tp.getPreferredSize().height-100;
			widthOffset=tp.getPreferredSize().width-100;
		}

		addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				if(onChange!=null && !disableEvents)
				{
					getInterface().getOwner().getCallbackHandler().callHandleErrors(onChange);
				}
			}
		});

	}

	/** @return The interface giving limited public access */
	TabPanel getInterface()
	{
		return externalInterface;
	}

	/** Interface available to public */
	private TabPanel externalInterface=new TabPanelInterface();

	/** Interface available to public */
	class TabPanelInterface extends BasicWidget implements TabPanel,InternalWidget
	{
		@Override
		public int getContentType() { return CONTENT_UNNAMEDSLOTS; }

		@Override
		public JComponent getJComponent()
		{
			return TabPanelImp.this;
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

			return iWidth+widthOffset+2*border;
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
					height=Math.max(height,iw.getPreferredHeight(width-widthOffset-2*border));
				}
			}

			return height+heightOffset+2*border;
		}

		@Override
		public void setBorder(final int border)
		{
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					TabPanelImp.this.border=border;
					TabPanelImp.this.setBorder(BorderFactory.createEmptyBorder(
						border,border,border,border));
				}
			});
		}

		@Override
		public void addXMLChild(String slotName, Widget child)
		{
			if(!(child instanceof Page)) throw new BugException(
			  "TabPanel <tab> may only contain <Page>");

			add((Page)child);
		}

		@Override
		public void add(final Page p)
		{
			((InternalWidget)p).setParent(this);
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					PageImp.PageInterface pi=(PageImp.PageInterface)p;
					TabPanelImp.this.addTab(p.getTitle(),pi.getJComponent());
					pi.informTitleChanges(TabPanelImp.this);
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
					TabPanelImp.this.remove(iw.getJComponent());
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
						TabPanelImp.this.remove(aiw[i].getJComponent());
					}
					repaint();
				}
			});
		}

		@Override
		public void display(String id) throws BugException
		{
			Widget w=getOwner().getWidget(id);
			if(w==null)
				throw new BugException("Can't find component "+id);
			try
			{
				setSelectedComponent(((InternalWidget)w).getJComponent());
			}
			catch(IllegalArgumentException iae)
			{
				throw new BugException("Component "+id+" is not within TabPanel");
			}
		}

		@Override
		public String getDisplayed()
		{
			java.awt.Component c=getSelectedComponent();
			synchronized(components)
			{
				for(InternalWidget iw : components)
				{
					if(iw.getJComponent()==c)
					{
						return iw.getID();
					}
				}
			}
			return null;
		}

		@Override
		public void setOnChange(String callback)
		{
			getOwner().getCallbackHandler().check(callback);
			onChange=callback;
		}
	}

	@Override
	public void pageTitleChanged(Page p)
	{
		int iIndex=indexOfComponent(((InternalWidget)p).getJComponent());
		if(iIndex!=-1)
			setTitleAt(iIndex,p.getTitle());
	};

}

