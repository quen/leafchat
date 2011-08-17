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
package leafchat.startup;

import java.net.*;

/**
 * Main class, responsible for launching everything else via a special
 * classloader.
 * <p>
 * The classloader is needed so that stuff in the main jar file can access
 * stuff in .api sections of plugins.
 */
public class Main
{
	/**
	 * Main method.
	 * @param args Arguments (ignored)
	 */
	public static void main(String[] args)
	{
		// Find the .jar file containing this class
		URL[] au=((URLClassLoader)Main.class.getClassLoader()).getURLs();
		for (int iURL= 0; iURL < au.length; iURL++)
		{
			// Try a classloader with just this URL and see if it has this class in
			URLClassLoader ucl=new URLClassLoader(new URL[] {au[iURL]},
			  new EmptyClassLoader());
		  try
		  {
				ucl.loadClass("leafchat.startup.Main");

				// Ok, if we got that far then this is the desired URL
				StartupClassLoader scl=new StartupClassLoader(au[iURL]);
				scl.startup();
		  }
		  catch(ClassNotFoundException cnfe)
		  {
		  }
		}
	}
}

/** Classloader that won't load any classes */
class EmptyClassLoader extends ClassLoader
{
	@Override
	protected synchronized Class<?> loadClass(String name, boolean resolve)
		throws ClassNotFoundException
	{
		if(name.equals("leafchat.startup.Main"))
			throw new ClassNotFoundException("Empty classloader");
		else return super.loadClass(name,resolve);
	}
}