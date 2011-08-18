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

import java.util.*;

import com.leafdigital.irc.api.*;

import leafchat.core.api.*;

/** Parses incoming IRC text into messages */
public class IRCMessageParser implements MsgOwner
{
	/** Message dispatcher */
	private MessageDispatch mdp;

	private PluginContext context;

	/**
	 * @param context Context used to register events.
	 * @throws GeneralException Any error registering messages
	 */
	public IRCMessageParser(PluginContext context) throws GeneralException
	{
		this.context = context;
		context.registerMessageOwner(this);
		context.registerExtraMessageClass(UserSourceIRCMsg.class);
		context.registerExtraMessageClass(UserIRCMsg.class);
		context.registerExtraMessageClass(ChanIRCMsg.class);
		context.registerExtraMessageClass(UserNoticeIRCMsg.class);
		context.registerExtraMessageClass(UserModeIRCMsg.class);
		context.registerExtraMessageClass(UserMessageIRCMsg.class);
		context.registerExtraMessageClass(UserCTCPResponseIRCMsg.class);
		context.registerExtraMessageClass(UserCTCPRequestIRCMsg.class);
		context.registerExtraMessageClass(UserActionIRCMsg.class);
		context.registerExtraMessageClass(UnknownIRCMsg.class);
		context.registerExtraMessageClass(TopicIRCMsg.class);
		context.registerExtraMessageClass(SilenceIRCMsg.class);
		context.registerExtraMessageClass(ServerIRCMsg.class);
		context.registerExtraMessageClass(ServerSendMsg.class);
		context.registerExtraMessageClass(ServerRearrangeMsg.class);
		context.registerExtraMessageClass(ServerNoticeIRCMsg.class);
		context.registerExtraMessageClass(ServerLineMsg.class);
		context.registerExtraMessageClass(ServerDisconnectedMsg.class);
		context.registerExtraMessageClass(ServerConnectionFinishedMsg.class);
		context.registerExtraMessageClass(ServerConnectedMsg.class);
		context.registerExtraMessageClass(QuitIRCMsg.class);
		context.registerExtraMessageClass(PingIRCMsg.class);
		context.registerExtraMessageClass(PartIRCMsg.class);
		context.registerExtraMessageClass(NumericIRCMsg.class);
		context.registerExtraMessageClass(NickIRCMsg.class);
		context.registerExtraMessageClass(KickIRCMsg.class);
		context.registerExtraMessageClass(JoinIRCMsg.class);
		context.registerExtraMessageClass(InviteIRCMsg.class);
		context.registerExtraMessageClass(ErrorIRCMsg.class);
		context.registerExtraMessageClass(ChanNoticeIRCMsg.class);
		context.registerExtraMessageClass(ChanModeIRCMsg.class);
		context.registerExtraMessageClass(ChanMessageIRCMsg.class);
		context.registerExtraMessageClass(ChanCTCPRequestIRCMsg.class);
		context.registerExtraMessageClass(ChanActionIRCMsg.class);

		context.requestMessages(ServerLineMsg.class, this);
	}

	private static int find(byte b, byte[] bytes, int start)
	{
		for(int i=start; i<bytes.length; i++)
		{
			if(bytes[i] == b)
			{
				return i;
			}
		}
		return -1;
	}

	/**
	 * Message processor called whenever a line of data is received from server.
	 * @param msg Server line message
	 * @throws GeneralException
	 */
	public void msg(ServerLineMsg msg) throws GeneralException
	{
		if(msg.isHandled())
		{
			return;
		}
		byte[] line = msg.getLine();
		msg.markHandled();
		int space;

		// Get prefix if provided
		byte[] prefix = null;
		int pos = 0;
		if(line[0] == ':')
		{
			space = find((byte)' ', line, 0);
			if(space == -1)
			{
				IRCMsg im = new UnknownIRCMsg("No space in line");
				im.init(msg.getServer(), line, null, null, new byte[][]{}, false);
				setEncoding(im,  im.getServer(),  null,  null);
				dispatchMessage(im);
				return;
			}

			prefix = new byte[space-1];
			System.arraycopy(line, 1, prefix, 0, space-1);
			pos = space+1;
		}

		// Get command
		space = find((byte)' ', line, pos);
		if(space == -1)
		{
			space = line.length;
		}
		byte[] command = new byte[space-pos];
		System.arraycopy(line, pos, command, 0, command.length);
		pos = space+1;

		// Get params
		boolean includesPostfix = false;
		List<byte[]> params = new LinkedList<byte[]>();
		while(pos < line.length)
		{
			if(line[pos] == ':') // Final parameter begins with : to include spaces
			{
				byte[] param = new byte[line.length-pos-1];
				System.arraycopy(line, pos+1, param, 0, param.length);
				params.add(param);
				pos = line.length;
				includesPostfix = true;
			}
			else
			{
				space = find((byte)' ', line, pos);
				if(space == -1) space = line.length;

				byte[] param = new byte[space-pos];
				System.arraycopy(line, pos, param, 0, param.length);
				params.add(param);
				pos = space+1;
			}
		}
		byte[][] paramsArray = params.toArray(new byte[0][]);

		IRCMsg base = new IRCMsg();
		base.init(msg.getServer(), line, prefix, command, paramsArray, includesPostfix);
		base.setSequence(msg);
		generateMessage(base);
	}

	/**
	 * Generates and dispatches an IRC message of specific subclass,  based on
	 * the parameters in the given base message.
	 * @param base Base message containing all the parameters
	 */
	private void generateMessage(IRCMsg base)
	{
		IRCMsg msg;
		try
		{
			String sCommand = base.getCommand();
			if(sCommand.matches("[0-9]{3}"))
			{
				msg = genNumeric(base);
			}
			else if(sCommand.equals("NOTICE"))
			{
				msg = genNotice(base);
			}
			else if(sCommand.equals("PRIVMSG"))
			{
				msg = genPrivMsg(base);
			}
			else if(sCommand.equals("NICK"))
			{
				msg = genNick(base);
			}
			else if(sCommand.equals("QUIT"))
			{
				msg = genQuit(base);
			}
			else if(sCommand.equals("SILENCE"))
			{
				msg = genSilence(base);
			}
			else if(sCommand.equals("MODE"))
			{
				msg = genMode(base);
			}
			else if(sCommand.equals("INVITE"))
			{
				msg = genInvite(base);
			}
			else if(sCommand.equals("JOIN"))
			{
				msg = genJoin(base);
			}
			else if(sCommand.equals("PART"))
			{
				msg = genPart(base);
			}
			else if(sCommand.equals("TOPIC"))
			{
				msg = genTopic(base);
			}
			else if(sCommand.equals("KICK"))
			{
				msg = genKick(base);
			}
			else if(sCommand.equals("PING"))
			{
				msg = genPing(base);
			}
			else if(sCommand.equals("ERROR"))
			{
				msg = genError(base);
			}
			else
			{
				throw new InvalidMessageException("Failed to recognise command");
			}
		}
		catch(InvalidMessageException ime)
		{
			msg = new UnknownIRCMsg(ime.getMessage());
		}
		// Add in base parameters and dispatch
		msg.init(base);
		if(!msg.hasEncoding())
			// Allows code further down to set specific encoding first,  if needed
		{
			if(msg instanceof ChanIRCMsg)
			{
				ChanIRCMsg fromChan = (ChanIRCMsg)msg;
				setEncoding(msg, msg.getServer(), fromChan.getChannel(), fromChan.getSourceUser());
			}
			else if(msg instanceof UserSourceIRCMsg)
			{
				UserSourceIRCMsg fromUser = (UserSourceIRCMsg)msg;
				setEncoding(msg, msg.getServer(), null, fromUser.getSourceUser());
			}
			else
			{
				setEncoding(msg, msg.getServer(), null, null);
			}
		}
		dispatchMessage(msg);
	}

	private void setEncoding(IRCMsg msg, Server s, String chan, IRCUserAddress user)
	{
		IRCEncoding.EncodingInfo ei =
			context.getSingle(IRCEncoding.class).getEncoding(s, chan, user);
		msg.setEncoding(ei);
	}

	private IRCMsg genNumeric(IRCMsg base) throws InvalidMessageException
	{
		checkPrefix(base);

		byte[][] params = base.getParams();
		if(params.length<1)
		{
			throw new InvalidMessageException("Missing target for server numeric");
		}
		String target = IRCMsg.convertISO(params[0]);

		int numeric = Integer.parseInt(base.getCommand());
		IRCMsg similar = null;
		switch(numeric)
		{
		case NumericIRCMsg.RPL_CHANNELMODEIS:
			if(params.length >= 3)
			{
				similar = genChanMode(
					base.getServer(), null, params, IRCMsg.convertISO(params[1]), 1);
			}
			break;
		}

		NumericIRCMsg im = new NumericIRCMsg(
			base.getPrefix(), target, numeric, similar);
		return im;
	}

	private IRCMsg genNick(IRCMsg base) throws InvalidMessageException
	{
		checkPrefix(base);

		byte[][] params = base.getParams();
		if(params.length<1)
		{
			throw new InvalidMessageException("Missing nick change");
		}

		return new NickIRCMsg(
			new IRCUserAddress(base.getPrefix(),  false),
			IRCMsg.convertISO(params[0]));
	}

	private IRCMsg genSilence(IRCMsg base) throws InvalidMessageException
	{
		checkPrefix(base);

		byte[][] params = base.getParams();
		if(params.length<1 || params[0].length<2)
		{
			throw new InvalidMessageException("Missing silence mask");
		}
		char flag = (char)params[0][0];
		boolean positive = (flag == '+');
		if(!positive && flag != '-')
		{
			throw new InvalidMessageException("Couldn't parse silence mask,  expecting + or -");
		}
		return new SilenceIRCMsg(
			new IRCUserAddress(base.getPrefix(),  false),
			positive,
			IRCMsg.convertISO(params[0]).substring(1));
	}

	private IRCMsg genQuit(IRCMsg base) throws InvalidMessageException
	{
		checkPrefix(base);

		byte[][] params = base.getParams();
		byte[] message = (params.length<1) ? null : params[0];

		return new QuitIRCMsg(
			new IRCUserAddress(base.getPrefix(),  false), message);
	}

	private IRCMsg genNotice(IRCMsg base) throws InvalidMessageException
	{
		String prefix = base.getPrefix();
		byte[][] params = base.getParams();
		if(params.length<2)
		{
			throw new InvalidMessageException("Wrong number of params for NOTICE");
		}
		String target = IRCMsg.convertISO(params[0]);
		byte[] text = params[1];

		if(prefix == null || prefix.indexOf('!') == -1)
		{
			return new ServerNoticeIRCMsg(
				prefix==null ? base.getServer().getReportedOrConnectedHost() : prefix,
				target, text);
		}
		else
		{
			IRCUserAddress source = new IRCUserAddress(prefix,  false);

			// Status messages
			if(target.length() >= 2 &&
				base.getServer().getStatusMsg().indexOf(target.charAt(0)) != -1 &&
				base.getServer().getChanTypes().indexOf(target.charAt(1)) != -1)
			{
				return new ChanNoticeIRCMsg(source, target.substring(1), target.charAt(0), text);
			}
			// Channel notices
			else if(target.length() >= 1 && base.getServer().getChanTypes().indexOf(target.charAt(0)) != -1)
			{
				return new ChanNoticeIRCMsg(source, target, (char)0, text);
			}
			// User notices
			else
			{
				if(text.length>2 && text[0] == 1 && text[text.length-1] == 1)
				{
					return genCTCP(source, target, true, false, text);
				}
				else
				{
					return new UserNoticeIRCMsg(source, target, text);
				}
			}
		}
	}

	private IRCMsg genPrivMsg(IRCMsg base) throws InvalidMessageException
	{
		checkPrefix(base);

		String prefix = base.getPrefix();
		byte[][] params = base.getParams();
		if(params.length<2)
			throw new InvalidMessageException("Wrong number of params for PRIVMSG");
		String target = IRCMsg.convertISO(params[0]);
		byte[] text = params[1];

		IRCUserAddress source = new IRCUserAddress(prefix,  false);

		// Channel messages
		if(target.length() >= 1 && base.getServer().getChanTypes().indexOf(target.charAt(0)) != -1)
		{
			if(text.length>2 && text[0] == 1)
			{
				return genCTCP(source, target, false, true, text);
			}
			else
			{
				return new ChanMessageIRCMsg(source, target, text);
			}
		}
		// User messages
		else
		{
			if(text.length>2 && text[0] == 1)
			{
				return genCTCP(source, target, false, false, text);
			}
			else
			{
				return new UserMessageIRCMsg(source, target, text);
			}
		}

	}

	private IRCMsg genCTCP(IRCUserAddress source, String target,
		boolean response, boolean chan, byte[] text)
	{
		// Split into request and other bits
		int space = 1; // Over initial char 1
		for(;space<text.length;space++)
		{
			if(text[space] == 32 || text[space] == 1) break;
		}
		byte[] request = new byte[space-1];
		byte[] after = new byte[Math.max(text.length-space-
			(text[text.length-1] == 1 ? 2 : 1), 0)]; // Also remove ending char 1 if present,  and space itself
		System.arraycopy(text, 1, request, 0, request.length);
		if(after.length>0)
		{
			System.arraycopy(text, space+1, after, 0, after.length);
		}
		String requestString = IRCMsg.convertISO(request);

		if(response)
		{
			return new UserCTCPResponseIRCMsg(source, target, requestString, after);
		}
		else
		{
			if(requestString.equals("ACTION"))
			{
				if(chan)
				{
				 return new ChanActionIRCMsg(source, target, after);
				}
				else
				{
				 return new UserActionIRCMsg(source, target, after);
				}
			}
			else
			{
				if(chan)
				{
					return new ChanCTCPRequestIRCMsg(source, target, requestString, after);
				}
				else
				{
					return new UserCTCPRequestIRCMsg(source, target, requestString, after);
				}
			}
		}
	}

	private IRCMsg genMode(IRCMsg base) throws InvalidMessageException
	{
		checkPrefix(base);

		IRCUserAddress source = new IRCUserAddress(base.getPrefix(),  false);
		byte[][] params = base.getParams();
		if(params.length<2)
		{
			throw new InvalidMessageException("Wrong number of params for MODE");
		}
		String target = IRCMsg.convertISO(params[0]);

		// Channel modes
		if(target.length() >= 1 && base.getServer().getChanTypes().indexOf(target.charAt(0)) != -1)
		{
			return genChanMode(base.getServer(), source, params, target, 0);
		}
		// User modes
		else
		{
			String modes = IRCMsg.convertISO(params[1]);
			return new UserModeIRCMsg(source, target, modes);
		}
	}

	/**
	 * @param s Server for message
	 * @param source Source user (or null for the numeric type)
	 * @param params Message params
	 * @param chan Target channel
	 * @param offset 0 for a usual message,  1 for the numeric (has an extra param)
	 * @return Message
	 */
	private IRCMsg genChanMode(Server s, IRCUserAddress source, byte[][] params, String chan, int offset)
	{
		String[] modeParams = new String[params.length-(2+offset)];
		for(int i = 0;i<modeParams.length;i++)
		{
			modeParams[i] = IRCMsg.convertISO(params[i+(2+offset)]);
		}

		boolean positive = true;
		int nextParam = 0;
		List<ChanModeIRCMsg.ModeChange> l = new LinkedList<ChanModeIRCMsg.ModeChange>();
		String modes = IRCMsg.convertISO(params[1+offset]);
		for(int i=0;i<modes.length();i++)
		{
			char c = modes.charAt(i);
			if(c == '+')
			{
				positive = true;
			}
			else if(c == '-')
			{
				positive = false;
			}
			else
			{
				int modeType = s.getChanModeType(c);
				String param = null;
				switch(modeType)
				{
				case Server.CHANMODE_SETPARAM:
					// If there's no param,  break
					if(!positive)	break;
					// Fall through
				case Server.CHANMODE_USERSTATUS:
				case Server.CHANMODE_ADDRESS:
				case Server.CHANMODE_ALWAYSPARAM:
					// There's a param,  try to get it
					if(modeParams.length > nextParam)
					{
						param = modeParams[nextParam++];
					}
					break;
				case Server.CHANMODE_NOPARAM:
				case Server.CHANMODE_UNKNOWN:
					break;
				default:
					assert false;
				}

				l.add(new ChanModeIRCMsg.ModeChange(c, positive, param));
			}
		}

		return new ChanModeIRCMsg(source, chan, modes, modeParams,
			l.toArray(new ChanModeIRCMsg	.ModeChange[l.size()]));
	}

	private IRCMsg genInvite(IRCMsg base) throws InvalidMessageException
	{
		checkPrefix(base);

		byte[][] params = base.getParams();
		if(params.length<2)
		{
			throw new InvalidMessageException("Wrong number of params for INVITE");
		}

		return new InviteIRCMsg(
			new IRCUserAddress(base.getPrefix(),  false),
			IRCMsg.convertISO(params[0]),
			IRCMsg.convertISO(params[1]));
	}

	private IRCMsg genJoin(IRCMsg base) throws InvalidMessageException
	{
		checkPrefix(base);
		IRCUserAddress source = new IRCUserAddress(base.getPrefix(),  false);

		byte[][] params = base.getParams();
		if(params.length<1)
		{
			throw new InvalidMessageException("Wrong number of params for JOIN");
		}
		String target = IRCMsg.convertISO(params[0]);

		return new JoinIRCMsg(source, target);
	}

	private IRCMsg genPart(IRCMsg base) throws InvalidMessageException
	{
		checkPrefix(base);
		IRCUserAddress source = new IRCUserAddress(base.getPrefix(),  false);

		byte[][] params = base.getParams();
		if(params.length<1)
		{
			throw new InvalidMessageException("Wrong number of params for PART");
		}
		String target = IRCMsg.convertISO(params[0]);
		byte[] text = params.length >= 2 ? params[1] : null;

		// Channel messages
		return new PartIRCMsg(source, target, text);
	}

	private IRCMsg genTopic(IRCMsg base) throws InvalidMessageException
	{
		checkPrefix(base);
		IRCUserAddress source = new IRCUserAddress(base.getPrefix(),  false);

		byte[][] params = base.getParams();
		if(params.length<1)
		{
			throw new InvalidMessageException("Wrong number of params for TOPIC");
		}
		String target = IRCMsg.convertISO(params[0]);
		byte[] text = params.length >= 2 ? params[1] : new byte[0];

		// Channel messages
		return new TopicIRCMsg(source, target, text);
	}

	private IRCMsg genKick(IRCMsg base) throws InvalidMessageException
	{
		checkPrefix(base);
		IRCUserAddress source = new IRCUserAddress(base.getPrefix(),  false);

		byte[][] params = base.getParams();
		if(params.length<2)
		{
			throw new InvalidMessageException("Wrong number of params for KICK");
		}
		String target = IRCMsg.convertISO(params[0]);
		String sVictim = IRCMsg.convertISO(params[1]);
		byte[] text = params.length >= 3 ? params[2] : null;

		// Channel messages
		return new KickIRCMsg(source, target, sVictim, text);
	}

	private IRCMsg genPing(IRCMsg base) throws InvalidMessageException
	{
		byte[][] params = base.getParams();
		String sCode = params.length >= 1 ? IRCMsg.convertISO(params[0]) : null;

		return new PingIRCMsg(sCode);
	}

	private IRCMsg genError(IRCMsg base) throws InvalidMessageException
	{
		byte[][] params = base.getParams();
		if(params.length<1)
		{
			throw new InvalidMessageException("Wrong number of params for ERROR");
		}
		return new ErrorIRCMsg(IRCMsg.convertISO(params[0]));
	}

	/**
	 * @param base Message to check
	 * @throws InvalidMessageException If there's no prefix
	 */
	private static void checkPrefix(IRCMsg base) throws InvalidMessageException
	{
		if(base.getPrefix() == null) throw new InvalidMessageException(
			"Numerics must include prefix");
	}

	private void dispatchMessage(IRCMsg im)
	{
		if(mdp == null) // For testing mode
		{
			System.err.println(im);
		}
		else
		{
			mdp.dispatchMessageHandleErrors(im, false);
		}
	}

	private static class InvalidMessageException extends GeneralException
	{
		InvalidMessageException(String sReason)
		{
			super(sReason);
		}
	}


	// MessageOwner
	@Override
	public void init(MessageDispatch mdp)
	{
		this.mdp = mdp;
	}

	@Override
	public String getFriendlyName()
	{
		return "IRC protocol messages received from server";
	}

	@Override
	public Class<? extends Msg> getMessageClass()
	{
		return IRCMsg.class;
	}

	@Override
	public boolean registerTarget(Object oTarget, Class<? extends Msg> cMessage,
		MessageFilter mf, int iRequestID, int iPriority)
	{
		return true;
	}

	@Override
	public void unregisterTarget(Object oTarget, int iRequestID)
	{
	}

	@Override
	public void manualDispatch(Msg m)
	{
	}

	@Override
	public boolean allowExternalDispatch(Msg m)
	{
		return false;
	}
}
