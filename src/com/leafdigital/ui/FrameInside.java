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
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;

import javax.swing.*;
import javax.swing.event.*;

import util.PlatformUtils;

/**
 * Handles a FrameContents inside the main application window.
 */
public class FrameInside extends MacFixInternalFrame implements FrameHolder
{
	/** Owning desktop */
	private InternalDesktop owner;

	/** Contents of frame */
	private WindowImp wi;

	private boolean active=false;

	private void trackActive()
	{
		active=true;
		wi.informActive(true);
		owner.informActive(this);
	}

	private boolean pastStartup=false;

	FrameInside(InternalDesktop owner,WindowImp contents,final boolean initialScreen,final Point initial,final boolean minimized)
	{
		this.owner=owner;
		this.wi=contents;
		contents.setFrame(this);

		UISingleton.runInSwing(new Runnable()
		{
			@Override
			public void run()
			{
				JComponent pContents=wi.getContents();
				getContentPane().setLayout(new BorderLayout());
				JPanel spacer=new JPanel();
				spacer.setPreferredSize(new Dimension(	pContents.getSize()));
				getContentPane().add(spacer,BorderLayout.CENTER);
				pack();
				getContentPane().remove(spacer);
				getContentPane().add(pContents,BorderLayout.CENTER);
				setClosable(true);
				setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
				setIconifiable(true);
				setMaximizable(true);
				setResizable(true);
				FrameInside.this.owner.addFrame(FrameInside.this,initialScreen,initial);
				setFrameIcon(null);
				if(!minimized)
				{
					setVisible(true);
					wi.getInterface().activate();
				}
				addInternalFrameListener(new InternalFrameListener()
				{
					@Override
					public void internalFrameOpened(InternalFrameEvent ife)
					{
					}

					@Override
					public void internalFrameClosing(InternalFrameEvent ife)
					{
						if(handleClosing())
						{
							dispose();
						}
					}

					@Override
					public void internalFrameClosed(InternalFrameEvent ife)
					{
						handleClose();
					}

					@Override
					public void internalFrameIconified(InternalFrameEvent ife)
					{
						try
						{
							setIcon(false);
						}
						catch(PropertyVetoException e)
						{
						}
						handleMinimize();
					}

					@Override
					public void internalFrameDeiconified(InternalFrameEvent ife)
					{
					}

					@Override
					public void internalFrameActivated(InternalFrameEvent ife)
					{
						trackActive();
					}

					@Override
					public void internalFrameDeactivated(InternalFrameEvent ife)
					{
						active=false;
						if(wi!=null) wi.informActive(false);
					}
				});

				pastStartup=true;
			}
		});
	}

	WindowImp getContents()
	{
		return wi;
	}

//	void setActive(boolean bActive)
//	{
//		wi.setActive(bActive);
//	}

	/** @return Size of contents of this frame */
	public Dimension getContentSize()
	{
		return wi.getContents().getSize();
	}

	@Override
	public void focusFrame()
	{
		if(!isVisible()) setVisible(true);

		try
		{
			setSelected(true);
		}
		catch(PropertyVetoException e)
		{
		}
		moveToFront();

		wi.focus();

		// This needs to be called later because otherwise, it can get run in the
		// constructor - before subclass constructors have been called. This
		// causes confusing behaviour if callbacks are called before constructor.
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				trackActive();
			}
		});
	}

	/** @return True if frame is focused */
	boolean isFocused()
	{
		return isVisible() && wi.isActive();
	}

	/**
	 * Called when window is being closed.
	 * @return True if it's OK for window to close
	 */
	public boolean handleClosing()
	{
		if(wi!=null)
			return wi.informClosing();
		else
			return true;
	}

	@Override
	public void handleClose()
	{
		owner.removeFrame(this);
		if(wi!=null) wi.informClosed();
	}

	@Override
	public void setIcon(Image i)
	{
		setFrameIcon(new ImageIcon(i));
	}

	@Override
	public Image getIcon()
	{
		if(getFrameIcon()==null)
			return null;
		else
		{
			Icon i=getFrameIcon();
			if(i instanceof ImageIcon)
			{
				return ((ImageIcon)i).getImage();
			}
			else
			{
				// May be a Windows ScalableIcon or something, so use standard icon
				// paint method.
				BufferedImage bi=new BufferedImage(i.getIconWidth(),i.getIconHeight(),BufferedImage.TYPE_INT_ARGB);
				i.paintIcon(null,bi.getGraphics(),0,0);
				return bi;
			}
		}
	}

	@Override
	public void handleMinimize()
	{
		if(!isVisible()) return;
		boolean wasFocused=isFocused();
		setVisible(false);
		if(sb!=null) sb.informChanged();
		if(wasFocused)
		{
			owner.reassignFocus();
		}
	}

	@Override
	public boolean isMinimized()
	{
		return !isVisible();
	}

	/** Called when window is being restored. */
	public void handleRestore()
	{
		if(isVisible()) return;

		setVisible(true);
		if(sb!=null) sb.informChanged();
	}

	@Override
	public void setTitle(String title)
	{
		super.setTitle(title);
		if(sb!=null) sb.informChanged();
	}

	@Override
	public void attention()
	{
		if(sb!=null) sb.attention();
	}

	@Override
	public long getAttentionTime()
	{
		if(sb != null)
		{
			return sb.getAttentionTime();
		}
		return 0;
	}

	boolean isReasonablyVisible()
	{
		return owner.isReasonablyVisible(this);
	}

	boolean canClearAttention()
	{
		return wi.canClearAttention();
	}

	@Override
	public void setBounds(int x,int y,int w,int h)
	{
		super.setBounds(x,y,w,h);
		if(pastStartup && wi!=null)	wi.informMoved(false,x,y);
		if(sb!=null) sb.informMoved();
	}

	void setSwitchButton(SwitchBar.SwitchButton sb)
	{
		this.sb=sb;
	}

	/** Keep track of our button */
	private SwitchBar.SwitchButton sb;

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
		if(fh.getIcon()!=null) setIcon(fh.getIcon());
		setResizable(fh.isResizable());
		setClosable(fh.isClosable());
		if(fh.isMinimized()) handleMinimize();
	}

	@Override
	public boolean isActive()
	{
		return active;
	}
	@Override
	public boolean isHidden()
	{
		return !isShowing() || PlatformUtils.isMacAppHidden();
	}
}
