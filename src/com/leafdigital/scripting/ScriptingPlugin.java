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
package com.leafdigital.scripting;

import java.io.File;
import java.util.*;

import util.*;

import com.leafdigital.scripting.Script.SaveContinuation;
import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/** Plugin that handles scripts. */
public class ScriptingPlugin implements Plugin,Script.StateListener
{
	private LinkedList<Script> scripts = new LinkedList<Script>();
	private ScriptingTool st;
	private PluginContext context;
	final static File scriptsFolder=new File(PlatformUtils.getUserFolder(),"scripts");

	@Override
	public void init(PluginContext context, PluginLoadReporter reporter) throws GeneralException
	{
		this.context=context;
		//ItemEvent.debugMessages(context.getMessageInfo(Msg.class),0); System.exit(0);

		// Register tool
		st=new ScriptingTool(context);
		context.getSingle(UI.class).registerTool(st);

		// Request message just for when people try to quit with unsaved changes
		context.requestMessages(SystemStateMsg.class,this);
	}

	@Override
	public void close() throws GeneralException
	{
	}

	@Override
	public String toString()
	{
		return "Scripting plugin";
	}

	Script[] getScripts()
	{
		return scripts.toArray(new Script[scripts.size()]);
	}

	/**
	 * @param name Proposed name for new script
	 * @return True if that name is ok to use
	 */
	static boolean isNewNameOkay(String name)
	{
		// Required name pattern
		String fullName=name+".leafChatScript";
		if(!fullName.matches(Script.ALLOWED_NAMES)) return false;
		// Names already in use
		if((new File(scriptsFolder,fullName)).exists()) return false;
		// OK!
		return true;
	}

	@Override
	public void scriptStateChanged(Script s)
	{
		st.informChanged(s);
	}

	/** Dialog used to prevent quit while unsaved script editor window is up. */
	@UIHandler("quitconfirm")
	public class QuitConfirm
	{
		QuitConfirm(SystemStateMsg m) throws GeneralException
		{
			objectToQuit=false;
			UI ui=context.getSingle(UI.class);
			quitConfirm = ui.createDialog("quitconfirm",this);
			quitConfirm.show(null);

			if(objectToQuit) m.markStopped();
		}
		private Dialog quitConfirm;
		private boolean objectToQuit;

		/** Callback: Quit button. */
		@UIAction
		public void actionQuit()
		{
			quitConfirm.close();
		}

		/** Callback: Cancel button. */
		@UIAction
		public void actionCancel()
		{
			quitConfirm.close();
			objectToQuit=true;
		}
	}

	/**
	 * Message: system state (used to object to quit, and to init).
	 * @param m Message
	 * @throws GeneralException
	 */
	public void msg(SystemStateMsg m) throws GeneralException
	{
		if(m.getType() == SystemStateMsg.REQUESTSHUTDOWN)
		{
			boolean changes = false;
			for(Script s : scripts)
			{
				if(s.isChanged())
				{
					changes = true;
					break;
				}
			}
			if(!changes)
			{
				return; // No script changes so quit is ok
			}

			new QuitConfirm(m);
		}
		else if(m.getType() == SystemStateMsg.PLUGINSLOADED)
		{
			// Load user scripts
			scriptsFolder.mkdirs();
			File[] scriptFiles = IOUtils.listFiles(scriptsFolder);
			for(int i=0; i<scriptFiles.length; i++)
			{
				if(scriptFiles[i].getName().matches(Script.ALLOWED_NAMES) && scriptFiles[i].isFile())
				{
					try
					{
						Script s = new Script(context, scriptFiles[i]);
						s.addStateListener(this);
						scripts.add(s);
					}
					catch(GeneralException e)
					{
						ErrorMsg.report("Error loading user script",e);
					}
				}
			}
		}
	}

	/**
	 * Adds a new named script.
	 * @param name Name for script
	 * @param afterOK Runnable gets run if the script save is successful as in
	 *   it doesn't throw an exception (even if compile fails)
	 * @throws GeneralException
	 */
	public void newScript(String name,Runnable afterOK) throws GeneralException
	{
		final Script s=new Script(context,new File(scriptsFolder,name+".leafChatScript"));
		newScript(s,afterOK);
	}

	/**
	 * Adds a new script.
	 * @param s Script to add
	 * @param afterOK Runnable gets run if the script save is successful as in
	 *   it doesn't throw an exception (even if compile fails)
	 * @throws GeneralException
	 */
	public void newScript(final Script s,
		final Runnable afterOK) throws GeneralException
	{
		scripts.add(s);
   	s.save(new SaveContinuation()
		{
			@Override
			public void afterSave(boolean success)
			{
				if(!success)
				{
					try
					{
						s.saveAndDisable();
					}
					catch(GeneralException e)
					{
						try
						{
							s.delete();
						}
						catch(Throwable t)
						{
						}
						scripts.remove(s);
						ErrorMsg.report("Unexpected error saving new script",e);
						return;
					}
				}
  			afterOK.run();
			}

			@Override
			public void afterSave(Throwable t)
			{
				try
				{
					s.delete();
				}
				catch(Throwable t2)
				{
				}
				scripts.remove(s);
				ErrorMsg.report("Unexpected error saving new script",t);
			}
		});
		s.addStateListener(this);
	}

	/**
	 * Deletes a script.
	 * @param s Script to delete
	 * @throws GeneralException
	 */
	public void deleteScript(Script s) throws GeneralException
	{
		s.delete();
		scripts.remove(s);
	}

}
