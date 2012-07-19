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
import java.awt.event.*;

import javax.swing.*;

import util.PlatformUtils;

/**
 * Provides framework to hold the window outside on its own.
 */
public class FrameOutside extends JFrame implements FrameHolder
{
	private WindowImp wi;

	/** Track last place we added a window onscreen */
	private static Point pAddLocation=new Point(20,20);

	/** Set true when we actually called close() */
	private boolean directClose=false;

	/** Time of last attention call */
	private long lastAttentionTime;

	FrameOutside(WindowImp fc)
	{
		this(fc,null);
	}

	FrameOutside(WindowImp wiContents,Point pScreen)
	{
		this.wi=wiContents;
		wiContents.setFrame(this);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(wiContents.getContents(),BorderLayout.CENTER);
		wiContents.getUI().initDefaultIcon(this);

//		addWindowFocusListener(new WindowFocusListener()
//		{
//			public void windowGainedFocus(WindowEvent e)
//			{
//				wi.informActive(true);
//			}
//
//			public void windowLostFocus(WindowEvent e)
//			{
//				wi.informActive(false);
//			}
//		});

		addWindowListener(new WindowListener()
		{
			@Override
			public void windowOpened(WindowEvent arg0)
			{
			}

			@Override
			public void windowClosing(WindowEvent arg0)
			{
				if(closable)
				{
					if(wi!=null)
					{
						if(!directClose && !wi.informClosing()) return;
						wi.informClosed();
					}
					dispose();
				}
			}

			@Override
			public void windowClosed(WindowEvent arg0)
			{
			}

			@Override
			public void windowIconified(WindowEvent arg0)
			{
			}

			@Override
			public void windowDeiconified(WindowEvent arg0)
			{
			}

			@Override
			public void windowActivated(WindowEvent arg0)
			{
				if(wi!=null) wi.informActive(true);
			}

			@Override
			public void windowDeactivated(WindowEvent arg0)
			{
				if(wi!=null) wi.informActive(false);
			}
		});

		addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentMoved(ComponentEvent e)
			{
				if(wi!=null) wi.informMoved(true,getX(),getY());
			}
			@Override
			public void componentResized(ComponentEvent e)
			{
				if(wi!=null) wi.informMoved(true,getX(),getY());
			}
		});

		addWindowStateListener(new WindowStateListener()
		{
			@Override
			public void windowStateChanged(WindowEvent e)
			{
				wi.informMaximized((getExtendedState() & JFrame.MAXIMIZED_BOTH)!=0);
			}
		});

		if(sizeOffset==null)
		{
			JFrame test=new JFrame("Hello");
			((JComponent)test.getContentPane()).setPreferredSize(new Dimension(100,100));
			test.pack();
			sizeOffset=new Dimension(
				test.getWidth()-100,test.getHeight()-100);
		}
		Dimension size=new Dimension(wiContents.getContents().getSize());
		size.width+=sizeOffset.width;
		size.height+=sizeOffset.height;

		if(pScreen==null)
		{
			Rectangle rMaximized=GraphicsEnvironment.getLocalGraphicsEnvironment().
				getMaximumWindowBounds();
			Rectangle r=new Rectangle(pAddLocation,size);
			pAddLocation.x=r.x+20;
			pAddLocation.y=r.y+20;
			if(r.width>rMaximized.width) r.width=rMaximized.width;
			if(r.height>rMaximized.height) r.height=rMaximized.height;
			if(r.x+r.width > rMaximized.width)
			{
				r.x=10;
				pAddLocation.x=30;
			}
			if(r.y+r.height> rMaximized.height)
			{
				r.y=10;
				pAddLocation.y=30;
			}
			setBounds(r);
		}
		else
		{
			// Put it in place specified
			setBounds(new Rectangle(pScreen,size));
		}

		setVisible(true);
	}

	private static Dimension sizeOffset=null;

	/**
	 * @return Limits for moving the frame (=screen size)
	 */
	public Dimension getMoveLimits()
	{
		return Toolkit.getDefaultToolkit().getScreenSize();
	}

	@Override
	public void handleClose()
	{
		closable=true;
		directClose=true;
		processEvent(new WindowEvent(this,WindowEvent.WINDOW_CLOSING));
	}

	@Override
	public void focusFrame()
	{
		toFront();
		wi.focus();
		if(isActive()) wi.informActive(true);
	}

	@Override
	public void setIcon(Image i)
	{
		super.setIconImage(i);
	}

	@Override
	public Image getIcon()
	{
		return getIconImage();
	}

	@Override
	public void handleMinimize()
	{
		setState(Frame.ICONIFIED);
	}

	@Override
	public boolean isMinimized()
	{
		return getState()==Frame.ICONIFIED;
	}

	@Override
	public void attention()
	{
		// Attention is not really implemented for FrameOutside, but we do
		// track the last time.
		lastAttentionTime = System.currentTimeMillis();
	}

	@Override
	public long getAttentionTime()
	{
		return lastAttentionTime;
	}

	private boolean closable=true;

	@Override
	public void setClosable(boolean closable)
	{
		this.closable=closable;
	}

	@Override
	public boolean isClosable()
	{
		return closable;
	}

	@Override
	public void killSilently()
	{
		wi=null;
		dispose();
	}

	@Override
	public void initialiseFrom(FrameHolder fh)
	{
		setTitle(fh.getTitle());
		setIcon(fh.getIcon());
		setResizable(fh.isResizable());
		setClosable(fh.isClosable());
		if(fh.isMinimized()) fh.handleMinimize();
	}
	@Override
	public boolean isHidden()
	{
		return !isShowing() || PlatformUtils.isMacAppHidden();
	}
}
