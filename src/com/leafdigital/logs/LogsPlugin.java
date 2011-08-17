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

import java.io.File;
import java.text.*;

import util.*;

import com.leafdigital.logs.api.Logger;
import com.leafdigital.prefs.api.*;
import com.leafdigital.ui.api.UI;

import leafchat.core.api.*;

/** Plugin that handles logging and related preferences. */
public class LogsPlugin implements Plugin
{
	final static String
		PREF_ROLLTIME="rolltime",PREF_ROLLTIME_DEFAULT="5",
		PREF_ONLYSELECTED="onlyselected",PREF_ONLYSELECTED_DEFAULT="f",
		PREFGROUP_SELECTED="selected",PREFGROUP_DONOTLOG="donotlog",PREFGROUP_NEVERDELETE="neverdelete",PREF_ITEM="item",
		PREF_RETENTION="retention",PREF_RETENTION_DEFAULT="30"	,
		PREF_ARCHIVE="archive",PREF_ARCHIVE_DEFAULT="f",
		PREF_DONOTLOG_INITED="donotlog-inited";

	final static String[] PREF_DONOTLOG_DEFAULTS=
	{
		"NickServ","ChanServ"
	};

	int getRollTime()
	{
		Preferences p=pc.getSingleton2(Preferences.class);
		return p.toInt(p.getGroup(this).get(PREF_ROLLTIME,PREF_ROLLTIME_DEFAULT));
	}

	int getRetentionDays()
	{
		Preferences p=pc.getSingleton2(Preferences.class);
		return p.toInt(p.getGroup(this).get(PREF_RETENTION,PREF_RETENTION_DEFAULT));
	}

	boolean shouldArchive()
	{
		Preferences p=pc.getSingleton2(Preferences.class);
		return p.toBoolean(p.getGroup(this).get(PREF_ARCHIVE,PREF_ARCHIVE_DEFAULT));
	}

	private void initDoNotLog()
	{
		Preferences p=pc.getSingleton2(Preferences.class);
		PreferencesGroup pg=p.getGroup(this);
		if(pg.get(PREF_DONOTLOG_INITED,"0").equals("0"))
		{
			for(int i=0;i<PREF_DONOTLOG_DEFAULTS.length;i++)
			{
				PreferencesGroup newItem=pg.getChild(PREFGROUP_DONOTLOG).addAnon();
				newItem.set(PREF_ITEM,PREF_DONOTLOG_DEFAULTS[i]);
			}
			pg.set(PREF_DONOTLOG_INITED,"1");
		}
	}

	boolean shouldExpire(String date,String category,String item)
	{
		Preferences p=pc.getSingleton2(Preferences.class);
		PreferencesGroup pg=p.getGroup(this);
		int retention=p.toInt(pg.get(PREF_RETENTION,PREF_RETENTION_DEFAULT));

		// Has user set it not to delete anything?
		if(retention==0) return false;

		// Is it time yet?
		try
		{
			long
				then=(new SimpleDateFormat(LoggerImp.ISOFORMAT)).parse(date).getTime(),
				now=System.currentTimeMillis();

			if( (now-then)/(1000L*60L*60L*24L) < retention ) return false;
		}
		catch(ParseException e)
		{
			throw new BugException(e);
		}

		// Is this on the don't-delete list?
		PreferencesGroup[] neverdelete=pg.getChild(	LogsPlugin.PREFGROUP_NEVERDELETE).getAnon();
		for(int i=0;i<neverdelete.length;i++)
		{
			if(matchItem(category,item,neverdelete[i].get(LogsPlugin.PREF_ITEM)))
				return false;
		}

		// Then go ahead and delete it!
		return true;
	}

	boolean shouldLog(String category,String item)
	{
		Preferences p=pc.getSingleton2(Preferences.class);
		PreferencesGroup pg=p.getGroup(pc.getPlugin());

		// Check if logging only selected items
		boolean onlySelected=p.toBoolean(pg.get(LogsPlugin.PREF_ONLYSELECTED,LogsPlugin.PREF_ONLYSELECTED_DEFAULT));
		if(onlySelected)
		{
			PreferencesGroup[] selected=pg.getChild(	LogsPlugin.PREFGROUP_SELECTED).getAnon();
			for(int i=0;i<selected.length;i++)
			{
				if(matchItem(category,item,selected[i].get(LogsPlugin.PREF_ITEM)))
					return true;
			}
			return false;
		}

		// Check donotlog list
		PreferencesGroup[] donotlog=pg.getChild(	LogsPlugin.PREFGROUP_DONOTLOG).getAnon();
		for(int i=0;i<donotlog.length;i++)
		{
			if(matchItem(category,item,donotlog[i].get(LogsPlugin.PREF_ITEM)))
				return false;
		}

		return true;
	}

	private boolean matchItem(String category,String item,String pattern)
	{
		return StringUtils.matchWildcard(pattern.toLowerCase(),item.toLowerCase());
	}

	private LoggerImp l;
	private PluginContext pc;

	@Override
	public void init(PluginContext pc, PluginLoadReporter plr) throws GeneralException
	{
		this.pc=pc;
		l=new LoggerImp(pc,plr,new File(PlatformUtils.getUserFolder(),"logs"));
		pc.registerSingleton(Logger.class,l);

		pc.getSingleton2(UI.class).registerTool(new LogsTool(pc));

		initDoNotLog();
	}

	@Override
	public void close() throws GeneralException
	{
		if(l!=null) l.close();
	}

	@Override
	public String toString()
	{
		return "Logs plugin";
	}


}
