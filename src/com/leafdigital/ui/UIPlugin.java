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
package com.leafdigital.ui;

import java.io.*;
import java.net.*;
import java.util.regex.*;

import util.*;
import util.xml.*;

import com.leafdigital.prefs.api.*;
import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/** User interface plugin */
@UIHandler({"errordialog", "license"})
public class UIPlugin implements Plugin
{
	final static String PREFGROUP_MAINWINDOW="mainwindow",
		PREF_X="x",PREFDEFAULT_X="-1",
		PREF_Y="y",PREFDEFAULT_Y="-1",
		PREF_WIDTH="width",PREFDEFAULT_WIDTH="800",
		PREF_HEIGHT="height",PREFDEFAULT_HEIGHT="600",
		PREF_AGREEDLICENSE="agreed-license";

	/** Context stored */
	private PluginContext context;

	/** License dialog */
	private Dialog license;

	/** License text */
	public TextView licenseTextUI;

	/** Error dialog */
	private Dialog errorDialog=null;

	/** Error message */
	public TextView errorMessageUI;

	/** Report checkbox */
	public CheckBox reportUI;

	/** Information about reporting. */
	public Label reportInfoUI;

	private ErrorReportThread reporter=null;


	@Override
	public void init(PluginContext pc, PluginLoadReporter plr) throws GeneralException
	{
		this.context=pc;
		UISingleton uis=new UISingleton(pc);
		pc.registerSingleton(UI.class,uis);
		pc.requestMessages(SystemStateMsg.class,this);
		pc.requestMessages(ErrorMsg.class,this, Msg.PRIORITY_LAST+1);
	}

	@Override
	public void close() throws GeneralException
	{
		UISingleton uis=(UISingleton)context.getSingle(UI.class);
		uis.close();
	}

	/**
	 * Action: agree to license.
	 * @throws GeneralException
	 */
	@UIAction
	public void actionLicenseAgree() throws GeneralException
	{
		Preferences prefs=context.getSingle(Preferences.class);
		PreferencesGroup group=prefs.getGroup(this);
		group.set(PREF_AGREEDLICENSE,"y");
		license.close();
	}

	/**
	 * Action: disagree to license.
	 */
	@UIAction
	public void actionLicenseDisagree()
	{
		// Abrupt exit
		System.exit(0);
	}

	/**
	 * Message: system date. Displays license if required, then dispatches the
	 * 'UI ready' system state message.
	 * @param m
	 * @throws GeneralException
	 */
	public void msg(SystemStateMsg m) throws GeneralException
	{
		if(m.getType() == SystemStateMsg.PLUGINSLOADED)
		{
			UISingleton.checkSwing();

			// Create window
			UISingleton uis = (UISingleton)context.getSingle(UI.class);
			uis.init("leafChat " + SystemVersion.getTitleBarVersion());

			// Check license if needed
			Preferences prefs = context.getSingle(Preferences.class);
			PreferencesGroup group = prefs.getGroup(this);
			if(group.get(PREF_AGREEDLICENSE, null) == null)
			{
				try
				{
					license = uis.createDialog(XML.parse(
						UIPlugin.class.getResourceAsStream("license.xml")), this);
					licenseTextUI.setStyleSheet("output { pad-left:4; pad-right:4; }");
					licenseTextUI.addXML(
						"<head>Free for personal non-commercial use</head>" +
						"<para>The author and copyright owner (Samuel Marshall) grants a " +
						"free license to use this program for personal, " +
						"non-commercial use only.</para>" +
						"<para>Commercial or institutional use (use for the " +
						"purposes of a company, educational institution, or other organisation) " +
						"requires a specific license which may be purchased from the author, " +
						"or otherwise negotiated. See Web site for details.</para>" +
						"<head>No warranty</head>" +
						"<para>This program is provided on an 'as is' basis without warranty of any kind, either express or implied. You are solely responsible for determining the appropriateness of using this program, and you assume all risks associated with its use, including but not limited to the risks and costs of program errors, compliance with applicable laws, and damage to or loss of data, programs or equipment.</para>" +
						"<head>Disclaimer of liability</head>" +
						"<para>The program's author accepts no liability for any direct, indirect, incidental, special, exemplary, or consequential damages, however caused, arising in any way out of the use of this program, even if advised of the possibility of such damages.</para>"+
						"<head>Copyright</head>"+
						"<para>leafChat is copyright &#x00a9; 2007 Samuel Marshall. All rights reserved.</para>"+
						"<head>Other components</head>"+
						"<para>The leafChat distribution incorporates two external libraries, " +
						"which have their own licenses and copyrights. These can be found in the 'lib' folder "+
						"within the distribution. Specifically:</para>"+
						"<para>This product includes a modified version of the Eclipse JDT Core compiler package, developed by Eclipse contributors and others.</para>"+
						"<para>This product includes software developed by SuperBonBon Industries (http://www.sbbi.net/).</para>"
						);
					license.show(null);
				}
				catch(XMLException e)
				{
					throw new GeneralException("Error parsing license dialog",e);
				}
			}

			// Send message indicating that UI is ready now
			m = new SystemStateMsg(SystemStateMsg.UIREADY);
			context.dispatchExternalMessage(SystemStateMsg.class, m, false);
		}
	}

	/**
	 * Handles ErrorMsg to display a dialog box and report error to server.
	 * @param msg Message
	 * @throws GeneralException
	 */
	public void msg(ErrorMsg msg) throws GeneralException
	{
		if(msg.isHandled())
		{
			return;
		}

		if(errorDialog==null)
		{
			// Only show the first error in dialog...
			UI ui = context.getSingle(UI.class);
			errorDialog = ui.createDialog("errordialog", this);

			reportUI.setChecked(!SystemVersion.getBuildVersion().startsWith("@"));

			errorMessageUI.setStyleSheet(
				"exception { type:block; font-size:0.8f; font-name:'Andale Mono',Monaco,'Courier New',Monospaced; }" +
				"line { gap-left:4; text-indent:20; text-first-indent:-20; }" +
				"exception { gap-left:4; gap-top:0.5f; }" +
				"exception > line { gap-left:0; }"
				);
			errorMessageUI.addXML(
				"<line><strong>" + XML.esc(msg.getMessage()) + "</strong></line>");
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			if(msg.getException() != null)
			{
				msg.getException().printStackTrace(pw);
			}
			pw.flush();
			String
				currentException = sw.toString(),
				currentMessage = msg.getMessage();
		  Matcher m = Pattern.compile(
		  	"^.*?UserScript(.*?)[$.(].*",Pattern.DOTALL).matcher(currentException);
		  boolean special = false;
		  if(m.matches())
		  {
		  	reportInfoUI.setText("<para>The error was caused by a user script, <key>" +
		  		XML.esc(m.group(1))+"</key>, and will not be reported automatically. If " +
		  		"you think the error is a system bug and not your script's fault, " +
		  		"click the checkbox below to send it in.</para>");
		  	reportUI.setChecked(false);
		  	special = true;
		  }
		  m = Pattern.compile(
		  	"^.*the theme (.*?)\\.leafChatTheme.*$",Pattern.DOTALL).matcher(currentMessage);
		  if(!special && m.matches())
		  {
		  	reportInfoUI.setText("<para>The error was caused by a user theme, <key>"+
		  		XML.esc(m.group(1))+"</key>.leafChatTheme, and will not be reported " +
		  		"automatically. If you think the error is a system bug and not your theme's fault, " +
		  		"click the checkbox below to send it in.</para>");
		  	reportUI.setChecked(false);
		  	special = true;
		  }
		  long freeSpace = IOUtils.getFreeSpace(PlatformUtils.getUserFolder());
		  if(!special && (currentException.indexOf("not enough space on the disk")!=-1
		  	|| currentException.indexOf("No space left on device")!=-1)
		  	|| (freeSpace != IOUtils.UNKNOWN && freeSpace < 65536L)	)
		  {
		  	reportInfoUI.setText("<para><strong>You have no remaining disk " +
	  			"space</strong>. To recover, free up space first, then quit " +
	  			"leafChat and restart it.</para>");
		  	reportUI.setChecked(false);
		  	special = true;
		  }

		  reporter = new ErrorReportThread(currentMessage, currentException);
			String trace = currentException;
			StringBuffer out = new StringBuffer("<exception><line>");
			while(true)
			{
				int line = trace.indexOf('\n');
				if(line == -1)
				{
					out.append(XML.esc(trace));
					break;
				}
				String thisLine = trace.substring(0,line).replaceFirst("^\\s*at\\s+","-");
				out.append(XML.esc(thisLine));
				out.append("</line><line>");
				trace = trace.substring(line+1);
			}
			out.append("</line></exception>");
			errorMessageUI.addXML(out.toString());

			String versions="<para>leafChat <key>"
				+ XML.esc(SystemVersion.getBuildVersion()) + "</key>, Java <key>"
				+ XML.esc(System.getProperty("java.version")) + "</key>, "
				+ XML.esc(System.getProperty("os.name")) + " <key>"
				+ XML.esc(System.getProperty("os.version")) + "</key></para>";
			errorMessageUI.addXML(versions);

			errorDialog.show((Window)null);
		}
		msg.markHandled();

		(context.getSingle(SystemLog.class)).log(
			this, "Error handler: " + msg.getMessage(), msg.getException());
	}

	/**
	 * User closes error dialog.
	 */
	@UIAction
	public void actionCloseErrorDialog()
	{
		if(reporter!=null && reportUI.isChecked())
		{
			reporter.start();
			reporter = null;
		}
		// Note: I don't see how it is possible that errorDialog would be null
		// at this point, but it occurred to one user, so I put the if in.
		if(errorDialog != null)
		{
			errorDialog.close();
		}
	}

	private class ErrorReportThread extends Thread
	{
		private String message,exception;
		public ErrorReportThread(String message,String exception)
		{
			super("Error report thread");
			this.message=message;
			this.exception=exception;
		}

		@Override
		public void run()
		{
			try
			{
				URL u=new URL("http://live.leafdigital.com/leafchat-remote/report.jsp");
				HttpURLConnection uc=(HttpURLConnection)u.openConnection();
				uc.setDoOutput(true);
				uc.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
				String output=
					"lc_version="+URLEncoder.encode(SystemVersion.getBuildVersion(),"UTF-8")+
					"&java_version="+URLEncoder.encode(System.getProperty("java.version"),"UTF-8")+
					"&os_name="+URLEncoder.encode(System.getProperty("os.name"),"UTF-8")+
					"&os_version="+URLEncoder.encode(System.getProperty("os.version"),"UTF-8")+
					"&message="+URLEncoder.encode(message,"UTF-8")+
					"&exception="+URLEncoder.encode(exception,"UTF-8");
				uc.getOutputStream().write(output.getBytes("UTF-8"));
				uc.getOutputStream().close();
				int code=uc.getResponseCode();
				if(code!=200) throw new Exception("Unexpected response code "+code);
				context.log("Error report: successful");
			}
			catch(Throwable t)
			{
				context.log("Error report: failed - "+t.getMessage());
			}
		}
	}

	/** Error dialog has been closed. */
	@UIAction
	public void closedErrorDialog()
	{
		errorDialog = null;
	}

	@Override
	public String toString()
	{
		return "UI plugin";
	}
}
