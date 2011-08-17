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
 * Interface representing the most basic version of a message filter. Message
 * dispatchers which do their own filtering (for performance) may require that
 * a more complex MessageFilter subclass be used.
 *
 * Implementing classes do not need to be in public .api packages; interfaces
 * that extend this interface normally do.
 */
public interface MessageFilter
{
	/**
	 * Simple version of message filter.
	 * @param m Message to consider
	 * @return True if message is accepted
	 */
	public boolean accept(Msg m);
}
