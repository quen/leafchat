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
package com.leafdigital.ircui;

import org.w3c.dom.Element;

import com.leafdigital.idle.api.Idle;
import com.leafdigital.irc.api.*;
import com.leafdigital.ircui.api.GeneralChatWindow;

import leafchat.core.api.*;

/**
 * Plugin chat window; a chat window created by another plugin.
 */
public class PluginChatWindow extends ChatWindow implements GeneralChatWindow
{
	private Handler h;
	private Plugin owner;

	private String logCategory,logSource,logItem;
	private int availableBytes;
	private String ownNick,target;

	/**
	 *
	 * @param context IRCUI context
	 * @param owner Owner context
	 * @param h Handler for commands, window close event
	 * @param logSource Source for log address. Usually a server address but
	 *   can be something else. Use null to prevent logging
	 * @param logCategory Category of thing for log. Logger.CATEGORY_xx constant
	 *   or custom name
	 * @param logItem Item name for log. For Logger.CATEGORY_USER, must be nick;
	 *   for CATEGORY_CHAN, must be channel. For custom types can be anything
	 *   appropriate
	 * @param availableBytes Number of bytes allowed per line of text (best
	 *   to include a safe margin), used for auto-wrapping text. Applies only to
	 *   text not to /-commands which are auto-limited at 400 odd.
	 * @param ownNick User's own nick for purposes of this window (display own
	 *   messages)
	 * @param target Target's nick (or other identifier), i.e. where text to this
	 *   window goes; will be matched up against showOwnText value
	 * @param startMinimised If true, window starts as minimised
	 * @throws GeneralException Any error
	 */
	PluginChatWindow(PluginContext context,PluginContext owner,Handler h,
		String logSource,String logCategory,String logItem,int availableBytes,
		String ownNick,String target,boolean startMinimised) throws GeneralException
	{
		super(context, "pluginchatwindow", true, !startMinimised);
		this.h=h;
		this.logCategory=logCategory;
		this.logSource=logSource;
		this.logItem=logItem;
		this.owner=owner.getPlugin();
		this.availableBytes=availableBytes;
		this.ownNick=ownNick;
		this.target=target;

		context.requestMessages(PluginUnloadMsg.class,this);
		commandUI.setEnabled(true); // Default to enabled, unlike real msgwindows
	}

	/**
	 * Message: Plugin that owns this box has been unloaded. (At present, native
	 * plugins can't be unloaded, but user scripts can.)
	 * @param msg Message
	 */
	public void msg(PluginUnloadMsg msg)
	{
		if(msg.getPlugin()==owner)
		{
			h=null;
			getWindow().close();
		}
	}

	@Override
	protected void doCommand(Commands c,String line) throws GeneralException
	{
		getPluginContext().getSingleton2(Idle.class).userAwake(Idle.AWAKE_COMMAND);
		h.doCommand(c,line);
	}

	@Override
	protected int getAvailableBytes() throws GeneralException
	{
		return availableBytes;
	}

	@Override
	protected String getLogCategory()
	{
		return logCategory;
	}

	@Override
	protected String getLogItem()
	{
		return logItem;
	}

	@Override
	protected String getLogSource()
	{
		return logSource;
	}

	@Override
	protected String getOwnNick()
	{
		return ownNick;
	}

	@Override
	protected boolean isUs(String target)
	{
		return target.equalsIgnoreCase(this.target);
	}

	@Override
	public void windowClosed() throws GeneralException
	{
		super.windowClosed();
		if(h!=null)	h.windowClosed();
	}

	@Override
	public void showRemoteText(int type,String nick,String text)
	{
		switch(type)
		{
		case MessageDisplay.TYPE_MSG :
			addLine("&lt;<nick>"+esc(nick)+"</nick>&gt; "+esc(text),"msg");
			reportActualMessage(getWindow().getTitle(),"<"+nick+"> "+text);
			break;
		case MessageDisplay.TYPE_ACTION :
			addLine(ACTIONSYMBOL+"<nick>"+esc(nick)+"</nick> "+esc(text),"action");
			reportActualMessage(getWindow().getTitle(),ACTIONSYMBOL+nick+" "+text);
			break;
		case MessageDisplay.TYPE_NOTICE:
			addLine("-<nick>"+esc(nick)+"</nick>- "+esc(text),"notice");
			reportActualMessage(getWindow().getTitle(),"-"+nick+"- "+text);
			break;
		default:
			throw new BugException("Unexpected type");
		}
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		commandUI.setEnabled(enabled);
	}

	@Override
	public void setTitle(String title)
	{
		getWindow().setTitle(title);
	}

	@Override
	public void setTarget(String target)
	{
		this.target=target;
	}

	@Override
	public MessageDisplay getMessageDisplay()
	{
		return this;
	}

	@Override
	protected boolean displayTimeStamps()
	{
		return true;
	}

	@Override
	protected void internalAction(Element e) throws GeneralException
	{
		h.internalAction(e);
	}
}
