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

import com.leafdigital.audio.api.Audio;
import com.leafdigital.prefs.api.*;
import com.leafdigital.ui.api.*;

import leafchat.core.api.*;

/**
 * Preferences options for highlighter.
 */
@UIHandler({"highlighter", "addword"})
public class HighlighterPage
{
	private PluginContext context;
	private Page p;
	private Dialog dialog;

	/**
	 * UI: highlight nicknames checkbox.
	 */
	public CheckBox nickUI;
	/**
	 * UI: list of highlighted words.
	 */
	public ListBox wordsUI;

	/**
	 * UI: edit button.
	 */
	public Button editUI;
	/**
	 * UI: remove button.
	 */
	public Button removeUI;

	/**
	 * UI: dialog word edit.
	 */
	public EditBox wordUI;

	/**
	 * UI: dialog set button.
	 */
	public Button setUI;

	/**
	 * Sound selector.
	 */
	public Dropdown soundUI;
	/**
	 * Option to restrict sound more than once per minute.
	 */
	public CheckBox restrictSoundUI;

	HighlighterPage(PluginContext context)
	{
		this.context = context;
		UI ui = context.getSingleton2(UI.class);
		p = ui.createPage("highlighter", this);
	}

	/**
	 * Action: enter prefs page.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void onSet() throws GeneralException
	{
		Preferences prefs = context.getSingleton2(Preferences.class);
		PreferencesGroup group = prefs.getGroup(context.getPlugin());

		nickUI.setChecked(prefs.toBoolean(group.get(
			HighlighterPlugin.PREF_HIGHLIGHT_NICKNAME,
			HighlighterPlugin.PREFDEFAULT_HIGHLIGHT_NICKNAME)));

		wordsUI.clear();
		PreferencesGroup[] anon = group.getAnon();
		for(int i=0; i<anon.length; i++)
		{
			wordsUI.addItem(anon[i].get(HighlighterPlugin.PREF_WORD),
				Integer.valueOf(i));
		}

		String restrictSound = group.get(
			HighlighterPlugin.PREF_HIGHLIGHT_SOUND,
			HighlighterPlugin.PREFDEFAULT_HIGHLIGHT_SOUND);
		soundUI.clear();
		soundUI.addValue("", "(No sound)");
		soundUI.setSelected("");
		Audio audio = context.getSingleton2(Audio.class);
		String[] sounds = audio.getSounds();
		for(int i=0; i<sounds.length; i++)
		{
			soundUI.addValue(sounds[i], sounds[i]);
			if(sounds[i].equals(restrictSound))
			{
				soundUI.setSelected(sounds[i]);
			}
		}

		restrictSoundUI.setChecked(prefs.toBoolean(group.get(
			HighlighterPlugin.PREF_HIGHLIGHT_RESTRICTSOUND,
			HighlighterPlugin.PREFDEFAULT_HIGHLIGHT_RESTRICTSOUND)));

		selectWords();
	}

	private void fillList()
	{
		Preferences prefs = context.getSingleton2(Preferences.class);
		PreferencesGroup group = prefs.getGroup(context.getPlugin());

		wordsUI.clear();
		PreferencesGroup[] anon = group.getAnon();
		for(int i=0; i<anon.length; i++)
		{
			wordsUI.addItem(anon[i].get(HighlighterPlugin.PREF_WORD),
				Integer.valueOf(i));
		}
	}

	/**
	 * Action: word selection change.
	 */
	@UIAction
	public void selectWords()
	{
		boolean selected = wordsUI.getSelected() != null;
		editUI.setEnabled(selected);
		removeUI.setEnabled(selected);
	}

	/**
	 * Action: change nickname checkbox.
	 */
	@UIAction
	public void changeNick()
	{
		Preferences prefs = context.getSingleton2(Preferences.class);
		PreferencesGroup group = prefs.getGroup(context.getPlugin());

		group.set(HighlighterPlugin.PREF_HIGHLIGHT_NICKNAME,
			prefs.fromBoolean(nickUI.isChecked()),
			HighlighterPlugin.PREFDEFAULT_HIGHLIGHT_NICKNAME);
	}

	/**
	 * Action: change restrict checkbox.
	 */
	@UIAction
	public void changeRestrict()
	{
		Preferences prefs = context.getSingleton2(Preferences.class);
		PreferencesGroup group = prefs.getGroup(context.getPlugin());

		group.set(HighlighterPlugin.PREF_HIGHLIGHT_RESTRICTSOUND,
			prefs.fromBoolean(restrictSoundUI.isChecked()),
			HighlighterPlugin.PREFDEFAULT_HIGHLIGHT_RESTRICTSOUND);
	}

	/**
	 * Action: add word.
	 * @throws GeneralException Error parsing xml dialog
	 */
	@UIAction
	public void actionAdd() throws GeneralException
	{
		UI ui = context.getSingleton2(UI.class);
		dialog = ui.createDialog("addword", this);
		dialog.show(p);
	}

	/**
	 * Action: edit word.
	 * @throws GeneralException Error parsing xml dialog
	 */
	@UIAction
	public void actionEdit() throws GeneralException
	{
		UI ui = context.getSingleton2(UI.class);
		dialog = ui.createDialog("addword", this);
		dialog.setTitle("Edit word");
		setUI.setLabel("Edit word");
		setUI.setOnAction("actionEditWord");
		wordUI.setOnEnter("actionEditWord");
		wordUI.setValue(wordsUI.getSelected());
		dialog.show(p);
	}

	/**
	 * Action: remove word.
	 */
	@UIAction
	public void actionRemove()
	{
		Integer selected = (Integer)wordsUI.getSelectedData();
		wordsUI.removeData(selected);

		Preferences prefs = context.getSingleton2(Preferences.class);
		PreferencesGroup group = prefs.getGroup(context.getPlugin());
		group.getAnon()[selected.intValue()].remove();

		fillList();
		selectWords();
	}

	/**
	 * Action: change sound.
	 */
	@UIAction
	public void selectSound()
	{
		Preferences prefs = context.getSingleton2(Preferences.class);
		PreferencesGroup group = prefs.getGroup(context.getPlugin());

		group.set(HighlighterPlugin.PREF_HIGHLIGHT_SOUND,
			(String)soundUI.getSelected(),
			HighlighterPlugin.PREFDEFAULT_HIGHLIGHT_RESTRICTSOUND);
	}

	/**
	 * Action: change word (dialog)
	 */
	@UIAction
	public void changeWord()
	{
		String word = wordUI.getValue();
		boolean present = false;
		String[] items = wordsUI.getItems();
		for(int i=0; i<items.length; i++)
		{
			if(items[i].equalsIgnoreCase(word))
			{
				present = true;
				break;
			}
		}
		setUI.setEnabled(word.trim().equals(word)	&& word.length() > 0 && !present);
	}

	/**
	 * Action: cancel (dialog)
	 */
	@UIAction
	public void actionCancel()
	{
		dialog.close();
	}

	/**
	 * Action: add (dialog)
	 */
	@UIAction
	public void actionAddWord()
	{
		Preferences prefs = context.getSingleton2(Preferences.class);
		PreferencesGroup group = prefs.getGroup(context.getPlugin());
		PreferencesGroup anon = group.addAnon();
		String word = wordUI.getValue();
		anon.set(HighlighterPlugin.PREF_WORD, word);
		Integer integer = Integer.valueOf(group.getAnon().length - 1);
		wordsUI.addItem(word, integer);
		wordsUI.setSelectedData(integer, true);
		dialog.close();
		selectWords();
	}

	/**
	 * Action: edit (dialog)
	 */
	public void actionEditWord()
	{
		Preferences prefs = context.getSingleton2(Preferences.class);
		PreferencesGroup group = prefs.getGroup(context.getPlugin());
		Integer integer = (Integer)wordsUI.getSelectedData();
		PreferencesGroup anon =
			group.getAnon()[integer.intValue()];
		String word = wordUI.getValue();
		anon.set(HighlighterPlugin.PREF_WORD, word);
		wordsUI.removeData(integer);
		wordsUI.addItem(word, integer);
		wordsUI.setSelectedData(integer, true);
		dialog.close();
	}

	/**
	 * Action: dialog closed
	 */
	@UIAction
	public void dialogClosed()
	{
		dialog = null;
	}

	Page getPage()
	{
		return p;
	}
}
