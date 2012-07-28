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

import javax.swing.JComponent;

import util.PlatformUtils;

import com.leafdigital.ui.api.*;

import leafchat.core.api.BugException;

/**
 * Implements ButtonPanel
 */
public class ButtonPanelImp extends JComponent
{
	/** Spacing between buttons */
	private int spacing=0;

	/** Border at edge of grid */
	private int border=0;

	/** Held components */
	private InternalWidget[] slots=new InternalWidget[3];

	/** Constructor */
	ButtonPanelImp()
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
	public Dimension getPreferredSize()
	{
		InternalWidget iw=(InternalWidget)externalInterface;
	  int width=iw.getPreferredWidth();
	  return new Dimension(width,iw.getPreferredHeight(width));
	}

	/** Move all components to their correct place in new layout */
	private void updateLayout()
	{
		UISingleton.checkSwing();

		// Align buttons right in the available space
		int x=getWidth()-border-((InternalWidget)externalInterface).getPreferredWidth();

		int[] order=	getButtonOrder();
		for(int i=0;i<order.length;i++)
		{
			if(order[i]==-1)
			{
				x+=2*spacing;
				continue;
			}

			InternalWidget iw=slots[order[i]];
			if(iw==null) continue;

			int thisWidth=iw.getPreferredWidth();
			iw.getJComponent().setBounds(x,border,thisWidth,iw.getPreferredHeight(thisWidth));
			x+=thisWidth+spacing;
		}

		repaint();
	}

	/**
	 * Obtains the necessary order for buttons (array of the YES/NO/CANCEL
	 * constants) depending on platform.
	 * @return Array of those three constants in the order to display buttons
	 */
	private int[] getButtonOrder()
	{
		return PlatformUtils.isWindows()
			? new int[] {ButtonPanel.YES,ButtonPanel.NO,ButtonPanel.CANCEL}
			: new int[] {ButtonPanel.NO,-1,ButtonPanel.CANCEL,ButtonPanel.YES};
	}

	/**
	 * @return Interface giving limited public access
	 */
	ButtonPanel getInterface()
	{
		return externalInterface;
	}

	/** Interface available to public */
	private ButtonPanel externalInterface=new ButtonPanelInterface();

	/** Interface available to public */
	class ButtonPanelInterface extends BasicWidget implements ButtonPanel,InternalWidget
	{
		@Override
		public int getContentType() { return CONTENT_NAMEDSLOTS; }

		@Override
		public void set(final int iSlot,Widget w)
		{
			final InternalWidget iw=(InternalWidget)w;
			iw.setParent(this);
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					if(slots[iSlot]!=null)
					{
						ButtonPanelImp.this.remove(slots[iSlot].getJComponent());
						slots[iSlot]=null;
					}
					if(iw!=null)
					{
						add(iw.getJComponent());
						slots[iSlot]=iw;
					}
					updateLayout();
				}
			});
		}

		@Override
		public Widget[] getWidgets()
		{
			List<Widget> all = new LinkedList<Widget>();
			for(InternalWidget w : slots)
			{
				if(w != null)
				{
					all.add(w);
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
					for(int i=0;i<slots.length;i++)
					{
						if(slots[i]==iw)
						{
							slots[i]=null;
							ButtonPanelImp.this.remove(iw.getJComponent());
						}
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
					for(int i=0;i<slots.length;i++)
					{
						if(slots[i]!=null)
						{
							InternalWidget iw=slots[i];
							slots[i]=null;
							ButtonPanelImp.this.remove(iw.getJComponent());
						}
					}
					updateLayout();
				}
			});
		}

		@Override
		public JComponent getJComponent()
		{
			return ButtonPanelImp.this;
		}

		@Override
		public int getPreferredWidth()
		{
			int width=0;
			int[] order=getButtonOrder();
			boolean somethingYet=false;
			for(int i=0;i<order.length;i++)
			{
				if(order[i]==-1)
				{
					if(somethingYet) // Only do a spacer after something...
					{
						boolean somethingAfter=false;
						for(int j=i+1;j<order.length;j++)
						{
							if(slots[order[j]]!=null)
							{
								somethingAfter=true;
								break;
							}
						}
						if(somethingAfter) // And before something
							width+=spacing;
					}
				}
				else
				{
					InternalWidget iw=slots[order[i]];
					if(iw!=null)
					{
						if(i!=0) width+=spacing;
						width+=iw.getPreferredWidth();
					}
				}
			}
			return width+2*border;
		}

		@Override
		public int getPreferredHeight(int width)
		{
			if(width==0) return 0;
			int height=0;
			for(int i=0;i<slots.length;i++)
			{
				if(slots[i]!=null)
				{
					height=Math.max(height,slots[i].getPreferredHeight(slots[i].getPreferredWidth()));
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
					ButtonPanelImp.this.spacing=spacing;
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
					ButtonPanelImp.this.border=border;
					updateLayout();
				}
			});
		}

		@Override
		public void addXMLChild(String slotName, Widget child)
		{
			int slot;
			if(slotName.equals("yes")) slot=YES;
			else if(slotName.equals("no")) slot=NO;
			else if(slotName.equals("cancel")) slot=CANCEL;
			else throw new BugException(
			  "Slot name invalid, expecting 'yes', 'no', or 'cancel': "+slotName);

			set(slot,child);
		}

		@Override
		public void redoLayout()
		{
			updateLayout();
			super.redoLayout();
		}
	};

}

