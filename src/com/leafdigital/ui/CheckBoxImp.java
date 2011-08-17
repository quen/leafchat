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

import javax.swing.*;
import javax.swing.event.*;

import util.PlatformUtils;

import com.leafdigital.ui.api.*;

import leafchat.core.api.BugException;

/** Provides a basic text button */
public class CheckBoxImp extends JComponent implements BaseGroup
{
	private String onChange=null;
	private String baseGroup=null;
	private JCheckBox b=new JCheckBox("Option");
	int topOffset=0;

	CheckBoxInterface rbi=new CheckBoxInterface();

	CheckBox getInterface() { return rbi; }

	private boolean lastReported;

	CheckBoxImp()
	{
		b.setOpaque(false);

		b.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				if(onChange!=null && b.isSelected()!=lastReported)
				{
					lastReported=b.isSelected();
					getInterface().getOwner().getCallbackHandler().callHandleErrors(onChange);
				}
			}
		});
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

	class CheckBoxInterface extends BasicWidget implements CheckBox,InternalWidget
	{
		@Override
		public int getContentType() { return CONTENT_NONE; }

		@Override
		public void setLabel(String text)
		{
			b.setText(text);
		}

		@Override
		public void setOnChange(String onChange)
		{
			getInterface().getOwner().getCallbackHandler().check(onChange);
			CheckBoxImp.this.onChange=onChange;
		}

		@Override
		public JComponent getJComponent()
		{
			return CheckBoxImp.this;
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
			throw new BugException("Radio buttons cannot contain children");
		}

		@Override
		public void setChecked(boolean checked)
		{
			lastReported=checked;
			b.setSelected(checked);
		}

		@Override
		public boolean isChecked()
		{
			return b.isSelected();
		}

		@Override
		public void setEnabled(boolean enabled)
		{
			if(b.isEnabled()!=enabled)
				b.setEnabled(enabled);
		}

		@Override
		public void setBaseGroup(String group)
		{
			if(baseGroup!=null)
			{
				BaseGroup.Updater.removeFromGroup(CheckBoxImp.this,baseGroup);
				baseGroup=null;
			}
			if(group!=null)
			{
				baseGroup=group;
				BaseGroup.Updater.addToGroup(CheckBoxImp.this,baseGroup);
			}
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
}
