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
package com.leafdigital.dcc;

import java.io.*;
import java.net.InetAddress;
import java.util.regex.*;

import util.StringUtils;
import util.xml.*;

import com.leafdigital.irc.api.*;
import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/**
 * Window that lets user choose whether or not to accept a file.
 */
@UIHandler("acceptexists")
public class FileAcceptWindow
{
	/**
	 * Label: Nickname.
	 */
	public Label nickTextUI;
	/**
	 * Label: Filename.
	 */
	public Label fileTextUI;
	/**
	 * Label: Resume information.
	 */
	public Label resumeOfferUI;
	/**
	 * Label: File exists.
	 */
	public Label existingTextUI;
	/**
	 * Button: Resume.
	 */
	public Button resumeUI;
	/**
	 * Button: Rename.
	 */
	public Button renameUI;

	private Server s;
	private String nick;
	private byte[] fileBytes;
	private int port;
	private long size;
	private InetAddress address;

	private File target,targetPartial;

	private long resumePos;

	private PluginContext context;

	/**
	 * @param context Plugin context
	 * @param s Server
	 * @param user User who sent request
	 * @param address User address
	 * @param port User port
	 * @param fileBytes Name of file (in raw bytes)
	 * @param size Size of file
	 * @param encodingReference Used to determine preferred character encoding
	 */
	public FileAcceptWindow(PluginContext context,Server s,
		IRCUserAddress user,InetAddress address,int port,byte[] fileBytes,long size,
		IRCMsg encodingReference)
	{
		this.context=context;
		this.s=s;
		this.nick=user.getNick();
		this.fileBytes=fileBytes;
		this.address=address;
		this.port=port;
		this.size=size;

		// Sanitise filename
		String file=encodingReference.convertEncoding(fileBytes);
		file=file.replaceAll("^.*[/\\\\]",""); // Get rid of any folder names
		StringBuffer clean=new StringBuffer();
		for(int i=0;i<file.length();i++)
		{
			char c=file.charAt(i);
			if(Character.isLetterOrDigit(c) || "._-,".indexOf(c)!=-1)
				clean.append(c);
			else
				clean.append('_');
		}
		file=clean.toString();

		// Get file target location
		File folder=((DCCPlugin)context.getPlugin()).getDownloadFolder();
		target=new File(folder,file);
		targetPartial=new File(folder,file+".partial");
		if(targetPartial.exists() && targetPartial.length()==0)
			targetPartial.delete();
		if(target.exists() && target.length()==0)
			target.delete();

		UI u=context.getSingleton2(UI.class);

		// Does the file exist already?
		if(target.exists() || targetPartial.exists())
		{
			w=u.createWindow("acceptexists", this);
			existingTextUI.setText(existingTextUI.getText().replaceAll("%EXISTINGSIZE%",
				StringUtils.displayBytes(targetPartial.exists() ? targetPartial.length() : target.length())));
			// If the offered file is bigger than current...
			if(targetPartial.exists() && (size==TransferProgress.SIZE_UNKNOWN || size>targetPartial.length()))
			{
				// May be OK to resume
			}
			else
			{
				resumeOfferUI.setVisible(false);
				resumeUI.setVisible(false);

				renameUI.setDefault(true);
			}
		}
		else
		{
			w=u.createWindow("accept", this);
		}

		nickTextUI.setText(nickTextUI.getText().
			replaceAll("%NICK%",XML.esc(user.getNick())).
			replaceAll("%IP%",address.getHostAddress()));
		String fileText=fileTextUI.getText();
		fileText=fileText.replaceAll("%FILENAME%",XML.esc(file));
		fileText=fileText.replaceAll("%SIZE%",
			size==TransferProgress.SIZE_UNKNOWN
				? "unknown size"
				:	StringUtils.displayBytes(size));
		fileTextUI.setText(fileText);
		w.setTitle(user.getNick()+" - file offer");
		w.show(true);
	}

	private void sendResume(long pos)
	{
		try
		{
			ByteArrayOutputStream baos=new ByteArrayOutputStream();
			baos.write(IRCMsg.constructBytes(
				"PRIVMSG "+nick+" :\u0001DCC RESUME "));
			baos.write(fileBytes);
			baos.write(IRCMsg.constructBytes(" "+port+" "+pos+"\u0001"));
			s.sendLine(baos.toByteArray());
			context.logDebug("Sent DCC RESUME: "+IRCMsg.convertISO(baos.toByteArray()));
		}
		catch(IOException e)
		{
			throw new Error("Unexpected error sending resume request",e);
		}
	}

	/**
	 * Message: DCC Accept.
	 * @param msg Message
	 */
	public void msg(UserCTCPRequestIRCMsg msg)
	{
		// Check it's a DCC ACCEPT for this user/file/etc
		if(msg.getServer()!=s || !msg.getSourceUser().getNick().equals(nick) ||
			!msg.getRequest().equals("DCC"))
			return;
		byte[][] params=IRCMsg.splitBytes(msg.getText());
		if(params.length<4) return;
		if(!IRCMsg.convertISO(params[0]).equals("ACCEPT")) return;
		// Ignore parameter 1, some clients don't give the filename
		if(!IRCMsg.convertISO(params[2]).equals(port+"")) return;

		// OK, we got it!
		try
		{
			resumePos=Long.parseLong(IRCMsg.convertISO(params[3]));
		}
		catch(NumberFormatException nfe)
		{
			return;
		}
		msg.markHandled();

		context.logDebug("Received DCC ACCEPT: "+msg.getLineISO());

		target.delete(); // Just in case
		w.close();
		startDownload();
	}

	private Window w;

	/**
	 * Callback: Window closed.
	 */
	@UIAction
	public void windowClosed()
	{
		context.unrequestMessages(null,this,PluginContext.ALLREQUESTS);
	}

	private void startDownload()
	{
		((DCCPlugin)context.getPlugin()).startDownload(nick,address,port,target,targetPartial,size,resumePos);
	}

	/**
	 * Action: Resume button.
	 */
	@UIAction
	public void actionResume()
	{
		sendResume(targetPartial.length());
	}

	/**
	 * Action: Overwrite button.
	 */
	@UIAction
	public void actionOverwrite()
	{
		targetPartial.delete();
		target.delete();
		w.close();
		startDownload();
	}

	private final static Pattern RENAMEPATTERN=Pattern.compile("^(.*_)([0-9]+)$");

	/**
	 * Action: Rename button.
	 */
	@UIAction
	public void actionRename()
	{
		// Pick a new filename using _2, _3 etc
		while(target.exists() || targetPartial.exists())
		{
			String name=target.getName();
			File folder=target.getParentFile();
			int dot=name.lastIndexOf('.');
			String extension= (dot==-1) ? "" : name.substring(dot);
			String main= (dot==-1) ? name : name.substring(0,dot);

			Matcher m=RENAMEPATTERN.matcher(main);
			if(m.matches())
			{
				main=m.group(1)+(Integer.parseInt(m.group(2))+1);
			}
			else
			{
				main+="_2";
			}

			target=new File(folder,main+extension);
			targetPartial=new File(folder,main+extension+".partial");
		}
		targetPartial.delete();
		w.close();
		startDownload();
	}

	/**
	 * Action: Ignore button. (Ignores this request, does not actually /ignore
	 * the user.)
	 */
	@UIAction
	public void actionIgnore()
	{
		w.close();
	}
}
