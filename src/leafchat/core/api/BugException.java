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
 * Exception probably indicates a bug in a plugin or the system. This is a
 * RuntimeException class so you don't need to catch it.
 */
public class BugException extends RuntimeException
{
	/**
	 * Constructs empty exception.
	 */
	public BugException()
	{
		super();
	}

	/**
	 * @param message Message
	 */
	public BugException(String message)
	{
		super(message);
	}

	/**
	 * @param cause Cause
	 */
	public BugException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message Message
	 * @param cause Cause
	 */
	public BugException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
