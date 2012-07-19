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

import java.net.InetAddress;

import com.leafdigital.prefs.api.PreferencesGroup;

import leafchat.core.api.BugException;

/** Represents a low-level server connection. */
public interface Server
{
	/** Used to report that a server request is not tracked */
	public final static int UNTRACKED_REQUEST=0;

	/**
	 * Interface used to report on progress of connection.
	 */
	public interface ConnectionProgress
	{
		/**
		 * Method is called from within server thread, so be careful!
		 * @param xml Text describing progress
		 */
		public void progress(String xml);
	}

	/**
	 * Starts the process of connecting to the server. Returns immediately. To
	 * monitor connection, you may synchronize on this ServerConnection object
	 * and wait repeatedly until either isConnected, or getError, returns something.
	 * @param host Host for connection
	 * @param port Port for connection
	 * @param cp Object that monitors progress of connection
	 */
	public void beginConnect(String host,int port,ConnectionProgress cp);

	/** @return True if the socket is connected. */
	public boolean isConnected();

	/** @return True if the connection is finished i.e. if ServerConnectionFinishedMsg was/is being sent */
	public boolean isConnectionFinished();

	/** @return True if currently connected and the connection is secure (SSL) */
	public boolean isSecureConnection();

	/** @return An exception that occurred, preventing connection (or null if none) */
	public NetworkException getError();

	/** @return True once a QUIT command has been sent to the server */
	public boolean wasQuitRequested();

	/**
	 * @return Default MessageDisplay for this server
	 * @throws IllegalStateException If
	 *   Connections.setDefaultMessageDisplay was not called
	 */
	public MessageDisplay getDefaultMessageDisplay() throws IllegalStateException;

	/**
	 * Sends a line of data to the server.
	 * @param line Line of data, not including CRLF
	 * @see #sendServerRequest(byte[])
	 */
	public void sendLine(byte[] line);

	/**
	 * Sends a line of data to the server. If the line corresponds to a supported
	 * server request, e.g. WHO, then the server will return a unique identifier
	 * which can be used to identify the response to that particular request.
	 * @param line Line of data, not including CRLF
	 * @return Tracking ID or UNTRACKED_REQUEST
	 * @see ServerIRCMsg#getResponseID()
	 */
	public int sendServerRequest(byte[] line);

	/**
	 * Disconnects from the server after sending a quit message.
	 */
	public void disconnectGracefully();

	/**
	 * Disconnects from the server.
	 */
	public void disconnect();

	/** @return Our current nickname, may be null if not connected */
	public String getOurNick();
	/** @return Our current username (as the server thinks), may be null if not yet
	 * known */
	public String getOurUser();
	/** @return Our current hostname (as the server thinks), may be null if not yet
	 * known */
	public String getOurHost();

	/**
	 * @return Approximate length in bytes of the current prefix :nick!user@host,
	 *   possibly allowing a slight overestimate in case servers mess with hostnames.
	 */
	public int getApproxPrefixLength();


	/** @return Reported hostname, may be null if not connected or not set */
	public String getReportedHost();

	/** @return Connected hostname (the one specified in beginConnect), may be null if not connected */
	public String getConnectedHost();

	/** @return Connected port, may be 0 if not connected */
	public int getConnectedPort();

	/** @return Connected remote IP address, may be null if not connected */
	public String getConnectedIpAddress();

	/** @return The internet address at this end of the connection */
	public InetAddress getLocalAddress();

	/**
	 * @return Short name. The short name varies depending on which servers are
	 * connected, and ranges from the network name, through the server name, up to
	 * the server name plus port.
	 */
	public String getCurrentShortName();

	/** @return Reported hostname if set, otherwise connected one if connected, otherwise null */
	public String getReportedOrConnectedHost();

	/** @return Server version string, may be null if unknown */
	public String getVersion();

	/** @return Preferences entries for this server (must be connected)
	 * @throws BugException If not connected (or something else goes horribly wrong) */
	public PreferencesGroup getPreferences();

	/** @return Default nick from preferences */
	public String getDefaultNick();

	/** @return Quit message from preferences */
	public String getQuitMessage();

	/** @return Server password from preferences, null if none */
	public String getServerPassword();

	/**
	 * @param nick Nickname
	 * @return Password from preferences for nickname, or "" if none
	 */
	public String getNickPassword(String nick);

	/**
	 * @param identifyEvent An IDENTIFYEVENT_xx constant
	 * @return True if client should identify in that context
	 */
	public boolean shouldIdentify(String identifyEvent);

	/**
	 * @return Identify command from preferences
	 * @deprecated This is the old identify command, do not use
	 */
	public String getIdentifyCommand();

	/**
	 * @return Identify pattern from preferences, containing ${nick} and ${password}
	 */
	public String getIdentifyPattern();

	/** Identify event constant: on connect */
	public final static String IDENTIFYEVENT_CONNECT="connect";

	/** Identify event constant: on nick change */
	public final static String IDENTIFYEVENT_NICK="nick";

	/** Identify event constant: when services come online */
	public final static String IDENTIFYEVENT_SERVICES="services";

	/**
	 * Access information from the RPL_ISUPPORT (005) numeric. This is only available
	 * once that numeric has been received! Also see the various access methods in
	 * this class, which handle problem cases such as if the value wasn't supplied.
	 * @param parameter ISUPPORT parameter name
	 * @return ISUPPORT parameter value, or null if none
	 */
	public String getISupport(String parameter);

	/** @return String containing channel prefixes that are supported e.g. "#&" (from CHANTYPES) */
	public String getChanTypes();

	/** @return String containing status prefixes that are supported in channel notices e.g. "@+" (from STATUSMSG) */
	public String getStatusMsg();

	/** @return The max number of chan modes with a param inside one command (from MODES) */
	public int getMaxModeParams();

	/** @return Max length of a topic in bytes (from TOPICLEN) */
	public int getMaxTopicLength();

	/**
	 * @return Array of channel status prefixes that are shown in WHO, NAMES output
	 *    (from PREFIX). First prefix is 'highest' level, etc.
	 */
	public StatusPrefix[] getPrefix();

	/**
	 * @param prefix User's prefix
	 * @param required Required prefix
	 * @return True if user's prefix is better than or equal to the required one
	 */
	public boolean isPrefixAtLeast(char prefix,char required);

	/** Mapping from mode letter to status prefix symbol */
	public static class StatusPrefix
	{
		/**
		 * @param mode Mode letter
		 * @param prefix Prefix symbol
		 */
		public StatusPrefix(char mode,char prefix)
		{
			this.mode=mode;
			this.prefix=prefix;
		}
		private char mode,prefix;
		/** @return Mode letter */
		public char getMode() { return mode; }
		/** @return Prefix symbol */
		public char getPrefix() { return prefix; }
	}

	/** @return String listing all available chan modes (from CHANMODES and PREFIX) */
	public String getChanModes();

	/**
	 * @param mode Mode letter
	 * @return Type of given chan mode (from CHANMODES and PREFIX) as CHANMODE_xx
	 */
	public int getChanModeType(char mode);

	/** Unrecognised channel mode */
	public final static int CHANMODE_UNKNOWN=0;
	/**
	 * Channel mode is an 'address' mode that has a param whether + or - and can be
	 * sent from client without + or - to get a list of people affected. (type A)
	 */
	public final static int CHANMODE_ADDRESS=1;
	/**
	 * Channel mode always takes a parameter, whether + or -. (type B)
	 */
	public final static int CHANMODE_ALWAYSPARAM=2;
	/**
	 * Channel mode takes a parameter when + but not when - (type C)
	 */
	public final static int CHANMODE_SETPARAM=3;
	/**
	 * Channel mode never takes a parameter. (type D)
	 */
	public final static int CHANMODE_NOPARAM=4;
	/**
	 * Channel mode that applies to user status (from PREFIX).
	 */
	public final static int CHANMODE_USERSTATUS=5;

	/**
	 * @return Maximum number of modes that take a parameter per mode command
	 */
	public int getChanModeParamCount();

	/**
	 * @param c Class that owns property
	 * @param key Property key
	 * @return Value of property or null if unset
	 */
	public Object getProperty(Class<?> c, String key);

	/**
	 * @param c Class that owns property
	 * @param key Property key
	 * @param value New value for property
	 * @return Previous value of property or null if unset
	 */
	public Object setProperty(Class<?> c, String key, Object value);

	/**
	 * Attempt to silence the given mask.
	 * @param mask Mask to silence
	 * @return True if mask was silenced, false if server doesn't support silence
	 *   (or we've run out of slots)
	 */
	public boolean silence(String mask);

	/**
	 * Attempt to unsilence the given mask. It is OK to call this with masks that
	 * aren't actually silenced at the moment.
	 * @param mask Mask to silence
	 * @return True if mask was previously silenced and is no longer, false
	 *   if it wasn't silenced anyhow
	 */
	public boolean unsilence(String mask);

	/**
	 * @return True if currently marked away
	 */
	public boolean isAway();

	/**
	 * Prevents autojoin for the server. Should be called before the
	 * {@link ServerConnectionFinishedMsg} is sent, e.g. while handling
	 * {@link ServerConnectedMsg}.
	 */
	public void suppressAutoJoin();
}