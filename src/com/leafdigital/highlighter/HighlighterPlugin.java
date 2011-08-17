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
package com.leafdigital.highlighter;

import java.util.*;
import java.util.regex.Pattern;

import org.w3c.dom.*;

import util.xml.*;

import com.leafdigital.audio.api.Audio;
import com.leafdigital.highlighter.api.Highlighter;
import com.leafdigital.prefs.api.*;
import com.leafdigital.prefsui.api.PreferencesUI;

import leafchat.core.api.*;

/**
 * Plugin for handling text highlighting.
 */
public class HighlighterPlugin implements Plugin, Highlighter
{
	/**
	 * Preference for words within highlighter anon children.
	 */
	public final static String PREF_WORD = "word";
	/**
	 * Preference for whether nickname is highlighted.
	 */
	public final static String PREF_HIGHLIGHT_NICKNAME = "highlight-nickname";
	/**
	 * Default nickname highlight (on).
	 */
	public final static String PREFDEFAULT_HIGHLIGHT_NICKNAME="t";
	/**
	 * Preference for highlight sound ("" = none)
	 */
	public final static String PREF_HIGHLIGHT_SOUND = "highlight-sound";
	/**
	 * Default value for highlight sound ("")
	 */
	public final static String PREFDEFAULT_HIGHLIGHT_SOUND = "";
	/**
	 * Preference for whether highlight sound should be restricted (not played
	 * more than once per minute).
	 */
	public final static String PREF_HIGHLIGHT_RESTRICTSOUND = "highlight-restrict-sound";
	/**
	 * Default restrict value (on)
	 */
	public final static String PREFDEFAULT_HIGHLIGHT_RESTRICTSOUND = "t";

	private final static long RESTRICT_TIME = 60000L;

	private final static Set<String> STOP_ELEMENTS = new HashSet<String>(Arrays.asList(
		new String[] { "nick", "server", "chan", "owntext"}));

	private PluginContext context;
	private long lastSound;

	@Override
	public synchronized void init(
		PluginContext context, PluginLoadReporter reporter) throws GeneralException
	{
		this.context=context;

		// Become a singleton
		context.registerSingleton(Highlighter.class, this);

		// Register prefs page
		PreferencesUI preferencesUI =
			context.getSingleton2(PreferencesUI.class);
		preferencesUI.registerPage(this,(new HighlighterPage(context)).getPage());
	}

	@Override
	public String toString()
	{
	  // Used to display in system log etc.
		return "Highlighter plugin";
	}

	@Override
	public String highlight(String currentNickname, String xml) throws XMLException
	{
		// Get data from prefs
		Preferences prefs = context.getSingleton2(Preferences.class);
		PreferencesGroup group = prefs.getGroup(this);
		PreferencesGroup[] anon = group.getAnon();
		boolean includeNickname = currentNickname != null
			&& prefs.toBoolean(group.get(PREF_HIGHLIGHT_NICKNAME,
				PREFDEFAULT_HIGHLIGHT_NICKNAME));

		// Build list of highlight words in regular expression format
		StringBuffer words = new StringBuffer();
		for(int i=0; i<anon.length; i++)
		{
			if(i != 0)
			{
				words.append('|');
			}
			words.append(Pattern.quote(anon[i].get(PREF_WORD)));
		}
		if(includeNickname)
		{
			if(anon.length > 0)
			{
				words.append('|');
			}
			words.append(Pattern.quote(currentNickname));
		}

		// Don't do anything if there are no words to find
		if(words.length() == 0)
		{
			return xml;
		}

		// Parse document and apply highlight
		Document d = XML.parse("<o>" + xml.replaceAll("xx", "xx ") + "</o>");
		String patternString = "(^|\\W)(" + words + ")(\\W|$)";
		Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
		boolean changed = highlight(d.getDocumentElement(), pattern);
		if(!changed)
		{
			return xml;
		}

		// Play sound if it's enabled
		String sound = group.get(PREF_HIGHLIGHT_SOUND, PREFDEFAULT_HIGHLIGHT_SOUND);
		if(!sound.equals(""))
		{
			boolean restrict = prefs.toBoolean(group.get(PREF_HIGHLIGHT_RESTRICTSOUND,
				PREFDEFAULT_HIGHLIGHT_RESTRICTSOUND));
			long now = System.currentTimeMillis();
			if(!restrict || (now - lastSound > RESTRICT_TIME))
			{
				Audio audio = context.getSingleton2(Audio.class);
				if(audio.soundExists(sound))
				{
					lastSound = now;
					try
					{
						audio.play(sound);
					}
					catch(GeneralException e)
					{
						ErrorMsg.report("Error playing highlight sound", e);
					}
				}
				else
				{
					// If the sound doesn't exist any more, turn off the pref
					group.unset(PREF_HIGHLIGHT_SOUND);
				}
			}
		}

		// Save result string and chop outer tag
		String result = XML.saveString(d);
		result = result.replaceAll("xxHIGHLIGHT", "<highlight>").replaceAll(
			"xx/HIGHLIGHT", "</highlight>").replaceAll("xx ", "xx");
		return result.substring(3, result.length()-4);
	}

	private boolean highlight(Element parent, Pattern pattern)
	{
		boolean result = false;
		NodeList nl = parent.getChildNodes();
		for(int i=0; i<nl.getLength(); i++)
		{
			Node n = nl.item(i);
			if(n instanceof Element)
			{
				Element e = (Element)n;
				if(STOP_ELEMENTS.contains(e.getTagName()))
				{
					continue;
				}
				result = result || highlight(e, pattern);
			}
			if(n instanceof Text)
			{
				Text t = (Text)n;
				String text = t.getData();
				String before = text;
				text = pattern.matcher(text).replaceAll("$1xxHIGHLIGHT$2xx/HIGHLIGHT$3");
				if(!before.equals(text))
				{
					result = true;
					t.setData(text);
				}
			}
		}
		return result;
	}

	@Override
	public void close() throws GeneralException
	{
	}
}
