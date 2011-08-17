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
package com.leafdigital.ui.api;

/**
 * Represents a Panel, a component which can hold other components in various
 * slots. This interface is not available directly; instead, use its subclasses.
 */
public abstract interface Panel extends Widget
{
	/**
	 * Sets the outside border around the panel.
	 * The default is for no border.
	 * @param border Border in pixels (if in doubt, use 4)
	 */
	public void setBorder(int border);

	/**
	 * Remove a component from the panel.
	 * @param c Component to remove
	 */
	public void remove(Widget c);

	/**
	 * Remove all components from the panel.
	 */
	public void removeAll();
}
