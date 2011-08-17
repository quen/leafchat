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
package leafchat.core.api;

/**
 * Metadata about a particular message filter type.
 */
public class FilterInfo
{
	private Class<? extends MessageFilter> c;
	private String name;

	/**
	 * Constructs with a particular filter class and a default name (the
	 * class name without Filter).
	 * @param c Class of filter to which this info applies
	 */
	public FilterInfo(Class<? extends MessageFilter> c)
	{
		this(c,c.getName().replaceAll("^.*\\.","").replaceAll("Filter$",""));
	}

	/**
	 * @param obj Comparison object
	 * @return True if the object is a FilterInfo referring to same class
	 */
	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof FilterInfo && c==((FilterInfo)obj).c;
	}

	/**
	 * Constructs with a particular filter class.
	 * @param c Class of filter to which this info applies
	 * @param name Display name for filter
	 */
	public FilterInfo(Class<? extends MessageFilter> c, String name)
	{
		this.c=c;
		this.name=name;
	}

	/** @return Class of filter */
	public Class<? extends MessageFilter> getFilterClass()
	{
		return c;
	}

	/** @return Display name	 */
	public final String getName()
	{
		return name;
	}

	/** Stores information about a parameter to the filter constructor. */
	public static class Parameter
	{
		private Class<?> type;
		private String name,description;
		/**
		 * @param type Type of parameter
		 * @param name Name for display to user in scripting UI
		 * @param description Additional description to display below field (null if none)
		 */
		public Parameter(Class<?> type, String name, String description)
		{
			this.type=type;
			this.name=name;
			this.description=description;
		}
		/** @return Name for display to user in scripting UI	 */
		public String getName()
		{
			return name;
		}
		/** @return Additional description to display below field (null if none) */
		public String getDescription()
		{
			return description;
		}
		/** @return Type of parameter */
		public Class<?> getType()
		{
			return type;
		}
	}

	/**
	 * @return A list of parameters for the constructor to use when scripting,
	 * or null if the filter does not support scripting.
	 */
	public Parameter[] getScriptingParameters()
	{
		return null;
	}

}
