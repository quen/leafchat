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

import javax.swing.*;

import util.PlatformUtils;

import com.leafdigital.ui.api.*;

import leafchat.core.api.BugException;

/** Combo box */
public class DropdownImp extends JComponent implements BaseGroup
{
	private String onSelectionChange;
	private boolean selecting;
	private String baseGroup=null;
	private JComboBox b=new JComboBox();
	private int topOffset=0;

	DropdownImp()
	{
		b.setOpaque(false);

		b.addItemListener(new ItemListener()
		{
			@Override
			public void itemStateChanged(ItemEvent ie)
			{
				if(ie.getStateChange()!=ItemEvent.SELECTED && onSelectionChange!=null && !selecting)
				{
					getInterface().getOwner().getCallbackHandler().callHandleErrors(onSelectionChange);
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

	private static class KeyWrapper
	{
		Object key;
		String value;
		@Override
		public String toString()
		{
			return value;
		}
		public KeyWrapper(Object key,String value)
		{
			this.key=key;
			this.value=value;
		}
	}

	/** @return Combo interface */
	public Dropdown getInterface() { return cInterface; }

	/** Combo interface */
	Dropdown cInterface=new ComboInterface();

	/** Class implementing combo interface */
	class ComboInterface extends BasicWidget implements Dropdown, InternalWidget
	{
		@Override
		public int getContentType() { return CONTENT_NONE; }

		@Override
		public void addXMLChild(String sSlotName, Widget wChild)
		{
			throw new BugException("Combos cannot contain children");
		}

		@Override
		public JComponent getJComponent()
		{
			return DropdownImp.this;
		}

		@Override
		public int getPreferredWidth()
		{
			return b.getPreferredSize().width;
		}

		@Override
		public int getPreferredHeight(int iWidth)
		{
			return b.getPreferredSize().height;
		}

		public String getValue()
		{
			String sValue=(String)b.getSelectedItem();
			if(sValue==null)
				return "";
			else
				return sValue;
		}

		@Override
		public void clear()
		{
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						selecting=true;
						b.removeAllItems();
					}
					finally
					{
						selecting=false;
					}
				}
			});
		}

		@Override
		public void addValue(Object key,String value)
		{
			final KeyWrapper wrapper=new KeyWrapper(key,value);
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						selecting=true;
						b.addItem(wrapper);
					}
					finally
					{
						selecting=false;
					}
				}
			});
		}

		@Override
		public Object getSelected()
		{
			KeyWrapper kw=(KeyWrapper)b.getSelectedItem();
			if(kw==null)
				return null;
			else
				return kw.key;
		}

		@Override
		public void setSelected(final Object key)
		{
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						selecting=true;
						for(int i=0;i<b.getItemCount();i++)
						{
							KeyWrapper kw=(KeyWrapper)b.getItemAt(i);
							if(kw.key==null)
							{
								if(key==null) b.setSelectedIndex(i);
							}
							else if(kw.key.equals(key))
							{
								b.setSelectedIndex(i);
							}
						}
					}
					finally
					{
						selecting=false;
					}
				}
			});
		}

		@Override
		public void setOnChange(String sCallback)
		{
			getInterface().getOwner().getCallbackHandler().check(sCallback);
			onSelectionChange=sCallback;
		}
		@Override
		public void setBaseGroup(String group)
		{
			if(baseGroup!=null)
			{
				BaseGroup.Updater.removeFromGroup(DropdownImp.this,baseGroup);
				baseGroup=null;
			}
			if(group!=null)
			{
				baseGroup=group;
				BaseGroup.Updater.addToGroup(DropdownImp.this,baseGroup);
			}
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
			bestGuess-=1;
		return bestGuess;
	}

	@Override
	public InternalWidgetOwner getInternalWidgetOwner()
	{
		return (InternalWidgetOwner)cInterface.getOwner();
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
