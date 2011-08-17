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

import java.awt.*;
import java.awt.event.*;
import java.util.Set;

import javax.swing.*;

import util.PlatformUtils;

import com.leafdigital.ui.api.*;

import leafchat.core.api.BugException;

/** Provides a basic text button */
public class RadioButtonImp extends JComponent implements ActionListener,BaseGroup
{
	private RadioButtonInterface rbi=new RadioButtonInterface();
	private String onAction=null;
	private String widthGroup,baseGroup;
	private JRadioButton b=new MyRadioButton();
	private int topOffset=0;

	class MyRadioButton extends JRadioButton
	{
		public MyRadioButton()
		{
			super("Option");
		}
		RadioButton getInterface()
		{
			return rbi;
		}
	}

	/**
	 * Constructs.
	 */
	public RadioButtonImp()
	{
		b.addActionListener(this);
		b.setOpaque(false);
		add(b);
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

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if(onAction!=null)
		{
			getInterface().getOwner().getCallbackHandler().callHandleErrors(onAction);
		}
	}

	RadioButton getInterface() { return rbi; }

	private String group=null;

	class RadioButtonInterface extends BasicWidget implements RadioButton,InternalWidget
	{
		@Override
		public int getContentType() { return CONTENT_NONE; }

		@Override
		public void setLabel(String sText)
		{
			b.setText(sText);
		}

		@Override
		public JComponent getJComponent()
		{
			return RadioButtonImp.this;
		}

		@Override
		public int getPreferredWidth()
		{
			int inner=getInnerPreferredWidth();
			if(widthGroup==null)	return inner;

			Set<BaseGroup> s = getWidthGroup();
			synchronized(s)
			{
				// Need to convert to array or we get commodification
				RadioButtonImp[] labels = s.toArray(new RadioButtonImp[s.size()]);
				for(int i=0;i<labels.length;i++)
				{
					inner=Math.max(inner,
						((RadioButtonInterface)labels[i].getInterface()).getInnerPreferredWidth());
				}
			}
			return inner;
		}

		private int getInnerPreferredWidth()
		{
			return b.getPreferredSize().width;
		}

		@Override
		public int getPreferredHeight(int iWidth)
		{
			return b.getPreferredSize().height;
		}

		@Override
		public void addXMLChild(String sSlotName, Widget wChild)
		{
			throw new BugException("Radio buttons cannot contain children");
		}

		@Override
		public boolean isSelected()
		{
			return b.isSelected();
		}

		@Override
		public void setGroup(String newGroup)
		{
			if((newGroup!=null && newGroup.equals(group)) || (newGroup==null && group==null))
				return;
			if(group!=null)
			{
				((InternalWidgetOwner)getOwner()).getButtonGroup(group).remove(b);
			}
			if(newGroup!=null)
			{
				((InternalWidgetOwner)getOwner()).getButtonGroup(newGroup).add(b);
			}
			group=newGroup;
		}

		@Override
		public void setSelected()
		{
			b.setSelected(true);
		}

		@Override
		public void setOnAction(String callback)
		{
			getInterface().getOwner().getCallbackHandler().check(callback);
			onAction=callback;
		}

		@Override
		public void setBaseGroup(String group)
		{
			if(baseGroup!=null)
			{
				BaseGroup.Updater.removeFromGroup(RadioButtonImp.this,baseGroup);
				baseGroup=null;
			}
			if(group!=null)
			{
				baseGroup=group;
				BaseGroup.Updater.addToGroup(RadioButtonImp.this,baseGroup);
			}
		}

		@Override
		public void setWidthGroup(String group)
		{
			RadioButtonImp.this.widthGroup=group;
			getWidthGroup().add(RadioButtonImp.this);
		}

		@Override
		public boolean isEnabled()
		{
			return b.isEnabled();
		}

		@Override
		public void setEnabled(boolean enabled)
		{
			b.setEnabled(enabled);
		}
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
		return (InternalWidgetOwner)rbi.getOwner();
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

	private Set<BaseGroup> getWidthGroup()
	{
		return ((InternalWidgetOwner)rbi.getOwner()).getArbitraryGroup("width\n"+widthGroup);
	}
}
