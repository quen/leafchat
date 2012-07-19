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
package com.leafdigital.notification.api;

import leafchat.core.api.Singleton;

/**
 * Singleton that provides popup notification facilities.
 * @see NotificationListMsg
 */
public interface Notification extends Singleton
{
	/**
	 * Creates the notification popup.
	 * @param type Type (must have been previously registered via
	 *   {@link NotificationListMsg})
	 * @param title Title for popup
	 * @param message Text for popup (empty string if none)
	 */
	public void notify(String type,String title,String message);

	/**
	 * Checks whether the tray icon (owned by this plugin) exists or not.
	 * @return True if there is a tray icon
	 */
	public boolean hasTrayIcon();
}
