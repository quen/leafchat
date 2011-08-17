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
package com.leafdigital.audio.api;

import java.io.*;

import leafchat.core.api.*;

/**
 * Singleton service for playing audio clips.
 */
public interface Audio extends Singleton
{
	/**
	 * Plays an audio file from the system or user 'sounds' folder, which must
	 * be in .ogg format.
	 * @param oggName File to play (name only, e.g. "frogs" if you want to play
	 *   "frogs.ogg" in one of those folders)
	 * @throws GeneralException Any error
	 */
	public void play(String oggName) throws GeneralException;

	/**
	 * Plays the given audio file, which must be in .ogg format.
	 * @param ogg File to play
	 * @throws GeneralException Any error
	 */
	public void play(File ogg) throws GeneralException;

	/**
	 * Plays audio from the given InputStream, which must be in .ogg format.
	 * @param oggStream Stream to play
	 * @throws GeneralException Any error
	 */
	public void play(InputStream oggStream) throws GeneralException;

	/**
	 * Obtains a list of all available sounds in system/user folders.
	 * @return List of sound names suitable for calling {@link #play(String)}
	 * @throws GeneralException Any error
	 */
	public String[] getSounds() throws GeneralException;

	/**
	 * Returns true if a sound with the given name currently exists.
	 * @param name Name
	 * @return True if it exists
	 */
	public boolean soundExists(String name);
}
