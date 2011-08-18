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
package com.leafdigital.prefsui;

import java.util.*;

import com.leafdigital.prefs.api.*;
import com.leafdigital.prefsui.api.PreferencesUI;
import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/**
 * Plugin with user interface for preferences system.
 */
@UIHandler({"wizard-intro", "wizard-outro"})
public class PrefsUIPlugin implements Plugin,PreferencesUI
{
	private PluginContext pc;
	private PrefsTool pt;

	private Map<Plugin, List<Page>> pages =
		new HashMap<Plugin, List<Page>>();

	private final static String PREF_DONEWIZARD="donewizard";

	PrefsWizard pw=null;

	/** Ordered list of pages in the wizard */
	private SortedSet<WizardPage> wizardPages = new TreeSet<WizardPage>();


	@Override
	public void init(PluginContext pc, PluginLoadReporter plr) throws GeneralException
	{
		this.pc=pc;
		pt=new PrefsTool(pc);
		pc.registerSingleton(PreferencesUI.class,this);
		pc.requestMessages(PluginUnloadMsg.class,this);
		pc.requestMessages(SystemStateMsg.class,this,Msg.PRIORITY_LATE);

		UI ui=pc.getSingle(UI.class);
		registerWizardPage(this,0,ui.createPage("wizard-intro", this));
		registerWizardPage(this,9999,ui.createPage("wizard-outro", this));

		ui.registerTool(pt);
	}

	@Override
	public void close() throws GeneralException
	{
		pc.getSingle(UI.class).unregisterTool(pt);
	}

	@Override
	public synchronized void registerPage(Plugin owner,Page p)
	{
		List<Page> l = pages.get(owner);
		if(l==null)
		{
			l=new LinkedList<Page>();
			pages.put(owner,l);
		}
		l.add(p);
	}

	@Override
	public synchronized void unregisterPage(Plugin owner,Page p)
	{
		List<Page> l = pages.get(owner);
		if(l!=null)
		{
			if(l.remove(p)) return;
		}
		throw new BugException("No such page");
	}

	synchronized Page[] getPages()
	{
		List<Page> l = new LinkedList<Page>();
		for(List<Page> pluginPages : pages.values())
		{
			l.addAll(pluginPages);
		}
		return l.toArray(new Page[l.size()]);
	}

	/**
	 * Message: System state.
	 * @param msg Message
	 */
	public void msg(SystemStateMsg msg)
	{
		// Show wizard on first run
		if(msg.getType()==SystemStateMsg.UIREADY)
		{
			Preferences p=pc.getSingle(Preferences.class);
			PreferencesGroup pg=p.getGroup(this);
			if(!p.toBoolean(pg.get(PREF_DONEWIZARD,p.fromBoolean(false))))
			{
				pg.set(PREF_DONEWIZARD,p.fromBoolean(true));
				showWizard();
			}
		}
	}

	synchronized void showWizard()
	{
		if(pw!=null) return;

		Page[] p=new Page[wizardPages.size()];
		int index=0;
		for(WizardPage wp : wizardPages)
		{
			p[index++]=wp.p;
		}

		pw=new PrefsWizard(pc,p);
	}

	synchronized void wizardClosed()
	{
		pw=null;
	}

	/**
	 * Message: Plugin unloaded
	 * @param unload Message
	 */
	public synchronized void msg(PluginUnloadMsg unload)
	{
		// This clears up any pages added by a plugin that's since been uninstalled
		if(pages.remove(unload.getPlugin())!=null)
		{
			// Better close the window with it in!
			pt.closeWindow();
		}

		for(Iterator<WizardPage> i=wizardPages.iterator(); i.hasNext();)
		{
			WizardPage wp = i.next();
			if(wp.owner==unload.getPlugin())
			{
				i.remove();
				if(pw!=null)
					pw.close();
			}
		}
	}
	@Override
	public String toString()
	{
		return "Preferences UI plugin";
	}

	/** Represents details held about a page in the wizard dialog */
	private static class WizardPage implements Comparable<WizardPage>
	{
		Plugin owner;
		int order;
		Page p;

		WizardPage(Plugin owner,int order,Page p)
		{
			this.owner=owner;
			this.order=order;
			this.p=p;
		}

		@Override
		public int compareTo(WizardPage o)
		{
			if(o==this)
			{
				return 0;
			}
			int compare = order - o.order;
			if(compare!=0)
			{
				return compare;
			}
			return hashCode() - o.hashCode();
		}
	}

	@Override
	public synchronized void registerWizardPage(Plugin owner,int order,Page p)
	{
		wizardPages.add(new WizardPage(owner,order,p));
	}

	@Override
	public synchronized void unregisterWizardPage(Page p)
	{
		for(Iterator<WizardPage> i=wizardPages.iterator();i.hasNext();)
		{
			WizardPage wp = i.next();
			if(wp.p==p)	i.remove();
		}
	}
}
