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

import com.leafdigital.net.api.Network;

import leafchat.core.api.*;

/**
 * Thread that handles DCC downloads.
 */
public class Downloader extends Thread
{
	private TransferProgress tp;
	private InetAddress address;
	private int port;
	private long pos,startPos;
	private long size;
	private File target,targetPartial;

	private PluginContext context;

	private FileOutputStream output;

	/**
	 * @param context Plugin context
	 * @param tp Receives progress information
	 * @param address Address for connection
	 * @param port Port for connection
	 * @param target Final target file location
	 * @param targetPartial Location to store partial download file
	 * @param resumePos Position to resume from
	 * @param size Download size
	 */
	public Downloader(PluginContext context,TransferProgress tp,InetAddress address,int port,File target,File targetPartial,long resumePos,long size)
	{
		// Set up values we'll need in thread
		this.context=context;
		this.tp=tp;
		this.address=address;
		this.port=port;
		this.pos=resumePos;
		this.startPos=resumePos;
		this.target=target;
		this.targetPartial=targetPartial;
		this.size=size;

		// Get file ready
		try
		{
			if(resumePos==0)
				output=new FileOutputStream(targetPartial);
			else
			{
				if(!targetPartial.exists())
					throw new BugException("Unexpected resume position - partial file doesn't exist");
				if(resumePos>targetPartial.length())
					throw new BugException("Unexpected resume position - doesn't match file length");
				if(resumePos<targetPartial.length())
				{
					RandomAccessFile raf=new RandomAccessFile(targetPartial,"wb");
					raf.setLength(resumePos);
					raf.close();
				}
				output=new FileOutputStream(targetPartial,true);
			}
		}
		catch(IOException ioe)
		{
			tp.error("Couldn't open local file");
			return;
		}

		// Check whether address looks plausible
		if(address.getHostAddress().startsWith("127."))
		{
			tp.error("Request gave localhost address");
			return;
		}
		if(port<1024 || port>65535)
		{
			tp.error("Request gave invalid port");
			return;
		}

		tp.setDownloader(this);

		// Start thread
		start();
	}

	void cancel()
	{
		cancelled=true;
	}

	private final static int BUFFERSIZE=65536;

	private boolean cancelled;

	@Override
	public void run()
	{
		// Connect
		tp.status("Connecting...");
		InputStream is;
		OutputStream os;
		Socket s;
		Network n=context.getSingle(Network.class);
		try
		{
			context.log("Beginning DCC transfer "+target.getName()+" from "+address+":"+port);
			s=n.connect(address.getHostAddress(),port,30000);
			s.setSoTimeout(1000);
			is=s.getInputStream();
			os=s.getOutputStream();
		}
		catch(IOException e)
		{
			context.log("DCC failed: connection failed",e);
			tp.error("Connection failed");
			return;
		}

		// Set initial position
		tp.status("Receiving...");
		tp.setTransferred(pos);

		// Read data
		try
		{
			byte[] buffer=new byte[BUFFERSIZE];
			long sentConfirmAt=0;
			long maxBlock=0;
			int confirmCount=0;
			boolean lastGotNothing=false;
			while(!cancelled)
			{
				int read;
				try
				{
					// Read data
					read=is.read(buffer);
					if(read==-1)
					{
						context.logDebug("Read EOF");
						break;
					}
					else
					{
						context.logDebug("Read "+read+" bytes");
					}
					lastGotNothing=false;
				}
				catch(SocketTimeoutException e)
				{
					if(cancelled) return;

					// We know what biggest block size is so don't send confirms more often,
					// probably just network delay
					if(pos >= sentConfirmAt+maxBlock || (pos>sentConfirmAt && lastGotNothing))
					{
						try
						{
							sendPos(os);
						}
						catch(IOException e1)
						{
							context.log("DCC failed: error confirming data",e);
							tp.error("Error confirming data",e1);
							return;
						}
						sentConfirmAt=pos;
						confirmCount++;
					}
					lastGotNothing=true;
					continue;
				}
				catch(IOException e)
				{
					context.log("DCC failed: error reading data",e);
					tp.error("Error reading data");
					return;
				}

				try
				{
					// Save in file
					output.write(buffer,0,read);
				}
				catch(IOException e)
				{
					context.log("DCC failed: error saving data",e);
					tp.error("Error saving data");
					return;
				}

				// Update position
				pos+=read;
				tp.setTransferred(pos);

				// Track largest observed block size (between confirmations)
				maxBlock=Math.max(read,maxBlock);

				// Send confirmation immediately if they seem to need it, and
				// every 4096 bytes regardless
				if((confirmCount>5 && maxBlock<4096) || (pos-sentConfirmAt>=4096))
				{
					try
					{
						sendPos(os);
					}
					catch(IOException e1)
					{
						context.log("DCC failed: error confirming data",e1);
						tp.error("Error confirming data",e1);
						return;
					}
					sentConfirmAt=pos;
				}
			}
			if(!cancelled)
			{
				if(pos>sentConfirmAt)
				{
					try
					{
						sendPos(os);
					}
					catch(IOException ioe)
					{
						// Who cares, we're done now
					}
				}
				tp.setFinished();
				if(size==TransferProgress.SIZE_UNKNOWN || size==pos)
				{
					targetPartial.renameTo(target);
				}
			}
		}
		finally
		{
			try
			{
				context.log("DCC complete");
				s.close();
				output.close();
			}
			catch(IOException e)
			{
			}
		}
	}

	private void sendPos(OutputStream os) throws IOException
	{
	  // mIRC, BitchX may require the ack to be the
		// number of bytes sent this session rather than overall
		long sendPos=pos-startPos;
		int b1=(int)((sendPos>>24)&0xff);
		int b2=(int)((sendPos>>16)&0xff);
		int b3=(int)((sendPos>>8)&0xff);
		int b4=(int)(sendPos&0xff);
		os.write(b1);
		os.write(b2);
		os.write(b3);
		os.write(b4);
		context.logDebug("Sending ack: "+sendPos);
		os.flush();
	}

}
