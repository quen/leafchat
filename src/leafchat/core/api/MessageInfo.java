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

import java.lang.reflect.*;
import java.util.*;

/**
 * Provides information about a particular class of Msg. Subclasses of this
 * should be provided by each message class. This information is used to
 * present a view of the message hierarchy for the scripting user interface.
 * To provide this for a message class, create a method - public static MessageInfo
 * getInfo() - in the class.
 * <p>
 * Note: This implements {@link Comparable}<Object> for compatibility reasons;
 * implementing {@link Comparable}<MessageInfo> would remove binary
 * compatibility with earlier versions.
 */
public class MessageInfo implements Comparable<Object>
{
	private Class<? extends Msg> c;
	private String name;
	private String description;

	private MessageInfo superclass;
	private TreeSet<MessageInfo> subclasses=new TreeSet<MessageInfo>();

	private Variables v=null;

	/** Class representing the list of available scripting variables */
	public class Variables
	{
		/**
		 * Add a variable to the list. If the variable is called 'frog' then
		 * its type will be determined by the getFrog() method in the message,
		 * which will also be used to define the variable.
		 * @param variable Variable name
		 * @throws BugException If the method isn't properly defined
		 */
		public void add(String variable)
		{
			String methodName="get"+
				variable.substring(0,1).toUpperCase()+
				variable.substring(1);

			Class<?> type;
			try
			{
				Method m=c.getMethod(methodName);
				if(m.getReturnType()!=int.class && m.getReturnType()!=String.class)
					throw new BugException("Scripting variable method must return int or String: "+methodName);
				if(!Modifier.isPublic(m.getModifiers()))
					throw new BugException("Scripting variable method must be public: "+methodName);
				type=m.getReturnType();
			}
			catch(NoSuchMethodException	e)
			{
				throw new BugException("Scripting variable method does not exist: "+methodName);
			}

			String code=(type==int.class ? "int" : "String")+" "+variable+"=msg."+methodName+"();";
			add(variable,type,code);
		}

		class VariableDetails
		{
			Class<?> type;
			String code;
			public VariableDetails(Class<?> type,String code)
			{
				this.type=type;
				this.code=code;
			}
		}

		private TreeMap<String, VariableDetails> variables =
			new TreeMap<String, VariableDetails>();

		/**
		 * Adds a custom variable to the list.
		 * @param variable Variable name
		 * @param type Type (should be int.class or String.class)
		 * @param code Code to define variable, which can assume the existance
		 *   of a msg variable of the message type, e.g. "int frog=msg.getFrog();"
		 */
		public void add(String variable,Class<?> type,String code)
		{
			variables.put(variable,new VariableDetails(type,code));
		}

		/**
		 * @return Sorted list of all variable names
		 */
		public String[] getNames()
		{
			return variables.keySet().toArray(new String[variables.keySet().size()]);
		}

		/**
		 * Obtains the code needed to define a variable, for example:
		 * "String myVariable=msg.getMyVariable();"
		 * @param variable Variable
		 * @return Code to define variable
		 */
		public String getDefinition(String variable)
		{
			VariableDetails v=variables.get(variable);
			return v.code;
		}

		/**
		 * Obtains the type of a named variable. Default is to look for the get method
		 * (as per getVariableType) and check its return value.
		 * @param variable Variablename
		 * @return Type of variable (should be either int.class or String.class)
		 * @throws BugException If variable method does not exist or there's something
		 *   wrong with it
		 */
		public Class<?> getType(String variable)
		{
			VariableDetails v=variables.get(variable);
			return v.type;
		}

		/**
		 * Removes a variable from the list.
		 * @param variable Variable to remove
		 */
		public void remove(String variable)
		{
			variables.remove(variable);
		}
	}

	/**
	 * Constructs with a particular message class and a default name (the
	 * class name without Msg).
	 * @param c Class of message to which this info applies
	 */
	public MessageInfo(Class<? extends Msg> c)
	{
		this(c,c.getName().replaceAll("^.*\\.","").replaceAll("Msg$",""),null);
	}

	/**
	 * @param obj Comparison object
	 * @return True if the object is a MessageInfo referring to same class
	 */
	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof MessageInfo && c==((MessageInfo)obj).c;
	}

	/**
	 * Constructs with a particular message class.
	 * @param c Class of message to which this info applies
	 * @param name Display name for message
	 * @param description Description of message
	 */
	public MessageInfo(Class<? extends Msg> c, String name, String description)
	{
		this.c=c;
		this.name=name;
		this.description=description;
	}

	/**
	 * Messages can supply code that initialises the context if needed, to be used
	 * when the message is received. Message will be in 'msg' variable. Default
	 * version just calls superclass or returns nothing.
	 * @return Java code
	 */
	public String getContextInit()
	{
		if(superclass==null) return "";
		return superclass.getContextInit();
	}

	/**
	 * @return List of scripting variables automatically provided by the message
	 */
	public Variables getVariables()
	{
		if(v==null)
		{
			v=new Variables();
			listScriptingVariables(v);
		}
		return v;
	}

	/** @return Class of message */
	public Class<? extends Msg> getMessageClass()
	{
		return c;
	}

	/** @return Display name	 */
	public final String getName()
	{
		return name;
	}

	/** @return Description for display, null if none */
	public final String getDescription()
	{
		return description;
	}

	@Override
	public int compareTo(Object obj)
	{
		MessageInfo other = (MessageInfo)obj;
		return getName().compareTo(other.getName());
	}

	/**
	 * @return True if messages of this type are never sent; default is
	 *   true if the message class is abstract
	 */
	public boolean isAbstract()
	{
		return Modifier.isAbstract(c.getModifiers());
	}

	/**
	 * @return True if this message appears in the scripting 'events' section,
	 *   false if it (and all subclasses) doesn't; default taken from superclass
	 *   (root default is false)
	 */
	public boolean allowScripting()
	{
		if(superclass==null) return false;
		return superclass.allowScripting();
	}

	/**
	 * Adds scripting variables to a list. Default just adds variables from
	 * superclass MessageInfo.
	 * @param v List to add variables to
	 */
	protected void listScriptingVariables(Variables v)
	{
		if(superclass==null)	return;
		superclass.listScriptingVariables(v);
	}

	/**
	 * @return List of message filter classes that may be appropriate for this
	 *   message type
	 */
	public final FilterInfo[] getAppropriateFilters()
	{
		List<FilterInfo> l = new LinkedList<FilterInfo>();
		listAppropriateFilters(l);
		return l.toArray(new FilterInfo[l.size()]);
	}

	/**
	 * Called by system to add a subclass to this info.
	 * @param subclass Subclass info
	 */
	public final void addSubclass(MessageInfo subclass)
	{
		subclasses.add(subclass);
	}

	/**
	 * Called by system to remove a subclass from this info.
	 * @param subclass Subclass info
	 */
	public final void removeSubclass(MessageInfo subclass)
	{
		subclasses.remove(subclass);
	}

	/**
	 * Called by system to set the superclass for this info
	 * @param superclass Superclass info
	 */
	public final void setSuperclass(MessageInfo superclass)
	{
		this.superclass=superclass;
	}

	/**
	 * Called by system to close this info; removes the info from its superclass,
	 * if any.
	 */
	public final void close()
	{
		if(superclass!=null) superclass.removeSubclass(this);
	}

	/**
	 * Adds permitted filters to a list. Default version just adds filters from
	 * superclass MessageInfo, or nothing if none.
	 * @param list List to add filters to
	 */
	protected void listAppropriateFilters(Collection<FilterInfo> list)
	{
		if(superclass==null)
		{
			return;
		}
		superclass.listAppropriateFilters(list);
	}

	/**
	 * @return List of subclasses of this message type
	 */
	public MessageInfo[] getSubclasses()
	{
		return subclasses.toArray(new MessageInfo[subclasses.size()]);
	}

	/**
	 * @return Superclass of this message type
	 */
	public MessageInfo getSuperclass()
	{
		return superclass;
	}
}
