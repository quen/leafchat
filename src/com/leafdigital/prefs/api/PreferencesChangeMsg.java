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
package com.leafdigital.prefs.api;

import leafchat.core.api.Msg;

/**
 * Sent when preferences change.
 */
public class PreferencesChangeMsg extends Msg
{
	private String name,oldValue,newValue;
	private PreferencesGroup pg;

	/**
	 * @param pg Group that changed
	 * @param name Preference within that group that changed
	 * @param oldValue Previous value
	 * @param newValue New value
	 */
	public PreferencesChangeMsg(
		PreferencesGroup pg,String name,String oldValue,String newValue)
	{
		this.pg=pg;
		this.name=name;
		this.oldValue=oldValue;
		this.newValue=newValue;
	}

	/** @return Property group this comes from */
	public PreferencesGroup getGroup()
	{
		return pg;
	}

	/** @return Property name */
	public String getName()
	{
		return name;
	}

	/** @return Previous value (null if unset) */
	public String getOldValue()
	{
		return oldValue;
	}

	/** @return New value (null if unset) */
	public String getNewValue()
	{
		return newValue;
	}
}
