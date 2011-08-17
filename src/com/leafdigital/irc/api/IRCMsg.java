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

import java.io.UnsupportedEncodingException;
import java.util.*;

import com.leafdigital.irc.api.IRCEncoding.EncodingInfo;

import leafchat.core.api.*;

/** Represents a parsed message received from an IRC server, parsed */
public class IRCMsg extends Msg
{
	/** Server that sent the message */
	private Server s = null;

	/** Entire line */
	private byte[] line = null;
	/** Prefix (may be null) */
	private byte[] prefix = null;
	/** Command */
	private byte[] command = null;
	/** Parameters */
	private byte[][] params = new byte[][]{};
	/** Whether or not last param was a postfix type */
	private boolean includesPostfix;

	/** Encoding set by user for this type of message */
	private EncodingInfo encoding;

	/**
	 * Converts text using ISO 8859-1 which is generally appropriate for 'system'
	 * parts of a message
	 * @param data Bytes
	 * @return String
	 */
	public static String convertISO(byte[] data)
	{
		if(data == null)
		{
			return null;
		}
		try
		{
			return new String(data, "ISO-8859-1");
		}
		catch(UnsupportedEncodingException e)
		{
			throw new Error("Missing basic character encodings",e);
		}
	}

	/**
	 * @param data Bytes from this message
	 * @return Message converted into a string as per user's encoding preferences
	 */
	public String convertEncoding(byte[] data)
	{
		return encoding.convertIncoming(data);
	}

	/**
	 * Given an array of bytes, splits it on spaces.
	 * @param data Data to split
	 * @return Array of byte arrays with split data
	 */
	public static byte[][] splitBytes(byte[] data)
	{
		List<byte[]> arrays = new LinkedList<byte[]>();
		for(int i=0; i<data.length; i++)
		{
			if(data[i] == 32)
			{
				if(i > 0)
				{
					byte[] before = new byte[i];
					System.arraycopy(data, 0, before, 0, i);
					arrays.add(before);
				}
				if(i+1 < data.length)
				{
					byte[] after = new byte[data.length - ( i+ 1)];
					System.arraycopy(data, i+1, after, 0, data.length - (i + 1));
					data = after;
					i = -1; // Restart loop for new 'data' array
				}
				else
				{
					// Nothing after the space
					data = new byte[0];
					break;
				}
			}
		}
		if(data.length>0)
		{
			arrays.add(data);
		}
		return arrays.toArray(new byte[arrays.size()][]);
	}


	/**
	 * Creates bytes suitable for an IRC message where the first part is in ISO
	 * 8859-1 and the second part has been converted to bytes
	 * @param isoPart ISO part of text (may be null)
	 * @param secondPart Other character set part (may be null)
	 * @return Bytes containing both parts
	 */
	public static byte[] constructBytes(
		String isoPart, byte[] secondPart)
	{
		try
		{
			byte[] data1 = null, data2 = secondPart;
			if(isoPart != null)
			{
				data1 = isoPart.getBytes("ISO-8859-1");
			}
			if(data1 == null && data2 == null)
			{
				return new byte[0];
			}
			if(data2 == null)
			{
				return data1;
			}
			if(data1 == null)
			{
				return data2;
			}

			byte[] out = new byte[data1.length + data2.length];
			System.arraycopy(data1, 0, out, 0, data1.length);
			System.arraycopy(data2, 0, out, data1.length, data2.length);
			return out;
		}
		catch(UnsupportedEncodingException e)
		{
			throw new Error(e);
		}
	}

	/**
	 * @param iso String that should be converted to bytes
	 * @return Bytes of string in ISO-8859-1 charset
	 */
	public static byte[] constructBytes(String iso)
	{
		return constructBytes(iso,null);
	}

	/**
	 * Sets up encoding for message (used by system only).
	 * @param encoding Encoding
	 */
	public void setEncoding(EncodingInfo encoding)
	{
		this.encoding = encoding;
	}

	/** @return True if encoding has been set up (used by system) */
	public boolean hasEncoding()
	{
		return encoding!=null;
	}

	/**
	 * @param base Message to copy parameters from
	 */
	public void init(IRCMsg base)
	{
		this.s = base.s;
		this.line = base.line;
		this.prefix = base.prefix;
		this.command = base.command;
		this.params = base.params;
		this.includesPostfix = base.includesPostfix;
		setSequence(base);
	}

	/**
	 * Init with actual parameters
	 * @param s Server
	 * @param line Original line
	 * @param prefix Prefix (or null)
	 * @param command Command
	 * @param params Parameters
	 * @param includesPostfix Whether or not the last param was a postfix type
	 */
	public void init(Server s,
		byte[] line, byte[] prefix, byte[] command, byte[][] params, boolean includesPostfix)
	{
		this.s = s;
		this.line = line;
		this.prefix = prefix;
		this.command = command;
		this.params = params;
		this.includesPostfix = includesPostfix;
	}

	/** @return Server that sent the message */
	public Server getServer()	 { 	return s;	}
	/** @return Entire line */
	public byte[] getLine() { return line; };
	/** @return Entire line as string */
	public String getLineISO() { return convertISO(line); }
	/** @return Prefix (may be null) */
	public byte[] getPrefixBytes() { return prefix; }
	/** @return Prefix (may be null) */
	public String getPrefix() { return convertISO(prefix); }
	/** @return Command */
	public byte[] getCommandBytes() { return command; }
	/** @return Command */
	public String getCommand() { return convertISO(command); }
	/** @return Parameters */
	public byte[][] getParams() { return params; }

	/**
	 * @param index Parameter index
	 * @return ISO-converted string
	 */
	public String getParamISO(int index)
	{
		return convertISO(getParams()[index]);
	}

	/**
	 * @param index Parameter index
	 * @return True if it is an integer
	 */
	public boolean isParamInteger(int index)
	{
		return getParamISO(index).matches("-?[0-9]+");
	}

	/** @return True if last param was a postfix type */
	public boolean includesPostfix()
	{
		return includesPostfix;
	}

	@Override
	public String toString()
	{
		String className = getClass().getName();
		int dot = className.lastIndexOf('.');
		if(dot != -1)
		{
			className=className.substring(dot + 1);
		}
		String value=
		  "[" + className + "]\n"
			+ "  Server: " + s + "\n"
			+ "  Line: "+convertISO(line) + "\n"
			+ "  Prefix: "+getPrefix() + "\n"
			+ "  Command: "+getCommand() + "\n";
		for(int i=0;i<params.length;i++)
		{
			value+="  Param: "+convertISO(params[i])+
			  (i==params.length-1 && includesPostfix() ? " [postfix]\n" : "\n");
		}
		return value;
	}

	/**
	 * Message info for scripting.
	 */
	public static MessageInfo info = new MessageInfo(IRCMsg.class, "IRC",
		"Events that are received from an IRC server.")
	{
		@Override
		public String getContextInit()
		{
			return "registerContext(msg.getServer(),null,null,null);";
		}
		@Override
		public boolean allowScripting()
		{
			return true;
		}
		@Override
		protected void listAppropriateFilters(Collection<FilterInfo> list)
		{
			super.listAppropriateFilters(list);
			list.add(ServerFilter.info);
		}
	};
}
