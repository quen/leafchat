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
package com.leafdigital.logs.api;

import leafchat.core.api.Singleton;

/** Interface for conversation/event logging. */
public interface Logger extends Singleton
{
	/**
	 * Call to log an event.
	 * @param source Source e.g. IRC server address
	 * @param category Category (CATEGORY_xx constant or custom type)
	 * @param item Item (e.g. channel name or person name)
	 * @param type Event type (e.g. message, notice, action; up to the
	 *   item what gets stored here)
	 * @param displayXML XML content that should be displayed when viewing the
	 *   log item.
	 */
	public void log(String source,String category,String item,String type,
		String displayXML);

	/** Events that occur related to a particular user */
	public final static String CATEGORY_USER="user";

	/** Events that occur on a particular channel */
	public final static String CATEGORY_CHAN="chan";
}
