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

import com.leafdigital.ui.api.Theme;

/** Interface for things that need to know about themes.
 * @see UISingleton#informThemeListener(ThemeListener)
 */
public interface ThemeListener
{
	/**
	 * Called when the theme changes.
	 * @param t New theme
	 */
	public void updateTheme(Theme t);
}
