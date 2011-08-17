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
package com.leafdigital.irc.api;

import leafchat.core.api.*;

/** Filters messages based on their server numeric */
public class NumericFilter implements MessageFilter
{
	private int numeric;

	/**
   * @param numeric Numeric of interest
	 */
	public NumericFilter(int numeric)
	{
		this.numeric=numeric;
	}

	@Override
	public boolean accept(Msg m)
	{
		if(!(m instanceof NumericIRCMsg)) return false;
		return ((NumericIRCMsg)m).getNumeric()==numeric;
	}

	/** Scripting filter information. */
	public static FilterInfo info=new FilterInfo(NumericFilter.class,"Numeric")
	{
		@Override
		public Parameter[] getScriptingParameters()
		{
			return new Parameter[] {new Parameter(int.class,"Numeric","Server numeric code, usually three-digit")};
		}
	};
}
