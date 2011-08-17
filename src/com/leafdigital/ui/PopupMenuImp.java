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

import java.awt.event.*;

import javax.swing.*;

import com.leafdigital.ui.api.PopupMenu;

/**
 * Popup menu implementation.
 */
public class PopupMenuImp extends JPopupMenu
{
  PopupMenu getInterface()
  {
  	return externalInterface;
  }

  PopupMenu externalInterface=new PopupMenu()
  {
		@Override
		public void addItem(String name,final Runnable action)
		{
			JMenuItem mi=new JMenuItem(name);
			mi.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					action.run();
				}
			});
			PopupMenuImp.this.add(mi);
		}

		@Override
		public void addSeparator()
		{
			PopupMenuImp.this.addSeparator();
		}
  };
}
