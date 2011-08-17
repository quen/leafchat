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

import leafchat.core.api.GeneralException;

/** A tool that's just a button. */
public interface SimpleTool extends Tool
{
	/** @return Text label for tool button */
	public String getLabel();

	/**
	 * Obtains theme type. Used for tool information. Also used for
	 * default filename - three images should be provided in the same folder
	 * as the implementing class, called themeType.normal.png, themeType.hover.png,
	 * and themeType.pressed.png.
	 * @return Theme type string
	 */
	public String getThemeType();

	/**
	 * Called when tool is clicked.
	 * @throws GeneralException Any error
	 */
	public void clicked() throws GeneralException;
}
