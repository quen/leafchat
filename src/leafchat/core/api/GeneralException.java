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
package leafchat.core.api;

/**
 * General exceptions (this is often used to wrap other exceptions so that
 * handling is easier, but we know they've been considered)
 */
public class GeneralException extends Exception
{
	/**
	 * Constructs empty exception.
	 */
	public GeneralException()
	{
		super();
	}

	/**
	 * @param message Message
	 */
	public GeneralException(String message)
	{
		super(message);
	}

	/**
	 * @param cause Cause
	 */
	public GeneralException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message Message
	 * @param cause Cause
	 */
	public GeneralException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
