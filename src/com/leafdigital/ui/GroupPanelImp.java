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

import java.awt.Insets;

import javax.swing.*;

import com.leafdigital.ui.api.*;

import leafchat.core.api.BugException;

/**
 * Implements GroupPanel
 */
public class GroupPanelImp extends JComponent
{
	/** Border inside panel */
	private int internalBorder=0;

	/** Border at edge of panel */
	private int border=0;

	/** Contained component */
	private InternalWidget contents;

	/** Current title */
	private String title;

	/** True if Swing border matches current settings */
	private boolean borderCurrent;

	/** Constructor */
	GroupPanelImp()
	{
		setLayout(null);
	}

	/* (non-Javadoc)
	 * @see java.awt.Component#setBounds(int, int, int, int)
	 */
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

		if(!borderCurrent)
		{
			// Handle the way that on Windows, borders inside group panels are
			// blatantly wrong
			int iModifiedSurround=internalBorder,iModifiedTop=internalBorder;
			if(System.getProperty("os.name").startsWith("Windows"))
			{
				iModifiedSurround=internalBorder-2;
				iModifiedTop=internalBorder-6;
			}

			setBorder(
				BorderFactory.createCompoundBorder(
					BorderFactory.createCompoundBorder(
						BorderFactory.createEmptyBorder(border,border,border,border),
						BorderFactory.createTitledBorder(title)),
					BorderFactory.createEmptyBorder(
						iModifiedTop,iModifiedSurround,iModifiedSurround,iModifiedSurround)));
			borderCurrent=true;
		}

		if(contents!=null)
		{
			Insets i=getInsets();
			contents.getJComponent().setBounds(
				i.left,i.top,getWidth()-i.left-i.right,getHeight()-i.top-i.bottom);
		}
	}

	/**
	 * @return Interface giving limited public access
	 */
	GroupPanel getInterface()
	{
		return externalInterface;
	}

	/** Interface available to public */
	private GroupPanel externalInterface=new GroupPanelInterface();

	/** Interface available to public */
	class GroupPanelInterface extends BasicWidget implements GroupPanel,InternalWidget
	{
		@Override
		public int getContentType() { return CONTENT_SINGLE; }

		@Override
		public JComponent getJComponent()
		{
			return GroupPanelImp.this;
		}

		@Override
		public int getPreferredWidth()
		{
			Insets i=getInsets();
			if(contents==null)
				return i.left+i.right;
			else
				return contents.getPreferredWidth()+i.left+i.right;
		}

		@Override
		public int getPreferredHeight(int iWidth)
		{
			if(iWidth==0) return 0;

			Insets i=getInsets();
			if(contents==null)
			  return i.top+i.bottom;
			else
				return contents.getPreferredHeight(iWidth -(i.left+i.right))+
			    (i.top+i.bottom);
		}

		@Override
		public void setInternalBorder(final int iInternalBorder)
		{
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					GroupPanelImp.this.internalBorder=iInternalBorder;
					borderCurrent=false;
					updateLayout();
				}
			});
		}

		@Override
		public void setBorder(final int iBorder)
		{
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					GroupPanelImp.this.border=iBorder;
					borderCurrent=false;
					updateLayout();
				}
			});
		}

		@Override
		public void addXMLChild(String sSlotName, Widget wChild)
		{
			if(contents!=null) throw new BugException(
			  "May contain only a single <contents> child.");

			set(wChild);
		}

		@Override
		public void setTitle(final String sTitle)
		{
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
				  GroupPanelImp.this.title=sTitle;
					borderCurrent=false;
					updateLayout();
				}
			});

		}

		@Override
		public void set(Widget w)
		{
			final InternalWidget iw=(InternalWidget)w;
			iw.setParent(this);
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					if(contents!=null)
					{
						GroupPanelImp.this.remove(contents.getJComponent());
						contents=null;
					}
					if(iw!=null)
					{
						GroupPanelImp.this.add(iw.getJComponent());
						contents=iw;
					}
					updateLayout();
				}
			});
		}

		@Override
		public Widget[] getWidgets()
		{
			if(contents == null)
			{
				return new Widget[0];
			}
			else
			{
				return new Widget[] { contents };
			}
		}

		@Override
		public void remove(Widget w)
		{
			if(w==contents) set(null);
		}
		@Override
		public void removeAll()
		{
			set(null);
		}
	};

}

