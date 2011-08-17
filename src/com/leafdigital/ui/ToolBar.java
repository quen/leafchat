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
import java.util.*;

import javax.swing.*;

import com.leafdigital.ui.api.*;

/** Toolbar at top of window */
public class ToolBar extends JPanel implements ThemeListener
{
	/** Space between buttons */
	private final static int BUTTONSPACING=4;

	/** Map of Tool -> ToolInfo */
	private Map<Tool, ToolInfo> m = new HashMap<Tool, ToolInfo>();

	/** UI singleton */
	private UISingleton ui;

	private BufferedImage tileOriginal, tileStretched;
	private int tileHeight=-1;

	private int usedWidth = 0;

	ToolBar(UISingleton ui)
	{
		this.ui = ui;
		setLayout(null);
		setOpaque(false);
		setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		ui.informThemeListener(this);
	}

	@Override
	public void updateTheme(Theme t)
	{
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		BufferedImage tile = ui.getTheme().getImageProperty(
			"toolbar", "tilePic", false, null, null);
		if(tile==null)
		{
			// Fill background with default colour
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
		}
		else
		{
			if(tile!=tileOriginal || getHeight()!=tileHeight)
			{
				// Stretch image if needed
				tileOriginal = tile;
				if(tileOriginal.getHeight() == getHeight())
				{
					tileStretched=tileOriginal;
				}
				else
				{
					tileStretched = new BufferedImage(
						tileOriginal.getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
					Graphics2D g2 = tileStretched.createGraphics();
					g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_BICUBIC);
					g2.drawImage(tileOriginal,
						0, 0, tileStretched.getWidth(), tileStretched.getHeight(),
						0, 0, tileOriginal.getWidth(), tileOriginal.getHeight(), null);
				}
				tileHeight=tileStretched.getHeight();
			}

			// Draw background using tile
			for(int x=0; x<getWidth(); x+=tileStretched.getWidth())
			{
				g.drawImage(tileStretched, x, 0, null);
			}
		}
	}

	@Override
	public Dimension getPreferredSize()
	{
		Insets i = getInsets();
		ToolButton button = new ToolButton(ui);

		return new Dimension(
			usedWidth, button.getPreferredSize().height + i.top + i.bottom);
	}

	@Override
	public void setBounds(int x, int y, int width, int height)
	{
		super.setBounds(x, y, width, height);
		rearrangeTools();
	}

	@Override
	public void validate()
	{
		super.validate();
		rearrangeTools();
	}

	void rearrangeTools()
	{
		UISingleton.runInSwing(new Runnable()
		{
			@Override
			public void run()
			{
				// Get list of tools and sort it by current position (note: we don't use
				// a sorted set because I'm not sure whether that handles position changing
				// after creation)
				ToolInfo[] tools = m.values().toArray(new ToolInfo[0]);
				Arrays.sort(tools);

				// Work out default/initial positions
				Insets i = getInsets();
				int x=i.left, y=i.top, height=getHeight()-i.top-i.bottom;

				// For each tool, add it if necessary and update position
				for(int tool=0; tool<tools.length; tool++)
				{
					Component c = tools[tool].c;
					Dimension toolSize = c.getPreferredSize();
					int width = toolSize.width;
					if(!tools[tool].added)
					{
						add(c);
						tools[tool].added = true;
					}
					c.setBounds(x, y, width, height);
					x += width + BUTTONSPACING;
				}

				usedWidth = x;
				repaint();
			}
		});
	}

	/**
	 * Registers a tool (adds it to the toolbar).
	 * @param t Tool to add
	 */
	public void register(final Tool t)
	{
		UISingleton.runInSwing(new Runnable()
		{
			@Override
			public void run()
			{
				// Create info about tool
				ToolInfo ti=new ToolInfo();
				ti.position=t.getDefaultPosition();

				// Set up tool's actual component
				if(t instanceof SimpleTool)
				{
					SimpleTool st=(SimpleTool)t;
					ti.c=new ToolButton(ui, st.getLabel(), st.getThemeType(), st);
				}
				else if(t instanceof PageTool)
				{
					PageTool pt=(PageTool)t;
					ti.c=new ToolPage(pt);
				}
				else throw new IllegalArgumentException("Unexpected tool type");

				m.put(t, ti);
				rearrangeTools();
			}
		});
	}

	/**
	 * Informs the toolbar that it's closed
	 */
	void informClosed()
	{
		for(ToolInfo ti :m.values())
		{
			if(ti.c instanceof ToolPage)
			{
				((ToolPage)ti.c).getPage().informClosed();
			}
		}
	}

	/**
	 * Unregisters a tool (removes it from the toolbar).
	 * @param t Tool to remove
	 */
	public void unregister(Tool t)
	{
		ToolInfo info = m.get(t);
		if(info.added)
		{
			remove(info.c);
			info.added = false;
		}
		m.remove(t);

		rearrangeTools();
	}

	private static class ToolInfo implements Comparable<ToolInfo>
	{
		int position;
		Component c;
		boolean added=false;

		@Override
		public int compareTo(ToolInfo other)
		{
			return position - other.position;
		}
	}
}
