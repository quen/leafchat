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
 * A Panel with nine component slots - all the compass points, plus
 * a central component. The central component stretches to take up all
 * available space. Other components are scaled to fit their desired
 * sizes, similar to a Java BorderLayout.
 * <p>
 * There are three possible ways to handle corners.
 */
public interface BorderPanel extends Panel
{
	/** BorderPanel slot: top */
	public final static int NORTH=0;
	/** BorderPanel slot: top right */
	public final static int NORTHEAST=1;
	/** BorderPanel slot: right */
	public final static int EAST=2;
	/** BorderPanel slot: bottom right */
	public final static int SOUTHEAST=3;
	/** BorderPanel slot: bottom */
	public final static int SOUTH=4;
	/** BorderPanel slot: bottom left */
	public final static int SOUTHWEST=5;
	/** BorderPanel slot: left */
	public final static int WEST=6;
	/** BorderPanel slot: top left */
	public final static int NORTHWEST=7;
	/** BorderPanel slot: middle */
	public final static int CENTRAL=8;

	/**
	 * Sets the component in one slot of the BorderPanel.
	 * @param slot Slot ID (BorderPanel.NORTH, etc.)
	 * @param w New component for slot (may be null to remove existing component)
	 */
	public void set(int slot,Widget w);

	/**
	 * Retrieves the widget in one slot of the BorderPanel
	 * @param slot Slot ID (BorderPanel.NORTH, etc.)
	 * @return Widget in that slot or null if none
	 */
	public Widget get(int slot);

	/**
	 * Sets the margin between grid rows/columns. The default is for no margin.
	 * @param spacing Spacing in pixels (if in doubt, use 4)
	 */
	public void setSpacing(int spacing);

	/**
	 * Control the way corners are handled, if a component isn't placed in
	 * those slots. The default is CORNERS_HORIZONTALFILL.
	 * @param corners A CORNER_xxx constant
	 */
	public void setCornerHandling(int corners);

	/**
	 * Fill corners horizontally - if the NE corner is empty, then the N
	 * component will stretch horizontally to fill it. C stretches to fill W/E.
	 */
	public static int CORNERS_HORIZONTALFILL=0;

	/**
	 * Fill corners vertically - if the NE corner is empty, then the E
	 * component will stretch vertically to fill it. C stretches to fill N/S.
	 */
	public static int CORNERS_VERTICALFILL=1;

	/**
	 * Leave corners blank - if the NE corner is empty, then it will remain
	 * empty; the panel will have a blank space there.
	 */
	public static int CORNERS_LEAVEBLANK=2;
}
