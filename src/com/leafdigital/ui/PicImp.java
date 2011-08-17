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

import java.awt.Graphics;
import java.awt.event.*;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

import com.leafdigital.ui.api.*;

import leafchat.core.api.BugException;

/**
 * Implementation of Pic widget.
 */
public class PicImp extends JComponent
{
	private PicInterface pi=new PicInterface();

	/**
	 * @return Pic interface
	 */
	public Pic getInterface()
	{
		return pi;
	}

	private String themeType=null,propertyName=null;
	private String hoverThemeType=null,hoverPropertyName=null;
	private Theme fixedTheme;
	private String fixedFilename;

	private String onAction;
  private boolean inside;

	PicImp(UISingleton ui)
	{
		pi.setUI(ui);
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if(onAction!=null)
				{
					requestFocus();
					getInterface().getOwner().getCallbackHandler().callHandleErrors(onAction);
				}
			}
			@Override
			public void mouseEntered(MouseEvent e)
			{
				inside=true;
				repaint();
			}
			@Override
			public void mouseExited(MouseEvent e)
			{
				inside=false;
				repaint();
			}
		});
	}

	private BufferedImage getImage()
	{
		if(themeType!=null)
			return pi.getUI().getTheme().getImageProperty(themeType,propertyName,true,null,null);
		if(fixedTheme!=null)
			return fixedTheme.getImageProperty(null,fixedFilename,true,null,null);
		return null;
	}

	private BufferedImage getHover()
	{
		if(hoverThemeType==null) return getImage();
		return pi.getUI().getTheme().getImageProperty(hoverThemeType,hoverPropertyName,true,null,null);
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		// Centre image
		BufferedImage bi=inside ? getHover() : getImage();
		if(bi!=null)
			g.drawImage(bi,(getWidth()-bi.getWidth())/2,(getHeight()-bi.getHeight())/2,this);
	}

	private class PicInterface extends BasicWidget implements Pic
	{
		@Override
		public void setProperty(String name)
		{
			fixedTheme=null;
			fixedFilename=null;
			if(name==null)
			{
				themeType=null;
				propertyName=null;
			}
			else
			{
				int slash=name.indexOf('/');
				if(slash==-1) throw new BugException(
					"Unexpected name "+name+"; should be of format category/filename");
				themeType=name.substring(0,slash);
				propertyName=name.substring(slash+1);
			}

			revalidate();
			repaint();
		}

		@Override
		public void setHover(String name)
		{
			if(name==null)
			{
				hoverThemeType=null;
				hoverPropertyName=null;
			}
			else
			{
				int slash=name.indexOf('/');
				if(slash==-1) throw new BugException(
					"Unexpected name "+name+"; should be of format category/filename");
				hoverThemeType=name.substring(0,slash);
				hoverPropertyName=name.substring(slash+1);
			}

			revalidate();
			repaint();
		}

		@Override
		public void setOnAction(String callback)
		{
			getOwner().getCallbackHandler().check(callback);
			PicImp.this.onAction=callback;
			setFocusable(callback!=null);
		}

		@Override
		public void setThemeFile(Theme t,String filename)
		{
			fixedTheme=t;
			fixedFilename=filename;
			themeType=null;
			propertyName=null;

			revalidate();
			repaint();
		}

		@Override
		public void addXMLChild(String sSlotName,Widget wChild)
		{
			throw new BugException("No children!");
		}

		@Override
		public int getContentType()
		{
			return CONTENT_NONE;
		}

		@Override
		public JComponent getJComponent()
		{
			return PicImp.this;
		}

		@Override
		public int getPreferredHeight(int iWidth)
		{
			BufferedImage bi=getImage();
			if(bi==null) return 1;
			return bi.getHeight();
		}

		@Override
		public int getPreferredWidth()
		{
			BufferedImage bi=getImage();
			if(bi==null) return 1;
			return bi.getWidth();
		}

		@Override
		public void setTooltip(String tip)
		{
			setToolTipText(tip);
		}
	}

}
