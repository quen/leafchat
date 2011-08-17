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
package leafchat.startup;

import java.util.Calendar;

import util.TimeUtils;
import leafchat.core.api.*;

/**
 * Minute message owner that looks after per-minute messages.
 */
public class MinuteMsgOwner extends BasicMsgOwner implements Runnable
{
	MinuteMsgOwner()
	{
		run();
	}

	void setUIReady()
	{
		uiReady=true;
	}

	private boolean uiReady=false;

	private MinuteMsg last=null;

	@Override
	public void run()
	{
		Calendar c=Calendar.getInstance();
		c.setTimeInMillis(System.currentTimeMillis());
		int hour=c.get(Calendar.HOUR_OF_DAY);
		int minute=c.get(Calendar.MINUTE);

		// Don't send event first time, or if time is the same
		MinuteMsg msg=new MinuteMsg(hour,minute);
		if(last!=null && (last.getHour()!=hour || last.getMinute()!=minute) && uiReady)
		{
			getDispatch().dispatchMessage(msg,false);
		}
		last=msg;

		// Number of milliseconds to next minute
		long nextMinute=(59-c.get(Calendar.SECOND))*1000+(1000-c.get(Calendar.MILLISECOND));
		TimeUtils.addTimedEvent(this,nextMinute,true);
	}

	@Override
	public String getFriendlyName()
	{
		return "Messages sent every minute";
	}

	@Override
	public Class<? extends Msg> getMessageClass()
	{
		return MinuteMsg.class;
	}
}
