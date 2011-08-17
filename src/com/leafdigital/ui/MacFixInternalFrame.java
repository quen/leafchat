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

/**
 * This class is basically an evil hack to make the growbox in an InternalFrame
 * 'internal' instead of taking extra space (which screws up various other
 * things)
 */
public class MacFixInternalFrame extends JInternalFrame
{
	/** Evil hack to get rid of the intrusive growbox */
	private Component macHack=null;

	@Override
	public void reshape(int x,int y,int width,int height)
	{
		super.reshape(x,y,width,height);
		if(macHack!=null)
		{
			Insets i=getInsets();
			getComponent(0).setBounds(i.left,i.top,
				getWidth()-i.left-i.right,getHeight()-i.top-i.bottom);
			macHack.setBounds(getWidth()-i.left-i.right-15,
				getHeight()-i.top-i.bottom-15,15,	15);
		}
	}

	@Override
	protected boolean isRootPaneCheckingEnabled()
	{
		return false;
	}

	MacFixInternalFrame()
	{
		if(getComponentCount()>1 &&
			getComponent(1).getClass().getName().endsWith(".AquaInternalFrameGrowPane"))
		{
			macHack=getComponent(1);
			setLayout(null);
			remove(1);
			getLayeredPane().add(macHack, JLayeredPane.DEFAULT_LAYER + 1);
		}
	}

	@Override
	public Dimension getPreferredSize()
	{
		if(macHack==null) return super.getPreferredSize();
		Dimension preferred=getRootPane().getPreferredSize();
		Insets i=getInsets();
		preferred.width+=i.left+i.right;
		preferred.height+=i.top+i.bottom;
		return preferred;
	}

	private static Insets standard=null;

	/**
	 * @return Standard insets
	 */
	public static Insets getStandardInsets()
	{
		if(standard==null)
		{
			MacFixInternalFrame f=new MacFixInternalFrame();
			standard=f.getInsets();
		}
		return standard;
	}
}
