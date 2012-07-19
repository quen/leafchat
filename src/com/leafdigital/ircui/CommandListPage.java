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

Copyright 2012 Samuel Marshall.
*/
package com.leafdigital.ircui;

import java.util.Arrays;

import com.leafdigital.irc.api.UserCommandListMsg;
import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/**
 * Preferences page for miscellaneous IRC settings.
 */
@UIHandler("prefs-commandlist")
public class CommandListPage
{
	private Page p;
	private PluginContext context;

	/** UI: Main scrollable text view */
	public TextView tvUI;

	CommandListPage(PluginContext context)
	{
		this.context = context;
		UI ui = context.getSingle(UI.class);
		p = ui.createPage("prefs-commandlist", this);
	}

	Page getPage()
	{
		return p;
	}

	/**
	 * Event: Page becomes active
	 * @throws GeneralException
	 */
	@UIAction
	public void onSet() throws GeneralException
	{
		UserCommandListMsg m = new UserCommandListMsg();
		context.dispatchExternalMessage(UserCommandListMsg.class, m, true);
		String[] commands = m.getCommands();
		Arrays.sort(commands);
		StringBuilder out = new StringBuilder();
		for(String command : commands)
		{
			out.append(m.getDescription(command));
		}
		tvUI.clear();
		tvUI.addXML(out.toString());
	}
}
