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
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import util.PlatformUtils;

import com.leafdigital.prefs.api.*;
import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/** Provides a basic edit box */
public class EditBoxImp extends JComponent implements ActionListener,FocusListener,BaseGroup,ThemeListener
{
	private MyTextField tf=new MyTextField();

	private final static int MAXHISTORY=64;
	private int defaultWidth=200;

	private String baseGroup=null;
	private int topOffset=0;

	private int lineMax=-1;
	private boolean lineWrap=false;
	private int[] lineBreaks=null;

	private boolean useFontSettings;

	EditBoxInterface ebi=new EditBoxInterface();
	private String onEnter=null,onChange=null,onFocus=null,onMultiLine=null;
	private TextViewImp tvi=null;
	private boolean inSet=false;

	private String require=null;

	private static Color defaultFG=null,defaultBG=null;

	/** Preference group for remembering position, if used */
	private PreferencesGroup prefsGroup = null;

	// TODO Is there a better way to get this colour? Shared TableImp, EditBoxImp
	private static Color errorBG=new Color(255,200,200);

	LinkedList<String> history = new LinkedList<String>();
	int historyPos=-1;

	private EditBox.TabCompletion tc=null;

	private class MyDocument extends PlainDocument
	{
		@Override
		public void insertString(int iPos,String s,AttributeSet as) throws BadLocationException
		{
			try
			{
				// Slightly complicated logic to calculate length in bytes (fun!)
				int iExistingBytes=getText(0,getLength()).getBytes("UTF-8").length;
				int iNewBytes=s.getBytes("UTF-8").length;
				while(lineMax!=EditBox.LINEBYTES_NOLIMIT &&
					iExistingBytes+iNewBytes > lineMax && !lineWrap)
				{
					s=s.substring(0,s.length()-1);
					iNewBytes=s.getBytes("UTF-8").length;
				}
			}
			catch(UnsupportedEncodingException e)
			{
				assert false;
			}
			super.insertString(iPos,s,as);
		}
	}

	@Override
	public void setBounds(int x,int y,int width,int height)
	{
		super.setBounds(x,y,width,height);
		relayout();
	}

	private void relayout()
	{
		int preferredHeight=tf.getPreferredSize().height;
		tf.setBounds(0,topOffset,getWidth(),preferredHeight);
	}

	/**
	 * @param owner Owner singleton
	 */
	public EditBoxImp(UISingleton owner)
	{
		setLayout(null);
		add(tf);
		owner.informThemeListener(this);
		if(defaultFG==null)
		{
			defaultFG=tf.getForeground();
			defaultBG=tf.getBackground();
		}
		tf.setDocument(new MyDocument());
		tf.addActionListener(this);
		tf.getDocument().addDocumentListener(new DocumentListener()
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
		tf.addFocusListener(this);
		tf.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP,0),"pageup");
		tf.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN,0),"pagedown");
		tf.getActionMap().put("pageup",new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				if(tvi!=null) tvi.pageUp();
			}
		});
		tf.getActionMap().put("pagedown",new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				if(tvi!=null) tvi.pageDown();
			}
		});
		tf.addCaretListener(new CaretListener()
		{
			@Override
			public void caretUpdate(CaretEvent e)
			{
				forgetTab();
			}
		});
		tf.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP,0),"historyup");
		tf.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,0),"historydown");
		tf.getActionMap().put("historyup",new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				if(historyPos>0)
				{
					historyPos--;
					tf.setText(history.get(historyPos));
				}
				else if(historyPos==-1 && history.size()>0)
				{
					historyPos=history.size()-1;
					history.addLast(tf.getText()); // Store current text...
					tf.setText(history.get(historyPos));
				}
				// Else do nothing, no history or at the top
			}
		});
		tf.getActionMap().put("historydown",new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				if(historyPos>=0 && historyPos < history.size()-1)
				{
					historyPos++;
					tf.setText(history.get(historyPos));

					if(historyPos==history.size()-1) // Back to 'current' text
					{
						historyPos=-1;
						history.removeLast();
					}
				}
			}
		});
		tf.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,0),"tabComplete");
		tf.getActionMap().put("tabComplete",new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				tabComplete();
			}
		});

		tf.getActionMap().put("wipeLine",new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if(!tf.getText().equals(""))
				{
					wiped=tf.getText();
					tf.setText("");
					if(wipeInfoOpacity==0)
					{
						wipeInfoOpacity=255;
						(new Thread(new Runnable() {
							@Override
							public void run()
							{
								while(true)
								{
									tf.repaint();
									if(wipeInfoOpacity==0) break;
									wipeInfoOpacity-=16;
									if(wipeInfoOpacity<0) wipeInfoOpacity=0;
									try
									{
										Thread.sleep(50);
									}
									catch(InterruptedException ie)
									{
									}
								}
							}
						})).start();
					}
				}
			}
		});
		tf.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),"wipeLine");
		tf.getActionMap().put("unWipeLine",new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				tf.setText(wiped);
			}
		});
		tf.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,InputEvent.SHIFT_MASK),"unWipeLine");
	}

	/** Wiped line or null if none */
	private String wiped;

	/** Information about how to un-wipe, fade level */
	private int wipeInfoOpacity=0;

	/** Cursor as it was on first tab press */
	private int tabCursor;

	/** Partial text that we're completing */
	private String tabPartial;

	/** Options for current tab sequence */
	private String[] tabOptions;

	/** Current option in tab sequence */
	private int tabOption;

	private boolean inTabStuff=false;

	private void forgetTab()
	{
		if(!inTabStuff)
		{
			tabOptions=null;
		}
	}

	private void tabComplete()
	{
		if(tc==null) return;

		int cursor=tf.getCaretPosition();
		String text=tf.getText();

		if(tabOptions==null)
		{
			// First time. Find partial word before cursor
			tabCursor=cursor;
			tabPartial=text.substring(0,cursor).replaceAll("^.*[ ,]","");
			if(tabPartial.length()==0) return;

			// Complete word
			tabOptions=tc.complete(tabPartial,tabPartial.length()==text.length());
			if(tabOptions.length==0 || tabOptions.length>10)
			{
				tabOptions=null;
				return;
			}

			tabOption=-1;
		}

		// Restore
	  try
		{
	  		inTabStuff=true;

	  		// Get rid of previous option
	  		if(tabOption!=-1)
	  		{
	  			int trim=tabOptions[tabOption].length()-tabPartial.length();
	  			tf.getDocument().remove(tabCursor,trim);
	  		}

			// Get rid of partial string too
			tf.getDocument().remove(tabCursor-tabPartial.length(),tabPartial.length());

	  		// Incremement option
			tabOption++;
			if(tabOption>=tabOptions.length)
			{
				// On repeating cycle, go back to original partial
				tabOption=-1;
				tf.getDocument().insertString(tabCursor-tabPartial.length(),tabPartial,null);
				tf.setCaretPosition(tabCursor);
			}
			else
			{
				// Add new text and position cursor after it
				tf.getDocument().insertString(tabCursor-tabPartial.length(),tabOptions[tabOption],null);
				tf.setCaretPosition(tabCursor-tabPartial.length()+tabOptions[tabOption].length());
			}
		}
		catch(BadLocationException e)
		{
			throw new Error(e);
		}
		finally
		{
			inTabStuff=false;
		}
	}

	EditBox getInterface() { return ebi; }

	class EditBoxInterface extends BasicWidget implements EditBox,InternalWidget
	{
		@Override
		public int getContentType() { return CONTENT_NONE; }

		@Override
		public void setOnEnter(String sEnter)
		{
			getInterface().getOwner().getCallbackHandler().check(sEnter);
			EditBoxImp.this.onEnter=sEnter;
		}

		@Override
		public void setOnMultiLine(String callback)
		{
			getInterface().getOwner().getCallbackHandler().check(callback,new Class[] {String.class});
			EditBoxImp.this.onMultiLine=callback;
		}

		@Override
		public void setOnChange(String sChange)
		{
			getInterface().getOwner().getCallbackHandler().check(sChange);
			EditBoxImp.this.onChange=sChange;
		}

		@Override
		public void setOnFocus(String sFocus)
		{
			getInterface().getOwner().getCallbackHandler().check(sFocus);
			EditBoxImp.this.onFocus=sFocus;
		}

		@Override
		public JComponent getJComponent()
		{
			return EditBoxImp.this;
		}

		@Override
		public int getPreferredWidth()
		{
			return defaultWidth;
		}

		@Override
		public int getPreferredHeight(int iWidth)
		{
			return tf.getPreferredSize().height+topOffset;
		}

		@Override
		public void addXMLChild(String sSlotName, Widget wChild)
		{
			throw new BugException("Edit boxes cannot contain children");
		}

		@Override
		public String getValue()
		{
			return tf.getText();
		}
		@Override
		public String[] getValueLines()
		{
			if(lineWrap && lineBreaks!=null)
			{
				// Not sure why this is called
				updateWrap();
				// updateWrap can set lineBreaks back to null (no I don't know why it
				// would've not been called already with the same data, probably same
				// reason it's called here, maybe to do with pasting in?)
				if(lineBreaks==null)
				{
					return new String[] {getValue()};
				}
				String sValue=getValue();
				String[] as=new String[lineBreaks.length+1];
				for(int i=0;i<as.length;i++)
				{
					as[i]=sValue.substring(i==0 ? 0 : lineBreaks[i-1],
						i>=lineBreaks.length ? sValue.length() : lineBreaks[i]).trim();
				}
				return as;
			}
			else
			{
				return new String[]{getValue()};
			}
		}

		@Override
		public void setValue(String s)
		{
			try
			{
				inSet=true;
				tf.setText(s);
			}
			finally
			{
				inSet=false;
			}
		}

		@Override
		public void setEnabled(boolean bEnabled)
		{
			tf.setEnabled(bEnabled);
		}

		@Override
		public boolean isEnabled()
		{
			return tf.isEnabled();
		}

		@Override
		public void focus()
		{
			getUI().focus(tf);
	  }

		@Override
		public void setTextView(final String sID)
		{
			try
			{
				tvi=((TextViewImp.TextViewInterface)getOwner().getWidget(sID)).getImp();
				tvi.informLinked(tf);
			}
			catch(ClassCastException cce)
			{
				throw new BugException("<editbox>: TextView= points to something other than a TextView");
			}
		}

		private int flag=FLAG_NORMAL;

		@Override
		public void setFlag(int iFlag)
		{
			switch(iFlag)
			{
			case FLAG_DIM:
				tf.setForeground(new Color(defaultFG.getRed(),defaultFG.getGreen(),defaultFG.getBlue(),128));
				tf.setBackground(defaultBG);
				break;
			case FLAG_ERROR:
				tf.setForeground(defaultFG);
				if(tf.getText().length()>0) // Don't show error flag for blank fields
	  		  		tf.setBackground(errorBG);
				break;
		  default:
		  		tf.setForeground(defaultFG);
		  		tf.setBackground(defaultBG);
		  		break;
			}
			this.flag=iFlag;
		}

		@Override
		public int getFlag()
		{
			return flag;
		}

		@Override
		public void setLineBytes(int iMax)
		{
			EditBoxImp.this.lineMax=iMax;
		}
		@Override
		public void setLineWrap(boolean bAllowWrap)
		{
			if(EditBoxImp.this.lineWrap==bAllowWrap) return;
			EditBoxImp.this.lineWrap=bAllowWrap;
			updateWrap();
		}

		@Override
		public void setWidth(int width)
		{
			defaultWidth=width;
		}

		@Override
		public void setTabCompletion(TabCompletion tc)
		{
			EditBoxImp.this.tc=tc;
			if(tc==null)
			{
				setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,null);
			}
			else
			{
				// Remove tab from the set
				Set<AWTKeyStroke> s = new HashSet<AWTKeyStroke>(
					getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
				s.remove(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB,0,false));
				setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,s);
			}
		}

		@Override
		public void selectAll()
		{
			tf.selectAll();
		}

		@Override
		public void setRequire(String require)
		{
			EditBoxImp.this.require=require;
			checkRequire();
		}

		@Override
		public void setBaseGroup(String group)
		{
			if(baseGroup!=null)
			{
				BaseGroup.Updater.removeFromGroup(EditBoxImp.this,baseGroup);
				baseGroup=null;
			}
			if(group!=null)
			{
				baseGroup=group;
				BaseGroup.Updater.addToGroup(EditBoxImp.this,baseGroup);
			}
		}

		@Override
		public void setUseFontSettings(boolean useFontSettings)
		{
			EditBoxImp.this.useFontSettings=useFontSettings;
			resetFont();
		}

		@Override
		public void setRemember(String category, String memoryId)
		{
			// Get preferences group for this edit box
			PluginContext context = getUI().getPluginContext();
			Preferences p = context.getSingle(Preferences.class);
			prefsGroup = p.getGroup(context.getPlugin()).getChild(
				"command-history").getChild(category).getChild(p.getSafeToken(memoryId));

			// Set up history based on data from group
			for(int i=0; i<MAXHISTORY; i++)
			{
				String value = prefsGroup.get("l" + i, null);
				if(value == null)
				{
					break;
				}
				history.addLast(value);
			}
		}

		@Override
		public void informClosed()
		{
			// When closed, save history
			if(prefsGroup == null)
			{
				return;
			}

			// Clear existing history
			for(int i=0; i<MAXHISTORY; i++)
			{
				prefsGroup.unset("l" + i);
			}

			// Set new history
			int index = 0;
			for(String value : history)
			{
				prefsGroup.set("l" + (index++), value);
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		// There's an error I can't reproduce which seems like it's possible for
		// this to be called after the editbox has been hidden (maybe if you sent
		// two CRs very quickly, and the first one closes it - but I can't make
		// this happen)
		if(!tf.isShowing())
		{
			return;
		}

		if(onEnter!=null)
		{
			if(historyPos!=-1)
			{
				historyPos=-1;
				history.removeLast();
			}

			history.addLast(tf.getText());
			if(history.size() > MAXHISTORY) history.removeFirst();

			getInterface().getOwner().getCallbackHandler().callHandleErrors(onEnter);
		}
		else
		{
			JButton defaultButton=tf.getRootPane().getDefaultButton();
			if(defaultButton!=null) defaultButton.doClick(100);
		}
	}

	private void updateWrap()
	{
		// If text is longer than the line limit...
		try
		{
			String text=tf.getText();
			int totalBytes=text.getBytes("UTF-8").length;
			if(lineWrap && lineMax!=EditBox.LINEBYTES_NOLIMIT &&
				totalBytes > lineMax)
			{
				List<Integer> breaks = new LinkedList<Integer>();
				int bytes=0,lastWhitespace=-1;
				for(int pos=0;pos<text.length();pos++)
				{
					char thisChar=text.charAt(pos);
					if(Character.isWhitespace(thisChar)) lastWhitespace=pos;
					int thisBytes=(""+thisChar).getBytes("UTF-8").length;
					bytes+=thisBytes;
					if(bytes > lineMax)
					{
						// OK, break at last whitespace if there was one
						if(lastWhitespace==-1) lastWhitespace=pos;
						breaks.add(lastWhitespace);
						pos=lastWhitespace; // We will actually start counting from just after
																// because of the for loop, but that's correct
																// since the whitespace will be trimmed anyhow
						lastWhitespace=-1;
						bytes=0;
					}
				}

				/* Old code non-multibyte
				int iPos=0;
				List lBreaks=new LinkedList();
				StringBuffer sb=new StringBuffer(getText());
				while(sb.length() > iLineMax)
				{
					int iBreak=iLineMax;
					for(;iBreak>0;iBreak--)
					{
						if(Character.isWhitespace(sb.charAt(iBreak))) break;
					}
					if(iBreak==0) iBreak=iLineMax;
					lBreaks.add(niBreak+iPos);
					iPos+=iBreak;
					sb.delete(0,iBreak);
				}
				*/
				int[] newBreaks=new int[breaks.size()];
				int index=0;
				for(Integer i : breaks)
				{
					newBreaks[index++] = i;
				}
				if(!Arrays.equals(newBreaks,lineBreaks))
				{
					lineBreaks=newBreaks;
					repaint();
				}
			}
			else if(lineBreaks!=null)
			{
				lineBreaks=null;
				repaint();
			}
		}
		catch(UnsupportedEncodingException e)
		{
			throw new BugException(e);
		}
	}

	private void checkRequire()
	{
		if(require!=null)
		{
			ebi.setFlag(tf.getText().matches(require) ? EditBox.FLAG_NORMAL : EditBox.FLAG_ERROR);
		}
	}

	private void changed()
	{
		updateWrap();
		forgetTab();
		checkRequire();
		if(onChange!=null && !inSet)
		{
			getInterface().getOwner().getCallbackHandler().callHandleErrors(onChange);
		}
	}

	@Override
	public void focusGained(FocusEvent arg0)
	{
		if(onFocus!=null)
		{
			// Defensive programming: it appears possible that sometimes this gets
			// called before the owning dialog is fully created. To account for that,
			// add an invokeLater. See issue #7.
			final WidgetOwner widgetOwner = getInterface().getOwner();
			if(widgetOwner.isCreated())
			{
				widgetOwner.getCallbackHandler().callHandleErrors(onFocus);
			}
			else
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						if(widgetOwner.isCreated())
						{
							widgetOwner.getCallbackHandler().callHandleErrors(onFocus);
						}
						else
						{
							ErrorMsg.report("Focus in uncreated container", null);
						}
					}
				});
			}
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
		private JTextField field;
		CutAction(JTextField field)
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
		private JTextField field;
		CopyAction(JTextField field)
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
		private JTextField field;
		PasteAction(JTextField field)
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

	private class MyTextField extends JTextField
	{
		private MyAction cut,copy,paste;
		private JPopupMenu pm;
		MyTextField()
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
		}

		@Override
		public void copy()
		{
			if(
				(tf.getSelectedText()==null || tf.getSelectedText().length()==0)
				&& tvi!=null)
			{
				tvi.copy();
				tvi.clearHighlight();
			}
			else
			{
				super.copy();
			}
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
			// If there are no newlines, go ahead and paste in
			if(text.indexOf('\n')==-1)
				replaceSelection(text);
			else if(onMultiLine!=null) // Multiple lines. Do we support that here?
			{
				// Can't actually put \n into field and have it keep it, so here's
				// a workaround...
				String newLine="--7275lkrbdyh98534--";
				replaceSelection(text.replaceAll("\\n",newLine));
				String multiLine=getText().replaceAll(newLine,"\n");
				setText("");
				getInterface().getOwner().getCallbackHandler().callHandleErrors(onMultiLine,new Object[] {multiLine});
			}
			else // Just do first line
				replaceSelection(text.substring(0,text.indexOf('\n')));
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			// Default paint
			super.paintComponent(g);
			// Line breaks
			if(lineBreaks!=null)
			{
				Color
				  fg=getForeground(),
				  fg2=new Color(fg.getRed(),fg.getGreen(),fg.getBlue(),166),
				  fg3=new Color(fg.getRed(),fg.getGreen(),fg.getBlue(),76);
				for(int iBreak=0;iBreak<lineBreaks.length;iBreak++)
				{
					try
					{
						int iCharacterX=modelToView(lineBreaks[iBreak]).x;
						g.setColor(fg);
						g.fillRect(iCharacterX,0,1,getHeight());
						g.setColor(fg2);
						g.fillRect(iCharacterX+1,0,1,getHeight());
						g.setColor(fg3);
						g.fillRect(iCharacterX+2,0,1,getHeight());
					}
					catch(BadLocationException ble)
					{
						assert false;
					}
				}
			}
			// Wipe information, a helper string that appears when you press Esc,
			// displayed in small font in centre of box.
			if(wipeInfoOpacity>0)
			{
				g.setColor(new Color(255,0,0,wipeInfoOpacity));
				Font currentFont=tf.getFont();
				Font smallFont=currentFont.deriveFont(currentFont.getSize2D()*0.8f);
				g.setFont(smallFont);
				String message="[Cleared. Shift+Escape to restore]";
				Rectangle2D r=smallFont.getStringBounds(
					message,((Graphics2D)g).getFontRenderContext());
				// Typically, y is -9.7 and h is 11.8.
				g.drawString(message,
					(getWidth()-(int)(r.getWidth()+0.5))/2,
					(getHeight()-	(int)(r.getHeight()+0.5))/2-(int)(r.getY()+0.5));
			}
		}
	}

	@Override
	public int getBaseline()
	{
		FontMetrics fm=tf.getFontMetrics(tf.getFont());
		int border=(tf.getPreferredSize().height-fm.getHeight())/2;
		return border+fm.getAscent();
	}

	@Override
	public InternalWidgetOwner getInternalWidgetOwner()
	{
		return (InternalWidgetOwner)ebi.getOwner();
	}

	@Override
	public void setTopOffset(int topOffset)
	{
		if(this.topOffset==topOffset) return;
		this.topOffset=topOffset;
		relayout();
	}

	private final static Font defaultTextFieldFont=(new JTextField()).getFont();

	/** Update font based on current settings */
	private void resetFont()
	{
		Font f=null;
		if(useFontSettings)
		{
			f=ebi.getUI().getFont();
		}
		if(f==null)
			f=defaultTextFieldFont;
		if(!tf.getFont().equals(f))
		{
			tf.setFont(f);
			ebi.redoLayout();
		}
	}

	@Override
	public void updateTheme(Theme t)
	{
		resetFont();
	}

	// Debugging
	@Override
	public void paintChildren(Graphics g)
	{
		super.paintChildren(g);
		BaseGroup.Debug.paint(g,this,topOffset);
	}
}
