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

import javax.swing.*;

import util.PlatformUtils;

/**
 * FrameHolder implementation for tab mode.
 */
public class FrameTab extends JPanel implements FrameHolder
{
	private Point previousPosition;
	private Dimension previousContentSize;
	private UISingleton ui;

	private SwitchBar.SwitchButton sb;

	private Image icon;

	private WindowImp wi;

	private String title="";

	private boolean closable=true;

	private static int nextID=0;
	private int id=nextID++;

	private boolean active;

	/**
	 * @return Tab ID
	 */
	public int getID() { return id; }

	/**
	 * @param ui UI
	 * @param wi Window that this tab contains
	 * @param previousPosition Position before it was a tab
	 * @param select True if tab should be selected
	 */
	public FrameTab(UISingleton ui,WindowImp wi,Point previousPosition,boolean select)
	{
		super(new BorderLayout());
		this.previousPosition=previousPosition;
		this.ui=ui;
		this.wi=wi;

		if(wi.getContents().getSize().width>0)
			previousContentSize=new Dimension(wi.getContents().getSize());
		else
			previousContentSize=new Dimension(wi.getContents().getPreferredSize());

		add(wi.getContents(),BorderLayout.CENTER);
		wi.setFrame(this);
		ui.addTab(this);
		if(select) focusFrame();
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

	@Override
	public void focusFrame()
	{
		ui.selectTab(this);
	}

	@Override
	public Image getIcon()
	{
		return icon;
	}

	@Override
	public String getTitle()
	{
		return title;
	}

	@Override
	public void handleClose()
	{
		ui.removeTab(this);
		if(wi!=null) wi.informClosed();
	}

	@Override
	public void handleMinimize()
	{
		// Tabs can't be minimised
	}

	@Override
	public void initialiseFrom(FrameHolder other)
	{
		setTitle(other.getTitle());
		if(other.getIcon()!=null) setIcon(other.getIcon());
		setClosable(other.isClosable());
		oldResizable=other.isResizable();
		oldMinimized=other.isMinimized();
	}

	@Override
	public boolean isClosable()
	{
		return closable;
	}

	/** Retain data if this was in another type of window, so we can put it back */
	private boolean oldResizable,oldMinimized;

	@Override
	public boolean isMinimized()
	{
		return oldMinimized;
	}

	@Override
	public boolean isResizable()
	{
		return oldResizable;
	}

	@Override
	public void killSilently()
	{
		ui.removeTab(this);
	}

	@Override
	public void setClosable(boolean closable)
	{
		this.closable=closable;
		if(sb!=null) sb.informChanged();
	}

	@Override
	public void setIcon(Image i)
	{
		this.icon=i;
		if(sb!=null) sb.informChanged();
	}

	@Override
	public void setResizable(boolean resizable)
	{
		oldResizable=resizable;
	}

	@Override
	public void setTitle(String title)
	{
		this.title=title;
		if(sb!=null) sb.informChanged();
	}

	void setSwitchButton(SwitchBar.SwitchButton sb)
	{
		this.sb=sb;
	}

	/**
	 * @return Position on screen of a previous window before it became a tab
	 */
	public Point getPreviousPosition()
	{
		if(previousPosition==null)
			return null;
		else
			return new Point(previousPosition);
	}
	/**
	 * @return Size of window before it became a tab
	 */
	public Dimension getPreviousContentSize()
	{
		if(previousContentSize==null)
			return null;
		else
			return new Dimension(previousContentSize);
	}

	boolean canClearAttention()
	{
		return wi.canClearAttention();
	}

	@Override
	public boolean isActive()
	{
		return ui.getSelectedTab()==this;
	}

	@Override
	public boolean isHidden()
	{
		return !isActive() || PlatformUtils.isMacAppHidden();
	}

	void informActive()
	{
		if(active) return;
		active=true;
		ui.runInThread(new Runnable()
		{
			@Override
			public void run()
			{
				wi.informActive(true);

			}
		});
	}

	void informInactive()
	{
		if(!active) return;
		active=false;
		ui.runInThread(new Runnable()
		{
			@Override
			public void run()
			{
				wi.informActive(false);
			}
		});
	}
}
