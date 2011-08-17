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
 * Interface that a plugin can use to request system services.
 */
public interface PluginContext
{
	/** Use in place of request ID to unrequest regardless of ID */
	public final static int ALLREQUESTS=-1;

	/**
	 * Registers a new message type provided by this plugin. The
	 * fully qualified name of mo.getMessageClass() becomes the message type
	 * identifier. The specified message class will automatically have its
	 * message information registered; you need to call {@link #registerExtraMessageClass(Class)}
	 * for each subclass.
	 * @param mo Owner that will generate messages of this type
	 */
	public void registerMessageOwner(MsgOwner mo);

	/**
	 * Registers an additional message class (apart from the base type). This
	 * adds the message class's info to the global set.
	 * @param c Message subclass.
	 */
	public void registerExtraMessageClass(Class<? extends Msg> c);

	/**
	 * Requests messages of a particular type. You may call multiple
	 * times e.g. with different filters, even for the same MessageTarget.
	 * @param message Message class
	 * @param target Target that wants messages
	 * @param mf Filter for messages (may be null)
	 * @param priority Priority for message (MessageTarget.PRIORITY_xxx)
	 * @return ID of request, used in unrequest.
	 */
	public int requestMessages(Class<? extends Msg> message,
	  Object target,MessageFilter mf, int priority);

	/**
	 * Requests messages of a particular type, with no filter.
	 * @param message Message class
	 * @param target Target that wants messages
	 * @param priority Priority for message (MessageTarget.PRIORITY_xxx)
	 * @return ID of request, used in unrequest.
	 */
	public int requestMessages(Class<? extends Msg> message,
		Object target, int priority);

	/**
	 * Requests messages of a particular type, with PRIORITY_NORMAL.
	 * @param message Message class
	 * @param target Target that wants messages
	 * @param mf Filter for messages
	 * @return ID of request, used in unrequest.
	 */
	public int requestMessages(Class<? extends Msg> message,
		Object target, MessageFilter mf);

	/**
	 * Requests messages of a particular type, with no filter and PRIORITY_NORMAL.
	 * @param message Message class
	 * @param target Target that wants messages
	 * @return ID of request, used in unrequest.
	 */
	public int requestMessages(Class<? extends Msg> message, Object target);

	/**
	 * Cancels a request for messages.
	 * <p>
	 * The system will automatically unrequest all messages when your plugin
	 * closes, so there is normally no need to call this method. You call this
	 * method only if, during your plugin's lifespan, there are some messages
	 * that you only need temporarily.
	 * <p>
	 * Does nothing if the plugin did not request messages with that ID.
	 * @param message Message class (may be null for 'all')
	 * @param target Target that no longer wants messages
	 * @param requestID ID returned by original request (may be PluginContext.ALLREQUESTS)
	 */
	public void unrequestMessages(Class<? extends Msg> message, Object target,
	  int requestID);

	/**
	 * Dispatch a message that is controlled by a MessageOwner in another plugin.
	 * <p>
	 * Don't use this for your own messages; for those, use the MessageDispatch
	 * class to dispatch.
	 * <p>
	 * The MessageOwner may reject this message rather than permitting its
	 * dispatch; in that case, this call will return false.
	 * @param message Class of message
	 * @param m Message to dispatch
	 * @param immediate True if message should be sent to all targets
	 * 	immediately before this message returns; false if it should be queued
	 *  for later sending after other messages have been handled
	 * @return True if message was dispatched, false if it was prohibited
	 * @throws GeneralException If the message is of invalid type or the
	 *   MessageOwner cannot be found
	 */
	public boolean dispatchExternalMessage(Class<? extends Msg> message, Msg m,
		boolean immediate) throws GeneralException;

	/**
	 * Register an object as the singleton implementing a given interface (which
	 * should be in the .api package)
	 * @param singletonInterface Interface
	 * @param s Object implementing said interface
	 * @throws BugException If object doesn't implement interface, there's
	 *   already an implementor of that interface, etc.
	 */
	public <C extends Singleton> void registerSingleton(
		Class<C> singletonInterface, C s) throws BugException;

	/**
	 * Returns singleton implementing the desired interface.
	 * @param singletonInterface Interface
	 * @return Singleton implementing interface
	 * @throws BugException If singleton doesn't exist or is of wrong type, etc.
	 */
	public <C extends Singleton> C getSingleton2(Class<C> singletonInterface);

	/**
	 * Returns singleton implementing the desired interface.
	 * @param singletonInterface Interface
	 * @return Singleton implementing interface (it is always safe to cast this
	 *   into the interface)
	 * @throws BugException If singleton doesn't exist or is of wrong type, etc.
	 * @deprecated Replaced by {@link #getSingleton2(Class)}
	 */
	public Object getSingleton(Class<? extends Singleton> singletonInterface);

	/**
	 * Register an object as the factory creating objects of a given interface
	 * (which should be in the .api package)
	 * @param objectInterface Interface
	 * @param f Factory that can create objects of said interface
	 * @throws BugException If there's already a factory for that interface
	 */
	public void registerFactory(Class<? extends FactoryObject> objectInterface,
		Factory f) throws BugException;

	/**
	 * Get a new instance of a factory-created object.
	 * @param objectInterface Desired interface
	 * @return New object implementing cInterface
	 * @throws GeneralException If there is a problem creating the object
	 */
	public <C extends FactoryObject> C newFactoryObject(Class<C> objectInterface)
		throws GeneralException;

	/**
	 * Get a new instance of a factory-created object.
	 * @param objectInterface Desired interface
	 * @return New object implementing cInterface
	 * @throws GeneralException If there is a problem creating the object
	 * @deprecated Replaced by {@link #newFactoryObject(Class)}
	 */
	public Object newInstance(Class<? extends FactoryObject> objectInterface)
		throws GeneralException;

	/**
	 * Dispatch message to a specific target (useful for re-handling your own
	 * messages, basically just calls the right msg() method).
	 * Message will go only to that target. Dispatch occurs immediately,
	 * call returns only when dispatch completes.
	 * @param m Message
	 * @param target Target to receive it
	 * @throws GeneralException E.g. if target doesn't have an appropriate msg()
	 */
	public void dispatchMsgToTarget(Msg m, Object target) throws GeneralException;

	/**
	 * Log something to the system log. The plugin's name will automatically be
	 * prepended.
	 * @param s String to log
	 */
	public void log(String s);

	/**
	 * Log something to the system log. The plugin's name will automatically be
	 * prepended.
	 * @param s String to log
	 * @param t Exception to log (null for none)
	 */
	public void log(String s,Throwable t);

	/**
	 * Log something to the system log if debug is turned on for this plugin.
	 * The plugin's name will automatically be prepended.
	 * @param s String to log
	 */
	public void logDebug(String s);

	/**
	 * Log something to the system log if debug is turned on for this plugin.
	 * The plugin's name will automatically be prepended.
	 * @param s String to log
	 * @param t Exception to log (null for none)
	 */
	public void logDebug(String s,Throwable t);

	/**
	 * @return The Plugin that owns this context.
	 */
	public Plugin getPlugin();

	/**
	 * Runs the given runnable after all other messages are processed and the
	 * event queue is idle. (Basically this is so you don't need to call
	 * SwingUtilities.invokeLater.)
	 * @param r Task to run
	 */
	public void yield(Runnable r);

	/**
	 * Obtains information about any available message type. (If you need a list
	 * of all message classes you can start with Msg.class and follow the tree.)
	 * @param c Class to request
	 * @return Information
	 * @throws BugException If the class isn't registered
	 */
	public MessageInfo getMessageInfo(Class<? extends Msg> c) throws BugException;
}
