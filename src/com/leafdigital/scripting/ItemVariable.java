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

import java.awt.Color;
import java.util.*;

import org.w3c.dom.Element;

import util.xml.*;

import com.leafdigital.ui.api.*;

import leafchat.core.api.GeneralException;

/**
 * A variable item.
 */
@UIHandler("itemsettings.variable")
public class ItemVariable extends ScriptItem
{
	private String type;
	private String name;
	private String initial;

	private static final HashSet<String> PRIMITIVES = new HashSet<String>(
		Arrays.asList(new String[] {"boolean","byte","char","double","float","int","long","short"}));

	/**
	 * Constructs from XML.
	 * @param parent Parent script
	 * @param e XML element
	 * @param index Index in script
	 * @throws XMLException
	 * @throws GeneralException
	 */
	public ItemVariable(Script parent,Element e,int index) throws XMLException,GeneralException
	{
		super(parent,e,index);

		type=XML.getRequiredAttribute(e,"type");
		name=XML.getRequiredAttribute(e,"name");
		if(e.hasAttribute("initial"))
			initial=e.getAttribute("initial");
		else
			initial=null;
	}

	/**
	 * Constructs blank.
	 * @param parent Parent script
	 * @param index Index in script
	 */
	public ItemVariable(Script parent,int index)
	{
		super(parent,index);
		type="String";
		name="";
		initial=null;
	}

	@Override
	void save(Element e)
	{
		super.save(e);
		e.setAttribute("type",type);
		e.setAttribute("name",name);
		if(initial!=null) e.setAttribute("initial",initial);
	}

	@Override
	String getSourceFields()
	{
		StringBuffer sb=new StringBuffer(
			"\t"+type+" "+name);
		if(initial!=null)
		{
			sb.append("=");
			if(type.equals("String"))
			{
				sb.append(getQuotedString(initial));
			}
			else
			{
				sb.append(initial);
			}
		}
		else if(type.equals("String"))
		{
			sb.append("=\"\"");
		}
		sb.append(";");
		return sb.toString();
	}

	@Override
	protected String getSummaryLabel()
	{
		StringBuffer sb=new StringBuffer(type+" <key>"+name+"</key>");
		if(initial!=null)
		{
			sb.append("=");
			sb.append("<key>");
			sb.append(initial);
			sb.append("</key>");
		}
		return sb.toString();
	}

	@Override
	protected Color getNormalStripeRGB()
	{
		return new Color(128,0,128);
	}

	/** Edit box: Name */
	public EditBox nameUI;
	/** Edit box: Other type name */
	public EditBox otherUI;
	/** Edit box: Initial value */
	public EditBox initialUI;
	/** Radio button: String */
	public RadioButton typeStringUI;
	/** Radio button: int */
	public RadioButton typeIntUI;
	/** Radio button: Other */
	public RadioButton typeOtherUI;

	@Override
	protected Page getPage(Button ok)
	{
		Page p=super.getPage(ok);
		nameUI.setValue(name);
		initialUI.setValue(initial!=null?initial:"");
		if(type.equals("int"))
			typeIntUI.setSelected();
		else if(type.equals("String"))
			typeStringUI.setSelected();
		else
			typeOtherUI.setSelected();
		change();

		return p;
	}

	/**
	 * Action: Other radio button.
	 */
	@UIAction
	public void actionOther()
	{
		otherUI.focus();
		change();
	}

	/**
	 * Action: Change values.
	 */
	@UIAction
	public void change()
	{
		// Update editbox flags and OK values
		boolean ok=true;

		// Name
		{
			boolean okLocal=true;
			String currentName=nameUI.getValue();
			if(currentName.length()==0)
			{
				okLocal=false;
			}
			else
			{
				if(!Character.isJavaIdentifierStart(currentName.charAt(0)))
					okLocal=false;
				else
				{
					for(int i=1;i<currentName.length();i++)
					{
						if(!Character.isJavaIdentifierPart(currentName.charAt(i)))
							okLocal=false;
					}
				}
			}
			if(okLocal)
			{
				nameUI.setFlag(EditBox.FLAG_NORMAL);
			}
			else
			{
				ok=false;
				nameUI.setFlag(EditBox.FLAG_ERROR);
			}
		}

		// Type
		{
			boolean okLocal=true;
			if(typeOtherUI.isSelected())
			{
				// Allow primitive, primitive array, and class types
				if(!PRIMITIVES.contains(otherUI.getValue().replaceFirst("(\\[\\])*$","")))
				{
					try
					{
						Class.forName(otherUI.getValue());
					}
					catch(ClassNotFoundException e)
					{
						try
						{
							Class.forName("java.lang."+otherUI.getValue());
						}
						catch(ClassNotFoundException e2)
						{
							okLocal=false;
						}
					}
				}
			}
			if(okLocal)
			{
				otherUI.setFlag(EditBox.FLAG_NORMAL);
			}
			else
			{
				ok=false;
				otherUI.setFlag(EditBox.FLAG_ERROR);
			}
		}

		// Initial value
		{
			boolean okLocal=true;
			if(typeIntUI.isSelected() && initialUI.getValue().length()>0)
			{
				try
				{
					Integer.parseInt(initialUI.getValue());
				}
				catch(NumberFormatException e)
				{
					okLocal=false;
				}
			}
			if(okLocal)
			{
				initialUI.setFlag(EditBox.FLAG_NORMAL);
			}
			else
			{
				ok=false;
				initialUI.setFlag(EditBox.FLAG_ERROR);
			}
		}

		// Enable/disable things
		initialUI.setEnabled(!typeOtherUI.isSelected());
		otherUI.setEnabled(typeOtherUI.isSelected());
		allowOK(ok);
	}

	@Override
	protected void saveSettings()
	{
		String currentType;
		if(typeStringUI.isSelected())
			currentType="String";
		else if(typeIntUI.isSelected())
			currentType="int";
		else
			currentType=otherUI.getValue();
		if(!currentType.equals(type))
		{
			type=currentType;
			markChanged();
		}

		String currentName=nameUI.getValue();
		if(!currentName.equals(name))
		{
			name=currentName;
			markChanged();
		}

		String currentInitial=initialUI.getValue();
		if(typeOtherUI.isSelected() || currentInitial.length()==0) currentInitial=null;
		if((currentInitial==null && initial!=null) ||
			(currentInitial!=null && !currentInitial.equals(initial)))
		{
			initial=currentInitial;
			markChanged();
		}
	}

}
