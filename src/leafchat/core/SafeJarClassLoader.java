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
import java.util.HashMap;
import java.util.jar.*;

/**
 * Classloader that loads resources from a jar file into memory, without
 * keeping the file open.
 */
public class SafeJarClassLoader extends ClassLoader
{
	private String fileName, shortName;
	private HashMap<String, byte[]> entries = new HashMap<String, byte[]>();

	private byte[] CLASS_RELEASED = "!!!released!!!".getBytes();

	private SafeJarURLStreamHandler handler =
		new SafeJarURLStreamHandler();

	private static class SafeJarURLConnection extends URLConnection
	{
		private byte[] data;

		private SafeJarURLConnection(URL u, byte[] data)
		{
			super(u);
			this.data = data;
		}

		@Override
		public void connect() throws IOException
		{
		}

		@Override
		public InputStream getInputStream() throws IOException
		{
			return new ByteArrayInputStream(data);
		}
	}

	private class SafeJarURLStreamHandler extends URLStreamHandler
	{
		@Override
		protected URLConnection openConnection(URL u) throws IOException
		{
			// Check path and convert it into entry name
			String path = u.getPath();
			if(!path.startsWith("/" + shortName + "/"))
			{
				throw new IOException("SafeJarURLStreamHandler: invalid URL "
					+ u + " for " + shortName);
			}
			path = path.substring(shortName.length() + 2);

			// Look for entry with that name
			byte[] data = entries.get(path);
			if(data == null)
			{
				throw new IOException("SafeJarURLStreamHandler: data not found at URL "
					+ u + " for " + shortName);
			}

			return new SafeJarURLConnection(u, data);
		}
	}

	/**
	 * Constructs classloader.
	 * @param file Jar file
	 * @param parent Parent classloader
	 * @throws IOException If jar file cannot be read
	 */
	public SafeJarClassLoader(File file, ClassLoader parent) throws IOException
	{
		super(parent);

		// Remember name
		fileName = file.getAbsolutePath();
		shortName = file.getName();

		// Open jar file
		FileInputStream fileInput = new FileInputStream(file);
		try
		{
			JarInputStream jarInput = new JarInputStream(fileInput);
			byte[] buffer = new byte[65536];

			// Read all entries into RAM
			while(true)
			{
				JarEntry entry = jarInput.getNextJarEntry();
				if(entry == null)
				{
					break;
				}

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				while(true)
				{
					int read = jarInput.read(buffer);
					if(read == -1)
					{
						break;
					}
					out.write(buffer, 0, read);
				}

				entries.put(entry.getName(), out.toByteArray());
			}
			jarInput.close();
		}
		finally
		{
			fileInput.close(); // Does nothing if jar was closed OK above
		}
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException
	{
		// Get filename
		String entryName = name.replace('.', '/') + ".class";

		// Find file data
		byte[] data = entries.get(entryName);
		if(data == null)
		{
			throw new ClassNotFoundException("Class not found: " + name + " in "
				+ fileName);
		}
		if(data == CLASS_RELEASED)
		{
			throw new Error("Unable to find class multiple times: " + name + " in "
				+ fileName);
		}

		// Define class, free RAM, and return
		Class<?> c = defineClass(name, data, 0, data.length);
		entries.put(entryName, CLASS_RELEASED);
		return c;
	}

	@Override
	protected URL findResource(String name)
	{
		// Find file data
		if (!entries.containsKey(name))
		{
			return null;
		}
		try
		{
			return new URL("safejar", "", 0, "/" + shortName + "/" + name, handler);
		}
		catch(MalformedURLException e)
		{
			throw new Error(e);
		}
	}

	/**
	 * @return Array listing names of all entries in jar
	 */
	public String[] getEntryNames()
	{
		return entries.keySet().toArray(
			new String[entries.keySet().size()]);
	}

	/**
	 * @param name Named entry
	 * @return Entry with given name, or null if none
	 */
	public byte[] getEntry(String name)
	{
		return entries.get(name);
	}
}
