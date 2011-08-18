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

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;

import org.w3c.dom.Document;

import util.*;
import util.xml.*;

import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/** Scripting tool/window. */
@UIHandler("scripting")
public class ScriptingTool implements SimpleTool
{
	private PluginContext context;
	private Window w;

	/** UI: Dependencies table */
	public Table dependenciesUI;
	/** UI: Plugins table */
	public Table pluginsUI;
	/** UI: Scripts table */
	public Table scriptsUI;
	/** UI: Create plugin button */
	public Button createUI;
	/** UI: Edit script button */
	public Button editScriptUI;
	/** UI: Delete script button */
	public Button deleteScriptUI;
	/** UI: Export script button */
	public Button exportScriptUI;
	/** UI: Shortname edit */
	public EditBox shortNameUI;
	/** UI: Display name edit */
	public EditBox displayNameUI;
	/** UI: Classname edit */
	public EditBox classNameUI;
	/** UI: Author edit */
	public EditBox authorUI;
	/** UI: Domain edit */
	public EditBox domainUI;
	/** UI: Target folder edit */
	public EditBox targetUI;
	/** UI: Show system plugins checkbox */
	public CheckBox showSystemUI;
	/** UI: Plugin name */
	public Label pluginNameUI;
	/** UI: Plugin description */
	public Label pluginDescriptionUI;
	/** UI: Plugin authors */
	public Label pluginAuthorsUI;
	/** UI: Plugin location */
	public Label pluginLocationUI;
	/** UI: Main choice panel */
	public ChoicePanel scriptMainUI;

	private boolean changedClassName;

	private HashMap<Script, ScriptEditor> editors = new HashMap<Script, ScriptEditor>();

	/**
	 * @param context Plugin context
	 */
	public ScriptingTool(PluginContext context)
	{
		this.context=context;
	}

	@Override
	public int getDefaultPosition()
	{
		return 500;
	}

	@Override
	public void removed()
	{
	}

	@Override
	public void clicked() throws GeneralException
	{
		if(w==null)
		{
			UI u=context.getSingle(UI.class);
			w=u.createWindow("scripting", this);
			w.setRemember("tool", "scripting");
			initWindow();
			w.show(false);
		}
		else
		{
			w.activate();
		}
	}

	/** Callback: Script window closed. */
	@UIAction
	public void windowClosed()
	{
		w=null;
		changedClassName=false;
	}

	private static class ExportedPackage implements Comparable<ExportedPackage>
	{
		String name;
		int maxVersion;
		ExportedPackage(File jar, String name, int maxVersion)
		{
			this.name=name;
			this.maxVersion=maxVersion;
		}
		@Override
		public int compareTo(ExportedPackage o)
		{
			return name.compareTo(o.name);
		}
	}

	private final static int COL_PACKAGE=0,COL_VERSION=1,COL_REQUIRED=2;

	private void initWindow()
	{
		targetUI.setFlag(EditBox.FLAG_ERROR);
		PluginInfo[] plugins=context.getSingle(PluginList.class).getPluginList();
	  SortedSet<ExportedPackage> sortedPackages = new TreeSet<ExportedPackage>();
		for(int i=0;i<plugins.length;i++)
		{
			PluginExport[] exports=plugins[i].getPluginExports();
			for(int j=0;j<exports.length;j++)
			{
				sortedPackages.add(new ExportedPackage(plugins[i].getJar(),exports[j].getPackage(),exports[j].getMaxVersion()));
			}
		}

		for(ExportedPackage ep : sortedPackages)
		{
			int index=dependenciesUI.addItem();
			dependenciesUI.setString(index,COL_PACKAGE,ep.name);
			dependenciesUI.setString(index,COL_VERSION,""+ep.maxVersion);
			if(ep.name.matches("com.leafdigital.(irc|ui).api"))
				dependenciesUI.setBoolean(index,COL_REQUIRED,true);
		}

		changeShowSystem();
		selectPlugins();
		updateScripts();
	}

	@Override
	public String getLabel()
	{
		return "Scripting";
	}

	@Override
	public String getThemeType()
	{
		return "scriptingButton";
	}

	/** Callback: Browse button. */
	@UIAction
	public void actionBrowse()
	{
		String existing=targetUI.getValue();
		if(existing.length()==0)
			existing=PlatformUtils.getDocumentsFolder();
		File selected=context.getSingle(UI.class).showFolderSelect(
			null,"Select parent folder", new File(existing));
		if(selected!=null)
		{
			targetUI.setValue(selected.getAbsolutePath());
			changeTarget();
		}
	}

	/**
	 * User clicks Help button.
	 */
	@UIAction
	public void actionHelp()
	{
		try
		{
			PlatformUtils.showBrowser((new File("help/scripts/index.html")).toURI().toURL());
		}
		catch(MalformedURLException e)
		{
			ErrorMsg.report("Failed to open help",e);
		}
		catch(IOException e)
		{
			ErrorMsg.report(e.getMessage(),e.getCause());
		}
	}

	/**
	 * Callback: Plugin create button.
	 * @throws GeneralException
	 */
	@UIAction
	public void actionCreate() throws GeneralException
	{
		try
		{
			File target=new File(targetUI.getValue(),shortNameUI.getValue());
			if(target.exists())
			{
				context.getSingle(UI.class).showUserError(w,
					"Folder exists", "The project folder " + target
					+ " already exists. If you want to overwrite it, you must manually delete it first.");
				return;
			}

			target.mkdir();

			// Make the API jar file
			File lib=new File(target,"lib");
			lib.mkdir();
			List<String> packagesList = new LinkedList<String>();
			for(int index=0; index<dependenciesUI.getNumItems(); index++)
			{
				if(dependenciesUI.getBoolean(index,COL_REQUIRED))
					packagesList.add(dependenciesUI.getString(index,COL_PACKAGE));
			}
			String[] packages=packagesList.toArray(new String[packagesList.size()]);
			context.getSingle(PluginList.class).saveAPIJar(
				packages,
				new File(lib,"leafchat.selectedapi.jar"));

			// Flip the package and make the relevant source folder
			String domainName=domainUI.getValue();
			String packageName="";
			while(domainName.length()>0)
			{
				packageName+=domainName.replaceAll("^.*\\.","")+".";
				domainName=domainName.replaceAll("(^[^.]+$)|(\\.[^.]+$)","");
			}
			packageName+=shortNameUI.getValue();
			File src=new File(target,"src/"+packageName.replace('.','/'));
			src.mkdirs();

			// Make the plugininfo.xml
			String pluginInfo=IOUtils.loadString(getClass().getResourceAsStream("template/plugininfo.xml.template"));
			pluginInfo=pluginInfo.replaceAll("%%DISPLAYNAME%%",XML.esc(displayNameUI.getValue()));
			pluginInfo=pluginInfo.replaceAll("%%AUTHOR%%",XML.esc(authorUI.getValue()));
			pluginInfo=pluginInfo.replaceAll("%%CLASSNAME%%",packageName+"."+classNameUI.getValue()+"Plugin");
			String dependencies="";
			for(int index=0;index<dependenciesUI.getNumItems();index++)
			{
				if(dependenciesUI.getBoolean(index,COL_REQUIRED))
				{
					dependencies+=
						"  <api>\n    <package>"+
						dependenciesUI.getString(index,COL_PACKAGE)+
						"</package>\n    <version>"+
						dependenciesUI.getString(index,COL_VERSION)+
						"</version>\n  </api>\n";
				}
			}
			pluginInfo=pluginInfo.replaceAll("%%DEPENDENCIES%%",dependencies);
			FileOutputStream fos=new FileOutputStream(new File(src,"plugininfo.xml"));
			fos.write(pluginInfo.getBytes("UTF-8"));
			fos.close();

			// Make the sample class file
			String java=IOUtils.loadString(getClass().getResourceAsStream("template/Plugin.java.template"));
			java=java.replaceAll("%%DISPLAYNAME%%",displayNameUI.getValue().replaceAll("([\\\"])","\\$1"));
			java=java.replaceAll("%%CLASSNAME%%",classNameUI.getValue()+"Plugin");
			java=java.replaceAll("%%PACKAGENAME%%",packageName);
			fos=new FileOutputStream(new File(src,classNameUI.getValue()+"Plugin.java"));
			fos.write(java.getBytes("UTF-8"));
			fos.close();

			// Make the ant script
			String ant=IOUtils.loadString(getClass().getResourceAsStream("template/build.xml.template"));
			ant=ant.replaceAll("%%DISPLAYNAME%%",XML.esc(displayNameUI.getValue()));
			ant=ant.replaceAll("%%SHORTNAME%%",shortNameUI.getValue());
			ant=ant.replaceAll("%%USERPLUGINS%%",XML.esc(PlatformUtils.getUserFolder()+"/plugins"));
			fos=new FileOutputStream(new File(target,"build.xml"));
			fos.write(ant.getBytes("UTF-8"));
			fos.close();

			createUI.setEnabled(false);
			if(!PlatformUtils.systemOpen(target))
			{
				context.getSingle(UI.class).showUserError(w,
					"Unable to open folder", "Because you are using an older Java version "
					+ "or a platform that doesn't support opening folders, leafChat "
					+ "cannot show the folder for you. However, the plugin template has "
					+ "been created at <key>" + target.getAbsolutePath() + "</key>.");
				return;
			}
			return;
		}
		catch(IOException e)
		{
			throw new GeneralException("Error saving project",e);
		}
	}

	/** Callback: Plugin target changed. */
	@UIAction
	public void changeTarget()
	{
		File f=new File(targetUI.getValue());
		targetUI.setFlag((f.isDirectory() && f.canWrite()) ? EditBox.FLAG_NORMAL : EditBox.FLAG_ERROR);
		changeEdit();
	}

	/** Callback: Class name changed. */
	@UIAction
  public void changeClassName()
  {
		changedClassName = true;
		changeEdit();
  }

  /** Callback: Short name changed. */
	@UIAction
  public void changeShortName()
  {
	  if(!changedClassName && shortNameUI.getFlag() == EditBox.FLAG_NORMAL)
	  {
  	  classNameUI.setValue(
	  		shortNameUI.getValue().substring(0,1).toUpperCase()
	  		+ shortNameUI.getValue().substring(1));
	  }
	  changeEdit();
  }

  /** Callback: Some edit changed. */
	@UIAction
	public void changeEdit()
	{
		createUI.setEnabled(
			shortNameUI.getFlag()==EditBox.FLAG_NORMAL &&
			displayNameUI.getFlag()==EditBox.FLAG_NORMAL &&
			classNameUI.getFlag()==EditBox.FLAG_NORMAL &&
			authorUI.getFlag()==EditBox.FLAG_NORMAL &&
			domainUI.getFlag()==EditBox.FLAG_NORMAL &&
			targetUI.getFlag()==EditBox.FLAG_NORMAL
			);
	}

	// Plugins view tab
	///////////////////

	private final static int PCOL_PLUGIN=0,PCOL_VERSION=1;
	PluginInfo[] pluginsList;

	/** Callback: System plugin checkbox changed. */
	@UIAction
	public void changeShowSystem()
	{
		boolean evenSystem=showSystemUI.isChecked();
		PluginInfo[] plugins=
			context.getSingle(PluginList.class).getPluginList();
		List<PluginInfo> includedPlugins = new LinkedList<PluginInfo>();
		for(int i=0;i<plugins.length;i++)
		{
			if(plugins[i].isSystem() && !evenSystem) continue;
			if(plugins[i].isUserScript()) continue;
			includedPlugins.add(plugins[i]);
		}
		pluginsList=includedPlugins.toArray(new PluginInfo[includedPlugins.size()]);

		pluginsUI.clear();
		for(int i=0;i<pluginsList.length;i++)
		{
			int index=pluginsUI.addItem();
			pluginsUI.setString(index,PCOL_PLUGIN,pluginsList[i].getName());
			pluginsUI.setString(index,PCOL_VERSION,pluginsList[i].getVersion());
		}
	}

	/**
	 * Callback: Plugin selected.
	 */
	@UIAction
	public void selectPlugins()
	{
		int index=pluginsUI.getSelectedIndex();
		if(index==Table.NONE)
		{
			pluginNameUI.setText("(Select plugin for information)");
			pluginDescriptionUI.setText("");
			pluginAuthorsUI.setText("");
			pluginLocationUI.setText("");
		}
		else
		{
			PluginInfo current=pluginsList[index];
			pluginNameUI.setText(XML.esc(current.getName())+" <s>"+current.getVersion()+"</s>");
			pluginDescriptionUI.setText(XML.esc(current.getDescription()));
			String authors="";
			for(int i=0;i<current.getAuthors().length;i++)
			{
				authors+="<line>"+XML.esc(current.getAuthors()[i])+"</line>";
			}
			pluginAuthorsUI.setText(authors);
			try
			{
				pluginLocationUI.setText(XML.esc(current.getJar().getCanonicalPath()));
			}
			catch(IOException e)
			{
				throw new BugException(e);
			}
		}
	}

	private Script[] displayedScripts;

	private final static int SCOL_NAME=0,SCOL_ERRORS=1,SCOL_ENABLED=2;

	private void updateScripts()
	{
		int indexBefore=scriptsUI.getSelectedIndex();
		Script selectedBefore=indexBefore==Table.NONE ? null : displayedScripts[indexBefore];

		scriptsUI.clear();
		displayedScripts=((ScriptingPlugin)context.getPlugin()).getScripts();
		for(int i=0;i<displayedScripts.length;i++)
		{
			scriptsUI.addItem();
			Script s=displayedScripts[i];
			scriptsUI.setString(i,SCOL_NAME,s.getName());
			updateScriptDetails(i,s);
			if(s==selectedBefore)
				scriptsUI.setSelectedIndex(i);
		}
		selectScripts();

		scriptMainUI.display(displayedScripts.length==0 ? "noneChoice" : "tableChoice");
	}

	private void updateScriptDetails(int i,Script s)
	{
		scriptsUI.setString(i,SCOL_ERRORS,s.hasErrors() ? s.getErrorCount()+" error"+(s.getErrorCount()==1 ? "" : "s") : "");
		scriptsUI.setBoolean(i,SCOL_ENABLED,s.isEnabled());
		scriptsUI.setEditable(i,SCOL_ENABLED,!s.isChanged() && s.hasJar() && !s.hasErrors());
		scriptsUI.setDim(i,SCOL_NAME,s.isChanged());
	}

	/** Callback: Script selection change. */
	@UIAction
	public void selectScripts()
	{
		int index=scriptsUI.getSelectedIndex();
		boolean gotOne=index!=Table.NONE;
		editScriptUI.setEnabled(gotOne);
		// You can only delete scripts that don't have unsaved changes
		deleteScriptUI.setEnabled(gotOne && !displayedScripts[index].isChanged());
		exportScriptUI.setEnabled(gotOne && !displayedScripts[index].isChanged());
	}

	/**
	 * Callback: Script enable tickbox changed.
	 * @param index Table row
	 * @param column Table column
	 * @param before Previous value
	 * @throws GeneralException
	 */
	@UIAction
	public void changeScripts(int index,int column,Object before) throws GeneralException
	{
		boolean enabled=scriptsUI.getBoolean(index,column);
		displayedScripts[index].setEnabled(enabled);
	}

	/** Dialog for handling new scripts. */
	@UIHandler("newscript")
	public class NewScript
	{
		/** UI: Script name */
		public EditBox nameUI;
		/** UI: Create button */
		public Button createUI;
		private Dialog d;

		NewScript() throws GeneralException
		{
			d = context.getSingle(UI.class).createDialog("newscript", this);
			d.show(w);
		}

		/** Callback: Name changed. */
		@UIAction
		public void changeName()
		{
			if(ScriptingPlugin.isNewNameOkay(nameUI.getValue()))
			{
				nameUI.setFlag(EditBox.FLAG_NORMAL);
				createUI.setEnabled(true);
			}
			else
			{
				nameUI.setFlag(EditBox.FLAG_ERROR);
				createUI.setEnabled(false);
			}
		}

		/**
		 * Callback: Create button.
		 * @throws GeneralException
		 */
		@UIAction
		public void actionCreate() throws GeneralException
		{
			d.close();
			((ScriptingPlugin)context.getPlugin()).newScript(nameUI.getValue(),
				new Runnable()
				{
					@Override
					public void run()
					{
						updateScripts();
						scriptsUI.setSelectedIndex(displayedScripts.length-1);
						informChanged(displayedScripts[displayedScripts.length-1]);
					}
				});
		}

		/**
		 * Callback: Cancel button.
		 */
		@UIAction
		public void actionCancel()
		{
			d.close();
		}
	}

	/**
	 * Callback: New script button.
	 * @throws GeneralException
	 */
	@UIAction
	public void actionNewScript() throws GeneralException
	{
		new NewScript();
	}

	/**
	 * Callback: Import script button.
	 * @throws GeneralException
	 */
	@UIAction
	public void actionImportScript() throws GeneralException
	{
		UI ui=context.getSingle(UI.class);
		File selected=ui.showFileSelect(w,"Choose script to import",false,
			new File(PlatformUtils.getDesktopFolder()),null,new String[] {".leafChatScript"},
			"leafChat scripts");
		if(selected==null) return;

		if(!selected.getName().matches(Script.ALLOWED_NAMES))
		{
			ui.showUserError(w,"Error importing script","Script filenames must end in .leafChatScript and must not contain any special characters before that; only A-Z, a-z, 0-9 and space are permitted.");
			return;
		}
		File f;
		try
		{
			// Disable script before importing it
			Document d=XML.parse(selected);
			if(!d.getDocumentElement().getTagName().equals("script") ||
				!d.getDocumentElement().hasAttribute("enabled"))
				throw new XMLException("Not valid");
			if(XML.getIntAttribute(d.getDocumentElement(),"version")!=1)
			{
				ui.showUserError(w,"Error importing script","This version of the script format is not supported. Check that you have the latest leafChat version.");
				return;
			}
			d.getDocumentElement().setAttribute("enabled","n");
			f=new File(ScriptingPlugin.scriptsFolder,selected.getName());
			if(f.exists())
			{
				ui.showUserError(w,"Error importing script","You already have a script of the same name.");
				return;
			}
			XML.save(f,d);
		}
		catch(XMLException e)
		{
			ui.showUserError(w,"Error importing script","Not a valid leafChat script.");
			return;
		}

		final Script s;
		try
		{
			s=new Script(context,f);
		}
		catch(GeneralException e)
		{
			f.delete();
			ErrorMsg.report("Error importing script",e);
			return;
		}

		((ScriptingPlugin)context.getPlugin()).newScript(s,new Runnable()
		{
			@Override
			public void run()
			{
				updateScripts();
				scriptsUI.setSelectedIndex(displayedScripts.length-1);
				informChanged(displayedScripts[displayedScripts.length-1]);

				UI ui=context.getSingle(UI.class);
				ui.showQuestion(w,"Import successful",
					"<para><strong>"+s.getName()+"</strong> was successfully imported. This " +
					"script has been disabled. To enable it, click the checkbox by its name.</para>" +
					"<para>It is <strong>critically important</strong> that you do not enable " +
					"any scripts unless they were written by you or someone you trust. Scripts " +
					"can easily <strong>damage your computer</strong>. Installing an unknown " +
					"leafChat script is just as dangerous as running an unknown application " +
					"program, so please take care.</para>",
					UI.BUTTON_YES,"OK",null,null,UI.BUTTON_YES);
			}
		});
	}

	/**
	 * Callback: Export script button.
	 * @throws GeneralException
	 */
	@UIAction
	public void actionExportScript() throws GeneralException
	{
		UI ui=context.getSingle(UI.class);
		File selected=ui.showFileSelect(w,"Export script as",true,
			new File(PlatformUtils.getDesktopFolder()),null,new String[] {".leafChatScript"},
			"leafChat scripts");
		if(selected==null) return;
		if(!selected.getName().endsWith(".leafChatScript"))
		{
			selected=new File(selected.getPath()+".leafChatScript");
		}
		if(selected.exists())
		{
			ui.showUserError(w,"Export failed","Target file already exists");
			return;
		}
		Script script=displayedScripts[scriptsUI.getSelectedIndex()];
		try
		{
			IOUtils.copy(
				new FileInputStream(script.getFile()),new FileOutputStream(selected),true);
		}
		catch(IOException e)
		{
			throw new GeneralException(e);
		}
	}

	/**
	 * Callback: Edit script button.
	 * @throws GeneralException
	 */
	@UIAction
	public void actionEditScript() throws GeneralException
	{
		Script script=displayedScripts[scriptsUI.getSelectedIndex()];
		ScriptEditor editor=editors.get(script);
		if(editor==null)
			editors.put(script,new ScriptEditor(context,this,script));
		else
			editor.focus();
	}

	void informClosed(Script s)
	{
		editors.remove(s);
	}

	/**
	 * Callback: Delete script button.
	 * @throws GeneralException
	 */
	@UIAction
	public void actionDeleteScript() throws GeneralException
	{
		Script script=displayedScripts[scriptsUI.getSelectedIndex()];
		int value=context.getSingle(UI.class).showQuestion(w,"Confirm delete",
			"Are you sure you want to delete the script <strong>"+
			XML.esc(script.getName())+"</strong>? Deleted scripts cannot be restored.",
			UI.BUTTON_YES|UI.BUTTON_CANCEL,"Delete script",null,null,UI.BUTTON_CANCEL);
		if(value==UI.BUTTON_YES)
		{
			// See if we have an editor for that script, if we do it has to go
			ScriptEditor editor=editors.get(script);
			if(editor!=null) editor.closing();
			((ScriptingPlugin)context.getPlugin()).deleteScript(script);
			updateScripts();
		}
	}

	/**
	 * Called by plugin when a script is marked changed or unchanged. We use it
	 * to grey out the Enabled checkbox and other items.
	 * @param script Script that is different
	 */
	public void informChanged(Script script)
	{
		if(w!=null && displayedScripts!=null)
		{
			for(int i=0;i<displayedScripts.length;i++)
			{
				if(displayedScripts[i]==script)
				{
					updateScriptDetails(i,script);
					selectScripts();
					return;
				}
			}
		}
	}
}
