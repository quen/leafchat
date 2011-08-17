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
package com.leafdigital.prefsui.api;

import com.leafdigital.ui.api.Page;

import leafchat.core.api.*;

/**
 * Singleton that allows you to add/remove pages from the preferences dialog
 * and the startup wizard dialog.
 */
public interface PreferencesUI extends Singleton
{
	/**
	 * Adds a page to the prefs dialog. Providing the plugin owner means you don't
	 * need to call unregisterPage when the plugin's closed.
	 * @param p Page to add
	 * @param owner Plugin that owns page
	 */
	public void registerPage(Plugin owner,Page p);

	/**
	 * Removes a page from the prefs dialog.
	 * @param p Page to remove
	 * @param owner Plugin that owns page
	 * @throws BugException if the page doesn't exist
	 */
	public void unregisterPage(Plugin owner,Page p);

	/**
	 * Registers a page for the basic wizard interface. Providing the plugin owner
	 * means you don't need to call unregisterWizardPage when the plugin's closed.
	 * @param owner Plugin that owns page
	 * @param order Integer indicating order of page. Lower numbers are first.
	 *   A gap should be left to allow insertion of other pages.
	 * @param p Page to add
	 */
	public void registerWizardPage(Plugin owner,int order,Page p);

	/**
	 * Removes a page from the prefs dialog.
	 * @param p Page to remove
	 */
	public void unregisterWizardPage(Page p);
}
