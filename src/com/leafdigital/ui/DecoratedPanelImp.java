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

import javax.swing.JComponent;

import com.leafdigital.ui.api.*;

/**
 * Implements DecoratedPanel
 */
public class DecoratedPanelImp extends JComponent
{
	private int top,right,bottom,left;
	private InternalWidget child;
	private int border=0;
	private String onPaint=null;

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		if(onPaint!=null)
		{
			externalInterface.getOwner().getCallbackHandler().call(onPaint,
				g, border, border, getWidth()-2*border, getHeight()-2*border);
		}
	}

	@Override
	public void setBounds(int x, int y, int width, int height)
	{
		super.setBounds(x, y, width, height);
		updateLayout();
	}

	/** Move components to their correct place in new layout */
	private void updateLayout()
	{
		UISingleton.checkSwing();
		if(child==null) return;

		child.getJComponent().setBounds(border+left,border+top,
			getWidth()-border*2-left-right,getHeight()-border*2-top-bottom);
		repaint();
	}

	/** @return Interface giving limited public access */
	DecoratedPanel getInterface()
	{
		return externalInterface;
	}

	/** Interface available to public */
	private DecoratedPanel externalInterface=new DecoratedPanelInterface();

	/** Interface available to public */
	class DecoratedPanelInterface extends BasicWidget implements DecoratedPanel,InternalWidget
	{
		@Override
		public int getContentType() { return CONTENT_SINGLE; }

		@Override
		public JComponent getJComponent()
		{
			return DecoratedPanelImp.this;
		}

		@Override
		public int getPreferredWidth()
		{
			int w=border*2+left+right;
			if(child!=null) w+=child.getPreferredWidth();
			return w;
		}
		@Override
		public int getPreferredHeight(int width)
		{
			int h=border*2+top+bottom;
			if(child!=null) h+=child.getPreferredHeight(width-border*2-left-right);
			return h;
		}

		@Override
		public void setBorder(final int border)
		{
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					DecoratedPanelImp.this.border=border;
					revalidate();
					updateLayout();
				}
			});
		}

		@Override
		public void addXMLChild(String slotName, Widget child)
		{
			set(child);
		}

		public void set(Widget w)
		{
			final InternalWidget iw=(InternalWidget)w;
			if(iw!=null) iw.setParent(this);
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					if(iw==null)
					{
						DecoratedPanelImp.this.removeAll();
						DecoratedPanelImp.this.child=null;
					}
					else
					{
						DecoratedPanelImp.this.add(iw.getJComponent());
						DecoratedPanelImp.this.child=iw;
					}
					revalidate();
					updateLayout();
				}
			});
		}

		@Override
		public Widget[] getWidgets()
		{
			if(child == null)
			{
				return new Widget[0];
			}
			else
			{
				return new Widget[] { child };
			}
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
		public void setOnPaint(String callback)
		{
			getOwner().getCallbackHandler().check(callback,new Class[] {
				Graphics2D.class,int.class,int.class,int.class,int.class});
			onPaint=callback;
		}

		@Override
		public void setPadding(int top,int right,int bottom,int left)
		{
			DecoratedPanelImp.this.top=top;
			DecoratedPanelImp.this.right=right;
			DecoratedPanelImp.this.bottom=bottom;
			DecoratedPanelImp.this.left=left;
		}

		@Override
		public void repaint()
		{
			DecoratedPanelImp.this.repaint();
		}

		@Override
		public void redoLayout()
		{
			updateLayout();
			super.redoLayout();
		}
	};
}
