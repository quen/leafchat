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

import leafchat.core.api.*;

/** Handles startup message
 */
public class ErrorMsgOwner implements MsgOwner
{
	@Override
	public void init(MessageDispatch dispatch)
	{
	}

	@Override
	public String getFriendlyName()
	{
		return "Error messages (may contain Java exceptions)";
	}

	@Override
	public Class<? extends Msg> getMessageClass()
	{
		return ErrorMsg.class;
	}

	@Override
	public boolean registerTarget(Object target, Class<? extends Msg> msgClass,
		MessageFilter mf, int requestID, int priority)
	{
		return true;
	}

	@Override
	public void unregisterTarget(Object target, int requestID)
	{
	}

	@Override
	public void manualDispatch(Msg m)
	{
	}

	@Override
	public boolean allowExternalDispatch(Msg m)
	{
		return true;
	}
}
