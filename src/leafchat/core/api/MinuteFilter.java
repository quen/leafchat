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

/** Used to receive the per-minute messages less often */
public class MinuteFilter implements MessageFilter
{
	private int period;

	/**
	 * @param period Number of minutes you want to pass between messages, e.g. 60
	 */
	public MinuteFilter(int period)
	{
		this.period=period;
	}

	@Override
	public boolean accept(Msg m)
	{
		if(!(m instanceof MinuteMsg)) return false;
		MinuteMsg msg=(MinuteMsg)m;

		int minutes=msg.getHour()*60+msg.getMinute();
		return (minutes%period==0);
	}

	/**
	 * Filter info
	 */
	public static FilterInfo info=new FilterInfo(MinuteFilter.class,"Minutes")
	{
		@Override
		public Parameter[] getScriptingParameters()
		{
			return new Parameter[] {new Parameter(int.class,"Period","E.g. set to 60 to receive event hourly")};
		}
	};
}
