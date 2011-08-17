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
package com.leafdigital.irc;

import java.util.LinkedList;
import java.util.regex.*;

/** Fixes IRC colours. */
class ColourFixer
{
	private boolean bold,underline;
	private int background = 0, foreground = 1;
	private StringBuffer out = new StringBuffer();
	private LinkedList<String> tags = null;

	private static final Pattern COLOURS=Pattern.compile(
		"^([0-9][0-9]?)(,([0-9][0-9]?))?.*$");

	/** @return Resulting text */
	String get()
	{
		return out.toString();
	}

	private final static Pattern CONTROLCHAR_REGEX = Pattern.compile(
		"<controlchar num='([0-9]+)'/>");

	/**
	 * @param s Input string
	 * @param allowStyles True if styles are allowed
	 * @param allowColours True if colours are allowed
	 */
	ColourFixer(String s, boolean allowStyles, boolean allowColours)
	{
		// Turn XML control characters back into real control characters
		Matcher m = CONTROLCHAR_REGEX.matcher(s);
		StringBuffer controls = new StringBuffer();
		while(m.find())
		{
			String replace = "" + (char)Integer.parseInt(m.group(1));
			m.appendReplacement(controls, replace);
		}
		m.appendTail(controls);
		s = controls.toString();

		for(int i=0;i<s.length();i++)
		{
			char c=s.charAt(i);
			switch(c)
			{
			case 2: // Bold (toggle)
				if(allowStyles)
				{
					if(!bold)
					{
						add("irc-b");
						bold=true;
					}
					else
					{
						remove("irc-b");
						bold=false;
					}
				}
				break;

			case 15: // Normal
				if(allowStyles)
				{
					bold=false;
					underline=false;
					background=0;
					foreground=1;
					removeAll();
				}
				break;

			case 18: // Reverse (toggle)
				if(allowStyles)
				{
					setColour(background,foreground);
				}
				break;

			case 31: // Underline (toggle)
				if(allowStyles)
				{
					if(!underline)
					{
						add("irc-u");
						underline=true;
					}
					else
					{
						remove("irc-u");
						underline=false;
					}
				}
				break;

			case 3: // Colour
				// Parse colour...
				m = COLOURS.matcher(s.substring(i+1));
				if(m.matches())
				{
					int
					  fg=Integer.parseInt(m.group(1)	),
					  bg=m.group(3)==null ? background : Integer.parseInt(m.group(3));
					if(fg>15 || bg>15) break;
					if(allowColours) setColour(fg,bg);
					i += m.group(1).length() + (m.group(2)!=null ? m.group(2).length() : 0);
				}
				else
				{
					// Switches colour off
					setColour(1, 0);
				}
				break;

			case '<':
				removeAll();
				out.append('<');
				break;

			default:
				// Throw away all other control characters
				if(c>=32)
				{
					out.append(c);
				}
				break;
			}
		}
		removeAll();
	}

	private void setColour(int fg, int bg)
	{
		if(bg != background)
		{
			if(background != 0)
			{
				remove("irc-bg" + background);
			}
			background = bg;
			if(background != 0)
			{
				add("irc-bg" + background);
			}
		}
		if(fg != foreground)
		{
			if(foreground != 1)
			{
				remove("irc-fg" + foreground);
			}
			foreground = fg;
			if(foreground != 1)
			{
				add("irc-fg" + foreground);
			}
		}
	}

	private void add(String tag)
	{
		// Only create list if needed
		if(tags==null)
		{
			tags = new LinkedList<String>();
		}
		out.append("<" + tag + ">");
		tags.addLast(tag);
	}

	private void remove(String tag)
	{
		if(tags==null || tags.isEmpty())
		{
			return;
		}
		String last = tags.removeLast();
		out.append("</" + last + ">");
		if(last.equals(tag))
		{
			return;
		}
		remove(tag); // Recurse
		tags.addLast(last);
		out.append("<" + last + ">");
	}

	private void removeAll()
	{
		if(tags==null)
		{
			return;
		}
		while(!tags.isEmpty())
		{
			out.append("</" + tags.removeLast() + ">");
		}
	}
}