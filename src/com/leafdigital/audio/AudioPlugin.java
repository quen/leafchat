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
package com.leafdigital.audio;

import java.io.*;
import java.util.*;

import javax.sound.sampled.*;

import org.tritonus.sampled.convert.jorbis.JorbisFormatConversionProvider;
import org.tritonus.sampled.file.jorbis.JorbisAudioFileReader;

import util.*;
import util.xml.XML;

import com.leafdigital.audio.api.Audio;
import com.leafdigital.irc.api.*;
import com.leafdigital.prefsui.api.PreferencesUI;

import leafchat.core.api.*;

/**
 * Plugin for audio playback.
 */
public class AudioPlugin implements Plugin, Audio
{
	private final static String SOUNDS_FOLDER = "sounds";

	private int threads = 0;
	private Object threadSynch = new Object();
	private boolean close = false;

	@Override
	public synchronized void init(
		PluginContext context, PluginLoadReporter reporter) throws GeneralException
	{
		// Become a singleton
		context.registerSingleton(Audio.class, this);

		// Listen for /sound command
		context.requestMessages(UserCommandMsg.class, this, Msg.PRIORITY_NORMAL);
		context.requestMessages(UserCommandListMsg.class, this, Msg.PRIORITY_NORMAL);

		// Register prefs page
		PreferencesUI preferencesUI =
			context.getSingle(PreferencesUI.class);
		preferencesUI.registerPage(this,(new SoundsPage(context)).getPage());
	}

	@Override
	public String toString()
	{
	  // Used to display in system log etc.
		return "Audio plugin";
	}

	@Override
	public void close() throws GeneralException
	{
		synchronized(threadSynch)
		{
			close = true;
			while(threads > 0)
			{
				try
				{
					threadSynch.wait();
				}
				catch(InterruptedException e)
				{
					throw new GeneralException("Close interrupted");
				}
			}
		}
	}

	/**
	 * Message: User types command.
	 * @param msg Message
	 * @throws GeneralException Any error
	 */
  public void msg(UserCommandMsg msg) throws GeneralException
  {
		if(msg.isHandled())
		{
			return;
		}
		String command = msg.getCommand();

		if("sound".equals(command))
		{
			sound(msg);
		}
  }

	/**
	 * Message: Listing available commands.
	 * @param msg Message
	 */
	public void msg(UserCommandListMsg msg)
	{
		msg.addCommand(false, "sound", UserCommandListMsg.FREQ_OBSCURE,
			"/sound <name>", "<key>Scripting:</key> Plays the named system sound (see Options/Sounds for list)");
	}

  private void sound(UserCommandMsg msg) throws GeneralException
  {
  	msg.markHandled();

  	// Get sound name
		String sound = msg.getParams().trim();
		if(sound.length() == 0)
		{
			msg.getMessageDisplay().showError(
				"Syntax: /sound &lt;sound name or full file path>");
			return;
		}

		File file;

		// Check if they're referring to a full path
		if(sound.contains("/") || sound.contains("\\"))
		{
			if(!sound.endsWith(".ogg"))
			{
				msg.getMessageDisplay().showError(
					"Sound file <key>" + XML.esc(sound) + "</key> must have .ogg "
					+ "extension and be in Ogg Vorbis format");
				return;
			}

			file = new File(sound);
			if(!file.exists())
			{
				msg.getMessageDisplay().showError(
					"Sound file <key>" + XML.esc(sound) + "</key> not found");
				return;
			}
		}
		else
		{
			// Not a full path, let's see if we can find it in the sounds folder
			file = findFile(sound);
			if(file == null)
			{
				msg.getMessageDisplay().showError(
					"Sound <key>" + XML.esc(sound) + "</key> not found; "
					+ "use the filename within leafChat's sound folder, without a "
					+ "path and without the .ogg extension");
				return;
			}
		}

		play(file);
  }

  /**
   * Gets file in either system or user folder.
   * @param name Name of file not including extension
   * @return File or null if not found
   */
  private File findFile(String name)
  {
		File possible = new File(getSoundsFolder(false), name + ".ogg");
		if(possible.exists())
		{
			return possible;
		}
		possible = new File(getSoundsFolder(true), name + ".ogg");
		if(possible.exists())
		{
			return possible;
		}
		return null;
  }

	/**
	 * @param system If true, returns system sounds folder, otherwise user folder
	 * @return Sounds folder
	 */
	public File getSoundsFolder(boolean system)
	{
		if(system)
		{
			return new File(SOUNDS_FOLDER);
		}
		else
		{
			File userSounds = new File(PlatformUtils.getUserFolder(), SOUNDS_FOLDER);
			if(!userSounds.exists())
			{
				userSounds.mkdirs();
			}
			return userSounds;
		}
	}

	@Override
	public void play(String oggName) throws GeneralException
	{
		File file = findFile(oggName);
		if(file == null)
		{
			throw new GeneralException("Unknown audio file " + oggName);
		}
		play(file);
	}

	@Override
	public void play(File ogg) throws GeneralException
	{
		try
		{
			play(new FileInputStream(ogg));
		}
		catch(IOException e)
		{
			throw new GeneralException("Failed to play audio file " + ogg, e);
		}
	}

	@Override
	public void play(InputStream oggStream) throws GeneralException
	{
		try
		{
			// NOTE: References to Jorbis libraries are hard-coded because the
			// service interface for some reason does not work in the leafChat
			// classloader architecture, so I can't just rely on the SPI existing.

			// Get input as audio stream and get format details
			JorbisAudioFileReader reader = new JorbisAudioFileReader();
			AudioInputStream audioStream = reader.getAudioInputStream(oggStream);
			AudioFormat format = audioStream.getFormat();

			// Convert to signed 16-bit little-endian PCM at the same sample rate
			AudioFormat targetFormat = new AudioFormat(
				AudioFormat.Encoding.PCM_SIGNED,
				format.getSampleRate(), 16, format.getChannels(),
				format.getChannels() * 2, format.getSampleRate(), false);

			// Get converting stream
			JorbisFormatConversionProvider provider = new JorbisFormatConversionProvider();
			AudioInputStream pcmStream = provider.getAudioInputStream(targetFormat, audioStream);

			// Get source data line, open and start it
			DataLine.Info lineInfo = new DataLine.Info(
				SourceDataLine.class, targetFormat, 8192);
			SourceDataLine line = (SourceDataLine)AudioSystem.getLine(lineInfo);
			line.open(targetFormat, 8192);
			line.start();

			// Now start a thread for playback
			synchronized(threadSynch)
			{
				new AudioPlayerThread(line, pcmStream);
				threads++;
			}
		}
		catch(IOException e)
		{
			throw new GeneralException("Error reading audio stream", e);
		}
		catch(UnsupportedAudioFileException e)
		{
			throw new GeneralException("Audio format not supported", e);
		}
		catch(LineUnavailableException e)
		{
			throw new GeneralException("Audio playback line not available", e);
		}
	}

	/**
	 * Thread that handles audio playback.
	 */
	private class AudioPlayerThread extends Thread
	{
		private SourceDataLine line;
		private AudioInputStream stream;

		private AudioPlayerThread(SourceDataLine line, AudioInputStream stream)
		{
			super("Audio player thread");
			this.line = line;
			this.stream = stream;
			start();
		}

		@Override
		public void run()
		{
			try
			{
				byte[] buffer = new byte[1024];
				while(true)
				{
					int read = stream.read(buffer);
					if(read == -1)
					{
						break;
					}
					synchronized(threadSynch)
					{
						if(close)
						{
							return;
						}
					}

					line.write(buffer, 0, read);
					synchronized(threadSynch)
					{
						if(close)
						{
							return;
						}
					}
				}

				line.drain();
			}
			catch(Throwable e)
			{
				ErrorMsg.report("Error playing audio stream", e);
			}
			finally
			{
				synchronized(threadSynch)
				{
					threads--;
					threadSynch.notifyAll();
				}
				line.close();
			}
		}
	}

	@Override
	public String[] getSounds() throws GeneralException
	{
		LinkedList<String> sounds = new LinkedList<String>();
		fillSoundList(sounds, true);
		fillSoundList(sounds, false);
		Collections.sort(sounds);
		return sounds.toArray(new String[sounds.size()]);
	}

	/**
	 * @param sounds List to fill with sounds
	 * @param system True to use system folder
	 */
	private void fillSoundList(LinkedList<String> sounds, boolean system)
	{
		File[] files = IOUtils.listFiles(getSoundsFolder(system));
		for(int i=0; i<files.length; i++)
		{
			String name = files[i].getName();
			if(files[i].isFile() && name.endsWith(".ogg"))
			{
				sounds.add(name.substring(0, name.length()-4));
			}
		}
	}

	@Override
	public boolean soundExists(String name)
	{
		return findFile(name) != null;
	}
}
