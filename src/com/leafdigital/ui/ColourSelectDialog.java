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

import javax.swing.*;

import util.PlatformUtils;

/**
 * Dialog for selecting a colour.
 */
public class ColourSelectDialog extends JDialog
{
	private final static int GRIDSIZE=16,SUBGRIDSIZE=GRIDSIZE/2,SELECTORSIZE=16;
	private Color chosen=null;

	private HueSelector[] hues;
	private HueSelector selectedHue;
	private SBSelector[] others;
	private SBSelector selectedOther;

	private ColourPreview colourPreview;

	private class HueSelector extends ColourSelector
	{
		float hue;

		HueSelector(float hue)
		{
			this.hue=hue;
		}

		@Override
		void clicked()
		{
			HueSelector oldSelectedHue=selectedHue;
			selectedHue=this;
			oldSelectedHue.repaint();
			repaint();

			for(int i=0;i<others.length;i++)
			{
				others[i].repaint();
			}

			colourPreview.repaint();
		}

		@Override
		Color getColour()
		{
			return Color.getHSBColor(hue,1.0f,1.0f);
		}

		@Override
		boolean isSelected()
		{
			return selectedHue==this;
		}
	}

	private class SBSelector extends ColourSelector
	{
		float saturation,brightness;

		SBSelector(float saturation,float brightness)
		{
			this.saturation=saturation;
			this.brightness=brightness;
		}

		@Override
		void clicked()
		{
			SBSelector oldSelectedOther=selectedOther;
			selectedOther=this;
			oldSelectedOther.repaint();
			repaint();
			colourPreview.repaint();
		}

		@Override
		Color getColour()
		{
			return Color.getHSBColor(selectedHue.hue,saturation,brightness);
		}

		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(SELECTORSIZE*2+4,SELECTORSIZE*2+4);
		}

		@Override
		boolean isSelected()
		{
			return selectedOther==this;
		}
	}

	private abstract class ColourSelector extends JComponent
	{
		public ColourSelector()
		{
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					clicked();
				}
			});
			addKeyListener(new KeyAdapter()
			{
				@Override
				public void keyTyped(KeyEvent e)
				{
					if(e.getKeyChar()==' ')
						clicked();
				}
			});
			addFocusListener(new FocusListener()
			{
				@Override
				public void focusGained(FocusEvent e)
				{
					repaint();
				}
				@Override
				public void focusLost(FocusEvent e)
				{
					repaint();
				}
			});
		}
		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(SELECTORSIZE,SELECTORSIZE);
		}

		@Override
		public boolean isFocusable()
		{
			return true;
		}

		abstract Color getColour();
		abstract void clicked();
		abstract boolean isSelected();
		@Override
		public boolean isOpaque()
		{
			return true;
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			g.setColor(getColour());
			g.fillRect(1,1,getWidth()-2,getHeight()-2);
			if(isSelected())
			{
				g.setColor(Color.black);
				g.drawRect(0,0,getWidth()-1,getHeight()-1);
				g.drawRect(2,2,getWidth()-5,getHeight()-5);
				g.setColor(Color.white);
				g.drawRect(1,1,getWidth()-3,getHeight()-3);
			}
			else
			{
				g.setColor(Color.black);
				g.drawRect(0,0,getWidth()-1,getHeight()-1);
			}
			if(hasFocus())
			{
				g.setColor(Color.white);
				g.drawRect(0,0,getWidth()-1,getHeight()-1);
			}
		}
	}

	private class ColourPreview extends JComponent
	{
		@Override
		protected void paintComponent(Graphics g)
		{
			Color c=Color.getHSBColor(
				selectedHue.hue,selectedOther.saturation,selectedOther.brightness);

			int divider=getHeight()/3;

			g.setColor(c);
			g.fillRect(0,0,getWidth(),divider);

			g.setColor(Color.white);
			g.fillRect(0,divider,getWidth(),divider);
			g.setColor(c);
			Shape previousClip=g.getClip();
			g.clipRect(0,divider,getWidth(),divider);
			int lineHeight=g.getFontMetrics().getHeight();
			int lines=divider/lineHeight + 1;
			for(int i=0;i<lines;i++)
			{
				g.drawString("TextTextTextTextTextTextTextTextText",-8*i,divider+(i+1)*lineHeight-5);
			}
			g.setClip(previousClip);

			g.setColor(c);
			g.fillRect(0,2*divider,getWidth(),getHeight()-2*divider);
			g.setColor(Color.black);
			previousClip=g.getClip();
			g.clipRect(0,2*divider,getWidth(),getHeight()-2*divider);
			for(int i=0;i<lines;i++)
			{
				g.drawString("BackgroundBackgroundBackgroundBackground",-8*i,2*divider+(i+1)*lineHeight-5);
			}
			g.setClip(previousClip);
		}

		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(96,96);
		}

		@Override
		public boolean isOpaque()
		{
			return true;
		}
	}


	ColourSelectDialog(Component parent,String title,Color original)
	{
		super((Frame)null,title,true);

	  JPanel main=new JPanel(new BorderLayout(8,8));
	  main.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
		getContentPane().add(main);
		JPanel selectors=new JPanel(new BorderLayout(8,8));
		main.add(selectors,BorderLayout.CENTER);
		JPanel hueControls=new JPanel(new BorderLayout(4,4));
		selectors.add(hueControls,BorderLayout.NORTH);
		JPanel otherControls=new JPanel(new BorderLayout(4,4));
		selectors.add(otherControls,BorderLayout.SOUTH);
		JLabel hueLabel=new JLabel("Hue");
		hueControls.add(hueLabel,BorderLayout.NORTH);
		JLabel otherLabel=new JLabel("Saturation and brightness");
		otherControls.add(otherLabel,BorderLayout.NORTH);
		JPanel hueSelectors=new JPanel(new GridLayout(1,GRIDSIZE,4,4));
		hueControls.add(hueSelectors,BorderLayout.SOUTH);
		JPanel otherSelectors=new JPanel(new GridLayout(SUBGRIDSIZE,SUBGRIDSIZE,4,4));
		otherControls.add(otherSelectors,BorderLayout.SOUTH);

		JPanel buttons1=new JPanel(new BorderLayout(8,8));
		main.add(buttons1,BorderLayout.SOUTH);
		JPanel buttons2=new JPanel(new BorderLayout());
		buttons1.add(buttons2,BorderLayout.CENTER);

		JButton select=new JButton("Select");
		select.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				chosen=Color.getHSBColor(
					selectedHue.hue,selectedOther.saturation,selectedOther.brightness);
				dispose();
			}
		});
		JButton cancel=new JButton("Cancel");
		cancel.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				chosen=null;
				dispose();
			}
		});

		getRootPane().setDefaultButton(select);

		if(PlatformUtils.isWindows())
		{
			buttons2.add(select,BorderLayout.EAST);
			buttons1.add(cancel,BorderLayout.EAST);
		}
		else
		{
			buttons1.add(select,BorderLayout.EAST);
			buttons2.add(cancel,BorderLayout.EAST);
		}

		colourPreview=new ColourPreview();
		main.add(colourPreview,BorderLayout.EAST);

		hues=new HueSelector[GRIDSIZE];
		for(int hue=0;hue<GRIDSIZE;hue++)
		{
			float hueValue=((float)hue/(float)GRIDSIZE);
			hues[hue]=new HueSelector(hueValue);
			hueSelectors.add(hues[hue]);
		}
		others=new SBSelector[SUBGRIDSIZE*SUBGRIDSIZE];
		for(int saturation=0;saturation<SUBGRIDSIZE;saturation++)
		{
			for(int brightness=0;brightness<SUBGRIDSIZE;brightness++)
			{
				int index=saturation*SUBGRIDSIZE+brightness;
				others[index]=new SBSelector(
					((float)saturation/(float)(SUBGRIDSIZE-1)),((float)brightness/(float)(SUBGRIDSIZE-1)));
				otherSelectors.add(others[index]);
			}
		}

		float[] hsbSpecified=Color.RGBtoHSB(original.getRed(),original.getGreen(),original.getBlue(),null);
    selectedHue=hues[Math.min(GRIDSIZE-1,Math.round(hsbSpecified[0]*GRIDSIZE))];
    selectedOther=others[
      Math.min(SUBGRIDSIZE-1,Math.round(hsbSpecified[1]*(SUBGRIDSIZE-1)))*SUBGRIDSIZE+
      Math.min(SUBGRIDSIZE-1,Math.round(hsbSpecified[2]*(SUBGRIDSIZE-1)))];

		pack();
		setResizable(false);
		setLocationRelativeTo(parent);
		setVisible(true);
	}

	Color getChosenColour()
	{
		return chosen;
	}

}
