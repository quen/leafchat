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

import java.io.File;

/**
 * Provides a list of all plugins for information and can save a jar file
 * that includes selected plugins for use by plugin developers.
 */
public interface PluginList extends Singleton
{
	/** @return Array of all plugins installed in the system */
	PluginInfo[] getPluginList();

	/** @return leafChat core jar file */
	File getCoreJar();

	/**
	 * Loads a plugin.
	 * @param f File to load
	 * @return Loaded plugin
	 * @throws GeneralException If anything went wrong
	 */
	public PluginInfo loadPluginFile(File f) throws GeneralException;

	/**
	 * Unloads a plugin.
	 * @param plugin Plugin to unload
	 * @throws GeneralException If any error occurs
	 */
	public void unloadPluginFile(PluginInfo plugin) throws GeneralException;


	/**
	 * Saves a jar file containing all API classes.
	 * @param packages Array of plugin packages to include or null to include
	 *   all plugin exports.
	 * @param target Target jar file (will be overwritten if it exists)
	 * @throws GeneralException If there are any file access errors etc.
	 */
	void saveAPIJar(String[] packages,File target) throws GeneralException;
}
