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
package com.leafdigital.irc.api;

import leafchat.core.api.*;

/**
 * Singleton that manages the list of people being watched. The program uses the
 * server's WATCH facility if available, or ISON if not, to maintain the list.
 */
public interface WatchList extends Singleton
{
	/**
	 * Adds a mask to the watch list.
	 * <p>
	 * Masks are of the form nick!user@host and
	 * may include * wildcards in user and host but not in nickname. If you add
	 * a mask that doesn't include a !, it assumes it's a nickname. If you add
	 * a mask that's already present, nothing happens. For servers that don't
	 * support WATCH, the username and host are ignored.
	 * @param mask Mask to add
	 * @return True if mask was added, false if it already exists
	 */
	public boolean addMask(IRCUserAddress mask);

	/**
	 * Adds a mask to the temporary watch list (doesn't show in UI). After calling
	 * this, removeTemporaryMask must be called the same number of times.
	 * @param s Server
	 * @param mask Mask to add
	 */
	public void addTemporaryMask(Server s, IRCUserAddress mask);

	/**
	 * Removes a mask from the watch list. Has no effect if it isn't there.
	 * @param mask Mask to remove
	 * @return True if mask was removed, false if it wasn't there
	 */
	public boolean removeMask(IRCUserAddress mask);

	/**
	 * Removes a mask from the temporary watch list.
	 * @param s Server
	 * @param mask Mask to remove
	 */
	public void removeTemporaryMask(Server s, IRCUserAddress mask);

	/**
	 * Returns the list of masks.
	 * @return List of all masks (in arbitrary order)
	 */
	public IRCUserAddress[] getMasks();

	/**
	 * Checks whether we know the online status of a user. We know the status
	 * if a mask (temp or permanent) has been added and a response from the server
	 * has been received.
	 * @param s Server
	 * @param nick Nickname
	 * @return True if we know whether they're online or not
	 */
	public boolean isKnown(Server s, String nick);

	/**
	 * Checks whether a user on the watch list is online.
	 * @param s Server to look on
	 * @param nick Nick to look for
	 * @return True if they are present, false otherwise
	 */
	public boolean isOnline(Server s, String nick);
}
