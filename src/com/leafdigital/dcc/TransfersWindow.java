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

import com.leafdigital.notification.api.Notification;
import com.leafdigital.ui.api.*;

import leafchat.core.api.PluginContext;

/**
 * Window that shows transfer progress.
 */
@UIHandler("transfers")
public class TransfersWindow
{
	private PluginContext pc;
	private Window w;

	/**
	 * Panel: main contents.
	 */
	public VerticalPanel contentsUI;

	TransfersWindow(PluginContext owner)
	{
		this.pc=owner;
		UI u=pc.getSingle(UI.class);

		w = u.createWindow("transfers", this);
		w.show(false);
	}

	private int unfinished=0;

	void add(TransferProgress tp)
	{
		contentsUI.add(tp.getPage());
		unfinished++;
		w.attention();
		w.setClosable(false);
	}

	void markFinished(TransferProgress tp)
	{
		unfinished--;
		if(unfinished==0) w.setClosable(true);
		w.attention();

		// Notify, except when cancelled (they know they cancelled)
		if(!tp.isCancelled())
		{
			pc.getSingle(Notification.class).notify(
				DCCPlugin.NOTIFICATION_TRANSFERCOMPLETE,tp.getFilename(),
				tp.isError()?"Error: "+tp.getError():"Transfer complete");
		}
	}

	void remove(TransferProgress tp)
	{
		contentsUI.remove(tp.getPage());
	}

	/*
	int tick=0;
	private Runnable test=new Runnable()
	{
		private TransferProgress tp1,tp2,tp3;
		public void run()
		{
			try
			{
				switch(tick)
				{
				case 1:
					tp1=new TransferProgress(TransfersWindow.this,pc,false,"quen","My Fun File.pdf",5000000);
					contentsUI.add(tp1.getPage());
					break;
				}
				if(tp1!=null)
				{
					int value=(tick-1)*100000;
					if(value>5000000)
					{
						tp1.setFinished();
						tp1=null;
					}
					else
						tp1.setTransferred(value);
				}

				tick++;
				TimeUtils.addTimedEvent(test,1000L,false);
			}
			catch(Throwable t)
			{
				t.printStackTrace();
			}
		}
	};
	*/

	/**
	 * Action: User closes window.
	 */
	@UIAction
	public void windowClosed()
	{
		w=null;
		((DCCPlugin)pc.getPlugin()).transfersWindowClosed();
	}

}
