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

import java.lang.reflect.*;
import java.util.*;

import javax.swing.SwingUtilities;

import util.ReflectionUtils;
import leafchat.core.api.*;

/** Provides system behaviour for message dispatching etc. */
public class MessageManager
{
	// Handle singleton behaviour (note: as this is not accessible to plugins,
	// it doesn't use the SingletonManager features)

	/** Single instance */
	private static MessageManager mm=new MessageManager();

	/** @return Singleton instance */
	public static MessageManager get() { return mm; }

	/** Private constructor to prevent separate construction */
	private MessageManager()
	{
		registerMessageClass(Msg.class);
	}

	// Actual implementation

	/** Map of message Class -> OwnerDetails */
	private Map<Class<? extends Msg>, OwnerDetails> owners =
		new HashMap<Class<? extends Msg>, OwnerDetails>();

	/** Map of Class(target) -> (Map of Class(message) -> Method) */
	private Map<Class<?>, Map<Class<? extends Msg>, Method>> classHandlerMethods =
		new HashMap<Class<?>, Map<Class<? extends Msg>, Method>>();

	/** Map from Class => MessageInfo */
	private HashMap<Class<?>, MessageInfo> messageInfo=new HashMap<Class<?>, MessageInfo>();

	/** Debug method that lists current requests */
	public void displayHandlers()
	{
		synchronized(owners)
		{
			System.err.println("Message requests");
			System.err.println("====");
			for(Map.Entry<Class<? extends Msg>, OwnerDetails> me : owners.entrySet())
			{
				Class<? extends Msg> cMessage = me.getKey();
				OwnerDetails od = me.getValue();
				System.err.println("\n"+cMessage.getName()+"\n----");

				for(Iterator<?> iRequest=od.requests.iterator();iRequest.hasNext();)
				{
					RequestDetails rd=(RequestDetails)iRequest.next();
					System.err.println(rd);
				}
			}
			System.err.println("====end");
		}
	}

	/**
	 * Registers a new message owner. This also automatically registers the
	 * main message class. You need to manually register any subclasses.
	 * @param owner Owner object
	 * @throws BugException If message owner is already registered
	 */
	public void registerOwner(MsgOwner owner) throws BugException
	{
		Class<? extends Msg> msgClass = owner.getMessageClass();
		if(!msgClass.getName().endsWith("Msg"))
			throw new BugException(
				"Message class must end with 'Msg': "+msgClass.getName());

		registerMessageClass(msgClass);

		synchronized(owners)
		{
			if(owners.containsKey(msgClass))
			  throw new BugException(
					"A MessageOwner "+msgClass.getName()+" is already registered");

			if(msgClass.isInterface())
				throw new BugException(
					"Message types must be classes, not interfaces");

			OwnerDetails od=new OwnerDetails(owner);

			owners.put(msgClass,od);
		}

		owner.init(new MessageDispatchProvider(this,msgClass));
	}

	/**
	 * Removes a registered message owner, along with all its registered requests.
	 * @param owner Owner object
	 * @throws GeneralException If message owner is not registered
	 */
	public void unregisterOwner(MsgOwner owner) throws GeneralException
	{
		Class<? extends Msg> msgClass = owner.getMessageClass();
		unregisterMessageClass(msgClass);

		synchronized(owners)
		{
			if(!owners.containsKey(msgClass))
				throw new GeneralException(
					"The MessageOwner for "+msgClass.getName()+" is not registered");
			owners.remove(msgClass);
		}
	}

	/**
	 * Registers a new message class. You do not need to call this for message
	 * classes that are directly returned by {@link MsgOwner#getMessageClass()}.
	 * @param c Message class
	 * @throws BugException If class is already registered or if class
	 *   is illegal for some reason.
	 */
	public void registerMessageClass(Class<? extends Msg> c)
	{
		MessageInfo info=null;
		try
		{
			info=(MessageInfo)c.getDeclaredField("info").
				get(null);
			if(info.getMessageClass()!=c)
				throw new BugException("Message class "+c+" has MessageInfo for other class");
		}
		catch(NoSuchFieldException e)
		{
			// Message class doesn't provide info, so use default
			info=new MessageInfo(c);
		}
		catch(Throwable t) // A huge range of other possible errors
		{
			throw new BugException(t);
		}

		synchronized(messageInfo)
		{
			if(messageInfo.containsKey(c))
				throw new BugException("The message "+c+" is already registered");
			messageInfo.put(c,info);
			Class<?> superclass=info.getMessageClass().getSuperclass();
			MessageInfo superclassInfo=messageInfo.get(superclass);
			if(superclassInfo!=null)
			{
				superclassInfo.addSubclass(info);
				info.setSuperclass(superclassInfo);
			}
			else
			{
				if(Msg.class.isAssignableFrom(superclass))
				{
					throw new BugException("The message "+c+" was registered before its superclass, "+superclass);
				}
			}
		}
	}

	/**
	 * Unregisters a message class. You do not need to call this for the main
	 * {@link MsgOwner#getMessageClass()}.
	 * @param c Class to unregister
	 * @throws BugException If class is not registered
	 */
	public void unregisterMessageClass(Class<? extends Msg> c)
	{
		synchronized(messageInfo)
		{
			MessageInfo info=messageInfo.remove(c);
			if(info==null)
				throw new BugException("The message "+c+" is not registered");
			info.close();
		}
	}

	/**
	 * Obtains the MessageInfo object for a particular message class.
	 * @param c Message class
	 * @return Info
	 * @throws BugException If you request MessageInfo for a class that doesn't
	 *   have it
	 */
	public MessageInfo getMessageInfo(Class<? extends Msg> c)
	{
		synchronized(messageInfo)
		{
			MessageInfo info=messageInfo.get(c);
			if(info==null)
				throw new BugException("No message info for class "+c);
			return info;
		}
	}

	/**
	 * Requests message notification for the given owner class.
	 * @param c Message class
	 * @param target Target for notifications
	 * @param mf Filter for notifications (may be null)
	 * @param priority Request priority
	 * @return ID of request
	 * @throws BugException If classname doesn't exist
	 */
	public int requestMessages(Class<? extends Msg> c, Object target,
		MessageFilter mf, int priority)
	  throws BugException
	{
		MsgOwner mo;
		int iRequestID;
		Collection<RequestDetails> cRequests;

		synchronized(owners)
		{
			OwnerDetails od=null;

			// Check there is an owner that handles these messages
			Class<? extends Msg> cProvided=c;
			while(cProvided != null)
			{
				od = owners.get(cProvided);
				if(od!=null)
				{
					break;
				}
				if(cProvided.equals(Msg.class))
				{
					break;
				}
				cProvided = cProvided.getSuperclass().asSubclass(Msg.class);
			}
			if(od == null)
			{
			  throw new BugException(
			    "No registered MessageOwner provides the message " + c.getName());
			}

			// Get handler methods for the class and check one of them implements this
			// message
			Map<Class<? extends Msg>, Method> mHandlers =
				getHandlerMethods(target.getClass());
			Class<? extends Msg> cHandledMessage=c;
			while(true)
			{
				// See if there's a handler for this
				if(mHandlers.containsKey(cHandledMessage))
				{
					// OK, good we have a handler
					break;
				}

				// Stop when we get to the message root
				if(cHandledMessage==Msg.class)
				{
					throw new BugException("Object class "+target.getClass().getName()+
						" cannot handle requested messages "+c.getName()+" (add a msg method)");
				}

				cHandledMessage = ReflectionUtils.getSuperclassOrInterface(
					cHandledMessage).asSubclass(Msg.class);
			}

			// Get ID from owner
			od.lastId++;
			iRequestID=od.lastId;
			mo=od.mo;
			cRequests=od.requests;
		}

		// See if the system is supposed to handle these messages
		if(mo.registerTarget(target,c,mf,iRequestID,priority))
		{
			RequestDetails rd=new RequestDetails(target,c,mf,iRequestID, priority);
			synchronized(cRequests)
			{
				cRequests.add(rd);
			}
		}

		return iRequestID;
	}

	/**
	 * Called when a plugin is unloaded. Any items cached by message manager
	 * have to be cleared now.
	 * @param pcl Classloader that's going
	 */
	void clearCachedItems(PluginClassLoader pcl)
	{
		synchronized(classHandlerMethods)
		{
			for(Iterator<Class<?>> i =
				classHandlerMethods.keySet().iterator(); i.hasNext();)
			{
				Class<?> c = i.next();
				if(c.getClassLoader()==pcl)
				{
					i.remove();
				}
			}
		}
	}

	/**
	 * Obtains (from cache or by working it out) a map of message handler methods in
	 * the given class.
	 * @param c Target class
	 * @return Map from Class (message class) -> Method
	 * @throws BugException Any errors in the class
	 */
	private Map<Class<? extends Msg>, Method> getHandlerMethods(
		Class<?> c) throws BugException
	{
		synchronized(classHandlerMethods)
		{
			Map<Class<? extends Msg>, Method> mHandlers = classHandlerMethods.get(c);
			if(mHandlers==null)
			{
				// Build list of msg methods in requester
				mHandlers = new HashMap<Class<? extends Msg>, Method>();
				classHandlerMethods.put(c, mHandlers);
				Method[] am=c.getMethods();
				for(int iMethod=0;iMethod<am.length;iMethod++)
				{
					// Consider only methods called msg
					Method m=am[iMethod];
					if(!m.getName().equals("msg")) continue;

					// Check modifiers
					int iMods=m.getModifiers();
					String sError=c.getName()+"."+m.getName();
					if(!Modifier.isPublic(iMods)) throw new BugException(
						"Message handler must be public: "+
						sError);
					if(Modifier.isStatic(iMods)) throw new BugException(
						"Message handler may not be static: "+
						sError);

					// Get message class
					Class<?>[] ac = m.getParameterTypes();
					if(ac.length!=1)  throw new BugException(
						"Message handler must take single parameter: "+
						sError);
					if(!(Msg.class.isAssignableFrom(ac[0]))) throw new BugException(
						"Message handler parameter must be a Msg subclass: "+
						sError);

					// Add to list
					mHandlers.put(ac[0].asSubclass(Msg.class), m);
				}
			}
			return mHandlers;
		}
	}

	/**
	 * Removes request notification with given ID.
	 * <p>
	 * This method ignores requests that aren't found, rather than throwing an
	 * exception, because it's possible that requests are removed automatically
	 * by the removal of a requestowner.
	 * @param c Message class
	 * @param target Target requesting removal
	 * @param requestId ID of request
	 */
	public void unrequestMessages(Class<? extends Msg> c, Object target, int requestId)
	{
		MsgOwner owner;
		Collection<RequestDetails> requests;

		// Find owner
		synchronized(owners)
		{
			OwnerDetails details=null;
			Class<? extends Msg> realMessageClass=c;
			while(true)
			{
				details = owners.get(realMessageClass);
				if(details!=null)
				{
					break;
				}
				if(realMessageClass == Msg.class)
				{
					break;
				}
				realMessageClass = realMessageClass.getSuperclass().asSubclass(Msg.class);
			}
			if(details==null) return; // Ignore if owner no longer exists

			owner = details.mo;
			requests = details.requests;
		}

		// Check if request is stored in our list and, if so, remove it from that
		synchronized(requests)
		{
			for(Iterator<RequestDetails> i=requests.iterator(); i.hasNext();)
			{
				RequestDetails rd = i.next();
				if(rd.requestId==requestId && rd.target==target)
				{
					i.remove();
					break;
				}
			}
		}

		// Tell the owner it's been removed
		owner.unregisterTarget(target,requestId);
	}

	/**
	 * Dispatch a message from outside the MessageOwner that owns it. Checks
	 * with the MessageOwner first.
	 * @param c Message class
	 * @param m Actual message
	 * @param immediate True if message should be sent to all targets
	 * 	immediately before this message returns; false if it should be queued
	 *  for later sending after other messages have been handled
	 * @return True if message was permitted, false otherwise
	 * @throws BugException If the message is of invalid type or the
	 *   MessageOwner cannot be found
	 */
	public boolean externalDispatch(Class<? extends Msg> c, Msg m,
		boolean immediate)
		throws BugException
	{
		MsgOwner mo;
		synchronized(owners)
		{
			OwnerDetails od=owners.get(c);
			if(od==null)
				throw new BugException(
					"No registered MessageOwner provides the message "+c.getName());
			mo=od.mo;
		}
		if(mo.allowExternalDispatch(m))
		{
			dispatch(c,m, immediate);
			return true;
		}
		else
			return false;
	}

	/**
	 * @return True if in the event thread
	 */
	public static boolean isEventThread()
	{
		return SwingUtilities.isEventDispatchThread();
	}

	/**
	 * Dispatch a message.
	 * @param c Message class
	 * @param m Actual message
	 * @param immediate True if message should be sent to all targets
	 * 	immediately before this message returns; false if it should be queued
	 *  for later sending after other messages have been handled
	 * @throws BugException If the message is of invalid type or the
	 *   MessageOwner cannot be found
	 */
	void dispatch(final Class<? extends Msg> c, final Msg m, boolean immediate)
		throws BugException
	{
		if(!SwingUtilities.isEventDispatchThread() && immediate)
			throw new BugException("Must be in Swing thread for immediate despatch");

		if(immediate)
		{
			internalDispatch(c,m);
		}
		else
		{
			PendingEvent event=new PendingEvent(c,m);

			synchronized(pendingEvents)
			{
				// Put event into list. Event goes at the end, except that if
				// there are any existing events that are sequenced to happen after
				// it, these are pulled out and put afterwards.
				LinkedList<PendingEvent> after=new LinkedList<PendingEvent>();
				for(Iterator<PendingEvent> i=pendingEvents.iterator();i.hasNext();)
				{
					PendingEvent other=i.next();
					if(event.m.sequenceBefore(other.m))
					{
						i.remove();
						after.add(other);
					}
				}
				pendingEvents.add(event);
				pendingEvents.addAll(after);

				if(pendingEvents.size()==1)
				{
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							flush();
						}
					});
				}
			}
		}
	}

	/** Event information on queue */
	class PendingEvent
	{
		Class<? extends Msg> messageClass;
		Msg m;

		public PendingEvent(Class<? extends Msg> messageClass,Msg m)
		{
			this.messageClass=messageClass;
			this.m=m;
		}
	}

	/**
	 * Runs through all pending events until the list is empty.
	 */
	public void flush()
	{
		while(true)
		{
			PendingEvent first;
			synchronized(pendingEvents)
			{
				if(pendingEvents.isEmpty()) break;
				first=pendingEvents.removeFirst();
			}

			try
			{
				internalDispatch(first.messageClass,first.m);
			}
			catch(Throwable t)
			{
				ErrorMsg.report(
					"Error during dispatch of "+first.m,t);
			}
		}
	}

	/**
	 * Runs a given task after the current event has finished processing
	 * and the event queue is empty.
	 * @param r Task to run
	 */
	public void yield(final Runnable r)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				synchronized(pendingEvents)
				{
					if(pendingEvents.isEmpty())
						r.run();
					else
						yield(r);
				}
			}
		});
	}

	private LinkedList<PendingEvent> pendingEvents =
		new LinkedList<PendingEvent>();

	/**
	 * Handles actual message dispatch.
	 * @param c Message class
	 * @param m Message
	 */
	private void internalDispatch(Class<? extends Msg> c, Msg m)
	{
		// Verify class
		if(!c.isAssignableFrom(m.getClass()))
			throw new BugException(
				"Message "+m+" is not of the claimed type "+c.getName());

		// Find owner
		MsgOwner owner;
		Collection<RequestDetails> requests;
		synchronized(owners)
		{
			OwnerDetails details = owners.get(c);
			if(details==null)
			{
				throw new BugException(
					"No registered MessageOwner provides the message "+c.getName());
			}

			owner = details.mo;
			requests = details.requests;
		}

		// Allow owner chance to do its own dispatch
		owner.manualDispatch(m);

		// Get list of targets for system dispatch
		RequestDetails[] ard;
		synchronized(requests)
		{
			// Doing just this is safer, so we don't stay synchronized while
			// processing messages
			ard=requests.toArray(new RequestDetails[0]);
		}

		// Dispatch to all targets
		for (int iTarget= 0; iTarget < ard.length; iTarget++)
		{
			if(m.isStopped()) break;
			RequestDetails rd=ard[iTarget];
			if(
					(rd.msgClass==null || rd.msgClass.isAssignableFrom(m.getClass())) &&
					(rd.mf==null || rd.mf.accept(m)))
			{
				dispatchMessageToTarget(m,rd);
			}
		}
	}


	/**
	 * Dispatchs a message to a single target.
	 * @param m Message to dispatch
	 * @param o Target to receive it
	 */
	public void dispatchMessageToTarget(Msg m, Object o)
	{
		dispatchMessageToTarget(m,
			new RequestDetails(o,null,null,-1,-1));
	}

	/**
	 * Dispatchs a message to a single target.
	 * @param m Message to dispatch
	 * @param rd Details of target to receive it
	 */
	private void dispatchMessageToTarget(Msg m, RequestDetails rd)
	{
		try
		{
			Map<?, ?> mHandlers = getHandlerMethods(rd.target.getClass());
			Class<? extends Msg> cHandledMessage = m.getClass();
			while(true)
			{
				// See if there's a handler for this
				Method mHandler=(Method)mHandlers.get(cHandledMessage);
				if(mHandler!=null)
				{
					// OK, good we have a handler
					mHandler.invoke(rd.target,new Object[]{m});
					break;
				}

				// Stop when we get to the message root
				if(cHandledMessage==Msg.class)
				{
					throw new BugException("Object class "+rd.target.getClass().getName()+
						" cannot handle requested messages "+m.getClass().getName());
				}

				cHandledMessage = ReflectionUtils.getSuperclassOrInterface(
					cHandledMessage).asSubclass(Msg.class);
			}
		}
		catch(Throwable t)
		{
			// Don't let an exception in message processing stop the other
			// targets from receiving the message; instead, report it through
			// the errorhandler message.

			// Special-case if this *is* the errorhandler message...
			if(m instanceof ErrorMsg)
			{
				t.printStackTrace();
			}
			else
			{
				// Ok, make a new error message and dispatch it
				ErrorMsg.report(
					"Error during dispatch of "+m+" to "+rd,t);
			}
		}
	}

	static class DispatchItem
	{
		Class<? extends Msg> msgClass;
		Msg m;

		DispatchItem(Class<? extends Msg> msgClass, Msg m)
		{
			this.msgClass = msgClass;
			this.m=m;
		}
	}

	static class OwnerDetails
	{
		OwnerDetails(MsgOwner mo)
		{
			this.mo=mo;
		}

		MsgOwner mo;
		int lastId = 0;
		Set<RequestDetails> requests = new TreeSet<RequestDetails>();
	}

	static class RequestDetails implements Comparable<RequestDetails>
	{
		Object target;
		MessageFilter mf;
		Class<? extends Msg> msgClass;
		int requestId;
		int priority;

		RequestDetails(Object oTarget, Class<? extends Msg> cMessage,
			MessageFilter mf,int iRequestID, int iPriority)
		{
			this.target=oTarget;
			this.msgClass=cMessage;
			this.mf=mf;
			this.requestId=iRequestID;
			this.priority=iPriority;
		}

		@Override
		public int compareTo(RequestDetails other)
		{
			if(other==this) return 0;

			if(priority > other.priority)
			{
				return -1; // This should come earlier
			}
			else if(priority < other.priority)
			{
				return 1; // This should come later
			}
			else // Lower request IDs come earlier
			{
				return requestId - other.requestId;
			}
		}

		@Override
		public String toString()
		{
			return "["+msgClass.getName()+"] -> "+target+" (pri "+priority+",id "+requestId+")";
		}
	}
}

