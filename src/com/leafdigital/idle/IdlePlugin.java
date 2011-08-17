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
package com.leafdigital.idle;

import java.awt.Point;
import java.util.*;

import util.*;

import com.leafdigital.idle.api.Idle;
import com.leafdigital.irc.api.*;
import com.leafdigital.prefs.api.*;
import com.leafdigital.prefsui.api.PreferencesUI;

import leafchat.core.api.*;

/** Plugin for handling auto-away/idle detection. */
public class IdlePlugin implements Plugin,Idle,Runnable
{
	/** Preference to enable auto-away */
	public final static String PREF_AUTOAWAY="auto-away";
	/** Default auto-away (on) */
	public final static String PREFDEFAULT_AUTOAWAY="t";
	/** Preference to control idle timeout */
	public final static String PREF_IDLETIME="idle-time";
	/** Default idle timeout (15m) */
	public final static String PREFDEFAULT_IDLETIME="15";
	/** Preference to control what counts as activity */
	public final static String PREF_ACTIVE="active";
	/** Active value: IRC command */
	public final static String PREFVALUE_ACTIVE_COMMAND="command";
	/** Active value: mouse movement */
	public final static String PREFVALUE_ACTIVE_MOUSE="mouse";
	/** Default activity: mouse */
	public final static String PREFDEFAULT_ACTIVE=PREFVALUE_ACTIVE_MOUSE;
	/** Preference to enable auto-unaway when command is typed */
	public final static String PREF_AUTOUNAWAY="auto-unaway";
	/** Default auto un-away (on) */
	public final static String PREFDEFAULT_AUTOUNAWAY="t";

	private final static long IDLECHECK_INTERVAL=30000;
	private int eventID;

	private long lastAction;

	private LinkedList<Server> markedServers=null;

	private PluginContext context;

	@Override
	public synchronized void init(PluginContext context, PluginLoadReporter reporter) throws GeneralException
	{
		this.context=context;
		// Become a singleton
		context.registerSingleton(Idle.class,this);

		// Register prefs page
		PreferencesUI preferencesUI=context.getSingleton2(PreferencesUI.class);
		preferencesUI.registerPage(this,(new IdlePage(context)).getPage());

		// Start tracking idle state
		eventID=TimeUtils.addTimedEvent(this,IDLECHECK_INTERVAL,true);
	}

	@Override
	public synchronized void close() throws GeneralException
	{
		TimeUtils.cancelTimedEvent(eventID);
	}

	private Point getMousePosition()
	{
		try
		{
			Object pointerInfo=Class.forName("java.awt.MouseInfo").
			  getMethod("getPointerInfo").invoke(null);
			return (Point)pointerInfo.getClass().
  		  getMethod("getLocation").invoke(pointerInfo);
		}
		catch(Exception e)
		{
			return new Point(0,0);
		}
	}

	private Point lastMousePosition=null;

	/** Executed every 30 seconds to check if the user has idled too long */
	@Override
	public synchronized void run()
	{
		try
		{
			long now=System.currentTimeMillis();
			if(lastAction==0) lastAction=now;

			Preferences p=context.getSingleton2(Preferences.class);
			PreferencesGroup pg=p.getGroup(context.getPlugin());
			boolean includeMouse=pg.get(IdlePlugin.PREF_ACTIVE,IdlePlugin.PREFDEFAULT_ACTIVE).equals(
				IdlePlugin.PREFVALUE_ACTIVE_MOUSE) && PlatformUtils.isJavaVersionAtLeast(1,5);
			if(includeMouse)
			{
				Point mousePosition=getMousePosition();
				if(lastMousePosition==null || !lastMousePosition.equals(mousePosition))
				{
					lastAction=now;
					lastMousePosition=mousePosition;
				}
			}

			boolean enabled=p.toBoolean(
				pg.get(IdlePlugin.PREF_AUTOAWAY,IdlePlugin.PREFDEFAULT_AUTOAWAY));
			if(enabled)
			{
				int minutes=p.toInt(
					pg.get(IdlePlugin.PREF_IDLETIME,IdlePlugin.PREFDEFAULT_IDLETIME));
				if( now-lastAction > minutes*60000L )
					setAutoAway(minutes);
			}
		}
		finally
		{
			eventID=TimeUtils.addTimedEvent(this,IDLECHECK_INTERVAL,true);
		}
	}

	@Override
	public String toString()
	{
	  // Used to display in system log etc.
		return "Idle plugin";
	}

	private void setAutoAway(int minutes)
	{
		String message="Auto-away: idle "+minutes+" minutes";

		Connections c=context.getSingleton2(Connections.class);
		Server[] servers=c.getConnected();
		markedServers = new LinkedList<Server>();
		for(int i=0;i<servers.length;i++)
		{
			// Mark away if the user hasn't done it manually
			if(!servers[i].isAway())
			{
				markedServers.add(servers[i]);
				servers[i].sendLine(IRCMsg.constructBytes("AWAY :"+message));
			}
		}
	}

	private void cancelAutoAway()
	{
		// Mark unaway for all servers that we marked away
		if(markedServers!=null)
		{
			for(Iterator<Server> i=markedServers.iterator();i.hasNext();)
			{
				Server s = i.next();
				if(s.isConnected() && s.isAway())
				{
					s.sendLine(IRCMsg.constructBytes("AWAY"));
				}
			}
		}

		markedServers=null;
	}

	@Override
	public synchronized void userAwake(String action)
	{
		lastAction=System.currentTimeMillis();
		if(markedServers!=null)
		{
			// If they just canceled away, we never count that as a reason to cancel
			// away ourselves.
			if(!action.equals(Idle.AWAKE_UNAWAY))
			{
				Preferences p=context.getSingleton2(Preferences.class);
				PreferencesGroup pg=p.getGroup(context.getPlugin());
				boolean cancel=p.toBoolean(
					pg.get(IdlePlugin.PREF_AUTOUNAWAY,IdlePlugin.PREFDEFAULT_AUTOUNAWAY));
				if(cancel)
				{
					cancelAutoAway();
				}
			}
			markedServers=null;
		}
	}
}
