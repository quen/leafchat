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
package com.leafdigital.ui.api;

import leafchat.core.api.BugException;

/** Represents callbacks for a particular object */
public interface CallbackHandler
{
	/**
	 * Checks that the given callback function exists, is accessible, etc.
	 * @param method Name of callback method
	 * @throws BugException If the callback doesn't actually exist or isn't accessible
	 */
	public void check(String method);

	/**
	 * Checks that the given callback function exists, is accessible, etc.
	 * @param method Name of callback method
	 * @param params Required parameters
	 * @throws BugException If the callback doesn't actually exist or isn't accessible
	 */
	public void check(String method, Class<?>... params);

	/**
	 * Calls the given callback function.
	 * @param method Name of function
	 * @throws BugException If you haven't called checkCallback
	 */
	public void call(String method);

	/**
	 * Same as call, but handles errors by displaying to user.
	 * @param method Name of function
	 * @return True if function completed successfully, false if there was an error
	 *   (which has already been displayed to the user)
	 */
	public boolean callHandleErrors(String method);

	/**
	 * Calls the given callback function.
	 * @param method Name of method
	 * @param params Parameters to method
	 * @throws BugException If you haven't called checkCallback
	 */
	public void call(String method, Object... params);

	/**
	 * Same as call, but handles errors by displaying to user.
	 * @param method Name of method
	 * @param params Parameters to method
	 * @return True if function completed successfully, false if there was an error
	 *   (which has already been displayed to the user)
	 */
	public boolean callHandleErrors(String method, Object... params);
}
