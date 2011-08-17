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

/** Filters messages based on their source server */
public class ServerFilter implements MessageFilter
{
	private Server s;

	/**
	 * @param s Filter accepts all messages from this server
	 */
	public ServerFilter(Server s)
	{
		this.s=s;
	}

	@Override
	public boolean accept(Msg m)
	{
		if(m instanceof ServerMsg)
		{
			return ((ServerMsg)m).getServer()==s;
		}
		else if(m instanceof IRCMsg)
		{
			return ((IRCMsg)m).getServer()==s;
		}
		return false;
	}

	/** Scripting filter information. */
	public static FilterInfo info=new FilterInfo(ServerFilter.class)
	{
	};
}
