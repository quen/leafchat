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

import java.util.HashSet;
import leafchat.core.api.Msg;

/**
 * Message sent to identify available notification types. These are events
 * that the application might want to notify the user about by a popup
 * message.
 * <p>
 * Plugins that use notification should request this message and, on
 * receiving it, call {@link #addType(String, boolean)} to register their
 * types.
 */
public class NotificationListMsg extends Msg
{
	private HashSet<String> types = new HashSet<String>();
	private HashSet<String> defaultTypes = new HashSet<String>();

	/**
	 * Adds a type. It is safe to add a type multiple times.
	 * @param type String defining the type. This should be a user-friendly
	 *   string as it will be displayed in a preferences interface.
	 * @param defaultOn
	 */
	public void addType(String type,boolean defaultOn)
	{
		types.add(type);
		if(defaultOn) defaultTypes.add(type);
	}

	/** @return Array of all type names so far */
	public String[] getTypes()
	{
		return types.toArray(new String[types.size()]);
	}

	/** @return Array of all default type names so far */
	public String[] getDefaultTypes()
	{
		return defaultTypes.toArray(new String[defaultTypes.size()]);
	}
}
