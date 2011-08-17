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
import java.util.*;

import javax.swing.*;

import org.w3c.dom.Element;

import com.leafdigital.ui.api.*;
import com.leafdigital.ui.api.Dialog;

import leafchat.core.api.BugException;

/** Implementation of dialog */
public class DialogImp extends JDialog
{
	final static int MINWIDTH=WindowImp.MINWIDTH,MINHEIGHT=WindowImp.MINHEIGHT;

	/** Minimum size for resizing etc. */
	private Dimension minSize=new Dimension(MINWIDTH,MINHEIGHT);

	private UISingleton owner;

	/** Handles callbacks */
	private CallbackHandler ch;

	/** Offsets for titlebar etc */
	private static int widthOffset=-1,heightOffset;

	private String onClosed=null;

	DialogImp(UISingleton uis,Object oCallbacks)
	{
		super((Frame)null,true); // Null because we don't know yet what parent is, it comes in for show()
		setResizable(false);
		owner=uis;
		ch=new CallbackHandlerImp(oCallbacks);
		getContentPane().setLayout(new BorderLayout());

		addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				if(getWidth()<minSize.width || getHeight()<minSize.height)
				{
					setSize(
						Math.max(minSize.width,getWidth()),
						Math.max(minSize.height,getHeight()));
				}
			}
		});

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosed(WindowEvent e)
			{
				if(onClosed!=null)
					getInterface().getCallbackHandler().callHandleErrors(onClosed);
			}
		});
	}

	Dialog getInterface()
	{
		return di;
	}

	private DialogInterface di=new DialogInterface();

	class DialogInterface implements Dialog,InternalWidgetOwner
	{
		private Dimension initialSize=null;

		private boolean shown;

		/** Map of string -> Widget for contained widgets */
		private Map<String, Widget> widgetIDs = new HashMap<String, Widget>();

		/** Map of string -> ButtonGroup */
		private Map<String, ButtonGroup> groups = new HashMap<String, ButtonGroup>();

		private InternalWidget rootWidget=null;
		private HashMap<String, Set<BaseGroup>> arbitraryGroups =
			new HashMap<String, Set<BaseGroup>>();

		@Override
		public void setTitle(String title)
		{
			DialogImp.this.setTitle(title);
		}

		@Override
		public void setInitialSize(int width, int height)
		{
			initialSize=new Dimension(width,height);
		}

		@Override
		public void setResizable(boolean resizable)
		{
			DialogImp.this.setResizable(resizable);
		}

		@Override
		public void setCloseable(boolean closeable)
		{
			DialogImp.this.setDefaultCloseOperation(
			  closeable ? DISPOSE_ON_CLOSE : DO_NOTHING_ON_CLOSE);
		}

		@Override
		public void setMinSize(int minWidth, int minHeight)
		{
			if(minWidth < MINWIDTH) minWidth=MINWIDTH;
			if(minHeight < MINHEIGHT) minHeight=MINHEIGHT;

			minSize=new Dimension(minWidth,minHeight);
		}

		@Override
		public void show(final WidgetOwner specifiedParent)
		{
			if(shown) throw new Error("Cannot show dialog twice.");
			shown=true;

			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					// Set the dialog's size
					int startW,startH;
					if(initialSize!=null)
					{
						startW=initialSize.width;
						startH=initialSize.height;
					}
					else
					{
						if(rootWidget!=null)
						{
							if(widthOffset==-1)
							{
								((JComponent)getContentPane()).setPreferredSize(new Dimension(100,100));
								pack();
								widthOffset=getSize().width-100;
								heightOffset=getSize().height-100;
							}

							startW=rootWidget.getPreferredWidth()+widthOffset;
							startH=rootWidget.getPreferredHeight(startW)+heightOffset;
						}
						else
						{
							startW=minSize.width;
							startH=minSize.height;
						}
					}

					int
						width=Math.max(startW,minSize.width),
						height=Math.max(startH,minSize.height);
					setSize(width,height);

					// Show window
					owner.setDialogPosition(DialogImp.this,specifiedParent);
					setVisible(true);
				}
			});
		}

		@Override
		public void setContents(Widget w)
		{
			// Clear IDs
			widgetIDs.clear();
			rootWidget=(InternalWidget)w;
			rootWidget.setOwner(this);

			// Just set it within the content panel
			getContentPane().removeAll();
			getContentPane().add(rootWidget.getJComponent(),BorderLayout.CENTER);
		}

		@Override
		public void setContents(Element e)
		{
			// Clear IDs
			widgetIDs.clear();
			rootWidget=(InternalWidget)owner.createWidget(e,this);
			rootWidget.setOwner(this);

			// Just set it within the content panel
			getContentPane().removeAll();
			getContentPane().add(rootWidget.getJComponent(),BorderLayout.CENTER);
		}

		@Override
		public void close()
		{
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					DialogImp.this.dispose();
				}
			});
		}

		@Override
		public Widget getWidget(String id)
		{
			Widget w=widgetIDs.get(id);
			if(w==null)
				throw new BugException("Widget ID not present: "+id);
			else
				return w;
		}

		@Override
		public void setWidgetID(String id,Widget w)
		{
			if(widgetIDs.put(id,w)!=null) throw new BugException(
				"Widget ID not unique: "+id);
		}

		@Override
		public ButtonGroup getButtonGroup(String group)
		{
			ButtonGroup bg=groups.get(group);
			if(bg==null)
			{
				bg=new ButtonGroup();
				groups.put(group,bg);
			}
			return bg;
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
		public Set<BaseGroup> getArbitraryGroup(String group)
		{
			Set<BaseGroup> s = arbitraryGroups.get(group);
			if(s==null)
			{
				s = new HashSet<BaseGroup>();
				arbitraryGroups.put(group,s);
			}
			return s;
		}

		@Override
		public CallbackHandler getCallbackHandler()
		{
			return ch;
		}

		@Override
		public void setOnClosed(String callback)
		{
			getCallbackHandler().check(callback);
			onClosed=callback;
		}

		JComponent getPositionReferent()
		{
			return (JComponent)getContentPane();
		}

	}

}
