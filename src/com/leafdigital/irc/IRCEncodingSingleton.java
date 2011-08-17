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

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import com.leafdigital.irc.api.*;
import com.leafdigital.prefs.api.*;

import leafchat.core.api.*;

/** Singleton for encoding/decoding IRC messages. */
public class IRCEncodingSingleton implements IRCEncoding
{
	static class EncodingInfoImp implements EncodingInfo
	{
		private String encoding;
		private String outgoing;
		private boolean utf8;

		public EncodingInfoImp(String encoding,boolean utf8,String outgoing)
		{
			this.encoding=encoding;
			this.utf8=utf8;
			this.outgoing=outgoing;
		}

		@Override
		public String getEncoding()
		{
			return encoding;
		}

		@Override
		public String getOutgoing()
		{
			return outgoing;
		}

		@Override
		public boolean isUTF8()
		{
			return utf8;
		}

		@Override
		public String convertIncoming(byte[] data)
		{
			try
			{
				if(utf8)
				{
					String s=new String(data,"UTF-8");
					if(Arrays.equals(s.getBytes("UTF-8"),data)) return s;
				}
				return new String(data,encoding);
			}
			catch(UnsupportedEncodingException e)
			{
				throw new Error("Missing expected character encoding",e);
			}
		}

		@Override
		public byte[] convertOutgoing(String text)
		{
			try
			{
				// Convert text and check it's OK
				byte[] converted=text.getBytes(outgoing);
				if(text.equals(new String(converted,outgoing)))
					return converted;

				// Not OK? Use UTF-8
				return text.getBytes("UTF-8");
			}
			catch(UnsupportedEncodingException e)
			{
				throw new BugException("Couldn't find expected character encoding",e);
			}
		}
	}

	private PluginContext pc;

	IRCEncodingSingleton(PluginContext pc)
	{
		this.pc=pc;
	}

	@Override
	public EncodingInfo getEncoding(Server s,String chan,IRCUserAddress user)
	{
		Preferences p=pc.getSingleton2(Preferences.class);
		PreferencesGroup encoding=p.getGroup(pc.getPlugin()).getChild(IRCPrefs.PREFGROUP_ENCODING);

		// User address takes priority
		if(user!=null)
		{
			PreferencesGroup[] users=encoding.getChild(IRCPrefs.PREFGROUP_BYUSER).getAnon();
			for(int i=0;i<users.length;i++)
			{
				if(user.matches(new IRCUserAddress(users[i].get(IRCPrefs.PREF_BYUSER_MASK),true)))
				{
					return new EncodingInfoImp(
						users[i].get(IRCPrefs.PREF_BYUSER_ENCODING),
						p.toBoolean(users[i].get(IRCPrefs.PREF_BYUSER_UTF8)),
						users[i].get(IRCPrefs.PREF_BYUSER_OUTGOING));
				}
			}
		}

		// Then channel
		if(chan!=null)
		{
			PreferencesGroup[] chans=encoding.getChild(IRCPrefs.PREFGROUP_BYCHAN).getAnon();
			for(int i=0;i<chans.length;i++)
			{
				if(chan.equalsIgnoreCase(chans[i].get(IRCPrefs.PREF_BYCHAN_NAME)))
				{
					return new EncodingInfoImp(
						chans[i].get(IRCPrefs.PREF_BYCHAN_ENCODING),
						p.toBoolean(chans[i].get(IRCPrefs.PREF_BYCHAN_UTF8)),
						chans[i].get(IRCPrefs.PREF_BYCHAN_OUTGOING));
				}
			}
		}

		// Then server
		if(s!=null)
		{
			String serverEncoding=
				s.getPreferences().getAnonHierarchical(IRCPrefs.PREF_ENCODING,null);
			if(serverEncoding!=null)
			{
				return new EncodingInfoImp(serverEncoding,
					p.toBoolean(	s.getPreferences().getAnonHierarchical(IRCPrefs.PREF_UTF8)),
					s.getPreferences().getAnonHierarchical(IRCPrefs.PREF_OUTGOING));
			}
		}

		// Then global default
		return new EncodingInfoImp(
			encoding.get(IRCPrefs.PREF_ENCODING,IRCPrefs.PREFDEFAULT_ENCODING),
			p.toBoolean(encoding.get(IRCPrefs.PREF_UTF8,IRCPrefs.PREFDEFAULT_UTF8)),
			encoding.get(IRCPrefs.PREF_OUTGOING,IRCPrefs.PREFDEFAULT_OUTGOING));
	}

	@Override
	public String processEscapes(String input, boolean allowStyles, boolean allowColours)
	{
		return (new ColourFixer(input, allowStyles, allowColours)).get();
	}
}
