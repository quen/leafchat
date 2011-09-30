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
package com.leafdigital.scripting;

import java.util.regex.*;

import org.w3c.dom.Element;

import com.leafdigital.ui.api.*;

import util.xml.*;
import leafchat.core.api.*;

/** Represents a single item that's part of a script */
public abstract class ScriptItem
{
	/** Parent script */
	private Script parent;

	/** Index within script */
	private int index;

	/** True if item is enabled */
	private boolean enabled;

	/** More than zero if there is an error in the item */
	private int errors;

	/** Error message text */
	private String messages;

	/**
	 * Constructs this item from XML.
	 * @param parent Owning script
	 * @param e XML element that holds all the data for this item
	 * @param index Unique index within script allocated to this item
	 * @throws XMLException
	 */
	public ScriptItem(Script parent,Element e,int index) throws XMLException
	{
		this.parent=parent;
		this.index=index;
		enabled=XML.getRequiredAttribute(e,"enabled").equals("y");
	}

	/**
	 * Constructs a new item.
	 * @param parent Owning script
	 * @param index Unique index within script allocated to this item
	 */
	public ScriptItem(Script parent,int index)
	{
		this.parent=parent;
		this.index=index;
		enabled=true;
	}

	/** @return True if item is enabled and should be compiled in */
	boolean isEnabled()
	{
		return enabled;
	}

	/** @return Unique index within script */
	int getIndex()
	{
		return index;
	}

	/**
	 * Alters the enabled state of this item.
	 * @param enabled True to enable, false to disable
	 */
	void setEnabled(boolean enabled)
	{
		if(enabled==this.enabled) return;
		this.enabled=enabled;
		markChanged();
	}

	/** Informs the parent script that this item has changed */
	protected void markChanged()
	{
		parent.markChanged();
	}

	/**
	 * Saves this item by setting up an element with its data.
	 * @param e Element (initialised with tag name only)
	 */
	void save(Element e)
	{
		e.setAttribute("enabled",enabled ? "y" : "n");
	}

	/**
	 * Called to get any required import statements for Java source. Default none.
	 * @return Source code or null if none
	 */
	String getSourceImports()
	{
		return null;
	}

	/**
	 * Called to get any required fields for Java source. Default none.
	 * @return Source code or null if none
	 */
	String getSourceFields()
	{
		return null;
	}

	/**
	 * Called to get required code for init method. Default none.
	 * @return Source code or null if none
	 */
	String getSourceInit()
	{
		return null;
	}

	/**
	 * Called to get required code for close method. Default none.
	 * @return Source code or null if none
	 */
	String getSourceClose()
	{
		return null;
	}

	/**
	 * Called to get any methods for Java source.
	 * @return Source code or null if none
	 */
	String getSourceMethods()
	{
		return null;
	}

	/**
	 * Adds user code into the source file, performing all necessary changes
	 * to that code so that it is converted into proper Java and marked up with
	 * its original line numbers.
	 * @param userCode Original code (may be multiple lines)
	 * @return Converted string
	 */
	protected String convertUserCode(String userCode)
	{
		String[] lines=userCode.split("\n");
		StringBuffer sb=new StringBuffer();
		for(int i=0;i<lines.length;i++)
		{
			sb.append("// ====v"+i+"\n");
			String line=lines[i];
			if(line.matches("^\\s*/[^/].*$"))
			{
				// IRC command
				sb.append("doCommand("+preprocessIRC(line.trim())+");\n");
			}
			else
			{
				sb.append(preprocessJava(line)+"\n");
			}
			sb.append("// ====^"+i+"\n");
		}
		return sb.toString();
	}

	/**
	 * Does preprocessing of Java code to support leafChat-specific syntax:
	 * 1. Allows singleton get to be written as [[Commands]] instead of
	 *    context.getSingleton(Commands.class)
	 * 2. There is no #2 (yet)
	 * @param line Original line
	 * @return Converted linee
	 */
	private String preprocessJava(String line)
	{
		return line.replaceAll("\\[\\[([A-Za-z0-9]+)\\]\\]","(($1)context.getSingleton($1.class))");
	}

	private static Pattern IRCLINEWITHINSERT=Pattern.compile("^(.*?)\\$\\{(.*?)\\}(.*)$");

	/**
	 * Process an IRC command line into a double-quoted Java string.
	 * @param line IRC line beginning with /
	 * @return Java quoted string beginning with and ending with "
	 */
	private String preprocessIRC(String line)
	{
		// Find any variable inserts/Java code ${}
		Matcher m=IRCLINEWITHINSERT.matcher(line);
		if(!m.matches())
		{
			// No variables. Just replace " with \" and return the line in a string
			return "\""+line.replaceAll("\"","\\\"")+"\"";
		}
		String before=m.group(1),java=m.group(2),after=m.group(3);
		return preprocessIRC(before)+"+("+preprocessJava(java)+")+"+preprocessIRC(after);
	}

	String getTag()
	{
		return getClass().getName().replaceAll("^.*Item","").toLowerCase();
	}

	protected String getTypeName()
	{
		return getClass().getName().replaceAll("^.*Item","");
	}

	/** @return XML text for the summary label on item list view */
	protected abstract String getSummaryLabel();

	protected boolean hasErrors()
	{
		return errors>0;
	}
	protected String getErrorLabel()
	{
		return errors+" error"+(errors!=1 ? "s" :"")+" "+messages;
	}

	/** @return Colour to use for striped background in editor */
	protected abstract java.awt.Color getNormalStripeRGB();

	java.awt.Color getStripeRGB()
	{
		if(errors>0)
		  return java.awt.Color.red;
		else
			return getNormalStripeRGB();
	}

	private Button okButton;

	/**
	 * Obtains an editing page for this item.
	 * @param ok Button to enable/disable when settings are (in)valid
	 * @return Page containing item settings for editing
	 */
	protected Page getPage(Button ok)
	{
		this.okButton = ok;
		UI u = parent.getContext().getSingle(UI.class);
		return u.createPage("itemsettings."
			+ getClass().getName().replaceAll("^.*Item","").toLowerCase(),
			this);
	}

	/**
	 * While the editing page is up, enables/disables the OK button.
	 * @param enable Whether to enable or disable
	 */
	protected void allowOK(boolean enable)
	{
		okButton.setEnabled(enable);
	}

	/** Saves settings on the UI for the editing page */
	protected abstract void saveSettings();

	/** Clears remembered errors within this item */
	void clearErrors()
	{
		errors=0;
		messages="";
	}

	final static int NOTINUSERCODE=-1;

	/**
	 * Marks that an error occurred in this item
	 * @param userCodeLine Line number (0-based) within user code or NOTINUSERCODE
	 * @param message Error message
	 */
	void markError(int userCodeLine,String message)
	{
		errors++;
		if(!messages.equals("")) messages+=" ";
		messages+="\u2022 "+XML.esc(message);
	}

	/** @return XML text information about variables provided by this item */
	public String getVariablesLabel()
	{
		return null;
	}

	/** @return Plugin context (for use by subclasses) */
	public PluginContext getContext()
	{
		return parent.getContext();
	}

	/**
	 * Updates index when other items are deleted
	 * @param index New index
	 */
	void setIndex(int index)
	{
		this.index=index;
	}

	/**
	 * Quotes a string for use in Java
	 * @param s String
	 * @return String surrounded in double quotes with necessary characters escaped
	 */
	protected String getQuotedString(String s)
	{
		return '"'+s.replaceAll("\\\\","\\\\").replaceAll("\\\"","\\\"")+'"';
	}
}
