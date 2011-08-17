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
 * A Panel with only a single slot, named 'contents' in XML. The panel has an
 * outline around it and a title in the outline.
 */
public interface GroupPanel extends Panel
{
	/**
	 * Set the text.
	 * @param title Text for title
	 */
	public void setTitle(String title);

		/**
	 * Sets the widget inside the panel
	 * @param w New widget, or null to remove
	 */
	public void set(Widget w);

	/**
	 * Sets the gap between the border outline and the component within. The
	 * default is no border.
	 * @param internalBorder Internal border in pixels (if in doubt, use 4)
	 */
	public void setInternalBorder(int internalBorder);
}
