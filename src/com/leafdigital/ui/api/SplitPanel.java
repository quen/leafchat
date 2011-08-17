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
 * A Panel with two component slots - 'main' and 'split'. Four orientations control
 * where the split bit goes. The initial size of the split must be specified. Users
 * can then drag the split bar.
 */
public interface SplitPanel extends Panel
{
	/**
	 * Sets the main component.
	 * @param w New widget for slot
	 */
	public void setMain(Widget w);

	/**
	 * Sets the split component.
	 * @param w New widget for slot
	 */
	public void setSplit(Widget w);

	/**
	 * Sets the size (in pixels) of the split area. The default is 100.
	 * @param size Size in pixels
	 */
	public void setSplitSize(int size);

	/** @return Size of split area in pixels */
	public int getSplitSize();

	/**
	 * Controls where the split part appears. The default is SIDE_EAST.
	 * @param side A SIDE_xxx constant
	 */
	public void setSide(int side);

	/**
	 * Split is on north side.
	 */
	public static int SIDE_NORTH=0;
	/**
	 * Split is on east side.
	 */
	public static int SIDE_EAST=1;
	/**
	 * Split is on south side.
	 */
	public static int SIDE_SOUTH=2;
	/**
	 * Split is on west side.
	 */
	public static int SIDE_WEST=3;
}
