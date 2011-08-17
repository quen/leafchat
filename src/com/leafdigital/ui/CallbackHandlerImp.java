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

import java.lang.reflect.*;
import java.util.*;

import com.leafdigital.ui.api.CallbackHandler;

import leafchat.core.api.*;

/** Implements UI callbacks */
public class CallbackHandlerImp implements CallbackHandler
{
	/** Object that callbacks will happen on (null if none) */
	private Object owner;

	/** Map of String -> Method */
	private Map<String, Method> mChecked = new HashMap<String, Method>();

	Object getCallbackObject()
	{
		return owner;
	}

	/**
	 * @param owner Object that callbacks will happen on (null if none)
	 */
	public CallbackHandlerImp(Object owner)
	{
		this.owner=owner;
	}

	@Override
	public synchronized void check(String method)
	{
		check(method, new Class<?>[0]);
	}

	@Override
	public synchronized void check(String method, Class<?>... params)
	{
		if(method==null) return;

		String prefix="Checking callback "+method+"(";
		if(params!=null && params.length>0)
		{
			for(int i=0;i<params.length;i++)
			{
				if(i>0) prefix+=",";
				prefix+=params[i].getName();
			}
		}
		prefix+=") in "+owner+": ";
		if(owner==null)
			throw new BugException(prefix+"no callback object specified");

		try
		{
			Method m=owner.getClass().getMethod(method,params);
			if(!Modifier.isPublic(m.getModifiers()))
				throw new BugException(prefix+"method must be public");
			if(Modifier.isAbstract(m.getModifiers()))
				throw new BugException(prefix+"method must not be abstract");
			if(m.getReturnType()!=void.class)
				throw new BugException(prefix+"method must return void");
			mChecked.put(method,m);
		}
		catch(NoSuchMethodException e)
		{
			throw new BugException(prefix+"method not found ");
		}
	}

	@Override
	public synchronized void call(String method, Object... params)
	{
		Method m=mChecked.get(method);

		String prefix="Calling "+method+" in "+owner+": ";
		if(m==null) throw new BugException(prefix+"method not checked");

		try
		{
			m.invoke(owner, params);
		}
		catch(IllegalArgumentException e)
		{
			throw new BugException(prefix+"Unexpected error",e);
		}
		catch(IllegalAccessException e)
		{
			throw new BugException(prefix+"Unexpected error",e);
		}
		catch(InvocationTargetException e)
		{
			throw new BugException(prefix+" Method caused exception",e.getCause());
		}
	}

	@Override
	public void call(String method)
	{
		call(method, new Object[0]);
	}

	@Override
	public boolean callHandleErrors(String method)
	{
		try
		{
			call(method);
			return true;
		}
		catch(BugException be)
		{
			ErrorMsg.report(be.getMessage(),be.getCause());
			return false;
		}
	}

	@Override
	public boolean callHandleErrors(String method, Object... params)
	{
		try
		{
			call(method,params);
			return true;
		}
		catch(BugException be)
		{
			ErrorMsg.report(be.getMessage(),be.getCause());
			return false;
		}
	}

}
