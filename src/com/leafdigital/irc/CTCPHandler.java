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
package com.leafdigital.irc;

import java.text.SimpleDateFormat;
import java.util.*;

import com.leafdigital.irc.api.*;

import leafchat.core.api.*;

/**
 * Handles standard CTCP requests.
 */
public class CTCPHandler
{
  CTCPHandler(PluginContext context)
  {
  }

  /**
   * Message: CTCP request.
   * @param msg Message
   * @throws GeneralException Any error
   */
  public void msg(UserCTCPRequestIRCMsg msg) throws GeneralException
  {
 	  String command=msg.getRequest();
		if(command.equals("VERSION"))
  	  handleVersion(msg);
  	else if(command.equals("PING"))
  	  handlePing(msg);
  	else if(command.equals("CLIENTINFO"))
	    handleClientInfo(msg);
  	else if(command.equals("TIME"))
	    handleTime(msg);
  }

  private void sendResponse(UserCTCPRequestIRCMsg msg,String s) throws GeneralException
  {
  	sendResponse(msg,s,msg.getRequest());
  }

  private void sendResponse(UserCTCPRequestIRCMsg msg,String s,String command) throws GeneralException
  {
  	msg.markResponded();
  	msg.getServer().sendLine(IRCMsg.constructBytes(
  	  "NOTICE "+msg.getSourceUser().getNick()+" :\u0001"+command+" "+s+"\u0001"
 	  ));
  }

  private void handleVersion(UserCTCPRequestIRCMsg msg) throws GeneralException
  {
  	sendResponse(msg,"leafChat "+SystemVersion.getCTCPVersion()+" "+
  		System.getProperty("os.name")+" "+System.getProperty("os.version")+" "+
  		"http://www.leafdigital.com/software/leafchat/");
  }

  private void handlePing(UserCTCPRequestIRCMsg msg) throws GeneralException
  {
  	byte[] sText=msg.getText();
  	// CTCP spec suggests limit of 16 but I am allowing 32 because some clients
  	// go over.
		if(sText.length==0 || sText.length>32) return;
		sendResponse(msg,IRCMsg.convertISO(sText),"PING");
  }

  private void handleClientInfo(UserCTCPRequestIRCMsg msg) throws GeneralException
  {
		sendResponse(msg,"VERSION PING CLIENTINFO TIME");
  }

  private void handleTime(UserCTCPRequestIRCMsg ucrim) throws GeneralException
  {
 	  SimpleDateFormat sdf=new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z",Locale.UK);
 	  sendResponse(ucrim,sdf.format(new Date()));
  }
}
