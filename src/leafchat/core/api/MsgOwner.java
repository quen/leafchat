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
 * Implemented by classes that dispatch messages.
 * <p>
 * This class does not need to be in a public .api package.
 */
public interface MsgOwner
{
	/**
	 * Called once the dispatcher is registered.
	 * @param md Provided by the system to handle message dispatch from this
	 *   dispatcher
	 */
	public void init(MessageDispatch md);

	/**
	 * Return a friendly name (for display to users) of the message, which is
	 * not used for other purposes.
	 * @return Friendly name
	 */
	public String getFriendlyName();

	/**
	 * Return the public class of messages dispatched by this object. This should
	 * normally be an interface or simple object, and must reside in a .api package.
	 * If the owner dispatches multiple messages, this should be the base class.
	 * @return Class of messages
	 */
	public Class<? extends Msg> getMessageClass();

	/**
	 * Called when a new target registers to receive messages from this class.
	 * @param target Target
	 * @param message Class of desired message (null = all)
	 * @param mf Filter (may be null)
	 * @param requestID ID associated with this request (used in unregister)
	 * @param priority Priority for message
	 * @return True if the system should automatically dispatch messages to
	 *   this target; false if this MessageOwner will dispatch
	 *   messages itself
	 */
	public boolean registerTarget(Object target, Class<? extends Msg> message,
		MessageFilter mf, int requestID, int priority);

	/**
	 * Called when a target unregisters.
	 * @param target Target
	 * @param requestID Identifier of the request to be removed
	 */
	public void unregisterTarget(Object target,int requestID);

	/**
	 * Called when the dispatcher should handle manual dispatch of a message
	 * (to all targets for which it returned false in registerTarget). Manual
	 * dispatch happens *before* any automatic dispatching.
	 * <p>
	 * It is up to the message owner to handle 'stopped' messages (m.isStopped)
	 * and not pass them on to further targets.
	 * @param m Message being sent
	 */
	public void manualDispatch(Msg m);

	/**
	 * Called if somebody tries to dispatch a message other than via the
	 * MessageDispatch class
	 * @param m Message for dispatch
	 * @return True to permit dispatch, false to prohibit
	 */
	public boolean allowExternalDispatch(Msg m);
}
