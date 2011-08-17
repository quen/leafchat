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
package com.leafdigital.idle.api;

import leafchat.core.api.Singleton;

/**
 * Singleton that is used to indicate when the user does something that means
 * they aren't idle, from the point of view of auto-away detection.
 */
public interface Idle extends Singleton
{
	/** Reason for awakeness: command typed. */
	public final static String AWAKE_COMMAND="command";
	/** Reason for awakeness: '/away' command sent. */
	public final static String AWAKE_UNAWAY="unaway";

	/**
	 * Notifies the system that the user did something and is therefore not idle.
	 * @param action Type of action (AWAKE_xx constant)
	 */
	public void userAwake(String action);
}
