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

import leafchat.core.api.Singleton;

/**
 * Singleton holding references to all current server connections. Can be
 * used to create a new server connection or obtain the list of current ones.
 */
public interface Connections extends Singleton
{
	/**
	 * Creates a new Server object. The server is not immediately connected; it's
	 * up to the caller to connect it. Until it is connected it will not appear
	 * in the lists of connected servers.
	 * @return Newly-created Server object
	 */
	public Server newServer();

	/**
	 * Obtains the currently-connected server of the given index. Indices start from
	 * 1 because these are user-typed.
	 * @param index Index (beginning from 1)
	 * @return Server object
	 * @throws ArrayIndexOutOfBoundsException If there is no server of that index
	 */
	public Server getNumbered(int index) throws ArrayIndexOutOfBoundsException;

	/**
	 * Returns array of connected servers.
	 * @return Server array (zero-length if not connected)
	 */
	public Server[] getConnected();

	/**
	 * Sets up a link that the plugin can use to get a default MessageDisplay
	 * for the given server.
	 * @param dmd Default message display provider
	 */
	public void setDefaultMessageDisplay(DefaultMessageDisplay dmd);
}
