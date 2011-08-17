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
import leafchat.core.api.*;

/** Filters messages based on their source user nickname, username, and host */
public class UserFilter implements MessageFilter
{
	private String nick,user,host;

	/**
	 * @param nick Nickname (not case-sensitive) from which messages are accepted;
	 *   may include wildcard *
	 * @param user Username (not case-sensitive) from which messages are accepted;
	 *   may include wildcard *
	 * @param host Host address (not case-sensitive) from which messages are accepted;
	 *   may include wildcard *
	 */
	public UserFilter(String nick,String user,String host)
	{
		this.nick=nick.toLowerCase();
		this.user=user.toLowerCase();
		this.host=host.toLowerCase();
	}

	@Override
	public boolean accept(Msg m)
	{
		if(!(m instanceof UserSourceIRCMsg)) return false;
		return
			StringUtils.matchWildcard(
				nick,((UserSourceIRCMsg)m).getSourceUser().getNick().toLowerCase()) &&
			StringUtils.matchWildcard(
				user,((UserSourceIRCMsg)m).getSourceUser().getUser().toLowerCase()) &&
			StringUtils.matchWildcard(
				host,((UserSourceIRCMsg)m).getSourceUser().getHost().toLowerCase());
	}

	/** Scripting filter information. */
	public static FilterInfo info=new FilterInfo(UserFilter.class,"Source user")
	{
		@Override
		public Parameter[] getScriptingParameters()
		{
			return new Parameter[]
      {
				new Parameter(String.class,"Nickname","Wildcard * is accepted"),
				new Parameter(String.class,"Username","Appears before @ in the user's information; use * as wildcard"),
				new Parameter(String.class,"Hostname","User's Internet address, available on some servers; use * as wildcard")
			};
		}
	};
}
