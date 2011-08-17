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
package com.leafdigital.encryption;

import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.spec.*;

import javax.crypto.*;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;

import org.w3c.dom.Element;

import com.leafdigital.irc.api.*;
import com.leafdigital.ircui.api.*;

import util.*;
import util.xml.XML;
import leafchat.core.api.*;

/**
 * Chat window for encrypted chat.
 */
public class EncryptedWindow implements GeneralChatWindow.Handler
{
	private PrivateKey firstPartyPrivate;
	private Cipher encrypter,decrypter;

	private PluginContext context;

	private GeneralChatWindow w;

	private Server s;
	private String nick;
	private String ourNick;

	private boolean gotRemote=false,everGotRemote=false;

	EncryptedWindow(PluginContext context,Server s,String nick)
	{
		this.context=context;
		this.s=s;
		this.nick=nick;
		this.ourNick=s.getOurNick();
		w=context.getSingleton2(IRCUI.class).createGeneralChatWindow(
			context,this,null,null,null,300,s.getOurNick(),nick,true);
		w.setEnabled(false);
		context.requestMessages(NickIRCMsg.class,this);
		context.requestMessages(ServerDisconnectedMsg.class,this);
		w.setTitle(nick+" - Encrypted chat");
	}

	/**
	 * Message: Server disconnected.
	 * @param msg Message
	 */
	public void msg(ServerDisconnectedMsg msg)
	{
		if(msg.getServer()==this)
		{
			w.getMessageDisplay().showInfo("Disconnected from server");
			((EncryptionPlugin)context.getPlugin()).killSession(s,this);
		}
	}

	/**
	 * Message: Nickname changed (used to detect changes of our own nickname).
	 * @param msg Message
	 */
	public void msg(NickIRCMsg msg)
	{
		if(msg.getServer()==s && msg.getSourceUser().getNick().equalsIgnoreCase(ourNick))
		{
			sendCTCP(false,EncryptionPlugin.CTCP_NICK,ourNick);
			ourNick=msg.getNewNick();
		}
	}

	void initLocal()
	{
		// This bit might take a while so let's call it in another thread
		w.getMessageDisplay().showInfo("Generating key...");
		(new Thread("EncryptedWindow key generation")
		{
			@Override
			public void run()
			{
				String key=null;
				Exception e=null;
				try
				{
					key=initFirstParty();
				}
				catch(GeneralSecurityException gse)
				{
					e=gse;
				}
				final String finalKey=key;
				final Exception finalException=e;
				context.yield(new Runnable()
				{
					@Override
					public void run()
					{
						if(finalKey==null)
						{
							context.log("Error generating key",finalException);
							w.getMessageDisplay().showError("Key could not be generated. " +
								"This is probably because your Java distribution is missing " +
								"required security features. See system log for details.");
						}
						else
						{
							w.getMessageDisplay().showInfo("Key generated. Contacting <nick>"+
								XML.esc(nick)+"</nick>...");
							sendCTCP(false,EncryptionPlugin.CTCP_INFO,
								ourNick+" is attempting to start an encrypted conversation " +
									"using a feature built into the leafChat 2 IRC client. " +
									"If you're using something else, please tell them so. They " +
									"should have checked with you first!");
							int partCount=(finalKey.length()+349)/350;
							for(int i=0;i<partCount;i++)
							{
								sendCTCP(false,EncryptionPlugin.CTCP_INIT,"DH/TripleDES "+
									(i+1)+"/"+partCount+" "+finalKey.substring(
										i*350,Math.min(finalKey.length(),(i+1)*350)));
							}
							TimeUtils.addTimedEvent(new Runnable()
							{
								@Override
								public void run()
								{
									if(!everGotRemote && w!=null)
									{
										w.getMessageDisplay().showError("No response from <nick>"+
											XML.esc(nick)+"</nick>. Are you sure they are using " +
											"leafChat 2? The encrypted chat feature works only with " +
											"other leafChat 2 users at present.");
									}
								}
							},15000,true);
						}
					}
				});
			}
		}).start();
	}

	private String remoteKey="";

	void initRemote(String remoteKeyPart,int partNum,int partTotal)
	{
		remoteKey+=remoteKeyPart;
		if(partNum!=partTotal) return;

		// This bit might take a while so let's call it in another thread
		w.getMessageDisplay().showInfo("<nick>"+XML.esc(nick)+"</nick> has requested an encrypted chat. Generating response key...");
		(new Thread("EncryptedWindow response key generation")
		{
			@Override
			public void run()
			{
				String key=null;
				Exception e=null;
				try
				{
					key=initSecondParty(remoteKey);
				}
				catch(GeneralSecurityException gse)
				{
					e=gse;
				}
				final String finalKey=key;
				final Exception finalException=e;
				context.yield(new Runnable()
				{
					@Override
					public void run()
					{
						if(finalKey==null)
						{
							context.log("Error generating response key",finalException);
							w.getMessageDisplay().showError("Key could not be generated. " +
								"This is probably because your Java distribution is missing " +
								"required security features. See system log for details.");
						}
						else
						{
							w.getMessageDisplay().showInfo("Encrypted chat ready.");
							w.setEnabled(true);
							int partCount=(finalKey.length()+349)/350;
							for(int i=0;i<partCount;i++)
							{
								sendCTCP(true,EncryptionPlugin.CTCP_INIT,
									(i+1)+"/"+partCount+" "+finalKey.substring(
										i*350,Math.min(finalKey.length(),(i+1)*350)));
							}
							gotRemote=true;
							everGotRemote=true;
						}
					}
				});
			}
		}).start();
	}

	void finishInit(String remoteKeyPart,int partNum,int partTotal)
	{
		remoteKey+=remoteKeyPart;
		if(partNum!=partTotal) return;

		try
		{
			finishFirstParty(remoteKey);
			w.getMessageDisplay().showInfo("Encrypted chat ready.");
			w.setEnabled(true);
			gotRemote=true;
			everGotRemote=true;
		}
		catch(GeneralSecurityException e)
		{
			context.log("Error confirming remote key",e);
			w.getMessageDisplay().showError("Remote key could not be confirmed. " +
				"This is probably because your Java distribution is missing " +
				"required security features. See system log for details.");
		}
	}

	private void sendCTCP(boolean response,String request,String text)
	{
		s.sendLine(IRCMsg.constructBytes((response?"NOTICE " : "PRIVMSG ")+nick+" :\u0001"+
			request+" "+text+"\u0001"));
	}

	@Override
	public void doCommand(Commands c,String text)
	{
		// Ignore blanks
		if(text.equals("")) return;
		boolean action=false;

		// Check for text or /me
		if(c.isCommandCharacter(text.charAt(0)))
		{
			if(text.startsWith("/me "))
			{
				action=true;
				text=text.substring(4);
			}
			else if(text.startsWith("/say "))
			{
				action=false;
				text=text.substring(5);
			}
			else
			{
				// Do other commands normally
				c.doCommand(text,s,null,null,w.getMessageDisplay(),false);
				return;
			}
		}

		// Encrypt message
		String encoded;
		try
		{
			encoded=encode(text);
			sendCTCP(false,EncryptionPlugin.CTCP_TEXT,(action?"A":"S")+" "+encoded);

			// Display message
			w.getMessageDisplay().showOwnText(action ? MessageDisplay.TYPE_ACTION : MessageDisplay.TYPE_MSG,nick,text);
		}
		catch(GeneralSecurityException e)
		{
			context.log("Encryption error",e);
			w.getMessageDisplay().showError(
				"An encryption error occurred. Please try closing and reopening the window.");
		}
	}

	void text(boolean action,String data)
	{
		// Decrypt text
		try
		{
			// Show text
			w.showRemoteText(action ? MessageDisplay.TYPE_ACTION : MessageDisplay.TYPE_MSG,nick,decode(data));
		}
		catch(GeneralSecurityException e)
		{
			context.log("Decryption error",e);
			w.getMessageDisplay().showError(
				"An encryption error occurred. Please try closing and reopening the window.");
		}
	}

	void end()
	{
		w.setEnabled(false);
		gotRemote=false;
		w.getMessageDisplay().showInfo("- <nick>"+XML.esc(nick)+"</nick> has closed the session");
	}

	void nick(String newNick)
	{
		this.nick=newNick;
		w.setTarget(nick);
		w.setTitle(nick+" - Encrypted chat");
	}

	@Override
	public void windowClosed()
	{
		if(gotRemote && s.isConnected())
		{
			sendCTCP(false,EncryptionPlugin.CTCP_END,"");
			gotRemote=false;
		}
		context.unrequestMessages(null,this,PluginContext.ALLREQUESTS);
		w=null;
		((EncryptionPlugin)context.getPlugin()).killSession(s,this);
	}

	private String initFirstParty() throws GeneralSecurityException
	{
    // Create the parameter generator for a 1024-bit DH key pair
    AlgorithmParameterGenerator paramGen1=AlgorithmParameterGenerator.getInstance(EncryptionPlugin.ALGORITHM_KEYAGREEMENT);
    paramGen1.init(1024);

    // Generate the parameters
    AlgorithmParameters params1=paramGen1.generateParameters();
    DHParameterSpec dhSpec1=params1.getParameterSpec(DHParameterSpec.class);

    // Use the values to generate a key pair
    KeyPairGenerator keyGen1 = KeyPairGenerator.getInstance(EncryptionPlugin.ALGORITHM_KEYAGREEMENT);
    keyGen1.initialize(dhSpec1);
    KeyPair keypair1 = keyGen1.generateKeyPair();

    // Get the generated public and private keys
    firstPartyPrivate=keypair1.getPrivate();
    PublicKey publicKey1 = keypair1.getPublic();

    // Send the public key bytes to the other party...
    return Base64.encodeBytes(publicKey1.getEncoded(),Base64.DONT_BREAK_LINES);
	}

	private void finishFirstParty(String remotePublicKeyBase64) throws GeneralSecurityException
	{
    // Convert the public key bytes into a PublicKey object
    X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(
    		Base64.decode(remotePublicKeyBase64));
    PublicKey remotePublicKey=KeyFactory.getInstance(EncryptionPlugin.ALGORITHM_KEYAGREEMENT).generatePublic(x509KeySpec);

    // Prepare to generate the secret key with the private key and public key of the other party
    KeyAgreement ka=KeyAgreement.getInstance(EncryptionPlugin.ALGORITHM_KEYAGREEMENT);
    ka.init(firstPartyPrivate);
    firstPartyPrivate=null;
    ka.doPhase(remotePublicKey, true);

    initCiphers(ka);
	}

	private void initCiphers(KeyAgreement ka) throws GeneralSecurityException
	{
    // Generate the secret key
    SecretKey key=ka.generateSecret(EncryptionPlugin.ALGORITHM_CIPHER);

    // Construct the ciphers
    encrypter = Cipher.getInstance(EncryptionPlugin.ALGORITHM_CIPHER);
    decrypter = Cipher.getInstance(EncryptionPlugin.ALGORITHM_CIPHER);
    encrypter.init(Cipher.ENCRYPT_MODE, key);
    decrypter.init(Cipher.DECRYPT_MODE, key);
	}

	private String initSecondParty(String remotePublicKeyBase64) throws GeneralSecurityException
	{
    // Convert the public key bytes into a PublicKey object
    X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(
    		Base64.decode(remotePublicKeyBase64));
    PublicKey remotePublicKey=KeyFactory.getInstance(EncryptionPlugin.ALGORITHM_KEYAGREEMENT).generatePublic(x509KeySpec);

    // Use the parameters from this key to generate a key pair
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance(EncryptionPlugin.ALGORITHM_KEYAGREEMENT);
    keyGen.initialize(((DHPublicKey)remotePublicKey).getParams());
    KeyPair keyPair = keyGen.generateKeyPair();

    // Get the generated public and private keys
    PrivateKey privateKey=keyPair.getPrivate();
    PublicKey publicKey=keyPair.getPublic();

    // Prepare to generate the secret key with the private key and public key of the other party
    KeyAgreement ka=KeyAgreement.getInstance(EncryptionPlugin.ALGORITHM_KEYAGREEMENT);
    ka.init(privateKey);
    ka.doPhase(remotePublicKey, true);

    initCiphers(ka);

    // Public key bytes...
    return Base64.encodeBytes(publicKey.getEncoded(),Base64.DONT_BREAK_LINES);
	}

	private String encode(String data) throws GeneralSecurityException
	{
		try
		{
			return Base64.encodeBytes(encrypter.doFinal(data.getBytes("UTF-8")),Base64.DONT_BREAK_LINES);
		}
		catch(UnsupportedEncodingException e)
		{
			throw new BugException(e);
		}
	}

	private String decode(String data) throws GeneralSecurityException
	{
		try
		{
			return new String(decrypter.doFinal(Base64.decode(data)),"UTF-8");
		}
		catch(UnsupportedEncodingException e)
		{
			throw new BugException(e);
		}
	}

	@Override
	public void internalAction(Element e)
	{
	}
}
