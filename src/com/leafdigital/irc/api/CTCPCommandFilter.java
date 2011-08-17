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
public class CTCPCommandFilter implements MessageFilter
{
	private String command;

	/**
   * @param command CTCP command (not case-sensitive)
	 */
	public CTCPCommandFilter(String command)
	{
		this.command=command;
	}

	@Override
	public boolean accept(Msg m)
	{
		if(m instanceof ChanCTCPRequestIRCMsg)
		{
			return ((ChanCTCPRequestIRCMsg)m).getCommand().equalsIgnoreCase(command);
		}
		else if(m instanceof UserCTCPRequestIRCMsg)
		{
			return ((UserCTCPRequestIRCMsg)m).getCommand().equalsIgnoreCase(command);
		}
		else if(m instanceof UserCTCPResponseIRCMsg)
		{
			return ((UserCTCPResponseIRCMsg)m).getCommand().equalsIgnoreCase(command);
		}
		else
		{
			return false;
		}
	}

	/** Scripting filter information. */
	public static FilterInfo info=new FilterInfo(CTCPCommandFilter.class,"CTCP command")
	{
		@Override
		public Parameter[] getScriptingParameters()
		{
			return new Parameter[] {new Parameter(String.class,"Command","CTCP command name e.g. PING")};
		}
	};
}
