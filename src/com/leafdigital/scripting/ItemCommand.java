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

import org.w3c.dom.Element;

import com.leafdigital.irc.api.UserCommandMsg;
import com.leafdigital.ui.api.*;

import util.xml.*;
import leafchat.core.api.GeneralException;

/**
 * Script item representing a /-command that the user can type directly.
 */
@UIHandler({"itemsettings.command"})
public class ItemCommand extends UserCodeItem
{
	private String command;

	private static class Param
	{
		Class<?> c;
		String name;
	}

	private Param[] params;

	/**
	 * Constructs from XML tag.
	 * @param parent Parent script
	 * @param e Element representing this item
	 * @param index Index within script
	 * @throws XMLException XML error
	 * @throws GeneralException Other error
	 */
	public ItemCommand(Script parent,Element e,int index) throws XMLException,GeneralException
	{
		super(parent,e,index);

		command=XML.getRequiredAttribute(e,"name");
		Element[] paramElements=XML.getChildren(e,"param");
		params=new Param[paramElements.length];
		for(int i=0;i<paramElements.length;i++)
		{
			params[i]=new Param();
			String type=XML.getRequiredAttribute(paramElements[i],"type");
			if(type.equals("java.lang.String"))
				params[i].c=String.class;
			else if(type.equals("int"))
				params[i].c=int.class;
			else
				throw new GeneralException("Unexpected type= parameter for command item");

			params[i].name=XML.getText(paramElements[i]);
			if(!params[i].name.matches("[A-Za-z][A-Za-z0-9_]*"))
				throw new GeneralException("Parameter name "+params[i].name+" invalid");
		}
	}

	/**
	 * Constructs blank item.
	 * @param parent Parent script
	 * @param index Index within script
	 */
	public ItemCommand(Script parent,int index)
	{
		super(parent,index);
		command="command";
		params=new Param[0];
	}

	@Override
	void save(Element e)
	{
		e.setAttribute("name",command);
		super.save(e);

		for(int i=0;i<params.length;i++)
		{
			Element param=XML.createChild(e,"param");
			XML.setText(param,params[i].name);
			param.setAttribute("type",params[i].c.getName());
		}
	}

	@Override
	String getSourceInit()
	{
		return
			"\t\tcontext.requestMessages(UserCommandMsg.class,new Item"+getIndex()+"(),\n"+
			"\t\t\tnew CommandFilter(\""+command+"\"),Msg.PRIORITY_NORMAL);\n";
	}

	@Override
	String getSourceMethods()
	{
		StringBuffer sb=new StringBuffer();
		sb.append(
			"\tpublic class Item"+getIndex()+"\n"+
			"\t{"+
			"\t\tpublic void msg(UserCommandMsg msg)\n"+
			"\t\t{\n"+
			"\t\t\tmsg.markHandled();\n"+
			"\t\t\t"+getContext().getMessageInfo(UserCommandMsg.class).getContextInit()+			"\n"+
			"\t\t\tString params=msg.getParams();\n");

		if(params.length>0)
		{
			sb.append("\t\t\tString INTERNAL_error=\"Expecting /"+command);
			for(int i=0;i<params.length;i++)
			{
				sb.append(" &lt;"+params[i].name+">");
			}
			sb.append("\";\n");

			sb.append(
				"\t\t\tString[] INTERNAL_split=params.split(\"\\\\s+\","+params.length+");\n"+
				"\t\t\tif(INTERNAL_split.length!="+params.length+")\n" +
				"\t\t\t{\n"+
				"\t\t\t\tcommandDisplay.showError(\"Not enough parameters for command. \"+INTERNAL_error);\n"+
				"\t\t\t\treturn;\n"+
				"\t\t\t}\n");

			for(int i=0;i<params.length;i++)
			{
				if(params[i].c==String.class)
				{
					sb.append(
						"\t\t\tString "+params[i].name+"=INTERNAL_split["+i+"];\n");
				}
				else if(params[i].c==int.class)
				{
					sb.append(
						"\t\t\tint "+params[i].name+";\n"+
						"\t\t\ttry\n"+
						"\t\t\t{\n"+
						"\t\t\t\t"+params[i].name+"=Integer.parseInt(INTERNAL_split["+i+"]);\n"+
						"\t\t\t}\n"+
						"\t\t\tcatch(NumberFormatException INTERNAL_e)\n"+
						"\t\t\t{\n"+
						"\t\t\t\tcommandDisplay.showError(\"Not a valid number for parameter "+
							params[i].name+". \"+INTERNAL_error);\n"+
						"\t\t\t\treturn;\n"+
						"\t\t\t}\n");
				}
			}
		}

		sb.append(
			convertUserCode()+"\n"+
			"\t\t}\n"+
			"\t}\n");
		return sb.toString();
	}

	@Override
	protected String getSummaryLabel()
	{
		StringBuffer sb=new StringBuffer("/<key>"+command+"</key>");
		for(int i=0;i<params.length;i++)
		{
			sb.append(" &lt;<key>"+params[i].name+"</key>>");
		}
		return sb.toString();
	}

	@Override
	public String getVariablesLabel()
	{
		StringBuffer sb=new StringBuffer("String <key>params</key>");
		for(int i=0;i<params.length;i++)
		{
			sb.append(", "+params[i].c.getName().replaceFirst("^.*\\.","")+" <key>"+
				params[i].name+"</key>");
		}
		return sb.toString();
	}

	@Override
	protected Color getNormalStripeRGB()
	{
		return new Color(0,128,0);
	}

	/** UI: Command edit */
	public EditBox commandUI;
	/** UI: type */
	public Dropdown[] classDropdowns;
	/** UI: name */
	public EditBox[] nameDropdowns;

	@Override
	protected Page getPage(Button ok)
	{
		Page p=super.getPage(ok);
		commandUI.setValue(command);

		for(int i=0;i<classDropdowns.length;i++)
		{
			classDropdowns[i].addValue(null,"-");
			classDropdowns[i].addValue(String.class,"String");
			classDropdowns[i].addValue(int.class,"int");

			if(params.length>i)
			{
				classDropdowns[i].setSelected(params[i].c);
				nameDropdowns[i].setValue(params[i].name);
			}
		}
		change();

		return p;
	}

	/**
	 * UI: Changed option
	 */
	@UIAction
	public void change()
	{
		boolean ok=commandUI.getFlag()==EditBox.FLAG_NORMAL;

		boolean hideAfter=false;
		for(int i=0;i<classDropdowns.length;i++)
		{
			if(hideAfter)
			{
				classDropdowns[i].setVisible(false);
				nameDropdowns[i].setVisible(false);
				continue;
			}
			classDropdowns[i].setVisible(true);
			nameDropdowns[i].setVisible(true);
			if(classDropdowns[i].getSelected()==null || nameDropdowns[i].getValue().equals(""))
			{
				hideAfter=true;
				if(classDropdowns[i].getSelected()!=null || !nameDropdowns[i].getValue().equals(""))
					ok=false;
			}
			else
			{
				if(nameDropdowns[i].getFlag()!=EditBox.FLAG_NORMAL) ok=false;
			}
		}

		allowOK(ok);
	}

	@Override
	protected void saveSettings()
	{
		String newCommand=commandUI.getValue();;
	  if(!newCommand.equals(command))
	  {
			command=newCommand;
			markChanged();
	  }
	  int count=0;
		for(;count<classDropdowns.length;count++)
		{
			if(classDropdowns[count].getSelected()==null) break;
		}
		if(count==params.length)
		{
			boolean same=true;
			for(int i=0;i<params.length;i++)
			{
				if(!(params[i].c.equals(classDropdowns[i].getSelected()) &&
					params[i].name.equals(nameDropdowns[i].getValue())))
				{
					same=false;
					break;
				}
			}
			if(same) return;
		}
		params=new Param[count];
		for(int i=0;i<params.length;i++)
		{
			params[i]=new Param();
			params[i].c=(Class<?>)classDropdowns[i].getSelected();
			params[i].name=nameDropdowns[i].getValue();
		}
		markChanged();
	}

}
