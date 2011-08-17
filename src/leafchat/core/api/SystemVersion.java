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

/** Access to leafChat core version numbers */
public class SystemVersion
{
	/** @return Version number for display in title bar */
	public static String getTitleBarVersion()
	{
		return "@TITLEBARVERSION@";
	}

	/** @return Version number for CTCP VERSION reports */
	public static String getCTCPVersion()
	{
		return "@CTCPVERSION@";
	}

	/** @return Internal build identifier */
	public static String getBuildVersion()
	{
		return "@BUILDVERSION@";
	}

}
