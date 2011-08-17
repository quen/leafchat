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

/** Filters messages based on their source server and channel */
public class ChanFilter implements MessageFilter
{
	private String chan;

	/**
   * @param chan Channel messages must be intended for
	 */
	public ChanFilter(String chan)
	{
		this.chan=chan;
	}

	@Override
	public boolean accept(Msg m)
	{
		if(!(m instanceof ChanIRCMsg)) return false;
		return ((ChanIRCMsg)m).getChannel().equalsIgnoreCase(chan);
	}

	/** Scripting filter information. */
	public static FilterInfo info = new FilterInfo(ChanFilter.class,"Channel")
	{
		@Override
		public Parameter[] getScriptingParameters()
		{
			return new Parameter[] {new Parameter(String.class,"Channel","Channel name including #")};
		}
	};
}
