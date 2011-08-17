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
import java.beans.*;

import javax.swing.*;

import com.leafdigital.ui.api.*;

import leafchat.core.api.BugException;

/**
 * Implements SplitPanel
 */
public class SplitPanelImp extends JSplitPane
{
	/** One of the SIDE_xxx constants */
	private int side;

	/** Size in pixels */
	private int splitSize;

	/** Widgets inside */
	private InternalWidget main,split;

	/** Constructs. */
	public SplitPanelImp()
	{
		externalInterface.setSide(SplitPanel.SIDE_EAST);
		externalInterface.setSplitSize(100);
		setBorder(BorderFactory.createEmptyBorder());
		addPropertyChangeListener(DIVIDER_LOCATION_PROPERTY,new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				switch(side)
				{
				case SplitPanel.SIDE_NORTH:
				case SplitPanel.SIDE_WEST:
					splitSize=getDividerLocation();
					break;
				case SplitPanel.SIDE_EAST:
					splitSize=getWidth()-getDividerLocation();
					// Swing doesn't let people set 0px. Argh! This handles it by
					// making 1 -> 0.
					if(splitSize==getDividerSize()+1) setDividerLocation(getWidth()-getDividerSize());
					break;
				case SplitPanel.SIDE_SOUTH:
					splitSize=getHeight()-getDividerLocation();
					break;
				}
			}
		});
		setOneTouchExpandable(true);
	}

	/**
	 * @return Interface giving limited public access
	 */
	SplitPanel getInterface()
	{
		return externalInterface;
	}

	@Override
	public void setBounds(int x,int y,int width,int height)
	{
		boolean changed=width!=getWidth();
		int splitSizeBefore=splitSize;
		super.setBounds(x,y,width,height);
		if(changed) externalInterface.setSplitSize(splitSizeBefore);
	}

	/** Interface available to public */
	private SplitPanel externalInterface=new SplitPanelInterface();

	/** Interface available to public */
	class SplitPanelInterface extends BasicWidget implements SplitPanel,InternalWidget
	{
		@Override
		public int getContentType() { return CONTENT_NAMEDSLOTS; }

		@Override
		public void setMain(Widget w)
		{
			final InternalWidget iw=(InternalWidget)w;
			iw.setParent(this);
			main=iw;
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					JComponent jc=(iw==null ? null : iw.getJComponent());
					switch(side)
					{
					case SIDE_NORTH:	setBottomComponent(jc); break;
					case SIDE_EAST:	setLeftComponent(jc); break;
					case SIDE_SOUTH:	setTopComponent(jc); break;
					case SIDE_WEST:	setRightComponent(jc); break;
					}
				}
			});
		}

		@Override
		public void setSplit(Widget w)
		{
			final InternalWidget iw=(InternalWidget)w;
			iw.setParent(this);
			split=iw;
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					JComponent jc=(iw==null ? null : iw.getJComponent());
					if(jc.getPreferredSize().width==0)
						jc.setPreferredSize(	new Dimension(iw.getPreferredWidth(),
							iw.getPreferredHeight(iw.getPreferredWidth())));
					switch(side)
					{
					case SIDE_NORTH:	setTopComponent(jc); break;
					case SIDE_EAST:	setRightComponent(jc); break;
					case SIDE_SOUTH:	setBottomComponent(jc); break;
					case SIDE_WEST:	setLeftComponent(jc); break;
					}
				}
			});
		}

		@Override
		public void setSide(final int side)
		{
			SplitPanelImp.this.side=side;
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					switch(side)
					{
						case SIDE_NORTH:
							setOrientation(JSplitPane.VERTICAL_SPLIT);
							setResizeWeight(0.0);
							break;
						case SIDE_EAST:
							setOrientation(JSplitPane.HORIZONTAL_SPLIT);
							setResizeWeight(1.0);
							break;
						case SIDE_SOUTH:
							setOrientation(JSplitPane.VERTICAL_SPLIT);
							setResizeWeight(1.0);
							break;
						case SIDE_WEST:
							setOrientation(JSplitPane.HORIZONTAL_SPLIT);
							setResizeWeight(0.0);
							break;
						default:
							throw new IllegalArgumentException("Side value not supported");
					}
				}
			});
		}

		@Override
		public void setSplitSize(final int size)
		{
			SplitPanelImp.this.splitSize=size;
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					if(getWidth()==0) return; // Don't bother
					switch(side)
					{
					case SIDE_NORTH:
					case SIDE_WEST:
						setDividerLocation(size);
						break;
					case SIDE_EAST:
						if(splitSize==getDividerSize()+1)
							splitSize=getDividerSize();
						setDividerLocation(getWidth()-splitSize);
						break;
					case SIDE_SOUTH:
						setDividerLocation(getHeight()-size);
						break;
					}
				}
			});
		}

		@Override
		public int getSplitSize()
		{
			return splitSize;
		}

		@Override
		public JComponent getJComponent()
		{
			return SplitPanelImp.this;
		}

		@Override
		public int getPreferredWidth()
		{
			int iMainWidth=(main==null || !main.isVisible() ? 0 : main.getPreferredWidth());
			int iSplitWidth=(split==null || !split.isVisible() ? 0 : split.getPreferredWidth());
			switch(side)
			{
			case SIDE_NORTH:
			case SIDE_SOUTH:
				return Math.max(iMainWidth,iSplitWidth);
			case SIDE_EAST:
			case SIDE_WEST:
				return iMainWidth+splitSize+getDividerSize();
			default:
				throw new Error("Invalid side value");
			}
		}

		@Override
		public int getPreferredHeight(int width)
		{
			if(width==0) return 0;

			switch(side)
			{
			case SIDE_NORTH:
			case SIDE_SOUTH:
				return splitSize+getDividerSize()+
				  (main==null || !main.isVisible() ? 0 : main.getPreferredHeight(width));
			case SIDE_EAST:
			case SIDE_WEST:
				return Math.max(
					(main==null || !main.isVisible() ? 0 : main.getPreferredHeight(width-splitSize)),
					(split==null || !split.isVisible() ? 0 : split.getPreferredHeight(splitSize)));
			default:
				throw new Error("Invalid side value");
			}
		}

		@Override
		public void addXMLChild(String slotName, Widget child)
		{
			if(slotName.equals("main")) setMain(child);
			else if(slotName.equals("split")) setSplit(child);
			else throw new BugException(
			  "Slot name invalid, expecting 'split' or 'main': "+slotName);
		}

		@Override
		public void setBorder(final int border)
		{
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					SplitPanelImp.this.setBorder(
						BorderFactory.createEmptyBorder(border,border,border,border));
				}
			});
		}

		@Override
		public void remove(Widget c)
		{
			if(c==main) setMain(null);
			else if(c==split) setSplit(null);
		}
		@Override
		public void removeAll()
		{
			setMain(null);
			setSplit(null);
		}
	};

}

