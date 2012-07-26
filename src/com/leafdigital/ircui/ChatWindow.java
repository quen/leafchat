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

Copyright 2012 Samuel Marshall.
*/
package com.leafdigital.ircui;

import java.awt.event.MouseEvent;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;

import org.w3c.dom.*;

import util.*;
import util.xml.*;

import com.leafdigital.highlighter.api.Highlighter;
import com.leafdigital.irc.api.*;
import com.leafdigital.logs.api.Logger;
import com.leafdigital.notification.api.Notification;
import com.leafdigital.prefs.api.*;
import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/** IRC-style chat windows, generic (not necessarily attached to a server) */
public abstract class ChatWindow implements MessageDisplay,TextView.MenuHandler
{
	private Window w;

	/** Text-view (main part of window) */
	public TextView tvUI;

	/** Command box */
	public EditBox commandUI;

	private long lastMessage;

	/** Timer event ID for code that gives users a reminder about the window */
	private int noticeTimerID=-1;

	protected final static String ACTIONSYMBOL="* ",EVENTSYMBOL="- ";

	private PluginContext context;

	protected PluginContext getPluginContext() { return context; }
	protected Window getWindow() { return w; }

	/**
	 * Keeps track of whether this window has ever been active (for the reminder
	 * feature)
	 */
	private boolean everActive;

	/**
	 * Constructs a server-based chat window.
	 * @param context Plugin context
	 * @param xml Name of xml file (excluding ".xml")
	 * @param showNow If true, shows before exiting constructor
	 * @param visible If true, appears popped-up, otherwise minimised
	 * @throws GeneralException
	 */
	public ChatWindow(PluginContext context, String xml, boolean showNow,
		boolean visible) throws GeneralException
	{
		this.context=context;

		UI ui=context.getSingle(UI.class);

		// Create window
		w=ui.createWindow(xml, this);
		w.setOnActive("actionOnActive");

		// Set up text view and editbox
		if(commandUI!=null)
		{
			commandUI.setOnChange("actionChange");
			commandUI.setTabCompletion(((IRCUIPlugin)context.getPlugin()).newTabCompletion(this));
			commandUI.setUseFontSettings(true);
			commandUI.setOnMultiLine("handleMultiLine");
			actionChange();

			// On Mac, add spacer
			if(PlatformUtils.isMac())
			{
				BorderPanel bp=(BorderPanel)w.getWidget("mainpanel");

				Spacer spacer=ui.newSpacer();
				spacer.setWidth(15);
				spacer.setHeight(15);

				Widget existing=bp.get(BorderPanel.SOUTHEAST);
				if(existing==null)
				{
  				bp.set(BorderPanel.SOUTHEAST,spacer);
				}
				else
				{
					HorizontalPanel hp=ui.newHorizontalPanel();
					hp.add(existing);
					hp.add(spacer);
					bp.set(BorderPanel.SOUTHEAST,hp);
				}
			}
		}

		tvUI.setMenuHandler(this);
		tvUI.setLineLimit(2000);
		tvUI.setScrolledUpWarning(true);
		tvUI.setAction("internalaction",new TextView.ActionHandler()
		{
			@Override
			public void action(Element e, MouseEvent me) throws GeneralException
			{
				internalAction(e);
			}
		});

		// Show window
		if(showNow)
		{
			((IRCUIPlugin)context.getPlugin()).informShown(this);
			w.show(!visible);
		}

		// Give the user a reminder in current window if they take too long to
		// notice this one
		if(!visible)
		{
			noticeTimerID=TimeUtils.addTimedEvent(new Runnable()
			{
				@Override
				public void run()
				{
					noticeTimerID=-1;
					if(everActive) return;
					ChatWindow recent=	((IRCUIPlugin)getPluginContext().getPlugin()).getRecentWindow();
					if(recent!=null) recent.showInfo(
						"<small>In case you missed it: the new window <key>"+w.getTitle()+"</key> appeared a little while ago.</small>");
				}
			},60*1000,true);
		}

		// Test cases for colour coding
//		addLine("T1 \u0002Bold text\u0002 not bold");
//		addLine("T2 \u0002Bold text, unclosed");
//		addLine("T3 \u0012Reverse text\u0012 not reverse");
//		addLine("T4 \u0012Reverse text, unclosed");
//		addLine("T5 \u001fUnderline text\u001f not underline");
//		addLine("T6 \u001fUnderline text, unclosed");
//		addLine("T7 \u001fUnderline \u0002and bold \u0012and reverse \u001fnot underline \u0002not bold \u0012not reverse");
//		addLine("T8 \u001fUnderline \u0002and bold \u0012and reverse \u000fand CUT");
// 	  addLine("blablah \u00035,12to be colored text and background\u0003 blablah");
//	  addLine("blablah \u00035to be colored text\u0003 blabla");
//	  addLine("blablah \u00033to be colored text \u00035,2other colored text and also background\u0003 blabla");
//	  addLine("blabla \u00033,5to be colored text and background \u00038other colored text but SAME background\u0003 blabla");
//	  addLine("blablah \u00033,5to be colored text and background \u00038,7other colored text and other background\u0003 blabla");
//	  addLine("\u00033,4!BG!");
		lastMessage=System.currentTimeMillis();
	}

	/**
	 * Overridable. Called when someone clicks on an <internalaction>. Default
	 * does nothing.
	 * @param e Element clicked on
	 * @throws GeneralException
	 */
	protected void internalAction(Element e) throws GeneralException
	{
	}

	/**
	 * Callback: When user hits Return.
	 * @throws GeneralException
	 */
	@UIAction
	public void actionSend() throws GeneralException
	{
		String[] asLine=commandUI.getValueLines();
		Commands c=context.getSingle(Commands.class);
		if(asLine.length==1)
		{
			doCommand(c,asLine[0]);
		}
		else
		{
			for(int i=0;i<asLine.length;i++)
				doCommand(c,"/say "+asLine[i]);
		}
		commandUI.setValue("");
	}

	/**
	 * Callback: When text in editbox changes.
	 * @throws GeneralException
	 */
	public void actionChange() throws GeneralException
	{
		if(commandUI.getValue().startsWith("/"))
		{
			commandUI.setLineBytes(450); // Leave some overhead in case the command needs a lot more chars than its / format
			commandUI.setLineWrap(false);
		}
		else
		{
			commandUI.setLineBytes(getAvailableBytes());
			commandUI.setLineWrap(true);
		}
	}

	protected abstract int getAvailableBytes() throws GeneralException;

	protected abstract void doCommand(Commands c,String line) throws GeneralException;

	/**
	 * Callback: When window becomes active.
	 * @throws GeneralException
	 */
	public void actionOnActive() throws GeneralException
	{
		everActive=true;
		if(commandUI!=null) commandUI.focus();
		((IRCUIPlugin)context.getPlugin()).informActive(this);
		fadeMark();
	}

	/** If appropriate, begins to fade down any mark that might be present */
	private void fadeMark()
	{
		if(w.getCanClearAttention() && w.isActive() && tvUI.hasMark() && markFadeTimerID==-1)
		{
			addMarkFadeTimer(256);
		}
	}

	/** Timer event ID for code that fades out markers */
	private int markFadeTimerID=-1;

	private void addMarkFadeTimer(final int currentOpacity)
	{
		markFadeTimerID=TimeUtils.addTimedEvent(new Runnable()
		{
			@Override
			public void run()
			{
				// Stop fading if the window is scrolled up
				if(!w.getCanClearAttention())
				{
					tvUI.fadeMark(255);
					markFadeTimerID=-1;
					return;
				}

				int newOpacity=currentOpacity-16;
				if(newOpacity<=0)
				{
					tvUI.removeMark();
					markFadeTimerID=-1;
				}
				else
				{
					tvUI.fadeMark(newOpacity);
					addMarkFadeTimer(newOpacity);
				}
			}
		},250,true);
	}

	/**
	 * Callback: When user scrolls window
	 * @throws GeneralException
	 */
	@UIAction
	public void actionScroll() throws GeneralException
	{
		w.setCanClearAttention(tvUI.isAtEnd());
		fadeMark();
	}

	/**
	 * Callback: When window is closed.
	 * @throws GeneralException
	 */
	public void windowClosed() throws GeneralException
	{
		if(markFadeTimerID!=-1)
			TimeUtils.cancelTimedEvent(markFadeTimerID);
		if(multiLineTimerID!=-1)
			TimeUtils.cancelTimedEvent(multiLineTimerID);
		if(noticeTimerID!=-1)
			TimeUtils.cancelTimedEvent(noticeTimerID);
		context.unrequestMessages(null,this,PluginContext.ALLREQUESTS);
		((IRCUIPlugin)context.getPlugin()).informClosed(this);
	}

	/** @return Category used for logging */
	protected abstract String getLogCategory();

	/** @return Item name used for logging */
	protected abstract String getLogItem();

	/**
	 * Adds a line of text to the window.
	 * @param xml XML to add
	 * @param sLogType Type of text
	 */
	public void addLine(String xml,String sLogType)
	{
		addLine(xml,true,sLogType, false);
	}
	/**
	 * Adds a line of text to the window.
	 * @param xml XML to add
	 */
	public void addLine(String xml)
	{
		addLine(xml,true,null, false);
	}

	String lastTimeStamp=null;

	protected boolean displayTimeStamps()
	{
		return false;
	}

	private String getTimeStamp()
	{
		if(!displayTimeStamps()) return "";
		String timeStamp=new SimpleDateFormat("HH:mm").format(new Date());
		if(lastTimeStamp==null || !lastTimeStamp.equals(timeStamp))
		{
			lastTimeStamp=timeStamp;
			return "<timestamp>"+lastTimeStamp+"</timestamp>";
		}
		return "";
	}

	private String lastDateStamp=null;

	private String getDateStamp()
	{
		if(!displayTimeStamps()) return "";
		String dateStamp=new SimpleDateFormat("EEEEE d MMMM").format(new Date());
		if(lastDateStamp==null)
		{
			// Don't display first one
			lastDateStamp=dateStamp;
		}
		else if(!lastDateStamp.equals(dateStamp))
		{
			lastDateStamp=dateStamp;
			return "<datestamp>"+lastDateStamp+"</datestamp>";
		}
		return "";
	}

	protected void clearMark()
	{
		tvUI.removeMark();
	}

	private final static Pattern PATTERN_URL=
		Pattern.compile("(\\b)((http(s)?://[^<]*?|www\\.[a-zA-Z0-9-]+\\.[a-zA-Z0-9.-]+(?:/[^<]*?)?))(<|, | |\\.$|\\. |\\)|$)");

	/**
	 * Processes colours in text then removes 'unsafe' characters not permitted
	 * in XML; normally called by addLine, but can be used by other things too.
	 * @param s Text to process
	 * @return Processed text including xml colour tags if needed
	 */
	protected String processColours(String s)
	{
		Preferences p=context.getSingle(Preferences.class);
		PreferencesGroup pg=p.getGroup(p.getPluginOwner("com.leafdigital.ui.UIPlugin"));
		boolean allowColours=p.toBoolean(
			pg.get(UIPrefs.PREF_IRCCOLOURS,UIPrefs.PREFDEFAULT_IRCCOLOURS));

		// Do IRC colours
		s = context.getSingle(IRCEncoding.class).processEscapes(
			s, true, allowColours);
		return s.replaceAll("[\\x00-\\x1f]","");
	}

	protected void addLine(String s,boolean bAttention,String sLogType, boolean arbitraryXML)
	{
		try
		{
			if((!w.getCanClearAttention() || !w.isActive()) && !tvUI.hasMark())
			{
				tvUI.markPosition();
			}

			// Do colours and remove special characters
			String safe = processColours(s);

			// Replace URL
			StringBuffer output=new StringBuffer();
			Matcher m=PATTERN_URL.matcher(safe);
			while(m.find())
			{
				String replace=m.group(1)+"<url>"+m.group(2)+"</url>"+m.group(5);
				try
				{
					// Same logic used in TextViewImp to allow urls without http
					String url=m.group(2);
					if(!(url.startsWith("http://") || url.startsWith("https://")))
						url="http://"+url;
					new URL(url);
				}
				catch(MalformedURLException e)
				{
					// Don't put the url tags in
					replace=m.group(1)+m.group(2)+m.group(5);
				}
				// Replace \ with \\.
				for(int pos=0;;)
				{
					int backslash=replace.indexOf('\\',pos);
					if(backslash==-1) break;
					replace=replace.substring(0,backslash)+"\\\\"+replace.substring(backslash+1);
					pos=backslash+2;
				}
				// Replace $ with \$. I can't get it to do this with a regexp replace
				for(int pos=0;;)
				{
					int dollar=replace.indexOf('$',pos);
					if(dollar==-1) break;
					replace=replace.substring(0,dollar)+"\\$"+replace.substring(dollar+1);
					pos=dollar+2;
				}

				// OK it's good, let's replace it
				m.appendReplacement(output,replace);
			}
			m.appendTail(output);
			safe=output.toString();

			// Highlighter
			try
			{
				safe = context.getSingle(Highlighter.class).highlight(
					getOwnNick(), safe);
			}
			catch(XMLException e)
			{
				throw new GeneralException(e);
			}

			boolean bAtEnd=tvUI.isAtEnd();
			if(arbitraryXML)
			{
				tvUI.addXML(safe);
			}
			else
			{
				tvUI.addXML(getDateStamp()+"<line>"+getTimeStamp()+safe+"</line>");
			}
			if(bAtEnd) tvUI.scrollToEnd();
			if(bAttention) w.attention();
			if(sLogType!=null && getLogSource()!=null)
			{
				getPluginContext().getSingle(Logger.class).log(
					getLogSource(),getLogCategory(),getLogItem(),sLogType,safe);
			}
		}
		catch(GeneralException ge)
		{
			ErrorMsg.report("Error adding text: "+s,ge);
		}
	}

	protected abstract String getLogSource();

	protected static String displayTime(long time)
	{
		Calendar c=Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY,0);
		c.set(Calendar.MINUTE,0);
		c.set(Calendar.SECOND,0);
		c.set(Calendar.MILLISECOND,0);
		long today=c.getTimeInMillis();

		if(time<today || time > today+(24*60*60*1000))
		{
			SimpleDateFormat sdf=new SimpleDateFormat("HH:mm 'on' EEE, dd MMMM yyyy");
			return sdf.format(new Date(time));
		}
		else
		{
			SimpleDateFormat sdf=new SimpleDateFormat("HH:mm:ss 'today'");
			return sdf.format(new Date(time));
		}
	}

	@Override
	public void showError(String message)
	{
		addLine("<error>"+message+"</error>");
	}

	@Override
	public void showInfo(String message)
	{
		addLine("<info>"+message+"</info>");
	}

	protected abstract boolean isUs(String target);

	@Override
	public void showOwnText(int type,String target,String text)
	{
		if(isUs(target) && type==TYPE_MSG || type==TYPE_ACTION)
		{
			String sNick=getOwnNick();
			switch(type)
			{
			case MessageDisplay.TYPE_MSG:
				addLine("&lt;<nick>"+esc(sNick)+"</nick>&gt; <owntext>"+esc(text)+"</owntext>","msg");
				break;
			case MessageDisplay.TYPE_ACTION:
				addLine(ACTIONSYMBOL+"<nick>"+esc(sNick)+"</nick> <owntext>"+esc(text)+"</owntext>","action");
				break;
			}
		}
		else
		{
		  addLine("-> "+esc(target)+": "+esc(text));
		}
	}

	protected static String esc(String text)
	{
		return XML.esc(XML.convertMultipleSpaces(text));
	}

	/**
	 * Chat window should add tab-completion options that it provides.
	 * @param options List for options
	 */
	public void fillTabCompletionList(TabCompletionList options)
	{
	}

	protected abstract String getOwnNick();

	/** @return Context nickname, if this window has one */
	protected String getContextNick()
	{
		return null;
	}

	/** @return Context channel, if this window has one */
	protected String getContextChannel()
	{
		return null;
	}

	@Override
	public void addItems(PopupMenu pm, Node n)
	{
	}

	@Override
	public void clear()
	{
		tvUI.clear();
	}

	/**
	 * Called to handle multi-line input.
	 * @param text Input text
	 * @throws GeneralException
	 */
	public void handleMultiLine(String text) throws GeneralException
	{
		new MultiLineDialog(text);
	}

	/** Dialog used when pasting in multi-line text. */
	@UIHandler("multiline")
	public class MultiLineDialog
	{
		private Dialog d;
		/** Paste as-is */
		public RadioButton asIsUI;
		/** Paste joined */
		public RadioButton joinUI;
		/** Display for preview */
		public TextView previewUI;
		/** Which character separates joined lines */
		public EditBox separatorUI;
		/** Info about the number of messages etc */
		public Label infoUI;

		private String[] originalLines,lines;

		MultiLineDialog(String text) throws GeneralException
		{
			String[] initialLines=text.split("\n");
			LinkedList<String> lines = new LinkedList<String>();
			for(int i=0;i<initialLines.length;i++)
			{
				String line=initialLines[i].trim();
				if(line.length()>0)
				{
					lines.add(line);
				}
			}
			originalLines=lines.toArray(new String[lines.size()]);

			UI ui=context.getSingle(UI.class);
			d = ui.createDialog("multiline", this);
			asIsUI.setSelected();
			actionAsIs();
			d.show(w);
		}

		/**
		 * Callback: When separator field is changed.
		 * @throws GeneralException
		 */
		@UIAction
		public void changeSeparator() throws GeneralException
		{
			joinUI.setSelected(); // If it isn't already
			actionJoin();
		}

		/**
		 * Callback: When 'as is' is clicked
		 * @throws GeneralException
		 */
		@UIAction
		public void actionAsIs() throws GeneralException
		{
			update(originalLines);
		}

		/**
		 * Callback: When 'join' is clicked
		 * @throws GeneralException
		 */
		@UIAction
		public void actionJoin() throws GeneralException
		{
			String separator=separatorUI.getValue();
			List<String> initialLines = new LinkedList<String>();
			StringBuffer currentLine=new StringBuffer(originalLines[0]);
			for(int line=1;line<originalLines.length;line++)
			{
				String trimmed=originalLines[line].trim();
				String append=" "+separator+" "+trimmed;
				try
				{
					if((currentLine.toString()+append).getBytes("UTF-8").length
						<= getAvailableBytes())
					{
						currentLine.append(append);
					}
					else
					{
					  initialLines.add(currentLine.toString());
					  currentLine.setLength(0);
					  currentLine.append(trimmed);
					}
				}
				catch(UnsupportedEncodingException e)
				{
					throw new BugException(e); // Can't happen
				}
			}
			initialLines.add(currentLine.toString());
			update(initialLines.toArray(new String[initialLines.size()]));
		}

		private void update(String[] initialLines) throws GeneralException
		{
			List<String> wrappedLines = new LinkedList<String>();
			String before = commandUI.getValue();
			for(int i=0; i<initialLines.length; i++)
			{
				// Let the edit box do the splitting
				commandUI.setValue(initialLines[i]);
				String[] addLines=commandUI.getValueLines();
				for(int j=0;j<addLines.length;j++)
					wrappedLines.add(addLines[j]);
			}
			commandUI.setValue(before);
			lines=wrappedLines.toArray(new String[wrappedLines.size()]);

			previewUI.clear();
			for(int i=0;i<lines.length;i++)
			{
				previewUI.addLine("&lt;<nick>"+getOwnNick()+"</nick>&gt; "+XML.esc(lines[i]));
			}
			infoUI.setText("<key>"+lines.length+"</key> line"+(lines.length!=1 ? "s" : "")+". Paste "+
				((lines.length<=2) ? "will be immediate." : "will take "+(lines.length*2)+" seconds."));
		}

		/**
		 * Callback: When user clicks OK
		 * @throws GeneralException
		 */
		@UIAction
		public void actionPaste() throws GeneralException
		{
			if(lines.length<=2)
			{
				Commands c=context.getSingle(Commands.class);
				for(int i=0;i<lines.length;i++)
					doCommand(c,"/say "+lines[i]);
			}
			else
			{
				multiLineBuffer.addAll(Arrays.asList(lines));
				if(multiLineTimerID==-1)
				{
					// Do first line straight away
					multiLineRunnable.run();
				}
			}
			d.close();
		}

		/**
		 * Callback: When user clicks Cancel.
		 */
		@UIAction
		public void actionCancel()
		{
			d.close();
		}
	}

	private int multiLineTimerID=-1;

	private LinkedList<String> multiLineBuffer=new LinkedList<String>();

	private Runnable multiLineRunnable=new Runnable()
	{
		@Override
		public void run()
		{
			String line=multiLineBuffer.removeFirst();
			Commands c=context.getSingle(Commands.class);
			try
			{
				doCommand(c,"/say "+line);
			}
			catch(GeneralException e)
			{
				ErrorMsg.report("Unexpected error handling multiline paste",e);
			}

			if(multiLineBuffer.isEmpty())
			{
				multiLineTimerID=-1;
			}
			else
			{
				multiLineTimerID=TimeUtils.addTimedEvent(multiLineRunnable,2000,true);
			}
		}
	};

	/** Time before a chan is considered idle (10 mins) */
	private final static long IDLE_TIME=10*60*1000;

	/**
	 * Called by subclass every time an actual 'message' (i.e. somebody saying
	 * something) occurs.
	 * @param title Title for display of notification message
	 * @param text Text for said message
	 */
	protected void reportActualMessage(String title,String text)
	{
		long now=System.currentTimeMillis();
		if(w.isHidden() || w.isMinimized())
		{
			context.getSingle(Notification.class).notify(
				IRCUIPlugin.NOTIFICATION_WINDOWMINIMIZED,title,text);
		}
		else if(now-lastMessage > IDLE_TIME)
		{
			long minutes=(now-lastMessage) / (60*1000);
			context.getSingle(Notification.class).notify(
				IRCUIPlugin.NOTIFICATION_DEIDLE,title,text+"\n\n(Previously idle "+minutes+" minutes)");
		}
		else if(!context.getSingle(UI.class).isAppActive())
		{
			context.getSingle(Notification.class).notify(
				IRCUIPlugin.NOTIFICATION_APPLICATIONINACTIVE,title,text);
		}
		lastMessage=now;
	}

	/**
	 * @return Command edit box
	 */
	public EditBox getCommandEdit()
	{
		return commandUI;
	}
}
