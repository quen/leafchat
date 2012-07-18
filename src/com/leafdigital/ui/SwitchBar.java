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
import java.awt.image.BufferedImage;

import javax.swing.*;

import util.GraphicsUtils;

import com.leafdigital.ui.api.UI;

import leafchat.core.api.BugException;

/**
 * The bar at the bottom of the main window which lets you switch between
 * windows.
 */
public class SwitchBar extends JPanel
{
	/** Size index of buttons */
	private int buttonSize=1;

	/** Normal and attention colours */
	static Color normalRGB=Color.black,attentionRGB=Color.red,minRGB=Color.gray;

	/** Button fonts */
	private Font tabButtonFont,tabButtonFontBold;

	/** UI (for accessing theme) */
	private UISingleton ui;

	SwitchBar(UISingleton ui)
	{
		this.ui=ui;
		setLayout(null);
		setOpaque(true);

		// Get default button font size (0.85* JButton font)
		JButton fontSample=new JButton("Hello");
		int defaultSize=Math.round(0.85f*fontSample.getFont().getSize());

		// Find most suited button set
		for(buttonSize=1;buttonSize<=ui.getTheme().getIntProperty("tabs","numSizes",1);buttonSize++)
		{
			int fontSize=getIntProperty("titleSize");
			if(fontSize==0) // Run out of sizes
			{
				buttonSize--;
				break;
			}
			else if(fontSize >= defaultSize)
			{
				// First one bigger than our desired size will do
				break;
			}
		}

		// Set up fonts
		tabButtonFont=fontSample.getFont().deriveFont((float)getIntProperty("titleSize"));
		tabButtonFontBold=tabButtonFont.deriveFont(Font.BOLD);

		// Listen for keypresses.

		// I tried to do this with a normal input map but couldn't get it to work!
		// It basically worked but not if you minimised all the windows - that left
		// focus in some bizarre state.

		// Note that this evil code means it's important SwitchBar is not
		// constructed more than once, so we check
		if(constructedAlready) throw new BugException(
			"SwitchBar may not be constructed more than once");
		constructedAlready=true;

		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(
			new KeyEventDispatcher()
			{
				@Override
				public boolean dispatchKeyEvent(KeyEvent e)
				{
					if(
						(e.getID()==KeyEvent.KEY_PRESSED) &&
						((e.getModifiers() & (KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK |
							KeyEvent.META_MASK | KeyEvent.CTRL_MASK))==KeyEvent.ALT_MASK) &&
						(e.getKeyCode() >= KeyEvent.VK_0) &&
						(e.getKeyCode() <= KeyEvent.VK_9) &&
						isVisible())
					{
						int key=e.getKeyCode()-KeyEvent.VK_0;
						clickButton(key==0 ? 9 : key-1);
						return true;
					}
					else
						return false;
				}
			});
	}

	private static boolean constructedAlready;

	/**
	 * Clicks on a specific button.
	 * @param index Index (0-based) of button. May not exist in which case
	 *   nothing happens
	 */
	private void clickButton(int index)
	{
		Component[] ac=getComponents();
		if(ac.length<=index)
			return;
		((SwitchButton)ac[index]).click();
	}

	int uiStyle=UI.UISTYLE_SINGLEWINDOW;

	void setUIStyle(int style)
	{
		this.uiStyle=style;
	}

	@Override
	public void paint(Graphics g)
	{
		if(uiStyle==UI.UISTYLE_TABBED)
		{
			// Paint background
			BufferedImage bg=getImageProperty("background");
			for(int x=0;x<getWidth();x+=bg.getWidth())
			{
				g.drawImage(bg,x,0,null);
			}

			// Paint all the background switchbuttons
			Component[] ac=getComponents();
			for(int i=0;i<ac.length;i++)
			{
				if((ac[i] instanceof TabSwitchButton) && !((TabSwitchButton)ac[i]).isFG())
				{
					paintChild(g,ac[i]);
				}
			}

			// Paint midground
			BufferedImage mid=getImageProperty("midground");
			for(int x=0;x<getWidth();x+=mid.getWidth())
			{
				g.drawImage(mid,x,0,null);
			}

			// Paint all the other components
			for(int i=0;i<ac.length;i++)
			{
				if(!((ac[i] instanceof TabSwitchButton) && !((TabSwitchButton)ac[i]).isFG()))
				{
					paintChild(g,ac[i]);
				}
			}
		}
		else
		{
			// Paint background
			BufferedImage bg=getImageProperty("buttonBackground");
			for(int x=0;x<getWidth();x+=bg.getWidth())
			{
				g.drawImage(bg,x,0,null);
			}
			paintChildren(g);
		}
	}

	private void paintChild(Graphics g,Component c)
	{
		g.setColor(c.getForeground());
		g.setFont(c.getFont());
		Rectangle bounds=c.getBounds();
		Shape clipBefore=g.getClip();
		g.clipRect(bounds.x,bounds.y,bounds.width,bounds.height);
		g.translate(bounds.x,bounds.y);
		c.paint(g);
		g.translate(-bounds.x,-bounds.y);
		g.setClip(clipBefore);
	}

	@Override
	public void setBounds(int x, int y, int width, int height)
	{
		super.setBounds(x, y, width, height);
		rearrangeButtons();
	}

	void rearrangeButtons()
	{
		Insets i=getInsets();

		Component[] ac=getComponents();
		if(ac.length==0) return;

		int gap=uiStyle==UI.UISTYLE_SINGLEWINDOW ? getIntProperty("buttonGap") : getIntProperty("gap");

		int iButtonWidth=(getWidth()-i.left-i.right-2*getIntProperty("gap"))/ac.length;

		int preferredWidth=uiStyle==UI.UISTYLE_SINGLEWINDOW ? getIntProperty("buttonWidth") : getIntProperty("width");
		if(iButtonWidth>preferredWidth) iButtonWidth=preferredWidth;
		if(iButtonWidth<preferredWidth/4) iButtonWidth=preferredWidth/4;

		int iX=i.left+gap;
		for (int iComponent= 0; iComponent < ac.length; iComponent++)
		{
			ac[iComponent].setBounds(iX,i.top,
			  iButtonWidth-gap,getHeight()-i.top-i.bottom);
			iX+=iButtonWidth;
		}
	}

	private void informMoved()
	{
		Component[] ac=getComponents();
		for(int i=0;i<ac.length;i++)
		{
			if(ac[i] instanceof SwitchButton)
			{
				SwitchButton sb=(SwitchButton)ac[i];
				if(sb.hasAttention())
				{
					((JComponent)sb).repaint();
				}
			}
		}
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(100,getIntProperty("height"));
	}

	void addFrame(FrameInside fi)
	{
		add(new InsideSwitchButton(fi));
		rearrangeButtons();
	}

	void removeFrame(FrameHolder fh)
	{
		Component[] ac=getComponents();
		for (int iComponent= 0; iComponent < ac.length; iComponent++)
		{
			SwitchButton sb=(SwitchButton)ac[iComponent];
			if(sb.getFrame()==fh)
				  sb.close();
		}
		rearrangeButtons();
	}

	/**
	 * Adds a tab to the switchbar.
	 * @param tab Tab
	 */
	public void addFrame(FrameTab tab)
	{
		add(new TabSwitchButton(tab));
		rearrangeButtons();
	}

	private FrameHolder active=null;

	void informActiveFrame(FrameHolder active)
	{
		if(this.active==active) return;
		this.active=active;
		repaint();
	}

	FrameHolder getActiveFrame()
	{
		return active;
	}

	private int getIntProperty(String name)
	{
		return ui.getTheme().getIntProperty("tabs",name+buttonSize,0);
	}

	private BufferedImage getImageProperty(String name)
	{
		return ui.getTheme().getImageProperty("tabs",name+buttonSize,true,null,null);
	}

	class InsideSwitchButton extends JComponent implements SwitchButton
	{
		private FrameInside fi;
		private JLabel title;

		InsideSwitchButton(FrameInside fiSet)
		{
			this.fi=fiSet;
			fi.setSwitchButton(this);

			setPreferredSize(new Dimension(200,getIntProperty("height")));
			setLayout(null);
			title=new JLabel(fi.getTitle());
			title.setFont(tabButtonFont);
			add(title);

			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					click();
				}
			});
			addKeyListener(new KeyAdapter()
			{
				@Override
				public void keyTyped(KeyEvent e)
				{
					if(e.getKeyChar()==' ' || e.getKeyChar()=='\n')
					{
						click();
					}
				}
			});

			setFocusable(true);
		}

		@Override
		public void click()
		{
			if(!fi.isVisible())
			{
				fi.handleRestore();
				fi.focusFrame();
			}
			else if(isActive())
			{
				fi.handleMinimize();
			}
			else
			{
				fi.focusFrame();
			}
		}

		@Override
		public void setBounds(int x,int y,int width,int height)
		{
			super.setBounds(x,y,width,height);

			int ascent=(int)tabButtonFont.getLineMetrics("Aj",
				GraphicsUtils.getFontRenderContext()).getAscent();
			title.setBounds(
				getIntProperty("buttonTitleLeftOffset"),
				height-getIntProperty("buttonTitleBaselineOffset")-ascent,
				getWidth()-getIntProperty("buttonTitleLeftOffset")-getIntProperty("buttonTitleRightOffset"),
			  title.getPreferredSize().height
				);
		}

		private void checkActiveState(boolean newActive)
		{
			if(newActive!=paintedActive)
			{
				paintedActive=newActive;
				title.setFont(paintedActive ? tabButtonFontBold : tabButtonFont);
			}

			if(attention && fi.canClearAttention())
			{
				if(paintedActive || fi.isReasonablyVisible())
				  attention=false;
			}

			Color rgb=attention ? attentionRGB : !fi.isVisible() ? minRGB : normalRGB;
			if(title.getForeground()!=rgb)
				title.setForeground(rgb);
		}

		boolean paintedActive=false;

		private boolean attention;

		boolean isActive()
		{
			return fi==active && fi.isVisible();
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			// Set up button if needed
			boolean active=isActive();
			checkActiveState(active);
			String activeText=active ? "Active" : "Inactive";
			// Draw background
			BufferedImage
				left=getImageProperty("buttonLeft"+activeText),
				middle=getImageProperty("buttonMiddle"+activeText),
				right=getImageProperty("buttonRight"+activeText);
			g.drawImage(left,0,0,null);
			int rightPos=getWidth()-right.getWidth();
			Shape s=g.getClip();
			g.clipRect(0,0,rightPos,getHeight());
			for(int pos=left.getWidth();pos<500;pos+=middle.getWidth())
			{
				g.drawImage(middle,pos,0,null);
			}
			g.setClip(s);
			g.drawImage(right,rightPos,0,null);

			if(hasFocus())
				GraphicsUtils.drawFocus((Graphics2D)g,0,0,getWidth(),getHeight());
		}

		@Override
		public void attention()
		{
			if(attention) return;
			attention=true;
			repaint();
		}

		@Override
		public boolean hasAttention()
		{
			return attention;
		}

		@Override
		public void close()
		{
			getParent().remove(this);
			SwitchBar.this.repaint();
		}

		@Override
		public FrameHolder getFrame()
		{
			return fi;
		}

		@Override
		public void informChanged()
		{
			String newTitle=fi.getTitle();
			if(!title.getText().equals(newTitle)) title.setText(newTitle);
			repaint();
		}

		@Override
		public void informMoved()
		{
			SwitchBar.this.informMoved();
		}
	}

	class TabSwitchButton extends JComponent implements SwitchButton
	{
		private FrameTab ft;
		private JLabel title;
		private JButton close;

		TabSwitchButton(FrameTab ft)
		{
			this.ft=ft;
			ft.setSwitchButton(this);

			setPreferredSize(new Dimension(200,getIntProperty("height")));
			setLayout(null);
			title=new JLabel(ft.getTitle());
			title.setFont(tabButtonFont);
			add(title);
			close=new JButton();
			close.setOpaque(false);
			close.setRolloverEnabled(true);
			close.setBorderPainted(false);
			close.setContentAreaFilled(false);
			close.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent ae)
				{
					TabSwitchButton.this.ft.handleClose();
				}
			});
			add(close);
			checkFGState(true);

			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					click();
				}
			});
			addKeyListener(new KeyAdapter()
			{
				@Override
				public void keyTyped(KeyEvent e)
				{
					if(e.getKeyChar()==' ' || e.getKeyChar()=='\n')
					{
						click();
					}
				}
			});

			setFocusable(true);
		}

		@Override
		public void click()
		{
			TabSwitchButton.this.ft.focusFrame();
		}

		@Override
		public void setBounds(int x,int y,int width,int height)
		{
			super.setBounds(x,y,width,height);

			int closeLeft;
			if(close.isVisible())
			{
				BufferedImage closeImg=getImageProperty("closeFG");
				closeLeft=getWidth()-closeImg.getWidth()-getIntProperty("closeRightOffset");
				close.setBounds(
					closeLeft,
					height-closeImg.getHeight()-getIntProperty("closeBottomOffset"),
					closeImg.getWidth(),closeImg.getHeight());
			}
			else
			{
				closeLeft=getWidth();
			}

			Font f=title.getFont();
			int ascent=(int)f.getLineMetrics("Aj",GraphicsUtils.getFontRenderContext()).getAscent();
			title.setBounds(
				getIntProperty("titleLeftOffset"),
				height-getIntProperty("titleBaselineOffset")-ascent,
				closeLeft-getIntProperty("titleLeftOffset")-getIntProperty("titleRightOffset"),
			  title.getPreferredSize().height
				);
		}

		private void checkFGState(boolean newFG)
		{
			if(newFG!=paintedFG)
			{
				paintedFG=newFG;

				String fg=newFG ? "FG" : "BG";

				close.setIcon(new ImageIcon(getImageProperty("close"+fg)));
				close.setRolloverIcon(new ImageIcon(getImageProperty("closeHover"+fg)));

				title.setFont(paintedFG ? tabButtonFontBold : tabButtonFont);
				repaint();
			}

			if(attention && paintedFG && ft.canClearAttention())
			{
				title.setForeground(normalRGB);
				attention=false;
				repaint();
			}
		}

		boolean paintedFG=false;

		private boolean attention;

		boolean isFG()
		{
			return ft==active;
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			// Set up button if needed
			boolean fg=isFG();
			checkFGState(fg);
			String fgText=fg ? "FG" : "BG";

			// Draw background
			BufferedImage
				left=getImageProperty("left"+fgText),
				middle=getImageProperty("middle"+fgText),
				right=getImageProperty("right"+fgText);
			g.drawImage(left,0,0,null);
			int rightPos=getWidth()-right.getWidth();
			Shape s=g.getClip();
			g.clipRect(0,0,rightPos,getHeight());
			for(int pos=left.getWidth();pos<500;pos+=middle.getWidth())
			{
				g.drawImage(middle,pos,0,null);
			}
			g.setClip(s);
			g.drawImage(right,rightPos,0,null);

			if(hasFocus())
				GraphicsUtils.drawFocus((Graphics2D)g,0,0,getWidth(),getHeight());
		}

		@Override
		public void attention()
		{
			if(attention) return;

			title.setForeground(attentionRGB);
			attention=true;
			repaint();
		}

		@Override
		public boolean hasAttention()
		{
			return attention;
		}

		@Override
		public void close()
		{
			getParent().remove(this);
			SwitchBar.this.repaint();
		}

		@Override
		public FrameHolder getFrame()
		{
			return ft;
		}

		@Override
		public void informChanged()
		{
			String newTitle=ft.getTitle();
			if(!title.getText().equals(newTitle)) title.setText(newTitle);

			if(ft.isClosable()!=close.isVisible())
			{
				close.setVisible(ft.isClosable());
				Rectangle r=getBounds();
				setBounds(r.x,r.y,r.width,r.height);
			}
		}

		@Override
		public void informMoved()
		{
			SwitchBar.this.informMoved();
		}
	}

	interface SwitchButton
	{
		FrameHolder getFrame();
		void informChanged();
		void informMoved();
		void attention();
		void close();
		boolean hasAttention();
		void click();
	}
}

