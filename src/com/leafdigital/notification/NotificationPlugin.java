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
package com.leafdigital.notification;

import java.awt.event.*;
import java.util.*;

import util.GraphicsUtils;

import com.growl.GrowlWrapper;
import com.leafdigital.irc.api.*;
import com.leafdigital.notification.api.*;
import com.leafdigital.prefs.api.*;
import com.leafdigital.prefsui.api.PreferencesUI;
import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/** Plugin provides notification facilities (popups etc) */
public class NotificationPlugin implements Plugin,Notification
{
	private NotificationListMsgOwner owner=new NotificationListMsgOwner();
	private GrowlWrapper growl;
	private PluginContext context;

	private final static String NOTIFICATION_USERCOMMAND="Scripted /popup events";

	@Override
	public void init(PluginContext context, PluginLoadReporter reporter) throws GeneralException
	{
		// Wait until plugins are loaded before setting up Growl
		context.requestMessages(SystemStateMsg.class,this);
		context.registerMessageOwner(owner);

		// Handle /popup
		context.requestMessages(UserCommandMsg.class,this);
		context.requestMessages(UserCommandListMsg.class,this);
		context.requestMessages(NotificationListMsg.class,this);

		// Become a notification singleton
		context.registerSingleton(Notification.class,this);

		// Register prefs page
		PreferencesUI preferencesUI = context.getSingle(PreferencesUI.class);
		preferencesUI.registerPage(this,(new NotificationPage(context)).getPage());

		// Need to know about preferences changes (in case of change to minimise-to-tray)
		context.requestMessages(PreferencesChangeMsg.class, this);

		this.context=context;
	}

	/** Message owner for notification messages */
	public class NotificationListMsgOwner extends BasicMsgOwner
	{
		@Override
		public String getFriendlyName()
		{
			return "Request supported notification types";
		}

		@Override
		public Class<? extends Msg> getMessageClass()
		{
			return NotificationListMsg.class;
		}
	}

	private boolean isUsingSystemTray;
	private Object trayIcon=null;

	boolean isUsingSystemTray()
	{
		return isUsingSystemTray;
	}

	@Override
	public boolean hasTrayIcon()
	{
		return trayIcon != null;
	}

	void updateTrayIcon()
	{
		if(!isUsingSystemTray) return;

		// Are any notifications turned on?
		boolean enabled = false;
		NotificationListMsg list = getNotifications();
		TreeSet<String> allTypes =
			new TreeSet<String>(Arrays.asList(list.getTypes()));
		HashSet<String> defaultTypes =
			new HashSet<String>(Arrays.asList(list.getDefaultTypes()));
		Preferences p = context.getSingle(Preferences.class);
		PreferencesGroup group = p.getGroup(context.getPlugin());
		for(String name : allTypes)
		{
			if(p.toBoolean(group.get("enabled-"+NotificationPlugin.getPrefName(name),
				defaultTypes.contains(name) ? "t" : "f")))
			{
				enabled = true;
				break;
			}
		}

		// Or does the user have minimise-to-tray on?
		group = p.getGroup(p.getPluginOwner("com.leafdigital.ui.UIPlugin"));
		enabled = enabled || group.get(UIPrefs.PREF_MINIMISE_TO_TRAY,
			UIPrefs.PREFDEFAULT_MINIMISE_TO_TRAY).equals("t");

		if(enabled && trayIcon==null)
		{
			// Let's show the tray icon then
			try
			{
				Class<?> trayClass = Class.forName("java.awt.SystemTray");
				if(((Boolean)trayClass.getMethod("isSupported").invoke(
					null)).booleanValue())
				{
					Object tray = trayClass.getMethod("getSystemTray").invoke(
						null);

					// Load icon of appropriate size
					int iconSize=
						((java.awt.Dimension)trayClass.getMethod("getTrayIconSize", new Class[0]).invoke(
							tray, new Object[0])).height;
					java.awt.Image icon;
					if(iconSize <= 16)
					{
						icon = GraphicsUtils.loadImage(getClass().getResource("icon16.png"));
					}
					else if(iconSize<=32)
					{
						icon = GraphicsUtils.loadImage(getClass().getResource("icon32.png"));
					}
					else
					{
						icon = GraphicsUtils.loadImage(getClass().getResource("icon48.png"));
					}

					Class<?> trayIconClass = Class.forName("java.awt.TrayIcon");
					trayIcon = trayIconClass.
						getConstructor(new Class[] {java.awt.Image.class, String.class}).newInstance(
							new Object[] {icon, "leafChat notifications"});
					trayIconClass.getMethod("setImageAutoSize", new Class[] { boolean.class }).invoke(
						trayIcon, new Object[] { Boolean.TRUE });
					MouseListener mouseListener = new MouseAdapter()
					{
						@Override
						public void mouseClicked(MouseEvent e)
						{
							if(e.getButton() == MouseEvent.BUTTON1)
							{
								context.getSingle(UI.class).activate();
							}
						}
					};
					ActionListener actionListener = new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							context.getSingle(UI.class).activate();
							context.getSingle(UI.class).showLatest();
						}
					};
					trayIconClass.getMethod("addMouseListener",
						new Class[] { MouseListener.class }).invoke(
							trayIcon, new Object[] { mouseListener });
					trayIconClass.getMethod("addActionListener",
						new Class[] { ActionListener.class }).invoke(
							trayIcon, new Object[] { actionListener });
					trayClass.getMethod("add",new Class[] {trayIcon.getClass()}).invoke(
						tray, new Object[] {trayIcon});
				}
			}
			catch(Exception e)
			{
				isUsingSystemTray = false;
				trayIcon = null;
				// This happens on Ubuntu where Java incorrectly reports
				// isSupported = true when it actually throws an exception.
				// So to avoid causing unnecessary user annoyance, log this
				// instead of reporting it as a visible error.
				context.getSingle(SystemLog.class).log(this,
					"Unable to add icon to system tray", e);
			}
		}
		else if(!enabled && trayIcon != null)
		{
			// Get rid of the icon!
			try
			{
				Class<?> trayClass = Class.forName("java.awt.SystemTray");
				Object tray = trayClass.getMethod("getSystemTray").invoke(null);
				trayClass.getMethod("remove", new Class[] {trayIcon.getClass()}).invoke(
						tray, new Object[] {trayIcon});
			}
			catch(Exception e)
			{
			}
			trayIcon = null;
		}

	}

	/**
	 * Message: system state. Used to set up notification once system starts.
	 * @param msg Message
	 */
	public synchronized void msg(SystemStateMsg msg)
	{
		if(msg.getType() == SystemStateMsg.PLUGINSLOADED)
		{
			// Get list of notifications
			NotificationListMsg list = getNotifications();

			// Construct a Growl wrapper
			growl = new GrowlWrapper("leafChat", "leafChat",
				list.getTypes(), list.getDefaultTypes());

			if(growl.getState() == GrowlWrapper.GROWL_NOT_MAC)
			{
				try
				{
					Class<?> trayClass = Class.forName("java.awt.SystemTray");
					if(((Boolean)trayClass.getMethod("isSupported").invoke(
						null)).booleanValue())
					{
						trayClass.getMethod("getSystemTray").invoke(null);
						isUsingSystemTray = true;
					}
				}
				catch(ClassNotFoundException e)
				{
				}
				catch(Exception e)
				{
					ErrorMsg.report("Unable to check popup notification system",e);
				}
			}

			updateTrayIcon();
		}
	}

	/**
	 * Message: preference changed. If it was the minimise-to-tray preference,
	 * we may need to hide or show the icon.
	 * @param msg Message
	 */
	public synchronized void msg(PreferencesChangeMsg msg)
	{
		if(msg.getName().equals(UIPrefs.PREF_MINIMISE_TO_TRAY))
		{
			updateTrayIcon();
		}
	}

	/**
	 * @return List of all notifications
	 */
	NotificationListMsg getNotifications()
	{
		NotificationListMsg list = new NotificationListMsg();
		owner.getDispatch().dispatchMessage(list, true);
		return list;
	}

	/**
	 * Message: notification list. Adds user-command notification to list.
	 * @param msg Message
	 */
	public synchronized void msg(NotificationListMsg msg)
	{
		msg.addType(NOTIFICATION_USERCOMMAND, true);
	}

	/**
	 * Message: user command. Implements the /popup command.
	 * @param msg Message
	 */
	public synchronized void msg(UserCommandMsg msg)
	{
		if("popup".equals(msg.getCommand()))
		{
			String params = msg.getParams().trim();
			if(params.length() == 0)
			{
				msg.getMessageDisplay().showError(
					"/popup requires parameters: /popup Title, or /popup Title / Text");
			}
			else
			{
				String title, text;
				if(params.indexOf('/') == -1)
				{
					title = params;
					text = "";
				}
				else
				{
					title = params.substring(0, params.indexOf('/')).trim();
					text = params.substring(params.indexOf('/') + 1).trim();
				}
				notify(NOTIFICATION_USERCOMMAND, title, text);
			}
			msg.markHandled();
		}
	}

	/**
	 * Message: Listing available commands.
	 * @param msg Message
	 */
	public void msg(UserCommandListMsg msg)
	{
		msg.addCommand(false, "popup", UserCommandListMsg.FREQ_OBSCURE,
			"/popup <title> [/ <text>]",
			"<key>Scripting:</key> Show a notification popup with the given title and " +
			"optional text (after a slash)");
	}

  @Override
	public void notify(String type, String title, String message)
	{
		// Let's see if we can use Java 6 to notify
		if(trayIcon != null)
		{
			// OK, let's see if it's turned on...
			NotificationListMsg list = getNotifications();
			HashSet<String> defaultTypes =
				new HashSet<String>(Arrays.asList(list.getDefaultTypes()));
			Preferences p = context.getSingle(Preferences.class);
			PreferencesGroup group = p.getGroup(context.getPlugin());

			boolean enabled = p.toBoolean(group.get("enabled-" + getPrefName(type),
					defaultTypes.contains(type) ? "t" : "f"));
			if(enabled)
			{
				try
				{
					Class<?> messageType=Class.forName("java.awt.TrayIcon$MessageType");
					trayIcon.getClass().getMethod("displayMessage", new Class[]
					{
						String.class, String.class, messageType
					}).invoke(trayIcon, new Object[]
					{
						title, message, messageType.getField("INFO").get(null)
					});
				}
				catch(Exception e)
				{
					ErrorMsg.report("Error generating popup notification", e);
				}
			}
		}
		else
		{
			growl.notify(type, title, message);
		}
	}

	GrowlWrapper getGrowl()
	{
		return growl;
	}

	@Override
	public void close() throws GeneralException
	{
		if(trayIcon != null)
		{
			try
			{
				Class<?> trayClass = Class.forName("java.awt.SystemTray");
				Object tray = trayClass.getMethod("getSystemTray").invoke(null);
				trayClass.getMethod("remove", new Class[] {trayIcon.getClass()}).invoke(
						tray, new Object[] {trayIcon});
			}
			catch(Exception e)
			{
			}
			trayIcon = null;
		}
	}

	@Override
	public String toString()
	{
	  // Used to display in system log etc.
		return "Notification plugin";
	}

	static String getPrefName(String notification)
	{
		return notification.toLowerCase().replaceAll("[^a-z0-9]", "_");
	}
}
