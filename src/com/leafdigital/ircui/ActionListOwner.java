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

import java.util.*;

import com.leafdigital.ircui.api.*;
import com.leafdigital.ui.api.PopupMenu;

import leafchat.core.api.*;

/** Message owner for IRCActionListMsg. */
public class ActionListOwner extends BasicMsgOwner
{
	private PluginContext context;

	/**
	 * @param context Context
	 */
	public ActionListOwner(PluginContext context)
	{
		this.context = context;
		context.registerMessageOwner(this);
	}

	@Override
	public String getFriendlyName()
	{
		return "IRC action list query";
	}

	@Override
	public Class<? extends Msg> getMessageClass()
	{
		return IRCActionListMsg.class;
	}

	@Override
	public boolean allowExternalDispatch(Msg m)
	{
		return true;
	}

	/**
	 * Fills a popup menu with relevant options.
	 * @param m IRCActionListMsg set up with the context of the situation
	 * @param pm Menu to fill
	 */
	void fillMenu(final IRCActionListMsg m, PopupMenu pm)
	{
		// Get list of messages
		getDispatch().dispatchMessage(m,true);

		// Now sort them
		TreeSet<IRCAction> ts = new TreeSet<IRCAction>(new Comparator<IRCAction>()
		{
			@Override
			public int compare(IRCAction a1, IRCAction a2)
			{
				int i = a1.getCategory() - a2.getCategory();
				if(i != 0)
				{
					return i;
				}
				i = a1.getOrder()-a2.getOrder();
				if(i != 0)
				{
					return i;
				}
				// Arbitrary order
				return a1.hashCode() - a2.hashCode();
			}
		});
		ts.addAll(m.getIRCActions());

		// Go through result...
		int lastCategory = -1;
		for(Iterator<IRCAction> i=ts.iterator(); i.hasNext();)
		{
			final IRCAction a = i.next();

			// Do category separators
			if(a.getCategory() != lastCategory)
			{
				if(lastCategory != -1)
				{
					pm.addSeparator();
				}
				lastCategory = a.getCategory();
			}

			// Item itself
			pm.addItem(a.getName(), new Runnable()
			{
				@Override
				public void run()
				{
					a.run(m.getServer(), m.getContextChannel(), m.getContextNick(),
						m.getSelectedChannel(), m.getSelectedNicks(),
						((IRCUIPlugin)context.getPlugin()).getMessageDisplay(m.getServer()));
				}
			});
		}
	}
}
