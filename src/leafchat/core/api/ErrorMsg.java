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

import leafchat.core.MessageManager;

/**
 * Sent when an error has occurred and should be reported to the user.
 * Normally handled to display a dialog box.
 */
public class ErrorMsg extends Msg
{
	/** Exception or null */
	private Throwable t;

	/** Message text */
	private String message;

	/** True if the message has been displayed to user etc. */
	private boolean bHandled;

	/**
	 * This method can be called directly to report errors to the user. It usually
	 * results in a dialog box appearing with details.
	 * @param message Text of message (may be null)
	 * @param t Exception (may be null)
	 */
	public static void report(String message,Throwable t)
	{
		ErrorMsg em=new ErrorMsg(message,t);
		try
		{
			MessageManager.get().externalDispatch(
				ErrorMsg.class,em, MessageManager.isEventThread());
		}
		catch(Throwable t2)
		{
			System.err.println("Problem dispatching error message. Error was:\n"+
			  message);
			t.printStackTrace();
			System.err.println();
			t2.printStackTrace();
		}
	}

	/**
	 * Constructs message
	 * @param message Text of message (may be null)
	 * @param t Exception (may be null)
	 */
	private ErrorMsg(String message,Throwable t)
	{
		this.message=message;
		this.t=t;
	}

	/**
	 * @return Message text or null if none
	 */
	public String getMessage() { return message; }

	/**
	 * @return Exception or null if none
	 */
	public Throwable getException() { return t; }

	/**
	 * @return True if message has already been displayed to user and shouldn't
	 *   be shown again.
	 */
	@Override
	public boolean isHandled() { return bHandled; }

	/** Call once message has been displayed to user. */
	@Override
	public void markHandled() { this.bHandled=true; }

	/**
	 * Message info
	 */
	public static MessageInfo info=new MessageInfo(ErrorMsg.class)
	{
		@Override
		public boolean allowScripting()
		{
			return false;
		}
	};

}
