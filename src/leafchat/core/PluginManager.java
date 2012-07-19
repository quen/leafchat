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
package leafchat.core;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import util.*;
import util.xml.XMLException;
import leafchat.core.api.*;
import leafchat.startup.*;

/** Class handles all the plugins */
public class PluginManager implements APIClassLocator,MsgOwner,PluginList
{
	// Handle singleton behaviour (note: as this is not accessible to plugins,
	// it doesn't use the SingletonManager features)

	/** Single instance */
	private static PluginManager pm=new PluginManager();

	/** @return Singleton instance */
	public static PluginManager get() { return pm; }

	/** Private constructor to prevent separate construction */
	private PluginManager()
	{
		MessageManager.get().registerOwner(this);
	}

	// Actual implementation

	/** Map of name -> PluginClassLoader for all API classes */
	private Map<String, PluginClassLoader> apiClasses =
		new HashMap<String, PluginClassLoader>();

	/** Set of PluginClassLoader */
	private Set<PluginClassLoader> loadedJars =
		new TreeSet<PluginClassLoader>(); // Sorted for nice display in lists

	/** List of PluginContextProvider */
	private List<PluginContextProvider> pluginList =
		new LinkedList<PluginContextProvider>();

	/** List of PluginClassLoader being loaded (cleared after init) */
	private List<PluginClassLoader> loadingJar =
		new LinkedList<PluginClassLoader>();

	private LinkedList<PluginInfo> fakePluginInfos=null;

	/**
	 * @param sName Classname
	 * @return The class
	 * @throws ClassNotFoundException If it isn't an existing API class
	 */
	@Override
	public Class<?> findAPIClass(String sName) throws ClassNotFoundException
	{
		PluginClassLoader pcl;
		synchronized(apiClasses)
		{
			pcl=apiClasses.get(sName);
			if(pcl==null)
				throw new ClassNotFoundException(sName);
		}
		return pcl.getAPIClass(sName);
	}

	/**
	 * Called from PluginClassLoader to register each API class
	 * @param pcl Classloader
	 * @param sClassName Provided classname
	 * @throws GeneralException If class of that name already exists
	 */
	void addAPIClass(PluginClassLoader pcl, String sClassName)
	  throws GeneralException
	{
		synchronized(apiClasses)
		{
			if(apiClasses.containsKey(sClassName))
			  throw new GeneralException("Defined class "+sClassName+" from "+pcl+
					" already exists in "+apiClasses.get(sClassName));

			apiClasses.put(sClassName,pcl);
		}
	}

	/**
	 * Loads a new jar.
	 * @param f File to load
	 * @param sandbox True for sandbox (non-system) mode; at present this is not
	 * implemented in a security sense, but it still marks the plugin as not a
	 * system one
	 * @return New class loader
	 * @throws GeneralException
	 */
	private PluginClassLoader loadJar(File f, boolean sandbox) throws GeneralException
	{
		synchronized(loadedJars)
		{
			// Constructing the PCL is going to end up calling addAPIClass
			try
			{
				PluginClassLoader pcl=new PluginClassLoader(this, f, sandbox);
				loadedJars.add(pcl);
				loadingJar.add(pcl);
				return pcl;
			}
			catch(IOException e)
			{
				throw new GeneralException("Error loading plugin " + f.getAbsolutePath()
					+ ": " + e.getMessage(), e);
			}
		}
	}

	@Override
	public void unloadPluginFile(PluginInfo plugin) throws GeneralException
	{
		// Remove from list of loaded jars
		PluginClassLoader found = null;
		synchronized(loadedJars)
		{
			for(PluginClassLoader pcl : loadedJars)
			{
				if(pcl.getInfo()==plugin)
				{
					found = pcl;
					break;
				}
			}
		}
		if(found == null)
		{
			throw new BugException("Couldn't find plugin for unload");
		}
		unloadPluginFile(found);
	}

	/**
	 * Unloads an existing jar.
	 * @param pcl Loader to unload
	 * @throws GeneralException Any error
	 */
	public void unloadPluginFile(PluginClassLoader pcl) throws GeneralException
	{
		// Remove from list of loaded jars
		synchronized(loadedJars)
		{
			loadedJars.remove(pcl);
		}

		// Remove API classes
		synchronized(apiClasses)
		{
			for(Iterator<Map.Entry<String, PluginClassLoader>> i =
				apiClasses.entrySet().iterator(); i.hasNext();)
			{
				Map.Entry<String, PluginClassLoader> me = i.next();
				if(me.getValue()==pcl)
				{
				  i.remove();
				}
			}
		}

		synchronized(pluginList)
		{
			for(Iterator<PluginContextProvider> i=pluginList.iterator(); i.hasNext();)
			{
				PluginContextProvider pcp = i.next();
				if(pcp.getPluginClassLoader()==pcl)
				{
					pcp.close();
					i.remove();
				}
			}
		}

		MessageManager.get().clearCachedItems(pcl);
	}

	/**
	 * Load all .leafChatPlugin files from a folder
	 * @param fFolder Folder
	 * @param bSecure True if security restrictions apply
	 * @param plr Methods of this reporter will be called to inform progress
	 */
	private void loadFolder(File fFolder,boolean bSecure,PluginLoadReporter plr)
	{
		if(!fFolder.isDirectory()) return;
		File[] af=fFolder.listFiles();
		if(af==null) return;
		for (int i= 0; i < af.length; i++)
		{
			if(!af[i].getName().endsWith(".jar")) continue;
			try
			{
				plr.reportLoading(af[i]);
				loadJar(af[i],bSecure);
			}
			catch(GeneralException ge)
			{
				plr.reportFailure(af[i],ge);
			}
		}
	}

	/**
	 * Call to initialise system by loading all plugins and creating them in
	 * appropriate dependency order.
	 * @param plr Methods of this reporter will be called to inform progress
	 */
	public void init(PluginLoadReporter plr)
	{
		// Load all jars
		loadFolder(new File("./core"),false,plr);
		loadFolder(new File("./plugins"),true,plr);
		File userPlugins=new File(PlatformUtils.getUserFolder()+"/plugins");
		userPlugins.mkdirs();
		loadFolder(userPlugins,true,plr);

		// Init plugins
		initPlugins(null,plr);
	}

	/**
	 * Instantiate all loaded PluginInfos (in lLoadingPluginInfo), which
	 * afterwards will be cleared
	 * @param supportedApis Pre-existing supported APIs, or null
	 * @param plr Load reporter, may be null, if it is then a GeneralException
	 *   will be returned in the event of error.
	 * @return GeneralException if error occurred and plr==null, otherwise null
	 */
	private GeneralException initPlugins(Set<String> supportedApis,
		PluginLoadReporter plr)
	{
		if(supportedApis==null)
		{
			supportedApis=new HashSet<String>();
		}

		// Keep looping around until there are none left
		while(true)
		{
			int iBefore=loadingJar.size();

			for(Iterator<PluginClassLoader> i=loadingJar.iterator();i.hasNext();)
			{
				PluginClassLoader pcl = i.next();

				APIDetails[] aapiDependencies=pcl.getInfo().getDependencies();
				boolean bLater=false;
				for (int iDependency= 0; iDependency < aapiDependencies.length; iDependency++)
				{
					if(!supportedApis.contains(aapiDependencies[iDependency].getRequiredString()))
					{
						bLater=true;
						break;
					}
				}

				if(!bLater)
				{
					try
					{
						// Remove pcl from list
						i.remove();

						// Create plugin
						if(plr!=null) plr.reportInstantiating(pcl);
						Plugin[] ap=pcl.createPlugins();
						for(int iPlugin=0;iPlugin<ap.length;iPlugin++)
						{
							PluginContextProvider pcp=new PluginContextProvider(this,ap[iPlugin]);
							synchronized(pluginList)
							{
								pluginList.add(pcp);
							}
							ap[iPlugin].init(pcp,plr);
						}

						// Track the APIs that are now supported
						APIDetails[] aapiProvided=pcl.getInfo().getExports();
						for (int iProvision= 0; iProvision < aapiProvided.length; iProvision++)
						{
							aapiProvided[iProvision].addSupportStrings(supportedApis);
						}
					}
					catch(GeneralException ge)
					{
						if(plr!=null)
						  plr.reportFailure(pcl,ge);
						else
						  return ge;
					}
				}
			}

			// If we loaded everything, stop
			int iAfter=loadingJar.size();
			if(iAfter==0) break;

			// If there are files we can't load (dependency failures)
			if(iAfter==iBefore)
			{
				for(PluginClassLoader pcl : loadingJar)
				{
					List<String> failedDependencies = new LinkedList<String>();

					APIDetails[] aapiDependencies=pcl.getInfo().getDependencies();
					for (int iDependency= 0; iDependency < aapiDependencies.length; iDependency++)
					{
						if(!supportedApis.contains(aapiDependencies[iDependency].getRequiredString()))
						{
							failedDependencies.add(aapiDependencies[iDependency].getRequiredString());
						}
					}

					if(plr!=null)
					{
						plr.reportFailure(pcl,
							failedDependencies.toArray(new String[0]));
					}
					else
					{
						StringBuffer sb=new StringBuffer("Plugin could not be instantiated " +
							"because the following dependencies are not present:");
						for(String dependency : failedDependencies)
						{
							sb.append(" "+ dependency);
						}
						return new GeneralException(sb.toString());
					}
				}
				loadingJar.clear();
				break;
			}
		}
		return null;
	}

	/**
	 * Hack plugin load when actually they're all in system classpath to
	 * begin with (for IDEStartupHandler)
	 * @param p Plugin
	 * @param plr Load reporter
	 * @throws GeneralException
	 */
	public void fakePluginInit(Plugin p, PluginLoadReporter plr)
		throws GeneralException
	{
		PluginContextProvider pcp=new PluginContextProvider(this,p);
		synchronized(pluginList)
		{
			pluginList.add(pcp);
			if(fakePluginInfos==null)
			{
				fakePluginInfos = new LinkedList<PluginInfo>();
			}
			try
			{
				fakePluginInfos.add(new PluginXMLDetails(
					p.getClass().getResourceAsStream("plugininfo.xml"),
					new File(StartupClassLoader.getIdeStartupTemplateApp().getPath() + "/core/"+
						p.getClass().getPackage().getName().replaceAll("^.*\\.","")+".jar"),true));
			}
			catch(XMLException e)
			{
				throw new GeneralException(e);
			}
		}

		p.init(pcp, plr);
	}

	/**
	 * Same as other method but with secure defaulting to false.
	 * @see #loadPluginFile(File, boolean)
	 * @param f Jar file
	 * @return Loaded plugin
	 * @throws GeneralException Any problems
	 */
	@Override
	public PluginInfo loadPluginFile(File f) throws GeneralException
	{
		return loadPluginFile(f,true).getInfo();
	}

	/**
	 * Load a plugin that has been added since init
	 * @param f Jar file
	 * @param secure True if secure
	 * @return Loaded plugin
	 * @throws GeneralException Any problems
	 */
	public PluginClassLoader loadPluginFile(File f,boolean secure) throws GeneralException
	{
		PluginClassLoader pclThis=loadJar(f,secure);

		// Get list of existing supported APIs
		Set<String> supportedAPIs = new HashSet<String>();
		PluginInfo[] info=getPluginList();
		for(int i=0;i<info.length;i++)
		{
			PluginExport[] exports=info[i].getPluginExports();
			for(int j=0;j<exports.length;j++)
			{
				((APIDetails)exports[j]).addSupportStrings(supportedAPIs);
			}
		}

		GeneralException ge=initPlugins(supportedAPIs,null);
		if(ge!=null) throw ge;
		return pclThis;
	}

	/** Close all plugins in preparation for shutdown */
	public void close()
	{
		PluginClassLoader[] apcl;
		synchronized(loadedJars)
		{
			apcl=loadedJars.toArray(new PluginClassLoader[0]);
		}
		for(int i=apcl.length-1;i>=0;i--)
		{
			try
			{
				unloadPluginFile(apcl[i]);
			}
			catch (GeneralException ge)
			{
				try
				{
					(SingletonManager.get().get(SystemLog.class)).log(
					  this,"Unload failed for "+apcl[i],ge);
				}
				catch (BugException e)
				{
				}
			}
		}

		// Are there still plugins? (This is caused by the fake plugin init.)
		synchronized(pluginList)
		{
			PluginContextProvider[] plugins=pluginList.toArray(new PluginContextProvider[pluginList.size()]);
			for(int i=plugins.length-1;i>=0;i--)
			{
				try
				{
					plugins[i].getPlugin().close();
				}
				catch(GeneralException ge)
				{
					try
					{
						(SingletonManager.get().get(SystemLog.class)).log(
						  this,"Close failed for "+plugins[i],ge);
					}
					catch (BugException e)
					{
					}
				}
			}
		}

	}

	// Message dispatcher for plugin unload messages

	void firePluginUnload(Plugin p)
	{
		mdp.dispatchMessage(new PluginUnloadMsg(p),true);
	}

	@Override
	public boolean allowExternalDispatch(Msg m)
	{
		return false;
	}

	@Override
	public String getFriendlyName()
	{
		return "Plugin unload notification";
	}

	@Override
	public Class<? extends Msg> getMessageClass()
	{
		return PluginUnloadMsg.class;
	}

	private MessageDispatch mdp;

	@Override
	public void init(MessageDispatch mdp)
	{
		this.mdp=mdp;
	}

	@Override
	public void manualDispatch(Msg m)
	{
	}

	@Override
	public boolean registerTarget(Object oTarget, Class<? extends Msg> cMessage,
		MessageFilter mf,int iRequestID,int iPriority)
	{
		return true;
	}

	@Override
	public void unregisterTarget(Object oTarget,int iRequestID)
	{
	}

	@Override
	public PluginInfo[] getPluginList()
	{
		List<PluginInfo> result = new LinkedList<PluginInfo>();

		// Add real plugins
		synchronized(loadedJars)
		{
			for(PluginClassLoader pcl : loadedJars)
			{
				result.add(pcl.getInfo());
			}
		}
		// If we have any fake ones, add them too
		if(fakePluginInfos!=null)
		{
			result.addAll(fakePluginInfos);
		}

		return result.toArray(new PluginInfo[result.size()]);
	}

	private PluginClassLoader findExportedPackage(String packageName)
	{
		for(PluginClassLoader pcl : loadedJars)
		{
			PluginExport[] exports=pcl.getInfo().getPluginExports();
			for(int j=0;j<exports.length;j++)
			{
				if(exports[j].getPackage().equals(packageName))
					return pcl;
			}
		}
		return null;
	}

	@Override
	public void saveAPIJar(String[] packages,File target) throws GeneralException
	{
		try
		{
			// If the packages list is null, build it from every plugin
			if(packages==null)
			{
				LinkedList<String> l = new LinkedList<String>();
				for(PluginClassLoader pcl : loadedJars)
				{
					PluginInfo pi = pcl.getInfo();
					PluginExport[] exports=pi.getPluginExports();
					for(int j=0;j<exports.length;j++)
					{
						l.add(exports[j].getPackage());
					}
				}
				packages=l.toArray(new String[l.size()]);
			}

			// Open target file
			JarOutputStream jos=new JarOutputStream(new FileOutputStream(target));

			// Find system jar file (used to load this class)
			File main = StartupClassLoader.getMainJar();
		  copyClasses(jos, main, null); // Do main API
		  File util = StartupClassLoader.getUtilJar();
		  copyClasses(jos, util, "util"); // Do util.*

			// Copy each file into it
			for(int i=0;i<packages.length;i++)
			{
				// Find the package
				PluginClassLoader pcl=findExportedPackage(packages[i]);
				if(pcl==null) throw new GeneralException("Couldn't find requested package: "+packages[i]);
				copyClasses(jos,pcl.getJar(),packages[i].replace('.','/'));
			}

			// Close jar file
			jos.close();
		}
		catch(IOException e)
		{
			throw new GeneralException("Error saving API jar",e);
		}
	}

	@Override
	public File getCoreJar()
	{
		throw new UnsupportedOperationException("Cannot use this method");
	}

	@Override
	public File[] getCoreJars()
	{
		return StartupClassLoader.getMainJars();
	}

	/**
	 * Copies data from one jar file into another.
	 * @param jos Target jar stream
	 * @param source Source file
	 * @param path If null, includes all 'api' folders within file. Otherwise
	 *   includes everything below specified path.
	 * @throws IOException Any disk error
	 */
	private void copyClasses(JarOutputStream jos,File source,String path)
		throws IOException
	{
		JarFile jf=new JarFile(source);
		for(Enumeration<JarEntry> e=jf.entries(); e.hasMoreElements();)
		{
			JarEntry entry=e.nextElement();
			String name=entry.getName();
			if( (path==null && name.indexOf("/api/")!=-1) ||
  				  (path!=null && name.startsWith(path+"/")))
			{
				jos.putNextEntry(entry);
				InputStream is=jf.getInputStream(entry);
				IOUtils.copy(is,jos,false);
				jos.closeEntry();
			}
		}
	}
}
