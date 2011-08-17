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

import java.util.*;

import leafchat.core.api.*;

/** Context for a plugin */
public class PluginContextProvider implements PluginContext
{
	/** Plugin this is context for */
	private Plugin p;

	/** Registered message owners */
	private List<MsgOwner> messageOwners = new LinkedList<MsgOwner>();

	/** Requests for messages made by this plugin */
	private List<MessageRequest> messageRequests =
		new LinkedList<MessageRequest>();

	/** List of singletons owned by this plugin */
	private List<SingletonRegistration> singletons =
		new LinkedList<SingletonRegistration>();

	/** List of factories owned by this plugin */
	private List<FactoryRegistration> factories =
		new LinkedList<FactoryRegistration>();

	/** List of message classes owned by this plugin */
	private List<Class<? extends Msg>> extraMessageClasses =
		new LinkedList<Class<? extends Msg>>();

	/**
	 * @param pmOwner PluginManager that created this
	 * @param p Plugin for which this is a context
	 */
	public PluginContextProvider(PluginManager pmOwner,Plugin p)
	{
		this.p=p;
		log("Plugin init (for "+getPluginClassLoader()+")");
	}

	@Override
	public void log(String sText)
	{
		log(sText,null);
	}

	@Override
	public void log(String sText,Throwable t)
	{
		SingletonManager.get().get(SystemLog.class).log(
			p,sText,t);
	}

	@Override
	public void logDebug(String text)
	{
		logDebug(text,null);
	}

	@Override
	public void logDebug(String text,Throwable t)
	{
		if(getPluginClassLoader()==null || getPluginClassLoader().getInfo().isDebug())
			SingletonManager.get().get(SystemLog.class).log(
				p,text,t);
	}

	/**
	 * @return Classloader for plugin
	 */
	PluginClassLoader getPluginClassLoader()
	{
		try
		{
			return (PluginClassLoader)p.getClass().getClassLoader();
		}
		catch(ClassCastException cce) // Only for IDE startup
		{
			return null;
		}
	}

	/**
	 * Closes the plugin and remove all its events etc.
	 * @return List of any errors that happened during close
	 */
	public Throwable[] close()
	{
		log("Initiating close");

		// Track all errors that occur during close, instead of letting them
		// interfere with the close process
		List<Throwable> errors = new LinkedList<Throwable>();

		// Inform plugin it's being closed
		try
		{
			p.close();
		}
		catch(Throwable t)
		{
			errors.add(t);
		}

		// Remove message classes
		synchronized(extraMessageClasses)
		{
			for(Class<? extends Msg> c : extraMessageClasses)
			{
				MessageManager.get().unregisterMessageClass(c);
			}
		}

		// Remove message owners
		synchronized(messageOwners)
		{
			for(Iterator<MsgOwner> i=messageOwners.iterator();i.hasNext();)
			{
				MsgOwner mo = i.next();
				try
				{
					log("Removing message owner for: "+mo.getMessageClass().getName());
					MessageManager.get().unregisterOwner(mo);
				}
				catch (Throwable t)
				{
					errors.add(t);
				}
				i.remove();
			}
		}

		// Remove message requests
		synchronized(messageRequests)
		{
			for(Iterator<MessageRequest> i=messageRequests.iterator();i.hasNext();)
			{
				MessageRequest mr = i.next();

				log("Removing message request: "+mr.cMessage.getName());
				MessageManager.get().unrequestMessages(mr.cMessage,mr.oTarget,mr.iRequestID);
				i.remove();
			}
		}

		// Remove singletons
		synchronized(singletons)
		{
			for(Iterator<SingletonRegistration> i=singletons.iterator();i.hasNext();)
			{
				SingletonRegistration sr = i.next();
				try
				{
					log("Removing singleton for: "+sr.cInterface.getName());
					SingletonManager.get().remove(sr.cInterface, sr.cInterface.cast(sr.s));
				}
				catch(Throwable t)
				{
					errors.add(t);
				}
			}
		}

		// Remove factories
		synchronized(factories)
		{
			for(Iterator<FactoryRegistration> i=factories.iterator(); i.hasNext();)
			{
				FactoryRegistration fr = i.next();
				try
				{
					FactoryManager.get().remove(fr.cInterface,fr.f);
				}
				catch(Throwable t)
				{
					errors.add(t);
				}
			}
		}

		// Close other bits
		try
		{
			PluginManager.get().firePluginUnload(p);
		}
		catch(Throwable t)
		{
			errors.add(t);
		}

		log("Close complete. Errors: " + errors.size());

		return errors.toArray(new Throwable[errors.size()]);
	}

	@Override
	public void registerMessageOwner(MsgOwner mo)
	{
		log("Registering as message owner for: "+mo.getMessageClass().getName());
		MessageManager.get().registerOwner(mo);
		synchronized(messageOwners)
		{
			messageOwners.add(mo);
		}
	}

	@Override
	public void registerExtraMessageClass(Class<? extends Msg> c)
	{
		MessageManager.get().registerMessageClass(c);
		synchronized(extraMessageClasses)
		{
			extraMessageClasses.add(c);
		}
	}

	@Override
	public int requestMessages(Class<? extends Msg> msgClass, Object oTarget,
		MessageFilter mf, int iPriority)
	{
		log("Requesting messages: "+msgClass.getName());
		int iRequestID=MessageManager.get().requestMessages(msgClass,oTarget,mf, iPriority);
		synchronized(messageRequests)
		{
			MessageRequest mr=new MessageRequest();
			mr.cMessage=msgClass;
			mr.oTarget=oTarget;
			mr.iRequestID=iRequestID;
			messageRequests.add(mr);
		}

		return iRequestID;
	}

	@Override
	public int requestMessages(Class<? extends Msg> cMessage, Object oTarget, int iPriority)
	{
		return requestMessages(cMessage,oTarget,null, iPriority);
	}

	@Override
	public int requestMessages(Class<? extends Msg> cMessage, Object oTarget)
	{
		return requestMessages(cMessage,oTarget,null,Msg.PRIORITY_NORMAL);
	}

	@Override
	public int requestMessages(Class<? extends Msg> cMessage, Object oTarget,MessageFilter mf)
	{
		return requestMessages(cMessage,oTarget,mf,Msg.PRIORITY_NORMAL);
	}

	@Override
	public void unrequestMessages(Class<? extends Msg> cMessage, Object oTarget, int iRequestID)
	{
		synchronized(messageRequests)
		{
			for(Iterator<MessageRequest> i=messageRequests.iterator(); i.hasNext(); )
			{
				MessageRequest mr = i.next();
				if(
					(iRequestID==ALLREQUESTS || mr.iRequestID==iRequestID) &&
					(cMessage==null || mr.cMessage.equals(cMessage))
				  && mr.oTarget==oTarget)
				{
					log("Removing message request for: "+mr.cMessage.getName());
					MessageManager.get().unrequestMessages(mr.cMessage,mr.oTarget,mr.iRequestID);
					i.remove();
				}
			}
		}
	}

	@Override
	public boolean dispatchExternalMessage(Class<? extends Msg> cMessage, Msg m, boolean bImmediate) throws GeneralException
	{
		return MessageManager.get().externalDispatch(cMessage,m, true);
	}

	@Override
	public <C extends Singleton> void registerSingleton(Class<C> cInterface, C s)
	{
		log("Registering singleton for: "+cInterface.getName());
		SingletonManager.get().add(cInterface,s);
		synchronized(singletons)
		{
			SingletonRegistration sr=new SingletonRegistration();
			sr.cInterface=cInterface;
			sr.s=s;
			singletons.add(sr);
		}
	}

	@Override
	public void registerFactory(Class<? extends FactoryObject> cInterface, Factory f)
	{
		log("Registering factory for: "+cInterface.getName());
		FactoryManager.get().add(cInterface,f);
		synchronized(factories)
		{
			FactoryRegistration fr=new FactoryRegistration();
			fr.cInterface=cInterface;
			fr.f=f;
			factories.add(fr);
		}
	}

	@Override
	public<C extends Singleton> C getSingleton2(Class<C> cInterface)
	{
		return SingletonManager.get().get(cInterface);
	}

	@Override
	public Object getSingleton(Class<? extends Singleton> cInterface)
	{
		return SingletonManager.get().get(cInterface);
	}

	@Override
	public<C extends FactoryObject> C newFactoryObject(Class<C> cInterface)
		throws GeneralException
	{
		return FactoryManager.get().newInstance(cInterface);
	}

	@Override
	public Object newInstance(Class<? extends FactoryObject> cInterface)
		throws GeneralException
	{
		return FactoryManager.get().newInstance(cInterface);
	}

	@Override
	public void dispatchMsgToTarget(Msg m,Object oTarget) throws GeneralException
	{
		MessageManager.get().dispatchMessageToTarget(m,oTarget);
	}

	@Override
	public Plugin getPlugin()
	{
		return p;
	}

	@Override
	public void yield(Runnable r)
	{
		MessageManager.get().yield(r);
	}

	@Override
	public MessageInfo getMessageInfo(Class<? extends Msg> c) throws BugException
	{
		return MessageManager.get().getMessageInfo(c);
	}
}

class MessageRequest
{
	Class<? extends Msg> cMessage;
	Object oTarget;
	int iRequestID;
}

class SingletonRegistration
{
	Class<? extends Singleton> cInterface;
	Singleton s;
}

class FactoryRegistration
{
	Class<? extends FactoryObject> cInterface;
	Factory f;
}