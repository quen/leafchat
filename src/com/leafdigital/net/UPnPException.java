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

/** Exception thrown when connection problems are caused by UPnP gateway */
public class UPnPException extends IOException
{
	/**
	 * @param message Error
	 * @param t Exception
	 */
	public UPnPException(String message,Throwable t)
	{
		super(message);
		initCause(t);
	}

	/**
	 * @param message Error
	 */
	public UPnPException(String message)
	{
		super(message);
	}
}
