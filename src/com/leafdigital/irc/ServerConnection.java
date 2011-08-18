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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import javax.net.ssl.SSLSocket;

import util.StringUtils;
import util.xml.XML;

import com.leafdigital.irc.api.*;
import com.leafdigital.net.api.Network;
import com.leafdigital.prefs.api.*;

import leafchat.core.api.*;

/** Represents a connection to a server. */
public class ServerConnection implements Server, IRCPrefs
{
	private final static boolean TRACECOMMS=false;
	/**
	 * How often to send a server ping if we don't receive anything, in multiples
	 * of 10 seconds.
	 */
	private final static int
		SERVERPING_10SECS_NORMAL=10,
		SERVERPING_10SECS_FREQUENT=3;

	private PluginContext context;
	private Socket s=null;
	private InputStream input;
	private OutputStream output;
	private String host, address;
	private String reportedHost,version;
	private int port;
	private int secureMode;
	private boolean gotEndOfMOTD=false,connectionFinished=false;
	private boolean quitRequested=false;
	private boolean away=false;
	private boolean deferConnectionFinished=false;

	private Map<String, Object> properties=new HashMap<String, Object>();

	private ConnectionsSingleton connections;

	private Throwable error=null;

	/** Current details of this user */
	private String ourNick,ourUser,ourHost;

	/** @return How often (in 10-second units) we should send server pings if no data */
	private int getServerPingFrequency()
	{
		Preferences p=context.getSingle(Preferences.class);
		if(p.toBoolean(p.getGroup(context.getPlugin()).get(PREF_FREQUENTPINGS,PREFDEFAULT_FREQUENTPINGS)))
			return SERVERPING_10SECS_FREQUENT;
		return SERVERPING_10SECS_NORMAL;
	}

	ServerConnection(ConnectionsSingleton cs,PluginContext context)
	{
		this.connections=cs;
		this.context=context;
	}

	@Override
	public String toString()
	{
		return host+":"+port;
	}

	@Override
	public void beginConnect(String sHost,int iPort,Server.ConnectionProgress cp)
	{
		this.host=sHost;
		this.port=iPort;
		String secureString =
			getPreferences().get(PREF_SECUREMODE, PREFDEFAULT_SECUREMODE);
		if(secureString.equals(PREF_SECUREMODE_NONE))
		{
			secureMode = Network.SECURE_NONE;
		}
		else if(secureString.equals(PREF_SECUREMODE_REQUIRED))
		{
			secureMode = Network.SECURE_REQUIRED;
		}
		else
		{
			secureMode = Network.SECURE_OPTIONAL;
		}
		new ServerThread(cp);
	}

	@Override
	public boolean isConnected()
	{
		return s!=null;
	}

	@Override
	public boolean isConnectionFinished()
	{
		return connectionFinished;
	}

	@Override
	public NetworkException getError()
	{
		if(error==null) return null;
		if(error instanceof NetworkException) return (NetworkException) error;
		return new NetworkException(error);
	}

	@Override
	public void sendLine(byte[] line)
	{
		sendServerRequest(line);
	}

	// WHO
	//  RPL_WHOREPLY
	//  RPL_ENDOFWHO

	//private final static

	/**
	 * Details about a particular type of command we track.
	 */
	private class TrackedCommand
	{
		private String command;
		private int[] includedNumerics;
		private int finalNumeric;

		private LinkedList<Integer> queue = new LinkedList<Integer>();

		/**
		 * @param command Command name in sent message
		 * @param includedNumerics Numerics that belong to the response
		 * @param finalNumeric Numeric that indicates the end of a response
		 */
		TrackedCommand(String command,int[] includedNumerics,
			int finalNumeric)
		{
			this.command=command;
			this.includedNumerics=includedNumerics;
			this.finalNumeric=finalNumeric;

			// Index the command name in the global list
			trackedCommandNames.put(command,this);

			// Index the required numerics in the global list
			int[] allNumerics=new int[includedNumerics.length+1];
			System.arraycopy(includedNumerics,0,allNumerics,1,includedNumerics.length);
			includedNumerics[0]=finalNumeric;

			for(int i=0;i<allNumerics.length;i++)
			{
				Integer key = allNumerics[i];
				if(trackedCommandNumerics.containsKey(key))
					throw new BugException("Duplicate numeric in tracked commands: "+key);
				trackedCommandNumerics.put(key,this);
			}
		}

		/**
		 * Adds a new request to the queue.
		 * @param trackingNumber Tracking number of the request
		 */
		synchronized void queueRequest(int trackingNumber)
		{
			queue.addLast(trackingNumber);
		}

		/**
		 * Handles a message that belongs to this
		 * @param msg
		 */
		synchronized void handleMessage(NumericIRCMsg msg)
		{
			// Unexpected message, doesn't correspond to a queue
			if(queue.isEmpty()) return;

			// Set response ID
			msg.setResponseID(queue.getFirst().intValue());

			// If this was the last one, pull it out of the queue
			if(msg.getNumeric()==finalNumeric) queue.removeFirst();
		}

		@Override
		public String toString()
		{
			String result="[TrackedCommand "+command;
			for(int i=0;i<includedNumerics.length;i++)
			{
				result+=","+includedNumerics[i];
			}
			result+=" -> "+finalNumeric;
			return result;
		}
	}

	/** Initialises list of tracked commands */
	void initTrackedCommands()
	{
		new TrackedCommand("WHO",
			new int[] {NumericIRCMsg.RPL_WHOREPLY},
			NumericIRCMsg.RPL_ENDOFWHO);
	};

	/** Index of tracked commands by involved numerics */
	private HashMap<Integer, TrackedCommand> trackedCommandNumerics =
		new HashMap<Integer, TrackedCommand>();
	/** Index of tracked commands by command name */
	private HashMap<String, TrackedCommand> trackedCommandNames =
		new HashMap<String, TrackedCommand>();

	/** ID used for next globally unique tracking number */
	private static int nextTrackingNumber=1;
	private static Object trackingNumberSynch=new Object();

	private int getTrackingNumber(String command)
	{
		TrackedCommand tc=trackedCommandNames.get(command);
		if(tc==null) return UNTRACKED_REQUEST;

		int number;
		synchronized(trackingNumberSynch)
		{
			number=nextTrackingNumber++;
		}
		tc.queueRequest(number);

		return nextTrackingNumber;
	}

	@Override
	public int sendServerRequest(byte[] line)
	{
		// Get command
		int commandLength=0;
		for(;commandLength<line.length;commandLength++)
		{
			if(line[commandLength]==' ') break;
		}
		String command;
		try
		{
			command=new String(line,0,commandLength,"US-ASCII");
		}
		catch(UnsupportedEncodingException e)
		{
			throw new BugException(e);
		}

		// Track when quit is requested
		if(command.equals("QUIT"))
			quitRequested=true;

		// Send
		buffer.send(line);

		return getTrackingNumber(command);
	}

	/** Buffer used to keep send rate safe */
	class SendBuffer implements Runnable
	{
		private final static int
			MILLISECONDS_PER_LINE=2000,
			BURST_SIZE=5;

		private LinkedList<byte[]> backlog=null;
		private long lastSend=0;
		private int sendCount=0;

		synchronized void send(byte[] line)
		{
			// If there's anything in the send buffer, just add this line
			if(backlog!=null)
			{
				backlog.addLast(line);
				return;
			}

			// Waiting 2N seconds reduces your send count by N
			long now=System.currentTimeMillis();
			if(sendCount>0)
			{
				long secondsDelay=(now-lastSend)/MILLISECONDS_PER_LINE;
				sendCount-=secondsDelay;
				if(sendCount<0) sendCount=0;
			}

			// Now increment send count for this message
			sendCount++;
			if(sendCount>BURST_SIZE)
			{
				backlog = new LinkedList<byte[]>();
				backlog.addLast(line);
				(new Thread(this,"Send buffer")).start();
			}
			else
			{
				lastSend=now;
				connections.informSend(ServerConnection.this,line);
			}
		}

		@Override
		public void run()
		{
			while(true)
			{
				byte[] nextLine;
				synchronized(this)
				{
					if(backlog.size()==0)
					{
						backlog=null;
						return;
					}
					nextLine=backlog.removeFirst();
				}

				try
				{
					Thread.sleep(MILLISECONDS_PER_LINE);
				}
				catch(InterruptedException ie)
				{
				}

				connections.informSend(ServerConnection.this,nextLine);
				lastSend=System.currentTimeMillis();
			}
	  }
	}

	SendBuffer buffer=new SendBuffer();

	/**
	 * Message: Send line.
	 * @param msg Message
	 * @throws NetworkException
	 */
	public synchronized void msg(ServerSendMsg msg) throws NetworkException
	{
		if(msg.isHandled()) return;
		if(s!=null) internalSend(msg.getLine(),false);
		msg.markHandled();
	}

	private synchronized void internalSend(byte[] abLine,boolean bFlush) throws NetworkException
	{
		if(s==null) throw new NetworkException("Not connected");
		try
		{
			byte[] complete=new byte[abLine.length+2];
			System.arraycopy(abLine,0,complete,0,abLine.length);
			complete[abLine.length]=13; // CR
			complete[abLine.length+1]=10; // LF
			output.write(complete);
			if(bFlush) output.flush();
			if(TRACECOMMS) System.err.println(">> "+new String(abLine));
		}
		catch(IOException e)
		{
			context.log("Error sending data to server",e);
			disconnect();
		}
	}

	@Override
	public void disconnect()
	{
		synchronized(this)
		{
			if(s==null) return;
			try
			{
				s.close();
			}
			catch(IOException e)
			{
			}
			s=null; // Probably not required, but just to make sure thread doesn't mess
		}
	}

	@Override
	public void disconnectGracefully()
	{
		try
		{
			IRCEncoding ie=connections.getPluginContext().getSingle(IRCEncoding.class);
			IRCEncoding.EncodingInfo ei=ie.getEncoding(this,null,null);
	 		internalSend(IRCMsg.constructBytes(
				"QUIT :",ei.convertOutgoing(getQuitMessage())),true);
		}
		catch(GeneralException ge)
		{
		}
		disconnect();
	}

	private long lastLineTime;

	/** @return Time at which last line was received from server */
	long getLastLineTime()
	{
		return lastLineTime;
	}

	private int waitingTimes=0;

	@Override
	public synchronized boolean isSecureConnection()
	{
		return s!=null && (s instanceof SSLSocket);
	}

	/** Thread that connects to server and reads server data */
	private class ServerThread extends Thread
	{
		private ConnectionProgress cp;

		ServerThread(Server.ConnectionProgress cp)
		{
			super("Server thread - "+host+":"+port);
			this.cp=cp;
			start();
		}

		@Override
		public void run()
		{
			// Connect to socket
			try
			{
				cp.progress("Looking up <key>"+XML.esc(host)+"</key>...");
				InetAddress ia=InetAddress.getByName(host);
				address = ia.getHostAddress();
				cp.progress("Connecting to <key>"+ia.getHostAddress()+"</key>...");
				s=context.getSingle(Network.class).connect(host, port, 30000, secureMode);
				s.setSoTimeout(10000);
				if(isSecureConnection())
				{
					cp.progress("<key>Connected! Connection is <key>secure</key>.</key>");
				}
				else
				{
					cp.progress("<key>Connected! Connection is <key>unencrypted</key>.</key>");
				}
				input=s.getInputStream();
				output=s.getOutputStream();
			}
			catch(Throwable t)
			{
				s=null;
				error=t;
			}

			try
			{
				// Start listening for server send messages
				ServerFilter sf=new ServerFilter(ServerConnection.this);
				connections.getPluginContext().requestMessages(
					ServerSendMsg.class,ServerConnection.this,
					sf,Msg.PRIORITY_NORMAL);

				// To track nickname send
				connections.getPluginContext().requestMessages(
					NumericIRCMsg.class,ServerConnection.this,sf,Msg.PRIORITY_FIRST);
				connections.getPluginContext().requestMessages(
					UserSourceIRCMsg.class,ServerConnection.this,sf,Msg.PRIORITY_FIRST); // Includes nick
				connections.getPluginContext().requestMessages(
					ServerConnectionFinishedMsg.class,ServerConnection.this,sf,Msg.PRIORITY_FIRST);
			}
			catch(Throwable t)
			{
				try
				{
					s.close();
				}
				catch(IOException e1)
				{
				}
				s=null;
				error=t;
			}

			// Connection is over, so no need for progress
			cp=null;

			// Notify that either connection or error occurred
			synchronized(ServerConnection.this)
			{
				ServerConnection.this.notifyAll();
				if(error!=null) return;
			}

			// OK we're connected, inform (now guaranteed to send disconnected too)
			connections.informConnected(ServerConnection.this);

			try
			{
				// Repeatedly read server data
				byte[] abBuffer=new byte[510];
				int iPos=0;
				int delayCount=0;
				while(true)
				{
					// Keep reading bytes until we get a CR or LF
					try
					{
						int iByte=input.read();
						if(iByte==-1 || s==null) break;
						delayCount=0;

						if(iByte==10 || iByte==13)
						{
							if(iPos>0)
							{
								byte[] abLine=new byte[iPos];
								System.arraycopy(abBuffer,0,abLine,0,iPos);
								lastLineTime=System.currentTimeMillis();
								connections.informLine(ServerConnection.this,abLine);
								if(TRACECOMMS) System.err.println("<< "+new String(abLine));
								iPos=0;
							}
						}
						else
						{
							if(iPos>=abBuffer.length)
								throw new NetworkException("IRC server sent a line longer than 510 bytes (rogue server?)");
							abBuffer[iPos++]=(byte)iByte;
						}
					}
					catch(SocketTimeoutException e)
					{
						delayCount++;
						int target=getServerPingFrequency();
						if(delayCount==target)
						{
							// Hrm, looks like we haven't received anything in 100 seconds
							// (more than most server ping timeouts). Let's send a TIME,
							// supposing the server is still around.
							synchronized(ServerConnection.this)
							{
								if(s==null) break;
								waitingTimes++;
								internalSend(IRCMsg.constructBytes("TIME"),true);
							}
						}
						else if(delayCount>=target+2)
						{
							// Still not got anything? That's bad. Let's drop the connection
							throw e;
						}
					}
				}
			}
			catch(Throwable t)
			{
				error=t;
			}
			finally
			{
				synchronized(ServerConnection.this)
				{
					try
					{
						if(s!=null) s.close();
					}
					catch(IOException e)
					{
					}
					s=null;
				}
				connections.informDisconnected(ServerConnection.this,error);
				connections.getPluginContext().unrequestMessages(
					null,ServerConnection.this,PluginContext.ALLREQUESTS);
			}
		}
	}

	@Override
	public PreferencesGroup getPreferences()
	{
		if(host==null) throw new BugException("Not connected");

		// Use reported host if we have one, otherwise regular host
		String sSearch=reportedHost;
		if(sSearch==null) sSearch=host;

		// Find preferences group for server
		Preferences p=
			connections.getPluginContext().getSingle(Preferences.class);
		PreferencesGroup pg=p.getGroup(connections.getPlugin()).getChild("servers");
		PreferencesGroup pgNow=pg.findAnonGroup(PREF_HOST, sSearch, true, true);

		// Make a new one if there wasn't one already
		if(pgNow==null)
		{
			pgNow=pg.addAnon();
			pgNow.set(PREF_HOST,sSearch);
			pgNow.set(PREF_PORTRANGE,port+"");
		}

		return pgNow;
	}

	/**
	 * Called to set the server name (what the server thinks its name is, not what
	 * was connected with)
	 * @param reportedHost
	 */
	private void setReportedHost(String reportedHost)
	{
		this.reportedHost = reportedHost;

		Preferences p =
			connections.getPluginContext().getSingle(Preferences.class);
		PreferencesGroup pg = p.getGroup(connections.getPlugin()).getChild("servers");

		PreferencesGroup thisGroup=null;

		// Find preferences group for server
		PreferencesGroup existingGroup = pg.findAnonGroup(PREF_HOST, reportedHost, true, true);
		if(existingGroup != null) // Existing group
		{
			// Mark that we've actually had this host reported now
			existingGroup.set(PREF_REPORTED, "yes");
			thisGroup = existingGroup;

			// Set host option in case it's a different case
			existingGroup.set(PREF_HOST, reportedHost);

			// Are there settings for original connected host?
			if(!reportedHost.equalsIgnoreCase(host))
			{
				PreferencesGroup previousGroup = pg.findAnonGroup(PREF_HOST, host, true, true);
				if(previousGroup != null)
				{
					// Discard those settings, if we've never really connected (reported
					// connection) to there and they weren't added by hand
					if(previousGroup.get(PREF_REPORTED, "no").equals("no") &&
						previousGroup.get(PREF_HANDADDED, "no").equals("no"))
					{
						previousGroup.remove();
					}
				}
			}
		}
		else
		{
			// Are there settings for original connected host?
			if(!reportedHost.equalsIgnoreCase(host))
			{
				PreferencesGroup previousGroup = pg.findAnonGroup(PREF_HOST, host, true, true);
				if(previousGroup != null)
				{
					// Move those settings to this, if we've never really connected (reported
					// connection) to there
					if(previousGroup.get(PREF_REPORTED,"no").equals("no"))
					{
						previousGroup.set(PREF_HOST, reportedHost);
						thisGroup = previousGroup;
					}
				}
			}
		}

		// If we still haven't got preferences, make them up now
		if(thisGroup == null)
		{
			thisGroup = pg.addAnon();
			thisGroup.set(PREF_HOST, reportedHost);
			thisGroup.set(PREF_REPORTED, "yes");
		}

		// Only consider if refusednetwork is not set and it's not already in a
		// network
		if(thisGroup.get(PREF_REFUSEDNETWORK,"no").equals("no") &&
			thisGroup.getAnonHierarchical(PREF_NETWORK,null)==null)
		{
			try
			{
				// Don't send connection-finished until user has decided
				deferConnectionFinished=true;

				// Consider all existing networks to see if there's one with a matching
				// suffix.
				PreferencesGroup pgNetwork=findMatchingNetwork(pg,reportedHost);
				if(pgNetwork!=null)
				{
					// Offer add to that network
					switch(connections.informRearrange(new ServerRearrangeMsg(
						this,reportedHost,pgNetwork.get("network"))))
					{
					case ServerRearrangeMsg.CONFIRM:
						// Actually add (automatically removes from previous parent)
						pgNetwork.addAnon(thisGroup,PreferencesGroup.ANON_LAST);
						break;

					case ServerRearrangeMsg.REJECT:
						thisGroup.set(PREF_REFUSEDNETWORK,"yes");
						break;

					default:
						// Indecisive user
					}
				}
				else
				{
					// Determine suffix: if last segment of hostname is >2 characters,
					// use last 2 segments (e.g. .dal.net). If it's 2 characters, use last
					// 3 (e.g. mynet.co.uk).
					String[] asSegs=reportedHost.split("\\.");
					if(asSegs.length>=3)
					{
						String sSuffix;
						if(asSegs[asSegs.length-1].length()>=3)
						{
							sSuffix="."+asSegs[asSegs.length-2]+"."+asSegs[asSegs.length-1];
						}
						else
						{
							sSuffix="."+asSegs[asSegs.length-3]+"."+asSegs[asSegs.length-2]+
								"."+asSegs[asSegs.length-1];
						}

						// Consider all existing servers to see if there's one with that suffix that
						// doesn't have refusednetwork=yes. If so, offer add for those servers to
						// a new network.
						PreferencesGroup pgOther = findMatchingServer(pg,sSuffix,thisGroup);
						if(pgOther!=null)
						{
							String sNetwork=sSuffix.substring(1);
							// Offer combine with those servers to make network
							switch(connections.informRearrange(new ServerRearrangeMsg(
								this,reportedHost,sNetwork,pgOther.get("host"))))
							{
							case ServerRearrangeMsg.CONFIRM:
								// Turn other server into network
								pgOther.set(PREF_NETWORK,sNetwork);
								pgOther.set(PREF_NETWORKSUFFIX,sSuffix);

								// Make new server to remember other one
								PreferencesGroup pgNewOther=pgOther.addAnon();
								pgNewOther.set(PREF_HOST,pgOther.get(PREF_HOST));
								if(pgOther.exists(PREF_REPORTED))
									pgNewOther.set(PREF_REPORTED,pgOther.get(PREF_REPORTED));
								pgOther.unset(PREF_HOST);
								pgOther.unset(PREF_REPORTED);

								// Add this one to the new network
								pgOther.addAnon(thisGroup,PreferencesGroup.ANON_LAST);
								break;

							case ServerRearrangeMsg.REJECT:
								thisGroup.set(PREF_REFUSEDNETWORK,"yes");
								break;

							default:
								// Indecisive user
							}
						}
					}
				}
			}
			finally
			{
				deferConnectionFinished=false;
			}
		}
	}

	/**
	 * Recursively searches for server with the given suffix.
	 * @param pgParent Parent (start by calling with 'servers' root)
	 * @param sSuffix Suffix to match (not case-sensitive)
	 * @param pgExclude Exclude this group from consideration
	 * @return Matching server or null if none
	 */
	private static PreferencesGroup findMatchingServer(
		PreferencesGroup pgParent,String sSuffix,PreferencesGroup pgExclude)
	{
		// If it isn't the exclude one, and matches, and isn't in a network, and
		// hasn't refused the network
		if(pgParent!=pgExclude
			&& pgParent.get(PREF_HOST,"").toLowerCase().endsWith(sSuffix.toLowerCase())
			&& pgParent.getAnonHierarchical(PREF_NETWORK,null)==null
			&& pgParent.get(PREF_REFUSEDNETWORK,"no").equals("no"))
		{
			return pgParent;
		}

		// Try children too
		PreferencesGroup[] apgChildren=pgParent.getAnon();
		for(int i=0;i<apgChildren.length;i++)
		{
			PreferencesGroup pgFound=findMatchingServer(apgChildren[i],sSuffix,pgExclude);
			if(pgFound!=null) return pgFound;
		}

		return null;
	}

	/**
	 * Recursively searches for a network that matches the given host.
	 * @param pgParent Parent start by calling with 'servers' root)
	 * @param sHost Hostname to match (not case-sensitive)
	 * @return Prefs for matching network, or null if none
	 */
	private static PreferencesGroup findMatchingNetwork(
		PreferencesGroup pgParent,String sHost)
	{
		// If it matches, return it
		if(sHost.toLowerCase().endsWith(
			pgParent.get(PREF_NETWORKSUFFIX,"!fail").toLowerCase()))
			return pgParent;

		// Otherwise try children
		PreferencesGroup[] apgChildren=pgParent.getAnon();
		for(int i=0;i<apgChildren.length;i++)
		{
			PreferencesGroup pgFound=findMatchingNetwork(apgChildren[i],sHost);
			if(pgFound!=null) return pgFound;
		}

		// Nope, wasn't herre
		return null;
	}

	@Override
	public String getISupport(String sParameter)
	{
		return mISupport.get(sParameter);
	}

	/** Map of information from ISUPPORT */
	private Map<String, String> mISupport = new HashMap<String, String>();

	/** Regex patterns used for interpreting ISUPPORT */
	private final static Pattern
	  ISUPPORT_NEGATE=Pattern.compile("-([A-Z0-9]{1,20})"),
		ISUPPORT_EMPTY=Pattern.compile("([A-Z0-9]{1,20})=?"),
		ISUPPORT_VALUE=Pattern.compile("([A-Z0-9]{1,20})=([A-Za-z0-9!\"#$%&'()*+,\\-./:;<=>?@\\[\\\\\\]^_`{|}~]+)"),
		ISUPPORT_ESCAPE=Pattern.compile("\\\\x([0-9A-Fa-f]{2})");

	/**
	 * Called to set the ISUPPORT data.
	 * @param nim 005 numeric message
	 */
	private void setISupport(NumericIRCMsg nim)
	{
		// This is an implementation of the grammar defined in
		// http://www.irc.org/tech_docs/draft-brocklesby-irc-isupport-03.txt

		byte[][] aabParams=nim.getParams();
		for(int iParam=0;iParam<aabParams.length;iParam++)
		{
			// Ignore postfix param
			if(iParam==aabParams.length-1 && nim.includesPostfix()) break;

			// Convert param to string
			String s=IRCMsg.convertISO(aabParams[iParam]);

			Matcher m=ISUPPORT_NEGATE.matcher(s);
			if(m.matches()) // Negate
				mISupport.remove(m.group(1));
			else
			{
				m=ISUPPORT_EMPTY.matcher(s);
				if(m.matches())
					mISupport.put(m.group(1),"");
				else
				{
					m=ISUPPORT_VALUE.matcher(s);
					if(m.matches())
					{
						String sValue=m.group(2);

						// Implement \x20 style escapes
						Matcher mEscapes=ISUPPORT_ESCAPE.matcher(sValue);
						StringBuffer sb = new StringBuffer();
						while(mEscapes.find())
						{
							char c=(char)Integer.parseInt(mEscapes.group(1),16);
					     mEscapes.appendReplacement(sb,""+c);
						}
						mEscapes.appendTail(sb);

						mISupport.put(m.group(1),sb.toString());
					}
					else
					{
						// If we fail to match a token, ignore it
					}
				}
			}
		}
	}

	@Override
	public String getChanTypes()
	{
		String sTypes=getISupport("CHANTYPES");
		if(sTypes!=null)
			return sTypes;
		else
			return "#&";
	}

	@Override
	public int getMaxTopicLength()
	{
		String maxLength=getISupport("TOPICLEN");
		if(maxLength!=null && maxLength.matches("[0-9]+"))
			return Integer.parseInt(maxLength);
		else
			return 80;
	}

	@Override
	public String getStatusMsg()
	{
		String sStatusMsg=getISupport("STATUSMSG");
		if(sStatusMsg!=null)
			return sStatusMsg;
		else
		{
			// Correct behaviour according to the spec would be to assume no support.
			// But since some servers (esper.net I'm looking at you) don't report
			// STATUSMSG, we'll default to the PREFIX (or @+)
			Matcher m=ISUPPORTVAL_PREFIX.matcher(getValidPrefix());
			m.matches(); // It does, see below code for why
			return m.group(2);
		}
	}

	private final static Pattern ISUPPORTVAL_PREFIX=Pattern.compile("\\((.*?)\\)(.*)");
	private final static Pattern ISUPPORTVAL_CHANMODES=Pattern.compile("(.*?),(.*?),(.*?),(.*?)(,.*)?");

	private String getValidPrefix()
	{
		String sPrefix=getISupport("PREFIX");
		if(sPrefix!=null)
		{
			Matcher m=ISUPPORTVAL_PREFIX.matcher(sPrefix);
			if(m.matches() && m.group(1).length()==m.group(2).length())
				return sPrefix;
		}
		return "(ov)@+";
	}

	private String getValidChanModes()
	{
		String sChanModes=getISupport("CHANMODES");
		if(sChanModes!=null)
		{
			Matcher m=ISUPPORTVAL_CHANMODES.matcher(sChanModes);
			if(m.matches())
				return sChanModes;
		}
		return "b,k,l,imnpstr";
	}

	@Override
	public StatusPrefix[] getPrefix()
	{
		String sPrefix=getValidPrefix();

		Matcher m=ISUPPORTVAL_PREFIX.matcher(sPrefix);
		if(!m.matches()) assert false;

		String sModes=m.group(1),sPrefixes=m.group(2);
		StatusPrefix[] asp=new StatusPrefix[sModes.length()];
		for(int i=0;i<sModes.length();i++)
		{
			asp[i]=new StatusPrefix(sModes.charAt(i),sPrefixes.charAt(i));
		}
		return asp;
	}

	@Override
  public boolean isPrefixAtLeast(char prefix,char required)
  {
  		Server.StatusPrefix[] prefixes=getPrefix();
  		for(int i=0;i<prefixes.length;i++)
		{
  			if(prefixes[i].getPrefix()==prefix) return true;
  			if(prefixes[i].getPrefix()==required) break;
		}
  		return false;
  }

	@Override
	public String getChanModes()
	{
		String
		  sChanModes=getValidChanModes(),
		  sPrefix=getValidPrefix();
		StringBuffer sbModes=new StringBuffer();
		Matcher m=ISUPPORTVAL_CHANMODES.matcher(sChanModes);
		if(!m.matches()) assert false;
		for(int i=1;i<=4;i++) sbModes.append(m.group(i));
		m=ISUPPORTVAL_PREFIX.matcher(sPrefix);
		if(!m.matches()) assert false;
		sbModes.append(m.group(1));
		return sbModes.toString();
	}

	@Override
	public int getChanModeType(char cMode)
	{
		String sChanModes=getValidChanModes();
		Matcher m=ISUPPORTVAL_CHANMODES.matcher(sChanModes);
		if(!m.matches()) assert false;
		for(int i=1;i<=4;i++) if(m.group(i).indexOf(cMode)!=-1) return i;

		// Things in prefix are assigned CHANMODE_USERSTATUS
	  String sPrefix=getValidPrefix();
		m=ISUPPORTVAL_PREFIX.matcher(sPrefix);
		if(!m.matches()) assert false;
		if(m.group(1).indexOf(cMode)!=-1) return CHANMODE_USERSTATUS;

		return CHANMODE_UNKNOWN;
	}

	@Override
	public int getChanModeParamCount()
	{
		String modes=getISupport("MODES");
		if(modes!=null)
		{
			try
			{
				return Integer.parseInt(modes);
			}
			catch(NumberFormatException nfe)
			{
			}
		}
		return 3;
	}

	@Override
	public Object getProperty(Class<?> c,String sKey)
	{
		synchronized(properties)
		{
			return properties.get(c+"\n"+sKey);
		}
	}

	@Override
	public Object setProperty(Class<?> c,String sKey,Object oValue)
	{
		synchronized(properties)
		{
			return properties.put(c + "\n" + sKey, oValue);
		}
	}

	boolean hasGotEndOfMOTD()
	{
		return gotEndOfMOTD;
	}

	/**
	 * Message: Nickname change. Used to update our own nick.
	 * @param msg Message
	 */
	public void msg(NickIRCMsg msg)
	{
		// Update nickname if nick applies to us
		if(msg.getSourceUser().getNick().equals(ourNick))
		{
			ourNick=msg.getNewNick();
		}
	}

	/**
	 * Message: Any message. Used to track our own username/host.
	 * @param msg Message
	 */
	public void msg(UserSourceIRCMsg msg)
	{
		// If it's our nickname and we don't already have a un/host:
		if(ourUser==null && !msg.getSourceUser().getHost().equals("") &&
			msg.getSourceUser().getNick().equals(ourNick))
		{
			ourUser=msg.getSourceUser().getUser();
			ourHost=msg.getSourceUser().getHost();
		}
	}

	/** Keep track of how many times we've tried different nicks */
	int iNickRetries=0;

	/**
	 * Message: Numerics. Used for connection nickname renaming and to track
	 * various other info.
	 * @param msg Message
	 * @throws GeneralException
	 */
	public void msg(NumericIRCMsg msg) throws GeneralException
	{
		// Handle tracking response numbers
		TrackedCommand tc=
			trackedCommandNumerics.get(msg.getNumeric());
		if(tc!=null) tc.handleMessage(msg);

		// Handle nickname changing
		if(msg.getNumeric()==NumericIRCMsg.ERR_NICKNAMEINUSE ||
			msg.getNumeric()==NumericIRCMsg.ERR_ERRONEUSNICKNAME)
		{
			// If this happens after connection, ignore it. Automatic changing should
			// only apply on connect (and doesn't work later, because the iNickRetries
			// never gets reset...)
			if(ourNick!=null) return;

			// If the message isn't valid (no param), ignore it
			if(msg.getParams().length < 2) return;

			// ...Better change it then!
			String sAttemptNick=msg.getParamISO(1);
			if(iNickRetries<3)
			{
				// Add _ characters at the end
				sAttemptNick+="_";
			}
			else if(iNickRetries<6)
			{
				if(iNickRetries==4)
				{
					// Trim off the _ characters at end
					sAttemptNick=sAttemptNick.substring(0,sAttemptNick.length()-3);
				}
				// Add _ characters at beginning
				sAttemptNick="_"+sAttemptNick;
			}
			else
			{
				// Pick a random (and 9-letter safe) nick
				Random r=new Random();
				sAttemptNick="lcuser"+r.nextInt(1000);
			}
			sendLine(IRCMsg.constructBytes("NICK "+sAttemptNick));
			iNickRetries++;

			return;
		}

		// Update nickname from server numeric if needed (should happen on first line
		// after connection registration, but on some cases it doesn't, we get * or
		// blank for various reasons)
		if(ourNick==null && !msg.getTarget().matches("\\*?")) ourNick=msg.getTarget();

		switch(msg.getNumeric())
		{
		case NumericIRCMsg.RPL_ISUPPORT:
			setISupport(msg);
			msg.markHandled();
			break;
		case NumericIRCMsg.RPL_MYINFO:
			if(msg.getParams().length >= 3)
			{
				version = msg.getParamISO(2);
			}
			if(msg.getParams().length >= 2)
			{
				String claimedHost = msg.getParamISO(1);
				// Some idiot servers put colours in this, so strip them out
				claimedHost = context.getSingle(IRCEncoding.class).
					processEscapes(claimedHost, false, false);
			  setReportedHost(claimedHost);
			}
		  break;
		case NumericIRCMsg.RPL_ENDOFMOTD:
			if(!gotEndOfMOTD)
			{
				gotEndOfMOTD=true;
				identify(ourNick,Server.IDENTIFYEVENT_CONNECT);
			}
			break;
		case NumericIRCMsg.RPL_WELCOME:
			// If username and hostname are in the welcome message, then obtain them
			String sText=IRCMsg.convertISO(msg.getParams()[msg.getParams().length-1]);
			Matcher m=Pattern.compile("^.* "+StringUtils.regexpEscape(ourNick)+"!(.*?)@(.*?)( .*)?$").matcher(sText);
			if(m.matches())
			{
				ourUser=m.group(1);
				ourHost=m.group(2);
			}
			break;

		case NumericIRCMsg.RPL_TIME:
			synchronized(ServerConnection.this)
			{
				if(waitingTimes>0)
				{
					waitingTimes--;
					msg.markHandled();
				}
			}
			break;

		case NumericIRCMsg.RPL_NOWAWAY:
			away=true;
			break;

		case NumericIRCMsg.RPL_UNAWAY:
			away=false;
			break;
		}
	}

	/**
	 * Message: Connection finished. Used to provide the boolean and to handle
	 * auto-join and to update security settings.
	 * @param msg Message
	 * @throws GeneralException
	 */
	public void msg(ServerConnectionFinishedMsg msg) throws GeneralException
	{
		// Update security mode if it's currently set to optional - note we
		// only do this on connection finished so we know it really worked
		boolean optional =
			getPreferences().get(PREF_SECUREMODE, PREFDEFAULT_SECUREMODE).
			equals(PREF_SECUREMODE_OPTIONAL);
		if(optional)
		{
			if(isSecureConnection())
			{
				getPreferences().set(PREF_SECUREMODE, PREF_SECUREMODE_REQUIRED);
			}
			else
			{
				getPreferences().set(PREF_SECUREMODE, PREF_SECUREMODE_NONE);
			}
		}
		connectionFinished=true;
		autojoin();
	}

	private void autojoin() throws GeneralException
	{
		Preferences p=connections.getPluginContext().getSingle(Preferences.class);

		PreferencesGroup serverPrefs=getPreferences(),networkPrefs=serverPrefs.getAnonParent();
		PreferencesGroup[] serverAndNetwork;
		if(networkPrefs==null)
			serverAndNetwork=new PreferencesGroup[] {serverPrefs};
		else
			serverAndNetwork=new PreferencesGroup[] {serverPrefs,networkPrefs};

		for(int group=0;group<serverAndNetwork.length;group++)
		{
			PreferencesGroup[] channels=
				serverAndNetwork[group].getChild(PREFGROUP_CHANNELS).getAnon();
			for(int channel=0;channel<channels.length;channel++)
			{
				PreferencesGroup thisChan=channels[channel];
				if(p.toBoolean(thisChan.get(PREF_AUTOJOIN)))
				{
					String name=thisChan.get(PREF_NAME),key=thisChan.get(PREF_KEY);
					sendLine(IRCMsg.constructBytes("JOIN "+name+(key.length()>0 ? " "+key:"")));
				}
			}
		}
	}

	/**
	 * Triggers identify in response to some event.
	 * @param nick Current/new nickname
	 * @param identifyEvent Event type
	 */
	public void identify(String nick,String identifyEvent)
	{
		String sPassword=getNickPassword(nick);
		if(sPassword!=null && !sPassword.equals("") && shouldIdentify(identifyEvent))
		{
			String sCommand=getIdentifyPattern();
			sCommand=StringUtils.replace(sCommand,"${password}",sPassword);
			sCommand=StringUtils.replace(sCommand,"${nick}",nick);

			// For this we use a special message display that doesn't display normal
			// results, only error. This is because we don't want to display the
			// password if they use /msg
			final MessageDisplay actual = getDefaultMessageDisplay();
			MessageDisplay secret = new MessageDisplay()
			{
				@Override
				public void showError(String message)
				{
					actual.showError(message);
				}

				@Override
				public void showInfo(String message)
				{
				}

				@Override
				public void showOwnText(int type, String target, String text)
				{
				}

				@Override
				public void clear()
				{
				}
			};
			context.getSingle(Commands.class).doCommand(
				sCommand, this, null, null, secret, false);
		}
	}

	@Override
	public MessageDisplay getDefaultMessageDisplay() throws IllegalStateException
	{
		return connections.getDefaultMessageDisplay().getMessageDisplay(this);
	}

	@Override
	public String getOurNick()
	{
		return ourNick;
	}

	@Override
	public String getOurUser()
	{
		return ourUser;
	}

	@Override
	public String getOurHost()
	{
		return ourHost;
	}

	@Override
	public int getApproxPrefixLength()
	{
		return
			3+ // Standard characers : ! @
			(ourNick==null ? 20 : ourNick.length()) + // Nick
			(ourUser==null ? 20 : ourUser.length()) + // User
			(ourHost==null ? 30 : ourHost.length()) + // Host
			20; // Allowance in case server is munging prefixes and making them longer
			// Note: If servers actually reported in 005 when they were going to do the
			// above then we could take note and only add the allowance if they were.
			// But they don't.
	}

	@Override
	public String getReportedHost()
	{
		return reportedHost;
	}

	@Override
	public String getReportedOrConnectedHost()
	{
		return reportedHost!=null ? reportedHost : host;
	}

	@Override
	public String getConnectedHost()
	{
		return host;
	}

	@Override
	public int getConnectedPort()
	{
		return port;
	}

	@Override
	public String getConnectedIpAddress()
	{
		if(address == null)
		{
			return null;
		}
		return address;
	}

	@Override
	public String getVersion()
	{
		return version;
	}

	@Override
	public String getDefaultNick()
	{
		return getPreferences().getAnonHierarchical(PREF_DEFAULTNICK);
	}

	@Override
	public String getNickPassword(String nick)
	{
		PreferencesGroup pg=getPreferences();

		while(pg!=null)
		{
			PreferencesGroup[] apgNicks=pg.getChild(PREFGROUP_NICKS).getAnon();

			// Get from this group if we've got it
			for(int iNick=0;iNick<apgNicks.length;iNick++)
			{
				if(apgNicks[iNick].get(PREF_NICK).equals(nick))
				{
					String sPassword=apgNicks[iNick].get(PREF_PASSWORD,null);
					return sPassword;
				}
			}

			// OK try parent
			pg=pg.getAnonParent();
		}
		return "";
	}

	@Override
	public boolean shouldIdentify(String identifyEvent)
	{
		return getPreferences().getAnonHierarchical(PREF_AUTOIDENTIFY,"y").
			equals("y");
	}

	@Override
	public String getIdentifyCommand()
	{
		return getPreferences().getAnonHierarchical(PREF_IDENTIFYCOMMAND,PREFDEFAULT_IDENTIFYCOMMAND);
	}

	@Override
	public String getIdentifyPattern()
	{
		return getPreferences().getAnonHierarchical(PREF_IDENTIFYPATTERN,PREFDEFAULT_IDENTIFYPATTERN);
	}

	@Override
	public String getQuitMessage()
	{
		return getPreferences().getAnonHierarchical(PREF_QUITMESSAGE,PREFDEFAULT_QUITMESSAGE);
	}

	@Override
	public String getServerPassword()
	{
		String password=getPreferences().getAnonHierarchical(PREF_SERVERPASSWORD,"");
		if(password.equals("")) return null;
		return password;
	}

	@Override
	public String getCurrentShortName()
	{
		return connections.getShortName(this);
	}

	/** Map from mask to Long (time when silence was set up) */
	private Map<String, Long> silenced=new HashMap<String, Long>();

	/**
	 * Prevent 'thrashing' SILENCE - this doesn't allow it to remove ones that
	 * were put in less than 2 minutes ago
	 */
	private final static long SILENCETHRASHPREVENTION=120000L;

	@Override
	public synchronized boolean silence(String mask)
	{
		// Check how many silences the server supports
		String number=getISupport("SILENCE");
		if(number==null)
			return false;
		int max;
		try
		{
			max=Integer.parseInt(number);
		}
		catch(NumberFormatException nfe)
		{
			return false;
		}
		if(max<1) return false;

		// OK, it supports some... are we at that limit?
		if(silenced.size()==max)
		{
			long oldest = 0;
			String oldestMask = null;

			for(Map.Entry<String, Long> me : silenced.entrySet())
			{
				long setupTime = me.getValue().longValue();
				if(oldestMask==null || setupTime<oldest)
				{
					oldest = setupTime;
					oldestMask = me.getKey();
				}
			}

			// No old silences
			if(System.currentTimeMillis() - oldest < SILENCETHRASHPREVENTION)
				return false;

			// OK, get rid of it
			unsilence(oldestMask);
		}

		sendLine(IRCMsg.constructBytes("SILENCE +"+mask));
		silenced.put(mask,new Long(System.currentTimeMillis()));
		return true;
	}

	@Override
	public synchronized boolean unsilence(String mask)
	{
		if(silenced.remove(mask)!=null)
		{
			sendLine(IRCMsg.constructBytes("SILENCE -"+mask));
			return true;
		}
		else
			return false;
	}

	@Override
	public InetAddress getLocalAddress()
	{
		return s.getLocalAddress();
	}

	@Override
	public int getMaxModeParams()
	{
		try
		{
			String modesIsupport=getISupport("MODES");
			if(modesIsupport!=null)	return Integer.parseInt(modesIsupport);
		}
		catch(NumberFormatException nfe)
		{
		}
		return 3;
	}

	@Override
	public boolean wasQuitRequested()
	{
		return quitRequested;
	}

	@Override
	public boolean isAway()
	{
		return away;
	}

	/**
	 * Used to prevent the connection-finished message being sent while we are
	 * waiting for user input.
	 * @return True if connection-finished should not be sent yet
	 */
	boolean deferConnectionFinished()
	{
		return deferConnectionFinished;
	}

}
