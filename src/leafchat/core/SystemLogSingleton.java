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
package leafchat.core;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import util.PlatformUtils;
import leafchat.core.api.SystemLog;

/** Implements the system log */
public class SystemLogSingleton implements SystemLog
{
	/**
	 * Name of system log file
	 */
	public final static String SYSTEMLOG = "systemlog.txt";

	/**
	 * If true, uses standard console instead of system log file
	 */
	public static boolean useConsole=false;

	private PrintWriter pw;
	private SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/**
	 * Constructs.
	 * @throws IOException Any error opening logfile
	 */
	public SystemLogSingleton() throws IOException
	{
		if(useConsole)
		{
			pw=new PrintWriter(System.out);
		}
		else
		{
			pw=new PrintWriter(new OutputStreamWriter(
				new FileOutputStream(new File(PlatformUtils.getUserFolder(),SYSTEMLOG)),"UTF-8"));
		}
	}

	@Override
	public void log(Object o, String sText)
	{
		log(o,sText,null);
	}

	@Override
	public synchronized void log(Object o, String sText, Throwable t)
	{
		pw.print(sdf.format(new Date())+" ");
		if(o!=null)
			pw.print("["+o.toString()+"] ");

		if(sText!=null)
		  pw.print(sText);

		if(sText!=null || o!=null) pw.println();

		if(t!=null) t.printStackTrace(pw);

		pw.flush();
	}

	@Override
	public String toString()
	{
		return "SystemLog";
	}

	/**
	 * Closes singleton.
	 */
	public synchronized void close()
	{
		log(this,"System exited cleanly");
		pw.close();
	}
}
