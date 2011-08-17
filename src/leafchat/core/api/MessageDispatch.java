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
package leafchat.core.api;


/**
 * Provided by the system to MessageOwner classes. Used by those
 * objects to actually dispatch messages.
 */
public interface MessageDispatch
{
	/**
	 * Dispatches a message. Will first call the MessageOwner's
	 * manualDispatch method, then dispatch to all system-handled targets.
	 * @param m Message for dispatch
	 * @param immediate True if message should be sent to all targets
	 * 	immediately before this message returns; false if it should be queued
	 *  for later sending after other messages have been handled
	 * @throws BugException If the message owner has been lost
	 */
	public void dispatchMessage(Msg m, boolean immediate);

	/**
	 * Does exactly the same as dispatchMessage but without throwing exceptions.
	 * @param m Message for dispatch
	 * @param immediate True if it should be sent before return, false if it
	 *   should be queued (if in doubt, use false)
	 */
	public void dispatchMessageHandleErrors(Msg m, boolean immediate);
}
