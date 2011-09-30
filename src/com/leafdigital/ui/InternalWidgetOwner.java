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

import java.util.Set;

import javax.swing.ButtonGroup;

import com.leafdigital.ui.api.WidgetOwner;

/**
 * Interface for items such as windows which own a collection of widgets.
 */
public interface InternalWidgetOwner extends WidgetOwner
{
	/**
	 * Obtains a radio button group.
	 * @param group Name of group. New names will result in new groups being
	 *   created.
	 * @return Group
	 */
	public ButtonGroup getButtonGroup(String group);

	/**
	 * Obtains a group of arbitrary widgets.
	 * @param group Name of group. New names will result in new groups being
	 *   created.
	 * @return Group as set (can add things to it, should synch on it)
	 */
	public Set<BaseGroup> getArbitraryGroup(String group);

	/**
	 * Marks item as created, meaning that callbacks can be called now.
	 * @see WidgetOwner#isCreated()
	 */
	void markCreated();
}
