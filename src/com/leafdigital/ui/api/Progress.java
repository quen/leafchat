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
 * Interface for progress bars
 */
public interface Progress extends Widget
{
	/**
	 * Sets the range of the progress bar. Automatically clears progress to zero.
	 * @param max Maximum value that corresponds to 100%
	 */
	public void setRange(int max);

	/** @return Maximum value that corresponds to 100% */
	public int getRange();

	/**
	 * Sets progress.
	 * @param progress New progress value
	 */
	public void setProgress(int progress);

	/**
	 * @return Progress toward range
	 */
	public int getProgress();

	/**
	 * Puts the progress bar into indeterminate mode. Calling setRange turns
	 * this off.
	 */
	public void setIndeterminate();

	/** @return True if bar is indeterminate */
	public boolean isIndeterminate();
}
