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

import leafchat.core.api.MessageInfo;

/** Silence information */
public class SilenceIRCMsg extends UserSourceIRCMsg
{
	/** True if mask is being added */
	private boolean positive;
	/** Mask */
	private String mask;

	/**
	 * @param source Source user
	 * @param positive True if mask is added (false if removed)
	 * @param mask User mask being silenced/unsilenced
	 */
	public SilenceIRCMsg(IRCUserAddress source,boolean positive,String mask)
	{
		super(source);
		this.mask=mask;
		this.positive=positive;
	}

	/** @return Mask being silenced/unsilenced */
	public String getMask() { return mask; }

	/** @return True if mask is being added, false if it's being removed */
	public boolean isPositive() { return positive; }

	@Override
	public String toString()
	{
		return super.toString()+
		  "  [SilenceIRCMessage]\n"+
			"  "+(isPositive()?"+":"-")+getMask()+"\n";
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(SilenceIRCMsg.class)
	{
		@Override
		public boolean allowScripting()
		{
			return false;
		}
	};

}
