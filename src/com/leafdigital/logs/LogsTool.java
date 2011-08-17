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
package com.leafdigital.logs;

import java.awt.event.MouseEvent;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;

import org.w3c.dom.Element;

import util.PlatformUtils;
import util.xml.*;

import com.leafdigital.logs.LoggerImp.LogFileInfo;
import com.leafdigital.logs.api.Logger;
import com.leafdigital.prefs.api.*;
import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/**
 * Logs tool (dialog and controls).
 */
@UIHandler({"logs", "additem"})
public class LogsTool implements SimpleTool
{
	// Auto-set widgets from main page...
	private PluginContext context;

	/** UI: Main log view */
	public TextView tvUI;
	/** UI: Date list */
	public ListBox datesUI;
	/** UI: Item list */
	public ListBox itemsUI;
	/** UI: Search box */
	public EditBox searchUI;
	/** UI: Search button */
	public Button searchButtonUI;
	/** UI: Clear search button */
	public Button clearSearchUI;
	/** UI: Search initial/ongoing panel */
	public ChoicePanel searchChoiceUI;
	/** UI: Search progress bar */
	public Progress searchProgressUI;
	/** UI: Export button */
	public Button exportUI;

	// ...from settings page...
	/** UI: Rollover hour edit */
	public EditBox rolloverHourUI;
	/** UI: Log only selected list */
	public ListBox selectedUI;
	/** UI: Do not log list */
	public ListBox doNotLogUI;
	/** UI: Add to selected */
	public Button selectedAddUI;
	/** UI: Delete from selected */
	public Button selectedDeleteUI;
	/** UI: Delete from do not log */
	public Button doNotLogDeleteUI;
	/** UI: Log only selected radio button */
	public RadioButton logSelectedUI;
	/** UI: Log everything except radio button */
	public RadioButton logEverythingUI;

	// ...from storage page...
	/** UI: Never delete list */
	public ListBox neverDeleteUI;
	/** UI: Never delete button */
	public Button neverDeleteDeleteUI;
	/** UI: Enable archive */
	public RadioButton archiveOnUI;
	/** UI: Disable archive */
	public RadioButton archiveOffUI;

	// ...and from add item dialog
	/** UI: Add item name */
	public EditBox addItemUI;
	/** UI: Set button */
	public Button addItemSetUI;

	private File startFolder;

	private boolean searchCancelled;

	private File displayedFile = null;
	private File[] others = null;

	/**
	 * @param context Context
	 * @throws GeneralException Any error
	 */
	public LogsTool(PluginContext context) throws GeneralException
	{
		this.context = context;
	}

	@Override
	public void removed()
	{
		if(logWindow!=null) logWindow.close();
	}

	@Override
	public String getLabel()
	{
		return "Logs";
	}

	@Override
	public String getThemeType()
	{
		return "logsButton";
	}

	@Override
	public int getDefaultPosition()
	{
		return 150;
	}

	private Window logWindow=null;

	@Override
	public void clicked() throws GeneralException
	{
		if(logWindow==null)
		{
			UI u=context.getSingleton2(UI.class);
			logWindow=u.createWindow("logs", this);
			initWindow();
			logWindow.show(false);
		}
		else
		{
			logWindow.activate();
		}
	}

	private LoggerImp.LogFileInfo[] logFileInfo;

	private void initWindow() throws GeneralException
	{
		logWindow.setRemember("tool","logs");

		LoggerImp li=(LoggerImp)context.getSingleton2(Logger.class);

		// Fill date and item boxes
		logFileInfo=li.getAllLogs();
		Set<String> dates = new TreeSet<String>();
		Set<String> items = new TreeSet<String>(new IgnoreCaseComparator());
		for(int i=0;i<logFileInfo.length;i++)
		{
			dates.add(logFileInfo[i].getDate());
			items.add(logFileInfo[i].getItem());
		}
		for(String item : items)
		{
			itemsUI.addItem(item);
		}
		String[] dateStrings=dates.toArray(new String[dates.size()]);
		for(int i=dateStrings.length-1;i>=0;i--)
		{
			datesUI.addItem(li.displayDate(dateStrings[i]),dateStrings[i]);
		}

		// Set up textview
		tvUI.setAction("file",new TextView.ActionHandler()
		{
			@Override
			public void action(Element e,MouseEvent me) throws GeneralException
			{
				datesUI.clearSelection();
				datesUI.setSelectedData(e.getAttribute("date"),true);
				itemsUI.clearSelection();
				itemsUI.setSelected(e.getAttribute("item"),true);
				selectionChanged();
			}
		});

		// Settings page

		// Roll time
		String value=""+((LogsPlugin)context.getPlugin()).getRollTime();
		if(value.length()==1) value="0"+value;
		rolloverHourUI.setValue(value);

		// Selected items
		Preferences p=context.getSingleton2(Preferences.class);
		PreferencesGroup pg=p.getGroup(context.getPlugin());
		if(p.toBoolean(pg.get(LogsPlugin.PREF_ONLYSELECTED,LogsPlugin.PREF_ONLYSELECTED_DEFAULT)))
			logSelectedUI.setSelected();
		else
			logEverythingUI.setSelected();
		actionLogChanged();

		// Fill lists
		fillSelected();
		fillDoNotLog();
		fillNeverDelete();

		// Keep option
		int retention=((LogsPlugin)context.getPlugin()).getRetentionDays();
		try
		{
			((RadioButton)logWindow.getWidget("keep"+retention)).setSelected();
		}
		catch(BugException be)
		{
			// OK so there wasn't an appropriate radio, let's leave them all unselected
		}

		// Archiving
		if(((LogsPlugin)context.getPlugin()).shouldArchive())
			archiveOnUI.setSelected();
		else
			archiveOffUI.setSelected();
	}

	/** Fills the 'selected' list */
	private void fillSelected()
	{
		fillListBox(LogsPlugin.PREFGROUP_SELECTED,selectedUI);
		selectionSelected();
	}

	/** Fills the 'do not log' list */
	private void fillDoNotLog()
	{
		fillListBox(LogsPlugin.PREFGROUP_DONOTLOG,doNotLogUI);
		selectionDoNotLog();
	}

	/** Fills the 'never delete' list */
	private void fillNeverDelete()
	{
		fillListBox(LogsPlugin.PREFGROUP_NEVERDELETE,neverDeleteUI);
		selectionNeverDelete();
	}

	/**
	 * @param prefGroup Preferences group that items come from
	 * @param listBox List box to fill
	 */
	private void fillListBox(String prefGroup,ListBox listBox)
	{
		Preferences p=context.getSingleton2(Preferences.class);
		PreferencesGroup pg=p.getGroup(context.getPlugin());
		PreferencesGroup[] selected=pg.getChild(	prefGroup).getAnon();
		listBox.clear();
		for(int i=0;i<selected.length;i++)
		{
			listBox.addItem(selected[i].get(LogsPlugin.PREF_ITEM),selected[i]);
		}
	}

	/**
	 * Action: Change selection in dates list.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void selectionDates() throws GeneralException
	{
		selectionChanged();
	}

	/**
	 * Action: Change selection in items list.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void selectionItems() throws GeneralException
	{
		selectionChanged();
	}

	private static class IgnoreCaseComparator implements Comparator<String>
	{
		@Override
		public int compare(String o1,String o2)
		{
			return o1.compareToIgnoreCase(o2);
		}
	}

	private void selectionChanged() throws GeneralException
	{
		actionCancelSearch();

		LoggerImp li=(LoggerImp)context.getSingleton2(Logger.class);

		// Get required content of items list filtered by selected dates
		Set<Object> selectedDates =
			new HashSet<Object>(Arrays.asList(datesUI.getMultiSelectedData()));
		Set<String> selectedItems = new HashSet<String>();
		String[] selectedItemsArray=itemsUI.getMultiSelected();
		for(int i=0;i<selectedItemsArray.length;i++)
		{
			selectedItems.add(selectedItemsArray[i].toUpperCase());
		}
		Set<String> filteredItemsSet =
			new TreeSet<String>(new IgnoreCaseComparator());
		if(selectedDates.size()==0)
		{
			for(int i=0;i<logFileInfo.length;i++)
			{
				filteredItemsSet.add(logFileInfo[i].getItem());
			}
		}
		else
		{
			for(int i=0;i<logFileInfo.length;i++)
			{
				if(selectedDates.contains(logFileInfo[i].getDate()))
					filteredItemsSet.add(logFileInfo[i].getItem());
			}
		}
		String[] filteredItems=filteredItemsSet.toArray(new String[filteredItemsSet.size()]);

		// Does items list need updating?
		String[] currentItems=itemsUI.getItems();
		if(!Arrays.equals(currentItems,filteredItems))
		{
			itemsUI.clear();
			for(int i=0;i<filteredItems.length;i++)
			{
				itemsUI.addItem(filteredItems[i]);
				if(selectedItems.contains(filteredItems[i].toUpperCase()))
				{
					itemsUI.setSelected(filteredItems[i],true);
				}
			}
		}

		// OK cool, now same for dates list
		selectedItems = new HashSet<String>(); // Redo this in case any got filtered
		selectedItemsArray=itemsUI.getMultiSelected();
		for(int i=0;i<selectedItemsArray.length;i++)
		{
			selectedItems.add(selectedItemsArray[i].toUpperCase());
		}
		Set<String> filteredDatesSet = new TreeSet<String>();
		if(selectedItems.size()==0)
		{
			for(int i=0;i<logFileInfo.length;i++)
			{
				filteredDatesSet.add(logFileInfo[i].getDate());
			}
		}
		else
		{
			for(int i=0;i<logFileInfo.length;i++)
			{
				if(selectedItems.contains(logFileInfo[i].getItem().toUpperCase()))
					filteredDatesSet.add(logFileInfo[i].getDate());
			}
		}
		String[] filteredDates=new String[filteredDatesSet.size()];
		int iPos=filteredDates.length;
		for(String s : filteredDatesSet)
		{
			filteredDates[--iPos] = s;
		}

		// Does dates list need updating?
		String[] currentDates=datesUI.getData(String.class);
		if(!Arrays.equals(currentDates,filteredDates))
		{
			datesUI.clear();
			for(int i=0;i<filteredDates.length;i++)
			{
				String displayDate=li.displayDate(filteredDates[i]);
				datesUI.addItem(displayDate,filteredDates[i]);
				if(selectedDates.contains(filteredDates[i]))
				{
					datesUI.setSelectedData(filteredDates[i],true);
				}
			}
			selectedDates = new HashSet<Object>(Arrays.asList(datesUI.getMultiSelectedData()));
		}

		// Fine, now have we selected a single date/item yet?
		Set<LogFileInfo> files = new HashSet<LogFileInfo>();

		boolean single=false;
		for(int i=0;i<logFileInfo.length;i++)
		{
			LogFileInfo current=logFileInfo[i];
			if(selectedDates.contains(current.getDate()) && selectedItems.contains(current.getItem().toUpperCase()))
			{
				if(!files.isEmpty())
				{
					LogFileInfo other=files.iterator().next();
					if(!(other.getDate().equals(current.getDate()) && other.getItem().equalsIgnoreCase(current.getItem())))
					{
						single=false;
						break;
					}
				}
				files.add(current);
				single=true;
			}
		}

		// Load and display that log (supposing it isn't already)
		if(single)
		{
			if(files.size()==1)
			{
				displayFile(files.iterator().next().getFile());
			}
			else // Same item and date, two servers
			{
				// Sort list by date of first event
				TreeMap<Long, File> tm = new TreeMap<Long, File>();
				for(LogFileInfo lfi : files)
				{
					try
					{
						BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(lfi.getFile()),"UTF-8"));
						String line=br.readLine();
						Matcher m=Pattern.compile("^<e time='([0-9]+)'.*$").matcher(line);
						if(m.matches())
							tm.put(new Long(Long.parseLong(m.group(1))),lfi.getFile());
						else
							ErrorMsg.report("Unexpected content in log file "+lfi.getFile(),null);
					}
					catch(IOException e)
					{
						ErrorMsg.report("Error reading log file "+lfi.getFile(),e);
					}
				}

				if(tm.isEmpty()) // Due to error above
				{
					displayFile(null);
				}
				else
				{
					// Get first file to use as unique identifier
					Iterator<Map.Entry<Long, File>> i = tm.entrySet().iterator();
					File first = i.next().getValue();

					// Get array of other files
					i.remove();
					File[] others=tm.values().toArray(new File[tm.values().size()]);

					displayFile(first,others);
				}
			}
		}
		else
		{
			displayFile(null);
		}
	}

	/**
	 * Causes a new file to be displayed in the view.
	 * @param f File to display or null for blank
	 * @throws GeneralException
	 */
	private void displayFile(File f) throws GeneralException
	{
		displayFile(f,new File[0]);
	}

	/**
	 * Causes a new file to be displayed in the view.
	 * @param f First file to display or null for blank
	 * @param others Other files to display after the first, or empty array
	 * @throws GeneralException
	 */
	private void displayFile(File f,File[] others) throws GeneralException
	{
		if(f==null)
		{
			if(displayedFile!=null)
			{
				displayedFile=null;
				tvUI.clear();
				exportUI.setEnabled(false);
			}
			return;
		}

		if(f.equals(displayedFile)) return;
		displayedFile=f;
		this.others = others;
		exportUI.setEnabled(true);

		tvUI.clear();

		if(others.length==0)
		{
			addFileToView(displayedFile,false);
		}
		else
		{
			addFileToView(displayedFile,true);
			for(int i=0;i<others.length;i++)
				addFileToView(others[i],true);
		}
	}

	/**
	 * @param f File to display
	 * @param showServer If true, includes server name
	 * @throws GeneralException
	 */
	private void addFileToView(File f,boolean showServer) throws GeneralException
	{
		LoggerImp li=(LoggerImp)context.getSingleton2(Logger.class);
		LinkedList<String> lines = li.readFileLines(f);

		if(showServer)
		{
			LogFileInfo lfi=new LogFileInfo(f);
			tvUI.addXML("<box><line>On server <key>"+XML.esc(lfi.getServer())+"</key></line></box>");
		}
		String lastTimestamp=null;
		SimpleDateFormat sdf=new SimpleDateFormat("HH:mm");
		while(!lines.isEmpty())
		{
			String line=lines.removeFirst();

			String timestamp="";
			long eventTime;
			try
			{
				eventTime=Long.parseLong(line.replaceAll("^.*?time=['\"]([0-9]+).*$","$1"));
				timestamp="<timestamp>"+sdf.format(new Date(eventTime))+"</timestamp>";
				if(timestamp.equals(lastTimestamp))
					timestamp="";
				else
					lastTimestamp=timestamp;
			}
			catch(NumberFormatException nfe)
			{
			}
			line=line.replaceAll("(^<e[^>]+>)|(</e>$)","");

			tvUI.addXML("<line>"+timestamp+line+"</line>");
		}
	}

	private void addFileToWriter(Writer w, File f, boolean showServer)
		throws IOException, GeneralException
	{
		LoggerImp li = (LoggerImp)context.getSingleton2(Logger.class);
		LinkedList<String> lines = li.readFileLines(f);

		String lf = "\n";
		if(PlatformUtils.isWindows())
		{
			lf = "\r\n";
		}

		if(showServer)
		{
			LogFileInfo lfi = new LogFileInfo(f);
			w.write("==" + lf + "== On server " + lfi.getServer() + lf + "==" +
				lf + lf);
		}
		String lastTimestamp = null;
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		while(!lines.isEmpty())
		{
			String line = lines.removeFirst();

			String timestamp = "";
			long eventTime;
			try
			{
				eventTime = Long.parseLong(
					line.replaceAll("^.*?time=['\"]([0-9]+).*$", "$1"));
				timestamp = sdf.format(new Date(eventTime));
				if(timestamp.equals(lastTimestamp))
				{
					timestamp = "";
				}
				else
				{
					lastTimestamp = timestamp;
					w.write("[" + timestamp + "]" + lf);
				}
			}
			catch(NumberFormatException nfe)
			{
			}
			line = line.replaceAll("(^<e[^>]+>)|(</e>$)","");

			// Remove all XML tags from line
			line = line.replaceAll("<[^>]+>", "");
			// Replace (some) entities
			line = XML.unesc(line);
			w.write(line + lf);
		}
	}

	/** Action: Window closed. */
	@UIAction
	public void windowClosed()
	{
		actionCancelSearch();
		logWindow=null;
	}

	/** Action: Change search text. */
	@UIAction
	public synchronized void changeSearch()
	{
		boolean hasText = searchUI.getValue().length()>0;
		searchButtonUI.setEnabled(hasText);
		clearSearchUI.setEnabled(hasText);
		datesUI.setEnabled(!hasText);
		itemsUI.setEnabled(!hasText);
	}

	/** Action: Clear search field. */
	@UIAction
	public synchronized void clearSearch()
	{
		searchUI.setValue("");
		changeSearch();
	}

	/**
	 * Action: Begin search.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void actionSearch() throws GeneralException
	{
		if(!searchButtonUI.isEnabled()) return;

		displayFile(null);
		tvUI.clear();
		datesUI.clearSelection();
		itemsUI.clearSelection();
		selectionChanged();

		// Things to search for
		final Set<String>
			positiveWords=new HashSet<String>(), // All these words (AND) must be in file
			negativeWords=new HashSet<String>(); // All these words (OR) must NOT be in file
		List<String[]>
			negativePhraseList=new LinkedList<String[]>(), // After getting candidate files, scan and reject with this phrase
			positivePhraseList=new LinkedList<String[]>(); // After getting candidate files, scan and reject without this phrase

		// Split search into quoted units or words and work out what this means for search
		Pattern p=Pattern.compile("(?:(-)?\"(.*?)\")|(?:(-)?([^\" ]+))");
		Matcher m=p.matcher(searchUI.getValue());
		while(m.find())
		{
			String wordGroup=m.group(4)!=null ? m.group(4) : m.group(2);
			boolean negative=m.group(4)!=null ? (m.group(3)!=null) : (m.group(1)!=null);

			List<String> l = new LinkedList<String>();
			LoggerImp.splitWords(wordGroup,l);
			if(l.size()==1)
			{
				String word=l.get(0);
				if(negative)
					negativeWords.add(word);
				else
					positiveWords.add(word);
			}
			else if(l.size()>1)
			{
				String[] words=l.toArray(new String[l.size()]);
				if(negative)
					negativePhraseList.add(words);
				else
				{
					positivePhraseList.add(words);
					for(int i=0;i<words.length;i++)
					{
						positiveWords.add(words[i]);
					}
				}
			}
		}
		final String[][] positivePhrases=positivePhraseList.toArray(
			new String[positivePhraseList.size()][]);
		final String[][] negativePhrases=negativePhraseList.toArray(
			new String[negativePhraseList.size()][]);

		if(positiveWords.size()<1)
		{
			tvUI.addLine("<error>You must search for at least one positive word.</error>");
			return;
		}

		// OK, apply search
		searchProgressUI.setProgress(0);
		searchCancelled=false;
		(new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					searchChoiceUI.display("searchActive");
					try
					{
						LoggerImp l=(LoggerImp)context.getSingleton2(Logger.class);

						SortedSet<SearchResult> results = new TreeSet<SearchResult>();
						doSearch(positiveWords,negativeWords,positivePhrases,negativePhrases,l,results);

						synchronized(LogsTool.this)
						{
							if(searchCancelled)
								return;

							if(results.isEmpty())
							{
								tvUI.addXML("<error>No matching log entries found.</error>");
							}
							else
							{
								for(SearchResult sr : results)
								{
									tvUI.addXML(
										"<searchresult><file date='"+sr.lfi.getDate()+"' item='"+XML.esc(sr.lfi.getItem())+"'>" +
										"<date>"+XML.esc(l.displayDate(sr.lfi.getDate()))+"</date> "+
										(sr.lfi.getCategory().equals("chan")
											? "<chan>"+XML.esc(sr.lfi.getItem())+"</chan>"
											: "<nick>"+XML.esc(sr.lfi.getItem())+"</nick>"
											)+" <searchserver>("+XML.esc(sr.lfi.getServer())+")</searchserver></file>"+
										sr.ss.getXML()+"</searchresult>");
								}
								tvUI.addXML("<line>Search complete with "+results.size()+" results.</line>");
							}
						}
					}
					finally
					{
						searchChoiceUI.display("searchInput");
					}
				}
				catch(GeneralException ge)
				{
					ErrorMsg.report("Error in search",ge);
				}
			}
		},"Log search thread")).start();
	}

	/**
	 * @param positiveWords Words that must exist
	 * @param negativeWords Words that must not exist
	 * @param positivePhrases Phrases that must exist
	 * @param negativePhrases Phrases that must not exist
	 * @param l Logger
	 * @param results Set that receives results
	 * @throws GeneralException
	 */
	private void doSearch(Set<String> positiveWords, Set<String> negativeWords,
		String[][] positivePhrases,String[][] negativePhrases,LoggerImp l,
		SortedSet<SearchResult> results) throws GeneralException
	{
		Set<File> totalFound=null;
		for(String positiveWord : positiveWords)
		{
			Set<File> found=l.findWord(positiveWord);
			if(totalFound==null)
			{
				totalFound=found;
			}
			else
			{
				for(Iterator<File> j=totalFound.iterator();j.hasNext();)
				{
					File f = j.next();
					if(!found.contains(f))
					{
						j.remove();
					}
				}
			}
		}

		// totalFound now includes all files with the positive words, let's
		// chuck out the negative...
		for(String negativeWord : negativeWords)
		{
			Set<File> found = l.findWord(negativeWord);
			for(File remove : found)
			{
				totalFound.remove(remove);
			}
		}

		int n=0;
		searchProgressUI.setRange(totalFound.size());
		// Right, now we have to read each file to get summaries or check phrases
		fileloop: for(Iterator<File> i=totalFound.iterator(); i.hasNext();)
		{
			searchProgressUI.setProgress(n++);
			File f=i.next();
			boolean[] gotPhrase=new boolean[positivePhrases.length];
			SummarySegment currentSegment=new SummarySegment(),bestSegment=null;
			int bestScore=-1;
			int gotPhrases=0;

			try
			{
				BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(f),"UTF-8"));
				while(true)
				{
					if(searchCancelled) return;

					String line=br.readLine();
					if(line==null) break;
					line=line.replaceAll("(^<e[^>]+>)|(</e>$)","");
					String[] words=LoggerImp.extractWords(line);
					if(matchPhrase(words,negativePhrases)!=-1)
					{
						i.remove();
						continue fileloop;
					}
					if(gotPhrases!=gotPhrase.length)
					{
						int found=matchPhrase(words,positivePhrases);
						if(found!=-1 && !gotPhrase[found])
						{
							gotPhrase[found]=true;
							gotPhrases++;
						}
					}
					int score=0;
					for(int word=0;word<words.length;word++)
					{
						if(positiveWords.contains(words[word]))
						{
							// Check word only contains alphanumeric plus ', or outside ASCII,
							// so we know it won't interfere with XML or regex.
							if(!words[word].matches("[\\x00-\\x7f&&[^a-z0-9']]"))
							{
								Matcher highlighter=Pattern.compile(
									"\\b("+words[word]+")\\b",Pattern.CASE_INSENSITIVE).matcher(line);
								StringBuffer result=new StringBuffer();
								int pos=0;
								outerloop: while(highlighter.find())
								{
									// Check we're not replacing stuff inside an xml tag
									for(int index=highlighter.end();index<line.length();index++)
									{
										switch(line.charAt(index))
										{
										case '<' : break;
										case '>' : continue outerloop; // This must be inside a tag
										}
									}
									// OK, add text before and replace it
									result.append(line.substring(pos,highlighter.start()));
									result.append("<found>");
									result.append(highlighter.group(1));
									result.append("</found>");
									pos=highlighter.end();
								}
								result.append(line.substring(pos));
								line=result.toString();
							}

							score+=10+words[word].length();
						}
					}

					currentSegment=currentSegment.next(line,score);
					if(currentSegment.getScore() > bestScore)
					{
						bestSegment=currentSegment;
						bestScore=currentSegment.getScore();
					}
				}
				br.close();
			}
			catch(IOException ioe)
			{
				throw new GeneralException("Failed to load log file",ioe);
			}

			// Didn't find all the positive phrases
			if(gotPhrases < gotPhrase.length)
				continue;

			results.add(new SearchResult(new LoggerImp.LogFileInfo(f),bestSegment));
		}
		searchProgressUI.setProgress(n);
	}

	private static class SearchResult implements Comparable<SearchResult>
	{
		LoggerImp.LogFileInfo lfi;
		SummarySegment ss;

		@Override
		public boolean equals(Object obj)
		{
			return (obj instanceof SearchResult) &&
				lfi.getFile().equals(((SearchResult)obj).lfi.getFile());
		}

		@Override
		public int compareTo(SearchResult other)
		{
			int i=other.lfi.getDate().compareTo(lfi.getDate());
			if(i!=0) return i;
			i=lfi.getCategory().compareTo(other.lfi.getCategory());
			if(i!=0) return i;
			i=lfi.getItem().compareTo(other.lfi.getItem());
			if(i!=0) return i;
			i=lfi.getServer().compareTo(other.lfi.getServer());
			return i;
		}

		public SearchResult(LogFileInfo lfi,SummarySegment ss)
		{
			this.lfi=lfi;
			this.ss=ss;
		}
	}

	private int matchPhrase(String[] words, String[][] phrases)
	{
		for(int phrase=0;phrase<phrases.length;phrase++)
		{
			for(int word=0;word<words.length-phrases[phrase].length;word++)
			{
				boolean ok=true;
				for(int pos=0;pos<phrases[phrase].length;pos++)
				{
					if(!words[word+pos].equals(phrases[phrase][pos]))
					{
						ok=false;
						break;
					}
				}
				if(ok)
					return phrase;
			}
		}
		return -1;
	}

	private static class SummarySegment
	{
		private final static int SUMMARYLINES=3;
		private final static int MIDPOINT=SUMMARYLINES/2;

		String[] lines=new String[SUMMARYLINES];
		int[] scores=new int[SUMMARYLINES];

		SummarySegment next(String line,int score)
		{
			SummarySegment ss=new SummarySegment();
			for(int i=0;i<SUMMARYLINES-1;i++)
			{
				ss.lines[i]=lines[i+1];
				ss.scores[i]=scores[i+1];
			}
			ss.lines[SUMMARYLINES-1]=line;
			ss.scores[SUMMARYLINES-1]=score;
			return ss;
		}

		int getScore()
		{
			int score=0;
			for(int line=0;line<scores.length;line++)
			{
				// Bias scores towards centre
				score+=scores[line]*(1+MIDPOINT-Math.abs(line - MIDPOINT));
			}
			return score;
		}

		String getXML()
		{
			StringBuffer sb=new StringBuffer();
			for(int i=0;i<SUMMARYLINES;i++)
			{
				if(lines[i]!=null)
					sb.append("<line>"+lines[i]+"</line>");
			}
			return sb.toString();
		}
	}

	/**
	 * Action: Cancel current search.
	 */
	@UIAction
	public void actionCancelSearch()
	{
		synchronized(this)
		{
			searchCancelled=true;
		}
	}

	/**
	 * Action: Export current log
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void actionExport() throws GeneralException
	{
		UI ui = context.getSingleton2(UI.class);
		if(startFolder == null)
		{
			startFolder = new File(PlatformUtils.getDocumentsFolder());
		}
		LogFileInfo lfi = new LogFileInfo(displayedFile);
		File defaultFile = new File(startFolder,
			LoggerImp.toFilePart(lfi.getItem()) + "." + lfi.getDate() + ".txt");
		File target = ui.showFileSelect(logWindow, "Export displayed log", true,
			startFolder, defaultFile, new String[] {".txt"}, "Plain text files");
		if(target != null)
		{
			startFolder = target.getParentFile();

			// Save log into the given file
			try
			{
				Writer out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(target), "UTF-8"));

				if(others.length==0)
				{
					addFileToWriter(out, displayedFile, false);
				}
				else
				{
					addFileToWriter(out, displayedFile, true);
					for(int i=0; i<others.length; i++)
					{
						out.write("\n");
						addFileToWriter(out, others[i], true);
					}
				}

				out.close();
			}
			catch(IOException e)
			{
				throw new GeneralException(e);
			}
		}
	}

	// Settings page
	////////////////

	/**
	 * Action: Changed log option.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void actionLogChanged() throws GeneralException
	{
		Preferences p=context.getSingleton2(Preferences.class);
		boolean onlySelected=logSelectedUI.isSelected();
		p.getGroup(context.getPlugin()).set(LogsPlugin.PREF_ONLYSELECTED,
			p.fromBoolean(onlySelected));

		selectedUI.setEnabled(onlySelected);
		selectedAddUI.setEnabled(onlySelected);
		selectionSelected();
	}

	/**
	 * Action: Selected something in 'log only selected' list.
	 */
	@UIAction
	public void selectionSelected()
	{
		selectedDeleteUI.setEnabled(logSelectedUI.isSelected() && selectedUI.getSelected()!=null);
	}

	/**
	 * Action: Add button in 'selected' list.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void actionSelectedAdd() throws GeneralException
	{
		showAddItem(new AddItemHandler()
		{
			@Override
			public void add(String item) throws GeneralException
			{
				Preferences p=context.getSingleton2(Preferences.class);
				PreferencesGroup pg=p.getGroup(context.getPlugin());
				PreferencesGroup newItem=pg.getChild(LogsPlugin.PREFGROUP_SELECTED).addAnon();
				newItem.set(LogsPlugin.PREF_ITEM,item);
				fillSelected();
			}
		});
	}

	/**
	 * Action: Delete button in 'selected' list.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void actionSelectedDelete() throws GeneralException
	{
		((PreferencesGroup)selectedUI.getSelectedData()).remove();
		fillSelected();
	}

	/**
	 * Action: Selected something in 'do not log' list.
	 */
	@UIAction
	public void selectionDoNotLog()
	{
		doNotLogDeleteUI.setEnabled(doNotLogUI.getSelected()!=null);
	}

	/**
	 * Action: Add button in 'do not log' list.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void actionDoNotLogAdd() throws GeneralException
	{
		showAddItem(new AddItemHandler()
		{
			@Override
			public void add(String item) throws GeneralException
			{
				Preferences p=context.getSingleton2(Preferences.class);
				PreferencesGroup pg=p.getGroup(context.getPlugin());
				PreferencesGroup newItem=pg.getChild(LogsPlugin.PREFGROUP_DONOTLOG).addAnon();
				newItem.set(LogsPlugin.PREF_ITEM,item);
				fillDoNotLog();
			}
		});
	}

	/**
	 * Action: Delete button in 'do not log' list.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void actionDoNotLogDelete() throws GeneralException
	{
		((PreferencesGroup)doNotLogUI.getSelectedData()).remove();
		fillDoNotLog();
	}

	/**
	 * Action: Changed rollover hour.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void changeRolloverHour() throws GeneralException
	{
		boolean ok=rolloverHourUI.getValue().matches("(0[1-9]|1[0-9]|2[0-3]|[0-9])");
		rolloverHourUI.setFlag(ok ? EditBox.FLAG_NORMAL : EditBox.FLAG_ERROR);
		if(ok)
		{
			Preferences p=context.getSingleton2(Preferences.class);
			p.getGroup(context.getPlugin()).set(LogsPlugin.PREF_ROLLTIME,
				p.fromInt(Integer.parseInt(rolloverHourUI.getValue())));
		}
	}

	// Retention page
	/////////////////

	/**
	 * Action: Retention option changed.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void actionKeepChanged() throws GeneralException
	{
		RadioButton selected=logWindow.getGroupSelected("keep");
		if(selected==null) return; // what?

		Preferences p=context.getSingleton2(Preferences.class);
		p.getGroup(context.getPlugin()).set(LogsPlugin.PREF_RETENTION,
			p.fromInt(Integer.parseInt(selected.getID().substring(4))));
	}

	/**
	 * Action: Selected item in 'never delete' list.
	 */
	@UIAction
	public void selectionNeverDelete()
	{
		neverDeleteDeleteUI.setEnabled(neverDeleteUI.getSelected()!=null);
	}

	/**
	 * Action: Add button in 'never delete' list.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void actionNeverDeleteAdd() throws GeneralException
	{
		showAddItem(new AddItemHandler()
		{
			@Override
			public void add(String item) throws GeneralException
			{
				Preferences p=context.getSingleton2(Preferences.class);
				PreferencesGroup pg=p.getGroup(context.getPlugin());
				PreferencesGroup newItem=pg.getChild(LogsPlugin.PREFGROUP_NEVERDELETE).addAnon();
				newItem.set(LogsPlugin.PREF_ITEM,item);
				fillNeverDelete();
			}
		});
	}

	/**
	 * Action: Delete button in 'never delete' list.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void actionNeverDeleteDelete() throws GeneralException
	{
		((PreferencesGroup)neverDeleteUI.getSelectedData()).remove();
		fillNeverDelete();
	}

	/**
	 * Action: Turn on archive.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void actionArchiveOn() throws GeneralException
	{
		setArchive(true);
	}

	/**
	 * Action: Turn off archive.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void actionArchiveOff() throws GeneralException
	{
		setArchive(false);
	}

	private void setArchive(boolean archive)
	{
		Preferences p=context.getSingleton2(Preferences.class);
		PreferencesGroup pg=p.getGroup(context.getPlugin());
		pg.set(LogsPlugin.PREF_ARCHIVE,p.fromBoolean(archive));
	}

	// Add item dialog
	//////////////////

	private interface AddItemHandler
	{
		public void add(String item) throws GeneralException;
	}

	/** Handler that gets called when the add item dialog finishes */
	private AddItemHandler aih;

	/** Add item dialog */
	private Dialog addItem;

	private void showAddItem(AddItemHandler aih) throws GeneralException
	{
		this.aih = aih;
		UI ui = context.getSingleton2(UI.class);
		addItem = ui.createDialog("additem", this);
		addItem.show(logWindow);
	}

	/**
	 * Action: Text changed in item to add.
	 */
	@UIAction
	public void changeAddItem()
	{
		boolean ok=addItemUI.getValue().matches("[^ *]+\\*?");
		addItemSetUI.setEnabled(ok);
		addItemUI.setFlag(ok ? EditBox.FLAG_NORMAL : EditBox.FLAG_ERROR);
	}

	/**
	 * Action: Set button clicked.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void actionAddItemSet() throws GeneralException
	{
		if(!addItemSetUI.isEnabled()) return;
		aih.add(addItemUI.getValue());
		actionAddItemCancel();
	}

	/**
	 * Action: Cancel button clicked.
	 */
	@UIAction
	public void actionAddItemCancel()
	{
		addItem.close();
	}
}
