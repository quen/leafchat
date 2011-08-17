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
package com.leafdigital.monitor;

import java.util.*;

import util.xml.XML;

import com.leafdigital.irc.api.*;

import leafchat.core.api.*;

/**
 * Debug plugin to monitor network messages.
 */
public class MonitorPlugin implements Plugin
{
	private PluginContext context;
	private MonitorWindow w;

	private Map<String, String> serverColours = new HashMap<String, String>();
	private int nextColour=1;

	private final static int BUFFERSIZE=100;
	private LinkedList<String> recentLines = new LinkedList<String>();

	@Override
	public void init(PluginContext context, PluginLoadReporter reporter) throws GeneralException
	{
		this.context=context;

		context.requestMessages(UserCommandMsg.class,this);

		context.requestMessages(ServerSendMsg.class,this,Msg.PRIORITY_LAST);
		context.requestMessages(ServerLineMsg.class,this,Msg.PRIORITY_LAST);
		context.requestMessages(ServerConnectedMsg.class,this,Msg.PRIORITY_LAST);
		context.requestMessages(ServerConnectionFinishedMsg.class,this,Msg.PRIORITY_LAST);
		context.requestMessages(ServerDisconnectedMsg.class,this,Msg.PRIORITY_LAST);
	}

	/**
	 * Message: User command (/debugmonitor)
	 * @param msg Message
	 */
	public void msg(UserCommandMsg msg)
	{
		if("debugmonitor".equals(msg.getCommand()))
		{
			if(w==null)
			{
				w=new MonitorWindow(context);
				for(String s : recentLines)
				{
					w.addLine(s);
				}
			}
			else
			{
				w.focus();
			}

			msg.markHandled();
		}
	}

	private void addLine(Server s,String line)
	{
		String host=s.getConnectedHost();
		String colour=serverColours.get(host);
		if(colour==null)
		{
			colour="irc-fg"+(nextColour++);
			if(nextColour>8) nextColour=1;
			serverColours.put(host,colour);
		}
		line="<"+colour+">"+line+"</"+colour+">";

		recentLines.addLast(line);
		if(recentLines.size()>BUFFERSIZE)
			recentLines.removeFirst();

		if(w!=null)
			w.addLine(line);
	}

	/**
	 * Message: Send data to server.
	 * @param msg Message
	 */
	public void msg(ServerSendMsg msg)
	{
		addLine(msg.getServer(),"&gt; "+XML.esc(IRCMsg.convertISO(msg.getLine())));
	}

	/**
	 * Message: Receive data from server.
	 * @param msg Message
	 */
	public void msg(ServerLineMsg msg)
	{
		addLine(msg.getServer(),"&lt; "+XML.esc(IRCMsg.convertISO(msg.getLine())));
	}

	/**
	 * Message: Server connected.
	 * @param msg Message
	 */
	public void msg(ServerConnectedMsg msg)
	{
		addLine(msg.getServer(),"- <join>Connected</join>: "+XML.esc(msg.getServer().getReportedOrConnectedHost()));
	}

	/**
	 * Message: Connection finished.
	 * @param msg Message
	 */
	public void msg(ServerConnectionFinishedMsg msg)
	{
		addLine(msg.getServer(),"- <join>Connection finished</join>: "+XML.esc(msg.getServer().getReportedOrConnectedHost()));
	}

	/**
	 * Message: Server disconnected.
	 * @param msg Message
	 */
	public void msg(ServerDisconnectedMsg msg)
	{
		addLine(msg.getServer(),"- <part>Disconnected</part>: "+XML.esc(msg.getServer().getReportedOrConnectedHost()));
	}

	@Override
	public void close() throws GeneralException
	{
		if(w!=null)
		{
			w.close();
		}
	}

	/**
	 * Called by {@link MonitorWindow} when it is closed.
	 */
	void windowClosed()
	{
		w=null;
	}

	@Override
	public String toString()
	{
	  // Used to display in system log etc.
		return "Debug monitor plugin";
	}

}
