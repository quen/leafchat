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
package leafchat.startup;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.io.*;

import javax.swing.*;

import util.GraphicsUtils;
import leafchat.core.PluginClassLoader;
import leafchat.core.api.*;

/** Splash screen that can display loading status */
public class SplashScreen extends JWindow implements PluginLoadReporter
{
	/** Splash image */
	private Image iSplash;

	/** Font used for status text */
	private Font fStatus=new Font("sans-serif",Font.PLAIN,12);

	/** Current status text */
	private String sText="Loading, please wait...";

	/**
	 * Constructs, loads image, and displays.
	 * @throws IOException Error loading image
	 */
	public SplashScreen() throws IOException
	{
		JComponent c=new JComponent()
		{
			@Override
			public void paintComponent(Graphics g)
			{
				paintPane((Graphics2D)g);
			}
		};
		c.setOpaque(true);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(c,BorderLayout.CENTER);

		iSplash=GraphicsUtils.loadJPEG(getClass().getResourceAsStream("splash.jpg"));

		setSize(385,287);
		setLocationRelativeTo(null);
		setVisible(true);
	}

	/**
	 * Paints the inner pane
	 * @param g2 Context
	 */
	private void paintPane(Graphics2D g2)
	{
		// Get fontrendercontext (and store it for the rest of the app)
		FontRenderContext frc=g2.getFontRenderContext();

		g2.drawImage(iSplash,0,0,this);

		g2.setFont(fStatus);
		int iWidth=(int)
		  fStatus.getStringBounds(sText,frc).getWidth();

		g2.drawString(sText,(getWidth()-iWidth)/2,264);
	}

	/**
	 * Call to set the status text
	 * @param sText Text line
	 */
	void setText(String sText)
	{
		this.sText=sText;
		repaint();
	}

	@Override
	public void reportFailure(File f, GeneralException ge)
	{
		JOptionPane.showMessageDialog(this,
		  "<html>Error loading plugin jar file "+f.getName()+"<br><br>" +		  "To avoid seeing this error in future, delete the plugin.<br>" +		  "("+ge.getMessage()+")","leafChat loading error",JOptionPane.ERROR_MESSAGE);
		System.err.println("Plugin loading error: "+f.getName());
		ge.printStackTrace();
	}

	@Override
	public void reportLoading(File f)
	{
		setText("Loading plugin: "+f.getName());
	}

	@Override
	public void reportProgress(String sProgress)
	{
		setText(sProgress);
	}

	@Override
	public void reportFailure(PluginClassLoader pcl, GeneralException ge)
	{
		JOptionPane.showMessageDialog(this,
			"<html>Error initialising plugin jar file "+pcl.getJarName()+"<br><br>" +
			"To avoid seeing this error in future, delete the plugin.<br>" +
			"("+ge.getMessage()+")","leafChat loading error",JOptionPane.ERROR_MESSAGE);
		System.err.println("Plugin initialising error: "+pcl.getJarName());
		ge.printStackTrace();
	}

	@Override
	public void reportInstantiating(PluginClassLoader pcl)
	{
		setText("Initialising: "+pcl.getName());
	}

	@Override
	public void reportFailure(PluginClassLoader pcl, String[] asDependencies)
	{
		JOptionPane.showMessageDialog(this,
			"<html>Error initialising plugin jar file "+pcl.getJarName()+"<br><br>" +
			"To avoid seeing this error in future, delete the plugin.<br>" +
			"(Missing dependencies)","leafChat loading error",JOptionPane.ERROR_MESSAGE);
		System.err.println("Missing dependencies for: "+pcl.getJarName());
		for(int i=0;i<asDependencies.length;i++)
		{
			System.err.println("* "+asDependencies[i]);
		}
	}
}
