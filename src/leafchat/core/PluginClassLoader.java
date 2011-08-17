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

import java.io.*;

import leafchat.core.api.*;

/**
 * Classloader used for plugins; adds extra features and bizarre delegation
 * to a URLClassLoader. Also stores general information about plugin.
 */
public class PluginClassLoader extends SafeJarClassLoader implements Comparable<PluginClassLoader>
{
	/** Owner, which we need for delegation */
	private PluginManager owner;

	/** Metadata from XML file */
	private PluginXMLDetails metadata;

	/** Remember name of jar file */
	private File jarFile;
	/** True if this plugin came from system folder */
	private boolean system;

	@Override
	public int compareTo(PluginClassLoader other)
	{
		if(other==this)
			return 0;
		if(system!=other.system)
			return system ? 1 : -1;
		int nameCompare=getName().compareTo(other.getName());
		if(nameCompare==0)
			return jarFile.compareTo(other.jarFile);
		else
			return nameCompare;
	}

	/**
	 * @return Metadata about the plugin
	 */
	public PluginXMLDetails getInfo()
	{
		return metadata;
	}

	/**
	 * Construct and analyse jar.
	 * @param owner Plugin manager
	 * @param jarFile Jar file to open
	 * @param sandbox True if file should be sandboxed (not actually implemented,
	 *   but still used to categorise 'system' files)
	 * @throws GeneralException Some other error
	 * @throws IOException If there's an error loading the file
	 */
	PluginClassLoader(PluginManager owner, File jarFile, boolean sandbox)
		throws GeneralException, IOException
	{
		// Construct as standard Java classloader based on this file
		super(jarFile, PluginClassLoader.class.getClassLoader());

		// Remember details
		this.owner=owner;
		this.jarFile=jarFile;
		system=!sandbox;

		// Find plugininfo.xml and classes for export
		boolean foundInfo = false;
		String[] entries = getEntryNames();
		for(int i=0; i<entries.length; i++)
		{
			String name = entries[i];
			if(name.endsWith("/plugininfo.xml") || name.equals("plugininfo.xml"))
			{
				if(foundInfo)
				{
					throw new GeneralException(
						"Plugin contains multiple plugininfo.xml files: "
						+ jarFile.getName());
				}

				metadata=new PluginXMLDetails(
					new ByteArrayInputStream(getEntry(name)), jarFile, system);
				foundInfo=true;
			}
			if(name.endsWith(".class"))
			{
				// Chop .class and fix into package
				String sClassName=
					name.substring(0,name.lastIndexOf('.')).replace('/','.');

				int iLastDot=sClassName.lastIndexOf('.');
				if(iLastDot!=-1)
				{
					String sPackage=sClassName.substring(0,iLastDot);
					if(sPackage.endsWith(".api"))
					{
						owner.addAPIClass(this,sClassName);
					}
				}
			}
		}
	}

	/**
	 * Find the given class; if it's a .api class, looks elsewhere as well
	 * @param name Class name
	 * @return Class
	 * @throws ClassNotFoundException
	 */
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException
	{
		try
		{
			return super.findClass(name);
		}
		catch(ClassNotFoundException e)
		{
			return owner.findAPIClass(name);
		}
	}

	/**
	 * Finds a class which we know is in this classloader and might already have
	 * been loaded.
	 * @param name Name of class
	 * @return Class object
	 * @throws ClassNotFoundException
	 */
	Class<?> getAPIClass(String name) throws ClassNotFoundException
	{
		Class<?> c=super.findLoadedClass(name);
		if(c!=null) return c;
		return super.findClass(name);
	}

	/**
	 * @return Nice display name including jar name
	 */
	@Override
	public String toString()
	{
		return "PluginClassLoader: "+jarFile.getName();
	}

	/** @return Name of jar file */
	public String getJarName()
	{
		return jarFile.getName();
	}

	/**
	 * Create actual plugin objects.
	 * @return New plugin objects
	 * @throws GeneralException
	 */
	Plugin[] createPlugins() throws GeneralException
	{
		String[] pluginClasses=metadata.getPluginClasses();
		Plugin[] ap=new Plugin[pluginClasses.length];
		for(int i=0;i<ap.length;i++)
		{
			try
			{
				Class<?> c=loadClass(pluginClasses[i]);
				ap[i]=(Plugin)c.newInstance();
			}
			catch(ClassCastException cce)
			{
				throw new GeneralException(
  				"Plugin instantiation: Class "+pluginClasses[i]+
					" must implement Plugin interface",cce);
			}
			catch (ClassNotFoundException cnfe)
			{
				throw new GeneralException(
					"Plugin instantiation: Class "+pluginClasses[i]+" not found",cnfe);
			}
			catch (InstantiationException ie)
			{
				throw new GeneralException(
				  "Plugin instantiation: Class "+pluginClasses[i]+
					" could not be instantiated",ie);
			}
			catch (IllegalAccessException iae)
			{
				throw new GeneralException(
					"Plugin instantiation: Class "+pluginClasses[i]+
					" could not be accessed (check public)",iae);
			}
		}
		return ap;
	}

	/**
	 * @return True if this is a system plugin
	 */
	public boolean isSystem()
	{
		return system;
	}

	/**
	 * @return Actual jar file used for this plugin
	 */
	public File getJar()
	{
		return jarFile;
	}

	/**
	 * @return Name of this plugin
	 */
	public String getName()
	{
		return metadata.getName();
	}
}

