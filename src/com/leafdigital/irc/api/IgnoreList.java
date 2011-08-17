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
 * Singleton that manages the list of ignored names. Names on this list will
 * have their messages ignored and (if the server supports it) SILENCEd too.
 */
public interface IgnoreList extends Singleton
{
	/**
	 * Adds a mask to the ignore list. (Masks are of the form nick!user@host and
	 * may include * wildcards. If you add a mask that doesn't include a !, it
	 * assumes it's a nickname. If you add a mask that's already present, nothing
	 * happens.)
	 * @param mask Mask to add
	 * @return True if mask was added, false if it already exists
	 */
	public boolean addMask(IRCUserAddress mask);

	/**
	 * Removes a mask from the ignore list. Has no effect if it isn't there.
	 * @param mask Mask to remove
	 * @return True if mask was removed, false if it wasn't there
	 */
	public boolean removeMask(IRCUserAddress mask);

	/**
	 * Returns the list of masks.
	 * @return List of all masks (in arbitrary order)
	 */
	public IRCUserAddress[] getMasks();
}
