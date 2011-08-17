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
package com.leafdigital.ircui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import util.*;

import com.leafdigital.irc.api.*;
import com.leafdigital.ui.api.*;
import com.leafdigital.ui.api.Button;
import com.leafdigital.ui.api.Dialog;
import com.leafdigital.ui.api.Label;
import com.leafdigital.ui.api.Window;

import leafchat.core.api.*;

/**
 * Implements the mode display/change area.
 */
@UIHandler("modeparam")
public class ModeDisplay extends JComponent implements SizeInfo
{
	private String chan;
	private Server s;
	private PluginContext context;
	private Window parent;

	private JButton add;

	private Font smallFont;

	private final static int HGAP=4,VGAP=4;

	private Map<String, JButton> letterModes =
		new HashMap<String, JButton>();

	void changeServer(Server s)
	{
		this.s=s;
	}

	/**
	 *
	 * @param pc Context
	 * @param server Server
	 * @param parent Parent window
	 * @param chan Channel
	 */
	public ModeDisplay(PluginContext pc,Server server,Window parent,String chan)
	{
		this.context=pc;
		this.s=server;
		this.chan=chan;
		this.parent=parent;

		setLayout(null);

		JLabel label=new JLabel("Mode");
		label.setBorder(BorderFactory.createEmptyBorder(7,0,0,2));
		add(label);

		add=new FancyButton("+");
		add.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				JPopupMenu pm=new JPopupMenu("Add mode");
				String modeString=s.getChanModes();
				String[] modes=new String[modeString.length()];
				for(int i=0;i<modes.length;i++)
				{
					modes[i]=modeString.substring(i,i+1);
				}
				Arrays.sort(modes,new Comparator<String>()
				{
					@Override
					public int compare(String s1, String s2)
					{
						int compare=s1.compareToIgnoreCase(s2);
						if(compare==0)
							return Character.isUpperCase(s1.charAt(0)) ? 1 : -1;
						else
							return compare;
					}
				});
				for(int i=0;i<modes.length;i++)
				{
					final char mode=modes[i].charAt(0);
					if(letterModes.containsKey(mode+"")) continue;

					switch(s.getChanModeType(mode))
					{
					case Server.CHANMODE_ALWAYSPARAM:
					case Server.CHANMODE_SETPARAM:
						pm.add(new AbstractAction()
						{
							@Override
							public Object getValue(String key)
							{
								if(key==NAME)
									return "+"+mode+getMenuDescription(mode)+"...";
								else if(key==MNEMONIC_KEY && Character.isLowerCase(mode))
									return new Integer(mode);
								return super.getValue(key);
							}

							@Override
							public void actionPerformed(ActionEvent e)
							{
								showSetDialog(mode,null);
							}
						});
						break;

					case Server.CHANMODE_NOPARAM:
						pm.add(new AbstractAction()
						{
							@Override
							public Object getValue(String key)
							{
								if(key==NAME)
									return "+"+mode+getMenuDescription(mode);
								else if(key==MNEMONIC_KEY && Character.isLowerCase(mode))
									return new Integer(mode);
								return super.getValue(key);
							}

							@Override
							public void actionPerformed(ActionEvent e)
							{
								s.sendLine(IRCMsg.constructBytes("MODE "+ModeDisplay.this.chan+" +"+mode));
							}
						});
						break;
					}
				}
				pm.show(ModeDisplay.this,add.getX(),add.getY()+add.getHeight());
			}
		});

		setBorder(BorderFactory.createEmptyBorder(0,0,2,0));

		smallFont=label.getFont();
		smallFont=smallFont.deriveFont(smallFont.getSize()*0.85f);
		label.setFont(smallFont);
		add.setFont(smallFont);

		relayout();
	}

	/**
	 * @param mode Mode letter
	 * @return True if the mode with that letter exists
	 */
	boolean hasMode(char mode)
	{
		return letterModes.containsKey(mode+"");
	}

	/**
	 * @param mode Mode letter
	 * @return Value of mode or null if not set
	 */
	String getModeValue(char mode)
	{
		LetterButton button = (LetterButton)letterModes.get(mode + "");
		if(button == null)
		{
			return null;
		}
		return button.getParam();
	}

	private static String getDescription(char mode)
	{
		switch(mode)
		{
		case 'm' : return "moderated";
		case 'p' : return "private";
		case 's' : return "secret";
		case 'i' : return "invite-only";
		case 't' : return "ops control topic";
		case 'n' : return "no messages from outside";
		case 'l' : return "user limit";
		case 'k' : return "channel key";
		default: return null;
		}
	}

	private static String getMenuDescription(char mode)
	{
		String description=getDescription(mode);
		if(description==null)
			return "";
		else
			return " ("+description+")";
	}

	private void relayout()
	{
		Insets insets=getInsets();
		int x=0,y=0,rowY=0,width=getWidth()-insets.left-insets.right;
		for(int i=0;i<getComponentCount();i++)
		{
			if(i>0) x+=HGAP;
			Dimension size=getComponent(i).getPreferredSize();
			if(x+size.width>width) // Next row
			{
				if(y!=0) y+=VGAP;
				y+=rowY;
				x=0;
				getComponent(i).setBounds(x+insets.left,y+insets.top,size.width,size.height);
				x=size.width;
				rowY=size.height;
			}
			else
			{
				getComponent(i).setBounds(x+insets.left,y+insets.top,size.width,size.height);
				x+=size.width;
				rowY=Math.max(rowY,size.height);
			}
		}
	}

	@Override
	public void setBounds(int x,int y,int width,int height)
	{
		super.setBounds(x,y,width,height);
		relayout();
	}

	/**
	 * Adds a mode.
	 * @param letter Mode letter
	 * @param param Parameter
	 */
	public void addMode(char letter,String param)
	{
		if(letterModes.containsKey(letter+"")) removeMode(letter);

		JButton letterButton=new LetterButton(letter,param);
		letterModes.put(letter+"",letterButton);

		int insertBefore=1;
		for(;insertBefore<getComponentCount()-1;insertBefore++)
		{
			LetterButton lb=(LetterButton)getComponent(insertBefore);
			if(lb.getLetter()>letter) break;
		}
		add(letterButton,insertBefore);
		relayout();
		revalidate();
	}

	/**
	 * Removes a mode.
	 * @param letter Mode letter
	 */
	public void removeMode(char letter)
	{
		JButton letterButton=letterModes.get(letter+"");
		if(letterButton!=null)
		{
			letterModes.remove(letter+"");
			remove(letterButton);
			relayout();
			revalidate();
		}
	}

	/** Clears all modes. */
	public void clearModes()
	{
		while(getComponentCount()>2)
		{
			remove(1);
		}
		letterModes.clear();
		relayout();
		revalidate();
	}

	private boolean op=false;

	/**
	 * Updates op status (whether you can change modes).
	 * @param op True if currently op
	 */
	public void updateOpStatus(boolean op)
	{
		if(this.op==op) return;
		this.op=op;
		if(op)
			add(add);
		else
			remove(add);
		relayout();
		revalidate();
	}

	private class FancyButton extends JButton
	{
		FancyButton(String label)
		{
			super(label);

			setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(5,0,0,0),
					BorderFactory.createCompoundBorder(
						BorderFactory.createLineBorder(new Color(151,151,151),1),
						BorderFactory.createEmptyBorder(1,3,2,3)
					)));
			setContentAreaFilled(false);
			setFocusPainted(false);
			setFont(smallFont);
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Insets i=getInsets();
			int x=i.left-3,y=i.top-1,w=getWidth()-(i.right-3)-x,h=getHeight()-(i.bottom-2)-y;
			int half=(h+1)/2;
			g.setColor(new Color(253,253,253));
			g.fillRect(x,y,w,half-1);
			g.setColor(new Color(245,245,245));
			g.fillRect(x,y+half,w,1);
			g.setColor(new Color(243,243,243));
			g.fillRect(x,y+half+1,w,h-(half+1));
			super.paintComponent(g);
			if(hasFocus())
			{
				GraphicsUtils.drawFocus((Graphics2D)g,x,y,w,h);
			}
		}
	}

	private class LetterButton extends FancyButton implements ActionListener
	{
		LetterButton(char letter,String param)
		{
			super(letter+(param==null ? "" : "="+param));
			addActionListener(this);
			String tooltip=getDescription(letter);
			if(tooltip!=null)
				setToolTipText(StringUtils.capitalise(tooltip));
		}

	  char getLetter()
		{
			return getText().charAt(0);
		}

	  String getParam()
	  {
  		if(getText().length()<2) return null;
  		return getText().substring(2);
	  }

		@Override
		public void actionPerformed(ActionEvent e)
		{
			if(!op) return;
			JPopupMenu pm=new JPopupMenu("Mode "+getText());
			final int type=s.getChanModeType(getLetter());

			pm.add(new AbstractAction()
			{
				@Override
				public Object getValue(String key)
				{
					if(key==NAME)
						return "Remove";
					else if(key==MNEMONIC_KEY)
						return KeyEvent.VK_R;
					return super.getValue(key);
				}

				@Override
				public void actionPerformed(ActionEvent e)
				{
					s.sendLine(IRCMsg.constructBytes("MODE "+chan+" -"+getLetter()+
						(type==Server.CHANMODE_ALWAYSPARAM ? " "+getParam() : "")));
				}

			});

			if(type==Server.CHANMODE_ALWAYSPARAM || type==Server.CHANMODE_SETPARAM)
			{
				pm.add(new AbstractAction()
				{
					@Override
					public Object getValue(String key)
					{
						if(key==NAME)
							return "Change";
						else if(key==MNEMONIC_KEY)
							return KeyEvent.VK_C;
						return super.getValue(key);
					}

					@Override
					public void actionPerformed(ActionEvent e)
					{
						showSetDialog(getLetter(),getParam());
					}

				});
			}

			pm.show(this,0,getHeight());
		}
	}

	private Dialog d=null;
	String dialogPrevious;
	char dialogLetter;

	private void showSetDialog(char letter,String previous)
	{
		dialogPrevious=previous;
		dialogLetter=letter;
		UI ui=context.getSingleton2(UI.class);
		d = ui.createDialog("modeparam", this);
		Label l=(Label)d.getWidget("modeletter");
		l.setText(letter+"=");
		EditBox eb=(EditBox)d.getWidget("value");
		if(previous!=null)
		{
			eb.setValue(previous);
			eb.setFlag(EditBox.FLAG_DIM);
		}
		d.show(parent);
	}

	/**
	 * Callback: Cancel button.
	 * @throws GeneralException
	 */
	@UIAction
	public void actionCancel() throws GeneralException
	{
		d.close();
	}

	/**
	 * Callback: Set button.
	 * @throws GeneralException
	 */
	@UIAction
	public void actionSet() throws GeneralException
	{
		Button b=(Button)d.getWidget("set");
		if(!b.isEnabled()) return;

		EditBox eb=(EditBox)d.getWidget("value");
		String value=eb.getValue();

		s.sendLine(IRCMsg.constructBytes("MODE "+chan+" +"+dialogLetter+
			" "+value));

		d.close();
	}

	/**
	 * Callback: Mode value changed.
	 * @throws GeneralException
	 */
	@UIAction
	public void changeValue() throws GeneralException
	{
		EditBox eb=(EditBox)d.getWidget("value");
		boolean error=!eb.getValue().matches("^[\\x21-\\x7f]+$");
		boolean same=eb.getValue().equals(dialogPrevious);
		eb.setFlag(error ? EditBox.FLAG_ERROR : same ? EditBox.FLAG_DIM : EditBox.FLAG_NORMAL);
		Button b=(Button)d.getWidget("set");
		b.setEnabled(!error && !same);
	}

	@Override
	public int getPreferredHeight(int width)
	{
		// Pretend to layout the components to get sizes
		Insets insets=getInsets();
		int x=0,y=0,rowY=0;
		for(int i=0;i<getComponentCount();i++)
		{
			if(i>0) x+=HGAP;
			Dimension size=getComponent(i).getPreferredSize();
			x+=size.width;
			if(x>width) // Next row
			{
				if(y!=0) y+=VGAP;
				y+=rowY;
				x=size.width;
				rowY=size.height;
			}
			else
			{
				rowY=Math.max(rowY,size.height);
			}
		}
		return y+rowY+insets.top+insets.bottom;
	}

	@Override
	public int getPreferredWidth()
	{
		// Add up all the components
		Insets insets=getInsets();
		int width=0;
		for(int i=0;i<getComponentCount();i++)
		{
			if(i>0) width+=HGAP;
			width+=getComponent(i).getPreferredSize().width;
		}
		return width+insets.left+insets.right;
	}
}
