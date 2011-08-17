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
package leafchat.core;

import leafchat.core.api.*;

/** Allows owner to dispatch messages */
public class MessageDispatchProvider implements MessageDispatch
{
	/** Name of message */
	private Class<? extends Msg> msgClass;

	/** Owner */
	private MessageManager manager;

	/**
	 * Constructs a provider.
	 * @param mmOwner Manager used to dispatch messages
	 * @param msgClass Class of messaegs
	 */
	public MessageDispatchProvider(MessageManager mmOwner,
		Class<? extends Msg> msgClass)
	{
		this.manager = mmOwner;
		this.msgClass = msgClass;
	}

	@Override
	public void dispatchMessage(Msg m, boolean bImmediate)
	{
		manager.dispatch(msgClass,m,bImmediate);
	}

	@Override
	public void dispatchMessageHandleErrors(Msg m,boolean bImmediate)
	{
		try
		{
			dispatchMessage(m,bImmediate);
		}
		catch(BugException be)
		{
			ErrorMsg.report("Unexpected error dispatching message",be);
		}
	}
}
