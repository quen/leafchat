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

/** Information about a particular plugin */
public interface PluginInfo
{
	/** @return Jar file for plugin */
	public File getJar();
	/** @return Name of plugin */
	public String getName();
	/** @return Author(s) of plugin */
	public String[] getAuthors();
	/** @return Description of plugin */
	public String getDescription();
	/** @return Plugin version as a string in format 1.10.34 */
	public String getVersion();
	/** @return True if plugin is a system one */
	public boolean isSystem();
	/** @return True if plugin is a user script */
	public boolean isUserScript();
	/** @return List of exported API packages */
	public PluginExport[] getPluginExports();
}
