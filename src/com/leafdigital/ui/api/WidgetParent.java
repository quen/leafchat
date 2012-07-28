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

Copyright 2012 Samuel Marshall.
*/
package com.leafdigital.ui.api;

/**
 * Interface for any widget that contains other widgets. Note that this is
 * the direct parent, unlike WidgetOwner which is more of a top-level owner.
 */
public interface WidgetParent
{
	/**
	 * Gets all widgets contained within this widget.
	 * @return Array of contained widgets (zero-length if none)
	 */
	public Widget[] getWidgets();
}
