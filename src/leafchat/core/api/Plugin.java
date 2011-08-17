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

/** Interface that must be implemented by all Plugin main classes */
public interface Plugin
{
	/**
	 * Plugin should store context for future use (if necessary) and register
	 * itself with events in which it has interest.
	 * @param pc Context for plugin
	 * @param status Load reporter to optionally display messages in splash
	 * @throws GeneralException Any error
	 */
	void init(PluginContext pc, PluginLoadReporter status) throws GeneralException;

	/**
	 * Plugin is being closed and should free resources etc if necessary.
	 * (Importantly, if this plugin created any threads, it should ensure that
	 * they end within a few hundred milliseconds of this call.)
	 * @throws GeneralException Any error
	 */
	void close() throws GeneralException;
}
