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

import util.StringUtils;

/**
 * Address of a user in "nick (exclamation) user (at) host" format.
 * <p>
 * Note: This implements {@link Comparable}<Object> for compatibility reasons;
 * implementing {@link Comparable}<IRCUserAddress> would remove binary
 * compatibility with earlier versions.
 */
public class IRCUserAddress implements Comparable<Object>
{
	/** Nickname */
	private String nick;
	/** Username */
	private String user="";
	/** Hostname */
	private String host="";

	/**
	 * Constructs from a string in format "nick (exclamation) user (at) host".
	 * All parts are optional; if there's
	 * no ! then it will only be a nick, no @ then nick!user.
	 * @param text User address in appropriate string format
	 * @param pattern If true, parts that would otherwise be blank become *
	 */
	public IRCUserAddress(String text, boolean pattern)
	{
		int iExclamation=text.indexOf('!');
		if(iExclamation==-1)
		{
			nick=text;
		}
		else
		{
			nick=text.substring(0,iExclamation);
			text=text.substring(iExclamation+1);
			int iAt=text.indexOf('@');
			if(iAt==-1)
			{
				user=text;
			}
			else
			{
				user=text.substring(0,iAt);
				host=text.substring(iAt+1);
			}
		}

		if(pattern)
		{
			if(nick.length()==0) nick="*";
			if(user.length()==0) user="*";
			if(host.length()==0) host="*";
		}
	}

	/**
	 * @param nick Nickname
	 * @param user Username
	 * @param host Hostname
	 */
	public IRCUserAddress(String nick,String user,String host)
	{
		if(nick==null || user==null || host==null)
			throw new IllegalArgumentException(
				"IRCUserAddress constructor requires non-null parameters");
		this.nick=nick;
		this.user=user;
		this.host=host;
	}

	/** @return Nickname */
	public String getNick() { return nick; }
	/** @return Username */
	public String getUser() { return user; }
	/** @return Hostname */
	public String getHost() { return host; }

	/**
	 * @return IRC mask string, "nick (exclamation) user (at) host"
	 */
	@Override
	public String toString()
	{
		return nick+"!"+user+"@"+host;
	}

	/**
	 * @param o Comparison object
	 * @return True if the three parts are equal (ignoring case)
	 */
	@Override
	public boolean equals(Object o)
	{
		if(o==null || !(o instanceof IRCUserAddress)) return false;
		return toString().equalsIgnoreCase(o.toString());
	}

	/**
	 * IRCUserAddress is sorted by nickname, username, then host.
	 * @param obj Comparison object
	 * @return Positive if this one is later than the comparison o
	 * @throws ClassCastException If obj is not an {@link IRCUserAddress}
	 */
	@Override
	public int compareTo(Object obj) throws ClassCastException
	{
		IRCUserAddress other = (IRCUserAddress)obj;
		int i=nick.compareToIgnoreCase(other.nick);
		if(i!=0) return i;
		i=user.compareToIgnoreCase(other.user);
		if(i!=0) return i;
		i=host.compareToIgnoreCase(other.host);
		return i;
	}

	/**
	 * @return Hash code based on lower-case version of address
	 */
	@Override
	public int hashCode()
	{
		return toString().toLowerCase().hashCode();
	}

	/**
	 * @param pattern Another IRCUserAddress which may include * wildcards
	 * @return True if this address matches the pattern
	 */
	public boolean matches(IRCUserAddress pattern)
	{
		return StringUtils.matchWildcard(pattern.nick,nick) &&
			StringUtils.matchWildcard(pattern.user,user) &&
			StringUtils.matchWildcard(pattern.host,host);
	}
}
