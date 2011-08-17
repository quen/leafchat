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

/** Mode change to a channel */
public class ChanModeIRCMsg extends ChanIRCMsg
{
	/** Mode change string */
	private String modes;
	/** Array of parameters */
	private String[] modeParams;
	/** Array of parsed changes */
	private ModeChange[] changes;

	/**
	 * @param source User that is in the message prefix
	 * @param channel Channel name
	 * @param modes Mode changes
	 * @param modeParams Parameters
	 * @param changes Parsed mode changes
	 */
	public ChanModeIRCMsg(IRCUserAddress source,String channel,
		String modes,String[] modeParams,ModeChange[] changes)
	{
		super(source,channel);
		this.modes=modes;
		this.modeParams=modeParams;
		this.changes=changes;
	}

	/** @return Mode change string */
	public String getModes() { return modes; }
	/** @return Parameters to modes */
	public String[] getModeParams() { return modeParams; }
	/** @return Parsed mode changes */
	public ModeChange[] getChanges() { return changes; }

	@Override
	public String toString()
	{
		StringBuffer sb=new StringBuffer(
		  super.toString()+
		  "  [ChanModeIRCMessage]\n"+
			"  Modes: "+modes+"\n");
		for(int i=0;i<modeParams.length;i++)
		{
			sb.append("  Mode param: "+modeParams[i]+"\n");
		}
		for(int i=0;i<changes.length;i++)
		{
			sb.append("  Change: "+changes[i]+"\n");
		}
		return sb.toString();
	}

	/** Represents a single mode change */
	public static class ModeChange
	{
		/** True if +, false if - */
		private boolean positive;
		/** Mode letter */
		private char mode;
		/** Parameter or null if none */
		private String param;

		/**
		 * @param mode Mode letter
		 * @param positive True if +, false if -
		 * @param param Parameter or null if none
		 */
		public ModeChange(char mode,boolean positive,String param)
		{
			this.mode=mode;
			this.positive=positive;
			this.param=param;
		}

		/** @return True if +, false if - */
		public boolean isPositive() { return positive; }
		/** @return Mode letter */
		public char getMode() { return mode; }
		/** @return Parameter or null if none */
		public String getParam() { return param; }

		@Override
		public String toString()
		{
			return (isPositive() ? "+" : "-") + getMode() +
				(getParam()==null? "" : ":"+ getParam());
		}
	}

	/** Scripting event information. */
	public static MessageInfo info=new MessageInfo(ChanModeIRCMsg.class,"Mode",
		"<para>Somebody has changed modes in a channel.</para>" +
		"<small>The information provided by this event is too complex " +
		"to be represented as simple variables. Please call msg.getChanges(); see " +
		"API documentation.</small>")
	{
	};
}
