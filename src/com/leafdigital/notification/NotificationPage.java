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

import java.util.*;

import util.PlatformUtils;
import util.xml.*;

import com.leafdigital.notification.api.NotificationListMsg;
import com.leafdigital.prefs.api.*;
import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/**
 * Preferences page with notification options.
 */
@UIHandler("notification")
public class NotificationPage
{
	private PluginContext context;
	private Page p;

	private final static int COL_NAME=0,COL_ENABLED=1;

	/**
	 * Choice panel: OS choice.
	 */
	public ChoicePanel osPanelUI;
	/**
	 * Choice panel: Growl options.
	 */
	public ChoicePanel growlPanelUI;
	/**
	 * Label: OS version.
	 */
	public Label osVersionUI;
	/**
	 * Label: Java link.
	 */
	public Label javaLinkUI;
	/**
	 * Table: notifications
	 */
	public Table notificationsUI;

	NotificationPage(PluginContext pc)
	{
		this.context=pc;
		UI ui=pc.getSingle(UI.class);
		p=ui.createPage("notification", this);
	}

	/**
	 * Action: Page selected.
	 */
	@UIAction
	public void onSet()
	{
		NotificationPlugin np=(NotificationPlugin)context.getPlugin();
		if(PlatformUtils.isMac())
		{
			osPanelUI.display("mac");
			int state=np.getGrowl().getState();
			growlPanelUI.display("growl"+state);
		}
		else if(PlatformUtils.isJavaVersionAtLeast(1,6))
		{
			if(np.isUsingSystemTray())
			{
				osPanelUI.display("java6");
				notificationsUI.clear();
				NotificationListMsg list=np.getNotifications();
				TreeSet<String> allTypes =
					new TreeSet<String>(Arrays.asList(list.getTypes()));
				HashSet<String> defaultTypes =
					new HashSet<String>(Arrays.asList(list.getDefaultTypes()));
				Preferences p=context.getSingle(Preferences.class);
				PreferencesGroup group=p.getGroup(context.getPlugin());
				for(String name : allTypes)
				{
					int index=notificationsUI.addItem();
					notificationsUI.setString(index,COL_NAME,name);
					boolean enabled=p.toBoolean(group.get("enabled-"+NotificationPlugin.getPrefName(name),
						defaultTypes.contains(name) ? "t" : "f"));
					notificationsUI.setBoolean(index,COL_ENABLED,enabled);
				}
			}
			else
			{
				osPanelUI.display("nosupport");
			}
		}
		else
		{
			osPanelUI.display("oldjava");
			osVersionUI.setText("You are using Java version <strong>"+
				XML.esc(System.getProperty("java.version"))+"</strong>.");
		}
	}

	/**
	 * Action: Table changed.
	 * @param index Row
	 * @param column Column
	 * @param before Previous value (?)
	 */
	@UIAction
	public void changeNotifications(int index,int column,Object before)
	{
		String name=notificationsUI.getString(index,COL_NAME);

		NotificationPlugin np=(NotificationPlugin)context.getPlugin();
		NotificationListMsg list=np.getNotifications();
		HashSet<String> defaultTypes =
			new HashSet<String>(Arrays.asList(list.getDefaultTypes()));

		Preferences p=context.getSingle(Preferences.class);
		PreferencesGroup group=p.getGroup(context.getPlugin());

		group.set("enabled-"+NotificationPlugin.getPrefName(name),
			p.fromBoolean(notificationsUI.getBoolean(index,column)),
			p.fromBoolean(defaultTypes.contains(name)));

		np.updateTrayIcon();
	}

	Page getPage()
	{
		return p;
	}
}
