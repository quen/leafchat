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

/** Implement this in factory classes */
public interface Factory
{
	/**
	 * Requests factory to create a new object implementing the given interface.
	 * (If a factory only implements one interface, it can ignore the parameter.)
	 * @param objectInterface Interface desired
	 * @return New object
	 * @throws GeneralException Factory may throw this exception if necessary
	 */
	public<C extends FactoryObject> C newInstance(Class<C> objectInterface)
		throws GeneralException;
}
