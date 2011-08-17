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

import leafchat.core.api.BugException;

/**
 * Interface for images from theme. Images may be clickable or not. To make
 * an image clickable, set the OnAction and optionally a hover image and tooltip.
 */
public interface Pic extends Widget
{
	/**
	 * Set the image property name in the format category/name (PNG and JPG
	 * supported). The image is loaded from the current theme.
	 * @param name Property name or null for no image
	 */
	public void setProperty(String name);

	/**
	 * Set an image directly from a theme without going through the theme
	 * property interface. (For special-case use only.)
	 * @param t Theme
	 * @param filename Name for iamge
	 */
	public void setThemeFile(Theme t,String filename);


	/**
	 * Sets the action method called when button is clicked.
	 * @param callback Name of method
	 * @throws BugException If method doesn't exist etc.
	 */
	@UICallback
	public void setOnAction(String callback);

	/**
	 * Sets a tooltip that appears when you hover over the button.
	 * @param tip Text of tip or null for none
	 */
	public void setTooltip(String tip);

	/**
	 * Alternate property to use when the user hovers over the button.
	 * @param name Property name or null for no image
	 */
	public void setHover(String name);

	// TODO Way to turn on scaling
}
