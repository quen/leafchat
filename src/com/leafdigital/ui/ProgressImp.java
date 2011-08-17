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
package com.leafdigital.ui;

import javax.swing.*;

import com.leafdigital.ui.api.*;

import leafchat.core.api.BugException;

/** A wrappable label */
public class ProgressImp extends JComponent
{
	private JProgressBar pb;

	/**
	 * Constructs.
	 */
	public ProgressImp()
	{
		setLayout(null);
		pb=new JProgressBar(0,100);
		add(pb);

		relayout();
	}

	@Override
	public void setBounds(int x,int y,int width,int height)
	{
		super.setBounds(x,y,width,height);
		relayout();
	}

	private void relayout()
	{
		int preferredHeight=pb.getPreferredSize().height;
		pb.setBounds(0,(getHeight()-preferredHeight)/2,getWidth(),preferredHeight);
	}

	Progress getInterface() { return pi; }

	ProgressInterface pi=new ProgressInterface();

	class ProgressInterface extends BasicWidget implements Progress, InternalWidget
	{
		@Override
		public int getContentType() { return CONTENT_NONE; }

		@Override
		public JComponent getJComponent()
		{
			return ProgressImp.this;
		}

		@Override
		public int getPreferredWidth()
		{
			return pb.getPreferredSize().width;
		}

		@Override
		public int getPreferredHeight(int iWidth)
		{
			return pb.getPreferredSize().height;
		}

		@Override
		public void addXMLChild(String sSlotName, Widget wChild)
		{
			throw new BugException("Progress bars cannot contain children");
		}

		@Override
		public void setProgress(final int progress)
		{
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					pb.setValue(progress);
				}
			});
		}

		@Override
		public void setRange(final int max)
		{
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					pb.setIndeterminate(false);
					pb.setMaximum(max);
					pb.setValue(0);
				}
			});
		}

		@Override
		public int getProgress()
		{
			return pb.getValue();
		}

		@Override
		public int getRange()
		{
			return pb.getMaximum();
		}

		@Override
		public boolean isIndeterminate()
		{
			return pb.isIndeterminate();
		}

		@Override
		public void setIndeterminate()
		{
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					pb.setIndeterminate(true);
				}
			});
		}
	}
}
