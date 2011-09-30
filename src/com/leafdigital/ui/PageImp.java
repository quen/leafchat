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

import java.util.*;

import javax.swing.*;

import org.w3c.dom.Element;

import com.leafdigital.ui.api.*;

import leafchat.core.api.BugException;

/**
 * Represents the contents of a page that can be included within other components.
 */
public class PageImp
{
	/** Owner */
	private UISingleton ui;

	/** Handles callbacks */
	private CallbackHandler ch;

	/** Content panel */
	private BorderPanelImp contents=new BorderPanelImp();

	private Page externalInterface=new PageInterface();

	interface PageTitleOwner
	{
		void pageTitleChanged(Page p);
	}

	private String setCallback=null;

	class PageInterface extends BasicWidget implements Page, InternalWidget, InternalWidgetOwner
	{
		private PageImp getPageImp()
		{
			return PageImp.this;
		}

		@Override
		public int getContentType() { return CONTENT_UNNAMEDSLOTS; }

		private String title="";
		private PageTitleOwner titleOwner=null;

		/** Map of string -> Widget for contained widgets */
		private Map<String, Widget> widgetIDs = new HashMap<String, Widget>();

		/** Map of string -> ButtonGroup */
		private Map<String, ButtonGroup> buttonGroups = new HashMap<String, ButtonGroup>();

		private HashMap<String, Set<BaseGroup>> arbitraryGroups =
			new HashMap<String, Set<BaseGroup>>();

		private boolean created;

		@Override
		public void setTitle(String title)
		{
			if(title.equals(this.title)) return;

			this.title=title;
			if(titleOwner!=null) titleOwner.pageTitleChanged(this);
		}

		public void informTitleChanges(PageTitleOwner titleOwner)
		{
			this.titleOwner=titleOwner;
		}

		@Override
		public String getTitle()
		{
			return title;
		}

		@Override
		public void setContents(Widget w)
		{
			// Clear IDs
			widgetIDs.clear();

			((InternalWidget)w).setParent(this);
			if(w instanceof PageImp.PageInterface)
			{
				PageImp newContents=((PageImp.PageInterface)w).getPageImp();
				if(newContents.setCallback!=null)
					newContents.ch.callHandleErrors(newContents.setCallback);
			}

			// Set it within the content panel
			contents.getInterface().set(BorderPanel.CENTRAL,w);
			contents.repaint();
		}

		@Override
		public void setContents(Element e)
		{
			// Clear IDs
			widgetIDs.clear();

			contents.getInterface().set(BorderPanel.CENTRAL,
				ui.createWidget(e, ch!=null ? this : getOwner()));
		}

		@Override
		public Widget getWidget(String sID)
		{
			Widget w=widgetIDs.get(sID);
			if(w==null)
				throw new BugException("Widget ID not present: "+sID);
			else
			  return w;
		}

		@Override
		public void setWidgetID(String sID,Widget w)
		{
			if(widgetIDs.put(sID,w)!=null) throw new BugException(
				"Widget ID not unique: "+sID);
		}

		@Override
		public ButtonGroup getButtonGroup(String group)
		{
			ButtonGroup bg=buttonGroups.get(group);
			if(bg==null)
			{
				bg=new ButtonGroup();
				buttonGroups.put(group,bg);
			}
			return bg;
		}

		@Override
		public Set<BaseGroup> getArbitraryGroup(String group)
		{
			Set<BaseGroup> s = arbitraryGroups.get(group);
			if(s==null)
			{
				s=new HashSet<BaseGroup>();
				arbitraryGroups.put(group,s);
			}
			return s;
		}

		@Override
		public RadioButton getGroupSelected(String group)
		{
			ButtonGroup bg=getButtonGroup(group);
			for(Enumeration<AbstractButton> e=bg.getElements();e.hasMoreElements();)
			{
				RadioButtonImp.MyRadioButton rb=(RadioButtonImp.MyRadioButton)e.nextElement();
				if(rb.isSelected()) return rb.getInterface();
			}
			return null;
		}

		@Override
		public CallbackHandler getCallbackHandler()
		{
			if(ch==null)
			{
				return getOwner().getCallbackHandler();
			}
			return ch;
		}

		@Override
		public void addXMLChild(String slotName,Widget child)
		{
			if(ch!=null)
				throw new BugException("Pages cannot contain children");
			else
				setContents(child);
		}

		@Override
		public JComponent getJComponent()
		{
			return contents;
		}

		@Override
		public int getPreferredWidth()
		{
			return ((InternalWidget)contents.getInterface()).getPreferredWidth();
		}

		@Override
		public int getPreferredHeight(int width)
		{
			return ((InternalWidget)contents.getInterface()).getPreferredHeight(width);
		}

		@Override
		public void setOnSet(String callback)
		{
			ch.check(callback);
			setCallback=callback;
		}

		@Override
		public void informClosed()
		{
			for(Widget w : widgetIDs.values())
			{
				w.informClosed();
			}
		}

		@Override
		public boolean isCreated()
		{
			return created;
		}

		@Override
		public void markCreated()
		{
			created = true;
		}
	};

	/** @return API interface for this object */
	Page getInterface()
	{
		return externalInterface;
	}

	/**
	 * Constructs as independent page.
	 * @param ui Owner singleton
	 * @param callbacks Callbacks object
	 */
	PageImp(UISingleton ui,Object callbacks)
	{
		this.ui=ui;
		ch=new CallbackHandlerImp(callbacks);
		((InternalWidget)contents.getInterface()).setOwner(externalInterface);
		((InternalWidget)contents.getInterface()).setParent((PageInterface)externalInterface);
	}

	/**
	 * Construct as something inside another dialog (whgich handles the callbacks).
	 * @param ui
	 */
	PageImp(UISingleton ui)
	{
		this.ui=ui;
		ch=null;
		((InternalWidget)contents.getInterface()).setOwner(externalInterface);
		((InternalWidget)contents.getInterface()).setParent((PageInterface)externalInterface);
	}

	/** @return Actual contents */
	JComponent getContents() { return contents; }
}

