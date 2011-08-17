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
package com.leafdigital.ircui;

import com.leafdigital.irc.api.*;

import leafchat.core.api.*;

/**
 * IRC slash-commands that are implemented by the UI layer.
 */
public class UICommands
{
	private PluginContext context;
	UICommands(PluginContext context) throws GeneralException
	{
		this.context=context;
		context.requestMessages(UserCommandMsg.class,this,Msg.PRIORITY_LATE);
	}

	/**
	 * Message: user command typed.
	 * @param msg Message
	 * @throws GeneralException Any error
	 */
  public void msg(UserCommandMsg msg) throws GeneralException
  {
  		if(msg.isHandled()) return;
  		String command=msg.getCommand();

  		if("query".equals(command)) query(msg);
  }

  private void query(UserCommandMsg ucm) throws GeneralException
  {
		// ok we got it covered here
		ucm.markHandled();

		Server s=ucm.getServer();
		if(s==null)
		{
			ucm.getMessageDisplay().showError("Don't know which server to query on; " +
				"type /query in a command box associated with a particular server.");
			return;
		}

		String[] params=ucm.getParams().split(" ");
		if(params.length!=1 || params[0].length()==0)
		{
			ucm.getMessageDisplay().showError("Syntax: /query &lt;nickname>");
			return;
		}

		// See if there's an existing message window for that user
		MsgWindow mw=MsgWindow.find(s,params[0]);
		if(mw==null)
			new MsgWindow(context,s,params[0],true);
		else
			mw.getWindow().activate();
  }
}
