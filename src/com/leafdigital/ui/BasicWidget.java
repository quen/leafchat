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

import org.w3c.dom.Element;

import com.leafdigital.ui.api.WidgetOwner;

import leafchat.core.api.BugException;

/** Shared facilities across widgets */
abstract class BasicWidget implements InternalWidget
{
	private String sID;

	private UISingleton uis;

	private WidgetOwner wo;

	@Override
	public void setID(String sID)
	{
		this.sID=sID;
	}

	@Override
	public String getID()
	{
		return sID;
	}

	@Override
	public void setUI(UISingleton uis)
	{
		this.uis=uis;
	}

	public UISingleton getUI()
	{
		return uis;
	}

	@Override
	public void setOwner(WidgetOwner wo)
	{
		this.wo=wo;
	}

	@Override
	public WidgetOwner getOwner()
	{
		if(wo==null && parent!=null)
			return parent.getOwner();
		return wo;
	}

	private InternalWidget parent;

	@Override
	public InternalWidget getParent()
	{
		return parent;
	}

	@Override
	public void setParent(InternalWidget parent)
	{
		this.parent=parent;
	}

	@Override
	public String[] getReservedChildren()
	{
		return new String[0];
	}

	@Override
	public void setReservedData(Element[] ae)
	{
		throw new BugException("Item does not accept reserved data");
	}

	private boolean visible=true;

	@Override
	public void setVisible(final boolean visible)
	{
		UISingleton.runInSwing(new Runnable()
		{
			@Override
			public void run()
			{
				if(BasicWidget.this.visible==visible) return;

				BasicWidget.this.visible=visible;
				getJComponent().setVisible(visible);
				if(parent!=null) parent.redoLayout();
			}
		});
	}

	@Override
	public boolean isVisible()
	{
		return visible;
	}

	@Override
	public void redoLayout()
	{
		if(parent!=null) parent.redoLayout();
	}

	@Override
	public void informClosed()
	{
		// Default ignores it
	}
}
