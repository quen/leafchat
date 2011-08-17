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

/**
 * Utility class that implements most of MsgOwner (assuming you are going to
 * let the system handle target registration).
 */
public abstract class BasicMsgOwner implements MsgOwner
{
	private MessageDispatch md;

	/** @return The MessageDispatch object used to send messages */
	public MessageDispatch getDispatch()
	{
		return md;
	}

	@Override
	public boolean allowExternalDispatch(Msg m)
	{
		return false;
	}

	@Override
	public void init(MessageDispatch md)
	{
		this.md=md;
	}

	@Override
	public void manualDispatch(Msg m)
	{
	}

	@Override
	public boolean registerTarget(Object target, Class<? extends Msg> message,
		MessageFilter mf, int requestID,int priority)
	{
		return true;
	}

	@Override
	public void unregisterTarget(Object target,int requestID)
	{
	}
}
