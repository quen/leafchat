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
import java.lang.reflect.Array;

import javax.swing.*;
import javax.swing.event.*;

import com.leafdigital.ui.api.*;

import leafchat.core.api.BugException;

/** Combo box */
public class ListBoxImp extends JPanel implements ThemeListener
{
	private JList l;
	private boolean sort;
	private int macIndent;
	private DefaultListModel dlm;
	private JScrollPane scrollPane;

	private String onAction,onSelectionChange,onMenu;

	private boolean selecting=false;

	ListBoxImp(UISingleton owner)
	{
		super(null);
		setOpaque(false);
		owner.informThemeListener(this);

		dlm=new DefaultListModel();
		l=new JList(dlm);

		scrollPane = new JScrollPane(l, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(scrollPane);

		outsideInterface.setMultiSelect(false);

		l.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent me)
			{
				if(me.getClickCount()==2 && onAction!=null)
				{
					getInterface().getOwner().getCallbackHandler().callHandleErrors(onAction);
				}
			}
			@Override
			public void mousePressed(MouseEvent e)
			{
				if(e.isPopupTrigger())
				{
					doMenu(e);
				}
			}
			@Override
			public void mouseReleased(MouseEvent e)
			{
				if(e.isPopupTrigger())
				{
					doMenu(e);
				}
			}
		});
		l.addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent lse)
			{
				if(onSelectionChange!=null && !lse.getValueIsAdjusting() && !selecting)
				{
					getInterface().getOwner().getCallbackHandler().callHandleErrors(onSelectionChange);
				}
			}
		});
		resetFont();
		l.setCellRenderer(new Renderer());
	}

	@Override
	public void setBounds(int x, int y, int width, int height)
	{
		super.setBounds(x, y, width, height);
		Insets i = getInsets();
		scrollPane.setBounds(i.left + macIndent, i.top,
			width - macIndent - i.left - i.right, height - i.top - i.bottom);
	}

	@Override
	public Dimension getPreferredSize()
	{
		Dimension size = new Dimension(scrollPane.getPreferredSize());
		size.width += macIndent;
		return size;
	}

	private class Renderer extends JLabel implements ListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(JList list,Object value,
			int index,boolean isSelected,boolean cellHasFocus)
		{
			ListItem li=(ListItem)value;
			if(li.bold)
			{
				if(getFont()!=currentBoldFont) setFont(currentBoldFont);
			}
			else
			{
				if(getFont()!=currentFont) setFont(currentFont);
			}
			setText(li.s);
			if(isSelected && l.isEnabled())
			{
				setForeground(l.getSelectionForeground());
				setBackground(l.getSelectionBackground());
				setOpaque(true);
			}
			else if(li.faint || !l.isEnabled())
			{
				Color original=l.getForeground();
				setForeground(new Color(original.getRed(),original.getGreen(),original.getBlue(),128));
				setBackground(l.getBackground());
			}
			else
			{
				setForeground(l.getForeground());
				setBackground(l.getBackground());
			}
			if(cellHasFocus)
			{
				setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.gray));
			}
			else
			{
				setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
			}
			return this;
		}
	}

	/** @return List interface */
	public ListBox getInterface() { return outsideInterface; }

	/** Combo interface */
	private ListBox outsideInterface=new ListBoxInterface();

	private static class ListItem
	{
		String s;
		Object data;
		boolean bold,faint;
		public ListItem(String s,Object data)
		{
			this.s=s;
			this.data=data;
		}
		@Override
		public String toString()
		{
			return s;
		}
	}

	/** Class implementing combo interface */
	class ListBoxInterface extends BasicWidget implements ListBox, InternalWidget
	{
		int width=-1;

		@Override
		public int getContentType() { return CONTENT_NONE; }

		@Override
		public void addXMLChild(String slotName, Widget child)
		{
			throw new BugException("Combos cannot contain children");
		}

		@Override
		public JComponent getJComponent()
		{
			return ListBoxImp.this;
		}

		@Override
		public int getPreferredWidth()
		{
			if(width==-1)
				return getPreferredSize().width;
			else
				return width;
		}

		@Override
		public int getPreferredHeight(int iWidth)
		{
			return getPreferredSize().height;
		}

		@Override
		public String getSelected()
		{
			ListItem li=(ListItem)l.getSelectedValue();
			return li==null ? null : li.s;
		}

		@Override
		public Object getSelectedData()
		{
			ListItem li=(ListItem)l.getSelectedValue();
			return li==null ? null : li.data;
		}

		@Override
		public void setSort(boolean b)
		{
			sort=b;
		}

		@Override
		public void addItem(String s)
		{
			addItem(s,s);
		}

		@Override
		public void addItem(String s,Object data)
		{
			if(!sort)
			{
				try
				{
					selecting=true;
					dlm.addElement(new ListItem(s,data));
				}
				finally
				{
					selecting=false;
				}
			}
			else
			{
				int afterIndex; // We want to insert before this
				for(afterIndex=0;afterIndex<dlm.getSize();afterIndex++)
				{
					if(((ListItem)dlm.get(afterIndex)).s.compareToIgnoreCase(s) > 0)
						break;
				}
				try
				{
					selecting=true;
					dlm.insertElementAt(new ListItem(s,data),afterIndex);
				}
				finally
				{
					selecting=false;
				}
			}
		}

		@Override
		public void removeItem(String s)
		{
			try
			{
				selecting=true;
				int index=findValue(s);
				if(index!=-1)
					dlm.remove(index);
			}
			finally
			{
				selecting=false;
			}
		}

		@Override
		public void removeData(Object data)
		{
			try
			{
				selecting=true;
				int index=findData(data);
				if(index!=-1)
					dlm.remove(index);
			}
			finally
			{
				selecting=false;
			}
		}

		@Override
		public void clear()
		{
			try
			{
				selecting=true;
				dlm.removeAllElements();
			}
			finally
			{
				selecting=false;
			}
		}

		@Override
		public void clearSelection()
		{
			try
			{
				selecting=true;
				l.clearSelection();
			}
			finally
			{
				selecting=false;
			}
		}

		@Override
		public<C> C[] getData(Class<C> c)
		{
			@SuppressWarnings("unchecked")
			C[] data = (C[])Array.newInstance(c,dlm.size());
			for(int i=0;i<dlm.size();i++)
			{
				data[i] = c.cast(((ListItem)dlm.get(i)).data);
			}
			return data;
		}

		@Override
		public String[] getItems()
		{
			String[] items=new String[dlm.size()];
			for(int i=0;i<dlm.size();i++)
			{
				items[i]=((ListItem)dlm.get(i)).s;
			}
			return items;
		}

		@Override
		public String[] getMultiSelected()
		{
			int[] selected=l.getSelectedIndices();
			String[] values=new String[selected.length];
			for(int i=0;i<values.length;i++)
			{
				values[i]=((ListItem)dlm.get(selected[i])).s;
			}
			return values;
		}

		@Override
		public Object[] getMultiSelectedData()
		{
			int[] selected=l.getSelectedIndices();
			Object[] data=new Object[selected.length];
			for(int i=0;i<selected.length;i++)
			{
				data[i]=((ListItem)dlm.get(selected[i])).data;
			}
			return data;
		}

		@Override
		public void setMultiSelect(boolean b)
		{
			try
			{
				selecting=true;
				l.setSelectionMode(b
					? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
					:	ListSelectionModel.SINGLE_SELECTION);
			}
			finally
			{
				selecting=false;
			}
		}

		@Override
		public void setOnAction(String callback)
		{
			getInterface().getOwner().getCallbackHandler().check(callback);
				ListBoxImp.this.onAction=callback;
		}
		@Override
		public void setOnChange(String callback)
		{
			getInterface().getOwner().getCallbackHandler().check(callback);
				ListBoxImp.this.onSelectionChange=callback;
		}

		@Override
		public void setWidth(int iWidth)
		{
			this.width=iWidth;
		}

		private int findValue(String s)
		{
			for(int i=0;i<dlm.size();i++)
			{
				ListItem li=(ListItem)dlm.get(i);
				if(li.s.equals(s))
				{
					return i;
				}
			}
			return -1;
		}

		private ListItem getItemByName(String s)
		{
			for(int i=0;i<dlm.size();i++)
			{
				ListItem li=(ListItem)dlm.get(i);
				if(li.s.equals(s))
				{
					return li;
				}
			}
			return null;
		}

		private int findData(Object data)
		{
			for(int i=0;i<dlm.size();i++)
			{
				ListItem li=(ListItem)dlm.get(i);
				if(li.data.equals(data))
				{
					return i;
				}
			}
			return -1;
		}

		@Override
		public void setSelectedData(Object data,boolean select)
		{
			setSelected(null,data,select);
		}
		@Override
		public void setSelected(String s,boolean select)
		{
			setSelected(s,null,select);
		}
		private void setSelected(String s,Object data,boolean select)
		{
			int[] selected=l.getSelectedIndices();
			if(select)
			{
				// Check it's not already selected
				for(int i=0;i<selected.length;i++)
				{
					ListItem li=(ListItem)dlm.get(selected[i]);
					if(
						(s!=null && li.s.equals(s)) ||
						(data!=null && li.data.equals(data))
						) return;
				}
				// OK, add it to list
				int newIndex=s!=null ? findValue(s) : findData(data);
				if(newIndex==-1) return;
				int[] newSelected=new int[selected.length+1];
				System.arraycopy(selected,0,newSelected,0,selected.length);
				newSelected[selected.length]=newIndex;
				try
				{
					selecting=true;
					l.setSelectedIndices(newSelected);
				}
				finally
				{
					selecting=false;
				}
			}
			else
			{
				// Check it's selected
				for(int i=0;i<selected.length;i++)
				{
					ListItem li=(ListItem)dlm.get(selected[i]);
					if((s!=null && li.s.equals(s)) || (data!=null && li.data.equals(data)))
					{
						int[] newSelected=new int[selected.length-1];
						System.arraycopy(selected,0,newSelected,0,i);
						System.arraycopy(selected,i+1,newSelected,i,selected.length-i-1);
						try
						{
							selecting=true;
							l.setSelectedIndices(newSelected);
						}
						finally
						{
							selecting=false;
						}
					}
				}
			}
		}

		@Override
		public void setEnabled(boolean enabled)
		{
			if(isEnabled()!=enabled)
			{
				ListBoxImp.this.setEnabled(enabled);
				l.setEnabled(enabled);
			}
		}

		@Override
		public boolean isEnabled()
		{
			return ListBoxImp.this.isEnabled();
		}

		@Override
		public void setOnMenu(String callback)
		{
			if(callback!=null)
			{
				getOwner().getCallbackHandler().check(callback,new Class[]
				{
					com.leafdigital.ui.api.PopupMenu.class
				});
			}
			onMenu=callback;
		}
		@Override
		public void setUseFontSettings(boolean useFontSettings)
		{
			ListBoxImp.this.useFontSettings=useFontSettings;
			resetFont();
		}

		@Override
		public void setBold(String s,boolean bold)
		{
			ListItem li=getItemByName(s);
			if(li.bold==bold) return;
			li.bold=bold;
			l.repaint();
		}

		@Override
		public void setFaint(String s,boolean faint)
		{
			ListItem li=getItemByName(s);
			if(li.faint==faint) return;
			li.faint=faint;
			l.repaint();
		}

		@Override
		public void setMacIndent(boolean macIndent)
		{
			setMacIndent(macIndent ? SupportsMacIndent.TYPE_EDIT_LEGACY
				: SupportsMacIndent.TYPE_NONE);
		}

		@Override
		public void setMacIndent(String macIndent)
		{
			int newValue = UISingleton.getMacIndent(macIndent);
			if(newValue != ListBoxImp.this.macIndent)
			{
				ListBoxImp.this.macIndent = newValue;
				Rectangle bounds = getBounds();
				setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
			}
		}
	}

	private void doMenu(MouseEvent e)
	{
		if(onMenu==null) return;

		// Get selection
		int[] selected=l.getSelectedIndices();
		if(selected.length>0)
		{
			// Check they actually clicked on one of the selected thing
			boolean found=false;
			for(int i=0;i<selected.length;i++)
			{
				Rectangle r=l.getCellBounds(selected[i],selected[i]);
				if(r.contains(e.getPoint()))
				{
					found=true;
					break;
				}
			}
			if(!found) selected=new int[0]; // Clear selection; next bit
			  // picks it up
		}
		if(selected.length==0)
		{
			// Nothing selected? OK let's select the thing under here
			int index=l.locationToIndex(e.getPoint());
			if(index==-1) return; // Nothing in list
			Rectangle r=l.getCellBounds(index,index);
			if(!r.contains(e.getPoint())) return; // Didn't actually hit anything
			l.setSelectedIndex(index);  // This will send 'user selected' messages
			selected=new int[] {index};
		}

		// Build menu
		PopupMenuImp pm=new PopupMenuImp();
		getInterface().getOwner().getCallbackHandler().call(
			onMenu,new Object[] {pm.getInterface()});

		// If nothing was added, exit
		if(pm.getComponentCount()==0) return;

		// Show menu
		pm.show(l,e.getX(),e.getY());
	}

	private final static Font defaultListFont=(new JList()).getFont();
	private boolean useFontSettings;

	private Font currentFont,currentBoldFont;

	/** Update font based on current settings */
	private void resetFont()
	{
		currentFont=null;
		if(useFontSettings)
		{
			currentFont=((ListBoxInterface)outsideInterface).getUI().getFont();
		}
		if(currentFont==null)
			currentFont=defaultListFont;
		currentBoldFont=currentFont.deriveFont(Font.BOLD);
		if(!l.getFont().equals(currentFont))
		{
			l.setFont(currentFont);
			((ListBoxInterface)outsideInterface).redoLayout();
		}
	}

	@Override
	public void updateTheme(Theme t)
	{
		resetFont();
	}


}
