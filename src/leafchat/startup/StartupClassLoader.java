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

import java.io.File;
import java.net.*;
import java.util.LinkedList;

import leafchat.core.api.BugException;

/** Classloader that will be able to find *.api as well */
public class StartupClassLoader extends URLClassLoader
{
	/** PluginManager is stored here */
	private APIClassLocator acl=null;

	/**
	 * Construct as standard URLClassLoader with main jar and any libraries
	 * @param url URL of main .jar file or main classpath
	 */
	public StartupClassLoader(URL url)
	{
		super(getJarList(url));
	}

	private static File mainJar;

	/**
	 * @return Main jar file
	 */
	public static File getMainJar()
	{
		if(StartupHandler.isIDEStartup()) return new File(
			"/Applications/leafChat.app/Contents/Resources/Java/leafChat.jar");
		if(mainJar==null)
			throw new BugException("Main jar file not set");
		return mainJar;
	}

	static void hackMainJar(File f)
	{
		mainJar=f;
	}

	private static URL[] getJarList(URL main)
	{
		LinkedList<URL> list = new LinkedList<URL>();
		list.add(main);
		try
		{
			mainJar=(new File(new URI(main.toString()))).getAbsoluteFile();
			File mainFolder=mainJar.getParentFile();
			File lib;
			if(mainFolder.toString().endsWith("/Contents/Resources/Java"))
				lib=new File(mainFolder,"../../../lib");
			else
				lib=new File(mainFolder,"lib");
			addJars(lib,list);
		}
		catch(URISyntaxException e)
		{
			// This really shouldn't happen
			e.printStackTrace();
			System.exit(1);
		}

		return list.toArray(new URL[list.size()]);
	}

	private static void addJars(File folder,LinkedList<URL> list)
	{
		File[] files=folder.listFiles();
		if(files==null) return;
		for(int i=0;i<files.length;i++)
		{
			File f=files[i];
			if(f.isDirectory())
				addJars(f,list);
			else if(f.getName().endsWith(".jar"))
			{
				try
				{
					list.add(f.toURI().toURL());
				}
				catch(MalformedURLException e)
				{
					// This really shouldn't happen
					e.printStackTrace();
					System.exit(1);
				}
			}
		}
	}

	/**
	 * Call to setup API class locator
	 * @param acl Class locator
	 */
	public void setAPIClassLocator(APIClassLocator acl)
	{
		// Note: This must be public, even though it's only called from the same
		// package. That's because it's called from StartupHandler which was loaded
		// by a different classloader.
		this.acl=acl;
	}

	/**
	 * Runs startup process.
	 */
	public void startup()
	{
		try
		{
			Class<?> c=loadClass("leafchat.startup.StartupHandler",true);
			c.newInstance();
		}
		catch(ClassNotFoundException cnfe)
		{
			cnfe.printStackTrace();
		}
		catch (InstantiationException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
	}

	/** Find the given class; if it's a .api class, looks elsewhere as well */
	@Override
	protected Class<?> findClass(String sName) throws ClassNotFoundException
	{
		try
		{
			return super.findClass(sName);
		}
		catch(ClassNotFoundException cnfe)
		{
			if(acl!=null)
			  return acl.findAPIClass(sName);
			else
				throw cnfe;
		}
	}

	/**
	 * Modify the ordering of classloading so we don't load anything from the
	 * system classloader unless we really have to
	 */
	@Override
	protected Class<?> loadClass(String name, boolean resolve)
		throws ClassNotFoundException
	{
		// Special-case the files that already have been loaded by system
		// classloader, otherwise it gets real ugly
		if(name.equals("leafchat.startup.Main") ||
			 name.equals("leafchat.startup.StartupClassLoader") ||
			 name.equals("leafchat.startup.APIClassLocator"))
		  return super.loadClass(name,resolve);

		// Modify the ordering
		Class<?> c=findLoadedClass(name);
		if(c==null)
		{
			try
			{
				c=findClass(name);
			}
			catch(ClassNotFoundException cnfe)
			{
				c=getParent().loadClass(name);
			}
		}
		if(resolve) resolveClass(c);
		return c;
	}

}
