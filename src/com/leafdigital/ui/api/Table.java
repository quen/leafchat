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
 * Interface for tables.
 * <p>
 * Tables have a number of columns, specified with 'column' elements within
 * the Table tag. Each column has a type (string or boolean), an editable
 * flag, and an optional width (0 = as small as possible).
 * <p>
 * Here's an example table in XML format:
 * <pre>
 * &lt;Table id="nicknames" OnChange="nicknamesChange" OnEditing="nicknamesEditing" OnSelect="nicknamesSelect" Width="200" Rows="4">
 *   &lt;column name="Nickname" type="string" editable="y" width="100"/>
 *   &lt;column name="Password" type="string" editable="y"/>
 *   &lt;column name="Default" type="boolean" editable="y" width="0"/>
 * &lt;/Table>
 * </pre>
 */
public interface Table extends Widget, SupportsMacIndent
{
	/**
	 * Adds new item to end of list.
	 * @return Index of newly-added item
	 */
	public int addItem();

	/**
	 * Sets preferred width of table.
	 * @param width Preferred width
	 */
	public void setWidth(int width);

	/**
	 * Removes item from list.
	 * @param index Index of item to remove
	 * @throws BugException If index is out of bounds
	 */
	public void removeItem(int index);

	/** Remove all items from list */
	public void clear();

	/** @return Number of items */
	public int getNumItems();

	/**
	 * @return Selected index or Table.NONE if none are selected.
	 * @throws BugException If MultiSelect="y"
	 */
	public int getSelectedIndex();

	/**
	 * Sets a single selected row.
	 * @param selected Row to select
	 */
	public void setSelectedIndex(int selected);

	/** @return Array of selected indices */
	public int[] getSelectedIndices();

	/**
	 * Sets multiple selected indices.
	 * @param selected Rows to select
	 */
	public void setSelectedIndices(int[] selected);

	/** Value used to indicate no selection */
	public final static int NONE=-1;

	/**
	 * Set number of displayed rows.
	 * @param rows Number of rows
	 */
	public void setRows(int rows);

	/**
	 * Sets value at a given column.
	 * @param index Index of item
	 * @param column Column index
	 * @param value Value at column
	 * @throws BugException If index or column are out of bounds, or column
	 *   isn't a string
	 */
	public void setString(int index, int column, String value);

	/**
	 * Obtains value at given column.
	 * @param index Index of item
	 * @param column Column index
	 * @return Value at column
	 * @throws BugException If index or column are out of bounds, or column
	 *   isn't a string
	 */
	public String getString(int index, int column);

	/**
	 * Sets value at a given column.
	 * @param index Index of item
	 * @param column Column index
	 * @param value Value at column
	 * @throws BugException If index or column are out of bounds, or column
	 *   isn't correct type
	 */
	public void setBoolean(int index, int column, boolean value);

	/**
	 * Obtains value at given column.
	 * @param index Index of item
	 * @param column Column index
	 * @return Value at column
	 * @throws BugException If index or column are out of bounds, or column
	 *   isn't correct type
	 */
	public boolean getBoolean(int index, int column);

	/**
	 * Sets the editable state of given cell. Only applies to columns that are
	 * in general editable.
	 * @param index Index of item
	 * @param column Column index
	 * @param editable True to allow edits, false to prohibit
	 * @throws BugException If index or column are out of bounds, or column
	 *   isn't editable
	 */
	public void setEditable(int index, int column, boolean editable);

	/**
	 * @param index Index of item
	 * @param column Column index
	 * @return True if item can be edited (in editable column and editable)
	 * @throws BugException If index or column are out of bounds
	 */
	public boolean isEditable(int index, int column);

	/**
	 * Sets the dim state of given cell.
	 * @param index Index of item
	 * @param column Column index
	 * @param dim True to make dim, false for normal
	 * @throws BugException If index or column are out of bounds
	 */
	public void setDim(int index, int column, boolean dim);

	/**
	 * @param index Index of item
	 * @param column Column index
	 * @return True if item is dim
	 * @throws BugException If index or column are out of bounds
	 */
	public boolean isDim(int index, int column);

	/**
	 * Sets the overwrite state of given cell (this just means that its text content
	 * will be highlighted when you select it). Only applies to string columns.
	 * @param index Index of item
	 * @param column Column index
	 * @param overwrite True to turn overwrite on, false to turn it off
	 * @throws BugException If index or column are out of bounds, or column
	 *   isn't a string
	 */
	public void setOverwrite(int index, int column, boolean overwrite);

	/**
	 * @param index Index of item
	 * @param column Column index
	 * @return True if item has overwrite set
	 * @throws BugException If index or column are out of bounds or isn't a string
	 */
	public boolean isOverwrite(int index, int column);

	/**
	 * Sets the callback method used when user edits something (or, in the case
	 * of textfields, finishes editing it). Signature of callback is:
	 * public void onChange(int index,int column,Object before)
	 * @param callback Name of method
	 * @throws BugException If method doesn't exist etc.
	 */
	@UICallback
	public void setOnChange(String callback);

	/**
	 * Sets the callback method used when selection changes.
	 * @param callback Name of method
	 * @throws BugException If method doesn't exist etc.
	 */
	@UICallback
	public void setOnSelect(String callback);

	/**
	 * Sets the callback method used while the user is editing a textfield after
	 * each change. Signature of callback is:
	 * public void onEditing(int index,int column,String value,EditingControl ec)
	 * @param callback Name of method
	 * @throws BugException If method doesn't exist etc.
	 */
	@UICallback
	public void setOnEditing(String callback);

	/** Class passed as part of the onEditing callback. */
	public class EditingControl
	{
		private boolean error = false, dim = false;
		/**
		 * If this method is called, the editbox will be shown red.
		 * onChange will not be called if editing ends at this point, and the
		 * new value will not be entered into the table.
		 */
		public void markError()
		{
			error = true;
		}
		/**
		 * @return True if the error flag is set
		 */
		public boolean isError()
		{
			return error;
		}
		/**
		 * If this method is called, the editbox will be shown dim.
		 */
		public void markDim()
		{
			dim=true;
		}
		/**
		 * @return True if the dim flag is set
		 */
		public boolean isDim()
		{
			return dim;
		}
	}

	/**
	 * @param multiSelect	If true, allows multiple rows to be selected
	 */
	public void setMultiSelect(boolean multiSelect);

	/**
	 * Sets callback method used when somebody double-clicks or presses Return.
	 * @param callback Name of callback method
	 */
	@UICallback
	public void setOnAction(String callback);
}
