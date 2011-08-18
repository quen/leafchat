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

import util.*;
import util.xml.XML;

import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/**
 * Receives information about DCC file transfer progress and updates a
 * progress bar window.
 */
public class TransferProgress implements Runnable
{
	private TransfersWindow tw;

	private Page page;
	private Progress p;
	private Button b;
	private Label transferRate,timeRemaining;

	private long soFar=0,size,startTime;

	long[] previousBytes=new long[5];
	long[] previousTime=new long[previousBytes.length];

	final static int SIZE_UNKNOWN=-1;
	private final static int UPDATEDELAY=1000;

	private int eventID;

	private Downloader d;
	private Uploader u;

	synchronized void setTransferred(long bytes)
	{
		if(soFar==0) startTime=System.currentTimeMillis();
		soFar=bytes;
	}

	void setDownloader(Downloader d)
	{
		this.d=d;
		b.setEnabled(true);
	}
	void setUploader(Uploader u)
	{
		this.u=u;
		b.setEnabled(true);
	}

	synchronized void setFinished()
	{
		stopTimer();

		if(soFar<size)
		{
			error("Transfer stopped before complete");
			return;
		}

		// Clear indeterminate state if set, and make sure bar is full
		p.setRange(1);
		p.setProgress(1);

		// Display final size
		long now=System.currentTimeMillis();
		transferRate.setText(StringUtils.displayBytes(soFar));
		timeRemaining.setText("Finished in "+StringUtils.displayMilliseconds(
			now-startTime));

		//	 TODO Maybe add support for changing to Open button here. For now,
		// just make the Cancel button go away
		b.setVisible(false);
		tw.markFinished(this);
	}

	/** Updates the display */
	@Override
	public synchronized void run()
	{
	  eventID=0;

		long now=System.currentTimeMillis();

		// Update progress bar
		if(!p.isIndeterminate())
			p.setProgress((int)(soFar/1024));

		String speed="";
		if(previousBytes[0]>0) // Don't display until we have an estimate
		{
			long bytesPerSecond=((soFar-previousBytes[0])*1000L)/(now-previousTime[0]);
			speed=" at "+StringUtils.displayBytes(bytesPerSecond)+"/s";

			if(size==SIZE_UNKNOWN)
				timeRemaining.setText("Total size unknown");
			else
			{
				if(bytesPerSecond==0)
					timeRemaining.setText("Stalled, waiting...");
				else
				{
					long remainingSeconds=((size-soFar) / bytesPerSecond)+1; // Pessimistic :) Actually to avoid rounding down to 0
					timeRemaining.setText(
						StringUtils.displayMilliseconds(remainingSeconds*1000L)+" remaining");
				}
			}
		}
		System.arraycopy(previousBytes,1,previousBytes,0,previousBytes.length-1);
		System.arraycopy(previousTime,1,previousTime,0,previousTime.length-1);
		previousBytes[previousBytes.length-1]=soFar;
		previousTime[previousTime.length-1]=now;
		transferRate.setText(StringUtils.displayBytes(soFar)+speed);
		startTimer();
	}

	/**
	 * Action: User clicks cancel button.
	 */
	public void actionButton()
	{
		if(d!=null)
			d.cancel();
		if(u!=null)
			u.cancel();
		cancelled=true;
		error("Cancelled");
	}

	private boolean cancelled=false;
	private String error=null;
	private String file;

	boolean isCancelled() { return cancelled; }
	boolean isError() { return error!=null; }
	String getError() { return error; }
	String getFilename() { return file; }

	/**
	 * Sets error text and marks as finished.
	 * @param error Error string (pure string, no XML)
	 */
	void error(String error)
	{
		b.setVisible(false);
		timeRemaining.setText("<error>"+XML.esc(error)+"</error>");
		p.setProgress(0);

		stopTimer();
		this.error=error;
		tw.markFinished(this);
	}

	/**
	 * Sets error text, marks as finished, logs in system log.
	 * @param error Error string (pure string, no XML)
	 * @param t Error that caused failure
	 */
	void error(String error, Throwable t)
	{
	}

	/**
	 * Sets status text. (Only applicable when transfer hasn't started yet.)
	 * @param status New status string (pure string, no XML)
	 */
	void status(String status)
	{
		timeRemaining.setText(XML.esc(status));
	}

	void stopTimer()
	{
		if(eventID!=0) TimeUtils.cancelTimedEvent(eventID);
	}

	Page getPage()
	{
		return page;
	}

	TransferProgress(TransfersWindow tw,PluginContext pc,boolean upload,String nick,String file,long size)
	{
		startTime=System.currentTimeMillis();
		this.tw=tw;
		this.size=size;
		this.file=file;

		UI u=pc.getSingle(UI.class);

		page=u.newPage(this);
		VerticalPanel vp=u.newVerticalPanel();
		page.setContents(vp);

		BorderPanel bpOuter=u.newBorderPanel();
		vp.add(bpOuter);

		BorderPanel bp=u.newBorderPanel();
		bp.setSpacing(8);
		bpOuter.set(BorderPanel.WEST,bp);
		b=u.newButton();
		b.setLabel("Cancel");
		b.setEnabled(false);
		bp.set(BorderPanel.EAST,b);
		b.setOnAction("actionButton");
		b.setBaseGroup("a");
		HorizontalPanel hp=u.newHorizontalPanel();
		bp.set(BorderPanel.CENTRAL,hp);
		Spacer s=u.newSpacer();
		s.setWidth(2);
		hp.add(s);
		Pic direction=u.newPic();
		direction.setProperty(upload ? "dcc/uploadIcon" : "dcc/downloadIcon");
		hp.add(direction);
		s=u.newSpacer();
		s.setWidth(4);
		hp.add(s);
		Label l=u.newLabel();
		l.setOwner(page);
		l.setText("<nick>"+XML.esc(nick)+"</nick>");
		l.setBaseGroup("a");
		hp.add(l);
		s=u.newSpacer();
		s.setWidth(8);
		hp.add(s);
		l=u.newLabel();
		l.setOwner(page);
		l.setText(XML.esc(file));
		l.setBaseGroup("a");
		hp.add(l);

		s=u.newSpacer();
		s.setHeight(3);
		vp.add(s);

		p=u.newProgress();

		if(size==SIZE_UNKNOWN)
			p.setIndeterminate();
		else
			p.setRange((int)(size/1024));

		vp.add(p);

		s=u.newSpacer();
		s.setHeight(1);
		vp.add(s);

		bp=u.newBorderPanel();
		vp.add(bp);
		hp=u.newHorizontalPanel();
		bp.set(BorderPanel.WEST,hp);
		s=u.newSpacer();
		s.setWidth(2);
		hp.add(s);
		transferRate=u.newLabel();
		transferRate.setSmall(true);
		transferRate.setText("");
		hp.add(transferRate);
		hp=u.newHorizontalPanel();
		bp.set(BorderPanel.EAST,hp);
		timeRemaining=u.newLabel();
		timeRemaining.setSmall(true);
		timeRemaining.setText("");
		hp.add(timeRemaining);
		s=u.newSpacer();
		s.setWidth(2);
		hp.add(s);

		tw.add(this);
		startTimer();
	}

	/** Starts the timer for next update */
	private void startTimer()
	{
		eventID=TimeUtils.addTimedEvent(this,UPDATEDELAY,false);
	}

}
