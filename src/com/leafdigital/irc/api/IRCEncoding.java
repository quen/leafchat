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
package com.leafdigital.irc.api;

import leafchat.core.api.Singleton;

/**
 * Provides character encoding information. Character encoding settings come
 * from the user's preferences; this class can obtain those settings for any
 * given context.
 * <p>
 * Also contains a method for processing colour codes.
 */
public interface IRCEncoding extends Singleton
{
	/** Information about encoding in a different context */
	public interface EncodingInfo
	{
		/**
		 * @return Incoming encoding
		 */
		public String getEncoding();

		/**
		 * @return Outgoing default encoding (UTF-8 is always used if this doesn't
		 *   support the text)
		 */
		public String getOutgoing();

		/**
		 * @return True if UTF-8 should be used by default for incoming text
		 */
		public boolean isUTF8();

		/**
		 * Converts incoming text as specified by preferences
		 * @param data Incoming data
		 * @return Converted
		 */
		public String convertIncoming(byte[] data);

		/**
		 * Converts outgoing text using the given charset or UTF-8.
		 * @param text Text
		 * @return Bytes
		 */
		public byte[] convertOutgoing(String text);
	}

	/**
	 * Obtains the character encoding for a given situation.
	 * @param s Server (may be null)
	 * @param chan Channel (may be null)
	 * @param user User (may be null)
	 * @return Encoding information
	 */
	public EncodingInfo getEncoding(Server s,String chan,IRCUserAddress user);

	/**
	 * Processes a string for IRC style escapes. If you allow styles or colours,
	 * then XML tags will be added to the string to indicate
	 * where colours start and end. In this case the string must have been
	 * preprocessed to escape XML characters. If you don't allow colours then
	 * they are simply removed (and the string need not be escaped).
	 * @param input Input text
	 * @param allowStyles True to allow styles (bold etc)
	 * @param allowColours True to allow colours
	 * @return Processed string
	 */
	public String processEscapes(String input,
		boolean allowStyles, boolean allowColours);
}
