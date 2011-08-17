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

import java.io.*;
import java.lang.Character.UnicodeBlock;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import org.w3c.dom.*;

import util.xml.*;

import com.leafdigital.logs.api.Logger;

import leafchat.core.api.*;

class LoggerImp implements Logger,Runnable
{
	static final String ISOFORMAT="yyyy-MM-dd";

	private final static int DEBUGLEVEL=20;

	private static final String INDEX_FILES="index.files";
	private static final String INDEX_WORDS="index.words";
	/** Max. number of bytes stored for a word in the index */
	private static final int INDEX_WORDBYTES=8;
	/** Max. number of files stored in a single index entry */
	private static final int INDEX_FILESPERENTRY=6;
	/** Number of bytes per entry as consequence */
	private static final int INDEX_BYTESPERENTRY=INDEX_WORDBYTES+INDEX_FILESPERENTRY*4;

	/** How many updates we can cache before flushing */
	private static final int UPDATES_MAXPENDING=16384;

	/** How frequently all logs are flushed */
	private static final int FILEFLUSHTIME=5*1000;

	/** How long before an unused logfile is discarded */
	private static final int FILECLOSETIME=60*1000;

	/** Log folder */
	private File folder;

	/** Context */
	private PluginContext context;

	/** Index file */
	private RandomAccessFile indexFile;

	/** Position at end of file */
	private int fileEndPos;

	/** File list from index, sorted by ID->name */
	private Map<Integer, String> filesIDToName = new HashMap<Integer, String>();

	/** File list from index, sorted by name->ID */
	private Map<String, Integer> filesNameToID=new HashMap<String, Integer>();

	/** Word data from index */
	private Map<String, IndexEntry[]> words = new HashMap<String, IndexEntry[]>();

	/** Map from log file (File) -> LogStream */
	private Map<File, LogStream> currentStreams = new HashMap<File, LogStream>();

	/** Buffered list of updates to index file */
	private SortedSet<IndexUpdate> indexUpdates = new TreeSet<IndexUpdate>();

	/** Regular expression matching files */
	private final static Pattern LOGFILENAME=Pattern.compile(
		"([0-9]{4}-[0-9]{2}-[0-9]{2})_([^_]+)_([^_]+)_([^_]+).lclog");

	/**
	 * Maximum number of blank entries to remember the location of (so we don't
	 * waste memory storing too many of them)
	 */
	private final static int INDEX_MAXBLANKENTRIES=4096;

	/**
	 * There must be at least 8 times as many real entries as blank ones, otherwise
	 * the file will be compacted.
	 */
	private final static int INDEX_COMPACTTHRESHOLD=8;

	/**
	 * List of Integer storing file positions that are available for reuse
	 */
	private LinkedList<Integer> blankEntrySpaces = new LinkedList<Integer>();

	private static boolean DEBUG_REBUILD_INDEX = false;
	private static boolean DEBUG_ANALYSE_INDEX = false;

	/** If bClose is true, closes down log and sets bClosed */
	private boolean close,closed;


	private void addIndexUpdate(IndexUpdate iu) throws IOException
	{
		indexUpdates.add(iu);
		if(indexUpdates.size() >= UPDATES_MAXPENDING)
			applyIndexChanges();
	}

	/** Stores a currently-open log file stream */
	private static class LogStream
	{
		Writer w;
		boolean dirty;
		long lastUsed;
	}

	/** One entry from the index file */
	private static class IndexEntry
	{
		/** Position in file for random updates */
		int filePos;
		/** List of file IDs stored */
		int[] files=new int[INDEX_FILESPERENTRY];
	}

	/** Stores a random-access update to the index file */
	private class IndexUpdate implements Comparable<IndexUpdate>
	{
		private int pos;
		private ByteArrayOutputStream outputBytes=new ByteArrayOutputStream();
		private DataOutputStream outputData=new DataOutputStream(outputBytes);

		IndexUpdate(int iPos)
		{
			this.pos=iPos;
		}

		void writeBytes(byte[] ab) throws IOException
		{
			outputData.write(ab);
		}
		void writeInt(int i) throws IOException
		{
			outputData.writeInt(i);
		}
		void writeByte(int i) throws IOException
		{
			outputData.writeByte(i);
		}

		void apply() throws IOException
		{
			indexFile.seek(pos);
			indexFile.write(outputBytes.toByteArray());
		}

		// Allow sort by file position in the hope that this will
		// reduce HD seek time when making a change batch
		@Override
		public int compareTo(IndexUpdate otherUpdate)
		{
			if(otherUpdate == this) return 0;
			else if(pos < otherUpdate.pos) return -1;
			else if(pos > otherUpdate.pos) return 1;
			// Because of the way writes work (we either write an entire new block,
			// or else a single entry inside a block which will be indexed at least
			// by 12 bytes), we should never try to write to the same position more
			// than once.
			throw new Error("Index writes inconsistent");
		}
	}

	LoggerImp(PluginContext context,PluginLoadReporter plr,File folder) throws GeneralException
	{
		this.folder=folder;
		this.context=context;
		try
		{
			loadIndex(plr);
		}
		catch(IOException ioe)
		{
			throw new GeneralException("Failed to initialise log index. Are you running "+
				"two copies of the program at once? That isn't supported.",ioe);
		}

		Thread t=new Thread(this,"Logger thread");
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
		}

	/** Handles thread that flushes logs etc. */
	@Override
	public synchronized void run()
	{
		try
		{
			while(true)
			{
				try
				{
					wait(FILEFLUSHTIME);
				}
				catch(InterruptedException ie)
				{
				}
				if(close) return;

				// Flush and/or close filehandles
				long now=System.currentTimeMillis();
				for(Iterator<Map.Entry<File, LogStream>> i =
					currentStreams.entrySet().iterator();i.hasNext();)
				{
					Map.Entry<File, LogStream> me = i.next();
					LogStream ls=me.getValue();
					// Write dirty files each time around this loop (5 seconds)
					if(ls.dirty)
					{
						try
						{
							ls.w.flush();
						}
						catch(IOException ioe)
						{
							// Close it, ignoring close errors
							try	{	ls.w.close();	}	catch(IOException ioe2)	{}
							i.remove();
							ErrorMsg.report(
								"Error writing to log file "+me.getKey(),ioe);
							continue;
						}
						ls.dirty=false;
					}
					// Chuck files away if they haven't been written to for a minute
					if(ls.lastUsed + FILECLOSETIME < now)
					{
						try	{	ls.w.close();	}	catch(IOException ioe)	{}
						i.remove();
					}
				}

				// Do index changes
				try
				{
					applyIndexChanges();
				}
				catch(IOException ioe)
				{
					ErrorMsg.report("Error writing to log index ",ioe);
				}
			}
		}
		finally
		{
			for(Iterator<LogStream> i=currentStreams.values().iterator();i.hasNext();)
			{
				LogStream ls = i.next();
				try	{	ls.w.close();	}	catch(IOException ioe)	{}
				i.remove();
			}

			try
			{
				applyIndexChanges();
			}
			catch(IOException ioe)
			{
				ErrorMsg.report("Error writing to log index ",ioe);
			}

			closed=true;
			notifyAll();
		}
	}

	/** Closes the thread and all files. */
	synchronized void close()
	{
		close=true;
		notifyAll();
		while(!closed)
		{
			try
			{
				wait();
			}
			catch(InterruptedException ie)
			{
			}
		}
		try
		{
			indexFile.close();
		}
		catch(IOException ioe)
		{
		}
		debugLog(10,"Log files closed");
	}

	/**
	 * @return Array containing information about all log files
	 */
	LogFileInfo[] getAllLogs()
	{
		List<LogFileInfo> logInfo = new LinkedList<LogFileInfo>();
		File[] logFiles=folder.listFiles();
		if(logFiles==null) logFiles=new File[0];
		for(int i=0;i<logFiles.length;i++)
		{
			try
			{
				logInfo.add(new LogFileInfo(logFiles[i]));
			}
			catch(GeneralException ge)
			{
				// OK, so it wasn't a logfile
			}
		}
		return logInfo.toArray(new LogFileInfo[logInfo.size()]);
	}

	/** Holds parsed information about a logfile */
	public static class LogFileInfo
	{
		private File f;
		private String date;
		private String server;
		private String category;
		private String item;

		LogFileInfo(File f) throws GeneralException
		{
			this.f=f;

			Matcher m=LOGFILENAME.matcher(f.getName());
			if(!m.matches()) throw new GeneralException("Not a log file");

			date=m.group(1);
			server=fromFilePart(m.group(2));
			category=fromFilePart(m.group(3));
			item=fromFilePart(m.group(4));
		}

		public String getDate()	{	return date; }
		public String getServer()	{	return server; }
		public String getCategory()	{	return category; }
		public String getItem()	{	return item; }
		public File getFile() { return f; }
	}

	/**
	 * Converts time into a compact string representation of local time suitable for use
	 * in filenames.
	 * @param time Time in milliseconds
	 * @return String representation e.g. 2006-02-04
	 */
	private String convertTime(long time)
	{
		int rollTime=((LogsPlugin)context.getPlugin()).getRollTime();

		// Subtract rollover time and convert to local time
		SimpleDateFormat sdf=new SimpleDateFormat(ISOFORMAT);
		return sdf.format(new Date(time-rollTime*60*60*1000));
	}

	public String displayDate(String isoDate) throws GeneralException
	{
		String today=convertTime(System.currentTimeMillis());
		if(isoDate.equals(today))
			return "Today";
		String yesterday=convertTime(System.currentTimeMillis()-24*60*60*1000);
		if(isoDate.equals(yesterday))
			return "Yesterday";

		// Get ISO time as Java date
		SimpleDateFormat sdf=new SimpleDateFormat(ISOFORMAT);
		Date d;
		try
		{
			d=sdf.parse(isoDate);
		}
		catch(ParseException e)
		{
			throw new IllegalArgumentException("Date is not in ISO format");
		}

		// Same year
		if(isoDate.split("-")[0].equals(today.split("-")[0]))
			sdf=new SimpleDateFormat("E d MMMM");
		else
			sdf=new SimpleDateFormat("d MMMM yyyy");

		return sdf.format(d);
	}

	/**
	 * Converts value to one safe for use in filenames (and not including the _
	 * separator).
	 * @param value Original string
	 * @return Safe string
	 */
	static String toFilePart(String value)
	{
		StringBuffer sb=new StringBuffer();
		for(int i=0;i<value.length();i++)
		{
			char c=value.charAt(i);
			if( (c>='A' && c<='Z') ||
				(c>='a' && c<='z') ||
				(c>='0' && c<='9') ||
				" ,.#@$^&()-='".indexOf(c)!=-1)
			{
				sb.append(c);
			}
			else
			{
				// Special characters are represented as % then 4 hex digits
				sb.append('%');
				String sHex=Integer.toHexString(c);
				for(int iZero=0;iZero<4-sHex.length();iZero++) sb.append('0');
				sb.append(sHex);
			}
		}
		return sb.toString();
	}

	/**
	 * Converts filename value back to the original string.
	 * @param filePart Value in filename
	 * @return Original filename
	 * @throws GeneralException If filename is invalid
	 */
	private static String fromFilePart(String filePart) throws GeneralException
	{
		StringBuffer sb=new StringBuffer();
		for(int i=0;i<filePart.length();i++)
		{
			char c=filePart.charAt(i);
			if(c!='%')
			{
				sb.append(c);
			}
			else
			{
				if(i+4>=filePart.length())
					throw new GeneralException("Invalid part in filename");
				String sCode=filePart.substring(i+1,i+5);
				i+=4; // Eat those next 4 chars
				try
				{
					sb.append((char)Integer.parseInt(sCode,16));
				}
				catch(NumberFormatException nfe)
				{
					throw new GeneralException("Invalid part in filename");
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Obtains the file that should be used for a particular event. Files are
	 * determined based only on these pieces of information.
	 * @param time Time of event
	 * @param source Source e.g. server address
	 * @param category Category e.g. chan
	 * @param item Item name e.g. channel name
	 * @return File for that logging
	 */
	public File getFile(long time,String source,String category,String item)
	{
		return new File(folder,convertTime(time)+
			"_"+toFilePart(source)+"_"+toFilePart(category)+"_"+toFilePart(item)+".lclog");
	}

	/**
	 * Extracts words from an XML string.
	 * @param xml XML string
	 * @return Array of words
	 * @throws XMLException If string can't be parsed
	 */
	static String[] extractWords(String xml) throws XMLException
	{
		Document d=XML.parse("<root>"+xml+"</root>");

		List<String> l = new LinkedList<String>();
		extractWords(d.getDocumentElement(),l);
		return l.toArray(new String[l.size()]);
	}

	private static void extractWords(Node n, List<String> l)
	{
		if(n instanceof Text)
		{
			splitWords(n.getNodeValue(),l);
		}
		else
		{
			for(Node child=n.getFirstChild();child!=null;child=child.getNextSibling())
			{
				extractWords(child,l);
			}
		}
	}

	static void splitWords(String s, List<String> l)
	{
		StringBuffer current=new StringBuffer();
		for(int i=0;i<s.length();i++)
		{
			char c=s.charAt(i);

			// Note: This is not the official way of splitting words, as per Unicode
			// standard appendix 29, which is hideously complicated. This only supports
			// English and similar languages and (sketchily) Japanese
			if(Character.isLetterOrDigit(c) || c=='\'')
			{
				current.append(c);
			}
			else
			{
				// Add any current data
				if(current.length()>0) l.add(current.toString().toLowerCase());
				current.setLength(0);

				// Some characters get treated as single entries
				UnicodeBlock ub=UnicodeBlock.of(c);
				if(ub==UnicodeBlock.HIRAGANA || ub==UnicodeBlock.KATAKANA ||
					ub==UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS)
				{
					l.add((c+"").toLowerCase());
				}
			}
		}
		// Add any current data
		if(current.length()>0) l.add(current.toString().toLowerCase());
	}

	/**
	 * Does all the saved-up index changes
	 * @throws IOException If there is an error writing the index data
	 */
	private synchronized void applyIndexChanges() throws IOException
	{
		int count=indexUpdates.size();
		long start=System.currentTimeMillis();
		for(Iterator<IndexUpdate> i=indexUpdates.iterator();i.hasNext();)
		{
			IndexUpdate iu =i.next();
			debugLog(40,"Saving changes @ "+iu.pos+" ("+iu.outputBytes.size()+" bytes)");
			iu.apply();
			i.remove();
		}
		if(count>0) debugLog(25,"Saved "+count+" index changes ("+(System.currentTimeMillis()-start)+" ms)");
	}

	private void debugLog(int iLevel,String sText)
	{
		if(iLevel <= DEBUGLEVEL)
			context.logDebug(sText);
	}

	@Override
	public String toString()
	{
		return "Logger";
	}

	/**
	 * Loads the index from disk.
	 * @param plr Reporter for information on load progress
	 * @throws IOException File error loading index
	 * @throws GeneralException Other error loading index
	 */
	private synchronized void loadIndex(PluginLoadReporter plr) throws IOException,GeneralException
	{
		plr.reportProgress("Loading full-text log index...");

		if(!folder.exists()) folder.mkdirs();

		// Clear existing data (if any; only needed after a compact run)
		filesIDToName.clear();
		filesNameToID.clear();
		blankEntrySpaces.clear();
		words.clear();

		long startTime=System.currentTimeMillis();

		// Read file list
		List<File> expireFiles = new LinkedList<File>();
		File fileIndex=new File(folder,INDEX_FILES);
		if(!fileIndex.exists())
		{
			new FileOutputStream(fileIndex).close();
		}
		DataInputStream dis=new DataInputStream(
			new BufferedInputStream(new FileInputStream(fileIndex)));
		try
		{
			while(true)
			{
				int number = dis.readInt();
				String name = dis.readUTF();
				Integer key = number;
				filesIDToName.put(key,name);
				filesNameToID.put(name,key);

				// Check file expiry
				try
				{
					File f=new File(folder,name);

					// Is this file due for expiry yet? OR does it not exist?
					if(!f.exists())
						expireFiles.add(f);
					else
					{
						LogFileInfo lfi=new LogFileInfo(f);
						if(((LogsPlugin)context.getPlugin()).shouldExpire(lfi.date,lfi.category,lfi.item))
							expireFiles.add(f);
					}
				}
				catch(GeneralException e)
				{
					// Shouldn't happen. Ignore for now.
					System.err.println("Problem loading log "+name+" ("+e.getMessage()+")");
				}
			}
		}
		catch(EOFException eof)
		{
		}
		dis.close();

		debugLog(10,"File index loaded with "+
			filesIDToName.keySet().size()+" files: "+(System.currentTimeMillis()-startTime)+"ms");
		startTime=System.currentTimeMillis();

		// Read main index
		File wordsIndex=new File(folder,INDEX_WORDS);
		if(!wordsIndex.exists())
		{
			new FileOutputStream(wordsIndex).close();
		}
		dis=new DataInputStream(
			new BufferedInputStream(new FileInputStream(wordsIndex)));
		byte[] wordBuffer=new byte[INDEX_WORDBYTES];
		int filePos=0,blank=0;
		try
		{
			while(true)
			{
				// Read 8-character max word
				String word;
				dis.readFully(wordBuffer);
				int length=INDEX_WORDBYTES;
				for(;length>0 && wordBuffer[length-1]==0;length--) ;

				if(length<INDEX_WORDBYTES)
				{
					byte[] shortWord=new byte[length];
					System.arraycopy(wordBuffer,0,shortWord,0,length);
					word=new String(shortWord,"UTF-8");;
				}
				else
				{
					word=new String(wordBuffer,"UTF-8");
				}

				// Read 6 * int file IDs that contain word
				boolean blankEntry=true;
				IndexEntry thisEntry=new IndexEntry();
				for(int i=0;i<INDEX_FILESPERENTRY;i++)
				{
					int fileID=dis.readInt();
					thisEntry.files[i]=fileID;
					if(fileID!=0) blankEntry=false;
				}
				blankEntry|=word.length()==0;

				if(blankEntry)
				{
					// Remember blank entries for later reuse
					if(blankEntrySpaces.size() < INDEX_MAXBLANKENTRIES)
					{
						blankEntrySpaces.addLast(filePos);
					}
					blank++;
				}
				else
				{
					// Find existing entries for that word
					IndexEntry[] aie=words.get(word);
					int newPos;
					if(aie==null) // No existing entries, start new array
					{
						aie=new IndexEntry[1];
						words.put(word,aie);
						newPos=0;
					}
					else // Reallocate array to add entry
					{
						IndexEntry[] aieNew=new IndexEntry[aie.length+1];
						System.arraycopy(aie,0,aieNew,0,aie.length);
						aie=aieNew;
						words.put(word,aie);
						newPos=aie.length-1;
					}
					thisEntry.filePos=filePos;
					aie[newPos]=thisEntry;
				}

				filePos+=INDEX_BYTESPERENTRY;
			}
		}
		catch(EOFException eof)
		{
		}
		dis.close();
		fileEndPos=filePos;

		int totalEntries=filePos/INDEX_BYTESPERENTRY;

		debugLog(10,"Word index loaded with "+
			words.keySet().size()+" words in "+totalEntries+" entries ("+
			(filePos / 1024)+"KB), of which "+blank+" blank: "+(System.currentTimeMillis()-startTime)+"ms");

		if(blank>0 && (totalEntries / blank < INDEX_COMPACTTHRESHOLD))
		{
			compactWordIndex(plr);
			loadIndex(plr);
		}

		// Open file now (and keep it open so that we can't end up with two
		// clients writing to it at once, which would be bad)
		indexFile=new RandomAccessFile(wordsIndex,"rw");

		// Expire files
		if(!expireFiles.isEmpty())
		{
			File[] expired=expireFiles.toArray(new File[expireFiles.size()]);

			boolean archive=((LogsPlugin)context.getPlugin()).shouldArchive();
			plr.reportProgress((archive ? "Archiving ": "Deleting ")+
				expired.length+" old log files...");

			startTime=System.currentTimeMillis();
			removeFiles(expired);

			if(archive)
			{
				File archiveFolder=new File(folder,"archive");
				if(!archiveFolder.exists()) archiveFolder.mkdirs();

				for(int i=0;i<expired.length;i++)
				{
					if(!expired[i].exists()) continue;
					if(!expired[i].renameTo(new File(archiveFolder,expired[i].getName())))
						throw new IOException("Failed to move log into archive: "+expired[i]);
				}
			}
			else
			{
				for(int i=0;i<expired.length;i++)
				{
					if(!expired[i].exists()) continue;
					if(!expired[i].delete())
						throw new IOException("Failed to delete log: "+expired[i]);
				}
			}

			debugLog(10,(archive ? "Archived " : "Deleted ")+expired.length+
				" old logs: "+(System.currentTimeMillis()-startTime)+"ms");
		}

		// Uncomment to rebuild index (should probably clear it first)
		if(DEBUG_REBUILD_INDEX) regenerateWordIndex();

		// Uncomment to obtain stats about index
		if(DEBUG_ANALYSE_INDEX) analyseIndex();
	}

	/**
	 * Compacts the word index, removing any unused entries from the file's disk
	 * footprint.
	 * @param plr
	 * @throws IOException
	 */
	private synchronized void compactWordIndex(PluginLoadReporter plr) throws IOException
	{
		plr.reportProgress("Compacting full-text log index...");
		long startTime=System.currentTimeMillis();

		// New file
		File newWordsIndex=new File(folder,INDEX_WORDS+".new");
	  DataOutputStream dos=new DataOutputStream(
	  		new BufferedOutputStream(new FileOutputStream(newWordsIndex)));

		// Go through each word...
	  int totalEntries=0,writtenEntries=0;
		for(Map.Entry<String, IndexEntry[]> me : words.entrySet())
		{
			boolean writtenStart=false;
			int entryPos=0;
			String word = me.getKey();
			IndexEntry[] aie = me.getValue();
			for(int entry=0;entry<aie.length;entry++)
			{
				totalEntries++;
				for(int file=0;file<INDEX_FILESPERENTRY;file++)
				{
					int fileID=aie[entry].files[file];
					if(fileID!=0)
					{
						if(!writtenStart)
						{
							// Write word, zero-padded
							byte[] abWord=word.getBytes("UTF-8");
							dos.write(abWord);
							for(int i=0;i<INDEX_WORDBYTES-abWord.length;i++)
								dos.write(0);
							writtenStart=true;
							writtenEntries++;
						}
						dos.writeInt(fileID);
						entryPos++;
						if(entryPos==INDEX_FILESPERENTRY)
						{
							writtenStart=false;
							entryPos=0;
						}
					}
				}
			}
			if(writtenStart)
			{
				for(;entryPos<INDEX_FILESPERENTRY;entryPos++)
					dos.writeInt(0);
			}
		}
		dos.close();

		// Rename files
		File
			wordsIndex=new File(folder,INDEX_WORDS),
			oldWordsIndex=new File(folder,INDEX_FILES+".old");
		if(oldWordsIndex.exists())
		{
			if(!oldWordsIndex.delete()) throw new IOException(
				"Failed to delete old log file index "+oldWordsIndex);
		}
		if(!wordsIndex.renameTo(oldWordsIndex))
			throw new IOException("Failed to rename old log file index "+wordsIndex);
		if(!newWordsIndex.renameTo(wordsIndex))
			throw new IOException("Failed to rename new log file index "+newWordsIndex);
		oldWordsIndex.delete();

		debugLog(10,"Word index compacted from "+totalEntries+" to "+writtenEntries+
			" entries: "+
			(System.currentTimeMillis()-startTime)+"ms");
	}

	private synchronized void regenerateWordIndex() throws IOException,XMLException,GeneralException
	{
		for(String name : filesNameToID.keySet())
		{
			File f=new File(folder,name);
			debugLog(10,"Regenerating index for "+name);
			reindex(f);
		}
	}

	private void analyseIndex()
	{
		Map<Integer, Integer> mResults = new TreeMap<Integer, Integer>();

		int iUsedWordFiles=0;
		int allocatedWordFiles=0,wastedEntries=0;

		for(Map.Entry<String, IndexEntry[]> me : words.entrySet())
		{
			int count=0;
			IndexEntry[] aie = me.getValue();
			for(int entry=0;entry<aie.length;entry++)
			{
				for(int file=0;file<aie[entry].files.length;file++)
				{
					if(aie[entry].files[file]!=0) count++;
				}
			}

			allocatedWordFiles+=INDEX_FILESPERENTRY*aie.length;
			int fullEntryEquivs=(( (count-1) / INDEX_FILESPERENTRY )+1);
			if(fullEntryEquivs < aie.length)
			{
				System.err.println("Wasted entries: <"+me.getKey()+"> (wanted "+count+" files, using "+aie.length+" entries)");
			}
			wastedEntries+=aie.length - fullEntryEquivs;

			Integer key = count;
			Integer existing=mResults.get(key);
			if(existing==null)
				mResults.put(key,1);
			else
				mResults.put(key,existing + 1);
			iUsedWordFiles+=count;
		}
		System.out.println("Total file/word references: "+iUsedWordFiles);
		System.out.println("          out of allocated: "+allocatedWordFiles);
		System.out.println("     Wasted entire entries: "+wastedEntries);

		int MINFILES=1,MAXFILES=9;
		int[] aiSize=new int[MAXFILES];
		for(Map.Entry<Integer, Integer> me : mResults.entrySet())
		{
			int iUsage = me.getKey();
			int iNumber = me.getValue();
			for(int iBlockSize=MINFILES;iBlockSize<MAXFILES;iBlockSize++)
			{
				// Work out how many blocks it's taking per instance of this many words
				// being full.
				int iBlocks=((iUsage-1)/iBlockSize)+1;
				// OK now multiply by number of instances and size of block
				aiSize[iBlockSize]+=iBlocks*iNumber*(iBlockSize*4+INDEX_WORDBYTES);
			}
		}

		for(int i=MINFILES;i<MAXFILES;i++)
		{
			System.out.println(i+": "+(aiSize[i]/1024)+"KB");
		}

		System.exit(0);
	}

	/**
	 * Find all files that contain a word.
	 * @param s Word being searched
	 * @return Set of File objects
	 */
	synchronized Set<File> findWord(String s)
	{
		Set<File> files=new HashSet<File>();
		IndexEntry[] entries=words.get(getCroppedString(s));
		if(entries!=null)
		{
			for(int iEntry=0;iEntry<entries.length;iEntry++)
			{
				for(int iFile=0;iFile<entries[iEntry].files.length;iFile++)
				{
					int iFileID=entries[iEntry].files[iFile];
					if(iFileID!=0)
					{
						// Look up in file list
						files.add(new File(folder,filesIDToName.get(iFileID)));
					}
				}
			}
		}
		return files;
	}

	/**
	 * Gets ID for the given file. If necessary, adds a new file to the file index.
	 * @param f File to find or add
	 * @return ID of requested file
	 * @throws IOException Any problem updating file index
	 */
	synchronized private int getFileID(File f) throws IOException
	{
		// Look for existing file
		String file=f.getName();
		Integer id=filesNameToID.get(file);
		if(id!=null)
		{
			debugLog(50,"File "+id+" ("+file+"): already indexed");
			return id.intValue();
		}

		// OK, add new file. Begin by finding max ID of existing files
		int max=0;
		for(Integer i : filesIDToName.keySet())
		{
			max = Math.max(max, i);
		}

		// All good, so add that file to the end of the index on disk...
		DataOutputStream dos=new DataOutputStream(
			new FileOutputStream(new File(folder,INDEX_FILES),true));
		dos.writeInt(max+1);
		dos.writeUTF(file);
		dos.close();

		// ...and to the in-memory index
		Integer key = max + 1;
		filesIDToName.put(key,file);
		filesNameToID.put(file,key);

		debugLog(20,"File "+key+" ("+file+"): Added to index");

		return max+1;
	}

	synchronized private void removeFiles(File[] files) throws IOException,GeneralException
	{
		// Find IDs for files and remove from memory maps
		Set<Integer> allIDs = new HashSet<Integer>();
		int[] ids=new int[files.length];
		for(int i=0;i<files.length;i++)
		{
			Integer idInt=filesNameToID.get(files[i].getName());
			if(idInt==null) throw new GeneralException("Log file "+files[i]+" not found in index");
			allIDs.add(idInt);
			ids[i]=idInt.intValue();
		}
		for(int i=0;i<files.length;i++)
		{
			filesIDToName.remove(ids[i]);
			filesNameToID.remove(files[i].getName());
		}

		// Rewrite file index
		File newFileIndex=new File(folder,INDEX_FILES+".new");
		DataOutputStream dos=new DataOutputStream(
			new FileOutputStream(newFileIndex));
		for(Map.Entry<Integer, String> me : filesIDToName.entrySet())
		{
			int id=me.getKey().intValue();
			dos.writeInt(id);
			dos.writeUTF(me.getValue());
		}
		dos.close();

		// Now the time-consuming bit! Scan entire word index...
		for(Map.Entry<String, IndexEntry[]> me : words.entrySet())
		{
			IndexEntry[] aie = me.getValue();
			for(int entry=0;entry<aie.length;entry++)
			{
				IndexEntry currentEntry=aie[entry];
				for(int entryFile=0;entryFile<INDEX_FILESPERENTRY;entryFile++)
				{
					int compare=currentEntry.files[entryFile];
					if(compare==0) continue;

					if(allIDs.contains(compare))
					{
						// Squash this one...
						currentEntry.files[entryFile]=0;

						// Save the change...
						IndexUpdate iu=new IndexUpdate(currentEntry.filePos+INDEX_WORDBYTES+entryFile*4);
						iu.writeInt(0);
						addIndexUpdate(iu);
					}
				}
			}
		}

		// Switch new index for old
		File
			fileIndex=new File(folder,INDEX_FILES),
			oldFileIndex=new File(folder,INDEX_FILES+".old");
		if(oldFileIndex.exists())
		{
			if(!oldFileIndex.delete()) throw new IOException(
				"Failed to delete old log file index "+oldFileIndex);
		}
		if(!fileIndex.renameTo(oldFileIndex))
			throw new IOException("Failed to rename old log file index "+fileIndex);
		if(!newFileIndex.renameTo(fileIndex))
			throw new IOException("Failed to rename new log file index "+newFileIndex);
		oldFileIndex.delete();

		// Must apply all index changes now as these aren't compatible with normal
		// writing (they can write to the same place, which causes a
		// consistency-check error later on)
		applyIndexChanges();
	}

	/**
	 * Adds a word to the index if necessary, updating in memory and on disk.
	 * @param fileID Log file that contains the given word
	 * @param s Word in question
	 * @throws IOException If there's any error writing the index changes
	 */
	synchronized private void addWord(int fileID,String s) throws IOException
	{
		String cropped=getCroppedString(s);
		assert(cropped.length()>0);

		IndexEntry[] entries=words.get(cropped);
		if(entries!=null)
		{
			// We already have entries for this word. See if one of them matches
			// the file in question; otherwise, look for a blank space
			int blankEntry=-1,iBlankFile=-1;
			for(int entry=0;entry<entries.length;entry++)
			{
				for(int file=0;file<INDEX_FILESPERENTRY;file++)
				{
					int thisFileID=entries[entry].files[file];
					if(thisFileID==0 && blankEntry==-1)
					{
						blankEntry=entry;
						iBlankFile=file;
					}

					if(fileID==thisFileID)
					{
						debugLog(50,"Word entry '"+cropped+"': already exists");
						return; // Yay! No need to do anything.
					}
				}
			}

			// Did we find a blank space? If so, use it
			if(blankEntry!=-1)
			{
				// Store in memory...
				entries[blankEntry].files[iBlankFile]=fileID;
				// ...and on disk
				IndexUpdate iu=new IndexUpdate(entries[blankEntry].filePos+INDEX_WORDBYTES+4*iBlankFile);
				iu.writeInt(fileID);
				addIndexUpdate(iu);
				// and we're done
				debugLog(30,"Word entry '"+cropped+"': adding to existing entry");
				return;
			}
		}

		// Need to add a new entry in memory...
		int newPos;
		if(entries==null) // No existing entries, start new array
		{
			entries=new IndexEntry[1];
			words.put(cropped,entries);
			newPos=0;
			debugLog(30,"Word entry '"+cropped+"': creating first entry");
		}
		else // Reallocate array to add entry
		{
			IndexEntry[] newEntries=new IndexEntry[entries.length+1];
			System.arraycopy(entries,0,newEntries,0,entries.length);
			entries=newEntries;
			words.put(cropped,entries);
			newPos=entries.length-1;
			debugLog(30,"Word entry '"+cropped+"': creating additional entry");
		}
		entries[newPos]=new IndexEntry();

		// If there's a blank space, add it there instead of at the end
		if(!blankEntrySpaces.isEmpty())
		{
			Integer i=blankEntrySpaces.getFirst();
			blankEntrySpaces.removeFirst();
			entries[newPos].filePos=i.intValue();
		}
		else
		{
			entries[newPos].filePos=fileEndPos;
			fileEndPos+=INDEX_BYTESPERENTRY;
		}

		entries[newPos].files[0]=fileID;
		IndexUpdate iu=new IndexUpdate(entries[newPos].filePos);
		byte[] word=cropped.getBytes("UTF-8");
		// Write word, zero-padded
		iu.writeBytes(word);
		for(int i=0;i<INDEX_WORDBYTES-word.length;i++)
			iu.writeByte(0);
		// Write this file ID and the blank spaces
		iu.writeInt(fileID);
		for(int i=0;i<INDEX_FILESPERENTRY-1;i++) iu.writeInt(0);
		addIndexUpdate(iu);
	}

	/**
	 * Crops a string to no more than 8 bytes in UTF-8.
	 * @param s The string
	 * @return Cropped string
	 */
	private String getCroppedString(String s)
	{
		int end=s.length();
		for(;end>0;end--)
		{
			byte[] ab;
			try
			{
				ab=s.substring(0,end).getBytes("UTF-8");
				if(ab.length<=8) return s.substring(0,end);
			}
			catch(UnsupportedEncodingException e)
			{
				throw new Error(e);
			}
		}
		throw new Error("Word could not be limited to 8 bytes");
	}

	/**
	 * Reindexes a particular file (if the index may have been damaged)
	 * @param f File to reindex
	 * @throws GeneralException Any problem reading file
	 * @throws IOException Index write error
	 * @throws XMLException Line not valid XML.
	 */
	private void reindex(File f) throws GeneralException, XMLException, IOException
	{
		for(String line : readFileLines(f))
		{
			line=line.replaceAll("^<e[^>]*>(.*)</e>","$1");
			index(f,line);
		}
	}

	/**
	 * Adds information about a particular file to the index.
	 * @param f File in question
	 * @param xml Some XML
	 * @throws IOException If addWord gives an error
	 * @throws XMLException IF the string isn't valid XML
	 */
	private void index(File f,String xml) throws IOException,XMLException
	{
		int fileID=getFileID(f);
		String[] words=extractWords(xml);
		for(int i=0;i<words.length;i++)
		{
			if(words[i].length()>0) addWord(fileID,words[i]);
		}
	}

	/**
	 * Allow read of log files to be managed from here so we can synchronize it.
	 * Doing it unsynchronized caused problems in very rare cases, I think when
	 * it tried to open a file for writing at the same time as reading it.
	 * @param f File to read
	 * @return List of all lines in file
	 * @throws GeneralException If there's a problem loading the file
	 */
	public synchronized LinkedList<String> readFileLines(File f)
		throws GeneralException
	{
		BufferedReader br = null;
		LinkedList<String> results = new LinkedList<String>();
		try
		{
			br = new BufferedReader(
				new InputStreamReader(new FileInputStream(f),"UTF-8"));
			while(true)
			{
				String line = br.readLine();
				if(line==null) break;
				results.addLast(line);
			}
			return results;
		}
		catch(IOException ioe)
		{
			throw new GeneralException("Failed to load log file",ioe);
		}
		finally
		{
			try
			{
				if(br!=null)
				{
					br.close();
				}
			}
			catch(IOException ioe)
			{
				// Ignore exception on close
			}
		}
	}

	@Override
	public synchronized void log(String source,String category,String item,String type,String displayXML)
	{
		if(close) return;
		try
		{
			if(!((LogsPlugin)context.getPlugin()).shouldLog(category,item)) return;

			// Pick current time and find file for it
			long time=System.currentTimeMillis();
			File f=getFile(time,source,category,item);

			// Do we have a stream for that already? If not, make one
			LogStream ls=currentStreams.get(f);
			if(ls==null)
			{
				ls=new LogStream();
				ls.w=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f,true),"UTF-8"));
				currentStreams.put(f,ls);
			}

			// Index data (note: this checks it's valid XML so do it first)
			index(f,displayXML);

			// Write data
			ls.w.write(
				"<e time='"+time+"' type='"+type+"'>"+displayXML+"</e>\n"
				);
			ls.dirty=true;
			ls.lastUsed=time;
		}
		catch(IOException ioe)
		{
			ErrorMsg.report("Error logging data",ioe);
		}
	}
}
