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
package com.leafdigital.prefs;

import java.awt.Font;
import java.io.*;
import java.util.*;

import org.w3c.dom.*;

import util.PlatformUtils;
import util.xml.*;

import com.leafdigital.prefs.api.*;

import leafchat.core.api.*;

/** Provides preferences support (singleton) */
public class PreferencesImp implements Preferences,MsgOwner
{
	/** If the document is dirty, store the time at which it was made so */
	private long dirtyTime=0;

	/** File where prefs are located */
	private File prefsFile;

	/** Owning context */
	private PluginContext context;

	/** True if preferences have been closed and cannot now be changed */
	private boolean closed=false;

	/** Message dispatcher */
	private MessageDispatch mdp;

	/** Root-level groups */
	private Map<String, PreferencesGroupImp> rootGroups =
		new HashMap<String, PreferencesGroupImp>();

	// Preferences implementation
	/////////////////////////////

	@Override
	public synchronized PreferencesGroup getGroup(String owner)
	{
		checkValid(owner);
		PreferencesGroupImp pg = rootGroups.get(owner);
		if(pg == null)
		{
			pg = new PreferencesGroupImp(null,owner);
			rootGroups.put(owner,pg);
		}
		return pg;
	}

	@Override
	public PreferencesGroup getGroup(Plugin p)
	{
		return getGroup(getPluginOwner(p));
	}

	@Override
	public int toInt(String value) throws BugException
	{
		try
		{
			return Integer.parseInt(value);
		}
		catch(NumberFormatException nfe)
		{
			throw new BugException(
				"Could not convert preferences value '"+value+"' to int");
		}
	}

	@Override
	public String fromInt(int value)
	{
		return value+"";
	}

	@Override
	public Font toFont(String value) throws BugException
	{
		String[] parts=value.split(":");
		if(parts.length!=3) throw new BugException(
			"Could not convert preferences value '"+value+"' to Font");
		return new Font(parts[0],
		  toBoolean(parts[1]) ? Font.BOLD : Font.PLAIN,toInt(parts[2]));
	}

	@Override
	public String fromFont(Font value)
	{
		return value.getFamily()+":"+
		  fromBoolean(((value.getStyle() & Font.BOLD)!=0) ? true : false)+":"+
		  fromInt(value.getSize());
	}

	@Override
	public boolean toBoolean(String value) throws BugException
	{
		if(value.equals("t")) return true;
		else if(value.equals("f")) return false;
		else throw new BugException(
			"Could not convert preferences value '"+value+"' to boolean");
	}

	@Override
	public String fromBoolean(boolean value)
	{
		return value ? "t" : "f";
	}

	@Override
	public String getPluginOwner(Plugin p)
	{
		return getPluginOwner(p.getClass().getName());
	}

	@Override
	public String getPluginOwner(String className)
	{
		// Make up a name based on the classname
		return "Plugin_"+className.replaceAll("[^A-Za-z0-9._\\-]","_");
	}

	@Override
	public String getSafeToken(String name)
	{
		StringBuffer sb=new StringBuffer("t_");
		for(int i=0;i<name.length();i++)
		{
			char c=name.charAt(i);
			if(Character.isLetterOrDigit(c) || c=='_' || c=='-')
				sb.append(c);
			else
			{
				sb.append('.');
				String fourDigit=""+Integer.toHexString(c);
				while(fourDigit.length()<4) fourDigit="0"+fourDigit;
				sb.append(fourDigit);
			}
		}
		return sb.toString();
	}

	// MessageOwner implementation
	//////////////////////////////

	@Override
	public void init(MessageDispatch mdp)
	{
		this.mdp=mdp;
	}
	@Override
	public String getFriendlyName()
	{
		return "Preferences change";
	}
	@Override
	public Class<? extends Msg> getMessageClass()
	{
		return PreferencesChangeMsg.class;
	}
	@Override
	public boolean registerTarget(Object target, Class<? extends Msg> message,
		MessageFilter mf, int requestID, int priority)
	{
		// Handle everything automatically
		return true;
	}
	@Override
	public void unregisterTarget(Object target, int requestID)
	{
		// Don't care
	}
	@Override
	public void manualDispatch(Msg m)
	{
		// Still don't care
	}
	@Override
	public boolean allowExternalDispatch(Msg m)
	{
		// Nope, we're the only people who can send preferences changes
		return false;
	}

	// Own methods
	//////////////

	/**
	 * Construct and load existing preferences
	 * @param context Plugin context
	 * @throws GeneralException Any failure
	 */
	PreferencesImp(PluginContext context) throws GeneralException
	{
		try
		{
			this.context=context;
			prefsFile = new File(PlatformUtils.getUserFolder(), "preferences.xml");
			boolean gotPrefs = prefsFile.exists();

			// If leafChat crashed partway through saving prefs, there might be a
			// .old or .new version
			if(!gotPrefs)
			{
				for(String suffix : new String[] {".new", ".old"})
				{
					File otherCopy = new File(prefsFile.getPath() + suffix);
					if(otherCopy.exists())
					{
						renameFileRepeated(otherCopy, prefsFile);
						gotPrefs = true;
						break;
					}
				}
			}

			if(gotPrefs)
			{
				// Load file
				Document d=XML.parse(prefsFile);
				Element[] groups=XML.getChildren(
					d.getDocumentElement(),"group");
				for(int i=0;i<groups.length;i++)
				{
					rootGroups.put(groups[i].getAttribute("name"),
						new PreferencesGroupImp(null,groups[i]));
				}
			}
		}
		catch(IOException e)
		{
			throw new GeneralException(e);
		}
	}

	/**
	 * Sets dirty flag and, if necessary, a new thread that'll save this at
	 * some later point.
	 */
	private synchronized void markDirty()
	{
		if(closed) throw new BugException("Cannot set preferences after close");
		// If it's not marked dirty, need to set up a new watch thread
		if(dirtyTime==0)
		{
			final Runnable r=new Runnable()
			{
				@Override
				public void run()
				{
					// Keep going until we manage to save it
					while(true)
					{
						try
						{
							Thread.sleep(5000);
						}
						catch (InterruptedException e)
						{
						}

						synchronized(PreferencesImp.this)
						{
							// Already saved? Then forget it
							if(dirtyTime==0) return;

							// If 4 seconds went by without doing anything
							if(System.currentTimeMillis()-dirtyTime > 4000)
							{
								flush();
								return;
							}
						}
					}
				}
			};

			(new Thread(r,"PreferencesSaver")).start();
		}
		dirtyTime=System.currentTimeMillis();
	}

	/** Flush any unsaved changes */
	synchronized void flush()
	{
		if(dirtyTime!=0)
		{
			try
			{
				// Note: There were problems with this code on Windows where in rare
				// cases it doesn't work. Since it's so important (you could lose
				// preferences) I have added a lot of defensive code.

				// Save new preferences in .new file
				File saveTemp = new File(prefsFile.getPath() + ".new");
				XML.save(saveTemp, buildPreferencesDoc());

				// Check if we already have a file
				boolean gotOldFile = prefsFile.exists();
				File oldTemp = null;
				if(gotOldFile)
				{
					// Delete the old '.old' copy if present
					oldTemp = new File(prefsFile.getPath() + ".old");
					if(oldTemp.exists())
					{
						deleteFileRepeated(oldTemp);
					}

					// For safety, rename away the old file first instead of deleting it
					renameFileRepeated(prefsFile, oldTemp);
				}

				// Rename new file into place
				renameFileRepeated(saveTemp, prefsFile);

				// Delete temp old file
				if(gotOldFile)
				{
					deleteFileRepeated(oldTemp);
				}

				context.getSingle(SystemLog.class).log(
				  context.getPlugin(),"Preferences saved");
				dirtyTime = 0;
			}
			catch(Exception e)
			{
				ErrorMsg.report("Error while saving preferences", e);
			}
		}
	}

	/**
	 * Renames a file, making repeated attempts if necessary.
	 * @param from Current file
	 * @param to New file
	 * @throws IOException If rename fails 20 times
	 */
	private static void renameFileRepeated(File from, File to) throws IOException
	{
		int retry = 0;
		while(!from.renameTo(to))
		{
			retry++;
			if(retry >= 20)
			{
				throw new IOException("Unable to rename file " + from +
					" to " + from);
			}
			try
			{
				Thread.sleep(100);
			}
			catch(InterruptedException ie)
			{
			}
		}
	}

	/**
	 * Deletes a file, making repeated attempts if necessary.
	 * @param file File to delete
	 * @throws IOException If delete fails 20 times
	 */
	private static void deleteFileRepeated(File file) throws IOException
	{
		int retry = 0;
		while(!file.delete())
		{
			retry++;
			if(retry >= 20)
			{
				throw new IOException("Unable to delete file " + file);
			}
			try
			{
				Thread.sleep(100);
			}
			catch(InterruptedException ie)
			{
			}
		}
	}

	private synchronized Document buildPreferencesDoc() throws XMLException
	{
		Document d=XML.newDocument("preferences");
		addLF(d.getDocumentElement());
		for(PreferencesGroupImp pg : rootGroups.values())
		{
			pg.buildDoc(d.getDocumentElement());
		}
		return d;
	}

	/**
	 * Make sure that string is a valid owner or preference name.
	 * @param s String to check
	 * @throws BugException If the string is not valid
	 */
	private static void checkValid(String s)
	{
		if(s.length()<1)
			throw new BugException("Property name/owner name may not be empty");
		if(!Character.isLetter(s.charAt(0)))
			throw new BugException("Property name/owner name must start with letter");
		for(int i=1;i<s.length();i++)
		{
			char c=s.charAt(i);
			if(c!='-' && c!='_' && c!='.' && !Character.isLetterOrDigit(c))
			  throw new BugException(
					"Property name/owner may only contain letters, digits, . _ and -");
		}
	}

	/**
	 * Flush any data, and don't allow future sets
	 */
	public synchronized void close()
	{
		flush();
		closed=true;
	}

	/** Implementation of public API */
	public class PreferencesGroupImp implements PreferencesGroup
	{
		private final static String NAME_ANON="*ANON*";

		/** Values; map of String -> String */
		private Map<String, String> values = new HashMap<String, String>();
		/** Named groups; map of String -> PreferencesGroupImp */
		private Map<String, PreferencesGroupImp> groups =
			new HashMap<String, PreferencesGroupImp>();
		/** Indexed anonymous groups */
		private PreferencesGroupImp[] anon=new PreferencesGroupImp[0];

		/** Parent */
		private PreferencesGroupImp parent;

		/** Group name */
		private String groupName;

		@Override
		public Preferences getPreferences()
		{
			return PreferencesImp.this;
		}

		private PreferencesGroupImp(PreferencesGroupImp parent,String name)
		{
			this.parent=parent;
			this.groupName=name;
		}

		private PreferencesGroupImp(PreferencesGroupImp parent,Element e)
		{
			this.parent=parent;
			if(e.hasAttribute("name"))
				groupName=e.getAttribute("name");
			else
				groupName=NAME_ANON;

			Element[] aePrefs=XML.getChildren(e,"pref");
			for(int i=0;i<aePrefs.length;i++)
			{
				values.put(aePrefs[i].getAttribute("name"),aePrefs[i].getAttribute("value"));
			}

			try
			{
				if(XML.hasChild(e,"children"))
				{
					Element[] aeGroups=XML.getChildren(XML.getChild(e,"children"),"group");
					for(int i=0;i<aeGroups.length;i++)
					{
						String sName=aeGroups[i].getAttribute("name");
						groups.put(sName,new PreferencesGroupImp(this,aeGroups[i]));
					}
				}

				if(XML.hasChild(e,"anon"))
				{
					Element[] aeAnon=XML.getChildren(XML.getChild(e,"anon"),"group");
					anon=new PreferencesGroupImp[aeAnon.length];
					for(int i=0;i<anon.length;i++)
					{
						anon[i]=new PreferencesGroupImp(this,aeAnon[i]);
					}
				}
			}
			catch(XMLException xe)
			{
				// Can't really happen
				throw new AssertionError(xe);
			}
		}

		private void buildDoc(Element parent)
		{
			Element group=XML.createChild(parent,"group");
			if(groupName!=NAME_ANON) // != is ok because we only ever use constant
				group.setAttribute("name",groupName);
			// Save prefs
			for(Map.Entry<String, String> me : values.entrySet())
			{
				addLF(group);
				Element eValue=XML.createChild(group,"pref");
				eValue.setAttribute("name",me.getKey());
				eValue.setAttribute("value",me.getValue());
			}
			// Save children
			if(groups.values().size()>0)
			{
				addLF(group);
				Element children=XML.createChild(group,"children");
				addLF(children);
				for(PreferencesGroupImp pgi : groups.values())
				{
					pgi.buildDoc(children);
				}
			}
			if(anon.length>0)
			{
				addLF(group);
				Element eAnon=XML.createChild(group,"anon");
				addLF(eAnon);
				for(int iAnon=0;iAnon<anon.length;iAnon++)
				{
					anon[iAnon].buildDoc(eAnon);
				}
			}
			addLF(group);
			addLF(parent);
		}

		/**
		 * @return Index (0-based position) of anonymous group within parent
		 */
		public int getIndex()
		{
			if(parent==null)
				throw new BugException("Not inside parent");
			if(groupName!=NAME_ANON)
				throw new BugException("Not anonymous group");
			for(int i=0;i<parent.anon.length;i++)
			{
				if(parent.anon[i]==this) return i;
			}
			throw new BugException("Not found in parent");
		}

		@Override
		public String toString()
		{
			StringBuffer sb=new StringBuffer();
			if(parent!=null)
			{
				sb.append(parent.toString());
				if(groupName==NAME_ANON)
				{
					sb.append("["+getIndex()+"]");
				}
			}
			if(groupName!=NAME_ANON)
				sb.append("/"+groupName);

			return sb.toString();
		}

		@Override
		public synchronized String get(String name)
		{
			String sValue=get(name,null);
			if(sValue==null)
			  throw new BugException("No such preference: "+toString()+":"+name);
			else
			  return sValue;
		}

		@Override
		public synchronized String get(String name,String defaultValue)
		{
			String value=values.get(name);
			if(value==null)
				return defaultValue;
			else
				return value;
		}

		@Override
		public synchronized PreferencesGroup getChild(String name)
		{
			checkValid(name);
			PreferencesGroupImp pgi=groups.get(name);
			if(pgi==null)
			{
				pgi=new PreferencesGroupImp(this,name);
				groups.put(name,pgi);
				markDirty();
			}
			return pgi;
		}

		@Override
		public synchronized boolean exists(String name)
		{
			return values.containsKey(name);
		}

		@Override
		public synchronized String set(String name,String value)
		{
			checkValid(name);
			String old=values.put(name,value);
			if(old==null || !old.equals(value))
			{
				markDirty();
				mdp.dispatchMessageHandleErrors(new PreferencesChangeMsg(this,name,old,value),false);
			}
			return old;
		}

		@Override
		public synchronized void set(String name,String value,String defaultValue)
		{
			if(value.equals(defaultValue))
				unset(name);
			else
				set(name,value);
		}

		@Override
		public synchronized boolean unset(String name)
		{
			String old=values.remove(name);
			if(old!=null)
			{
				markDirty();
				mdp.dispatchMessageHandleErrors(new PreferencesChangeMsg(this,name,old,null),false);
				return true;
			}
			return false;
		}

		@Override
		public PreferencesGroup getAnonParent()
		{
			if(groupName==NAME_ANON)
				return parent;
			else
				return null;
		}

		@Override
		public String getAnonHierarchical(String name)
		{
			String value=getAnonHierarchical(name,null);
			if(value==null) throw new BugException(
				"No such hierarchical preference: "+toString()+":"+name);
			return value;
		}

		@Override
		public String getAnonHierarchical(String name,String defaultValue)
		{
			return getAnonHierarchical(name,defaultValue,true);
		}

		@Override
		public String getAnonHierarchical(String name,String defaultValue,boolean includeThis)
		{
			if(includeThis)
			{
				String value=get(name,null);
				if(value!=null) return value;
			}

			if(groupName==NAME_ANON && parent!=null)
				return parent.getAnonHierarchical(name,defaultValue);

			return defaultValue;
		}

		@Override
		public synchronized PreferencesGroup findAnonGroup(
			String pref, String value, boolean recursive, boolean ignoreCase)
		{
			for(int i=0; i<anon.length; i++)
			{
				String local = anon[i].get(pref, null);
				if(ignoreCase)
				{
					if(local!=null && local.equalsIgnoreCase(value))
					{
						return anon[i];
					}
				}
				else
				{
					if(local!=null && local.equals(value))
					{
						return anon[i];
					}
				}
				if(recursive)
				{
					PreferencesGroup pg = anon[i].findAnonGroup(
						pref, value, true, ignoreCase);
					if(pg != null)
					{
						return pg;
					}
				}
			}
			return null;
		}

		@Override
		public synchronized PreferencesGroup findAnonGroup(
			String pref, String value,boolean recursive)
		{
			return findAnonGroup(pref, value, recursive, false);
		}

		private synchronized void removeAnon(PreferencesGroupImp child)
		{
			for(int i=0;i<anon.length;i++)
			{
				if(anon[i]==child)
				{
					PreferencesGroupImp[] changed=new PreferencesGroupImp[anon.length-1];
					System.arraycopy(anon,0,changed,0,i);
					System.arraycopy(anon,i+1,changed,i,anon.length-(i+1));
					anon=changed;
					markDirty();
					return;
				}
			}
		}

		private synchronized void removeNamed(String name)
		{
			if(groups.remove(name)!=null) markDirty();
		}

		@Override
		public void remove()
		{
			if(parent==null) return;

			if(groupName==NAME_ANON)
			{
				parent.removeAnon(this);
			}
			else
			{
				parent.removeNamed(groupName);
			}
			synchronized(this)
			{
				parent=null;
			}
		}

		@Override
		public synchronized PreferencesGroup[] getAnon()
		{
			PreferencesGroupImp[] result=new PreferencesGroupImp[anon.length];
			System.arraycopy(anon,0,result,0,result.length);
			return result;
		}

		@Override
		public synchronized PreferencesGroup addAnon()
		{
			PreferencesGroupImp[] changed=new PreferencesGroupImp[anon.length+1];
			System.arraycopy(anon,0,changed,0,anon.length);
			anon=changed;
			anon[anon.length-1]=new PreferencesGroupImp(this,NAME_ANON);

			markDirty();
			return anon[anon.length-1];
		}

		@Override
		public synchronized int addAnon(PreferencesGroup pg,int position)
		{
			pg.remove();
			((PreferencesGroupImp)pg).parent=this;

			if(position<0 || position>anon.length)
				position=anon.length;
			PreferencesGroupImp[] changed=new PreferencesGroupImp[anon.length+1];
			System.arraycopy(anon,0,changed,0,position);
			changed[position]=(PreferencesGroupImp)pg;
			System.arraycopy(anon,position,changed,position+1,anon.length-position);
			anon=changed;

			markDirty();
			return position;
		}

		@Override
		public synchronized void clearAnon()
		{
			if(anon.length==0) return;
			anon=new PreferencesGroupImp[0];
			markDirty();
		}
	}
	private static void addLF(Element parent)
	{
		parent.appendChild(parent.getOwnerDocument().createTextNode("\n"));
	}
}
