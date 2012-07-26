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

import java.awt.Graphics;
import java.awt.FontMetrics;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import util.PlatformUtils;

import com.leafdigital.ui.api.*;

import leafchat.core.api.BugException;

/**
 * Provides a basic text button.
 */
public class ButtonImp extends JComponent implements ActionListener,HierarchyListener,BaseGroup
{
	ButtonInterface bi=new ButtonInterface();
	private String onAction=null;
	private boolean isDefault;
	private String baseGroup=null;
	private JButton b=new JButton("Action"); // Default text needed for size calculation
	int topOffset=0;

	/**
	 * Constructs.
	 */
	public ButtonImp()
	{
		b.addActionListener(this);
		b.addHierarchyListener(this);
		b.setOpaque(false);
		add(b);
		addAncestorListener(new AncestorListener()
		{
		  @Override
			public void ancestorAdded(AncestorEvent ae)
		  {
		  		reallySetDefault();
		  }
		  @Override
			public void ancestorRemoved(AncestorEvent ae)
		  {
		  		reallySetDefault();
		  }
			@Override
			public void ancestorMoved(AncestorEvent arg0)
			{
			}
		});
	}

	@Override
	public void setBounds(int x,int y,int width,int height)
	{
		super.setBounds(x,y,width,height);
		relayout();
	}

	private void relayout()
	{
		int preferredHeight=b.getPreferredSize().height;
		b.setBounds(0,topOffset,getWidth(),preferredHeight);
	}

	Button getInterface() { return bi; }

	class ButtonInterface extends BasicWidget implements Button,InternalWidget
	{
		@Override
		public int getContentType() { return CONTENT_NONE; }

		@Override
		public void setLabel(String text)
		{
			b.setText(text);
		}

		@Override
		public void setOnAction(String callback)
		{
			getOwner().getCallbackHandler().check(callback);
			ButtonImp.this.onAction=callback;
		}

		@Override
		public JComponent getJComponent()
		{
			return ButtonImp.this;
		}

		@Override
		public int getPreferredWidth()
		{
			return b.getPreferredSize().width;
		}

		@Override
		public int getPreferredHeight(int width)
		{
			return b.getPreferredSize().height;
		}

		@Override
		public void addXMLChild(String slotName, Widget child)
		{
			throw new BugException("Buttons cannot contain children");
		}

		@Override
		public void setDefault(boolean isDefault)
		{
			ButtonImp.this.isDefault=isDefault;
			reallySetDefault();
		}

		@Override
		public void setEnabled(boolean enabled)
		{
			if(isEnabled()!=enabled)
				b.setEnabled(enabled);
		}

		@Override
		public boolean isEnabled()
		{
			return b.isEnabled();
		}

		@Override
		public void setBaseGroup(String group)
		{
			if(baseGroup!=null)
			{
				BaseGroup.Updater.removeFromGroup(ButtonImp.this,baseGroup);
				baseGroup=null;
			}
			if(group!=null)
			{
				baseGroup=group;
				BaseGroup.Updater.addToGroup(ButtonImp.this,baseGroup);
			}
		}

		@Override
		public void focus()
		{
			getUI().focus(b);
		}
	}

	private void reallySetDefault()
	{
		JRootPane rp=getRootPane();
		if(rp==null) return;
		if(!isShowing()) 	return;

		if(isDefault)
		{
			JButton def=rp.getDefaultButton();
			if(def!=b)
				rp.setDefaultButton(b);
		}
		else
		{
			JButton def=rp.getDefaultButton();
			if(def==b) rp.setDefaultButton(null);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		// Only send event if button is showing. It's just possible that queued
		// events happen after button was hidden (I think) - see #17
		if(onAction!=null && isShowing() && isEnabled())
		{
			getInterface().getOwner().getCallbackHandler().callHandleErrors(onAction);
		}
	}

	@Override
	public void hierarchyChanged(HierarchyEvent he)
	{
		if(
			((he.getChangeFlags() & HierarchyEvent.PARENT_CHANGED)!=0) ||
			((he.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED)!=0))
			reallySetDefault();
	}

	// BaseGroup methods
	////////////////////

	@Override
	public int getBaseline()
	{
		FontMetrics fm=b.getFontMetrics(b.getFont());
		int border=(b.getPreferredSize().height-fm.getHeight())/2;
		int bestGuess=border+fm.getAscent();
		if(PlatformUtils.isMac())
			bestGuess+=1;
		return bestGuess;
	}

	@Override
	public InternalWidgetOwner getInternalWidgetOwner()
	{
		return (InternalWidgetOwner)bi.getOwner();
	}

	@Override
	public void setTopOffset(int topOffset)
	{
		if(this.topOffset==topOffset) return;
		this.topOffset=topOffset;
		relayout();
	}

	// Debugging
	@Override
	protected void paintChildren(Graphics g)
	{
		super.paintChildren(g);
		BaseGroup.Debug.paint(g,this,topOffset);
	}
}
