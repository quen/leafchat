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
 * A Panel with any number of slots (all named 'slot' in XML). Components are
 * laid out horizontally, going across the panel. All components have the full
 * height of the panel.
 */
public interface HorizontalPanel extends Panel
{
	/**
	 * Adds a widget to the next available slot.
	 * @param w New widget
	 */
	public void add(Widget w);

	/**
	 * Sets the gap between components. The default is for no margin.
	 * @param spacing Spacing in pixels (if in doubt, use 4)
	 */
	public void setSpacing(int spacing);
}
