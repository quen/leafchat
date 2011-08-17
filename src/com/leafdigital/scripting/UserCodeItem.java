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

import org.w3c.dom.Element;

import util.xml.*;
import leafchat.core.api.GeneralException;

/** Superclass for items that contain user code */
public abstract class UserCodeItem extends ScriptItem
{
	private String userCode;

	final static String DEFAULTCODE="/echo Enter IRC commands or Java code in this box";

	/**
	 * Constructs from XML file.
	 * @param parent Script
	 * @param e Element
	 * @param index Index in script
	 * @throws XMLException
	 * @throws GeneralException
	 */
	public UserCodeItem(Script parent,Element e,int index) throws XMLException,
		GeneralException
	{
		super(parent,e,index);
		userCode=XML.getChildText(e,"code",false);
	}

	/**
	 * Constructs blank.
	 * @param parent Script
	 * @param index Index in script
	 */
	public UserCodeItem(Script parent,int index)
	{
		super(parent,index);
		userCode=DEFAULTCODE;
	}

	@Override
	void save(Element e)
	{
		XML.setText(XML.createChild(e,"code"),userCode);
		super.save(e);
	}

	protected String getUserCode()
	{
		return userCode;
	}

	void setUserCode(String userCode)
	{
		if(!this.userCode.equals(userCode))
		{
			this.userCode=userCode;
			userCodeErrors=null;
			markChanged();
		}
	}

	protected String convertUserCode()
	{
		return super.convertUserCode(userCode);
	}

	private boolean[] userCodeErrors=null;

	boolean[] getUserCodeErrors()
	{
		return userCodeErrors;
	}

	int[] getErrorLines()
	{
		if(userCodeErrors==null) return null;

		int count=0;
		for(int i=0;i<userCodeErrors.length;i++)
		{
			if(userCodeErrors[i]) count++;
		}
		if(count==0) return null;

		int[] result=new int[count];
		int pos=0;
		for(int i=0;i<userCodeErrors.length;i++)
		{
			if(userCodeErrors[i]) result[pos++]=i;
		}
		return result;
	}

	@Override
	void clearErrors()
	{
		super.clearErrors();
		userCodeErrors=null;
	}

	@Override
	void markError(int userCodeLine,String message)
	{
		super.markError(userCodeLine,message);
		if(userCodeLine==NOTINUSERCODE)
		{
			userCodeErrors=null;
			return;
		}

		String[] lines=userCode.split("\n");
		if(userCodeErrors==null)
		{
			userCodeErrors=new boolean[lines.length];
		}

		userCodeErrors[userCodeLine]=true;

		// Error might have actually occurred on previous lines, so let's also
		// mark everything back to the previous non-blank line (hedging bets).
		for(int startLine=userCodeLine-1;startLine>=0;startLine--)
		{
			userCodeErrors[startLine]=true;
			if(!lines[startLine].matches("\\s*(//.*)?")) {
				break;
			}
		}
	}

}