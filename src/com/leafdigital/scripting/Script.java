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

Copyright 2012 Samuel Marshall.
*/
package com.leafdigital.scripting;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.jar.*;

import javax.swing.SwingUtilities;

import org.w3c.dom.*;

import util.*;
import util.xml.*;
import leafchat.core.api.*;

/**
 * Represents a 'script', an XML file defining a collection of actions expressed
 * in a combination of data and Java code, which can be compiled into a
 * leafChat plugin.
 */
public class Script
{
	private final static boolean DEBUG_PRINT_SOURCE = false;

	private PluginContext context;

	/** Script file (.leafChatScript) - may not exist yet */
	private File f;

	/** Corresponding jar file (.leafChatScript.jar) - may not exist yet */
	private File jar;

	/** Errors file - may not exist */
	private File errors,errorsSource;

	/** If true, this script is loaded and runs */
	private boolean enabled=true;

	/** If currently loaded, the script's PluginInfo is saved here */
	private PluginInfo installed=null;

	/** True if changes have been made that require save */
	private boolean changed=false;

	/** List of items in the script */
	private LinkedList<ScriptItem> items;

	/** List of dependencies (does not include system plugins) */
	private LinkedList<Dependency> dependencies;

	/** Information for a dependency */
	private static class Dependency
	{
		String packageName;
		int version;
	}

	static final String ALLOWED_NAMES="[A-Za-z 0-9]+\\.leafChatScript";

	private LinkedList<StateListener> stateListeners =
		new LinkedList<StateListener>();

	/**
	 * Construct script from a particular file, which may exist or not. If it
	 * exists, items from the file will be loaded.
	 * @param context Plugin context
	 * @param f File to load
	 * @throws GeneralException Any problems loading the file
	 */
	Script(PluginContext context,File f) throws GeneralException
	{
		// Ensure name is standard and safe (it is used to generate classnames)
		if(!f.getName().matches(ALLOWED_NAMES))
			throw new GeneralException("Invalid script filename: "+f.getName());

		this.context=context;
		this.f=f;

		this.jar=new File(f.getPath()+".jar");
		this.errors=new File(f.getPath()+".errors.xml");
		this.errorsSource=new File(f.getPath()+".errors.java");

		load();

		if(enabled && jar.exists())	install();
	}

	/** Number of attempts to delete files (which tend to be inexplicably locked) */
	private final static int DELETEATTEMPTS=3;

	private void internalDelete(File going,boolean gc) throws GeneralException
	{
		if(going.delete()) return;
		for(int i=0;i<DELETEATTEMPTS;i++)
		{
			try
			{
				if(gc) System.gc();
				Thread.sleep(250);
			}
			catch(InterruptedException ie)
			{
			}
			going.delete();
			if(!going.exists()) return;
		}
		throw new GeneralException("Failed to delete "+going);
	}

	/**
	 * Deletes the script from disk and memory.
	 * @throws GeneralException Any error
	 */
	void delete() throws GeneralException
	{
		uninstall();
		internalDelete(f,false);
		if(hasJar())
		{
			internalDelete(jar,true);
		}
		if(hasErrors())
		{
			internalDelete(errors,false);
			internalDelete(errorsSource,false);
		}
	}

	PluginContext getContext()
	{
		return context;
	}

	/** Number of errors */
	private int errorCount=0;

	/** @return Number of errors in error file, or 0 if none */
	int getErrorCount()
	{
		return errorCount;
	}

	/**
	 * In-memory initialises any errors from the given file.
	 * @param resultFile Eclipse compiler result file
	 * @param source Original source file
	 * @return True if compile was a success, false if it failed
	 * @throws GeneralException Any error
	 */
	private boolean readResultFile(File resultFile,File source) throws GeneralException
	{
		// Clear existing error markings
		for(ScriptItem item :  items)
		{
			item.clearErrors();
		}

		// Load results
		try
		{
			String resultString;
			try
			{
				resultString=IOUtils.loadStringPlatformEncoding(new FileInputStream(resultFile));
			}
			catch(IOException e)
			{
				throw new BugException(e);
			}
			resultString=resultString.replaceAll("<!DOCTYPE[^>]*>","");
			Document results=XML.parse(resultString);
			Element stats=XML.getChild(results.getDocumentElement(),"stats");
			if(XML.hasChild(stats,"problem_summary"))
				errorCount=XML.getIntAttribute(XML.getChild(stats,"problem_summary"),
					"errors");
			else
				errorCount=0;
			if(errorCount==0)
				return true;

			try
			{
				// Load source file
				String[] code=IOUtils.loadString(new FileInputStream(source)).split("\n");

				// Loop through all the problems
				Element[] problems=XML.getChildren(XML.getChild(XML.getChild(
					XML.getChild(results.getDocumentElement(),"sources"),"source"),"problems"),"problem");
				for(int i=0;i<problems.length;i++)
				{
					if(problems[i].getAttribute("severity").equals("ERROR"))
					{
						String message="";
						if(XML.hasChild(problems[i],"message"))
						{
							message=XML.getRequiredAttribute(XML.getChild(problems[i],"message"),"value");
						}
				    markError(code,Integer.parseInt(problems[i].getAttribute("line")),message);
					}
				}
			}
			catch(IOException e)
			{
				throw new GeneralException(e);
			}

			return false;
		}
		catch(XMLException e)
		{
			throw new BugException(e);
		}
	}

	private boolean markError(String[] code,int line,String message)
	{
		// Find error location, converted into item and user code line
		int item=-1,userCode=-2;
		for(int check=line-1;check>=0;check--)
		{
			String checkLine=code[check];
			if(checkLine.startsWith("// ----^")) // Ooops, looks like we weren't in an item
			{
				return false;
			}
			if(checkLine.startsWith("// ----v")) // Aha, found item we're in
			{
				item=Integer.parseInt(checkLine.substring(8));
				break;
			}
			if(userCode==-2) // Not ascertained user code yet
			{
				if(checkLine.startsWith("// ====v")) // Aha, found user code we're in
				{
					userCode=Integer.parseInt(checkLine.substring(8));
				}
				if(checkLine.startsWith("// ====^")) // Ooops, not in user code
				{
					userCode=-1;
				}
			}
		}
		if(item==-1) return false;

		getItems()[item].markError(userCode>=0 ? userCode : ScriptItem.NOTINUSERCODE,message);
		return true;
	}

	/**
	 * Installs the script into the system. Script jar must exist. Does nothing
	 * if already installed.
	 * @throws GeneralException If jar doesn't exist or something else is wrong
	 */
	private void install() throws GeneralException
	{
		if(installed!=null) return;
		if(!jar.exists()) throw new GeneralException("Can't install script when jar file doesn't exist: "+jar);
		PluginList pl=context.getSingle(PluginList.class);
	  installed=pl.loadPluginFile(jar);
	}

	/**
	 * Uninstalls script from system. Does nothing if not already installed
	 * @throws GeneralException Any errors while uninstalling
	 */
	private void uninstall() throws GeneralException
	{
		if(installed==null) return;
		PluginList pl=context.getSingle(PluginList.class);
	  pl.unloadPluginFile(installed);
	  installed=null;
	  System.gc();
	  try
		{
			Thread.sleep(500);
		}
		catch(InterruptedException ie)
		{
		}
	  System.gc();
	}

	/**
	 * Saves the current state of the Script object to the file.
	 * @throws GeneralException
	 */
	private void internalSave() throws GeneralException
	{
		try
		{
			File target=new File(f.getPath()+".new");
			Document d=XML.newDocument("script");
			Element root=d.getDocumentElement();
			root.setAttribute("version","1");
			root.setAttribute("enabled",enabled?"y":"n");

			Element dependenciesRoot=XML.createChild(root,"dependencies");
			for(Dependency dep : dependencies)
			{
				Element api=XML.createChild(dependenciesRoot,"api");
				XML.setText(XML.createChild(api,"package"),dep.packageName);
				XML.setText(XML.createChild(api,"version"),dep.version+"");
			}

			Element itemRoot=XML.createChild(root,"items");
			for(ScriptItem item :  items)
			{
				item.save(XML.createChild(itemRoot,item.getTag()));
			}

			XML.save(target,d);
			// If there is an old file, rename it before deleting it in case that
			// avoids rare problem on Windows where we can't rename to the target
			// immediately after deleting it. (Speculation based on automatic error
			// report.)
			File old = new File(f.getPath() + ".old");
			if(old.exists())
			{
				if(!old.delete())
				{
					throw new IOException("Failed to delete old script");
				}
			}
			if(f.exists())
			{
				if(!f.renameTo(old))
				{
					throw new IOException("Failed to rename existing script");
				}
				old.delete(); // Not too bothered if this one fails.
			}
			if(!target.renameTo(f))
			{
				throw new IOException("Couldn't rename to final destination from "+target);
			}
			markUnchanged();
		}
		catch(IOException e)
		{
			throw new GeneralException("Script file "+f+" could not be saved: "+e.getMessage(),e);
		}
	}

	static interface StateListener
	{
		/**
		 * Called when the script's state (enabled/disabled, modified/unmodified)
		 * changes.
		 * @param s Script that has changed
		 */
		public void scriptStateChanged(Script s);
	}

	synchronized void addStateListener(StateListener l)
	{
		stateListeners.add(l);
	}
	synchronized void removeStateListener(StateListener l)
	{
		stateListeners.remove(l);
	}

	/** Sets the changed flag */
	synchronized void markChanged()
	{
		if(changed) return;
		changed=true;
		fireStateListeners();
	}

	synchronized private void fireStateListeners()
	{
		StateListener[] listeners=stateListeners.toArray(new StateListener[stateListeners.size()]);
		for(int i=0;i<listeners.length;i++)
		{
			listeners[i].scriptStateChanged(this);
		}
	}

	synchronized private void markUnchanged()
	{
		if(!changed) return;
		changed=false;
		fireStateListeners();
	}

	/** @return True if script has changed and not been saved */
	boolean isChanged()
	{
		return changed;
	}

	/** @return True if script is enabled for use */
	boolean isEnabled()
	{
		return enabled;
	}

	/** @return True if a compiled jar file exists */
	boolean hasJar()
	{
		return jar.exists();
	}

	/** @return Main script file */
	File getFile()
	{
		return f;
	}

	/** @return True if an error report file exists */
	boolean hasErrors()
	{
		return errors.exists() && errorsSource.exists();
	}

	/** @return Name of script */
	String getName()
	{
		return f.getName().replaceAll("\\.leafChatScript$","");
	}

	/**
	 * Saves Java source code corresponding to this script into the compile folder.
	 * Automatically called by {@link #compile()}.
	 * @param compileFolder Base folder for compile
	 * @return Class name
	 * @throws IOException File error saving source
	 */
	private String saveSource(File compileFolder) throws IOException
	{
		String source;
		try
		{
			source=IOUtils.loadString(getClass().getResourceAsStream("userscript.source.txt"));
		}
		catch(IOException e)
		{
			throw new BugException("Failed to load script template");
		}

		// Do imports for system plugins
		PluginList pl=context.getSingle(PluginList.class);
		PluginInfo[] info=pl.getPluginList();
		StringBuffer sb=new StringBuffer();
		for(int i=0;i<info.length;i++)
		{
			if(info[i].isSystem())
			{
				PluginExport[] exports=info[i].getPluginExports();
				for(int j=0;j<exports.length;j++)
				{
					sb.append("import "+exports[j].getPackage()+".*;\n");
				}
			}
		}
		source=StringUtils.replace(source,"%%SYSTEMIMPORTS%%",sb.toString());

		// Do imports for specifically-marked dependency API packages
		sb=new StringBuffer();
		for(Dependency dep : dependencies)
		{
			sb.append("import "+dep.packageName+".*;\n");
		}
		source=StringUtils.replace(source,"%%DEPENDENCYIMPORTS%%",sb.toString());

		// Any other imports?
		sb=new StringBuffer();
		for(ScriptItem item :  items)
		{
			if(!item.isEnabled()) continue;
			sb.append(markOwningItem(item,item.getSourceImports()));
		}
		source=StringUtils.replace(source,"%%ITEMIMPORTS%%",sb.toString());

		// Class name
		String className=
			"UserScript"+
			StringUtils.capitalise(f.getName().replaceAll("\\..*$","")).
				replaceAll("[^A-Za-z0-9]","");
		source=StringUtils.replace(source,"%%CLASSNAME%%",className);

		// Fields
		sb=new StringBuffer();
		for(ScriptItem item :  items)
		{
			if(!item.isEnabled()) continue;
			sb.append(markOwningItem(item,item.getSourceFields()));
		}
		source=StringUtils.replace(source,"%%ITEMFIELDS%%",sb.toString());

		// Init method
		sb=new StringBuffer();
		for(ScriptItem item :  items)
		{
			if(!item.isEnabled()) continue;
			sb.append(markOwningItem(item,item.getSourceInit()));
		}
		source=StringUtils.replace(source,"%%ITEMINIT%%",sb.toString());

		// Close method
		sb=new StringBuffer();
		for(ScriptItem item :  items)
		{
			if(!item.isEnabled()) continue;
			sb.append(markOwningItem(item,item.getSourceClose()));
		}
		source=StringUtils.replace(source,"%%ITEMCLOSE%%",sb.toString());

		// Methods
		sb=new StringBuffer();
		for(ScriptItem item :  items)
		{
			if(!item.isEnabled()) continue;
			sb.append(markOwningItem(item,item.getSourceMethods()));
		}
		source=StringUtils.replace(source,"%%ITEMMETHODS%%",sb.toString());

		if(DEBUG_PRINT_SOURCE)
		{
			System.out.println("\n\n"+source+"\n\n");
		}

		File target=new File(compileFolder,className+".java");
		IOUtils.saveString(source,new FileOutputStream(target));
		return className;
	}

	/**
	 * Marks source code that belongs to a particular item.
	 * @param item Item that owns code
	 * @param text Source being marked
	 * @return New source
	 */
	private static String markOwningItem(ScriptItem item,String text)
	{
		if(text==null)
			return "";
		else
		{
			// Trim \n from start and end of string just to tidy it up (and don't
			// call trim() because that gets rid of indent tabs).
			while(text.startsWith("\n")) text=text.substring(1);
			while(text.endsWith("\n")) text=text.substring(0,text.length()-1);
			return "// ----v"+item.getIndex() +"\n"+ text+"\n// ----^"+item.getIndex()+"\n";
		}
	}

	private final static Object COMPILESYNCH=new Object();

	/**
	 * Saves source and compiles script to a temporary jar file. Call
	 * {@link #setupJar()} to make this jar into the real one.
	 * @return True if compile was successful, false if there were errors.
	 * @throws GeneralException
	 */
	private boolean compile() throws GeneralException
	{
		File compile=null;
		try
		{
			// Create compilation folder
			String rand=(Math.random()+"").replaceAll("\\.","").replaceFirst("^0","");
			compile=new File(System.getProperty("java.io.tmpdir"),"leafChat.compile."+rand);
			compile.mkdirs();

			// Save file in folder
			String className=saveSource(compile);
			File java=new File(compile,className+".java");

			// Work out classpath
			PluginList pl=context.getSingle(PluginList.class);
			String classPath = "";
			for(File file : pl.getCoreJars())
			{
				if(classPath.length() > 0)
				{
					classPath += System.getProperty("path.separator");
				}
				classPath += file.getAbsolutePath();
			}
			PluginInfo[] plugins=pl.getPluginList();
			for(int i=0;i<plugins.length;i++)
			{
				classPath+=System.getProperty("path.separator")+
					plugins[i].getJar().getAbsolutePath();
			}

			// Compile it
			File buildErrors=getBuildErrors();
			synchronized(COMPILESYNCH)
			{
				org.eclipse.jdt.internal.compiler.batch.Main.main(new String[] {
					"-nowarn","-classpath",classPath,
					"-sourcepath",compile.getAbsolutePath(),"-d",compile.getAbsolutePath(),
					"-target","1.4","-encoding", "UTF-8","-log",buildErrors.getAbsolutePath(),"-noExit",
					java.getAbsolutePath()});
			}

			if(!readResultFile(buildErrors,java))
			{
				// Copy source to store it
				IOUtils.copy(new FileInputStream(java),new FileOutputStream(getBuildErrorsSource()),true);
				return false;
			}

			// Delete errors file, no errors
			buildErrors.delete();

			// Uninstall existing version, if loaded
			uninstall();

			// Put classes in jar file
		  JarOutputStream jos=new JarOutputStream(new FileOutputStream(		getBuildJar()));
		  makeJar(jos,compile,"");

		  // Build plugininfo
			String xml=IOUtils.loadString(ScriptingTool.class.getResourceAsStream("userscript.metadata.xml"));

			// Class name
			xml=xml.replaceAll("%%CLASSNAME%%",className);

			// Dependencies
			StringBuffer dependencyXML=new StringBuffer();
			PluginInfo[] info=pl.getPluginList();
			for(int i=0;i<info.length;i++)
			{
				if(!info[i].isSystem()) continue; // Only require all system plugins
				PluginExport[] exports=info[i].getPluginExports();
				for(int j=0;j<exports.length;j++)
				{
					dependencyXML.append("<api><package>"+
						exports[j].getPackage()+"</package><version>"+
						exports[j].getMaxVersion()+
						"</version></api>");
				}
			}
			for(Dependency dep : dependencies)
			{
				dependencyXML.append("<api><package>"+
					dep.packageName+"</package><version>"+
					dep.version+	"</version></api>");
			}
			xml=xml.replaceAll("%%DEPENDENCIES%%",dependencies.toString());

			// Save plugin info
			JarEntry je=new JarEntry("plugininfo.xml");
			jos.putNextEntry(je);
			IOUtils.copy(new ByteArrayInputStream(xml.getBytes("UTF-8")),jos,false);
			jos.closeEntry();

			// Finish jar file
		  jos.close();

		  // Successful compile and jar
		  return true;
		}
		catch(IOException e)
		{
			throw new GeneralException(e);
		}
		finally
		{
			// Clean up compile folder
			try
			{
				if(compile.exists()) IOUtils.recursiveDelete(compile);
			}
			catch(IOException ioe)
			{
			}
		}
	}

	/** @return Jar file created in temporary builds	 */
	private File getBuildJar()
	{
		return new File(jar.getParentFile(),jar.getName()+".new");
	}

	/** @return Errors file created in temporary builds	 */
	private File getBuildErrors()
	{
		return new File(errors.getParentFile(),errors.getName()+".new.xml");
	}

	/** @return Source file created in temporary builds	 */
	private File getBuildErrorsSource()
	{
		return new File(errorsSource.getParentFile(),errorsSource.getName()+".new.java");
	}

	/**
	 * Adds entries to a jar file from a disk folder. Does not close stream.
	 * @param jos Stream into which new entries will be stuffed
	 * @param folder Folder to search
	 * @param prefix Used in recursive calls, initial value ""
	 * @throws IOException
	 */
	private static void makeJar(JarOutputStream jos,File folder,String prefix)
	  throws IOException
	{
		File[] files=IOUtils.listFiles(folder);
		for(int i=0;i<files.length;i++)
		{
			if(files[i].isDirectory())
				makeJar(jos,files[i],prefix+files[i].getName()+"/");
			else if(files[i].getName().endsWith(".class"))
			{
			  JarEntry je=new JarEntry(prefix+files[i].getName());
			  jos.putNextEntry(je);
			  IOUtils.copy(new FileInputStream(files[i]),jos,false);
			  jos.closeEntry();
			}
		}
	}

	/**
	 * Enables or disables the script. This can only be done when the script is
	 * not being edited i.e. when isChanged() returns false. The change to enable
	 * state is saved directly to disk.
	 * @param enabled New enable state
	 * @throws GeneralException If there is an error when compiling the script
   * @throws BugException If you call this when there are unsaved changes
	 */
	void setEnabled(boolean enabled) throws GeneralException
	{
		if(this.enabled==enabled) return;
		if(isChanged()) throw new BugException("Cannot alter enabled setting while changes are unsaved");
		if(enabled)
		{
			if(!jar.exists())
			{
				if(compile())
					setupJar();
				else
					throw new GeneralException("Cannot enable script that has errors");
			}

			this.enabled=true;
			internalSave();
			install();
		}
		else
		{
			this.enabled=false;
			internalSave();
			uninstall();
		}
		fireStateListeners();
	}

	/**
	 * Interface for continuation so that UI can make it do something after
	 * save.
	 */
	public interface SaveContinuation
	{
		/**
		 * Called after save with expected result.
		 * @param success True if save is successful
		 */
		public void afterSave(boolean success);
		/**
		 * Called after save with unexpected error.
		 * @param t Error exception
		 */
		public void afterSave(Throwable t);
	}

	/**
	 * Compiles and saves the script. If there is an error, passes false
	 * to the continuation, in which case you can show UI then {@link #saveAndDisable()}.
	 * @param sc Continuation which controls what happens after save
	 * @throws GeneralException If something goes wrong (other than compile error)
	 */
	void save(final SaveContinuation sc) throws GeneralException
	{
		(new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					if(compile())
					{
						SwingUtilities.invokeLater(new Runnable()
						{
							@Override
							public void run()
							{
								try
								{
									deleteErrors();
									internalSave();
									uninstall();
									setupJar();
									if(isEnabled()) install();
									sc.afterSave(true);
								}
								catch(Throwable t)
								{
									sc.afterSave(t);
								}
							}
						});
					}
					else
					{
						SwingUtilities.invokeLater(new Runnable()
						{
							@Override
							public void run()
							{
								sc.afterSave(false);
							}
						});
					}
				}
				catch(final Throwable t)
				{
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							sc.afterSave(t);
						}
					});
				}
			}
		},"Script compile thread")).start();
	}

	/**
	 * Disables the script and saves it, without compiling. May only be called
	 * if errors occurred during compile.
	 * @throws GeneralException If anything goes wrong with save or there wasn't
	 *   an errors file
	 */
	void saveAndDisable() throws GeneralException
	{
		enabled=false;
		internalSave();
		uninstall();
		deleteJar();
		setupErrors();
	}

	/**
	 * Renames the build jar file to replace the current actual jar.
	 * @throws GeneralException Any error
	 */
	private void setupJar()	throws GeneralException
	{
		deleteJar();
		if(!getBuildJar().renameTo(jar))
			throw new GeneralException("Failed to install new version of script file "+jar);
		deleteErrors();
	}

	/**
	 * Renames the build errors file to replace the current actual errors file.
	 * @throws GeneralException Any error
	 */
	private void setupErrors()	 throws GeneralException
	{
		deleteErrors();
		if(!getBuildErrors().renameTo(errors))
			throw new GeneralException("Failed to update stored errors "+errors);
		if(!getBuildErrorsSource().renameTo(errorsSource))
			throw new GeneralException("Failed to update stored errors source "+errorsSource);
		deleteJar();
	}

	/**
	 * Deletes current script jar file.
	 * @throws GeneralException Any error
	 */
	private void deleteJar() throws GeneralException
	{
		if(!jar.exists())
		{
			return;
		}
		File oldJar = new File(jar.getPath() + ".old");
		if(oldJar.exists())
		{
			if(!oldJar.delete())
			{
				throw new GeneralException("Failed to delete old script file " + oldJar);
			}
		}
		if(!jar.renameTo(oldJar))
		{
			throw new GeneralException("Failed to rename script file to old file " + oldJar);
		}
		oldJar.delete(); // Doesn't actually matter if this one fails
	}

	/**
	 * Deletes current script errors file.
	 * @throws GeneralException Any error
	 */
	private void deleteErrors() throws GeneralException
	{
		if(!errors.exists()) return;
		if(!errors.delete())
			throw new GeneralException("Failed to delete script errors file "+errors);
		if(!errorsSource.delete())
			throw new GeneralException("Failed to delete script errors source file "+errorsSource);
	}

	ScriptItem[] getItems()
	{
		return items.toArray(new ScriptItem[items.size()]);
	}

	/**
	 * Adds a new item to the script.
	 * @param newItem New item
	 */
	public void addItem(ScriptItem newItem)
	{
		items.add(newItem);
		markChanged();
	}

	/**
	 * Deletes an item from the script.
	 * @param item Item to be deleted
	 */
	public void deleteItem(ScriptItem item)
	{
		int index=item.getIndex();
		items.remove(item);
		for(;index<items.size();index++)
		{
			items.get(index).setIndex(index);
		}

		markChanged();
	}

	/**
	 * Reverts any changes, returning to the on-disk version.
	 * @throws GeneralException Any error
	 */
	public void load() throws GeneralException
	{
		LinkedList<ScriptItem> itemsBefore = items;
		LinkedList<Dependency> dependenciesBefore = dependencies;
		items = new LinkedList<ScriptItem>();
		dependencies = new LinkedList<Dependency>();

		if(f.exists())
		{
			try
			{
				// Load file and check basic elements
				Document d=XML.parse(f);
				Element root=d.getDocumentElement();
				if(!root.getTagName().equals("script"))
					throw new XMLException("Expected <script> tag");
				if(XML.getIntAttribute(root,"version")!=1)
					throw new XMLException("Version not supported");
				enabled=XML.getRequiredAttribute(root,"enabled").equals("y");

				// Get dependencies
				Element[] dependencyElements=XML.getChildren(XML.getChild(root,"dependencies"));
				for(int i=0;i<dependencyElements.length;i++)
				{
					if(!dependencyElements[i].getTagName().equals("api"))
						throw new XMLException("Unexpected tag inside dependencies: <"+dependencyElements[i].getTagName()+">");

					Dependency dep=new Dependency();
					dep.packageName=XML.getChildText(dependencyElements[i],"package");
					try
					{
						dep.version=Integer.parseInt(XML.getChildText(dependencyElements[i],"version"));
						dependencies.add(dep);
					}
					catch(NumberFormatException e)
					{
						throw new XMLException("Invalid version inside dependencies");
					}
				}

				// Get all the items
				Element[] itemElements=XML.getChildren(XML.getChild(root,"items"));
				for(int i=0;i<itemElements.length;i++)
				{
					Element item=itemElements[i];
					Element el=item;
					String className=
						"com.leafdigital.scripting.Item"+StringUtils.capitalise(el.getTagName());
					try
					{
						items.add(
							Class.forName(className).asSubclass(ScriptItem.class).getConstructor(
								Script.class, Element.class, int.class).
								newInstance(this, el, i));
					}
					catch(InstantiationException e)
					{
						throw new BugException("Incorrect class definition in "+className,e);
					}
					catch(IllegalAccessException e)
					{
						throw new BugException("Non-public constructor in "+className,e);
					}
					catch(InvocationTargetException e)
					{
						if(e.getCause() instanceof XMLException)
							throw (XMLException)e.getCause();
						if(e.getCause() instanceof GeneralException)
							throw (GeneralException)e.getCause();
						if(e.getCause() instanceof BugException)
							throw (BugException)e.getCause();
						throw new BugException("Unexpected exception in "+className,e.getCause());
					}
					catch(NoSuchMethodException e)
					{
						throw new BugException("Missing constructor in "+className,e);
					}
					catch(ClassNotFoundException e)
					{
						throw new XMLException("Unknown item type: <"+el.getTagName()+">");
					}
				}

				if(hasErrors())
					readResultFile(errors,errorsSource);
				markUnchanged();
			}
			catch(XMLException e)
			{
				throw new GeneralException("Script file "+f+" cannot be loaded: "+e.getMessage(),e);
			}
			catch(GeneralException e)
			{
				throw new GeneralException("Script file "+f+" cannot be loaded: "+e.getMessage(),e);
			}
			catch(BugException e)
			{
				throw new BugException("Script file "+f+" cannot be loaded: "+e.getMessage(),e);
			}
			finally
			{
				// If a revert fails, put it back to what it was before
				if(isChanged())
				{
					items=itemsBefore;
					dependencies=dependenciesBefore;
				}
			}
		}
	}
}
