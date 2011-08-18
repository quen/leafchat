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

import org.w3c.dom.Element;

import util.xml.*;

import com.leafdigital.irc.api.*;
import com.leafdigital.ircui.api.*;
import com.leafdigital.logs.api.Logger;
import com.leafdigital.net.api.Network;

import leafchat.core.api.*;

/**
 * Implements the DCC chat window.
 */
public class DCCChatWindow implements GeneralChatWindow.Handler
{
	private GeneralChatWindow w;
	private PluginContext context;
	private String nick;
	private Server server;

	private OutputStream output;

	private boolean cancelled=false;

	private InetAddress remoteAddress;
	private int remotePort;

	/**
	 * Constructs window (outgoing).
	 * @param context Plugin context
	 * @param s Server
	 * @param nick Nickname
	 */
	public DCCChatWindow(PluginContext context, Server s, String nick)
	{
		this(context, s, nick, null, 0);
	}

	/**
	 * Constructs window (incoming).
	 * @param context Plugin context
	 * @param s Server
	 * @param nick Nickname
	 * @param address Address for connection
	 * @param port Port for connection
	 */
	public DCCChatWindow(PluginContext context, Server s, String nick, InetAddress address, int port)
	{
		this.context = context;
		this.nick = nick;
		this.server = s;

		// Create window, minimised if this is newly-received message
		IRCUI ircui = context.getSingle(IRCUI.class);
		w = ircui.createGeneralChatWindow(context, this, "DCC",
			Logger.CATEGORY_USER, nick, 510, s.getOurNick(), nick, address!=null);
		w.setTitle(nick + " - DCC chat");
		w.setEnabled(false);

		// Listen or connect?
		if(address == null)
		{
			(new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					runServer();
				}
			}, "DCC chat server thread: " + nick)).start();
		}
		else
		{
			this.remoteAddress = address;
			this.remotePort = port;

			w.addLine("<nick>" + XML.esc(nick) + "</nick> ("+
				"<key>" + remoteAddress.getHostAddress() + "</key>:<key>" + remotePort + "</key>) " +
				"has requested that you connect directly for a conversation that " +
				"doesn't go via the IRC server. If you weren't expecting it, close " +
				"this window.");
			w.addLine("<internalaction id='connect'>Accept and connect</internalaction>.");
		}
	}

	/** Just used to track whether user has clicked the Connect link or not */
	private boolean clicked;

	@Override
	public void internalAction(Element e) throws GeneralException
	{
		try
		{
			if(XML.getRequiredAttribute(e, "id").equals("connect") && !clicked)
			{
				clicked = true;
				connect();
			}
		}
		catch(XMLException ex)
		{
			throw new BugException(ex);
		}
	}

	private void connect()
	{
		(new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				runClient();
			}
		}, "DCC chat client thread: "+nick)).start();
	}

	private void error(String xml)
	{
		w.addLine(xml);
		w.setEnabled(false);
		cancelled=true;
	}

	private void runClient()
	{
		context.log("DCC chat: connecting to " + remoteAddress.getHostAddress() + ":"+remotePort);
		w.addLine("Connecting to <key>" + remoteAddress.getHostAddress() + "</key>:<key>"+remotePort+"</key>...");
		Network n = context.getSingle(Network.class);
		Socket s;
		InputStream input;
		try
		{
			s = n.connect(remoteAddress.getHostAddress(), remotePort, 30000);
			output = s.getOutputStream();
			input = s.getInputStream();
		}
		catch(IOException e)
		{
			error("Connection failed: <error>" + XML.esc(e.getMessage()) + "</error>");
			return;
		}

		try
		{
			connected(input);
		}
		finally
		{
			try
			{
				s.close();
			}
			catch(IOException e)
			{
			}
		}
	}

	private void runServer()
	{
		Network.Port p = null;
		Socket s = null;
		try
		{
			InetAddress ia;
			long ipnumber;
			try
			{
				p = ((DCCPlugin)context.getPlugin()).getDCCListenPort(nick);
				ia = p.getPublicAddress();
				ipnumber = ((DCCPlugin)context.getPlugin()).getStupidIPNumber(ia);
			}
			catch(GeneralException e)
			{
				error("Connection setup failed: <error>" + XML.esc(e.getMessage()) + "</error>");
				return;
			}
			int port = p.getPublicPort();

			// OK, we have a port and stupid-version IP, let's send the DCC message.
			try
			{
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				out.write(IRCMsg.constructBytes(
					"PRIVMSG " + nick + " :\u0001DCC CHAT chat " + ipnumber + " " + port + "\u0001"));
				server.sendLine(out.toByteArray());
				context.logDebug("Sent DCC CHAT: " + IRCMsg.convertISO(out.toByteArray()));
			}
			catch(IOException e)
			{
				error("Failed to send chat request: <error>" + XML.esc(e.getMessage()) + "</error>");
				return;
			}

			// That's great, now let's listen
			InputStream input;
			try
			{
				context.log("DCC chat: listening on " + ia.getHostAddress() + ":" + port);
				w.addLine("Waiting for a connection on <key>" + ia.getHostAddress() + "</key>:<key>" + port + "</key>...");

				while(true)
				{
					try
					{
						s = p.accept(1000);
						s.setSoTimeout(1000);
						context.logDebug("Got connection on server socket");
						context.log("DCC send: connected from " + s.getInetAddress().getHostAddress());
						break;
					}
					catch(SocketTimeoutException e)
					{
					}
					if(cancelled)
					{
						return;
					}
				}
				output = s.getOutputStream();
				input = s.getInputStream();
			}
			catch(IOException e)
			{
				error("Problem with local socket: <error>" + XML.esc(e.getMessage()) + "</error>");
				return;
			}

			// Now do connected bit
			connected(input);
		}
		finally
		{
			try
			{
				if(p != null)
				{
					p.close();
				}
				if(s!=null)
				{
					s.close();
				}
			}
			catch(IOException e)
			{
			}
		}
	}

	private void connected(InputStream input)
	{
		w.addLine("Connected and ready.");
		w.setEnabled(true);

		byte[] buffer = new byte[1024];
		byte[] leftovers = new byte[0];
		while(true)
		{
			try
			{
				int read = input.read(buffer);
				if(read == -1)
				{
					error("Connection closed.");
					return;
				}

				leftovers = handleBuffer(buffer, read, leftovers);
			}
			catch(SocketTimeoutException e)
			{
			}
			catch(IOException e)
			{
				error("Error reading data: <error>" + XML.esc(e.getMessage()) + "</error>");
				return;
			}
			if(cancelled)
			{
				return;
			}
		}
	}

	private byte[] handleBuffer(byte[] buffer, int read, byte[] leftovers)
	{
		for(int i=0; i<read; i++)
		{
			int foundsize = 0;
			if(i < read - 1 && buffer[i] == '\r' && buffer[i + 1] == '\n')
			{
				foundsize = 2;
			}
			else if(buffer[i] == '\n')
			{
				foundsize = 1;
			}
			if(foundsize>0)
			{
				byte[] line = new byte[leftovers.length + i];
				System.arraycopy(leftovers, 0, line, 0, leftovers.length);
				System.arraycopy(buffer, 0, line, leftovers.length, i);
				handleLine(line);
				System.arraycopy(buffer, i + foundsize, buffer, 0, read - (i + foundsize));
				read -= (i + foundsize);
				leftovers = new byte[0];
				return handleBuffer(buffer, read, leftovers);
			}
		}
		byte[] newLeftovers = new byte[read + leftovers.length];
		System.arraycopy(leftovers, 0, newLeftovers, 0, leftovers.length);
		System.arraycopy(buffer, 0, newLeftovers, leftovers.length, read);
		return newLeftovers;
	}

	private void handleLine(byte[] line)
	{
		IRCEncoding.EncodingInfo encoding = getEncoding();

		// Is it a /me?
		if(line[0] == 1 && line.length > 7 && (new String(line, 1, 7)).equals("ACTION "))
		{
			int length = line.length - 8;
			if(line[line.length - 1] == 1)
			{
				length--; // Optional ^A at end
			}
			byte[] data = new byte[length];
			System.arraycopy(line, 8, data, 0, length);
			String text = encoding.convertIncoming(data);
			w.showRemoteText(MessageDisplay.TYPE_ACTION, nick, text);
		}
		else
		{
			w.showRemoteText(MessageDisplay.TYPE_MSG, nick, encoding.convertIncoming(line));
		}
	}

	private IRCEncoding.EncodingInfo getEncoding()
	{
		IRCEncoding encoding = context.getSingle(IRCEncoding.class);
		IRCEncoding.EncodingInfo ei = encoding.getEncoding(
			null, null, new IRCUserAddress(nick, "", ""));
		return ei;
	}

	private final static byte[] CRLF={'\r', '\n'};
	private final static byte[] ACTIONSTART={1, 'A', 'C', 'T', 'I', 'O', 'N', ' '};
	private final static byte[] ACTIONEND={1, '\r', '\n'};

	@Override
	public void doCommand(Commands c, String line)
	{
		if(line.length()==0)
		{
			return; // May already have been checked but
		}
		IRCEncoding.EncodingInfo ei = getEncoding();
		try
		{
			if(!c.isCommandCharacter(line.charAt(0)))
			{
				// Send line, CRLF terminated
				output.write(ei.convertOutgoing(line));
				output.write(CRLF);
				output.flush();
				w.showOwnText(MessageDisplay.TYPE_MSG, nick, line);
			}
			else if(line.length() >= 4 && line.substring(1, 4).equalsIgnoreCase("me "))
			{
				output.write(ACTIONSTART);
				output.write(ei.convertOutgoing(line.substring(4)));
				output.write(ACTIONEND);
				output.flush();
				w.showOwnText(MessageDisplay.TYPE_ACTION, nick, line.substring(4));
			}
			else // Run as normal command
			{
				c.doCommand(line, null, new IRCUserAddress(nick, "", ""), null, w.getMessageDisplay(), false);
			}
		}
		catch(IOException e)
		{
			error("Error sending data: <error>" + e.getMessage() + "</error>");
		}
	}

	@Override
	public void windowClosed()
	{
		cancelled = true; // If still waiting for connection, this stops it
	}
}
