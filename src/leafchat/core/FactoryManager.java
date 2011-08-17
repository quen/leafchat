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

/** Provide access to all factory objects */
public class FactoryManager
{
	// Singleton behaviour (yes, this class is a singleton; yes, that's confusing)

	/** Single instance */
	private static FactoryManager fm=new FactoryManager();

	/**
	 * @return Single instance
	 */
	public static FactoryManager get()
	{
		return fm;
	}

	/** Private constructor to prevent separate construction */
	private FactoryManager() {}

	// Actual implementation

	/** Actual storage of factories */
	private Map<Class<? extends FactoryObject>, Factory> factories =
		new HashMap<Class<? extends FactoryObject>, Factory>();

	/**
	 * Creates a new instance of the given interface.
	 * @param interfaceClass Desired interface
	 * @return New object implementing interface
	 * @throws BugException If a factory for that interface doesn't exist,
	 *   or the new object doesn't implement the interface
	 * @throws GeneralException If the factory reports an error
	 */
	public<C extends FactoryObject> C newInstance(Class<C> interfaceClass)
		throws GeneralException
	{
		Factory f;
		synchronized(factories)
		{
			f = factories.get(interfaceClass);
		}
		if(f==null)
		{
			throw new BugException("There is no factory for the desired interface "
				+ interfaceClass.getName());
		}
		C o = f.newInstance(interfaceClass);
		if(!interfaceClass.isAssignableFrom(o.getClass()))
		{
		  throw new BugException(
		    "Factory returned object that does not match interface "+
		    interfaceClass.getName()+": "+o);
		}
		return o;
	}

	/**
	 * Registers a factory.
	 * @param cInterface Interface of objects that can be created
	 * @param f Factory that will create these objects
	 * @throws BugException If a factory for this interface already exists
	 */
	public void add(Class<? extends FactoryObject> cInterface, Factory f)
	{
		synchronized(factories)
		{
			if(factories.containsKey(cInterface))
			  throw new BugException(
			    "There is already a factory for interface "+cInterface.getName());
			factories.put(cInterface,f);
		}
	}

	/**
	 * Unregisters a factory.
	 * @param cInterface Interface of objects that cannot now be created
	 * @param f Factory being removed
	 * @throws BugException If factory doesn't exist or isn't == f
	 */
	public void remove(Class<? extends FactoryObject> cInterface,Factory f)
	{
		synchronized(factories)
		{
			if(factories.get(cInterface)==f)
				factories.remove(cInterface);
			else
			  throw new BugException(
  				"There is no factory for interface "+cInterface.getName()+
					", or it doesn't match the given one for removal");
		}
	}
}
