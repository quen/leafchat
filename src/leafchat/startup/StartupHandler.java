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

import java.io.*;
import java.nio.channels.FileLock;
import javax.swing.*;

import util.*;
import leafchat.core.*;
import leafchat.core.api.*;

/** First class that is loaded by the StartupClassLoader */
public class StartupHandler
{
	private static boolean doneInit;
	private static boolean ideStartup;

	protected SplashScreen ss;

	private SystemLogSingleton log;

	protected void classloaderInit(PluginManager pm)
	{
		((StartupClassLoader)(getClass().getClassLoader())).setAPIClassLocator(pm);
	}

	protected void pluginInit(PluginManager pm) throws GeneralException
	{
		pm.init(ss);
	}

	/**
	 * Sends REQUESTSHUTDOWN.
	 * @return True if it's ok to shutdown
	 */
	public static boolean sendRequestShutdownMsg()
	{
		SystemStateMsg request=new SystemStateMsg(SystemStateMsg.REQUESTSHUTDOWN);
		try
		{
			MessageManager.get().externalDispatch(SystemStateMsg.class,
				request,false);
			MessageManager.get().flush();
		}
		catch(Throwable t)
		{
			ErrorMsg.report("Error when requesting shutdown",t);
		}
		return !request.isStopped();
	}

	/**
	 * Sends SHUTDOWN.
	 * @param exitAfterwards If true, exits as soon as messages finish
	 */
	public static void sendShutdownMsg(boolean exitAfterwards)
	{
		try
		{
			MessageManager.get().externalDispatch(SystemStateMsg.class,
				new SystemStateMsg(SystemStateMsg.SHUTDOWN),false);
			MessageManager.get().flush();
		}
		catch(Throwable t)
		{
			ErrorMsg.report("Error when shutting down",t);
		}
		// Exit once the messages have finished
		if(exitAfterwards) System.exit(0);
	}

	static void initStatics(boolean ideStartup)
	{
		// Do static init
		if(doneInit) return;
		doneInit=true;

		StartupHandler.ideStartup=ideStartup;
		PlatformUtils.setUserFolder("leafChat");
		TimeUtils.setErrorHandler(new ErrorHandler()
		{
			@Override
			public void reportError(Throwable t)
			{
				ErrorMsg.report(
				  "An error occurred inside a timed method",t);
				}
		});
	}

	/**
	 * @return True if the program was launched from IDE
	 */
	public static boolean isIDEStartup()
	{
		return ideStartup;
	}

	private MinuteMsgOwner minutes;

	/** Just used to stop you running two copies at once. Held open for duration of run. */
	private static RandomAccessFile lockFile;
	private static FileLock lock;

	/**
	 * Initialise app.
	 */
	public StartupHandler()
	{
		initStatics(false);

		// Prevent multiple runs by same user
		try
		{
			lockFile=new RandomAccessFile(
				new File(PlatformUtils.getUserFolder(),"lockfile"),"rw");
		  lock=lockFile.getChannel().tryLock();
		  if(lock==null)
		  {
				JOptionPane.showMessageDialog(null,
					"You can only run one copy of leafChat at a time.",
					"Already running",JOptionPane.INFORMATION_MESSAGE);
		  	System.exit(0);
		  }
		}
		catch(IOException e)
		{
			handleError(e);
		}

		// Get plugin manager
		PluginManager pm=PluginManager.get();

		// Tell the classloader to search plugin APIs as well
		classloaderInit(pm);

		// Set L&F
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception t)
		{
			handleError(t);
		}

		// Create splash screen
		try
		{
			ss=new SplashScreen();
		}
		catch(IOException t)
		{
			handleError(t);
		}

		// Init Mac platform support
		if(PlatformUtils.isMac())
		{
			// Try using the new version first
			try
			{
				Class.forName("com.apple.eawt.QuitHandler");
				Class.forName("leafchat.startup.MacPlatformNew");
			}
			catch(ClassNotFoundException t)
			{
				// Okay, now use the new version
				try
				{
					Class.forName("leafchat.startup.MacPlatformOld");
				}
				catch(ClassNotFoundException t2)
				{
					handleError(t2);
				}
			}
		}

		// Set up system-provided singletons etc.
		try
		{
			log=new SystemLogSingleton();
			SingletonManager.get().add(SystemLog.class,log);
			SingletonManager.get().add(PluginList.class,PluginManager.get());
			MessageManager.get().registerOwner(new ErrorMsgOwner());
			MessageManager.get().registerOwner(new SystemStateMsgOwner());
			minutes=new MinuteMsgOwner();
			MessageManager.get().registerOwner(minutes);
			MessageManager.get().requestMessages(SystemStateMsg.class,this,null,
			  Msg.PRIORITY_LAST);
			MessageManager.get().requestMessages(SystemStateMsg.class,new SplashCloser(),null,
			  Msg.PRIORITY_NORMAL);
			MessageManager.get().requestMessages(ErrorMsg.class,this,null,
				Msg.PRIORITY_LAST);
		}
		catch(Throwable t)
		{
			handleError(t);
		}

		// Load all plugins
		try
		{
			pluginInit(pm);
		}
		catch(Throwable t)
		{
			handleError(t);
		}

		ss.setText("Loaded, starting system...");

		try
		{
			MessageManager.get().externalDispatch(SystemStateMsg.class,
				new SystemStateMsg(SystemStateMsg.PLUGINSLOADED), false);
		}
		catch(Throwable t)
		{
			handleError(t);
		}
	}

	/**
	 * Class that closes splash screen.
	 */
	public class SplashCloser
	{
		/**
		 * Message: System state message (close splash when UIREADY).
		 * @param msg Message
		 * @throws GeneralException
		 */
		public void msg(SystemStateMsg msg) throws GeneralException
		{
			if(msg.getType()==SystemStateMsg.UIREADY)
			{
				// Get rid of splash screen and remove request
				ss.dispose();
				MessageManager.get().unrequestMessages(SystemStateMsg.class,this,PluginContext.ALLREQUESTS);
			}
		}
	}

	private void handleError(Throwable t)
	{
		if(log!=null)
		{
			log.log(null,"Error on startup",t);
		}
		else
		{
			t.printStackTrace();
			try
			{
				PrintWriter pw=new PrintWriter(new OutputStreamWriter(
					new FileOutputStream(new File(PlatformUtils.getUserFolder(),SystemLogSingleton.SYSTEMLOG)),"UTF-8"));
				t.printStackTrace(pw);
				pw.close();
			}
			catch(Throwable t2)
			{
				System.err.println("Unable to save error");
			}
		}

		JOptionPane.showMessageDialog(null,
			"An error has occurred. Logged in: "+new File(PlatformUtils.getUserFolder(),SystemLogSingleton.SYSTEMLOG).getPath(),
			"Error on startup",JOptionPane.ERROR_MESSAGE);
		System.exit(0);
	}

	/**
	 * Message: System state.
	 * @param msg Message
	 * @throws GeneralException
	 */
	public void msg(SystemStateMsg msg) throws GeneralException
	{
		if(msg.getType()==SystemStateMsg.SHUTDOWN)
		{
			PluginManager.get().close();
			((SystemLogSingleton)(SingletonManager.get().get(SystemLog.class))).close();
			System.exit(0);
		}
		if(msg.getType()==SystemStateMsg.UIREADY)
		{
			minutes.setUIReady();
		}
	}

	@Override
	public String toString()
	{
		return "Startup handler";
	}

	/**
	 * Message: Error message (logs it to SystemLog).
	 * @param msg Message
	 * @throws GeneralException
	 */
	public void msg(ErrorMsg msg) throws GeneralException
	{
		if(msg.isHandled()) return;

		msg.markHandled();
		(SingletonManager.get().get(SystemLog.class)).log(
		  this,"Error: "+msg.getMessage(),msg.getException());
	}

}
