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

import util.xml.*;

import com.leafdigital.ircui.api.IRCAction;
import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/**
 * Script item: menu option.
 */
@UIHandler("itemsettings.menu")
public class ItemMenu extends UserCodeItem
{
	private int category; // IRCAction.CATEGORY_xx constant
	private String name; // Display name

	/** Edit box: option name */
	public EditBox nameUI;
	/** Radio button: User */
	public RadioButton categoryUserUI;
	/** Radio button: Channel */
	public RadioButton categoryChanUI;
	/** Radio button: User in channel */
	public RadioButton categoryUserChanUI;

	/**
	 * Constructs from XML.
	 * @param parent Owner script
	 * @param e Element
	 * @param index Index in script
	 * @throws XMLException
	 * @throws GeneralException
	 */
	public ItemMenu(Script parent,Element e,int index) throws XMLException,GeneralException
	{
		super(parent,e,index);

		String categoryAttribute=XML.getRequiredAttribute(e,"category");
		if(categoryAttribute.equals("user")) category=IRCAction.CATEGORY_USER;
		else if(categoryAttribute.equals("userchan")) category=IRCAction.CATEGORY_USERCHAN;
		else if(categoryAttribute.equals("chan")) category=IRCAction.CATEGORY_CHAN;
		else throw new GeneralException("Script contains unexpected type= for <menu>");

		name=XML.getRequiredAttribute(e,"name");
	}

	/**
	 * Constructs empty.
	 * @param parent Owner script
	 * @param index Index in script
	 */
	public ItemMenu(Script parent,int index)
	{
		super(parent,index);
		category=IRCAction.CATEGORY_USER;
		name="";
	}

	@Override
	protected String getTypeName()
	{
		return "Menu item";
	}

	@Override
	void save(Element e)
	{
		super.save(e);
		String categoryAttribute;
		switch(category)
		{
		case IRCAction.CATEGORY_USER : categoryAttribute="user"; break;
		case IRCAction.CATEGORY_USERCHAN : categoryAttribute="userchan"; break;
		case IRCAction.CATEGORY_CHAN : categoryAttribute="chan"; break;
		default: throw new BugException("Unexpected category");
		}
		e.setAttribute("category",categoryAttribute);
		e.setAttribute("name",name);
	}

	@Override
	String getSourceInit()
	{
		return
			"\t\tcontext.requestMessages(IRCActionListMsg.class,new Item"+getIndex()+"(),\n"+
			"\t\t\tMsg.PRIORITY_NORMAL);\n";
	}

	@Override
	String getSourceMethods()
	{
		StringBuffer sb=new StringBuffer();
		sb.append(
			"\tpublic class Item"+getIndex()+"\n"+
			"\t{\n"+
			"\t\tpublic void msg(IRCActionListMsg msg)\n"+
			"\t\t{\n\t\t\t");
		switch(category)
		{
		case IRCAction.CATEGORY_USER : sb.append("if(msg.hasSingleNick())"); break;
		case IRCAction.CATEGORY_USERCHAN : sb.append("if(msg.hasChannel() && msg.hasSingleNick())"); break;
		case IRCAction.CATEGORY_CHAN : sb.append("if(msg.hasChannel() && !msg.hasSelectedNicks())"); break;
		default: throw new BugException("Unexpected category");
		}
		sb.append(
			"\n\t\t\t{\n" +
			"\t\t\t\tmsg.addIRCAction(new IRCAction()\n" +
			"\t\t\t\t{\n" +
			"\t\t\t\t\tpublic int getCategory()\n" +
			"\t\t\t\t\t{\n" +
			"\t\t\t\t\t\t");
		switch(category)
		{
		case IRCAction.CATEGORY_USER : sb.append("return IRCAction.CATEGORY_USER;"); break;
		case IRCAction.CATEGORY_USERCHAN : sb.append("return IRCAction.CATEGORY_USERCHAN;"); break;
		case IRCAction.CATEGORY_CHAN : sb.append("return IRCAction.CATEGORY_CHAN;"); break;
		default: throw new BugException("Unexpected category");
		}
		sb.append(
			"\n" +
			"\t\t\t\t\t}\n" +
			"\t\t\t\t\tpublic String getName()\n" +
			"\t\t\t\t\t{\n"+
			"\t\t\t\t\t\treturn "+getQuotedString(name)+";\n" +
			"\t\t\t\t\t}\n" +
			"\t\t\t\t\tpublic int getOrder()\n" +
			"\t\t\t\t\t{\n" +
			"\t\t\t\t\t\treturn 10000;\n" +
			"\t\t\t\t\t}\n" +
			"\t\t\t\t\tpublic void run(Server s,String contextChannel,String contextNick,String selectedChannel,String[] selectedNicks,MessageDisplay caller)\n" +
			"\t\t\t\t\t{\n"
			);

		if(category!=IRCAction.CATEGORY_CHAN) sb.append(
			"\t\t\t\t\t\tString nick=(selectedNicks!=null && selectedNicks.length==1) ? selectedNicks[0] : contextNick;\n");
		if(category!=IRCAction.CATEGORY_USER) sb.append(
			"\t\t\t\t\t\tString chan=selectedChannel!=null ? selectedChannel : contextChannel;\n");

		sb.append(
			"\t\t\t\t\t\tregisterContext(s," +
				"new IRCUserAddress((selectedNicks!=null && selectedNicks.length==1) ? selectedNicks[0] : contextNick,\"\",\"\")," +
				"selectedChannel!=null ? selectedChannel : contextChannel,caller);\n"+
			convertUserCode()+"\n"+
			"\t\t\t\t\t}\n"+
			"\t\t\t\t});\n"+
			"\t\t\t}\n"+
			"\t\t}\n"+
			"\t}\n");
		return sb.toString();
	}

	@Override
	protected String getSummaryLabel()
	{
		String categoryName;
		switch(category)
		{
		case IRCAction.CATEGORY_USER : categoryName="user"; break;
		case IRCAction.CATEGORY_USERCHAN : categoryName="user with channel"; break;
		case IRCAction.CATEGORY_CHAN : categoryName="channel"; break;
		default: throw new BugException("Unexpected category");
		}
		return "<key>"+name+"</key> ("+categoryName+")";
	}

	@Override
	public String getVariablesLabel()
	{
		StringBuffer sb=new StringBuffer();
		if(category!=IRCAction.CATEGORY_CHAN)
		{
			sb.append("String <key>nick</key>");
		}
		if(category!=IRCAction.CATEGORY_USER)
		{
			if(sb.length()!=0) sb.append(", ");
			sb.append("String <key>chan</key>");
		}
		return sb.toString();
	}

	@Override
	protected Color getNormalStripeRGB()
	{
		return new Color(0,128,128);
	}

	@Override
	protected Page getPage(Button ok)
	{
		Page p=super.getPage(ok);
		nameUI.setValue(name);
		switch(category)
		{
		case IRCAction.CATEGORY_USER : categoryUserUI.setSelected(); break;
		case IRCAction.CATEGORY_USERCHAN : categoryUserChanUI.setSelected(); break;
		case IRCAction.CATEGORY_CHAN : categoryChanUI.setSelected(); break;
		default: throw new BugException("Unexpected category");
		}
		changeName();

		return p;
	}

	/**
	 * Action: Name changed
	 */
	@UIAction
	public void changeName()
	{
		allowOK(nameUI.getFlag()==EditBox.FLAG_NORMAL);
	}

	@Override
	protected void saveSettings()
	{
		int currentCategory;
		if(categoryUserUI.isSelected())
			currentCategory=IRCAction.CATEGORY_USER;
		else if(categoryUserChanUI.isSelected())
			currentCategory=IRCAction.CATEGORY_USERCHAN;
		else
			currentCategory=IRCAction.CATEGORY_CHAN;
		if(currentCategory!=category)
		{
			category=currentCategory;
			markChanged();
		}

		String currentName=nameUI.getValue();
		if(!currentName.equals(name))
		{
			name=currentName;
			markChanged();
		}
	}

}
