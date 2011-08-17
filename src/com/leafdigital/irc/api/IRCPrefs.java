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

/**
 * Includes constants used for preferences that belong to the IRC module.
 * Some preferences are used by other plugins, so this list is public.
 */
public interface IRCPrefs
{
	/** Servers group */
	public final static String PREFGROUP_SERVERS="servers";

	// Basic server prefs

	/** Security mode */
	public static final String PREF_SECUREMODE = "secure-mode";
	/** No security */
	public static final String PREF_SECUREMODE_NONE = "none";
	/** Optional security. */
	public static final String PREF_SECUREMODE_OPTIONAL = "optional";
	/** Required security. */
	public static final String PREF_SECUREMODE_REQUIRED = "required";
	/** Default security mode */
	public static final String PREFDEFAULT_SECUREMODE = PREF_SECUREMODE_OPTIONAL;

	/** User name */
	public static final String PREF_USER="user";
	/** Default username */
	public static final String PREFDEFAULT_USER="lc";

	/** Realname */
	public static final String PREF_REALNAME="realname";
	/** Default realname */
	public static final String PREFDEFAULT_REALNAME="leafChat 2 user";

	/** Auto-identify pref */
	public static final String PREF_AUTOIDENTIFY="autoidentify";

	/** Deprecated */
	public static final String PREF_IDENTIFYCOMMAND="identifycommand";
	/** Deprecated */
	public static final String PREFDEFAULT_IDENTIFYCOMMAND="/identify";

	/** Pattern used for identify command */
	public static final String PREF_IDENTIFYPATTERN="identifypattern";
	/** Default pattern for identify command */
	public static final String PREFDEFAULT_IDENTIFYPATTERN="/identify ${password}";

	/** Quit message */
	public static final String PREF_QUITMESSAGE="quitmessage";
	/** Default quit message */
	public static final String PREFDEFAULT_QUITMESSAGE="Using leafChat 2";

	// Server prefs dealing with nickname system
	/** Nickname group */
	public static final String PREFGROUP_NICKS="nicks";
	/** Default nick */
	public static final String PREF_DEFAULTNICK="defaultnick";
	/** Password */
	public static final String PREF_PASSWORD="password";
	/** Nick */
	public static final String PREF_NICK="nick";

	// Server prefs used to store information about connection
	/** Network suffix */
	public static final String PREF_NETWORKSUFFIX="networksuffix";
	/** Network */
	public static final String PREF_NETWORK="network";
	/** Reported host */
	public static final String PREF_REPORTED="reported";
	/** True if added by hand */
	public static final String PREF_HANDADDED="handadded";
	/** Host */
	public static final String PREF_HOST="host";
	/** Remember if user refused network auto-add */
	public static final String PREF_REFUSEDNETWORK="refusednetwork";
	/** Port range */
	public static final String PREF_PORTRANGE="portrange";
	/** Default port range */
	public static final String PREFDEFAULT_PORTRANGE="6667";
	/** Server password */
	public static final String PREF_SERVERPASSWORD="password";
	/** Default server password */
	public static final String PREFDEFAULT_SERVERPASSWORD="";
	/** Failure tracking */
	public static final String PREF_FAILURES="failures";

	// Server prefs for channels
	/** Channels group */
	public static final String PREFGROUP_CHANNELS="channels";
	/** Channel name */
	public static final String PREF_NAME="name";
	/** Channel key */
	public static final String PREF_KEY="key";
	/** Autojoin for channel */
	public static final String PREF_AUTOJOIN="autojoin";

	// Ignore list
	/** Ignore group */
	public static final String PREFGROUP_IGNORE="ignore";
	/** Nick to ignore */
	public static final String PREF_IGNORE_NICK="nick";
	/** User to ignore */
	public static final String PREF_IGNORE_USER="user";
	/** Host to ignore */
	public static final String PREF_IGNORE_HOST="host";

	// Watch list
	/** Watch group */
	public static final String PREFGROUP_WATCH="watch";
	/** Nick to watch */
	public static final String PREF_WATCH_NICK="nick";
	/** User to watch */
	public static final String PREF_WATCH_USER="user";
	/** Host to watch */
	public static final String PREF_WATCH_HOST="host";

	// Character encoding. Stored in 'encoding' group
	/** Encoding group */
	public static final String PREFGROUP_ENCODING="encoding";
	// Standard encoding used if no overrides
	/** Standard encoding */
	public static final String PREF_ENCODING="encoding";
	/** Default standard encoding */
	public static final String PREFDEFAULT_ENCODING="ISO-8859-15";
	// If true, checks for UTF-8 first
	/** Automatic UTF-8 support */
	public static final String PREF_UTF8="utf8";
	/** Default for automatic UTF-8 support */
	public static final String PREFDEFAULT_UTF8="t";
	// Outgoing encoding
	/** Outgoing encoding */
	public static final String PREF_OUTGOING="outgoing";
	/** Default outgoing encoding */
	public static final String PREFDEFAULT_OUTGOING="UTF-8";
	// Group storing encoding for specific channel. Contains anon groups.
	/** Encoding per-channel group */
	public static final String PREFGROUP_BYCHAN="bychan";
	/** Channel name */
	public static final String PREF_BYCHAN_NAME="name";
	/** Incoming encoding */
	public static final String PREF_BYCHAN_ENCODING="encoding";
	/** Outgoing encoding */
	public static final String PREF_BYCHAN_OUTGOING="outgoing";
	/** Automatic UTF-8 */
	public static final String PREF_BYCHAN_UTF8="utf8";
	// Group storing encoding for specific user. Contains anon groups.
	/** Encoding per-user group */
	public static final String PREFGROUP_BYUSER="byuser";
	/** User mask */
	public static final String PREF_BYUSER_MASK="mask";
	/** Incoming encoding */
	public static final String PREF_BYUSER_ENCODING="encoding";
	/** Outgoing encoding */
	public static final String PREF_BYUSER_OUTGOING="outgoing";
	/** Automatic UTF-8 */
	public static final String PREF_BYUSER_UTF8="utf8";
	// Per-server encoding are stored in PREF_ENCODING/PREF_UTF8 inside the
	// server/net prefs and are optional.

	/** Extra command character other than / */
	public static final String PREF_EXTRACOMMANDCHAR="extra-command-character";
	/** Default extra command character */
	public static final String PREFDEFAULT_EXTRACOMMANDCHAR="";

	/** Frequent pings (for dodgy connection) */
	public static final String PREF_FREQUENTPINGS="frequent-pings";
	/** Default frequent ping */
	public static final String PREFDEFAULT_FREQUENTPINGS="f";

	/** Whether away message applies across all servers */
	public static final String PREF_AWAYMULTISERVER="away-multi-server";
	/** Default away multi-server */
	public static final String PREFDEFAULT_AWAYMULTISERVER="t";

	/** Class name for IRC plugin where prefs are stored */
	public static final String IRCPLUGIN_CLASS="com.leafdigital.irc.IRCPlugin";
}
