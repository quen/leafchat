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

import java.util.Collection;

import leafchat.core.api.*;

/**
 * Numeric IRC message. This class also contain constants for a large set of
 * standard or standard-ish server numerics.
 */
public class NumericIRCMsg extends ServerIRCMsg
{
	/** Numeric */
  private int numeric;

  /**
   * If this message is similar to a 'defined' IRCMsg, we sometimes parse it
   * as that and throw that in here. Usually it's null.
   */
  private IRCMsg similar=null;

  /**
	 * @param sourceServer Server that sent the message
	 * @param target Target of notice
   * @param numeric Numeric (as integer)
   * @param similar If message is similar to a defined IRCMsg, we parse that here, otherwise null
   */
  public NumericIRCMsg(String sourceServer,String target,int numeric,IRCMsg similar)
  {
		super(sourceServer,target);
  		this.numeric=numeric;
  		this.similar=similar;
  }

  /**
   * If message is similar to a defined IRCMsg, that is returned here (to avoid
   * duplication of the parsing code).
   * <p>
   * <table><tr><th>Numeric</th><th>Type</th></tr>
   * <tr><td>RPL_CHANNELMODEIS</td><td>ChanModeIRCMsg</td></tr>
   * </table>
   * @return Message or null if none
   */
  public IRCMsg getSimilar()
  {
  		return similar;
  }

  /** @return Numeric */
  public int getNumeric() { return numeric; }

	@Override
	public String toString()
	{
		return super.toString()+
		  "  [NumericIRCMessage]\n"+
			"  Number: "+((1000+numeric)+"").substring(1)+"\n";
	}

  // Numeric constants from RFC1459

	/** RFC1459: ERR_NOSUCHNICK */
  public final static int	ERR_NOSUCHNICK = 401;
	/** RFC1459: ERR_NOSUCHSERVER */
	public final static int ERR_NOSUCHSERVER = 402;
	/** RFC1459: ERR_NOSUCHCHANNEL */
	public final static int ERR_NOSUCHCHANNEL = 403;
	/** RFC1459: ERR_CANNOTSENDTOCHAN */
	public final static int ERR_CANNOTSENDTOCHAN = 404;
	/** RFC1459: ERR_TOOMANYCHANNELS */
	public final static int ERR_TOOMANYCHANNELS = 405;
	/** RFC1459: ERR_WASNOSUCHNICK */
	public final static int ERR_WASNOSUCHNICK = 406;
	/** RFC1459: ERR_TOOMANYTARGETS */
	public final static int ERR_TOOMANYTARGETS = 407;
	/** RFC1459: ERR_NOORIGIN */
	public final static int ERR_NOORIGIN = 409;
	/** RFC1459: ERR_NORECIPIENT */
	public final static int ERR_NORECIPIENT = 411;
	/** RFC1459: ERR_NOTEXTTOSEND */
	public final static int ERR_NOTEXTTOSEND = 412;
	/** RFC1459: ERR_NOTOPLEVEL */
	public final static int ERR_NOTOPLEVEL = 413;
	/** RFC1459: ERR_WILDTOPLEVEL */
	public final static int ERR_WILDTOPLEVEL = 414;
	/** RFC1459: ERR_UNKNOWNCOMMAND */
	public final static int ERR_UNKNOWNCOMMAND = 421;
	/** RFC1459: ERR_NOMOTD */
	public final static int ERR_NOMOTD = 422;
	/** RFC1459: ERR_NOADMININFO */
	public final static int ERR_NOADMININFO = 423;
	/** RFC1459: ERR_FILEERROR */
	public final static int ERR_FILEERROR = 424;
	/** RFC1459: ERR_NONICKNAMEGIVEN */
	public final static int ERR_NONICKNAMEGIVEN = 431;
	/** RFC1459: ERR_ERRONEUSNICKNAME */
	public final static int ERR_ERRONEUSNICKNAME = 432;
	/** RFC1459: ERR_NICKNAMEINUSE */
	public final static int ERR_NICKNAMEINUSE = 433;
	/** RFC1459: ERR_NICKCOLLISION */
	public final static int ERR_NICKCOLLISION = 436;
	/** RFC1459: ERR_USERNOTINCHANNEL */
	public final static int ERR_USERNOTINCHANNEL = 441;
	/** RFC1459: ERR_NOTONCHANNEL */
	public final static int ERR_NOTONCHANNEL = 442;
	/** RFC1459: ERR_USERONCHANNEL */
	public final static int ERR_USERONCHANNEL = 443;
	/** RFC1459: ERR_NOLOGIN */
	public final static int ERR_NOLOGIN = 444;
	/** RFC1459: ERR_SUMMONDISABLED */
	public final static int ERR_SUMMONDISABLED = 445;
	/** RFC1459: ERR_USERSDISABLED */
	public final static int ERR_USERSDISABLED = 446;
	/** RFC1459: ERR_NOTREGISTERED */
	public final static int ERR_NOTREGISTERED = 451;
	/** RFC1459: ERR_NEEDMOREPARAMS */
	public final static int ERR_NEEDMOREPARAMS = 461;
	/** RFC1459: ERR_ALREADYREGISTRED */
	public final static int ERR_ALREADYREGISTRED = 462;
	/** RFC1459: ERR_NOPERMFORHOST */
	public final static int ERR_NOPERMFORHOST = 463;
	/** RFC1459: ERR_PASSWDMISMATCH */
	public final static int ERR_PASSWDMISMATCH = 464;
	/** RFC1459: ERR_YOUREBANNEDCREEP */
	public final static int ERR_YOUREBANNEDCREEP = 465;
	/** RFC1459: ERR_KEYSET */
	public final static int ERR_KEYSET = 467;
	/** RFC1459: ERR_CHANNELISFULL */
	public final static int ERR_CHANNELISFULL = 471;
	/** RFC1459: ERR_UNKNOWNMODE */
	public final static int ERR_UNKNOWNMODE = 472;
	/** RFC1459: ERR_INVITEONLYCHAN */
	public final static int ERR_INVITEONLYCHAN = 473;
	/** RFC1459: ERR_BANNEDFROMCHAN */
	public final static int ERR_BANNEDFROMCHAN = 474;
	/** RFC1459: ERR_BADCHANNELKEY */
	public final static int ERR_BADCHANNELKEY = 475;
	/** RFC1459: ERR_NOPRIVILEGES */
	public final static int ERR_NOPRIVILEGES = 481;
	/** RFC1459: ERR_CHANOPRIVSNEEDED */
	public final static int ERR_CHANOPRIVSNEEDED = 482;
	/** RFC1459: ERR_CANTKILLSERVER */
	public final static int ERR_CANTKILLSERVER = 483;
	/** RFC1459: ERR_NOOPERHOST */
	public final static int ERR_NOOPERHOST = 491;
	/** RFC1459: ERR_UMODEUNKNOWNFLAG */
	public final static int ERR_UMODEUNKNOWNFLAG = 501;
	/** RFC1459: ERR_USERSDONTMATCH */
	public final static int ERR_USERSDONTMATCH = 502;

	/** RFC1459: RPL_NONE */
	public final static int RPL_NONE = 300;
	/** RFC1459: RPL_USERHOST */
	public final static int RPL_USERHOST = 302;
	/** RFC1459: RPL_ISON */
	public final static int RPL_ISON = 303;
	/** RFC1459: RPL_AWAY */
	public final static int RPL_AWAY = 301;
	/** RFC1459: RPL_UNAWAY */
	public final static int RPL_UNAWAY = 305;
	/** RFC1459: RPL_NOWAWAY */
	public final static int RPL_NOWAWAY = 306;
	/** RFC1459: RPL_WHOISUSER */
	public final static int RPL_WHOISUSER = 311;
	/** RFC1459: RPL_WHOISSERVER */
	public final static int RPL_WHOISSERVER = 312;
	/** RFC1459: RPL_WHOISOPERATOR */
	public final static int RPL_WHOISOPERATOR = 313;
	/** RFC1459: RPL_WHOISIDLE */
	public final static int RPL_WHOISIDLE = 317;
	/** RFC1459: RPL_ENDOFWHOIS */
	public final static int RPL_ENDOFWHOIS = 318;
	/** RFC1459: RPL_WHOISCHANNELS */
	public final static int RPL_WHOISCHANNELS = 319;
	/** RFC1459: RPL_WHOWASUSER */
	public final static int RPL_WHOWASUSER = 314;
	/** RFC1459: RPL_ENDOFWHOWAS */
	public final static int RPL_ENDOFWHOWAS = 369;
	/** RFC1459: RPL_LISTSTART */
	public final static int RPL_LISTSTART = 321;
	/** RFC1459: RPL_LIST */
	public final static int RPL_LIST = 322;
	/** RFC1459: RPL_LISTEND */
	public final static int RPL_LISTEND = 323;
	/** RFC1459: RPL_CHANNELMODEIS */
	public final static int RPL_CHANNELMODEIS = 324;
	/** RFC1459: RPL_NOTOPIC */
	public final static int RPL_NOTOPIC = 331;
	/** RFC1459: RPL_TOPIC */
	public final static int RPL_TOPIC = 332;
	/** RFC1459: RPL_TOPICWHOTIME */
	public final static int RPL_TOPICWHOTIME = 333;
	/** RFC1459: RPL_INVITING */
	public final static int RPL_INVITING = 341;
	/** RFC1459: RPL_SUMMONING */
	public final static int RPL_SUMMONING = 342;
	/** RFC1459: RPL_VERSION */
	public final static int RPL_VERSION = 351;
	/** RFC1459: RPL_WHOREPLY */
	public final static int RPL_WHOREPLY = 352;
	/** RFC1459: RPL_ENDOFWHO */
	public final static int RPL_ENDOFWHO = 315;
	/** RFC1459: RPL_NAMREPLY */
	public final static int RPL_NAMREPLY = 353;
	/** RFC1459: RPL_ENDOFNAMES */
	public final static int RPL_ENDOFNAMES = 366;
	/** RFC1459: RPL_LINKS */
	public final static int RPL_LINKS = 364;
	/** RFC1459: RPL_ENDOFLINKS */
	public final static int RPL_ENDOFLINKS = 365;
	/** RFC1459: RPL_BANLIST */
	public final static int RPL_BANLIST = 367;
	/** RFC1459: RPL_ENDOFBANLIST */
	public final static int RPL_ENDOFBANLIST = 368;
	/** RFC1459: RPL_INFO */
	public final static int RPL_INFO = 371;
	/** RFC1459: RPL_ENDOFINFO */
	public final static int RPL_ENDOFINFO = 374;
	/** RFC1459: RPL_MOTDSTART */
	public final static int RPL_MOTDSTART = 375;
	/** RFC1459: RPL_MOTD */
	public final static int RPL_MOTD = 372;
	/** RFC1459: RPL_ENDOFMOTD */
	public final static int RPL_ENDOFMOTD = 376;
	/** RFC1459: RPL_YOUREOPER */
	public final static int RPL_YOUREOPER = 381;
	/** RFC1459: RPL_REHASHING */
	public final static int RPL_REHASHING = 382;
	/** RFC1459: RPL_TIME */
	public final static int RPL_TIME = 391;
	/** RFC1459: RPL_USERSSTART */
	public final static int RPL_USERSSTART = 392;
	/** RFC1459: RPL_USERS */
	public final static int RPL_USERS = 393;
	/** RFC1459: RPL_ENDOFUSERS */
	public final static int RPL_ENDOFUSERS = 394;
	/** RFC1459: RPL_NOUSERS */
	public final static int RPL_NOUSERS = 395;
	/** RFC1459: RPL_TRACELINK */
	public final static int RPL_TRACELINK = 200;
	/** RFC1459: RPL_TRACECONNECTING */
	public final static int RPL_TRACECONNECTING = 201;
	/** RFC1459: RPL_TRACEHANDSHAKE */
	public final static int RPL_TRACEHANDSHAKE = 202;
	/** RFC1459: RPL_TRACEUNKNOWN */
	public final static int RPL_TRACEUNKNOWN = 203;
	/** RFC1459: RPL_TRACEOPERATOR */
	public final static int RPL_TRACEOPERATOR = 204;
	/** RFC1459: RPL_TRACEUSER */
	public final static int RPL_TRACEUSER = 205;
	/** RFC1459: RPL_TRACESERVER */
	public final static int RPL_TRACESERVER = 206;
	/** RFC1459: RPL_TRACENEWTYPE */
	public final static int RPL_TRACENEWTYPE = 208;
	/** RFC1459: RPL_TRACELOG */
	public final static int RPL_TRACELOG = 261;
	/** RFC1459: RPL_STATSLINKINFO */
	public final static int RPL_STATSLINKINFO = 211;
	/** RFC1459: RPL_STATSCOMMANDS */
	public final static int RPL_STATSCOMMANDS = 212;
	/** RFC1459: RPL_STATSCLINE */
	public final static int RPL_STATSCLINE = 213;
	/** RFC1459: RPL_STATSNLINE */
	public final static int RPL_STATSNLINE = 214;
	/** RFC1459: RPL_STATSILINE */
	public final static int RPL_STATSILINE = 215;
	/** RFC1459: RPL_STATSKLINE */
	public final static int RPL_STATSKLINE = 216;
	/** RFC1459: RPL_STATSYLINE */
	public final static int RPL_STATSYLINE = 218;
	/** RFC1459: RPL_ENDOFSTATS */
	public final static int RPL_ENDOFSTATS = 219;
	/** RFC1459: RPL_STATSLLINE */
	public final static int RPL_STATSLLINE = 241;
	/** RFC1459: RPL_STATSUPTIME */
	public final static int RPL_STATSUPTIME = 242;
	/** RFC1459: RPL_STATSOLINE */
	public final static int RPL_STATSOLINE = 243;
	/** RFC1459: RPL_STATSHLINE */
	public final static int RPL_STATSHLINE = 244;
	/** RFC1459: RPL_UMODEIS */
	public final static int RPL_UMODEIS = 221;
	/** RFC1459: RPL_LUSERCLIENT */
	public final static int RPL_LUSERCLIENT = 251;
	/** RFC1459: RPL_LUSEROP */
	public final static int RPL_LUSEROP = 252;
	/** RFC1459: RPL_LUSERUNKNOWN */
	public final static int RPL_LUSERUNKNOWN = 253;
	/** RFC1459: RPL_LUSERCHANNELS */
	public final static int RPL_LUSERCHANNELS = 254;
	/** RFC1459: RPL_LUSERME */
	public final static int RPL_LUSERME = 255;
	/** RFC1459: RPL_ADMINME */
	public final static int RPL_ADMINME = 256;
	/** RFC1459: RPL_ADMINLOC1 */
	public final static int RPL_ADMINLOC1 = 2571;
	/** RFC1459: RPL_ADMINLOC2 */
	public final static int RPL_ADMINLOC2 = 2582;
	/** RFC1459: RPL_ADMINEMAIL */
	public final static int RPL_ADMINEMAIL = 259;

	// Extensions (some of which are used for different things on different networks!)

	/** Extension (various): RPL_WHOISREGNICK */
	public final static int RPL_WHOISREGNICK = 307;
	/** Extension (seen on SorceryNet): RPL_WHOISMASKED */
	public final static int RPL_WHOISMASKED = 550;
	/** Extension (seen in Unreal 3.2): RPL_WHOISHOST */
	public final static int RPL_WHOISHOST = 378;
	/** Extension (seen in Unreal 3.2): RPL_CREATIONTIME */
	public final static int RPL_CREATIONTIME = 329;
	/** Extension (seen in Bahamut 1.8): RPL_GLOBALUSERS */
	public final static int RPL_GLOBALUSERS = 266;
	/** Extension (seen in Bahamut 1.8): RPL_LOCALUSERS */
	public final static int RPL_LOCALUSERS = 265;
	/** Extension (seen on SorceryNet): RPL_STATSCONN */
	public final static int RPL_STATSCONN = 250;
	/** Extension (seen in Bahamut 1.8): RPL_LOGON */
	public final static int RPL_LOGON = 600;
	/** Extension (seen in Bahamut 1.8): RPL_LOGOFF */
	public final static int RPL_LOGOFF = 601;
	/** Extension (seen in Bahamut 1.8): RPL_WATCHOFF */
	public final static int RPL_WATCHOFF = 602;
	/** Extension (seen in Bahamut 1.8): RPL_WATCHSTAT */
	public final static int RPL_WATCHSTAT = 603;
	/** Extension (seen in Bahamut 1.8): RPL_NOWON */
	public final static int RPL_NOWON = 604;
	/** Extension (seen in Bahamut 1.8): RPL_NOWOFF */
	public final static int RPL_NOWOFF = 605;

  // Connection numerics as defined in RFC2812 http://www.faqs.org/rfcs/rfc2812.html

  /** RFC2812: RPL_WELCOME */
	public final static int RPL_WELCOME = 1;
  /** RFC2812: RPL_YOURHOST */
	public final static int RPL_YOURHOST = 2;
  /** RFC2812: RPL_CREATED */
	public final static int RPL_CREATED = 3;
  /** RFC2812: RPL_MYINFO */
	public final static int RPL_MYINFO = 4;

  // ISUPPORT numeric defined in http://www.irc.org/tech_docs/draft-brocklesby-irc-isupport-03.txt

  /** Extension: RPL_ISUPPORT */
	public final static int RPL_ISUPPORT = 5;

	/** Scripting event info */
	public static MessageInfo info=new MessageInfo(NumericIRCMsg.class,"Numeric",
		"<para>Events sent by the server with three-digit numeric codes.</para>" +
		"<small>See RFC1459 and updated variants for information about most codes. " +
		"Note that most server software defines extra numerics of its own.</small>")
	{
		@Override
		protected void listAppropriateFilters(Collection<FilterInfo> list)
		{
			super.listAppropriateFilters(list);
			list.add(NumericFilter.info);
		}
		@Override
		protected void listScriptingVariables(Variables v)
		{
			super.listScriptingVariables(v);
			v.add("numeric");
			v.add("params",String.class,
				"String params=\"\"; " +
				"{ " +
					"byte[][] allParams=msg.getParams(); " +
					"for(int i=1;i<allParams.length;i++) " +
					"{ " +
						"if(i!=1) params+=' '; " +
						"params+=IRCMsg.convertISO(allParams[i]); " +
					"} " +
				"}");
		}
	};
}
