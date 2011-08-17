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

import java.awt.Font;

import leafchat.core.api.*;

/**
 * Singleton to provide preferences.
 * <p>
 * Preferences are currently stored in an XML file. Owner names and preference
 * names must meet the following restrictions:
 * <p>
 * The first character must be a letter (not digit). Future characters
 * must be letters, digits, _ or -; no spaces or other special characters.
 * <p>
 * To ensure uniqueness, if you create a new category of PreferencesOwner,
 * begin the string with a word and an underline;
 * <code>Plugin_com.leafdigital.whatever</code> is how the system plugin
 * ones are generated.
 */
public interface Preferences extends Singleton
{
	/**
	 * Returns a preference group.
	 * @param owner Owner of preference (if in doubt, use getPluginOwner)
	 * @return Group that can be used to check preferences
	 * @throws BugException If owner name is invalid
	 */
	public PreferencesGroup getGroup(String owner);

	/**
	 * Returns a preference group. Shortcut for calling getPluginOwner and getGroup.
	 * @param p Plugin owning preference
	 * @return Group that can be used to check preferences
	 */
	public PreferencesGroup getGroup(Plugin p);


	// Because all preferences are strings, these to/from methods are provided
	// so that you can store other things in preferences. Obviously you can do
	// it manually if you need to store something more complicated.

	/**
	 * @param value Retrieved preference
	 * @return Value as int
	 * @throws BugException If conversion fails
	 */
	public int toInt(String value) throws BugException;

	/**
	 * @param value Integer
	 * @return String equivalent
	 */
	public String fromInt(int value);

	/**
	 * @param value Retrieved preference
	 * @return Value as font
	 * @throws BugException If conversion fails
	 */
	public Font toFont(String value) throws BugException;

	/**
	 * @param value Font to represent as string
	 * @return String equivalent of font
	 */
	public String fromFont(Font value);

	/**
	 * @param value Retrieved preference
	 * @return Value as boolean
	 * @throws BugException If conversion fails
	 */
	public boolean toBoolean(String value) throws BugException;

	/**
	 * @param value Boolean to represent as string
	 * @return String equivalent of boolean
	 */
	public String fromBoolean(boolean value);

	/**
	 * Given an arbitrary string, returns a token safe to use in preferences names.
	 * @param name String
	 * @return Safed string
	 */
	public String getSafeToken(String name);

	// These methods obtain PreferencesOwner objects representing particular things

	/**
	 * @param p Plugin
	 * @return Owner string representing a particular plugin
	 */
	public String getPluginOwner(Plugin p);

	/**
	 * @param className Plugin class name
	 * @return owner string representing a particular plugin given full class name
	 */
	public String getPluginOwner(String className);
}
