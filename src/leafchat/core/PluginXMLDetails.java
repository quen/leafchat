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
import java.net.*;

import org.w3c.dom.*;

import util.xml.*;

import leafchat.core.api.*;

class PluginXMLDetails implements PluginInfo
{
	/** Metadata: plugin name */
	private String name;
	/** Metadata: plugin authors */
	private String[] authors;
	/** Metadata: plugin description */
	private String description;
	/** Metadata: version details */
	private int versionMajor,versionMinor,versionSub;
	/** Metadata: web homepage */
	private URL homepage=null;
	/** Metadata: update details */
	private URL updateJar=null,updateCheck=null;
	/** Metadata: dependencies */
	private APIDetails[] dependencies;
	/** Metadata: exports */
	private APIDetails[] exports;
	/** Metadata: plugin classes */
	private String[] pluginClasses;
	/** Metadata: debug mode */
	private boolean debug;

	private File jarFile;
	private boolean system;

	/**
	 * Loads data from the XML file.
	 * @param is Input stream for XML
	 * @param jarFile Jar file
	 * @param system True for system plugin
	 * @throws util.xml.XMLException Parse error
	 */
	PluginXMLDetails(InputStream is, File jarFile, boolean system) throws util.xml.XMLException
	{
		this.jarFile=jarFile;
		this.system=system;

		Document d=XML.parse(is);
		Element ePluginInfo=XML.getChild(d,"plugininfo");

		name=XML.getChildText(ePluginInfo,"name");
		if(name.equals("")) throw new XMLException("Must include <name>");

		authors=XML.getChildTexts(ePluginInfo,"author");
		if(authors.length==0 || authors[0].equals(""))
		  throw new XMLException("Must include <author>");

		debug=(XML.getChildren(ePluginInfo,"debug").length!=0);

		Element eVersion=XML.getChild(ePluginInfo,"version");
		try
		{
			versionMajor=Integer.parseInt(eVersion.getAttribute("major"));
			versionMinor=Integer.parseInt(eVersion.getAttribute("minor"));
			versionSub=Integer.parseInt(eVersion.getAttribute("sub"));
		}
		catch(Exception e) // NumberFormatException, NullPointerException
		{
			throw new XMLException("<version> tag invalid");
		}

		description=XML.getChildText(ePluginInfo,"description");
		if(description.equals(""))
			throw new XMLException("Must include <description>");

		Element eUpdate=null;
		try
		{
			Element eWeb=XML.getChild(ePluginInfo,"web");

			try
			{
				homepage=new URL(XML.getChildText(eWeb,"homepage"));
			}
			catch(XMLException xe) {} // <homepage> optional
			catch(MalformedURLException mue)
			{
				throw new XMLException("Invalid URL in <homepage>");
			}

			try
			{
				eUpdate=XML.getChild(eWeb,"update");
			}
			catch(XMLException xe) {} // <update> optional
		}
		catch(XMLException xe) {} // <web> optional

		if(eUpdate!=null)
		{
			try
			{
				updateJar=new URL(XML.getChildText(eUpdate,"jar"));
				updateCheck=new URL(XML.getChildText(eUpdate,"check"));
			}
			catch(MalformedURLException mue)
			{
				throw new XMLException("Invalid URL in <jar> or <check>");
			}
		}

		Element eDependencies=XML.getChild(ePluginInfo,"dependencies");
		Element[] aeAPI=XML.getChildren(eDependencies,"api");
		dependencies=new APIDetails[aeAPI.length];
		for (int iDependency= 0; iDependency < aeAPI.length; iDependency++)
		{
			dependencies[iDependency]=new APIDetails(aeAPI[iDependency],true);
		}

		Element eExports=XML.getChild(ePluginInfo,"exports");
		aeAPI=XML.getChildren(eExports,"api");
		exports=new APIDetails[aeAPI.length];
		for (int iExport= 0; iExport < aeAPI.length; iExport++)
		{
			exports[iExport]=new APIDetails(aeAPI[iExport],false);
		}

		pluginClasses=XML.getChildTexts(ePluginInfo,"class");
	}

	/** @return Friendly name of plugin */
	@Override
	public String getName()
	{
		return name;
	}

	/** @return Exported APIs */
	public APIDetails[] getExports()
	{
		return exports;
	}
	@Override
	public PluginExport[] getPluginExports()
	{
		return exports;
	}

	/** @return APIs on which this is dependent */
	public APIDetails[] getDependencies()
	{
		return dependencies;
	}

	/** @return True if plugin/s are in debug mode */
	public boolean isDebug()
	{
		return debug;
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	@Override
	public String getVersion()
	{
		return versionMajor+"."+versionMinor+"."+versionSub;
	}

	@Override
	public String[] getAuthors()
	{
		return authors;
	}

	String[] getPluginClasses()
	{
		return pluginClasses;
	}

	@Override
	public File getJar()
	{
		return jarFile;
	}

	@Override
	public boolean isSystem()
	{
		return system;
	}

	@Override
	public boolean isUserScript()
	{
		return name.equals("[User script]");
	}

	/**
	 * @return the homepage
	 */
	public URL getHomepage()
	{
		return homepage;
	}

	/**
	 * @return the updateCheck
	 */
	public URL getUpdateCheck()
	{
		return updateCheck;
	}

	/**
	 * @return the updateJar
	 */
	public URL getUpdateJar()
	{
		return updateJar;
	}
}