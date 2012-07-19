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

Copyright 2012 Samuel Marshall.
*/
package leafchat.startup;

import java.io.File;
import java.net.*;
import java.util.LinkedList;

import util.PlatformUtils;

import leafchat.core.api.BugException;

/**
 * Classloader that will be able to find *.api as well.
 * <p>
 * IMPORTANT: This file must not reference ANY other leafChat classes. If
 * it does, those are loaded with the system classloader and not this one.
 * The only classes that are supposed to be loaded by the system classloader
 * are {@link Main}, {@link APIClassLocator}, and this class.
 */
public class StartupClassLoader extends URLClassLoader
{
	private static boolean ideStartup;

	private static File mainJar;

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

	/**
	 * Sets the IDE startup value. Called from {@link StartupHandler}.
	 * <p>
	 * NOTE: It looks like this function doesn't need to be public (same package)
	 * but it does because the other class is from a different classloader.
	 * @param ideStartup True if started up from IDE, else false
	 */
	public static void setIdeStartup(boolean ideStartup)
	{
		StartupClassLoader.ideStartup = ideStartup;
	}

	/**
	 * @return True if this was startup up from IDE and not using classloader
	 */
	public static boolean isIdeStartup()
	{
		return ideStartup;
	}

	/**
	 * When starting up from IDE or command-line, you must provide an existing
	 * installation of leafChat client which it uses for a few resources that it
	 * has to access in compiled jar form.
	 * <p>
	 * (This is mainly used when scripting, and means if you're changing
	 * scripting, you need to do a full build & install the app if you change
	 * certain things.)
	 * @return Location of template app
	 */
	public static File getIdeStartupTemplateApp()
	{
		String installed = System.getProperty("leafchat.installation");
		if(installed == null)
		{
			throw new BugException(
				"When running from IDE or command-line, you must provide the " +
				"location (root folder) of a leafChat client installation.\n" +
				"Example: -Dleafchat.installation=/Applications/leafChat.app");
		}
		File file = new File(installed);
		if(!file.exists() || !file.isDirectory())
		{
			throw new BugException(
				"leafchat.installation value points to an invalid location (" +
				installed + ").\n" +
				"Example: -Dleafchat.installation=/Applications/leafChat.app");
		}
		return file;
	}

	/**
	 * @return Main jar file
	 */
	public static File getMainJar()
	{
		// Requires the jar file, so if using IDE startup (which doesn't run from
		// jar files), this must point to a normal installation.
		if(isIdeStartup())
		{
			return new File(getIdeStartupTemplateApp(),
				(PlatformUtils.isMac() ? "Contents/Resources/Java/" : "") +
				"leafChat.jar");
		}
		if(mainJar==null)
		{
			throw new Error("Main jar file not set");
		}
		return mainJar;
	}

	/**
	 * @return Util jar file
	 */
	public static File getUtilJar()
	{
		return new File(getLibFolder(), "leafdigital/util.jar");
	}

	/**
	 * Main (core) jar files; currently the main jar plus util.jar
	 * @return Array of jar files
	 */
	public static File[] getMainJars()
	{
		return new File[]
		{
			getMainJar(),
			getUtilJar()
		};
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
			File lib = getLibFolder();
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

	/**
	 * @return Lib folder based on main jar folder
	 */
	private static File getLibFolder()
	{
		File mainFolder = getMainJar().getParentFile();
		File lib;
		if(mainFolder.toString().endsWith("/Contents/Resources/Java"))
			lib=new File(mainFolder,"../../../lib");
		else
			lib=new File(mainFolder,"lib");
		return lib;
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
