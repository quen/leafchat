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

import java.io.*;
import java.net.*;

/**
 * Test class. Pretends to be an IRC server using the fake server transcript
 * fakeserver.txt.
 */
public class FakeServer implements Runnable
{
	private ServerSocket ss;

	FakeServer()
	{
		(new Thread(this,"Fake server thread")).start();
	}

	@Override
	public void run()
	{
		try
		{
			ss=new ServerSocket(6667);
			while(true)
			{
				Socket s=ss.accept();
				BufferedReader br=new BufferedReader(new InputStreamReader(
					getClass().getResourceAsStream("fakeserver.txt")));
				OutputStream os=s.getOutputStream();
				while(true)
				{
					String sLine=br.readLine();
					if(sLine==null) break;
					if(sLine.startsWith("!!! Received: "))
					{
						sLine=sLine.substring("!!! Received: ".length());
						os.write((sLine+"\r\n").getBytes());
					}
					else if(sLine.startsWith("<< "))
					{
						sLine=sLine.substring("<< ".length());
						os.write((sLine+"\r\n").getBytes());
					}
					else if(sLine.startsWith("!PAUSE"))
					{
						try
						{
							Thread.sleep(5000);
						}
						catch(InterruptedException ie)
						{
						}
					}
					else if(sLine.startsWith("!END"))
					{
						break;
					}
				}
				int i=1;
				while(i<0)//1000)
				{
					try
					{
						Thread.sleep(2000);
					}
					catch(InterruptedException ie)
					{
					}
					os.write((":fickle!whatever@whatever PRIVMSG #quentesting :Automatically generated message "+i+"\r\n").getBytes());
					i++;
				}
			}
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
}
