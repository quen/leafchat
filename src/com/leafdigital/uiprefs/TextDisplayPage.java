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
package com.leafdigital.uiprefs;

import java.awt.*;
import java.util.*;
import java.util.regex.*;

import textlayout.stylesheet.*;

import com.leafdigital.prefs.api.*;
import com.leafdigital.ui.api.*;
import com.leafdigital.ui.api.Button;

import leafchat.core.api.PluginContext;

/** Preferences page controlling text display options. */
@UIHandler("textdisplay")
public class TextDisplayPage
{
	private PluginContext context;
	private Page p;

	/** UI: Use default font */
	public RadioButton fontDefaultUI;
	/** UI: Use selected font */
	public RadioButton fontSelectedUI;
	/** UI: Font name dropdown */
	public Dropdown fontNameUI;
	/** UI: Font size textbox */
	public EditBox fontSizeUI;
	/** UI: Disable colours checkbox */
	public CheckBox coloursDisableUI;

	/** UI: List of system colour types */
	public ListBox coloursUI;

	/** UI: Revert to default colour */
	public Button defaultColourUI;
	/** UI: Change colour */
	public Button changeColourUI;
	/** UI: Panel showing selected colour */
	public DecoratedPanel selectedColourUI;

	/** Map from keyword => ColourInfo */
	private Map<String, ColourInfo> colours=new HashMap<String, ColourInfo>();

	private static class ColourInfo
	{
		String description;
		Color baseRGB;
		Color userRGB;
	}

	private final static Pattern RGB=Pattern.compile("([0-9]+),([0-9]+),([0-9]+)");

	TextDisplayPage(PluginContext context)
	{
		this.context=context;

		UI ui=context.getSingleton2(UI.class);
		p = ui.createPage("textdisplay", this);
	}

	/** @return Page object */
	public Page getPage()
	{
		return p;
	}

	/** Callback: Set this page current */
	@UIAction
	public void onSet()
	{
		Preferences p=context.getSingleton2(Preferences.class);
		PreferencesGroup pg=p.getGroup(p.getPluginOwner("com.leafdigital.ui.UIPlugin"));

		// System font
		boolean system=p.toBoolean(pg.get(UIPrefs.PREF_SYSTEMFONT,UIPrefs.PREFDEFAULT_SYSTEMFONT));
		if(system)
			fontDefaultUI.setSelected();
		else
			fontSelectedUI.setSelected();

		// Fill font list
		String selectedFont=pg.get(UIPrefs.PREF_FONTNAME,UIPrefs.PREFDEFAULT_FONTNAME);
		String[] fontNames=GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		fontNameUI.clear();
		for(int i=0;i<fontNames.length;i++)
		{
			fontNameUI.addValue(fontNames[i],fontNames[i]);
			if(fontNames[i].equals(selectedFont))
				fontNameUI.setSelected(fontNames[i]);
		}

		// Font size
		fontSizeUI.setValue(pg.get(UIPrefs.PREF_FONTSIZE,UIPrefs.PREFDEFAULT_FONTSIZE));

		// Get available colours
		colours.clear();
		UI ui=context.getSingleton2(UI.class);
		Stylesheet[] stylesheets=ui.getTheme().getStylesheets();
		for(int i=0;i<stylesheets.length;i++)
		{
			RGBDeclaration[] styleColours=stylesheets[i].getColours();
			for(int j=0;j<styleColours.length;j++)
			{
				// For now, don't let users change colours that are set to reference
				// another colour keyword
				if(styleColours[j].getDefaultKeyword()!=null) continue;

				ColourInfo info=new ColourInfo();
				info.description=styleColours[j].getDescription();
				info.baseRGB=styleColours[j].getRGB();
				colours.put(styleColours[j].getKeyword(),info);
			}
		}

		// Get user preferences for them, if set
		PreferencesGroup[] colourPrefs=pg.getChild(UIPrefs.PREFGROUP_COLOURS).getAnon();
		for(int i=0;i<colourPrefs.length;i++)
		{
			String keyword=colourPrefs[i].get(UIPrefs.PREF_KEYWORD);
			String rgb=colourPrefs[i].get(UIPrefs.PREF_RGB);
			Matcher m=RGB.matcher(rgb);
			if(!m.matches()) continue;
			ColourInfo info=colours.get(keyword);
			if(info==null) continue;
			info.userRGB=new Color(Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2)),Integer.parseInt(m.group(3)));
		}

		// Fill list with colours
		coloursUI.clear();
		for(Map.Entry<String, ColourInfo> me : colours.entrySet())
		{
			ColourInfo info=me.getValue();
			coloursUI.addItem(info.description,me.getKey());
		}
	}

	/** Callback: Select default font */
	@UIAction
	public void actionDefault()
	{
		Preferences p=context.getSingleton2(Preferences.class);
		PreferencesGroup pg=p.getGroup(p.getPluginOwner("com.leafdigital.ui.UIPlugin"));
		pg.set(UIPrefs.PREF_SYSTEMFONT,p.fromBoolean(true),UIPrefs.PREFDEFAULT_SYSTEMFONT);
		UI ui=context.getSingleton2(UI.class);
		ui.refreshTheme();
	}

	/** Callback: Select chosen font */
	@UIAction
	public void actionSelected()
	{
		Preferences p=context.getSingleton2(Preferences.class);
		PreferencesGroup pg=p.getGroup(p.getPluginOwner("com.leafdigital.ui.UIPlugin"));
		pg.set(UIPrefs.PREF_SYSTEMFONT,p.fromBoolean(false),UIPrefs.PREFDEFAULT_SYSTEMFONT);
		UI ui=context.getSingleton2(UI.class);
		ui.refreshTheme();
	}

	/** Callback: Disable colours checkbox */
	@UIAction
	public void changeColoursDisable()
	{
		Preferences p=context.getSingleton2(Preferences.class);
		PreferencesGroup pg=p.getGroup(p.getPluginOwner("com.leafdigital.ui.UIPlugin"));
		pg.set(UIPrefs.PREF_IRCCOLOURS,p.fromBoolean(!coloursDisableUI.isChecked()),UIPrefs.PREFDEFAULT_IRCCOLOURS);
	}

	/** Callback: Font name changed */
	@UIAction
	public void selectionChangeFontName()
	{
		Preferences p=context.getSingleton2(Preferences.class);
		PreferencesGroup pg=p.getGroup(p.getPluginOwner("com.leafdigital.ui.UIPlugin"));
		pg.set(UIPrefs.PREF_FONTNAME,(String)fontNameUI.getSelected(),UIPrefs.PREFDEFAULT_FONTNAME);
		fontSelectedUI.setSelected();
		actionSelected();
	}

	/** Callback: Font size changed */
	@UIAction
	public void changeFontSize()
	{
		if(fontSizeUI.getFlag()!=EditBox.FLAG_NORMAL) return;
		Preferences p=context.getSingleton2(Preferences.class);
		PreferencesGroup pg=p.getGroup(p.getPluginOwner("com.leafdigital.ui.UIPlugin"));
		pg.set(UIPrefs.PREF_FONTSIZE,fontSizeUI.getValue(),UIPrefs.PREFDEFAULT_FONTSIZE);
		fontSelectedUI.setSelected();
		actionSelected();
	}

	/** Callback: Selected colour changed */
	@UIAction
	public void changeColour()
	{
		String keyword=(String)coloursUI.getSelectedData();
		if(keyword==null)
		{
			changeColourUI.setEnabled(false);
			defaultColourUI.setEnabled(false);
		}
		else
		{
			changeColourUI.setEnabled(true);
			defaultColourUI.setEnabled(colours.get(keyword).userRGB!=null);
		}
		selectedColourUI.repaint();
	}

	/**
	 * Callback: Paint into panel.
	 * @param g2 Graphics
	 * @param left X
	 * @param top Y
	 * @param width Width
	 * @param height Height
	 */
	@UIAction
	public void paintSelectedColour(Graphics2D g2,int left,int top,int width,int height)
	{
		String keyword=(String)coloursUI.getSelectedData();
		if(keyword==null) return;
		ColourInfo info=colours.get(keyword);
		Color c;
		if(info.userRGB!=null)
			c=info.userRGB;
		else
			c=info.baseRGB;
		g2.setColor(c);
		int dividerX=width/2,dividerY=height/2;
		g2.fillRect(left,top,dividerX,dividerY);
		g2.fillRect(left+dividerX,top+dividerY,width-dividerX,height-dividerY);

		g2.setColor(Color.white);
		g2.fillRect(left+dividerX,top,width-dividerX,dividerY);
		g2.setColor(c);
		Shape previousClip=g2.getClip();
		g2.clipRect(left+dividerX,top,width-dividerX,dividerY);
		int lineHeight=g2.getFontMetrics().getHeight()-g2.getFontMetrics().getLeading();
		int lines=dividerY/lineHeight + 1;
		for(int i=0;i<lines;i++)
		{
			g2.drawString("TextTextTextTextTextTextTextTextText",left+dividerX-8*i,top+(i+1)*lineHeight-5);
		}
		g2.setClip(previousClip);

		g2.fillRect(left,top+dividerY,dividerX,height-dividerY);
		g2.setColor(Color.black);
		previousClip=g2.getClip();
		g2.clipRect(left,top+dividerY,dividerX,height-dividerY);
		for(int i=0;i<lines;i++)
		{
			g2.drawString("BackgroundBackgroundBackgroundBackground",left-8*i,top+dividerY+(i+1)*lineHeight-5);
		}
		g2.setClip(previousClip);

	}

	/** Callback: 'Change' button clicked */
	@UIAction
	public void actionChangeColour()
	{
		String keyword=(String)coloursUI.getSelectedData();
		if(keyword==null) return;
		ColourInfo info=colours.get(keyword);
		Color c;
		if(info.userRGB!=null)
			c=info.userRGB;
		else
			c=info.baseRGB;
		UI ui=context.getSingleton2(UI.class);
		c=ui.showColourSelect(p,info.description+" - Select colour",c);
		if(c!=null)
		{
			Preferences p=context.getSingleton2(Preferences.class);
			PreferencesGroup pg=p.getGroup(p.getPluginOwner("com.leafdigital.ui.UIPlugin"));
			PreferencesGroup[] colourPrefs=pg.getChild(UIPrefs.PREFGROUP_COLOURS).getAnon();
			PreferencesGroup newGroup=null;
			for(int i=0;i<colourPrefs.length;i++)
			{
				String prefKeyword=colourPrefs[i].get(UIPrefs.PREF_KEYWORD);
				if(prefKeyword.equals(keyword))
				{
					newGroup=colourPrefs[i];
					break;
				}
			}
			if(newGroup==null)
			{
				newGroup=pg.getChild(UIPrefs.PREFGROUP_COLOURS).addAnon();
				newGroup.set(UIPrefs.PREF_KEYWORD,keyword);
			}
			newGroup.set(UIPrefs.PREF_RGB,	c.getRed()+","+c.getGreen()+","+c.getBlue());
			info.userRGB=c;
			selectedColourUI.repaint();
			defaultColourUI.setEnabled(true);
			ui.refreshTheme();
		}
	}

	/** Callback: 'Default' button clicked */
	@UIAction
	public void actionDefaultColour()
	{
		String keyword=(String)coloursUI.getSelectedData();
		if(keyword==null) return;
		ColourInfo info=colours.get(keyword);

		Preferences p=context.getSingleton2(Preferences.class);
		PreferencesGroup pg=p.getGroup(p.getPluginOwner("com.leafdigital.ui.UIPlugin"));
		PreferencesGroup[] colourPrefs=pg.getChild(UIPrefs.PREFGROUP_COLOURS).getAnon();
		for(int i=0;i<colourPrefs.length;i++)
		{
			String prefKeyword=colourPrefs[i].get(UIPrefs.PREF_KEYWORD);
			if(prefKeyword.equals(keyword))
			{
				colourPrefs[i].remove();
				info.userRGB=null;
				selectedColourUI.repaint();
				defaultColourUI.setEnabled(false);
				UI ui=context.getSingleton2(UI.class);
				ui.refreshTheme();
				return;
			}
		}
	}
}
