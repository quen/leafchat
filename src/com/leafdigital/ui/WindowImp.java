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
import java.awt.image.BufferedImage;
import java.util.*;

import javax.swing.*;

import org.w3c.dom.Element;

import util.*;

import com.leafdigital.prefs.api.*;
import com.leafdigital.ui.api.*;
import com.leafdigital.ui.api.Window;

import leafchat.core.api.*;

/**
 * Represents the contents of a window, whether it's internal
 * (FrameInside) or external (FrameOutside)
 */
public class WindowImp
{
	/** Owner */
	private UISingleton owner;

	/** Callback method for when window is closed */
	private String onClosed;

	/** Callback method for when window is about to be closed */
	private String onClosing;

	/** Callback method for when window becomes active */
	private String onActive;

	/** Handles callbacks */
	private CallbackHandler ch;

	/** Content panel */
	private BorderPanelImp contents=new BorderPanelImp();

	/** True if the window is active */
	private boolean active=false;

	/** True if the window is maximized */
	private boolean maximized=false;

	/** True if window has been closed */
	private boolean closed=false;

	/** Preference group for remembering position, if used */
	private PreferencesGroup prefsGroup=null;
	/** Preference ID for remembering position, if used */
	private String prefsID=null;
	/** Initial position for window, or null for default */
	private Point initialPosition=null;
	/** If true, initialPosition is relative to screen */
	private boolean initialScreenRelative;

	private String remember=null;
	private String extraRemember=null;

  UISingleton getUI()
  {
  		return owner;
  }

	/** Owner window */
	private FrameHolder fh;

	/** Absolute min size */
	final static int MINWIDTH=150,MINHEIGHT=100;

	/** Min width and height */
	private Dimension minSize=new Dimension(MINWIDTH,MINHEIGHT);

	/** True if attention flag can be cleared at present */
	private boolean canClearAttention=true;

	/** @return True if attention flag can be cleared at present */
	boolean canClearAttention() { return canClearAttention; }

	private WindowInterface externalInterface = new WindowInterface();

	class WindowInterface implements Window, InternalWidgetOwner
	{
		private Dimension initialSize=null;

		private boolean shown, created;

		// Stuff that needs to be stored if fh isn't up yet
		private String titlePre=null;
		private Image iconPre=null;
		private boolean unResizable=false;
		private boolean unClosable=false;
		private boolean minimisePre=false;

		/** Map of string -> Widget for contained widgets */
		private Map<String, Widget> widgetIDs = new HashMap<String, Widget>();

		private Map<String, ButtonGroup> groups=new HashMap<String, ButtonGroup>();
		private HashMap<String, Set<BaseGroup>> arbitraryGroups =
			new HashMap<String, Set<BaseGroup>>();

		@Override
		public void setTitle(String title)
		{
			if(fh==null)
				titlePre=title;
			else
				fh.setTitle(title);
		}

		@Override
		public String getTitle()
		{
			if(fh==null)
				return titlePre;
			else
				return fh.getTitle();
		}

		@Override
		public void setIcon(Image icon)
		{
			if(fh==null)
				iconPre=icon;
			else
				fh.setIcon(icon);
		}

		@Override
		public void setCanClearAttention(boolean canClearAttention)
		{
			if(WindowImp.this.canClearAttention!=canClearAttention)
			{
				WindowImp.this.canClearAttention=canClearAttention;
			}
		}

		@Override
		public boolean getCanClearAttention()
		{
			return canClearAttention;
		}

		@Override
		public boolean isActive()
		{
			return active;
		}

		@Override
		public void attention()
		{
			if(fh!=null) fh.attention();
		}

		@Override
		public void minimize()
		{
			if(fh==null)
			  minimisePre=true;
			else
				fh.handleMinimize();
		}

		@Override
		public void setInitialSize(int width, int height)
		{
			initialSize=new Dimension(width,height);
		}

		@Override
		public void setResizable(boolean resizable)
		{
			if(fh==null)
			{
				if(!resizable) unResizable=true;
			}
			else
				fh.setResizable(resizable);
		}

		@Override
		public void setClosable(boolean closable)
		{
			if(fh==null)
			{
				if(!closable) unClosable=true;
			}
			else
				fh.setClosable(closable);
		}

		@Override
		public void setMinSize(int minWidth, int minHeight)
		{
			if(minWidth < MINWIDTH) minWidth=MINWIDTH;
			if(minHeight < MINHEIGHT) minHeight=MINHEIGHT;

			minSize=new Dimension(minWidth,minHeight);
		}

		@Override
		public void setRemember(String category, String id)
		{
			Preferences p=owner.getPluginContext().getSingle(Preferences.class);
			prefsGroup=p.getGroup(owner.getPluginContext().getPlugin()).
				getChild("window-positions").getChild(category);
			if(id==null)
				prefsID="pos";
			else
				prefsID=p.getSafeToken(id);
			String value=prefsGroup.get(prefsID,null);
			if(value!=null && value.matches("[+@][0-9]+,[0-9]+:[0-9]+,[0-9]+(:.*)?"))
			{
				String[] coords=value.substring(1).split("[,:]",5);
				initialSize=new Dimension(Integer.parseInt(coords[2]),Integer.parseInt(coords[3]));
				initialPosition=new Point(Integer.parseInt(coords[0]),Integer.parseInt(coords[1]));
				if(coords.length==5)
				{
					remember=value.replaceAll(":[^:]*$","");
					extraRemember=coords[4];
				}
				else
				{
					remember=value;
					extraRemember=null;
				}
				initialScreenRelative=value.charAt(0)=='@';
			}
		}

		@Override
		public void show(final boolean minimised)
		{
			if(shown) return;
			shown=true;

			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					int startW,startH;
					if(initialSize!=null)
					{
						startW=initialSize.width;
						startH=initialSize.height;
					}
					else
					{
						startW=((InternalWidget)(contents.getInterface())).getPreferredWidth();
						startW=Math.max(minSize.width,startW);
						startH=((InternalWidget)(contents.getInterface())).getPreferredHeight(startW);
					}

					int
						width=Math.max(startW,minSize.width),
						height=Math.max(startH,minSize.height);
					// Hack because Java makes its windows smaller
					if(PlatformUtils.isMacOSVersion(10,5,0) && owner.getUIStyle()==UISingleton.UISTYLE_MULTIWINDOW)
						width-=28;
					contents.setSize(width,height);

					// Delegate to UI singleton to give us a holder
					owner.showFrameContents(WindowImp.this,initialScreenRelative,initialPosition,minimised);

					// We now have a holder so do any deferred stuff
					if(titlePre!=null) setTitle(titlePre);
					if(iconPre!=null) setIcon(iconPre);
					if(unResizable) setResizable(false);
					if(unClosable) setClosable(false);
					if(minimisePre) minimize();
				}
			});
		}

		@Override
		public void close()
		{
			fh.handleClose();
		}

		@Override
		public boolean isClosed()
		{
			return closed;
		}

		@Override
		public void setContents(Widget w)
		{
			// Clear IDs
			widgetIDs.clear();

			// Just set it within the content panel
			contents.getInterface().set(BorderPanel.CENTRAL,w);
		}

		@Override
		public void setContents(Element e)
		{
			// Clear IDs
			widgetIDs.clear();

			contents.getInterface().set(BorderPanel.CENTRAL,
			  owner.createWidget(e,this));
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
		public void activate()
		{
			fh.focusFrame();
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
		@Override
		public void setOnClosing(String callback)
		{
			getCallbackHandler().check(callback);
			onClosing=callback;
		}
		@Override
		public void setOnActive(String callback)
		{
			getCallbackHandler().check(callback);
			onActive=callback;
		}

		JComponent getPositionReferent()
		{
			return contents;
		}

		@Override
		public String getExtraRemember()
		{
			return extraRemember;
		}

		@Override
		public void setExtraRemember(String text)
		{
			if(prefsGroup==null)
				throw new BugException("Can't set extra remember when remember isn't enabled.");
			extraRemember=text;
			updateRemember();
		}

		@Override
		public boolean isHidden()
		{
			// If it doesn't have a holder set up, it's probably not going to be hidden,
			// it's just about to appear
			return fh==null ? false : fh.isHidden();
		}

		@Override
		public boolean isMinimized()
		{
			// If it doesn't have a holder set up, it's probably not going to be hidden,
			// it's just about to appear
			return fh==null ? false : fh.isMinimized();
		}

		private void informClosed()
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
	}

	/** @return API interface for this object */
	Window getInterface()
	{
		return externalInterface;
	}


	void setFrame(FrameHolder newHolder)
	{
		// First time, just set it
		if(fh!=null)
			newHolder.initialiseFrom(fh);
		fh=newHolder;
	}

	/** @return Holder for this window */
	FrameHolder getHolder()
	{
		return fh;
	}

	/**
	 * @param owner UI singleton
	 * @param callbacks Object that receives callbacks
	 */
	WindowImp(UISingleton owner, Object callbacks)
	{
		this.owner=owner;
		ch=new CallbackHandlerImp(callbacks);

		// Content pane; ensure it stays the right size
		((InternalWidget)contents.getInterface()).setOwner(externalInterface);
		contents.setBackground(Color.white);
		contents.setFocusable(false);

		owner.addWindow(this);
	}

	/** @return Actual contents */
	JComponent getContents() { return contents; }

	/**
	 * Rescale an image to the desired (square) size, or return the image if
	 * it's already that size.
	 * @param i Image
	 * @param buttonSize Desired size
	 * @return Scaled image
	 */
	static Image rescaleIcon(Image i,int buttonSize)
	{
		int iOrigWidth=i.getWidth(null),iOrigHeight=i.getHeight(null);
		if(iOrigWidth==iOrigHeight && iOrigWidth==buttonSize) return i;

		BufferedImage bi=new BufferedImage(
			buttonSize,buttonSize,BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2=bi.createGraphics();
		g2.setRenderingHint(
			RenderingHints.KEY_INTERPOLATION,
			RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2.drawImage(i,
			0,0,buttonSize,buttonSize,
			0,0,iOrigWidth,iOrigHeight,null);

		return bi;
	}

	/**
	 * Sets the focus (only) to a component within this window.
	 * <p>
	 * This is <b>not</b> the right command to programmatically switch to a
	 * window. For that, see {@link FrameHolder#focusFrame()}.
	 */
	void focus()
	{
		focusFirstAppropriateComponent(new java.awt.Component[] {contents});
	}

	/**
	 * Depth-first search for first component to focus.
	 * @param choices Possible components
	 * @return True if it focused a component, false if it didn't find one
	 */
	private static boolean focusFirstAppropriateComponent(java.awt.Component[] choices)
	{
		for(int i=0;i<choices.length;i++)
		{
			if(choices[i].isFocusable())
			{
				choices[i].requestFocus();
				return true;
			}
			else if(choices[i] instanceof Container)
			{
				if(focusFirstAppropriateComponent( ((Container)choices[i]).getComponents()))
				  return true;
			}
		}
		return false;
	}

//	public boolean isFocusCycleRoot()
//	{
//		// Focus loops within this window.
//		return true;
//	}

	private long lastActiveChange=0;
	private boolean doingActiveTimer=false;

	/**
	 * Set active state of frame.
	 * @param active True if active
	 */
	void informActive(boolean active)
	{
		if(this.active==active) return;

		// Sometimes you get 'deactive, active, deactive' instead of just 'deactive'
		// This is a hack to prevent that. The first change is processed immediately
		// but after that there is 200ms delay before processing any future changes
		long now=System.currentTimeMillis();
		if(now-lastActiveChange < 200)
		{
			if(doingActiveTimer) return;
			doingActiveTimer=true;
			TimeUtils.addTimedEvent(new Runnable()
			{
				@Override
				public void run()
				{
					doingActiveTimer=false;
					informActive(fh.isActive());
				}
			},200,true);
			return;
		}
		lastActiveChange=now;

		this.active=active;

		if(active)
			owner.informActiveWindow(this);
		else
			owner.informInactiveWindow(this);
		if(active && onActive!=null)
		{
			getInterface().getCallbackHandler().callHandleErrors(onActive);
		}
	}

	/** @return True if frame is in active state */
	boolean isActive()
	{
		return active;
	}

	/**
	 * Set maximized state of frame.
	 * @param maximized True if maximized
	 */
	void informMaximized(boolean maximized)
	{
		if(this.maximized==maximized) return;

		this.maximized=maximized;
	}

	/**
	 * Called before window is closed.
	 * @return True if it's ok for window to close
	 */
	boolean informClosing()
	{
		if(onClosing==null)	return true;
		getInterface().getCallbackHandler().callHandleErrors(onClosing);
		return false;
	}

	/**
	 * Called after window has been closed
	 */
	void informClosed()
	{
		// Before actually closing the window, we need to get any tables to stop
		// editing stuff. Otherwise it gets the 'changed' event after this 'closed'
		// event, which is silly.
		recursivelyStopTableEditing(contents.getInterface());

		closed=true;
		owner.removeWindow(this);
		if(onClosed!=null)
		{
			getInterface().getCallbackHandler().callHandleErrors(onClosed);
		}
		externalInterface.informClosed();
	}

	private void recursivelyStopTableEditing(Widget w)
	{
		if(w instanceof TableImp.TableInterface)
		{
			((TableImp.TableInterface)w).stopEditing();
		}
		else if(w instanceof WidgetParent)
		{
			for(Widget child : ((WidgetParent)w).getWidgets())
			{
				recursivelyStopTableEditing(child);
			}
		}
	}

	/**
	 * Called to inform window that it's been moved or resized. Co-ordinates
	 * refer to the position of the window frame.
	 * @param screen If true, the window is screen-relative. Otherwise it's
	 *   main-window-relative
	 * @param x
	 * @param y
	 */
	void informMoved(boolean screen,int x,int y)
	{
		if(prefsGroup!=null)
		{
			try
			{
				// TODO Not sure if I should be using the frame W/H or the inner W/H
				remember=(screen ? "@" : "+")+x+","+y+":"+
					contents.getWidth()+","+contents.getHeight();
				updateRemember();
			}
			catch(BugException e)
			{
				ErrorMsg.report("Failed to store window position in preferences",e);
			}
		}
	}

	private void updateRemember()
	{
		if(remember==null) return; // Do it later
		String value=remember;
		if(extraRemember!=null)	value+=":"+extraRemember;
		prefsGroup.set(prefsID,value);
	}
}

