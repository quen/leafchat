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
package com.leafdigital.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;

import org.w3c.dom.Document;

import textlayout.LayoutException;
import textlayout.stylesheet.Stylesheet;
import util.*;
import util.xml.*;

import com.leafdigital.ui.api.Theme;

import leafchat.core.api.BugException;

/**
 * Implementation of {@link Theme} interface.
 */
public class ThemeImp implements Theme, Comparable<ThemeImp>
{
	/** Parent theme, if any */
	private ThemeImp parent;

	/** File that was used to create this theme */
	private File f;

	/** XML document */
	private Document d;

	/** Stylesheet if included */
	private Stylesheet ss=null;

	private Map<String, BufferedImage> imageCache =
		new HashMap<String, BufferedImage>();

	void setParent(ThemeImp parent)
	{
		this.parent=parent;
	}

	@Override
	public int compareTo(ThemeImp other)
	{
		if(equals(other)) return 0;
		int names=
			getStringProperty(Theme.META,Theme.META_NAME,"?").compareToIgnoreCase(
				other.getStringProperty(Theme.META,Theme.META_NAME,"?"));
		if(names!=0) return names;
		return f.compareTo(other.f);
	}

	@Override
	public boolean equals(Object o)
	{
		return f.equals(((ThemeImp)o).f);
	}

	@Override
	public int hashCode()
	{
		return f.hashCode();
	}

	ThemeImp(File f) throws IOException
	{
		this.f=f;

		getData("theme.xml",new ZipDataHandler()
		{
			@Override
			public void handle(InputStream is) throws IOException
			{
				try
				{
					d=XML.parse(is);
				}
				catch(XMLException e)
				{
					IOException ioe=new IOException(e.getMessage());
					ioe.initCause(e);
					throw ioe;
				}
			}
		},false,false);

		getData("theme.css",new ZipDataHandler()
		{
			@Override
			public void handle(InputStream is) throws IOException
			{
				try
				{
					ss=new Stylesheet(is);
				}
				catch(LayoutException le)
				{
					IOException ioe=new IOException(le.getMessage());
					ioe.initCause(le);
					throw ioe;
				}
			}
		},true,false);
	}

	private interface ZipDataHandler
	{
		public void handle(InputStream is) throws IOException;
	}

	private void getData(String sFile,ZipDataHandler zdh,boolean allowEmpty,boolean useParents) throws IOException
	{
		ZipFile zf=new ZipFile(f);
		try
		{
			ZipEntry ze=zf.getEntry(sFile);
			if(ze==null)
			{
				if(useParents && parent!=null)
				{
					parent.getData(sFile,zdh,allowEmpty,useParents);
					return;
				}
				if(allowEmpty)
					return;
				else
					throw new IOException("No such entry in theme zip: "+sFile);
			}
			zdh.handle(zf.getInputStream(ze));
		}
		finally
		{
			zf.close();
		}
	}

	File getFile()
	{
		return f;
	}

	@Override
	public BufferedImage getImageProperty(String themeType, String property,
		boolean transparency, Class<?> defaultReference, String defaultFilename)
	{
		String key=(themeType==null ? "direct\n" : themeType)+"\n"+property;
		BufferedImage bi=imageCache.get(key);
		if(bi!=null)
			return bi;

		String filename;
		if(themeType==null)
		{
			filename=property;
		}
		else
		{
			filename=getStringProperty(themeType,property,null);
		}
		if(filename==null)
		{
			if(defaultReference!=null && defaultFilename!=null)
			{
				try
				{
					bi=ensureBuffered(
						GraphicsUtils.loadImage(defaultReference.getResource(defaultFilename)),
						transparency);
				}
				catch(IOException e)
				{
					throw new BugException(e);
				}
				imageCache.put(key,bi);
				return bi;
			}
			return null;
		}
		final String val=filename;

		final List<Image> result = new LinkedList<Image>();
		try
		{
			getData(val,new ZipDataHandler()
			{
				@Override
				public void handle(InputStream is) throws IOException
				{
					result.add(GraphicsUtils.loadImage(is,val));
				}
			},true,true);

			if(result.isEmpty())
				return null;

			Image i=result.get(0);
			bi=ensureBuffered(i,transparency);
			imageCache.put(key,bi);
			return bi;
		}
		catch(IOException ioe)
		{
			if(parent!=null && themeType!=null)
				return parent.getImageProperty(themeType,property,transparency,null,null);
			else
				return null;
		}
	}

	private BufferedImage ensureBuffered(Image i,boolean transparency)
	{
		BufferedImage bi=null;
		if(i instanceof BufferedImage)
			bi=(BufferedImage)i;
		if(bi==null || (!transparency && bi.getType()!=BufferedImage.TYPE_INT_RGB))
		{
			bi=new BufferedImage(i.getWidth(null),i.getHeight(null),BufferedImage.TYPE_INT_ARGB);
			bi.createGraphics().drawImage(i,0,0,null);
		}
		return bi;
	}

	@Override
	public int getIntProperty(String themeType,String property,int def)
	{
		String val=getStringProperty(themeType,property,null);
		if(val==null)
			return def;
		try
		{
			return Integer.parseInt(val);
		}
		catch(NumberFormatException nfe)
		{
			return def;
		}
	}

	@Override
	public boolean getBooleanProperty(String themeType,String property,boolean def)
	{
		String val=getStringProperty(themeType,property,null);
		return "y".equals(val) ? true : "n".equals(val) ? false : def;
	}

	@Override
	public String getStringProperty(String themeType,String property,String def)
	{
		try
		{
			return XML.getChildText(XML.getChild(d.getDocumentElement(),themeType),property);
		}
		catch(XMLException e)
		{
			if(parent!=null)
				return parent.getStringProperty(themeType,property,def);
			else
				return def;
		}
	}

	@Override
	public Stylesheet[] getStylesheets()
	{
		LinkedList<Stylesheet> found = new LinkedList<Stylesheet>();
		ThemeImp current=this;
		while(current!=null)
		{
			if(current.ss!=null) found.addFirst(current.ss);
			current=current.parent;
		}
		return found.toArray(new Stylesheet[found.size()]);
	}

	@Override
	public File getLocation()
	{
		return f;
	}
}
