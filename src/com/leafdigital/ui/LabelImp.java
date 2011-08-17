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
import java.io.IOException;
import java.net.*;
import java.util.*;

import javax.swing.JComponent;

import org.w3c.dom.*;

import textlayout.StaticLayout;
import textlayout.stylesheet.*;
import util.PlatformUtils;
import util.xml.XML;

import com.leafdigital.ui.api.*;
import com.leafdigital.ui.api.TextView.ActionHandler;
import com.leafdigital.ui.api.Label;

import leafchat.core.api.*;

/** A wrappable label */
public class LabelImp extends JComponent implements BaseGroup
{
	private ActionStaticLayout sl;
	private static StyleContext smallContext;

	private String text="";
	private boolean small=false;

	private String widthGroup, baseGroup;
	private int topOffset=0;
	private int macIndent = 0;

	/** Map of actions from URL -> ActionHandler */
	private Map<String, ActionHandler> actions =
		new HashMap<String, ActionHandler>();

	private Set<BaseGroup> getWidthGroup()
	{
		return ((InternalWidgetOwner)li.getOwner()).getArbitraryGroup("width\n"+widthGroup);
	}

	/** Constructor */
	public LabelImp()
	{
		setLayout(null);
		sl=new ActionStaticLayout("",-1);
		add(sl);

		// Add default URL action
		li.setAction("url",new TextView.ActionHandler()
		{
			@Override
			public void action(Element e,MouseEvent me)
			{
				String sURL=XML.getText(e);
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

	@Override
	public void setBounds(int x,int y,int width,int height)
	{
		super.setBounds(x,y,width,height);
		relayout();
	}

	private void relayout()
	{
		sl.setBounds(macIndent, topOffset, getWidth()-macIndent, getHeight()-topOffset);
	}

	@Override
	public int getBaseline()
	{
		return sl.getFirstBaseline();
	}
	@Override
	public InternalWidgetOwner getInternalWidgetOwner()
	{
		return (InternalWidgetOwner)li.getOwner();
	}
	@Override
	public void setTopOffset(int topOffset)
	{
		if(this.topOffset==topOffset) return;
		this.topOffset=topOffset;
		relayout();
	}

	Label getInterface() { return li; }

	LabelInterface li=new LabelInterface();

	class LabelInterface extends BasicWidget implements Label, InternalWidget
	{
		private int iMinWidth=-1;

		@Override
		public int getContentType() { return CONTENT_NONE; }

		@Override
		public String getText()
		{
			return text;
		}

		@Override
		public void setText(final String text)
		{
			LabelImp.this.text=text;
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						StyleContext sc=TextViewImp.getDefaultStyle(getUI(),false);
						if(small)
						{
							if(smallContext==null)
							{
								smallContext=new StyleContext(sc);
								smallContext.addStylesheet(new Stylesheet("output { font-size:0.85f }"));
							}
							sc=smallContext;
						}
						sl.setText(text,sc);
						if(baseGroup!=null)
							BaseGroup.Updater.updateGroup(LabelImp.this,baseGroup);
						relayout();
					}
					catch(Exception e)
					{
						ErrorMsg.report("Unexpected error setting text",e);
					}
				}
			});
		}

		@Override
		public void setDefaultWidth(int width)
		{
			sl.setPreferredWidth(width);
		}

		@Override
		public JComponent getJComponent()
		{
			return LabelImp.this;
		}

		@Override
		public int getPreferredWidth()
		{
			int inner=getInnerPreferredWidth();
			if(widthGroup==null)	return inner;

			Set<BaseGroup> s = getWidthGroup();
			synchronized(s)
			{
				// Need to convert to array or we get commodification
				LabelImp[] labels=s.toArray(new LabelImp[s.size()]);
				for(int i=0;i<labels.length;i++)
				{
					inner=Math.max(inner,
						((LabelInterface)labels[i].getInterface()).getInnerPreferredWidth());
				}
			}

			return inner;
		}

		private int getInnerPreferredWidth()
		{
			int preferred = sl.getPreferredWidth();
			if(iMinWidth!=-1)
			{
				preferred=Math.max(iMinWidth,preferred);
			}
			preferred += macIndent;
			return preferred;
		}

		@Override
		public int getPreferredHeight(int width)
		{
			int h=sl.getPreferredHeight(width);
			h+=topOffset;
			return h;
		}

		@Override
		public void addXMLChild(String slotName, Widget child)
		{
			throw new BugException("Labels cannot contain children");
		}

		@Override
		public void setMinWidth(int width)
		{
			iMinWidth=width;
		}

		@Override
		public void setSmall(boolean small)
		{
			if(LabelImp.this.small==small) return;
			LabelImp.this.small=small;
			setText(text);
		}

		@Override
		public void setWidthGroup(String group)
		{
			LabelImp.this.widthGroup=group;
			Set<BaseGroup> s = getWidthGroup();
			synchronized(s)
			{
				s.add(LabelImp.this);
			}
		}

		@Override
		public void setBaseGroup(String group)
		{
			if(baseGroup!=null)
			{
				BaseGroup.Updater.removeFromGroup(LabelImp.this,baseGroup);
				baseGroup=null;
			}
			if(group!=null)
			{
				baseGroup=group;
				BaseGroup.Updater.addToGroup(LabelImp.this,baseGroup);
			}
		}

		@Override
		public void setAction(String tag,ActionHandler ah)
		{
			actions.put(tag,ah);
		}

		@Override
		public void setMacIndent(boolean macIndent)
		{
			setMacIndent(macIndent ? SupportsMacIndent.TYPE_EDIT_LEGACY
				: SupportsMacIndent.TYPE_NONE);
		}

		@Override
		public void setMacIndent(String macIndent)
		{
			int newValue = UISingleton.getMacIndent(macIndent);
			if(newValue != LabelImp.this.macIndent)
			{
				LabelImp.this.macIndent = newValue;
				setText(text);
			}
		}
	}

	// Debugging
	@Override
	protected void paintChildren(Graphics g)
	{
		super.paintChildren(g);
		BaseGroup.Debug.paint(g,this,topOffset);
	}

	private class ActionStaticLayout extends StaticLayout
	{
		/**
		 * @param text
		 * @param preferredWidth
		 */
		public ActionStaticLayout(String text,int preferredWidth)
		{
			super(text,preferredWidth);
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
	}
}
