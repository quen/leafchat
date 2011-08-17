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

import java.awt.image.BufferedImage;
import java.io.File;

import textlayout.stylesheet.Stylesheet;

/**
 * API for information held by the theme.
 * <p>
 * Since themes are considered to be user-created, these methods are more lenient
 * than usual; they do not throw exceptions.
 */
public interface Theme
{
	/** Theme property type: meta */
	public final static String META="meta";
	/** Theme property: theme name */
	public final static String META_NAME="name";
	/** Theme property: theme authors */
	public final static String META_AUTHORS="authors";
	/** Theme property: theme description */
	public final static String META_DESCRIPTION="description";
	/** Theme property: theme preview */
	public final static String META_PREVIEW="preview";

	/**
	 * @return Physical location of theme
	 */
	public File getLocation();

	/**
	 * Obtains an integer property.
	 * @param themeType Type of window/item under consideration, or Theme.META
	 * @param property Name of property
	 * @param def Default value of property if not specified, or if specified but not an integer
	 * @return Value of property
	 */
	public int getIntProperty(String themeType,String property,int def);

	/**
	 * Obtains a boolean property.
	 * @param themeType Type of window/item under consideration, or Theme.META
	 * @param property Name of property
	 * @param def Default value of property if not specified
	 * @return Value of property
	 */
	public boolean getBooleanProperty(String themeType,String property,boolean def);

	/**
	 * Obtains a string property.
	 * @param themeType Type of window/item under consideration, or Theme.META
	 * @param property Name of property
	 * @param def Default value of property if not specified
	 * @return Value of property
	 */
	public String getStringProperty(String themeType,String property,String def);

	/**
	 * Obtains an image property.
	 * @param themeType Type of window/item under consideration, or Theme.META,
	 *   or null to request a file directly from this theme (will not search
	 *   parent themes or use the default reference).
	 * @param property Name of property, or file if themeType is null
	 * @param transparency If true, transparency from image will be used; otherwise
	 *   you'll get a solid image
	 * @param defaultReference A class that can be used as reference to load a default
	 *   image, or null if no default
	 * @param defaultFilename Filename for default image, or null if no default
	 * @return Image or null if not specified
	 */
	public BufferedImage getImageProperty(String themeType, String property,
		boolean transparency, Class<?> defaultReference, String defaultFilename);

	/**
	 * Obtains the theme's stylesheets (if provided).
	 * @return Stylesheets in ascending order of importance i.e. first is most general
	 *   and should be added first.
	 */
	public Stylesheet[] getStylesheets();
}
