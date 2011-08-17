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
 * Provides visual decoration as a background to other components.
 */
public interface DecoratedPanel extends Panel
{
	/**
	 * Sets padding that applies around the inner component for decoration.
	 * @param top Top inset (px)
	 * @param right Right inset (px)
	 * @param bottom Bottom inset (px)
	 * @param left Left inset (px)
	 */
	public void setPadding(int top,int right,int bottom,int left);

	/**
	 * Sets a callback used to paint the decoration. The callback must have
	 * the signature public void callback(Graphics2D g,int left,int top,int width,int height).
	 * @param callback Callback method or null for none
	 */
	@UICallback
	public void setOnPaint(String callback);

	/**
	 * Causes the panel to repaint itself.
	 */
	public void repaint();
}
