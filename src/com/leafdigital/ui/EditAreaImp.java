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
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.IOException;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.BadLocationException;

import util.PlatformUtils;

import com.leafdigital.ui.api.*;

import leafchat.core.api.BugException;

/** Provides a basic edit box */
public class EditAreaImp extends JComponent implements FocusListener,BaseGroup
{
	private JScrollPane sp;
	private MyTextArea ta=new MyTextArea();

	private int defaultWidth=200,defaultHeight=100;
	private boolean autoStretch = false;
	private int autoHeight = -1;
	private int lastPreferredHeight = -1;

	private String baseGroup=null;
	private int topOffset=0;

	EditAreaInterface externalInterface=new EditAreaInterface();
	private String onChange=null,onFocus=null;
	private boolean inSet=false;

	@Override
	public void setBounds(int x,int y,int width,int height)
	{
		super.setBounds(x,y,width,height);
		relayout();
	}

	private void relayout()
	{
		sp.setBounds(0,topOffset,getWidth(),getHeight()-topOffset);
	}

	/**
	 * @param owner UI object
	 */
	public EditAreaImp(UISingleton owner)
	{
		setLayout(null);
		sp=new JScrollPane(ta,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		add(sp);
		ta.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent arg0)
			{
				changed();
			}

			@Override
			public void removeUpdate(DocumentEvent arg0)
			{
				changed();
			}

			@Override
			public void changedUpdate(DocumentEvent arg0)
			{
				changed();
			}
		});
		ta.addFocusListener(this);
	}

	EditArea getInterface() { return externalInterface; }

	class EditAreaInterface extends BasicWidget implements EditArea,InternalWidget
	{
		@Override
		public int getContentType() { return CONTENT_NONE; }

		@Override
		public void setOnChange(String sChange)
		{
			getInterface().getOwner().getCallbackHandler().check(sChange);
			EditAreaImp.this.onChange=sChange;
		}

		@Override
		public void setOnFocus(String sFocus)
		{
			getInterface().getOwner().getCallbackHandler().check(sFocus);
			EditAreaImp.this.onFocus=sFocus;
		}

		@Override
		public JComponent getJComponent()
		{
			return EditAreaImp.this;
		}

		@Override
		public int getPreferredWidth()
		{
			return defaultWidth;
		}

		@Override
		public int getPreferredHeight(int iWidth)
		{
			if(autoStretch)
			{
				return Math.max(defaultHeight, autoHeight) + topOffset;
			}
			else
			{
				return defaultHeight+topOffset;
			}
		}

		@Override
		public void addXMLChild(String sSlotName, Widget wChild)
		{
			throw new BugException("Edit boxes cannot contain children");
		}

		@Override
		public String getValue()
		{
			return ta.getText();
		}
		@Override
		public void setValue(String s)
		{
			try
			{
				inSet=true;
				ta.setText(s);
				updateAutoHeight();
			}
			finally
			{
				inSet=false;
			}
		}

		@Override
		public void setEnabled(boolean bEnabled)
		{
			ta.setEnabled(bEnabled);
		}

		@Override
		public boolean isEnabled()
		{
			return ta.isEnabled();
		}

		@Override
		public void focus()
		{
			getUI().focus(ta);
	  }

		@Override
		public void setWidth(int width)
		{
			defaultWidth=width;
		}

		@Override
		public void setHeight(int height)
		{
			defaultHeight=height;
		}

		@Override
		public void setBaseGroup(String group)
		{
			if(baseGroup!=null)
			{
				BaseGroup.Updater.removeFromGroup(EditAreaImp.this,baseGroup);
				baseGroup=null;
			}
			if(group!=null)
			{
				baseGroup=group;
				BaseGroup.Updater.addToGroup(EditAreaImp.this,baseGroup);
			}
		}

		@Override
		public void selectAll()
		{
			ta.selectAll();
		}

		@Override
		public void highlightErrorLines(int[] lines)
		{
			errorLines=lines;
			ta.repaint();
		}

		@Override
		public void setAutoStretch(final boolean autoStretch)
		{
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					if(autoStretch != EditAreaImp.this.autoStretch)
					{
						EditAreaImp.this.autoStretch = autoStretch;
						sp.setVerticalScrollBarPolicy(
							autoStretch ? JScrollPane.VERTICAL_SCROLLBAR_NEVER
								: JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
					}
				}
			});
		}
	}

	private void changed()
	{
		updateAutoHeight();

		if(onChange!=null && !inSet)
		{
			getInterface().getOwner().getCallbackHandler().callHandleErrors(onChange);
		}
		if(errorLines!=null)
		{
			errorLines=null;
			repaint();
		}
	}

	/**
	 * If autostretch mode is on, updates the current height.
	 */
	private void updateAutoHeight()
	{
		if(autoStretch)
		{
			int currentPreferredHeight = ta.getLineCount() *
				ta.getFontMetrics(ta.getFont()).getHeight();
			if(lastPreferredHeight == -1 || lastPreferredHeight != currentPreferredHeight)
			{
				lastPreferredHeight = currentPreferredHeight;

				// Work out difference between scrollpane height and textarea height
				int margin = sp.getInsets().top + sp.getViewport().getInsets().top +
					sp.getInsets().bottom + sp.getViewport().getInsets().bottom +
					sp.getHorizontalScrollBar().getPreferredSize().height;
				autoHeight = currentPreferredHeight + margin;

				externalInterface.redoLayout();
			}
		}
	}

	@Override
	public void focusGained(FocusEvent arg0)
	{
		if(onFocus!=null)
		{
			getInterface().getOwner().getCallbackHandler().callHandleErrors(onFocus);
		}
	}

	@Override
	public void focusLost(FocusEvent arg0)
	{
	}

	private static abstract class MyAction extends AbstractAction
	{
		MyAction(String name)
		{
			super(name);
		}
		abstract void update();
	}

	private static class CutAction extends MyAction
	{
		private JTextArea field;
		CutAction(JTextArea field)
		{
			super("Cut");
			this.field=field;
			putValue(MNEMONIC_KEY, KeyEvent.VK_T);
			putValue(ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_X,
				PlatformUtils.isMac()? InputEvent.META_MASK : InputEvent.CTRL_MASK));
		}

		@Override
		void update()
		{
			setEnabled(field.getSelectedText()!=null);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			field.cut();
		}
	}

	private static class CopyAction extends MyAction
	{
		private JTextArea field;
		CopyAction(JTextArea field)
		{
			super("Copy");
			this.field=field;
			putValue(MNEMONIC_KEY, KeyEvent.VK_C);
			putValue(ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_C,
				PlatformUtils.isMac()? InputEvent.META_MASK : InputEvent.CTRL_MASK));
		}

		@Override
		void update()
		{
			setEnabled(field.getSelectedText()!=null);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			field.copy();
		}
	}

	private static class PasteAction extends MyAction
	{
		private JTextArea field;
		PasteAction(JTextArea field)
		{
			super("Paste");
			this.field=field;
			putValue(MNEMONIC_KEY, KeyEvent.VK_P);
			putValue(ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_V,
				PlatformUtils.isMac()? InputEvent.META_MASK : InputEvent.CTRL_MASK));
		}

		@Override
		void update()
		{
			Transferable t=Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
			if(t==null)
				setEnabled(false);
			else
			 setEnabled(t.isDataFlavorSupported(DataFlavor.stringFlavor));
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			field.paste();
		}
	}

	private int[] errorLines;

	private class MyTextArea extends JTextArea
	{
		private MyAction cut,copy,paste;
		private JPopupMenu pm;
		MyTextArea()
		{
			// Setup popup menu
			pm=new JPopupMenu();
			cut=new CutAction(this);
			copy=new CopyAction(this);
			paste=new PasteAction(this);
			pm.add(cut);
			pm.add(copy);
			pm.add(paste);
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					if(e.isPopupTrigger())
					{
						cut.update(); copy.update(); paste.update();
						pm.show(e.getComponent(),e.getX(),e.getY());
					}
				}
			  @Override
				public void mouseReleased(MouseEvent e)
			  {
			  		mousePressed(e);
			  }
			});
			setBackground(new Color(0,0,0,0));
		}

		@Override
		public void paste()
		{
			// Get clipboard contents
			Transferable t=Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
			if(t==null) return;

			String text;
			try
			{
				text=(String)t.getTransferData(DataFlavor.stringFlavor);
			}
			catch(UnsupportedFlavorException e)
			{
				e.printStackTrace();
				return;
			}
			catch(IOException e)
			{
				e.printStackTrace();
				return;
			}
			// Standardise newlines
			text=text.replaceAll("\\r\\n","\n");
			text=text.replaceAll("\\r","\n");
			// Get rid of newlines at start and end
			while(text.startsWith("\n")) text=text.substring(1);
			while(text.endsWith("\n")) text=text.substring(0,text.length()-1);
			// Go ahead and paste in
			replaceSelection(text);
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			g.setColor(SystemColor.text);
			g.fillRect(0,0,getWidth(),getHeight());

			if(errorLines!=null)
			{
				g.setColor(Color.yellow);
				for(int i=0;i<errorLines.length;i++)
				{
					int line=errorLines[i];
					if(line>=getLineCount()) continue;
					try
					{
						int pos=getLineStartOffset(line);
						Rectangle r=modelToView(pos);
						g.fillRect(0,r.y,getWidth(),r.height);
					}
					catch(BadLocationException e)
					{
					}
				}
			}

			super.paintComponent(g);
		}
	}

	@Override
	public int getBaseline()
	{
		try
		{
			// TODO This probably does not work
			return ta.modelToView(0).y;
		}
		catch(BadLocationException e)
		{
			return 0;
		}
	}

	@Override
	public InternalWidgetOwner getInternalWidgetOwner()
	{
		return (InternalWidgetOwner)externalInterface.getOwner();
	}

	@Override
	public void setTopOffset(int topOffset)
	{
		if(this.topOffset==topOffset) return;
		this.topOffset=topOffset;
		relayout();
	}

	// Debugging
	@Override
	public void paintChildren(Graphics g)
	{
		super.paintChildren(g);
		BaseGroup.Debug.paint(g,this,topOffset);
	}
}
