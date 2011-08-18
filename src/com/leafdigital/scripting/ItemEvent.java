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

import java.lang.reflect.*;
import java.util.*;

import org.w3c.dom.Element;

import com.leafdigital.ui.api.*;
import com.leafdigital.ui.api.TreeBox.Item;

import util.xml.*;
import leafchat.core.api.*;
import leafchat.core.api.FilterInfo.Parameter;

/** Script item that handles an event. */
@UIHandler("itemsettings.event")
public class ItemEvent extends UserCodeItem implements TreeBox.SingleSelectionHandler
{
	private MessageInfo event=null;
	private FilterInfo filter=null;
	private String[] filterValues=null;
	private String priority;

	private InfoItem rootItem;

	private LinkedList<EditBox> filterParamEdits = new LinkedList<EditBox>();

	/**
	 * Constructs from XML.
	 * @param parent Owning script
	 * @param e XML element
	 * @param index Index within script
	 * @throws XMLException
	 * @throws GeneralException
	 */
	public ItemEvent(Script parent,Element e,int index) throws XMLException,GeneralException
	{
		super(parent,e,index);
		rootItem=new InfoItem(parent.getContext().getMessageInfo(Msg.class),null);
		String eventClass=XML.getRequiredAttribute(e,"class");
		try
		{
			event=getContext().getMessageInfo(
				Class.forName(eventClass).asSubclass(Msg.class));
		}
		catch(ClassNotFoundException ex)
		{
			throw new BugException("Item event requests unknown event: "+eventClass);
		}

		if(e.hasAttribute("priority"))
		{
			priority=e.getAttribute("priority");
			if(!priority.equals("BEFORENORMAL") && !priority.equals("LAST")
				&& !priority.equals("NORMAL"))
			{
				throw new BugException("Event priority not supported: "+priority);
			}
		}
		else
		{
			priority="NORMAL";
		}

		if(XML.hasChild(e,"filter"))
		{
			Element filterEl=XML.getChild(e,"filter");
			String filterClass=filterEl.getAttribute("class");
			FilterInfo[] appropriateFilters=event.getAppropriateFilters();
			for(int i=0;i<appropriateFilters.length;i++)
			{
				if(appropriateFilters[i].getFilterClass().getName().equals(filterClass))
				{
					filter=appropriateFilters[i];
				}
			}
			if(filter==null)
				throw new BugException("Item event requests unknown or unsupported filter: "+filterClass);

			FilterInfo.Parameter[] filterParams=filter.getScriptingParameters();
			filterValues=XML.getChildTexts(filterEl,"param");
			if(filterParams.length!=filterValues.length)
				throw new BugException("Item event supplies incorrect number of filter parameters");
		}
	}

	/**
	 * Constructs blank item.
	 * @param parent Owning script
	 * @param index Index in script
	 */
	public ItemEvent(Script parent,int index)
	{
		super(parent,index);
		rootItem=new InfoItem(parent.getContext().getMessageInfo(Msg.class),null);
		priority="AFTERNORMAL";
//		debugMessages(rootItem.info,0);
	}


	@Override
	String getSourceInit()
	{
		StringBuffer sb=new StringBuffer();
		sb.append("\t\tcontext.requestMessages("+event.getMessageClass().getName()+".class,new Item"+getIndex()+"(),\n");
		if(filter!=null)
		{
			sb.append("\t\t\tnew "+filter.getFilterClass().getName()+"(");
			Parameter[] params=filter.getScriptingParameters();
			for(int i=0;i<params.length;i++)
			{
				if(i!=0) sb.append(",");
				if(params[i].getType()==int.class)
				{
					try
					{
						sb.append(Integer.parseInt(filterValues[i])+"");
					}
					catch(NumberFormatException nfe)
					{
						throw new BugException("Unexpected (non-integer) value for integer filter parameter");
					}
				}
				else
					sb.append(getQuotedString(filterValues[i]));
			}
			sb.append("),\n");
		}
		sb.append("\t\t\tMsg.PRIORITY_"+priority+");\n");
		return sb.toString();
	}

	@Override
	String getSourceMethods()
	{
		String messageClass=event.getMessageClass().getName();
		StringBuffer sb=new StringBuffer();
		sb.append(
			"\tpublic class Item"+getIndex()+"\n"+
			"\t{"+
			"\t\tpublic void msg("+messageClass+" msg)\n"+
			"\t\t{\n"+
			"\t\t\t"+event.getContextInit()+"\n");
		String[] variables=event.getVariables().getNames();
		for(int i=0;i<variables.length;i++)
		{
			sb.append("\t\t\t");
			sb.append(event.getVariables().getDefinition(variables[i]));
			sb.append("\n");
		}

		sb.append(
			convertUserCode()+"\n"+
			"\t\t}\n"+
			"\t}\n");
		return sb.toString();
	}

	/**
	 * Display debug information.
	 * @param info MessageInfo
	 * @param indent Indent for recursive calls
	 */
	public static void debugMessages(MessageInfo info,int indent)
	{
		for(int i=0;i<indent;i++)
			System.out.print("    ");
		System.out.print(info.getName());
		System.out.print(" <");
		FilterInfo[] filters=info.getAppropriateFilters();
		for(int i=0;i<filters.length;i++)
		{
			if(i>0) System.out.print(",");
			String name=filters[i].getName();
			System.out.print(name);
		}
		System.out.print("> [");
		Method[] methods=info.getMessageClass().getMethods();
		boolean first=true;
		for(int i=0;i<methods.length;i++)
		{
			String name=methods[i].getName();
			if(name.startsWith("get") && !name.equals("getClass") &&
				!Modifier.isStatic(methods[i].getModifiers()) &&
				Modifier.isPublic(methods[i].getModifiers()))
			{
				if(first)
					first=false;
				else
					System.out.print(",");
				name=name.substring(3,4).toLowerCase()+name.substring(4);
				System.out.print(name+":");
				String returnType=methods[i].getReturnType().getName();
				if(returnType.equals("[B"))
					returnType="byte[]";
				else if(returnType.equals("[[B"))
					returnType="byte[][]";
				else if(returnType.indexOf('.')!=-1)
					returnType=returnType.substring(returnType.lastIndexOf('.')+1);
				System.out.print(returnType);
			}
		}
		System.out.println("]");
		MessageInfo[] children=info.getSubclasses();
		for(int i=0;i<children.length;i++)
		{
			debugMessages(children[i],indent+1);
		}
	}

	@Override
	void save(Element e)
	{
		super.save(e);

		e.setAttribute("class",event.getMessageClass().getName());
		e.setAttribute("priority",priority);
		if(filter!=null)
		{
			Element filterEl=XML.createChild(e,"filter");
			filterEl.setAttribute("class",filter.getFilterClass().getName());
			for(int i=0;i<filterValues.length;i++)
				XML.setText(XML.createChild(filterEl,"param"),filterValues[i]);
		}
	}

	@Override
	protected java.awt.Color getNormalStripeRGB()
	{
		return new java.awt.Color(0,0,128);
	}

	/** UI: List of message types */
	public TreeBox messagesUI;
	/** UI: Filter options */
	public Dropdown filterUI;
	/** UI: Filter settings */
  public VerticalPanel filterSettingsUI;
  /** UI: Description text */
  public Label descriptionUI;
  /** UI: Variables list */
  public Label variablesUI;
  /** UI: Priority dropdown */
  public Dropdown priorityUI;

	@Override
	protected Page getPage(Button ok)
	{
		Page p=super.getPage(ok);
		messagesUI.setHandler(this);
		if(event==null)
		{
			selected(null);
		}
		else
		{
			InfoItem item=rootItem.find(event);
			messagesUI.select(item);
			selected(item);
			if(filter==null)
			{
				filterUI.setSelected(null);
				changeFilter();
			}
			else
			{
				filterUI.setSelected(filter);
				changeFilter();
				for(int i=0;i<filterValues.length;i++)
				{
					filterParamEdits.get(i).setValue(filterValues[i]);
				}
			}
		}
		priorityUI.addValue("BEFORENORMAL","Handle before normal processing");
		priorityUI.addValue("LAST","Handle after normal processing");
		if(priority.equals("NORMAL"))
			priorityUI.setSelected("BEFORENORMAL");
		else
			priorityUI.setSelected(priority);
		return p;
	}

	@Override
	protected String getSummaryLabel()
	{
		StringBuffer label=new StringBuffer();
		label.append("<key>"+XML.esc(event.getName())+"</key>");
		if(filter!=null)
		{
			label.append(" [<key>"+XML.esc(filter.getName())+"</key> filter]");
			for(int i=0;i<filterValues.length;i++)
			{
				label.append(" "+XML.esc(filter.getScriptingParameters()[i].getName())+"=<key>"+XML.esc(filterValues[i])+"</key>");
			}
		}

		return label.toString();
	}

	@Override
	public String getVariablesLabel()
	{
		return getVariablesLabel(event);
	}

	/**
	 * Lists variables for event type.
	 * @param info MessageInfo
	 * @return String listing events with type and name (in XML output format)
	 */
	public static String getVariablesLabel(MessageInfo info)
	{
		StringBuffer sb=new StringBuffer();
		String[] variables=info.getVariables().getNames();
		for(int i=0;i<variables.length;i++)
		{
			if(i!=0) sb.append(", ");

			sb.append(info.getVariables().getType(variables[i]).getName().replaceFirst("^.*\\.","")+" <key>"+
				variables[i]+"</key>");
		}
		return sb.toString();
	}

	@Override
	protected void saveSettings()
	{
		MessageInfo selectedMessage=selectedEvent.getMessageInfo();
		String selectedPriority=(String)priorityUI.getSelected();
		FilterInfo selectedFilter=((FilterInfo)filterUI.getSelected());
		FilterInfo.Parameter[] filterParams=selectedFilter==null ? null : selectedFilter.getScriptingParameters();
		String[] enteredValues=null;
		if(filterParams!=null)
		{
			enteredValues=new String[filterParams.length];
			int count=0;
			for(EditBox eb : filterParamEdits)
			{
				enteredValues[count++]=eb.getValue();
			}
			assert(count==enteredValues.length);
		}
	  if(event!=null && event.equals(selectedMessage) && priority.equals(selectedPriority) &&
	  	((filter==null && selectedFilter==null) || (filter!=null && filter.equals(selectedFilter))))
		{
	  		// Event and filter the same, how about filter parameters?
	  		if(filter==null || Arrays.equals(filterValues,enteredValues)) return;
		}

	  // OK, something changed, save it
	  event=selectedMessage;
	  filter=selectedFilter;
	  filterValues=enteredValues;
	  priority=selectedPriority;
	  markChanged();
	}

	static class InfoItem implements TreeBox.Item
	{
		InfoItem[] children;
		InfoItem parent;
		MessageInfo info;

		InfoItem(MessageInfo info,InfoItem parent)
		{
			this.parent=parent;
			this.info=info;

			MessageInfo[] childrenInfo=info.getSubclasses();
			List<InfoItem> l = new LinkedList<InfoItem>();
			for(int i=0;i<childrenInfo.length;i++)
			{
				if(childrenInfo[i].equals(info))
					throw new BugException("Message loop on "+info.getName()+" (check that a MessageInfo isn't set up with the wrong class parameter)");
				if(childrenInfo[i].allowScripting())
					l.add(new InfoItem(childrenInfo[i],this));
			}
			children = l.toArray(new InfoItem[l.size()]);
		}

		MessageInfo getMessageInfo()
		{
			return info;
		}

		@Override
		public TreeBox.Item[] getChildren()
		{
			return children;
		}

		@Override
		public java.awt.Image getIcon()
		{
			return null;
		}

		@Override
		public TreeBox.Item getParent()
		{
			return parent;
		}

		@Override
		public String getText()
		{
			return info.getName();
		}

		@Override
		public boolean isLeaf()
		{
			return children.length==0;
		}

		InfoItem find(MessageInfo searchInfo)
		{
			if(searchInfo.equals(info))
				return this;
			for(int i=0;i<children.length;i++)
			{
				InfoItem found=children[i].find(searchInfo);
				if(found!=null) return found;
			}
			return null;
		}
	}

	@Override
	public TreeBox.Item getRoot()
	{
		return rootItem;
	}

	@Override
	public boolean isRootDisplayed()
	{
		return false;
	}

	private InfoItem selectedEvent;

	@Override
	public void selected(Item i)
	{
		if(i==null)
		{
			filterUI.clear();
			filterUI.setEnabled(false);
			selectedEvent=(InfoItem)i;
			descriptionUI.setVisible(false);
			variablesUI.setText("");
			change();
			changeFilter();
		}
		else
		{
			filterUI.clear();
			filterUI.addValue(null,"All messages of this type");
			MessageInfo info=((InfoItem)i).getMessageInfo();
			FilterInfo[] filters=info.getAppropriateFilters();
			int validFilters=0;
			for(int filter=0;filter<filters.length;filter++)
			{
				if(filters[filter].getScriptingParameters()!=null)
				{
					filterUI.addValue(filters[filter],	filters[filter].getName());
					validFilters++;
				}
			}
			filterUI.setEnabled(validFilters>0);
			if(info.getDescription()==null)
			{
				descriptionUI.setVisible(false);
			}
			else
			{
				descriptionUI.setText(info.getDescription());
				descriptionUI.setVisible(true);
			}
			selectedEvent=(InfoItem)i;
			variablesUI.setText(getVariablesLabel(info));
			change();
			changeFilter();
		}
	}

	/** Callback: Filter changed */
	@UIAction
	public void changeFilter()
	{
		filterSettingsUI.removeAll();
		filterParamEdits.clear();

		if(filterUI.getSelected()!=null)
		{
			UI ui=getContext().getSingle(UI.class);

			FilterInfo.Parameter[] parameters=((FilterInfo)filterUI.getSelected()).getScriptingParameters();
			for(int i=0;i<parameters.length;i++)
			{
				BorderPanel bp=ui.newBorderPanel();
				filterSettingsUI.add(bp);
				bp.setSpacing(8);
				Label l=ui.newLabel();
				bp.set(BorderPanel.WEST,l);
				l.setText(parameters[i].getName());
				l.setWidthGroup("labels");
				l.setBaseGroup("param"+i);
				VerticalPanel vp=ui.newVerticalPanel();
				bp.set(BorderPanel.CENTRAL,vp);
				vp.setSpacing(2);
				EditBox eb=ui.newEditBox();
				vp.add(eb);
				eb.setBaseGroup("param"+i);
				if(parameters[i].getType()==int.class)
				{
					eb.setRequire("[0-9]+");
				}
				else
				{
					eb.setRequire(".+");
				}
				eb.setOnChange("change");
				filterParamEdits.add(eb);
				if(parameters[i].getDescription()!=null)
				{
					l=ui.newLabel();
					l.setText(parameters[i].getDescription());
					l.setSmall(true);
					vp.add(l);
				}
			}
		}
		change();
	}

	/** Callback: Event changed */
	@UIAction
	public void change()
	{
		boolean ok=true;

		if(selectedEvent==null)
		{
			ok=false;
		}
		else
		{
			for(EditBox eb : filterParamEdits)
			{
				if(eb.getFlag()!=EditBox.FLAG_NORMAL)
				{
					ok=false;
					break;
				}
			}
		}

		allowOK(ok);
	}
}
