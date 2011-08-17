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
package com.leafdigital.net;

import java.io.IOException;
import java.net.*;

import com.leafdigital.net.api.Network;

class ServerSocketWrapper implements Network.Port
{
	private ServerSocket socket;
	private InetAddress publicAddress;
	private int publicPort;

	ServerSocketWrapper(ServerSocket socket,InetAddress publicAddress,int publicPort)
	{
		this.socket=socket;
		this.publicAddress=publicAddress;
		this.publicPort=publicPort;
	}

	@Override
	public Socket accept() throws IOException
	{
		return socket.accept();
	}

	@Override
	public Socket accept(int timeout) throws IOException
	{
		socket.setSoTimeout(timeout);
		return socket.accept();
	}

	@Override
	public void close() throws IOException
	{
		socket.close();
	}

	@Override
	public InetAddress getPublicAddress()
	{
		return publicAddress;
	}

	@Override
	public int getPublicPort()
	{
		return publicPort;
	}
}