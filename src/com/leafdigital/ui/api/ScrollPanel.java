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
 * A panel with a vertical scrollbar which contains one widget.
 */
public interface ScrollPanel extends Panel
{
	/**
	 * Sets the inner widget.
	 * @param w New widget or null for none
	 */
	public void set(Widget w);

	/**
	 * Sets the preferred size of the panel.
	 * @param width Width
	 * @param height Height
	 */
	public void setPreferredSize(int width,int height);

	/**
	 * Sets how many pixels the panel will scroll for a single click of an arrow.
	 * @param amount Amount in pixels
	 */
	public void setScrollAmount(int amount);

	/**
	 * @param outline If true (default), uses 3D outline as per platform defaults.
	 *   If false, has no drawn outline.
	 */
	public void setOutline(boolean outline);
}
