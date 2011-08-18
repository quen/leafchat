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
package com.leafdigital.scripting;

import java.awt.*;
import java.util.HashMap;

import com.leafdigital.ui.api.*;
import com.leafdigital.ui.api.Button;
import com.leafdigital.ui.api.Dialog;
import com.leafdigital.ui.api.Label;
import com.leafdigital.ui.api.Window;

import leafchat.core.api.*;

/**
 * Interface for users to edit an individual script.
 */
@UIHandler("scripteditor")
public class ScriptEditor implements Script.StateListener
{
	private final static Class<?>[] itemTypes =
	{
		ItemCommand.class,ItemMenu.class,ItemEvent.class,ItemVariable.class
	};

	private ScriptingTool owner;
	private Script script;
	private Window w;
	private PluginContext context;

	/** UI: Panel containing all items */
	public VerticalPanel itemsUI;
	/** UI: Save button */
	public Button saveUI;
	/** UI: Revert button */
	public Button revertUI;
	/** UI: Add button */
	public Button addUI;
	/** UI: Script status */
	public Label statusUI;
	/** UI: Enabled checkbox */
	public CheckBox enabledUI;
	/** UI: Save/compile progress */
	public Progress progressUI;

	HashMap<ScriptItem, ItemDetails> detailsMap =
		new HashMap<ScriptItem, ItemDetails>();

	ScriptEditor(PluginContext context,ScriptingTool owner,Script script) throws GeneralException
	{
		this.context=context;
		this.owner=owner;
		this.script=script;

		UI ui = (context.getSingle(UI.class));
		w = ui.createWindow("scripteditor", this);
		w.setRemember("scripting", script.getName());
		initWindow();
		w.show(false);

		script.addStateListener(this);
	}

	private void initWindow()
	{
		// Set title
		w.setTitle("Editing script - "+script.getName());

		// Remove any existing items
		ScriptItem[] items=detailsMap.keySet().toArray(new ScriptItem[detailsMap.keySet().size()]);
		for(int i=0;i<items.length;i++)
		{
			deleteItem(items[i]);
		}

		// Add new items
		items=script.getItems();
		for(int i=0;i<items.length;i++)
		{
			addItem(items[i]);
		}

		enabledUI.setChecked(script.isEnabled());
		addUI.setEnabled(true);
		scriptStateChanged(script);
		progressUI.setVisible(false);
	}

	/**
	 * Object that handles UI callbacks from each item.
	 */
	@UIHandler("scriptitem")
	public class ItemCallbacks
	{
		/** UI: Item summary */
		public Label summaryUI;
		/** UI: Item title */
		public Label titleUI;
		/** UI: Item error info (if any) */
		public Label errorUI;
		/** UI: Item variables list */
		public Label variablesUI;
		/** UI: Spacer used if there's no error */
		public Spacer noErrorUI;
		/** UI: Edit area for code */
		public EditArea codeUI;
		/** UI: Enabled checkbox */
		public CheckBox enabledUI;
		/** UI: Settings button */
		public Button settingsUI;
		/** UI: Delete button */
		public Button deleteUI;

		private ScriptItem item;
		private ItemCallbacks(ScriptItem item)
		{
			this.item=item;
		}
		private void init()
		{
			titleUI.setText("<strong>"+item.getTypeName()+"</strong>");
			summaryUI.setText(item.getSummaryLabel());
			if(item.getVariablesLabel()==null)
				variablesUI.setVisible(false);
			else
				variablesUI.setText(item.getVariablesLabel());
			if(item.hasErrors())
			{
				errorUI.setText("<error>"+item.getErrorLabel()+"</error>");
				noErrorUI.setVisible(false);
			}
			else
			{
				errorUI.setVisible(false);
				noErrorUI.setVisible(true);
			}
			if(item instanceof UserCodeItem)
			{
				codeUI.setValue(((UserCodeItem)item).getUserCode());
				codeUI.highlightErrorLines(((UserCodeItem)item).getErrorLines());
			}
			else
			{
				codeUI.setVisible(false);
			}
			enabledUI.setChecked(item.isEnabled());
		}

		private void disable()
		{
			codeUI.setEnabled(false);
			enabledUI.setEnabled(false);
			settingsUI.setEnabled(false);
			deleteUI.setEnabled(false);
		}

		/** Action: User changes enabled checkbox */
		@UIAction
		public void changeEnabled()
		{
			item.setEnabled(enabledUI.isChecked());
		}

		/** Action: Code text changes */
		@UIAction
		public void changeCode()
		{
			((UserCodeItem)item).setUserCode(codeUI.getValue());
		}

		/** Action: Code focused */
		@UIAction
		public void focusCode()
		{
			if(codeUI.getValue().equals(ItemCommand.DEFAULTCODE))
				codeUI.selectAll();
		}

		/** Action: Click settings button */
		@UIAction
		public void actionSettings()
		{
			new SettingsDialog(item);
		}

		/** Action: Click delete button */
		@UIAction
		public void actionDelete()
		{
			int value=context.getSingle(UI.class).showQuestion(w,"Confirm delete",
				"Are you sure you want to delete this script item? Deleted items cannot be restored.",
				UI.BUTTON_YES|UI.BUTTON_CANCEL,"Delete item",null,null,UI.BUTTON_CANCEL);
			if(value==UI.BUTTON_YES)
			{
				script.deleteItem(item);
				deleteItem(item);
			}
		}

		/**
		 * Paints item background graphics.
		 * @param g Context
		 * @param left Left pixels
		 * @param top Top pixels
		 * @param width Width in pixels
		 * @param height Height in pixels
		 */
		@UIAction
		public void paintBackground(Graphics2D g, int left, int top,
			int width, int height)
		{
			Color initial=item.getStripeRGB();
			g.setColor(item.hasErrors() ? Color.red : initial);
			g.fillRect(left+4,top,4,height);
			left+=8;
			width-=8;
			initial=item.getNormalStripeRGB();

		  int
		  		rStart=(initial.getRed()+3*255)>>2,
		  		gStart=(initial.getGreen()+3*255)>>2,
		  		bStart=(initial.getBlue()+3*255)>>2;
			for(int x=0;x<width;x+=4)
			{
				int proportion=256-((x<<8)/width);
				g.setColor(new Color(
					(proportion * 255 + (256-proportion) * rStart)>>8,
					(proportion * 255 + (256-proportion) * gStart)>>8,
					(proportion * 255 + (256-proportion) * bStart)>>8
					));
				int w=x+4>width ? width-x : 4;
				g.fillRect(x+left,top,w,height);
			}
		}

	}

	private static class ItemDetails
	{
		Page p;
		ItemCallbacks callbacks;
		public ItemDetails(Page p,ItemCallbacks callbacks)
		{
			this.p=p;
			this.callbacks=callbacks;
		}
	}

	/**
	 * Adds an item to the display (not the script).
	 * @param item New item
	 */
	void addItem(ScriptItem item)
	{
		ItemCallbacks callbacks=new ItemCallbacks(item);
		Page p = context.getSingle(UI.class).createPage(
			"scriptitem", callbacks);
		callbacks.init();
		detailsMap.put(item,new ItemDetails(p,callbacks));
		itemsUI.add(p);
	}

	/**
	 * Deletes an item from the display (not the script).
	 * @param item Item to go
	 */
	void deleteItem(ScriptItem item)
	{
		ItemDetails details=detailsMap.remove(item);
		itemsUI.remove(details.p);
	}

	void focus()
	{
		w.activate();
	}

	/**
	 * Callback: Window is closed.
	 */
	@UIAction
	public void closed()
	{
		script.removeStateListener(this);
		owner.informClosed(script);
	}

	/**
	 * Callback: Window is closing.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void closing() throws GeneralException
	{
		if(script.isChanged())
		{
			int action=context.getSingle(UI.class).showQuestion(
				w,"Confirm close",	"This script has unsaved changes.",
				UI.BUTTON_YES|UI.BUTTON_NO|UI.BUTTON_CANCEL,
				"Save changes","Discard changes",null,UI.BUTTON_YES);
			switch(action)
			{
			case UI.BUTTON_YES: 	actionSave(); break;
			case UI.BUTTON_NO: actionRevert(); break;
			case UI.BUTTON_CANCEL: return;
			}
		}
		// Just close straight off if no changes
		w.close();
	}

	/**
	 * User clicks Save button.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void actionSave() throws GeneralException
	{
		progressUI.setIndeterminate();
		progressUI.setVisible(true);
		revertUI.setEnabled(false);
		saveUI.setEnabled(false);
		enabledUI.setEnabled(false);
		addUI.setEnabled(false);
		for(ItemDetails details :  detailsMap.values())
		{
			details.callbacks.disable();
		}

		script.save(new Script.SaveContinuation()
		{
			@Override
			public void afterSave(boolean success)
			{
				if(!success)
				{
					try
					{
						int action=UI.BUTTON_YES; // Only ask if it's not already disabled
						if(script.isEnabled())
						{
							action=context.getSingle(UI.class).showQuestion(
								w,"Error in script",	"This script contains errors and can only be saved if you disable it.",
								UI.BUTTON_YES|UI.BUTTON_CANCEL,
								"Save and disable",null,null,UI.BUTTON_YES);
						}
						if(action==UI.BUTTON_YES)
						{
							script.saveAndDisable();
						}
					}
					catch(GeneralException ge)
					{
						ErrorMsg.report("Error saving script",ge);
					}
				}
				initWindow();
				owner.informChanged(script);
			}
			@Override
			public void afterSave(Throwable t)
			{
				t.printStackTrace();
				initWindow();
			}
		});
	}

	/**
	 * User clicks Revert button.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void actionRevert() throws GeneralException
	{
		script.load();
		initWindow();
	}

	/**
	 * User clicks Help button.
	 */
	@UIAction
	public void actionHelp()
	{
		owner.actionHelp();
	}

	/**
	 * User clicks Add button.
	 */
	@UIAction
	public void actionAdd()
	{
		new SettingsDialog(null);
	}

	/**
	 * User changes Enabled checkbox.
	 * @throws GeneralException Any error
	 */
	@UIAction
	public void changeEnabled() throws GeneralException
	{
		script.setEnabled(enabledUI.isChecked());
	}

	/**
	 * Item settings dialog.
	 */
	@UIHandler("itemsettings")
	public class SettingsDialog
	{
		private Dialog d;
		/** UI: Item type */
		public Dropdown typeUI;
		/** UI: Settings page */
		public Page settingsUI;
		/** UI: OK button */
		public Button okUI;
		/** UI: Top part of dialog */
		public HorizontalPanel topBitUI;

		private ScriptItem startItem;

		SettingsDialog(ScriptItem item)
		{
			this.startItem=item;
			d = context.getSingle(UI.class).createDialog("itemsettings",this);

			if(startItem!=null)
			{
				d.setTitle("Item settings");
				okUI.setLabel("OK");
				topBitUI.setVisible(false);

				settingsUI.setContents(
					startItem.getPage(okUI));
			}
			else
			{
				// Show list of available types
				for(int i=0;i<itemTypes.length;i++)
				{
					ScriptItem newItem;
					try
					{
						newItem=(ScriptItem)itemTypes[i].getConstructor(Script.class,int.class).
							newInstance(script, script.getItems().length);
					}
					catch(Exception e)
					{
						throw new BugException(e);
					}
					typeUI.addValue(newItem,newItem.getTypeName());
				}
				changeType();
			}

			d.show(w);
		}

		/** Action: User clicks OK. */
		@UIAction
		public void actionOK()
		{
			d.close();
			if(startItem==null)
			{
				ScriptItem newItem=(ScriptItem)typeUI.getSelected();
				newItem.saveSettings();
				script.addItem(newItem);
				addItem(newItem);
			}
			else
			{
				ScriptItem existingItem=startItem;
				existingItem.saveSettings();
				ItemDetails details=detailsMap.get(existingItem);
				details.callbacks.init();
			}
		}

		/** Action: User clicks Cancel. */
		@UIAction
		public void actionCancel()
		{
			d.close();
		}

		/** Action: User changes Type dropdown. */
		@UIAction
		public void changeType()
		{
			settingsUI.setContents(
				((ScriptItem)typeUI.getSelected()).getPage(okUI));
		}
	}

	@Override
	public void scriptStateChanged(Script s)
	{
		saveUI.setEnabled(s.isChanged());
		revertUI.setEnabled(s.isChanged());
		enabledUI.setEnabled(!s.isChanged() && !s.hasErrors());
		enabledUI.setChecked(s.isEnabled());

		if(s.isChanged())
		{
			statusUI.setText(
				script.isEnabled()
					? "Changes to this script will not take effect until you click Save."
					: "This script is disabled. Changes will not take effect until you Save then enable it.");
		}
		else
		{
			statusUI.setText("");
		}
	}
}
