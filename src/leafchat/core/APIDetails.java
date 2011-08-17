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

import java.util.Set;

import org.w3c.dom.Element;

import util.xml.*;
import leafchat.core.api.PluginExport;

/** Represents an API package, either from dependencies or exports */
public class APIDetails implements PluginExport
{
	/** Package name */
	private String sPackage;

	/** Version number(s) */
	private int[] aiVersions;

	APIDetails(Element e,boolean bRequired) throws XMLException
	{
		sPackage=XML.getChildText(e,"package");
		if(sPackage.equals("")) throw new XMLException("<api> must have <package>");

		String[] asVersions=XML.getChildTexts(e,"version");
		if(bRequired && asVersions.length!=1)
		  throw new XMLException("Required <api> must have a single <version>");
		if(asVersions.length==0)
		  throw new XMLException("<api> must have <version>");

		try
		{
			aiVersions=new int[asVersions.length];
			for(int i=0;i<aiVersions.length;i++)
			{
				aiVersions[i]=Integer.parseInt(asVersions[i]);
			}
		}
		catch(NumberFormatException nfe)
		{
			throw new XMLException("<version> is not a valid integer",nfe);
		}
	}

	int getRequiredVersion() { return aiVersions[0]; }
	@Override
	public int[] getCompatibleVersions() { return aiVersions; }
	@Override
	public String getPackage() { return sPackage; }
	@Override
	public int getMaxVersion()
	{
		int max=0;
		for(int i=0;i<aiVersions.length;i++)
		{
			max=Math.max(max,aiVersions[i]);
		}
		return max;
	}

	@Override
	public int hashCode()
	{
		int iHashCode=aiVersions.length*1000;
		for(int i=0;i<aiVersions.length;i++)
		  iHashCode+=(i+1)*aiVersions[i];
		return iHashCode+sPackage.hashCode();
	}

	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof APIDetails)) return false;
		APIDetails api=(APIDetails)o;

		if(!sPackage.equals(api.sPackage)) return false;

		if(aiVersions.length!=api.aiVersions.length) return false;
		for(int i=0;i<aiVersions.length;i++)
		  if(aiVersions[i]!=api.aiVersions[i]) return false;

		return true;
	}

	/**
	 * Add package:version strings, one for each supported version,
	 * to the set, which will be used for testing support later on.
	 * @param sSupportedAPIs Set of supported APIs
	 */
	public void addSupportStrings(Set<String> sSupportedAPIs)
	{
		for(int iVersion=0;iVersion<aiVersions.length;iVersion++)
		{
			sSupportedAPIs.add(sPackage+":"+aiVersions[iVersion]);
		}
	}

	/**
	 * @return Package:version string used for testing support
	 */
	public String getRequiredString()
	{
		return sPackage+":"+aiVersions[0];
	}
}
