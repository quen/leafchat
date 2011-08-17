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

import leafchat.core.PluginClassLoader;

/** Called to pass back information about progress of plugin load */
public interface PluginLoadReporter
{
	/**
	 * @param f File that could not be loaded
	 * @param ge Exception describing failure
	 */
	void reportFailure(File f, GeneralException ge);

	/**
	 * @param f File that we are now attempting to load
	 */
	void reportLoading(File f);

	/**
	 * @param pcl PluginClassLoader for plugin that could not be instantiated
	 * @param ge Exception describing failure
	 */
	void reportFailure(PluginClassLoader pcl, GeneralException ge);

	/**
	 * @param pcl Plugin that is being instantiated
	 */
	void reportInstantiating(PluginClassLoader pcl);

	/**
	 * @param pcl PluginClassLoader for plugin that could not be instantiated
	 * @param dependencies API dependencies that were not satisfied
	 */
	void reportFailure(PluginClassLoader pcl, String[] dependencies);

	/**
	 * @param sProgress Generic progress string to display
	 */
	void reportProgress(String sProgress);
}
