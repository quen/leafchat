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
import java.util.LinkedList;

import javax.swing.*;

import util.PlatformUtils;

/**
 * Internal desktop, within which FrameInside objects can be placed
 */
public class InternalDesktop extends JDesktopPane
{
	/** Switchbar */
	private SwitchBar sb;

	private LinkedList<FrameInside> frameOrder = new LinkedList<FrameInside>();

	private Dimension overrideSize=null;

	InternalDesktop(SwitchBar sb,ToolBar tb)
	{
		this.sb=sb;

		if(PlatformUtils.isMac())
		{
			// Change window: Command-F6, Command-Shift-F6
			// (Ctrl-F6, Ctrl-Tab can't be mapped here)
			getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_F6,KeyEvent.META_DOWN_MASK),
				"changeWindowNext");
			getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_F6,KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
				"changeWindowPrev");
			getActionMap().put("changeWindowNext",new AbstractAction()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					nextWindow();
				}
			});
			getActionMap().put("changeWindowPrev",new AbstractAction()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					prevWindow();
				}
			});
		}
	}

	private void nextWindow()
	{
		JInternalFrame current=getSelectedFrame();
		if(current==null) return;

		int index=frameOrder.indexOf(current);
		if(index==-1) return;

		index++;
		if(index>=frameOrder.size()) index=0;

		frameOrder.get(index).focusFrame();
	}

	private void prevWindow()
	{
		JInternalFrame current=getSelectedFrame();
		if(current==null) return;

		int index=frameOrder.indexOf(current);
		if(index==-1) return;

		index--;
		if(index<0) index=frameOrder.size()-1;

		frameOrder.get(index).focusFrame();
	}

	/**
	 * Called when the previously-focused frame has been hidden. Assigns
	 * focus to the topmost frame.
	 */
	void reassignFocus()
	{
		JInternalFrame[] frames=getAllFrames();
		int bestPosition=-1;
		FrameInside best=null;
		for(int i=0;i<frames.length;i++)
		{
			if(!frames[i].isVisible()) continue;
			if(bestPosition==-1 || getPosition(frames[i]) < bestPosition)
			{
				bestPosition=getPosition(frames[i]);
				best=(FrameInside)frames[i];
			}
		}
		if(best!=null)
		{
			best.focusFrame();
		}
	}

	/** Point where next window will be added */
	private Point pAddLocation=new Point(20,20);

	int HITPOINTS=12;

	/**
	 * Checks whether an internal frame is 'reasonably visible'. That means it's
	 * not hidden and that a good proportion of the frame is not covered by
	 * other frames.
	 * @param fi Frame to examine
	 * @return True if it's reasonably visible
	 */
	public boolean isReasonablyVisible(FrameInside fi)
	{
		// Not showing? false
		if(!fi.isVisible()) return false;

		// Window and detection points
		Rectangle rBounds=fi.getBounds();
		int iSpacingX=(rBounds.width+HITPOINTS/2) / (HITPOINTS+1);
		int iSpacingY=(rBounds.height+HITPOINTS/2)/ (HITPOINTS+1);
		boolean[][] abHitPoints=new boolean[HITPOINTS-2][HITPOINTS-2];
		int iHits=0;

		// Check it's in the window...
		Rectangle rDesktopBounds=getBounds();
		rDesktopBounds.y=0;

		int iPosX,iPosY=rBounds.y+iSpacingY*2;
		for(int iY=0;iY<HITPOINTS-2;iY++)
		{
			iPosX=rBounds.x+iSpacingX*2;
			for(int iX=0;iX<HITPOINTS-2;iX++)
			{
				if(!rDesktopBounds.contains(iPosX,iPosY) && !abHitPoints[iY][iX])
				{
					abHitPoints[iY][iX]=true;
					iHits++;
				}
				iPosX+=iSpacingX;
			}
			iPosY+=iSpacingY;
		}

		// Now see if it's behind something else
		int iLayer=getLayer(fi);
		int iPosition=getPosition(fi);

		// Loop through all higher components
		Component[] ac=getComponentsInLayer(iLayer);
		for(int i=0;i<ac.length;i++)
		{
			if(getPosition(ac[i])>=iPosition) continue;
			Rectangle rThis=ac[i].getBounds();

			iPosY=rBounds.y+iSpacingY*2;
			for(int iY=0;iY<HITPOINTS-2;iY++)
			{
				iPosX=rBounds.x+iSpacingX*2;
				for(int iX=0;iX<HITPOINTS-2;iX++)
				{
					if(rThis.contains(iPosX,iPosY) && !abHitPoints[iY][iX])
					{
						abHitPoints[iY][iX]=true;
						iHits++;
					}
					iPosX+=iSpacingX;
				}
				iPosY+=iSpacingY;
			}
		}

		// How much was covered area (very roughly)?
		return (iHits*100) / ((HITPOINTS-2)*(HITPOINTS-2)) <10;
	}

	/**
	 * Add an internal frame to the desktop.
	 * @param fi New internal frame
	 * @param initialScreen If true, the initial point is in screen co-ordinates,
	 *   otherwise it is in desktop co-ordinates
	 * @param initial Initial point to start at (null for default)
	 */
	public void addFrame(final FrameInside fi,final boolean initialScreen,
		final Point initial)
	{
		UISingleton.runInSwing(new Runnable()
		{
			@Override
			public void run()
			{
				add(fi);
				frameOrder.addLast(fi);

				Dimension size=overrideSize!=null ? overrideSize : getSize();

				// If it has a screen location, see if that'll work out
				if(initial!=null)
				{
					Point start=initialScreen
						?	new Point(	initial.x-getLocationOnScreen().x,
					  		initial.y-getLocationOnScreen().y)
						: new Point(initial.x,initial.y);
					Rectangle r=new Rectangle(start,fi.getSize());

					if(r.x<0) r.x=0;
					if(r.y<0) r.y=0;

					if(r.width>size.width) r.width=size.width;
					if(r.height>size.height) r.height=size.height;
					if(r.x+r.width > size.width)
					{
						r.x=size.width-r.width;
					}
					if(r.y+r.height> size.height)
					{
						r.y=size.height-r.height;
					}
					fi.setBounds(r);
				}
				else
				{
					// Work out desired size and location
					Rectangle r=new Rectangle(pAddLocation,fi.getSize());
					pAddLocation.x=r.x+20;
					pAddLocation.y=r.y+20;
					if(r.width>size.width) r.width=size.width;
					if(r.height>size.height) r.height=size.height;
					if(r.x+r.width > getWidth())
					{
						r.x=10;
						pAddLocation.x=30;
					}
					if(r.y+r.height> size.height)
					{
						r.y=10;
						pAddLocation.y=30;
					}
					fi.setBounds(r);
				}

				// Put in front
//				moveToFront(fi);
				sb.addFrame(fi);
			}
		});
	}

	/**
	 * Remove an existing frame
	 * @param fi Frame
	 */
	public void removeFrame(final FrameInside fi)
	{
		UISingleton.runInSwing(new Runnable()
		{
			@Override
			public void run()
			{
				remove(fi);
				frameOrder.remove(fi);
				sb.removeFrame(fi);
				repaint();
			}
		});
	}

	void informActive(FrameInside fi)
	{
		sb.informActiveFrame(fi);
	}

	/**
	 * Temporarily fixes the size of new windows created by the
	 * {@link #addFrame(FrameInside, boolean, Point)} method.
	 * @param dimension Size to use from now on instead of window default size
	 * @see #clearOverrideSize()
	 */
	public void setOverrideSize(Dimension dimension)
	{
		overrideSize=dimension;
	}

	/**
	 * Clears the size temporarily set by {@link #setOverrideSize(Dimension)}.
	 */
	public void clearOverrideSize()
	{
		overrideSize=null;
	}
}
