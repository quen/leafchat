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
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;

import org.w3c.dom.*;

import textlayout.*;
import textlayout.stylesheet.*;
import util.PlatformUtils;
import util.xml.*;

import com.leafdigital.ui.api.*;

import leafchat.core.api.*;
import leafchat.core.api.BugException;

/** Implements TextView using the ScrollingLayout class */
public class TextViewImp extends ScrollingLayout implements ThemeListener
{
	/** Limit to number of lines */
	private int lineLimit=TextView.LINELIMIT_NONE;

	/** Scroll callback */
	private String sScroll=null;

	/** Name in theme */
	private String themeType=null;

	/** Owner */
	private UISingleton owner;

	private TextView.MenuHandler mh;

	/** Currently marked position */
	private int markedPosition=0;

	/** Opacity of marker */
	private int markOpacity=255;

	/** True if a warning should be displayed when scrolled up */
	private boolean scrolledUpWarning=false;

	/** Map of actions from URL -> ActionHandler */
	private Map<String, TextView.ActionHandler> actions =
		new HashMap<String, TextView.ActionHandler>();

	/**
	 * Constructs at default size.
	 * @param owner Owner
	 */
	public TextViewImp(UISingleton owner)
	{
		super(200,200,getDefaultStyle(owner,true));
		this.owner=owner;
		owner.informThemeListener(this);

		// Add default URL action
		tvi.setAction("url",new TextView.ActionHandler()
		{
			@Override
			public void action(Element e,MouseEvent me)
			{
				String sURL=XML.getText(e, true, true);
				if(!(sURL.startsWith("http://") || sURL.startsWith("https://")))
					sURL="http://"+sURL;
				try
				{
					PlatformUtils.showBrowser(new URL(sURL));
				}
				catch(MalformedURLException e1)
				{
					ErrorMsg.report("Not a valid URL",e1);
				}
				catch(IOException e1)
				{
					ErrorMsg.report(e1.getMessage(),e1.getCause());
				}
				return;
			}
		});
	}

	private static StyleContext contextWithThemes=null,contextWithThemesTV=null;
	private static Theme currentTheme;

	static StyleContext getDefaultStyle(UISingleton ui,boolean textView)
	{
		Theme t=ui.getTheme();
		if(t==currentTheme)
		{
			if(textView && contextWithThemesTV!=null)
				return contextWithThemesTV;
			else if(!textView && contextWithThemes!=null)
				return contextWithThemes;
		}

		try
		{
			currentTheme=t;
			StyleContext creating=new StyleContext(StyleContext.getDefault(true));
			Stylesheet[] ss=t.getStylesheets();
			for(int i=0;i<ss.length;i++)
			{
				creating.addStylesheet(ss[i]);
			}
			if(textView)
			{
				creating.addStylesheet(new Stylesheet(	ui.getSettingsStylesheet()));
				contextWithThemesTV=creating;
			}
			else
			{
				contextWithThemes=creating;
			}
			creating.fix();
			return creating;
		}
		catch(Exception e)
		{
			throw new BugException(e);
		}
	}

	@Override
	protected boolean isAction(Node n,int iCharacter)
	{
		while(n!=null)
		{
			if((n instanceof Element) && actions.containsKey(n.getNodeName())) return true;
			n=n.getParentNode();
		}
		return false;
	}
	@Override
	protected void doAction(Node n,int iCharacter,MouseEvent me)
	{
		while(n!=null)
		{
			if(n instanceof Element)
			{
				TextView.ActionHandler ah=actions.get(n.getNodeName());
				if(ah!=null)
				{
					try
					{
						ah.action((Element)n,me);
					}
					catch(GeneralException ge)
					{
						ErrorMsg.report("Error carrying out text view action",ge);
					}
					return;
				}
			}
			n=n.getParentNode();
		}
	}

  @Override
	protected JPopupMenu buildMenu(Node n,int character)
  {
  		PopupMenuImp pm=new PopupMenuImp();
  		if(mh!=null)
  		{
  			mh.addItems(pm.getInterface(),n);
  		}
  		return pm;
  }

	@Override
	protected void scrollbarChanged()
	{
		super.scrollbarChanged();
		if(sScroll!=null)
		{
			getInterface().getOwner().getCallbackHandler().callHandleErrors(sScroll);
		}
	}

	TextView getInterface() { return tvi; }

	private TextViewInterface tvi=new TextViewInterface();

	class TextViewInterface extends BasicWidget implements TextView,InternalWidget
	{
		@Override
		public int getContentType() { return CONTENT_NONE; }

		TextViewImp getImp() { return TextViewImp.this; }

		@Override
		public void setThemeType(String themeType)
		{
			TextViewImp.this.themeType=themeType;
			updateTheme(getUI().getTheme());
		}

		@Override
		public void setDefaultWidth(int iWidth)
		{
			setPreferredSize(new Dimension(iWidth,getPreferredSize().height));
		}

		@Override
		public void setDefaultHeight(int iHeight)
		{
			setPreferredSize(new Dimension(getPreferredSize().width,iHeight));
		}

		@Override
		public JComponent getJComponent()
		{
			return TextViewImp.this;
		}

		@Override
		public int getPreferredWidth()
		{
			return getPreferredSize().width;
		}

		@Override
		public int getPreferredHeight(int iWidth)
		{
			return getPreferredSize().height;
		}

		@Override
		public void addXMLChild(String sSlotName, Widget wChild)
		{
			throw new BugException("TextViews cannot contain children");
		}

		@Override
		public void addPara(String sPara) throws GeneralException
		{
			addXML("<para>"+sPara+"</para>");
		}

		@Override
		public void addLine(String sLine) throws GeneralException
		{
			addXML("<line>"+sLine+"</line>");
		}

		@Override
		public void addXML(String xml) throws GeneralException
		{
			try
			{
				String tag=themeType==null ? "output" : "theme_"+themeType;
				TextViewImp.this.addBlocks(
					XML.parse("<"+tag+">"+xml+"</"+tag+">").getDocumentElement());
				if(lineLimit!=LINELIMIT_NONE && getNumBlocks() >= lineLimit + (lineLimit/4))
				{
					markedPosition-=deleteFirstBlocks(lineLimit/4);
					if(markedPosition<0) markedPosition=0;
				}
			}
			catch(XMLException e)
			{
				throw new GeneralException("Invalid XML: "+xml,e);
			}
			catch(LayoutException e)
			{
				throw new GeneralException(e);
			}
		}

		@Override
		public void setStyleSheet(InputStream is) throws GeneralException
		{
			try
			{
				StyleContext sc=new StyleContext(getDefaultStyle(owner,true));
				sc.addStylesheet(new Stylesheet(is));
				updateStyle(sc);
			}
			catch(Exception e)
			{
				// TODO Improve this handling
				throw new GeneralException(e);
			}
		}

		@Override
		public void setStyleSheet(String styles) throws GeneralException
		{
			try
			{
				StyleContext sc=new StyleContext(getDefaultStyle(owner,true));
				sc.addStylesheet(new Stylesheet(styles));
				updateStyle(sc);
			}
			catch(Exception e)
			{
				// TODO Improve this handling
				throw new GeneralException(e);
			}
		}

		@Override
		public void scrollToEnd()
		{
			TextViewImp.this.scrollToEnd();
		}

		@Override
		public boolean isAtEnd()
		{
			return TextViewImp.this.isAtEnd();
		}

		@Override
		public void setOnScroll(String sCallback)
		{
			getInterface().getOwner().getCallbackHandler().check(sCallback);
			TextViewImp.this.sScroll=sCallback;
		}

		@Override
		public void clear()
		{
			TextViewImp.this.clear();
		}

		@Override
		public void setAction(String tag,ActionHandler ah)
		{
			actions.put(tag,ah);
		}

		@Override
		public void copy()
		{
			TextViewImp.this.copy();
		}

		@Override
		public void selectAll()
		{
			TextViewImp.this.highlightAll();
		}

		@Override
		public void selectNone()
		{
			TextViewImp.this.clearHighlight();
		}

		@Override
		public boolean hasSelection()
		{
			return hasHighlight();
		}

		@Override
		public void setMenuHandler(MenuHandler mh)
		{
			TextViewImp.this.mh=mh;
		}

		@Override
		public void markPosition()
		{
			if(markedPosition!=0) repaint(); // To get rid of old marker
			markedPosition=getLayoutHeight();
			markOpacity=255;
		}

		@Override
		public void fadeMark(int opacity)
		{
			if(markedPosition==0) return;
			markOpacity=opacity;
			repaint();
		}

		@Override
		public void removeMark()
		{
			if(markedPosition==0) return;
			markedPosition=0;
			repaint();
		}

		@Override
		public boolean hasMark()
		{
			return markedPosition!=0;
		}

		@Override
		public void setLineLimit(int limit)
		{
			lineLimit=limit;
		}

		@Override
		public void setScrolledUpWarning(boolean enable)
		{
			if(scrolledUpWarning==enable) return;
			scrolledUpWarning=enable;
			repaint();
		}
	}

	@Override
	public void updateTheme(Theme t)
	{
		// This is a bit lame, means we'll reload the stylesheet for each textview,
		// but I couldn't think of a better way
		contextWithThemesTV=null;
		currentTheme=null;

		if(themeType==null)
		{
			setMargins(0,0);
		}
		else
		{
			setMargins(
				t.getIntProperty(themeType,"leftMargin",0),
				t.getIntProperty(themeType,"rightMargin",0));
		}
		try
		{
			updateStyle(getDefaultStyle(owner,true));
		}
		catch(LayoutException le)
		{
			throw new BugException("Error setting textview style",le);
		}

		repaint();
	}

	@Override
	protected void paintBehind(Graphics g,int width,int height, int startY)
	{
		// Draw theme background
		BufferedImage
			bottomLeft=null,bottomRight=null,topLeft=null,topRight=null,
			top=null,bottom=null,left=null,right=null;
		if(themeType!=null)
		{
			Theme t=owner.getTheme();
			bottomLeft=t.getImageProperty(themeType,"bottomLeftPic",true,null,null);
			bottomRight=t.getImageProperty(themeType,"bottomRightPic",true,null,null);
			topLeft=t.getImageProperty(themeType,"topLeftPic",true,null,null);
			topRight=t.getImageProperty(themeType,"topRightPic",true,null,null);
			top=t.getImageProperty(themeType,"topPic",true,null,null);
			bottom=t.getImageProperty(themeType,"bottomPic",true,null,null);
			left=t.getImageProperty(themeType,"leftPic",true,null,null);
			right=t.getImageProperty(themeType,"rightPic",true,null,null);
		}

		if(top!=null)
		{
			for(int x=0;x<width;x+=top.getWidth())
				g.drawImage(top,x,0,null);
		}
		if(bottom!=null)
		{
			for(int x=0;x<width;x+=bottom.getWidth())
				g.drawImage(bottom,x,height-bottom.getHeight(),null);
		}
		if(left!=null)
		{
			for(int y=0;y<height;y+=left.getHeight())
				g.drawImage(left,0,y,null);
		}
		if(right!=null)
		{
			for(int y=0;y<height;y+=right.getHeight())
				g.drawImage(right,width-right.getWidth(),y,null);
		}
		if(topLeft!=null)
			g.drawImage(topLeft,0,0,null);
		if(topRight!=null)
			g.drawImage(topRight,width-topRight.getWidth(),0,null);
		if(bottomLeft!=null)
			g.drawImage(bottomLeft,0,height-bottomLeft.getHeight(),null);
		if(bottomRight!=null)
			g.drawImage(bottomRight,width-bottomRight.getWidth(),height-bottomRight.getHeight(),null);

		// Draw position marker if it is not the end
		if(markedPosition!=0 && getLayoutHeight()!=markedPosition)
		{
			g.setColor(new Color(
				SwitchBar.attentionRGB.getRed(),
				SwitchBar.attentionRGB.getGreen(),
				SwitchBar.attentionRGB.getBlue(),
				markOpacity
				));
			Graphics2D g2=(Graphics2D)g;
			g2.setStroke(new BasicStroke(1,
				BasicStroke.CAP_SQUARE,BasicStroke.JOIN_MITER,10,new float[] {1,2},0));
			g2.drawLine(1,markedPosition-startY,width,markedPosition-startY);
		}

		// Draw scrolled-up warning if enabled
		if(scrolledUpWarning)
		{
			int distance=getDistanceFromEnd();
			if(distance>0)
			{
				int value=Math.min(distance,16);
				for(int i=0;i<4;i++)
				{
					g.setColor(new Color(
						SwitchBar.attentionRGB.getRed(),
						SwitchBar.attentionRGB.getGreen(),
						SwitchBar.attentionRGB.getBlue(),
						(4-i)*value));
					g.fillRect(0,height-1-i,width,1);
				}
			}
		}
	}
}
