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
 * Message sent when a plugin is being unloaded. Sent just after the plugin's
 * close method has been called.
 */
public class PluginUnloadMsg extends Msg
{
	private Plugin p;

	/**
	 * @param p Plugin that was unloaded
	 */
	public PluginUnloadMsg(Plugin p)
	{
		this.p=p;
	}

	/**
	 * @return Plugin that has just been closed
	 */
	public Plugin getPlugin()
	{
		return p;
	}
}
