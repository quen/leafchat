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

import leafchat.core.api.Msg;

/** Filters messages based on their source server and channel */
public class ChanAndServerFilter extends ServerFilter
{
	private String chan;

	/**
	 * @param s Server messages must come from
	 * @param chan Channel messages must be intended for
	 */
	public ChanAndServerFilter(Server s,String chan)
	{
		super(s);
		this.chan=chan;
	}

	@Override
	public boolean accept(Msg m)
	{
		if(!super.accept(m)) return false;
		if(!(m instanceof ChanIRCMsg)) return false;
		return ((ChanIRCMsg)m).getChannel().equalsIgnoreCase(chan);
	}
}
