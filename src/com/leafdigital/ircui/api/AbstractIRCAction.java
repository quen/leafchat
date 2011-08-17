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
package com.leafdigital.ircui.api;

/** Convenience class to make it easier to create IRCActions */
public abstract class AbstractIRCAction implements IRCAction
{
	/**
	 * @param name Name of action for display
	 * @param category An IRCAction.CATEGORY_XX constant
	 * @param order Order of action
	 */
	public AbstractIRCAction(String name,int category,int order)
	{
		this.name=name;
		this.category=category;
		this.order=order;
	}

	private String name;
	private int category;
	private int order;

	@Override
	public int getCategory()
	{
		return category;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public int getOrder()
	{
		return order;
	}
}