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

import java.io.IOException;

/** Exception thrown for SOCKS5 connection issues */
public class SOCKS5Exception extends IOException
{
	SOCKS5Exception(String message,Throwable cause)
	{
		super(message);
		initCause(cause);
	}
	SOCKS5Exception(String message)
	{
		super("Proxy reports error: "+message);
	}
	private static String getMessage(int reply)
	{
		switch(reply)
		{
		case 1: return "General SOCKS server failure";
		case 2: return "Connection not permitted by SOCKS server";
		case 3: return "Network unreachable";
		case 4: return "Remote host: unreachable";
		case 5: return "Remote host: connection refused";
		case 6: return "TTL expired";
		case 7: return "Command not supported";
		case 8: return "Address type not supported";
		default: return "Unknown SOCKS error";
		}
	}
	SOCKS5Exception(int reply)
	{
		super(getMessage(reply));
	}
}
