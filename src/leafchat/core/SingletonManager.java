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
package leafchat.core;

import java.util.*;

import leafchat.core.api.*;

/** Provide access to all singleton classes */
public class SingletonManager
{
	// Singleton behaviour (yes, this class is a singleton; yes, that's confusing)

	/** Single instance */
	private static SingletonManager sm = new SingletonManager();

	/** @return Single instance */
	public static SingletonManager get()
	{
		return sm;
	}

	/** Private constructor to prevent separate construction */
	private SingletonManager() {}

	// Actual implementation

	/** Actual storage of singletons */
	private Map<Class<? extends Singleton>, Singleton> singletons =
		new HashMap<Class<? extends Singleton>, Singleton>();

	/**
	 * Obtains a singleton object.
	 * @param cInterface Desired class/interface
	 * @return Object of that class
	 * @throws BugException If there is no object of that class
	 */
	public<C extends Singleton> C get(Class<C> cInterface)
	{
		synchronized(singletons)
		{
			Singleton s = singletons.get(cInterface);
			if(s!=null)
			  return cInterface.cast(s);
			else
			  throw new BugException("No singleton of class "+cInterface.getName());
		}
	}

	/**
	 * Adds a new singleton.
	 * @param cInterface Class/interface by which this is referenced
	 * @param s Singleton object
	 * @throws BugException If object is not of desired interface, or there's
	 *   already a singleton for it
	 */
	public <C extends Singleton> void add(Class<C> cInterface, C s)
	{
		if(!cInterface.isAssignableFrom(s.getClass()))
		{
		  throw new BugException(
		    "Object does not implement singleton class " + cInterface.getName());
		}

		synchronized(singletons)
		{
			if(singletons.containsKey(cInterface))
			{
			  throw new BugException(
          "Cannot add second singleton of class " + cInterface.getName());
			}
			singletons.put(cInterface,s);
		}
	}

	/**
	 * Removes an existing singleton.
	 * @param cInterface Class/interface by which this is referenced
	 * @param s Object to remove
	 */
	public <C extends Singleton> void remove(Class<C> cInterface, Singleton s)
	{
		synchronized(singletons)
		{
			if(singletons.get(cInterface) == s)
			{
			  singletons.remove(cInterface);
			}
			else
			{
			  throw new BugException(
					"Attempt to remove singleton of class " + cInterface.getName() + ", "
					+ "which is not present or does not match");
			}
		}
	}
}
