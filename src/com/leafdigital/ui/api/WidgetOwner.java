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
 * Interface (implemented by windows and dialogs) for the 'owner' of widgets,
 * to be identified by string IDs.
 */
public interface WidgetOwner
{
	/**
	 * Returns a widget within this window. This method only works if the
	 * widget was created as part of the XML setContents method. Other widgets
	 * do not have IDs unless manually assigned.
	 * @param id Desired ID
	 * @return Widget
	 * @throws BugException If the widget cannot be found
	 */
	public Widget getWidget(String id);

	/**
	 * Called by the UI singleton to record widget IDs. You can also call this
	 * manually to assign new widget IDs.
	 * <p>
	 * All IDs are cleared when either setContents method is used.
	 * @param id ID for widget
	 * @param w Widget
	 * @throws BugException If the widget ID is already in use
	 */
	public void setWidgetID(String id, Widget w);

	/**
	 * @param group Name of group.
	 * @return Selected widget within that group, or null if none
	 */
	public RadioButton getGroupSelected(String group);

	/**
	 * @return Callback handler for the given window
	 */
	public CallbackHandler getCallbackHandler();
}