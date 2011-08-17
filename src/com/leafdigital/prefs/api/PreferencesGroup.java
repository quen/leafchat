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

import leafchat.core.api.BugException;

/**
 * Represents a group of preferences.
 * <p>
 * Each group has its own set of named preferences. You can create child groups
 * that are named, or 'anonymous' numbered child groups (0, 1, ...) in an array
 * that can be rearranged. When you use anonymous groups, you can request
 * preferences in a hierarchical manner.
 */
public interface PreferencesGroup
{
	/**
	 * @return Main preferences object
	 */
	public Preferences getPreferences();

	// Actual preference access
	///////////////////////////

	/**
	 * Check whether a program preference has been set.
	 * @param name Name of preference
	 * @return True if it exists
	 */
	public boolean exists(String name);

	/**
	 * Return value of a named preference within group.
	 * @param name Name of preference
	 * @return Value of preference as string
	 * @throws BugException If the preference doesn't exist
	 */
	public String get(String name);

	/**
	 * Return value of a named preference within group.
	 * @param name Name of preference
	 * @param defaultValue Value to use for preference if it isn't found
	 * @return Value of preference as string
	 */
	public String get(String name,String defaultValue);

	/**
	 * Return value of a named preference within group or parent anonymous groups,
	 * up to the first non-anonymous ancestor. For instance, if this group is
	 * /frog/server/1/3, then this will search for the preference in this group,
	 * in /frog/server/1, and in /frog/server.
	 * @param name Name of preference
	 * @return Value of preference as string
	 * @throws BugException If the preference doesn't exist
	 */
	public String getAnonHierarchical(String name);

	/**
	 * Return value of a named preference within group or parent anonymous groups.
	 * (See {@link #getAnonHierarchical(String)} for description of procedure.
	 * @param name Name of preference
	 * @param defaultValue Value to use for preference if it isn't found
	 * @return Value of preference as string
	 */
	public String getAnonHierarchical(String name,String defaultValue);

	/**
	 * Return value of a named preference within parent anonymous groups or,
	 * optionally, this group.
	 * (See {@link #getAnonHierarchical(String)} for description of procedure.
	 * @param name Name of preference
	 * @param defaultValue Value to use for preference if it isn't found
	 * @param includeThis If true, includes this group; otherwise ignores any
	 *   setting within this group
	 * @return Value of preference as string
	 */
	public String getAnonHierarchical(String name,String defaultValue,boolean includeThis);

	/**
	 * Looks for a preferences group in all anonymous children (and their anonymous
	 * children, optionally) that contains the given preference. Case-sensitive.
	 * @param pref Name of pref
	 * @param value Value of pref
	 * @param recursive If true, follows into anonymous children of subgroups
	 * @return Group that contains this pref, or null if none
	 */
	public PreferencesGroup findAnonGroup(String pref, String value, boolean recursive);

	/**
	 * Looks for a preferences group in all anonymous children (and their anonymous
	 * children, optionally) that contains the given preference.
	 * @param pref Name of pref
	 * @param value Value of pref
	 * @param recursive If true, follows into anonymous children of subgroups
	 * @param ignoreCase If true, ignores case of 'value' parameter
	 * @return Group that contains this pref, or null if none
	 */
	public PreferencesGroup findAnonGroup(String pref, String value,
		boolean recursive, boolean ignoreCase);

	/**
	 * Sets value of a named program preference.
	 * @param name Name of preference	(be sure to follow restrictions)
	 * @param value String value of preference
	 * @return Previous value, null if none
	 * @throws BugException If name or owner didn't follow restrictions
	 */
	public String set(String name,String value);

	/**
	 * Sets value of a named program preference. If the value is equal to the
	 * default, actually unsets it.
	 * @param name Name of preference	(be sure to follow restrictions)
	 * @param value String value of preference
	 * @param defaultValue Default value of preference
	 * @throws BugException If name or owner didn't follow restrictions
	 */
	public void set(String name,String value,String defaultValue);

	/**
	 * Unsets an existing preference (exists() will return false after this).
	 * @param name Name of preference
	 * @return True if preference previously existed and was unset, false if it
	 *   didn't exist anyhow
	 */
	public boolean unset(String name);

	// Parent
	/////////

	/**
	 * @return Parent of anonymous group; null if this isn't an anonymous group.
	 */
	public PreferencesGroup getAnonParent();

	// Named child groups
	/////////////////////

	/**
	 * Returns a nested preference group. Creates the group if it doesn't already exist.
	 * @param name Name of preference group
	 * @return Group that can be used to check preferences within that name
	 * @throws BugException If name isn't valid
	 */
	public PreferencesGroup getChild(String name);

	/**
	 * Removes this group from its parent. If this is an anonymous group, other
	 * entries in the group will be shuffled along. If the group doesn't have
	 * a parent, nothing will happen.
	 */
	public void remove();

	// Anonymous child groups
	/////////////////////////

	/**
	 * @return Array of anonymous child groups (may be zero-length)
	 */
	public PreferencesGroup[] getAnon();

	/**
	 * Creates a new anonymous child group.
	 * @return New group (will be last in the array)
	 */
	public PreferencesGroup addAnon();

	/** Constant referring to last entry in index */
	public final static int ANON_LAST=-1;

	/**
	 * Adds a group from somewhere else into this anonymous array.
	 * If the group has another parent, it will automatically be removed from there.
	 * @param pg Group to add
	 * @param position Index for new group (others will be shuffled
	 *   along) or ANON_LAST for end of group
	 * @return Index of new group
	 */
	public int addAnon(PreferencesGroup pg,int position);

	/** Removes all anon children at once (to save you calling remove() on each one) */
	public void clearAnon();
}
