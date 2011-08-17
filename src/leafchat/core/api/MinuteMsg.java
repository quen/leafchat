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
package leafchat.core.api;

import java.util.Collection;

/** Message sent every minute */
public class MinuteMsg extends Msg
{
	private int hour,minute;

	/**
	 * @param hour Hour (0-23) in local time
	 * @param minute Minute in local time
	 */
	public MinuteMsg(int hour,int minute)
	{
		this.hour=hour;
		this.minute=minute;
	}

	/** @return Hour (0-23) in local time */
	public int getHour()
	{
		return hour;
	}

	/** @return Minute (0-59) in local time */
	public int getMinute()
	{
		return minute;
	}

	/** Message info; allows scripting */
	public static MessageInfo info=new MessageInfo(MinuteMsg.class,"Timer",
		"Event generated every minute.")
	{
		@Override
		protected void listAppropriateFilters(Collection<FilterInfo> list)
		{
			super.listAppropriateFilters(list);
			list.add(MinuteFilter.info);
		}
		@Override
		protected void listScriptingVariables(Variables v)
		{
			super.listScriptingVariables(v);
			v.add("hour");
			v.add("minute");
		}
		@Override
		public boolean allowScripting()
		{
			return true;
		}
		@Override
		public String getContextInit()
		{
			return "registerContext(null,null,null,null);";
		}
	};
}
