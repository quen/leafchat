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
import java.net.*;

import util.StringUtils;

import com.leafdigital.irc.api.*;
import com.leafdigital.net.api.Network;

import leafchat.core.api.*;

/**
 * Thread that handles DCC file uploads.
 */
public class Uploader extends Thread
{
	private File source;
	private Server server;
	private String nick;
	private byte[] sendName;

	private int port;

	private long startPos=0;

	private PluginContext context;

	private TransferProgress tp;

	private boolean cancelled;

	/**
	 * @param context Plugin context
	 * @param tp Transfer progress display window
	 * @param s Server
	 * @param nick Nickname
	 * @param source Source file on disk
	 */
	public Uploader(PluginContext context,TransferProgress tp,Server s,String nick,File source)
	{
		this.source=source;
		this.server=s;
		this.nick=nick;
		this.tp=tp;
		this.context=context;

		try
		{
			sendName=source.getName().replaceAll("(\\s|[/\\\\:*?\"<>|])","_").getBytes("UTF-8");
		}
		catch(UnsupportedEncodingException e)
		{
			throw new BugException(e);
		}

		// For RESUME requests
		context.requestMessages(UserCTCPRequestIRCMsg.class,this,Msg.PRIORITY_EARLY);

		tp.setUploader(this);

		// Start listening thread
		start();
	}

	void cancel()
	{
		cancelled=true;
		context.unrequestMessages(null,this,PluginContext.ALLREQUESTS);
	}

	/**
	 * Message: DCC RESUME request.
	 * @param msg Message
	 */
	public void msg(UserCTCPRequestIRCMsg msg)
	{
		if(msg.getServer()!=server || !msg.getSourceUser().getNick().equals(nick) ||
			!msg.getRequest().equals("DCC")) return;

		byte[][] params=IRCMsg.splitBytes(msg.getText());
		if(params.length<4) return; // RESUME file port pos
		String command=IRCMsg.convertISO(params[0]).toUpperCase();
		if(!command.equals("RESUME")) return;

		int specifiedPort;
		long resumePos;
		try
		{
			specifiedPort=Integer.parseInt(IRCMsg.convertISO(params[2]));
			resumePos=Long.parseLong(IRCMsg.convertISO(params[3]));
		}
		catch(NumberFormatException e)
		{
			return;
		}

		context.logDebug("Received DCC RESUME: "+msg.getLineISO());

		// I made it not check the filename as apparently some clients
		// send invalid filenames
		if(port!=specifiedPort) return;

		// OK, a valid request and for us!
		msg.markHandled();
		startPos=Math.min(source.length(),resumePos);

		// Send response
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		try
		{
			baos.write(IRCMsg.constructBytes(
				"PRIVMSG "+nick+" :\u0001DCC ACCEPT "));
			baos.write(sendName);
			baos.write(IRCMsg.constructBytes(" "+port+" "+startPos+"\u0001"));
		}
		catch(IOException e)
		{
			throw new BugException(e);
		}
		server.sendLine(baos.toByteArray());
		context.logDebug("Sent DCC ACCEPT: "+IRCMsg.convertISO(baos.toByteArray()));

		tp.status("Resuming from "+StringUtils.displayBytes(startPos));
	}

	@Override
	public void run()
	{
		tp.status("Setting up");
		Network.Port p;
		InetAddress ia;
		long ipnumber;
		try
		{
			p=((DCCPlugin)context.getPlugin()).getDCCListenPort(nick);
			ia=p.getPublicAddress();
			ipnumber=((DCCPlugin)context.getPlugin()).getStupidIPNumber(ia);
		}
		catch(GeneralException e)
		{
			tp.error(e.getMessage());
			return;
		}
		port=p.getPublicPort();

		long size=source.length();
		Socket s=null;
		FileInputStream fis=null;
		try
		{
			// OK, we have a port and stupid-version IP, let's send the DCC message.
			try
			{
				ByteArrayOutputStream baos=new ByteArrayOutputStream();
				baos.write(IRCMsg.constructBytes(
					"PRIVMSG "+nick+" :\u0001DCC SEND "));
				baos.write(sendName);
				baos.write(IRCMsg.constructBytes(" "+ipnumber+" "+port+" "+size+"\u0001"));
				server.sendLine(baos.toByteArray());
				context.logDebug("Sent DCC SEND: "+IRCMsg.convertISO(baos.toByteArray()));
			}
			catch(IOException e)
			{
				throw new BugException(e);
			}

			tp.status("Waiting for connection");

			// Now listen
			try
			{
				context.logDebug("Listening on server socket");
				context.log("DCC send: "+source.getName()+" on "+ia.getHostAddress()+":"+port);

				while(true)
				{
					try
					{
						s=p.accept(1000);
						s.setSoTimeout(0); // Just in case this is needed
						context.logDebug("Got connection on server socket");
						context.log("DCC send: connected from "+s.getInetAddress().getHostAddress());
						break;
					}
					catch(SocketTimeoutException e)
					{
						if(cancelled) return;
					}
				}
			}
			catch(IOException e)
			{
				tp.error("Problem with local socket",e);
				return;
			}

			// No more need to listen for resume!
			context.unrequestMessages(null,this,PluginContext.ALLREQUESTS);

			// Open file and skip start if requested
			try
			{
				fis=new FileInputStream(source);
				if(startPos>0)
				{
					context.logDebug("Skipping "+startPos+" bytes");
					long remaining=startPos-fis.skip(startPos);
					while(remaining>0)
					{
						if(fis.read()==-1)
						{
							tp.error("Unexpected error scanning file");
							return;
						}
						remaining--;
					}
				}
			}
			catch(IOException e)
			{
				tp.error("Error opening local file",e);
				return;
			}

			tp.status("Sending...");
			context.logDebug("Beginning send");

			try
			{
				OutputStream os=s.getOutputStream();
				InputStream is=s.getInputStream();

				byte[] buffer=new byte[BLOCKSIZE],ackBuffer=new byte[BLOCKSIZE];
				long sent=startPos;
				while(sent<size)
				{
					// Read but ignore any acknowledgements
					while(true)
					{
						int ack=is.available();
						if(ack==0) break;
						ack=Math.max(ack,ackBuffer.length);
						context.logDebug("Reading and ignoring ack");
						is.read(ackBuffer,0,ack);
					}

					// Get new data from file and write it
					int read;
					try
					{
						read=fis.read(buffer);
					}
					catch(IOException e)
					{
						tp.error("Error reading local file",e);
						return;
					}

					if(cancelled) return;
					context.logDebug("Sending "+read+" bytes");
					os.write(buffer,0,read);
					os.flush();
					if(cancelled) return;
					sent+=read;
					tp.setTransferred(sent);
				}

				tp.setFinished();

				// Wait for CLOSEDELAY milliseconds after last received ack, then close
				// the socket
				long closeAfter=System.currentTimeMillis()+CLOSEDELAY;
				while(true)
				{
					long now=System.currentTimeMillis();
					if(now>closeAfter) break;
					int ack=is.available();
					if(ack!=0)
					{
						ack=Math.max(ack,ackBuffer.length);
						context.logDebug("Reading ack");
						is.read(ackBuffer,0,ack);
						closeAfter=now+CLOSEDELAY;
					}
					try
					{
						sleep(250);
					}
					catch(InterruptedException e)
					{
						throw new BugException(e);
					}
				}

				context.logDebug("Closing socket");
				context.log("DCC send: complete");
				os.close();
				s.close();
			}
			catch(IOException e)
			{
				tp.error("Connection error",e);
				return;
			}
		}
		finally
		{
			try { 	if(fis!=null) fis.close(); } catch(IOException e) {}
			try { if(p!=null) p.close(); } catch(IOException e) {}
			try { if(s!=null) s.close(); } catch(IOException e) {}
		}
	}


	private final static int BLOCKSIZE=4096;
	private final static long CLOSEDELAY=3000L;
}
