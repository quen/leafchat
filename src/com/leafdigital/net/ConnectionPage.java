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
package com.leafdigital.net;

import javax.swing.SwingUtilities;

import com.leafdigital.ui.api.*;

import leafchat.core.api.PluginContext;

/**
 * Preferences page with network connection options.
 */
@UIHandler("connectionpage")
public class ConnectionPage
{
	private Page p;
	private NetPlugin np;

	/**
	 * UI: P&P enabled.
	 */
	public RadioButton upnpUI;
	/**
	 * UI: manual address.
	 */
	public RadioButton manualUI;
	/**
	 * UI: use proxy.
	 */
	public RadioButton proxyUI;

	/**
	 * UI: manual address.
	 */
	public EditBox manualAddressUI;
	/**
	 * UI: manual min port.
	 */
	public EditBox manualPortMinUI;
	/**
	 * UI: manual max port.
	 */
	public EditBox manualPortMaxUI;
	/**
	 * UI: proxy address.
	 */
	public EditBox proxyAddressUI;
	/**
	 * UI: proxy port.
	 */
	public EditBox proxyPortUI;
	/**
	 * UI: proxy username.
	 */
	public EditBox proxyUsernameUI;
	/**
	 * UI: proxy password.
	 */
	public EditBox proxyPasswordUI;

	ConnectionPage(PluginContext context)
	{
		UI ui = context.getSingleton2(UI.class);
		p = ui.createPage("connectionpage", this);

		np=(NetPlugin)context.getPlugin();
	}

	/**
	 * @return Page object
	 */
	public Page getPage()
	{
		return p;
	}

	/**
	 * Action: Click manual address option.
	 */
	@UIAction
	public void actionManual()
	{
		proxyAddressUI.setEnabled(false);
		proxyPortUI.setEnabled(false);
		proxyUsernameUI.setEnabled(false);
		proxyPasswordUI.setEnabled(false);
		manualAddressUI.setEnabled(true);
		manualPortMinUI.setEnabled(true);
		manualPortMaxUI.setEnabled(true);
		np.setConnectionType(NetPlugin.CONNECTION_MANUAL);
	}

	/**
	 * Action: Click proxy option.
	 */
	@UIAction
	public void actionProxy()
	{
		proxyAddressUI.setEnabled(true);
		proxyPortUI.setEnabled(true);
		proxyUsernameUI.setEnabled(true);
		proxyPasswordUI.setEnabled(true);
		manualAddressUI.setEnabled(false);
		manualPortMinUI.setEnabled(false);
		manualPortMaxUI.setEnabled(false);
		np.setConnectionType(NetPlugin.CONNECTION_SOCKS5);
	}

	/**
	 * Action: Click PnP option.
	 */
	@UIAction
	public void actionUPnP()
	{
		proxyAddressUI.setEnabled(false);
		proxyPortUI.setEnabled(false);
		proxyUsernameUI.setEnabled(false);
		proxyPasswordUI.setEnabled(false);
		manualAddressUI.setEnabled(false);
		manualPortMinUI.setEnabled(false);
		manualPortMaxUI.setEnabled(false);
		np.setConnectionType(NetPlugin.CONNECTION_UPNP);
	}

	/**
	 * Action: Change proxy settings.
	 */
	@UIAction
	public void changeProxySettings()
	{
		if(proxyAddressUI.getFlag()==EditBox.FLAG_NORMAL)
			np.setSOCKSHost(proxyAddressUI.getValue());
		if(proxyPortUI.getFlag()==EditBox.FLAG_NORMAL)
			np.setSOCKSPort(Integer.parseInt(proxyPortUI.getValue()));
		if(proxyUsernameUI.getFlag()==EditBox.FLAG_NORMAL)
			np.setSOCKSUsername(proxyUsernameUI.getValue());
		if(proxyPasswordUI.getFlag()==EditBox.FLAG_NORMAL)
			np.setSOCKSPassword(proxyPasswordUI.getValue());
	}

	/**
	 * Page shown.
	 */
	@UIAction
	public void onSet()
	{
		// Initialise radio button and enable fields
		int type=np.getConnectionType();
		switch(type)
		{
		case NetPlugin.CONNECTION_UPNP:
			upnpUI.setSelected();
			actionUPnP();
			break;
		case NetPlugin.CONNECTION_MANUAL:
			manualUI.setSelected();
			actionManual();
			break;
		case NetPlugin.CONNECTION_SOCKS5:
			proxyUI.setSelected();
			actionProxy();
			break;
		}

		// Check if gateway is available and enable UPnP option
		if(type!=NetPlugin.CONNECTION_UPNP)
		{
			upnpUI.setEnabled(false);
			(new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					if(np.getUPnPGateway()!=null)
					{
						SwingUtilities.invokeLater(new Runnable()
						{
							@Override
							public void run()
							{
								upnpUI.setEnabled(true);
							}
						});
					}
				}
			},"leafChat UPnP check thread")).start();
		}

		// Fill in proxy settings
		proxyAddressUI.setValue(np.getSOCKSHost());
		proxyPortUI.setValue(np.getSOCKSPort()+"");
		proxyUsernameUI.setValue(np.getSOCKSUsername());
		proxyPasswordUI.setValue(np.getSOCKSPassword());

		// Fill in manual settings
		manualAddressUI.setValue(np.getManualPublicAddress()==null ? "" : np.getManualPublicAddress());
		manualPortMinUI.setValue(np.getListenPortMin()+"");
		manualPortMaxUI.setValue(np.getListenPortMax()+"");
	}

	/**
	 * Action: Change manual settings.
	 */
	@UIAction
	public void changeManualSettings()
	{
		// Address
		if(manualAddressUI.getFlag()==EditBox.FLAG_NORMAL)
		{
			String address=manualAddressUI.getValue();
			np.setManualPublicAddress(address.equals("") ? null : address);
		}

		// Ports
		String
			minString=manualPortMinUI.getValue(),
			maxString=manualPortMaxUI.getValue();
		int min=-1,max=-1;
		if(minString.matches("[0-9]{1,8}"))	min=Integer.parseInt(minString);
		if(maxString.matches("[0-9]{1,8}")) 	max=Integer.parseInt(maxString);
		if(min==-1 || max==-1 || min<1024 || max<1024 || min>max || min>65535 || max>65535)
		{
			manualPortMinUI.setFlag(EditBox.FLAG_ERROR);
			manualPortMaxUI.setFlag(EditBox.FLAG_ERROR);
		}
		else
		{
			manualPortMinUI.setFlag(EditBox.FLAG_NORMAL);
			manualPortMaxUI.setFlag(EditBox.FLAG_NORMAL);
			np.setListenPorts(min,max);
		}
	}
}
