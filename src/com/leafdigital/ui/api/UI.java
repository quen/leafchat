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
package com.leafdigital.ui.api;

import java.awt.Color;
import java.io.File;

import javax.swing.JComponent;

import org.w3c.dom.Document;

import leafchat.core.api.*;

/**
 * Main UI singleton.
 * <p>
 * Access this to create new UI objects or control global UI features.
 * <p>
 * The leafChat UI is, in general, thread-safe.
 */
public interface UI extends Singleton
{
	/** 'Yes' button bitfield constant for showQuestion */
	public final static int BUTTON_YES=1;
	/** 'No' button bitfield constant for showQuestion */
	public final static int BUTTON_NO=2;
	/** 'Cancel' button bitfield constant for showQuestion */
	public final static int BUTTON_CANCEL=4;

	/** UI style: single window 'classic' style */
	public final static int UISTYLE_SINGLEWINDOW=1;
	/** UI style: multi-window style */
	public final static int UISTYLE_MULTIWINDOW=2;
	/** UI style: tabbed */
	public final static int UISTYLE_TABBED=3;

	/**
	 * @param callbacks Object used for callback methods
	 * @return A new Window
	 */
	public Window newWindow(Object callbacks);

	/**
	 * Creates a new Dialog.
	 * <p>
	 * The use of dialogs should be avoided as they are modal; while a dialog is
	 * shown, users cannot access other windows.
	 * @param callbacks Object used for callback methods
	 * @return A new Dialog
	 */
	public Dialog newDialog(Object callbacks);

	/**
	 * Creates a new Page. Pages can be inserted within other windows or dialogs
	 * and have an independent set of IDs.
	 * @param callbacks Object used for callback methods
	 * @return A new Page
	 */
	public Page newPage(Object callbacks);

	/**
	 * @return A new Page (as a widget within an existing dialog)
	 */
	public Page newPage();

	/**
	 * @return A new BorderPanel
	 */
	public BorderPanel newBorderPanel();

	/**
	 * @return A new SplitPanel
	 */
	public SplitPanel newSplitPanel();

	/**
	 * @return A new VerticalPanel
	 */
	public VerticalPanel newVerticalPanel();

	/**
	 * @return A new VerticalPanel
	 */
	public HorizontalPanel newHorizontalPanel();

	/**
	 * @return A new GroupPanel
	 */
	public GroupPanel newGroupPanel();

	/**
	 * @return A new TabPanel
	 */
	public TabPanel newTabPanel();

	/**
	 * @return A new ChoicePanel
	 */
	public ChoicePanel newChoicePanel();

	/**
	 * @return A new ScrollPanel
	 */
	public ScrollPanel newScrollPanel();

	/**
	 * @return A new DecoratedPanel
	 */
	public DecoratedPanel newDecoratedPanel();

	/**
	 * @return A new ButtonPanel
	 */
	public ButtonPanel newButtonPanel();

	/**
	 * @return A new Label
	 */
	public Label newLabel();

	/**
	 * @return A new Image
	 */
	public Pic newPic();

	/**
	 * @return A new TextView
	 */
	public TextView newTextView();

	/**
	 * @return A new Button
	 */
	public Button newButton();

	/**
	 * @return A new RadioButton
	 */
	public RadioButton newRadioButton();

	/**
	 * @return A new CheckBox
	 */
	public CheckBox newCheckBox();

	/**
	 * @return A new dropdown list
	 */
	public Dropdown newDropdown();

	/**
	 * @return A new EditBox
	 */
	public EditBox newEditBox();

	/**
	 * @return A new EditArea
	 */
	public EditArea newEditArea();

	/**
	 * @return A new ListBox
	 */
	public ListBox newListBox();

	/**
	 * @return A new TreeBox
	 */
	public TreeBox newTreeBox();

	/**
	 * @return A new Table
	 */
	public Table newTable();

	/**
	 * @return A new Progress
	 */
	public Progress newProgress();

	/**
	 * @return A new Spacer
	 */
	public Spacer newSpacer();

	/**
	 * @return A new PopupMenu
	 */
	public PopupMenu newPopupMenu();

	/**
	 * Creates a widget wrapper for a JComponent.
	 * <p>
	 * Though leafChat UI is thread-safe, this of course does not apply to the
	 * Swing component. Be careful when calling methods on the Swing component.
	 * @param c Swing component
	 * @return Widget wrapping the JComponent
	 */
	public Widget newJComponentWrapper(JComponent c);

	/**
	 * Creates a Widget from an XML document.
	 * @param d XML widget document
	 * @param owner Window that maintains a list of IDs for the created widgets
	 * @return Top-level widget
	 * @throws BugException If the XML document is invalid
	 */
	public Widget createWidget(Document d, WidgetOwner owner);

	/**
	 * Creates a Window from an XML document.
	 * @param d XML document describing the window
	 * @param callbacks Object used for callback methods
	 * @return Window object
	 * @throws BugException If the XML document is invalid
	 * @deprecated Use {@link #createWindow(String, Object)} instead
	 */
	public Window createWindow(Document d, Object callbacks);

	/**
	 * Creates a Window from an XML document. The XML document will be loaded
	 * relative to the given callback object.
	 * @param xml Name of XML file without the '.xml' at the end
	 * @param callbacks Callback object, which must have {@link UIHandler}
	 *   annotation including this xml file
	 * @return Window object
	 * @throws BugException If the XML document is invalid or can't be loaded
	 */
	public Window createWindow(String xml, Object callbacks);

	/**
	 * Creates a Page from an XML document.
	 * @param d XML document describing the page
	 * @param callbacks Object used for callback methods
	 * @return Page object
	 * @throws BugException If the XML document is invalid
	 * @deprecated Use {@link #createPage(String, Object)} instead
	 */
	public Page createPage(Document d, Object callbacks);

	/**
	 * Creates a Page from an XML document.
	 * @param xml Name of XML file without the '.xml' at the end
	 * @param callbacks Callback object, which must have {@link UIHandler}
	 *   annotation including this xml file
	 * @return Page object
	 * @throws BugException If the XML document is invalid
	 */
	public Page createPage(String xml, Object callbacks);

	/**
	 * Creates a Dialog from an XML document.
	 * <p>
	 * The use of dialogs should be avoided as they are modal; while a dialog is
	 * shown, users cannot access other windows.
	 * @param d XML document describing the dialog
	 * @param callbacks Object used for callback methods
	 * @return Dialog object
	 * @throws BugException If the XML document is invalid
	 * @deprecated Use {@link #createDialog(String, Object)} instead
	 */
	public Dialog createDialog(Document d, Object callbacks);

	/**
	 * Creates a Dialog from an XML document.
	 * <p>
	 * The use of dialogs should be avoided as they are modal; while a dialog is
	 * shown, users cannot access other windows.
	 * @param xml Name of XML file without the '.xml' at the end
	 * @param callbacks Callback object, which must have {@link UIHandler}
	 *   annotation including this xml file
	 * @return Dialog object
	 * @throws BugException If the XML document is invalid
	 */
	public Dialog createDialog(String xml, Object callbacks);

	/**
	 * Show a yes/no/cancel confirm dialog.
	 * @param parent Parent that dialog will be aligned with
	 * @param title Dialog title
	 * @param message Message in XML
	 * @param buttons Bitfield e.g. BUTTON_YES|BUTTON_CANCEL
	 * @param yesLabel Label for Yes button (null = Yes)
	 * @param noLabel Label for No button (null = No)
	 * @param cancelLabel Label for Cancel button (null = Cancel)
	 * @param defaultButton Default button BUTTON_xx constant
	 * @return BUTTON_xx constant
	 */
	public int showQuestion(WidgetOwner parent, String title, String message,
		int buttons, 	String yesLabel, String noLabel, String cancelLabel, int defaultButton);

	/**
	 * Shows a dialog with don't-show-this-again feature. If don't-show-this-again
	 * has been selected, the function will immediately return with the relevant
	 * value. (Cancelling does not enable don't-show, but yes or no do.)
	 * @param prefID ID used to disable appearance
	 * @param parent Parent that dialog will be aligned with
	 * @param title Dialog title
	 * @param message Message in XML
	 * @param buttons Bitfield e.g. BUTTON_YES|BUTTON_CANCEL
	 * @param yesLabel Label for Yes button (null = Yes)
	 * @param noLabel Label for No button (null = No)
	 * @param cancelLabel Label for Cancel button (null = Cancel)
	 * @param defaultButton Default button BUTTON_xx constant
	 * @return Result of user's choice (BUTTON_xx constant)
	 */
	public int showOptionalQuestion(String prefID, WidgetOwner parent, String title, String message,
		int buttons, String yesLabel, String noLabel, String cancelLabel, int defaultButton);

	/**
	 * Displays an alert dialog indicating some kind of error, with an OK button
	 * @param parent Parent that dialog will be aligned with
	 * @param title Dialog title
	 * @param message Message in XML
	 */
	public void showUserError(WidgetOwner parent, String title, String message);

	/**
	 * Shows a file-select dialog as appropriate for the platform.
	 * @param parent Parent that dialog will be aligned with, or null for default
	 * @param title Title of dialog
	 * @param saveMode True for save, false for load (in save mode, the
	 *   user will be asked to confirm overwrite if they select a file that
	 *   exists, and the confirm prompt will change to 'Save' instead of 'Open')
	 * @param folder Initial folder or null for none
	 * @param file Initial file or null for none (overrides folder)
	 * @param extensions Array of supported extensions in the form ".jpg" (not
	 *  case-sensitive) or null to show all files
	 * @param filterName Name describing supported extensions e.g. 'Image files' or null for default
	 * @return Selected file or null if the user cancelled
	 */
	public File showFileSelect(WidgetOwner parent, String title, boolean saveMode, File folder, File file, final String[] extensions, String filterName);

	/**
	 * Shows a folder-select dialog as appropriate for the platform.
	 * @param parent Parent that dialog will be aligned with, or null for default
	 * @param title Title of dialog
	 * @param folder Initial folder or null for none
	 * @return Selected folder or null if the user cancelled
	 */
	public File showFolderSelect(WidgetOwner parent, String title, File folder);

	/**
	 * Shows a colour-select dialog.
	 * @param parent Parent that dialog will be aligned with, or null for default
	 * @param title Title of dialog
	 * @param original Initial colour
	 * @return New colour or null if the user cancelled
	 */
	public Color showColourSelect(WidgetOwner parent, String title, Color original);

	/**
	 * Registers a new Tool that will be placed on the toolbar.
	 * @param t SimpleTool or ComplexTool implementor
	 * @throws BugException If there is any problem registering the tool
	 */
	public void registerTool(Tool t);

	/**
	 * Unregisters an existing Tool and removes it from the toolbar.
	 * @param t Tool to remove
	 */
	public void unregisterTool(Tool t);

	/**
	 * Tools can call this command to make the system resize the toolbar as
	 * appropriate given changed content.
	 */
	public void resizeToolbar();

	/**
	 * Obtains the user's selected theme.
	 * @return Theme
	 */
	public Theme getTheme();

	/**
	 * Obtains the list of available themes.
	 * @return List of themes
	 */
	public Theme[] getAvailableThemes();

	/**
	 * Sets the selected theme (should be called by preferences, which for
	 * technical reasons has to be in a different plugin).
	 * @param t New theme
	 */
	public void setTheme(Theme t);

	/**
	 * Refreshes the current theme to take account of user style changes
	 * (userstyle.css).
	 */
	public void refreshTheme();

	/**
	 * Obtains the folder used for themes.
	 * @param system If true, returns the system theme folder, otherwise
	 *  returns the per-user theme folder
	 * @return Theme folder; the user theme folder is guaranteed to exist
	 *  after this call, but the system theme folder might not
	 */
	public File getThemeFolder(boolean system);

	/**
	 * Installs a new theme file into the user's theme folder, checking it for
	 * errors.
	 * @param newTheme New theme file
	 * @throws GeneralException If there is an error with the theme or with
	 *  copying
	 */
	public void installUserTheme(File newTheme) throws GeneralException;

	/**
	 * Changes the current UI style and remembers it in preferences.
	 * @param uiStyle One of the UISTYLE_xx constants
	 */
	public void setUIStyle(int uiStyle);

	/** @return Current UI style */
	public int getUIStyle();

	/**
	 * Runs a task in the user-interface/event thread.
	 * @param r Task to run
	 */
	public void runInThread(Runnable r);

	/**
	 * @return True if the application (or one of its windows) is currently
	 *  active
	 */
	public boolean isAppActive();

	/**
	 * Activates the app. (If the app is minimised to tray, this method must be
	 * called to make it visible again.)
	 */
	public void activate();

	/**
	 * Switches to the window with most recent changes (if any window has
	 * changes).
	 */
	public void showLatest();
}
