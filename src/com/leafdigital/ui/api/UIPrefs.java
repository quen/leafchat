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
 * Preferences values for UI; which need to be accessed by the uiprefs plugin
 * so are public.
 */
public interface UIPrefs
{
	/** UI style preference */
  public final static String PREF_UISTYLE="uistyle";
  /** Classic UI style */
  public final static String PREFVALUE_UISTYLE_CLASSIC="classic";
  /** Separate windows UI style */
  public final static String PREFVALUE_UISTYLE_SEPARATE="separate";
  /** Tabs UI style */
  public final static String PREFVALUE_UISTYLE_TABS="tabs";
  /** Default UI style: classic */
  public final static String PREFDEFAULT_UISTYLE="classic";

  /** Minimise to tray preference */
  public final static String PREF_MINIMISE_TO_TRAY="minimise-to-tray";
  /** Default minimise to tray: no */
	public final static String PREFDEFAULT_MINIMISE_TO_TRAY="f";

	/** Font name preference */
	public final static String PREF_FONTNAME="fontname";
	/** Default font name: Arial */
	public final static String PREFDEFAULT_FONTNAME="Arial";

	/** Font size preference */
	public final static String PREF_FONTSIZE="fontsize";
	/** Default font size: 14 */
	public final static String PREFDEFAULT_FONTSIZE="14";

	/** System font preference */
	public final static String PREF_SYSTEMFONT="systemfont";
	/** Default system font: yes */
	public final static String PREFDEFAULT_SYSTEMFONT="t";

	/** IRC colours preference */
	public final static String PREF_IRCCOLOURS="irccolours";
	/** Default IRC colours: yes */
	public final static String PREFDEFAULT_IRCCOLOURS="t";

	/** Colours group */
	public final static String PREFGROUP_COLOURS="colours";
	/** RGB value for colour */
	public final static String PREF_RGB="rgb";
	/** Keyword for colour */
	public final static String PREF_KEYWORD="keyword";
}
