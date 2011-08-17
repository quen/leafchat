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
 * Generic object representing communication between plugins. All messages
 * must extend this class.
 * <p>
 * In addition to the visible API below, user-defined message types may include
 * a public static {@link MessageInfo} member called 'info'. This provides
 * information for the scripting system. For example, if a message type isn't
 * appropriate for use as a scripting event, override the MessageInfo to return
 * false to {@link MessageInfo#allowScripting()}. Here's a code example.
 *
<pre>public static MessageInfo info=new MessageInfo(ErrorIRCMsg.class,"Error",
	"The server sends this event when you quit or are otherwise disconnected.")
{
	protected void listScriptingVariables(Variables v)
	{
		super.listScriptingVariables(v);
		v.add("message");
	}
};</pre>
 */
public abstract class Msg
{
	/** Indicates message should not be further processed */
	private boolean stopped;

	/**
	 * Informative indicator that message has already been 'handled' (displayed
	 * to user etc.) although it is still being passed through
	 */
	private boolean handled;

	/** @return True if message processing has been stopped */
	public boolean isStopped()
	{
		return stopped;
	}

	/** Call to stop further processing */
	public void markStopped()
	{
		stopped=true;
	}

	/** @return True if message has already been 'handled' (usually 'displayed to user') */
	public boolean isHandled()
	{
		return handled;
	}

	/** Call to mark handled; still processed, but marked as already shown to user */
	public void markHandled()
	{
		handled=true;
	}

	/**
	 * Priority constant:
	 * Use where message should be handled last
	 */
	public final static int PRIORITY_LAST = 6000;
	/**
	 * Priority constant:
	 * Use where message should be handled after most handlers, but not
	 * necessarily last
	 */
	public final static int PRIORITY_LATE = 8000;
	/**
	 * Priority constant:
	 * Where message should be handled after normal handlers
	 */
	public final static int PRIORITY_AFTERNORMAL = 9000;
	/**
	 * Priority constant:
	 * Use for default message handling or where order does not matter
	 */
	public final static int PRIORITY_NORMAL = 10000;
	/**
	 * Priority constant:
	 * Where message should be handled before normal handlers
	 */
	public final static int PRIORITY_BEFORENORMAL = 11000;
	/**
	 * Priority constant:
	 * Use where message should be handled before most other handlers
	 */
	public final static int PRIORITY_EARLY = 12000;
	/**
	 * Priority constant:
	 * Use where message should be handled first
	 */
	public final static int PRIORITY_FIRST = 14000;

	private String sequenceName=null;
	private int sequenceNumber;

	/**
	 * Associate this message with a particular sequence. There can be any number
	 * of sequences, each with an arbitrary name. A message with number N in
	 * a sequence S will only be dispatched once there are no messages with number
	 * M<N and sequence S in the pending-message queue. (Note that this does not
	 * completely guarantee sequencing in all cases, since the queue could become
	 * empty and then an earlier-sequenced message could be sent.)
	 * @param sequenceName Arbitrary sequence name
	 * @param sequenceNumber Position in sequence
	 */
	public void setSequence(String sequenceName,int sequenceNumber)
	{
		this.sequenceName=sequenceName;
		this.sequenceNumber=sequenceNumber;
	}
	/**
	 * Indicates that this message should belong to the same sequence as some
	 * other message (i.e. this one is presumably being dispatched in response
	 * to that one).
	 * @param relative Message whose sequence details will be copied.
	 */
	public void setSequence(Msg relative)
	{
		this.sequenceName=relative.sequenceName;
		this.sequenceNumber=relative.sequenceNumber;
	}
	/**
	 * Checks whether this message ought to run before the message under
	 * consideration.
	 * @param consider Message being considered
	 * @return True if this message should run earlier than the considered one
	 */
	public boolean sequenceBefore(Msg consider)
	{
		if(sequenceName==null || consider.sequenceName==null) return false;
		if(!sequenceName.equals(consider.sequenceName)) return false;
		return sequenceNumber < consider.sequenceNumber;
	}
}
