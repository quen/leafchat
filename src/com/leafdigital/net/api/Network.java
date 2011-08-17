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
package com.leafdigital.net.api;

import java.io.IOException;
import java.net.*;

import leafchat.core.api.Singleton;

/**
 * Network interface provides low-level networking features.
 */
public interface Network extends Singleton
{
	/**
	 * SSL is not enabled for this connection.
	 */
	public final static int SECURE_NONE = 0;
	/**
	 * SSL is optional for this connection; try SSL first, if it fails then
	 * try a normal connection.
	 */
	public final static int SECURE_OPTIONAL = 1;
	/**
	 * SSL is required for this connection.
	 */
	public final static int SECURE_REQUIRED = 2;

	/**
	 * Connect to the given address using an insecure (non-SSL) connection
	 * and return a connected socket.
	 * @param host Host name or IP string
	 * @param port Port number
	 * @param timeout Timeout in milliseconds
	 * @return Connected socket
	 * @throws IOException
	 */
	public Socket connect(String host,int port,int timeout) throws IOException;

	/**
	 * Connect to the given address and return a connected socket.
	 * @param host Host name or IP string
	 * @param port Port number
	 * @param timeout Timeout in milliseconds
	 * @param secureMode SECURE_xx constant
	 * @return Connected socket
	 * @throws IOException
	 */
	public Socket connect(String host, int port, int timeout, int secureMode)
		throws IOException;

	/** @return True if the version of listen() with a target must be called */
	public boolean needsListenTarget();

	/**
	 * Obtains the current public address. Depending on connection settings and
	 * whether the user has connected to anything yet or not, this may very
	 * possibly be unknown and should not be relied on. The Port object, obtained
	 * when listening, always returns a valid public address and should be used
	 * instead of this for all practical purposes; this is just for display.
	 * @return The user's public address or null if not known
	 */
	public InetAddress getPublicAddress();

	/**
	 * Open a public TCP port to listen for connections.
	 * @return Port ready for accept() calls
	 * @throws IOException If there are any problems creating the port
	 */
	public Port listen() throws IOException;

	/**
	 * Report a possible public address that has been detected from another
	 * system (i.e. report from server). Should only be called if we are fairly
	 * confident about the address.
	 * @param ia Possible public address (will be ignored if it isn't)
	 */
	public void reportPublicAddress(InetAddress ia);

	/**
	 * Open a public TCP port to listen for connections from a specific
	 * host. (Must be called in some cases depending on user network settings.)
	 * Note that this does not necessarily provide a security feature; in some
	 * cases, the remote host will not be checked before being accepted.
	 * @param remoteHost Remote host address that will connect
	 * @return Port ready for accept() calls. Note that this version of the function
	 *   returns a version which may only be accept()ed once.
	 * @throws IOException If there are any problems creating the port
	 */
	public Port listen(String remoteHost) throws IOException;

	/**
	 * A listening port.
	 */
	public interface Port
	{
		/**
		 * Waits infinitely for a connection then returns it.
		 * @return Socket connection
		 * @throws IOException
		 */
		public Socket accept() throws IOException;

		/**
		 * Waits for a connection then returns it, or throws SocketTimeoutException.
		 * @param timeout Timeout in milliseconds
		 * @return Socket connection
		 * @throws IOException
		 */
		public Socket accept(int timeout) throws IOException;

		/**
		 * Closes the port, cancelling any in-progress accept calls
		 * @throws IOException Any error
		 */
		public void close() throws IOException;

		/** @return Public IP address for receiving connections */
		public InetAddress getPublicAddress();

		/** @return Public port number	 */
		public int getPublicPort();
	}
}
