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
package com.leafdigital.dcc;

import java.io.File;

import util.PlatformUtils;
import util.xml.XML;

import com.leafdigital.irc.api.*;
import com.leafdigital.net.api.Network;
import com.leafdigital.ui.api.UI;

import leafchat.core.api.*;

/**
 * Deals with DCC-related user /-commands.
 */
public class DCCCommands
{
	private PluginContext context;
	DCCCommands(PluginContext context) throws GeneralException
	{
		this.context=context;
		context.requestMessages(UserCommandMsg.class,this,Msg.PRIORITY_LATE);
		context.requestMessages(UserCommandListMsg.class,this,Msg.PRIORITY_LATE);
	}

	/**
	 * Message: User types a command.
	 * @param msg Message
	 * @throws GeneralException Any error
	 */
  public void msg(UserCommandMsg msg) throws GeneralException
  {
  		if(msg.isHandled()) return;
  		String command=msg.getCommand();

  		if("dccsend".equals(command)) dccsend(msg);
  		else if("dccchat".equals(command)) dccchat(msg);
  		else if("dccaddress".equals(command)) dccaddress(msg);
  }

	/**
	 * Message: Listing available commands.
	 * @param msg Message
	 */
	public void msg(UserCommandListMsg msg)
	{
		msg.addCommand(true, "dccsend", UserCommandListMsg.FREQ_UNCOMMON,
			"/dccsend <nick>",
			"Opens the file select dialog box so you can send a file to the user");
		msg.addCommand(true, "dccchat", UserCommandListMsg.FREQ_UNCOMMON,
			"/dccchat <nick>",
			"Opens DCC chat with the user");
		msg.addCommand(true, "dccaddress", UserCommandListMsg.FREQ_UNCOMMON,
			"/dccaddress <nick> <IP address>",
			"When connecting through a proxy, sets the public address for " +
			"connection from given user (before /dccsend or /dccchat)");
	}

  private void dccaddress(UserCommandMsg msg) throws GeneralException
  {
		// ok we got it covered here
		msg.markHandled();

		Network n=context.getSingle(Network.class);
		if(!n.needsListenTarget())
		{
			msg.getMessageDisplay().showError("You do not need to use /dccaddress except when connecting through a proxy");
			return;
		}

		String[] params=msg.getParams().split(" ");
		if(params.length!=2 || !params[1].matches("([0-9]+\\.){3}[0-9]+"))
		{
			msg.getMessageDisplay().showError("Syntax: /dccaddress &lt;nickname> &lt;address>");
			return;
		}

		DCCPlugin dp=(DCCPlugin)context.getPlugin();
		dp.setDCCAddress(params[0],params[1]);
		msg.getMessageDisplay().showInfo("Set address for <nick>"+XML.esc(params[0])+"</nick> to "+params[1]+"; you can now attempt DCC send.");
  }

  private void dccsend(UserCommandMsg msg) throws GeneralException
  {
		// ok we got it covered here
		msg.markHandled();

		Server s=msg.getServer();
		if(s==null)
		{
			msg.getMessageDisplay().showError("Don't know which server to send on; " +
				"type /dccsend in a command box associated with a particular server.");
			return;
		}

		String[] params=msg.getParams().split(" ");
		if(params.length!=1 || params[0].length()==0)
		{
			msg.getMessageDisplay().showError("Syntax: /dccsend &lt;nickname>");
			return;
		}
		String targetNick=params[0];

		// Select file
		UI ui=context.getSingle(UI.class);
		File f=ui.showFileSelect(null,"Send file to "+targetNick,
			false,new File(PlatformUtils.getDocumentsFolder()),null, null, null);
		if(f==null) return;
		if(!f.canRead())
		{
			ui.showUserError(null,
				"Cannot read file",
				"leafChat could not access the selected file:<p/>"+XML.esc(f.getPath())+"<p/>Ensure " +
				"the file exists and is not hidden or otherwise unavailable.");
			return;
		}

		((DCCPlugin)context.getPlugin()).startListen(s,targetNick,f);
  }

  private void dccchat(UserCommandMsg msg) throws GeneralException
  {
		// ok we got it covered here
		msg.markHandled();

		Server s=msg.getServer();
		if(s==null)
		{
			msg.getMessageDisplay().showError("Don't know which server to send on; " +
				"type /dccchat in a command box associated with a particular server.");
			return;
		}

		String[] params=msg.getParams().split(" ");
		if(params.length!=1 || params[0].length()==0)
		{
			msg.getMessageDisplay().showError("Syntax: /dccchat &lt;nickname>");
			return;
		}
		String targetNick=params[0];

		((DCCPlugin)context.getPlugin()).startChat(s,targetNick);
  }
}
