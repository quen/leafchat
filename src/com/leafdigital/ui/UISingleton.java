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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

import javax.swing.*;

import org.w3c.dom.*;

import util.*;
import util.xml.*;

import com.leafdigital.prefs.api.*;
import com.leafdigital.ui.api.*;
import com.leafdigital.ui.api.Button;
import com.leafdigital.ui.api.Dialog;
import com.leafdigital.ui.api.Label;
import com.leafdigital.ui.api.Window;

import leafchat.core.api.*;

/**
 * User interface factory, creates UI objects
 */
public class UISingleton implements UI
{
	/** Name of themes folder */
	private static final String THEME_FOLDER = "themes";

	/** Extension used for theme files */
	private static final String THEME_EXTENSION = ".leafChatTheme";

	/** Pattern matching RGB options */
	private final static Pattern RGB = Pattern.compile("[0-9]+,[0-9]+,[0-9]+");

	/** Field with a number at the end */
	private static final Pattern ARRAYFIELD = Pattern.compile("(.*)([0-9]+)");

	/** List of WeakReferences to ThemeListeners */
	private LinkedList<WeakReference<ThemeListener>> themeListeners =
		new LinkedList<WeakReference<ThemeListener>>();

	/** One and only internaldesktop */
	private InternalDesktop desktop = null;

	/** Context */
	private PluginContext context;

	/** Toolbar */
	private ToolBar toolbar = new ToolBar(this);

	/** Main frame */
	private JFrame f;

	/** Switch bar (null if not used) */
	private SwitchBar sb;

	/** Tabs panel (null if not used) */
	private JPanel tabs;

	/** Layout manager for tabs */
	private CardLayout tabsLayout;

	private boolean skipNextActivation = true; // Skip first activation
	private long skipActiveUntil = 0;

	private int focusRunning = 0;

	/** Currently-selected theme */
	private ThemeImp currentTheme;

	/** Default shared theme */
	private ThemeImp sharedTheme;

	/** Current UI style */
	private int uiStyle = UISTYLE_SINGLEWINDOW;

	/** List of open windows */
	private LinkedList<WindowImp> windows = new LinkedList<WindowImp>();

	/**
	 * @param context Plugin context for UI plugin
	 * @throws BugException
	 */
	public UISingleton(PluginContext context)
	{
		this.context = context;

		// Init theme
		Theme[] themes = getAvailableThemes();
		if(themes.length == 0)
		{
			throw new BugException("No themes available");
		}
		for(int i=0; i<themes.length; i++)
		{
			if(((ThemeImp)themes[i]).getFile().getName().equals(
				"shared" + THEME_EXTENSION))
			{
				sharedTheme = (ThemeImp)themes[i];
				break;
			}
		}
		if(sharedTheme==null)
		{
			throw new BugException("Can't find shared theme");
		}

		Preferences p = context.getSingle(Preferences.class);
		PreferencesGroup pg = p.getGroup(context.getPlugin());
		String themeID = pg.get("theme", "leaves");
		for(int i=0; i<themes.length; i++)
		{
			if(((ThemeImp)themes[i]).getFile().getName().equals(
				themeID + THEME_EXTENSION))
			{
				currentTheme = (ThemeImp)themes[i];
				if(currentTheme != sharedTheme)
				{
					currentTheme.setParent(sharedTheme);
				}
				break;
			}
		}

		// Init UI style
		String style = pg.get(UIPrefs.PREF_UISTYLE, UIPrefs.PREFDEFAULT_UISTYLE);
		uiStyle = UISTYLE_SINGLEWINDOW;
		if(style.equals(UIPrefs.PREFVALUE_UISTYLE_SEPARATE))
		{
			uiStyle = UISTYLE_MULTIWINDOW;
		}
		else if(style.equals(UIPrefs.PREFVALUE_UISTYLE_TABS))
		{
			uiStyle = UISTYLE_TABBED;
		}
	}

	@Override
	public Window newWindow(Object oCallbacks)
	{
		return (new WindowImp(this, oCallbacks)).getInterface();
	}

	@Override
	public Dialog newDialog(Object oCallbacks)
	{
		return (new DialogImp(this, oCallbacks)).getInterface();
	}

	@Override
	public Page newPage(Object oCallbacks)
	{
		return (new PageImp(this, oCallbacks)).getInterface();
	}

	@Override
	public Page newPage()
	{
		return (new PageImp(this)).getInterface();
	}

	@Override
	public BorderPanel newBorderPanel()
	{
		BorderPanel l = (new BorderPanelImp()).getInterface();
		((InternalWidget)l).setUI(this);
		return l;
	}

	@Override
	public SplitPanel newSplitPanel()
	{
		SplitPanel l = (new SplitPanelImp()).getInterface();
		((InternalWidget)l).setUI(this);
		return l;
	}

	@Override
	public VerticalPanel newVerticalPanel()
	{
		VerticalPanel l = (new VerticalPanelImp()).getInterface();
		((InternalWidget)l).setUI(this);
		return l;
	}

	@Override
	public HorizontalPanel newHorizontalPanel()
	{
		HorizontalPanel l = (new HorizontalPanelImp()).getInterface();
		((InternalWidget)l).setUI(this);
		return l;
	}

	@Override
	public GroupPanel newGroupPanel()
	{
		GroupPanel l = (new GroupPanelImp()).getInterface();
		((InternalWidget)l).setUI(this);
		return l;
	}

	@Override
	public TabPanel newTabPanel()
	{
		TabPanel l = (new TabPanelImp()).getInterface();
		((InternalWidget)l).setUI(this);
		return l;
	}

	@Override
	public ChoicePanel newChoicePanel()
	{
		ChoicePanel l = (new ChoicePanelImp()).getInterface();
		((InternalWidget)l).setUI(this);
		return l;
	}

	@Override
	public ScrollPanel newScrollPanel()
	{
		ScrollPanel l = (new ScrollPanelImp()).getInterface();
		((InternalWidget)l).setUI(this);
		return l;
	}

	@Override
	public DecoratedPanel newDecoratedPanel()
	{
		DecoratedPanel l = (new DecoratedPanelImp()).getInterface();
		((InternalWidget)l).setUI(this);
		return l;
	}

	@Override
	public ButtonPanel newButtonPanel()
	{
		ButtonPanel l = (new ButtonPanelImp()).getInterface();
		((InternalWidget)l).setUI(this);
		return l;
	}

	@Override
	public Dropdown newDropdown()
	{
		Dropdown l = (new DropdownImp()).getInterface();
		((InternalWidget)l).setUI(this);
		return l;
	}

	@Override
	public com.leafdigital.ui.api.PopupMenu newPopupMenu()
	{
		return (new PopupMenuImp()).getInterface();
	}

	@Override
	public Widget newJComponentWrapper(JComponent c)
	{
		Widget l = new JComponentWrapper(c);
		((InternalWidget)l).setUI(this);
		return l;
	}

	@Override
	public Button newButton()
	{
		Button l = (new ButtonImp()).getInterface();
		((InternalWidget)l).setUI(this);
		return l;
	}

	@Override
	public RadioButton newRadioButton()
	{
		RadioButton l = (new RadioButtonImp()).getInterface();
		((InternalWidget)l).setUI(this);
		return l;
	}

	@Override
	public CheckBox newCheckBox()
	{
		CheckBox l = (new CheckBoxImp()).getInterface();
		((InternalWidget)l).setUI(this);
		return l;
	}

	@Override
	public Label newLabel()
	{
		Label l = (new LabelImp()).getInterface();
		((InternalWidget)l).setUI(this);
		return l;
	}

	@Override
	public Pic newPic()
	{
		Pic p = new PicImp(this).getInterface();
		((InternalWidget)p).setUI(this);
		return p;
	}

	@Override
	public TextView newTextView()
	{
		TextView l = (new TextViewImp(this)).getInterface();
		((InternalWidget)l).setUI(this);
		return l;
	}

	@Override
	public EditBox newEditBox()
	{
		EditBox l = (new EditBoxImp(this)).getInterface();
		((InternalWidget)l).setUI(this);
		return l;
	}

	@Override
	public EditArea newEditArea()
	{
		EditArea l = (new EditAreaImp(this)).getInterface();
		((InternalWidget)l).setUI(this);
		return l;
	}

	@Override
	public ListBox newListBox()
	{
		ListBox l = (new ListBoxImp(this)).getInterface();
		((InternalWidget)l).setUI(this);
		return l;
	}

	@Override
	public TreeBox newTreeBox()
	{
		TreeBox l = (new TreeBoxImp()).getInterface();
		((InternalWidget)l).setUI(this);
		return l;
	}

	@Override
	public Table newTable()
	{
		Table l = (new TableImp()).getInterface();
		((InternalWidget)l).setUI(this);
		return l;
	}

	@Override
	public Progress newProgress()
	{
		Progress l = (new ProgressImp()).getInterface();
		((InternalWidget)l).setUI(this);
		return l;
	}

	@Override
	public Spacer newSpacer()
	{
		Spacer l = (new SpacerImp()).getInterface();
		((InternalWidget)l).setUI(this);
		return l;
	}

	/**
	 * @return Single internal desktop
	 */
	InternalDesktop getInternalDesktop()
	{
		return desktop;
	}

	void informInactiveWindow(WindowImp wi)
	{
		skipActiveUntil = System.currentTimeMillis() + 100L;
	}

	/**
	 * Requests that the given component is focused. The actual focus happens
	 * in invokeLater. If another focus request is received first, the first
	 * focus will never happen - this helps prevent irritating loops.
	 * @param c Component to focus
	 */
	public void focus(final JComponent c)
	{
		runInSwing(new Runnable()
		{
			@Override
			public void run()
			{
				focusRunning++;
				SwingUtilities.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						focusRunning--;
						if(focusRunning==0)
						{
							c.requestFocus();
						}
					}
				});
			}
		});
	}

	/**
	 * Called when the main frame has been minimized.
	 */
	private void minimized()
	{
		// Do we need to minimise to tray?
		Preferences p = context.getSingle(Preferences.class);
		PreferencesGroup group = p.getGroup(p.getPluginOwner(context.getPlugin()));
		if(group.get(UIPrefs.PREF_MINIMISE_TO_TRAY,
			UIPrefs.PREFDEFAULT_MINIMISE_TO_TRAY).equals("t"))
		{
			// If so, hide minimised window
			f.setVisible(false);
		}
	}

	@Override
	public void activate()
	{
		runInSwing(new Runnable()
		{
			@Override
			public void run()
			{
				f.setVisible(true);
				f.setExtendedState(Frame.NORMAL);
				if (!f.isActive())
				{
					// This is necessary to bring the window to front. You can't do
					// f.bringToFront; that doesn't work on any platform because the
					// platforms decided to block it (which was a pretty stupid idea
					// because although it's bad practice in some cases, not in response
					// to a user request it isn't, and the platform can't tell that).
					f.setVisible(false);
					f.setVisible(true);
				}
			}
		});
	}

	@Override
	public void showLatest()
	{
		runInSwing(new Runnable()
		{
			@Override
			public void run()
			{
				// Find most recently changed window
				long bestTime = 0;
				WindowImp bestWindow = null;
				for(WindowImp window : windows)
				{
					long time = window.getHolder().getAttentionTime();
					if(time > bestTime)
					{
						bestTime = time;
						bestWindow = window;
					}
				}
				// Focus it
				if(bestWindow != null)
				{
					bestWindow.getHolder().focusFrame();
				}
			}
		});
	}

	/**
	 * Create the application main window.
	 * @param sAppTitle Title for window
	 */
	public void init(String sAppTitle)
	{
		if(desktop!=null) return;

 		f = new JFrame(sAppTitle);
 		f.addWindowListener(new WindowAdapter()
 		{
			@Override
			public void windowActivated(WindowEvent e)
			{
				if(skipNextActivation)
				{
					skipNextActivation = false;
					return;
				}
				if(System.currentTimeMillis()<skipActiveUntil)
					return;
				if(e.getOppositeWindow()==null && uiStyle==UISTYLE_MULTIWINDOW)
				{
					// Focus all the other windows too when somebody focuses this
					// one.
					synchronized(UISingleton.this)
					{
						for(WindowImp wi : windows)
						{
							wi.getInterface().activate();
						}
					}
					skipNextActivation = true;
					f.toFront();
				}
			}
			@Override
			public void windowClosing(WindowEvent e)
			{
				// Send app shutdown message
				SystemStateMsg.sendShutdown();
			}
			@Override
			public void windowIconified(WindowEvent e)
			{
				minimized();
			}
 		});

		sb = new SwitchBar(this);
		desktop = new InternalDesktop(sb, toolbar);
		tabsLayout = new CardLayout();
		tabs = new JPanel(tabsLayout);
		sb.setUIStyle(uiStyle);

		f.getContentPane().setLayout(new BorderLayout());

		Preferences p = context.getSingle(Preferences.class);
		final PreferencesGroup mainWindow = p.getGroup(context.getPlugin()).getChild(UIPlugin.PREFGROUP_MAINWINDOW);
		Dimension size = new Dimension(
			p.toInt(mainWindow.get(UIPlugin.PREF_WIDTH, UIPlugin.PREFDEFAULT_WIDTH)),
			p.toInt(mainWindow.get(UIPlugin.PREF_HEIGHT, UIPlugin.PREFDEFAULT_HEIGHT)			));
		Point position = new Point(
			p.toInt(mainWindow.get(UIPlugin.PREF_X, UIPlugin.PREFDEFAULT_X)),
			p.toInt(mainWindow.get(UIPlugin.PREF_Y, UIPlugin.PREFDEFAULT_Y)			));
		if(position.x==-1 || position.y==-1)
		{
			if(uiStyle==UISTYLE_MULTIWINDOW)
			{
				position.x = 0;
				position.y = 0;
			}
			else
			{
				// Size has not been stored yet, so try to centre
				Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
				size.width = Math.min(size.width, screenSize.width);
				size.height = Math.min(size.height, screenSize.height);
				position.x = (screenSize.width-size.width) / 2 + screenSize.x;
				position.y = (screenSize.height-size.height) / 2 + screenSize.y;
			}
		}

		switch(uiStyle)
		{
		case UISTYLE_SINGLEWINDOW:
			f.getContentPane().add(desktop, BorderLayout.CENTER);
			f.getContentPane().add(sb, BorderLayout.SOUTH);
			f.getContentPane().add(toolbar, BorderLayout.NORTH);
			f.setLocation(position);
			f.setSize(size);
			break;
		case UISTYLE_MULTIWINDOW:
			f.getContentPane().add(toolbar, BorderLayout.NORTH);
			f.pack();
			f.setLocation(position);
			f.setResizable(false);
			break;
		case UISTYLE_TABBED:
			f.getContentPane().add(toolbar, BorderLayout.NORTH);
			JPanel tabsHolder = new JPanel(new BorderLayout());
			tabsHolder.add(sb, BorderLayout.NORTH);
			tabsHolder.add(tabs, BorderLayout.CENTER);
			f.getContentPane().add(tabsHolder, BorderLayout.CENTER);
			f.setLocation(position);
			f.setSize(size);
			break;
		}

		f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		f.setVisible(true);
		f.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentMoved(ComponentEvent e)
			{
				mainWindow.set(UIPlugin.PREF_X, "" + f.getLocation().x);
				mainWindow.set(UIPlugin.PREF_Y, "" + f.getLocation().y);
			}
			@Override
			public void componentResized(ComponentEvent e)
			{
				if(uiStyle==UISTYLE_MULTIWINDOW) return;
				mainWindow.set(UIPlugin.PREF_WIDTH, "" + f.getSize().width);
				mainWindow.set(UIPlugin.PREF_HEIGHT, "" + f.getSize().height);
			}
		});

		initDefaultIcon(f);
	}

	void initDefaultIcon(Frame f)
	{
		try
		{
			// Don't set on Mac, it makes minimise view ugly
			if(PlatformUtils.isMac()) return;

			if(PlatformUtils.isJavaVersionAtLeast(1, 6))
			{
				LinkedList<Image> iconImages = new LinkedList<Image>();
				iconImages.add(GraphicsUtils.loadImage(getClass().getResource("icon48.png")));
				iconImages.add(GraphicsUtils.loadImage(getClass().getResource("icon32.png")));
				iconImages.add(GraphicsUtils.loadImage(getClass().getResource("icon16.png")));

				JFrame.class.getMethod("setIconImages", new Class[] { List.class }).invoke(
					f, new Object[] { iconImages });
			}
			else
			{
				// Java 1.4 version
				f.setIconImage(GraphicsUtils.loadImage(getClass().getResource("icon48.png")));
			}
		}
		catch(Exception e)
		{
			throw new BugException(e);
		}
	}

	/**
	 * Creates a frame for the contents and makes them visible.
	 * @param wi New contents that need a window
	 * @param initialScreen True if initial pos is a screen location not window relative
	 * @param initial Initial position (null if unknown)
	 * @param minimized True to start minimized
	 */
	protected void showFrameContents(WindowImp wi, boolean initialScreen, Point initial, boolean minimized)
	{
		switch(uiStyle)
		{
		case UISTYLE_SINGLEWINDOW:
			new FrameInside(desktop, wi, initialScreen, initial, minimized);
			break;
		case UISTYLE_MULTIWINDOW:
			if(initialScreen)
				new FrameOutside(wi, initial);
			else
				new FrameOutside(wi);
			break;
		case UISTYLE_TABBED:
			new FrameTab(this, wi, initialScreen ? initial : null, !minimized);
			break;
		}
	}

	/** Throws error if thread is not Swing thread */
	public static void checkSwing()
	{
		if(!SwingUtilities.isEventDispatchThread())
		 throw new Error("Must be in Swing thread");
	}

	/**
	 * Run something in Swing, either right away or with invokeLater.
	 * @param r Runnable
	 */
	public static void runInSwing(Runnable r)
	{
		if(SwingUtilities.isEventDispatchThread())
		{
		 r.run();
		}
		else
		{
		 SwingUtilities.invokeLater(r);
		}
	}

	@Override
	public Widget createWidget(Document d, WidgetOwner wOwner)
	{
		return createWidget(d.getDocumentElement(), wOwner);
	}

	@Override
	public Window createWindow(Document d, Object callbacks)
	{
		// Check document
		Element el = d.getDocumentElement();
		if(!el.getTagName().equals("Window"))
		 throw new BugException("Window document must have <Window> as root");

		// Create window and set properties
		Window w = newWindow(callbacks);
		invokeSetMethods(el, w, "Window: ", callbacks);

		// Create window contents
		Element[] children = XML.getChildren(el);
		if(children.length!=1)
		{
			throw new BugException("Window tag must contain single child");
		}

		w.setContents(children[0]);

		((InternalWidgetOwner)w).markCreated();

		return w;
	}

	@Override
	public Window createWindow(String xml, Object callbacks)
	{
		checkUIHandler(xml, callbacks);
		try
		{
			Document d = XML.parse(callbacks.getClass().getResourceAsStream(
				xml + ".xml"));
			return createWindow(d, callbacks);
		}
		catch(XMLException e)
		{
			throw new BugException(e);
		}
	}

	@Override
	public Page createPage(Document d, Object callbacks)
	{
		// Check document
		Element el = d.getDocumentElement();
		if(!el.getTagName().equals("Page"))
		 throw new BugException("Page document must have <Page> as root");

		// Create page and set properties
		Page p = newPage(callbacks);
		invokeSetMethods(el, p, "Page: ", callbacks);

		// Create page contents
		Element[] children = XML.getChildren(el);
		if(children.length!=1)
		 throw new BugException("Page tag must contain single child");

		p.setContents(children[0]);

		((InternalWidgetOwner)p).markCreated();

		return p;
	}

	@Override
	public Page createPage(String xml, Object callbacks)
	{
		checkUIHandler(xml, callbacks);
		try
		{
			Document d = XML.parse(callbacks.getClass().getResourceAsStream(
				xml + ".xml"));
			return createPage(d, callbacks);
		}
		catch(XMLException e)
		{
			throw new BugException(e);
		}
	}

	@Override
	public Dialog createDialog(Document d, Object callbacks)
	{
		// Check document
		Element el = d.getDocumentElement();
		if(!el.getTagName().equals("Dialog"))
			throw new BugException("Dialog document must have <Dialog> as root");

		// Create dialog and set properties
		Dialog newDialog = newDialog(callbacks);
		invokeSetMethods(el, newDialog, "Dialog: ", callbacks);

		// Create window contents
		Element[] children = XML.getChildren(el);
		if(children.length!=1)
			throw new BugException("Dialog tag must contain single child");

		newDialog.setContents(children[0]);

		((InternalWidgetOwner)newDialog).markCreated();

		return newDialog;
	}

	@Override
	public Dialog createDialog(String xml, Object callbacks)
	{
		checkUIHandler(xml, callbacks);
		try
		{
			Document d = XML.parse(callbacks.getClass().getResourceAsStream(
				xml + ".xml"));
			return createDialog(d, callbacks);
		}
		catch(XMLException e)
		{
			throw new BugException(e);
		}
	}

	/**
	 * Checks that the given callback object includes annotation for the given
	 * XML name.
	 * <p>
	 * Note: This does not break backward compatibility because it is only
	 * called for the new versions of {@link #createWindow(String, Object)}
	 * etc. and not the older deprecated ones.
	 * @param xml Name of xml file without ".xml"
	 * @param callbacks Object that contains callbacks
	 * @throws BugException If it doesn't
	 */
	private void checkUIHandler(String xml, Object callbacks) throws BugException
	{
		// Check annotation
		UIHandler annotation = callbacks.getClass().getAnnotation(UIHandler.class);
		if(annotation == null)
		{
			throw new BugException("Callback class must be marked with @UIHandler(\""
				+ xml + "\")");
		}
		boolean ok = false;
		for(String value : annotation.value())
		{
			if(value.equals(xml))
			{
				ok = true;
				break;
			}
		}
		if(!ok)
		{
			throw new BugException("Callback class @UIHandler must list \""
				+ xml + "\"");
		}
	}

	/**
	 * @param e XML widget document element
	 * @param wOwner Window that maintains a list of IDs for the created widgets
	 * @return Top-level widget
	 * @throws BugException If the XML document is invalid
	 */
	Widget createWidget(Element e, WidgetOwner wOwner)
	{
		// Create tree of widgets first; then run all set methods (in case the
		// set methods depend on relationships with other widgets)
		List<SetMethods> sets = new LinkedList<SetMethods>();
		Widget w = createWidgetInner(e, wOwner, sets);
		for(SetMethods sm : sets)
		{
			sm.invoke();
		}
		return w;
	}

	private class SetMethods
	{
		private Element e;
		private InternalWidget iw;
		private String sErrorPrefix;
		private Object callbacks;

		SetMethods(Element e, InternalWidget iw, String sErrorPrefix, Object callbacks)
		{
			this.e = e; this.iw = iw; this.sErrorPrefix = sErrorPrefix; this.callbacks = callbacks;
		}

		void invoke()
		{
			invokeSetMethods(e, iw, sErrorPrefix, callbacks);
		}
	}

	/**
	 * @param e XML widget document element
	 * @param owner Window that maintains a list of IDs for the created widgets
	 * @param setMethods List of set methods
	 * @return Top-level widget
	 * @throws BugException If the XML document is invalid
	 */
	private Widget createWidgetInner(Element e, WidgetOwner owner,
		List<SetMethods> setMethods)
	{
		// Get ID if present
		String sID=e.getAttribute("id");
		if(sID!=null && sID.equals("")) sID=null;

		// Name used in error messages
		String sReportName = "<" + e.getTagName() + ">";
		if(sID!=null) sReportName += " [" + sID + "]";

		InternalWidget iw;
		try
		{
			// Element name maps to a newThing() method, if it's valid
			Method m = getClass().getDeclaredMethod("new" + e.getTagName(), new Class[0]);
			iw = (InternalWidget)m.invoke(this, new Object[0]);
		}
		catch(NoSuchMethodException nsme)
		{
			throw new BugException(sReportName + ": Not a valid widget tag");
		}
		catch(InvocationTargetException ite)
		{
			throw new BugException(sReportName + ": Error instantiating widget", ite.getCause());
		}
		catch(IllegalAccessException iae)
		{
			throw new BugException(sReportName + ": Unexpected error instantiating widget", iae);
		}
		catch (IllegalArgumentException iae)
		{
			throw new BugException(
				sReportName + ": Unexpected error instantiating widget", iae);
		}

		// Initialise widget
		iw.setUI(this);
		iw.setOwner(owner);
		if(sID!=null) iw.setID(sID);

		// Use attributes to invoke set methods
		setMethods.add(new SetMethods(e, iw, sReportName + ": ",
			((CallbackHandlerImp)owner.getCallbackHandler()).getCallbackObject()));

		// Get list of reserved (non-slot) child elements
		Set<String> reserved = new HashSet<String>();
		String[] asReserved = iw.getReservedChildren();
		for(int i=0; i<asReserved.length; i++)
		{
			reserved.add(asReserved[i]);
		}
		List<Element> reservedElements = new LinkedList<Element>();

		// Child elements count as slots, within which other widgets are placed,
		// unless widget has the SINGLESLOT or NAMELESSSLOTS property
		int contentType = iw.getContentType();

		Element[] children = XML.getChildren(e);

		// Extract reserved elements
		for(int childIndex=0; childIndex<children.length; childIndex++)
		{
			// Get slotname and widget tag
			String sSlot = children[childIndex].getTagName();
			if(reserved.contains(sSlot))
			{
				reservedElements.add(children[childIndex]);
				children[childIndex]=null;
			}
		}

		switch(contentType)
		{
			case InternalWidget.CONTENT_NONE:
				if(children.length!=reservedElements.size())
					throw new BugException(sReportName + ": may not contain other components");
				break;
			case InternalWidget.CONTENT_SINGLE:
				if(children.length>1)
					throw new BugException(sReportName + ": must contain at most one component");
				// Fall through
			case InternalWidget.CONTENT_UNNAMEDSLOTS:
				for(int childIndex = 0; childIndex<children.length; childIndex++)
				{
					if(children[childIndex]==null) 	continue;

					// Construct widget and add to this one
					Widget wChild = createWidgetInner(children[childIndex], owner, setMethods);
					try
					{
						((InternalWidget)wChild).setParent(iw);
						iw.addXMLChild(null, wChild);
					}
					catch(BugException be)
					{
						// Add widget identifier to any error messages
						throw new BugException(sReportName + ": " + be.getMessage());
					}
				}
				break;
			case InternalWidget.CONTENT_NAMEDSLOTS:
				for(int childIndex = 0; childIndex<children.length; childIndex++)
				{
					if(children[childIndex]==null) 	continue;

					// Get slotname and widget tag
					String sSlot = children[childIndex].getTagName();
					Element[] aeWidgets = XML.getChildren(children[childIndex]);
					if(aeWidgets.length!=1)
					 throw new BugException(sReportName + ": " + sSlot +
							" must contain precisely one widget");

					// Construct widget and add to this one
					Widget wChild = createWidgetInner(aeWidgets[0], owner, setMethods);
					try
					{
						((InternalWidget)wChild).setParent(iw);
						iw.addXMLChild(sSlot, wChild);
					}
					catch(BugException be)
					{
						// Add widget identifier to any error messages
						throw new BugException(sReportName + ": " + be.getMessage());
					}
				}
				break;
		}

		// Call with any reserved children
		if(!reserved.isEmpty())
		{
			try
			{
				iw.setReservedData(
					reservedElements.toArray(new Element[reservedElements.size()]));
			}
			catch(BugException be)
			{
				// Add widget identifier to any error messages
				throw new BugException(sReportName + ": " + be.getMessage());
			}
		}

		// Remember widget within owner, and return it
		if(sID!=null) owner.setWidgetID(sID, iw);
		return iw;
	}

	private static void invokeSetMethods(Element e, Object o, String errorPrefix, Object callbacks)
		throws BugException
	{
		// Attributes apart from id map to setXXX methods
		String[] attributes = XML.getAttributeNames(e);
		for(int attribute = 0; attribute<attributes.length; attribute++)
		{
			// Get attribute name and value
			String name = attributes[attribute], value = e.getAttribute(name);

			// ID we use if it matches a public variable in the handler object to
			// set up that variable automagically.
			if(name.equals("id"))
			{
				if(callbacks!=null)
				{
					// Try from this class to superclasses
					for(Class<?> tryClass = callbacks.getClass(); tryClass!=null; tryClass = tryClass.getSuperclass())
					{
						// See if there's a public field with that name plus 'UI' in the callback class
						Field f;
						try
						{
							f = tryClass.getField(value + "UI");
							f.set(callbacks, o);
							break;
						}
						catch(NoSuchFieldException x)
						{
							// Not a field, that's cool. If this ends in a number, is there an
							// array?
							Matcher m = ARRAYFIELD.matcher(value);
							if(m.matches())
							{
								String arrayName = m.group(1);
								int index = Integer.parseInt(m.group(2));
								try
								{
									f = tryClass.getField(arrayName + "UI");
									Object array = f.get(callbacks);
									if(array==null)
									{
										array = Array.newInstance(f.getType().getComponentType(), index + 1);
										f.set(callbacks, array);
									}
									if(Array.getLength(array)<=index)
									{
										Object newArray = Array.newInstance(f.getType().getComponentType(), index + 1);
										System.arraycopy(array, 0, newArray, 0, Array.getLength(array));
										array = newArray;
										f.set(callbacks, array);
									}
									Array.set(array, index, o);
									break;
								}
								catch(NoSuchFieldException xx)
								{
									// No array either, that's cool
								}
								catch(Exception xx)
								{
									throw new BugException("Error autosetting public array field " + arrayName + "UI", xx);
								}
							}
						}
						catch(Exception x)
						{
							throw new BugException("Error autosetting public field " + value + "UI", x);
						}
					}
				}

				// But otherwise it doesn't call any set methods or anything
				continue;
			}

			String sPropertyErrorPrefix = errorPrefix + "Property " + name + ":";

			// See if there's a set method for either string, int, boolean, or two
			// ints, or Color, and that name
			try
			{
				outer: do // This is not really a loop; I just want to be able to break out of it
				{
					try
					{
						Method mString = o.getClass().getMethod(
							"set" + name, new Class[] { String.class });
						mString.invoke(o, new Object[] {value});
						break;
					}
					catch(NoSuchMethodException nsme)
					{
						// Not string property
					}

					try
					{
						Method mInt = o.getClass().getMethod(
							"set" + name, new Class[] { int.class });

						int iValue = getIntValue(o, errorPrefix, value);

						mInt.invoke(o, iValue);
						break;
					}
					catch(NoSuchMethodException nsme)
					{
						// Not int property
					}

					try
					{
						Method mBoolean = o.getClass().getMethod(
							"set" + name, new Class[] { boolean.class });

						boolean bValue;
						if(value.equals("y"))
							bValue = true;
						else if(value.equals("n"))
						 bValue = false;
						else
						 throw new BugException(sPropertyErrorPrefix +
								"Value must be y or n, not: " + value);

						mBoolean.invoke(o, new Object[] { new Boolean(bValue)});
						break;
					}
					catch(NoSuchMethodException nsme)
					{
						// Not int property
					}

					for(int multi = 2; multi<=4; multi++)
					{
						try
						{
							Class<?>[] classes = new Class<?>[multi];
							for(int i = 0; i < classes.length; i++)
							{
								classes[i]=int.class;
							}

							Method multiInt = o.getClass().getMethod("set" + name, classes);

							String[] values = value.split(",");
							if(values.length!=multi)
							 throw new BugException(sPropertyErrorPrefix +
	 							"Expecting " + multi + " values separated by commas, not: " + value);

							Object[] objects = new Object[multi];
							for(int i = 0; i<objects.length; i++)
							{
								objects[i] = getIntValue(o, errorPrefix, values[i]);
							}

							multiInt.invoke(o, objects);
							break outer;
						}
						catch(NoSuchMethodException nsme)
						{
							// Not int, int
						}
					}

					try
					{
						Method mColor = o.getClass().getMethod(
							"set" + name, Color.class);

						Color c = getColorValue(o, errorPrefix, value);

						mColor.invoke(o, c);
						break;
					}
					catch(NoSuchMethodException nsme)
					{
						// Not Color
					}

					// We didn't find any appropriate methods
					throw new BugException(sPropertyErrorPrefix + "No such property");
				}
				while(false);
			}
			catch (IllegalArgumentException iae)
			{
				throw new BugException(
					sPropertyErrorPrefix + "Unexpected error setting widget property", iae);
			}
			catch (IllegalAccessException iae)
			{
				throw new BugException(
					sPropertyErrorPrefix + "Unexpected error setting widget property", iae);
			}
			catch (InvocationTargetException ite)
			{
				throw new BugException(
					sPropertyErrorPrefix + "Error setting widget property", ite.getCause());
			}

		}
	}

	private static Color getColorValue(Object o, String sPropertyErrorPrefix, String sValue)
		throws BugException
	{
		String sV=sValue;
		if(!sV.startsWith("#"))
		 throw new BugException(sPropertyErrorPrefix +
		 		"Expecting color beginning with #, not: " + sValue);

		sV=sV.substring(1);
		if(sV.length()==3)
		{
			sV=sV.substring(0, 1) + sV.charAt(0) +
				sV.charAt(1) + sV.charAt(1) + sV.charAt(2) + sV.charAt(2);
		}
		if(sV.length()!=6)
			throw new BugException(sPropertyErrorPrefix +
	 			"Expecting three or six-digit hex color, not: " + sValue);

		try
		{
			return new Color(
				Integer.parseInt(sV.substring(0, 2), 16),
				Integer.parseInt(sV.substring(2, 4), 16),
				Integer.parseInt(sV.substring(4, 6), 16));
		}
		catch(NumberFormatException nfe)
		{
			throw new BugException(sPropertyErrorPrefix +
				"Expecting valid hex color, not: " + sValue);
		}
	}

	/**
	 * Converts a string to integer, allowing both standard integers and also
	 * constants that are declared in any interface of the given object's class.
	 * @param o Target object
	 * @param sPropertyErrorPrefix Prefix used in error exceptions
	 * @param sValue Value to parse
	 * @return Integer value
	 * @throws IllegalAccessException If there is a problem while searching
	 * @throws BugException If the string can't be matched to an integer
	 */
	private static int getIntValue(
		Object o,
		String sPropertyErrorPrefix,
		String sValue)
		throws IllegalAccessException, BugException
	{
		int iValue = -1; // This should always be set later, but Java isn't
										// clever enough to work that out
		try
		{
			iValue = Integer.parseInt(sValue);
		}
		catch(NumberFormatException nfe)
		{
			// Look for a constant of that name in all interfaces iw
			// implements
			Class<?>[] ac = o.getClass().getInterfaces();
			boolean bGot = false;
			searchLoop: for(int iInterface = 0; iInterface<ac.length; iInterface++)
			{
				Class<?> cInterface = ac[iInterface];
				Field[] af = cInterface.getFields();
				for(int iField = 0; iField<af.length; iField++)
				{
					if(af[iField].getName().equals(sValue))
					{
						try
						{
							iValue = af[iField].getInt(o);
							bGot = true;
							break searchLoop;
						}
						catch(IllegalArgumentException iae)
						{
							// Looks like this field isn't an option, let's just
							// continue with the loop so we eventually throw
							// invalid value (below)
						}
					}
				}
			}

			if(!bGot) throw new BugException(sPropertyErrorPrefix + "Expecting integer or constant, not: " + sValue);
		}
		return iValue;
	}

	@Override
	public void registerTool(final Tool t)
	{
		toolbar.register(t);
	}

	@Override
	public void unregisterTool(final Tool t)
	{
		toolbar.unregister(t);
	}

	@Override
	public void resizeToolbar()
	{
		runInSwing(new Runnable()
		{
			@Override
			public void run()
			{
				toolbar.rearrangeTools();
				if(uiStyle == UISTYLE_MULTIWINDOW)
				{
					if(f != null)
					{
						f.pack();
					}
				}
			}
		});
	}

	/**
	 * @return UI singleton's plugin context (for use by other UI components)
	 */
	PluginContext getPluginContext()
	{
		return context;
	}

	/** Handler for the yes/no/cancel question dialog */
	public static class QuestionDialogHandler
	{
		/**
		 * Constructs handler.
		 * @param defaultButton Default button for user
		 */
		public QuestionDialogHandler(int defaultButton)
		{
			result = defaultButton;
		}

		private Dialog d;
		private boolean remember;
		void setDialog(Dialog d)
		{
			this.d = d;
		}

		private int result;

		/** UI action: User clicks Yes button. */
		public void actionYes()
		{
			result = BUTTON_YES;
			d.close();
		}

		/** UI action: User clicks No button. */
		public void actionNo()
		{
			result = BUTTON_NO;
			d.close();
		}

		/** UI action: User clicks Cancel button. */
		public void actionCancel()
		{
			result = BUTTON_CANCEL;
			d.close();
		}

		/** UI action: User toggles Remember checkbox. */
		public void changeRemember()
		{
			remember = ((CheckBox)d.getWidget("remember")).isChecked();
		}

		int getResult()
		{
			return result;
		}

		boolean isRemember()
		{
			return remember;
		}
	}

	@Override
	public int showQuestion(WidgetOwner parent, String title, String message, int buttons,
		String yesLabel, String noLabel, String cancelLabel, int defaultButton)
	{
		try
		{
			QuestionDialogHandler qdh = new QuestionDialogHandler(
				(buttons & BUTTON_CANCEL)!=0 ? BUTTON_CANCEL : defaultButton);
			Dialog d = createDialog(XML.parse(UISingleton.class, "questiondialog.xml"), qdh);
			qdh.setDialog(d);
			d.setTitle(title);
			((Label)d.getWidget("message")).setText(message);
			Button yes = (Button)d.getWidget("yes"), no = (Button)d.getWidget("no"),
			 cancel = (Button)d.getWidget("cancel");
			if(noLabel!=null) no.setLabel(noLabel);
			if(yesLabel!=null) yes.setLabel(yesLabel);
			if(cancelLabel!=null) cancel.setLabel(cancelLabel);
			if((buttons & BUTTON_YES)==0) yes.setVisible(false);
			if((buttons & BUTTON_NO)==0) no.setVisible(false);
			if((buttons & BUTTON_CANCEL)==0) cancel.setVisible(false);
			switch(defaultButton)
			{
			case BUTTON_YES: yes.setDefault(true); break;
			case BUTTON_NO: no.setDefault(true); break;
			case BUTTON_CANCEL: cancel.setDefault(true); break;
			default: throw new BugException("Unexpected default button " + defaultButton);
			}
			d.show(parent);
			return qdh.getResult();
		}
		catch(XMLException e)
		{
			ErrorMsg.report("Error showing question dialog", e);
			if((buttons&BUTTON_CANCEL)!=0)
				return BUTTON_CANCEL;
			else
				return defaultButton;
		}
	}

	@Override
	public int showOptionalQuestion(String prefID, WidgetOwner parent, String title, String message, int buttons,
		String yesLabel, String noLabel, String cancelLabel, int defaultButton)
	{
		Preferences p = context.getSingle(Preferences.class);
		PreferencesGroup group = p.getGroup(context.getPlugin()).getChild("optional-questions");
		String selected = group.get(prefID, null);
		if(selected!=null)
		{
			return p.toInt(selected);
		}

		try
		{
			QuestionDialogHandler qdh = new QuestionDialogHandler(
				(buttons & BUTTON_CANCEL)!=0 ? BUTTON_CANCEL : defaultButton);
			Dialog d = createDialog(XML.parse(UISingleton.class, "optionalquestiondialog.xml"), qdh);
			qdh.setDialog(d);
			d.setTitle(title);
			((Label)d.getWidget("message")).setText(message);
			Button yes = (Button)d.getWidget("yes"), no = (Button)d.getWidget("no"),
			 cancel = (Button)d.getWidget("cancel");
			if(noLabel!=null) no.setLabel(noLabel);
			if(yesLabel!=null) yes.setLabel(yesLabel);
			if(cancelLabel!=null) cancel.setLabel(cancelLabel);
			if((buttons & BUTTON_YES)==0) yes.setVisible(false);
			if((buttons & BUTTON_NO)==0) no.setVisible(false);
			if((buttons & BUTTON_CANCEL)==0) cancel.setVisible(false);
			switch(defaultButton)
			{
			case BUTTON_YES: yes.setDefault(true); yes.focus(); break;
			case BUTTON_NO: no.setDefault(true); no.focus(); break;
			case BUTTON_CANCEL: cancel.setDefault(true); cancel.focus(); break;
			default: throw new BugException("Unexpected default button " + defaultButton);
			}
			d.show(parent);
			int result = qdh.getResult();
			if(result!=BUTTON_CANCEL && qdh.isRemember())
			{
				group.set(prefID, p.fromInt(result));
			}
			return result;
		}
		catch(XMLException e)
		{
			ErrorMsg.report("Error showing question dialog", e);
			if((buttons&BUTTON_CANCEL)!=0)
				return BUTTON_CANCEL;
			else
				return defaultButton;
		}
	}

	/** Handler for user error dialog where there's just an OK button. */
	public static class OKDialogHandler
	{
		private Dialog d;
		void setDialog(Dialog d)
		{
			this.d = d;
		}

		/**
		 * User clicks OK.
		 */
		public void actionOK()
		{
			d.close();
		}
	}

	@Override
	public void showUserError(WidgetOwner parent, String title, String message)
	{
		try
		{
			Document dXML=XML.parse(UISingleton.class, "usererror.xml");
			OKDialogHandler odh = new OKDialogHandler();
			Dialog d = createDialog(dXML, odh);
			odh.setDialog(d);
			d.setTitle(title);
			((Label)d.getWidget("message")).setText(message);
			d.show(parent);
		}
		catch(XMLException e)
		{
			ErrorMsg.report("Error showing user error dialog", e);
		}
	}

	// Theme methods

	@Override
	public File getThemeFolder(boolean system)
	{
		if(system)
		{
			return new File(THEME_FOLDER);
		}
		else
		{
			File userThemes = new File(PlatformUtils.getUserFolder(), THEME_FOLDER);
			if(!userThemes.exists())
				userThemes.mkdirs();
			return userThemes;
		}
	}

	@Override
	public void installUserTheme(File newTheme) throws GeneralException
	{
		// Test theme
		try
		{
			new ThemeImp(newTheme);
		}
		catch(Exception e)
		{
			throw new GeneralException(e);
		}

		try
		{
			// Check target for theme file
			File target = new File(getThemeFolder(false), newTheme.getName());
			boolean isCurrent = currentTheme.getFile().getCanonicalPath().equals(target.getCanonicalPath());

			// If it's the current theme, switch to something else real quick like
			if(isCurrent) currentTheme = sharedTheme;

			// Copy in the new file
			IOUtils.copy(newTheme, target, true);

			// If it was current, switch the theme back
			if(isCurrent)
			{
				Theme[] result = getAvailableThemes();
				for(int i = 0; i<result.length; i++)
				{
					if(((ThemeImp)result[i]).getFile().getCanonicalPath().equals(target.getCanonicalPath()))
					{
						setTheme(result[i]);
					}
				}
			}
		}
		catch(IOException e)
		{
			throw new GeneralException(e);
		}
	}

	@Override
	public Theme[] getAvailableThemes()
	{
		SortedSet<Theme> themes = new TreeSet<Theme>();
		if(currentTheme!=null)
			themes.add(currentTheme);
		if(sharedTheme!=null && sharedTheme!=currentTheme)
			themes.add(sharedTheme);

		File[][] search = new File[2][];

		File userThemes = getThemeFolder(false);
		search[0]=IOUtils.listFiles(userThemes);

		File programThemes = getThemeFolder(true);
		if(programThemes.exists())
			search[1]=IOUtils.listFiles(programThemes);

		for(int i = 0; i<search.length; i++)
		{
			File[] files = search[i];
			if(files==null) continue;
			for(int j = 0; j<files.length; j++)
			{
				if(files[j].getName().endsWith(THEME_EXTENSION))
				{
					if(
						(currentTheme==null || !files[j].equals(currentTheme.getFile())) &&
						(sharedTheme==null || !files[j].equals(sharedTheme.getFile())))
					{
						try
						{
							themes.add(new ThemeImp(files[j]));
						}
						catch(IOException e)
						{
							ErrorMsg.report(
								"An error occurred when trying to load the theme " + files[j].getName(), e);
						}
					}
				}
			}
		}

		return themes.toArray(new Theme[themes.size()]);
	}

	@Override
	public Theme getTheme()
	{
		return currentTheme==null ? sharedTheme : currentTheme;
	}

	@Override
	public synchronized void setTheme(Theme t)
	{
		currentTheme = (ThemeImp)t;
		if(currentTheme!=null && currentTheme!=sharedTheme)
		{
			currentTheme.setParent(sharedTheme);
		}

		Preferences p;
		try
		{
			p = context.getSingle(Preferences.class);
		 p.getGroup(context.getPlugin()).set("theme",
		 		currentTheme==null ? "" :
		 		currentTheme.getFile().getName().replaceAll("\\" + THEME_EXTENSION, ""));
		}
		catch(BugException e)
		{
			ErrorMsg.report("Error setting theme in preferences", e);
		}

		refreshTheme();
	}

	@Override
	public synchronized void refreshTheme()
	{
		Theme t = getTheme();
		for(Iterator<WeakReference<ThemeListener>> i = themeListeners.iterator();
			i.hasNext(); )
		{
			WeakReference<ThemeListener> wr = i.next();
			ThemeListener tl = wr.get();
			if(tl==null)
			{
				i.remove();
			}
			else
			{
				tl.updateTheme(t);
			}
		}
		// TODO Maybe fire an event as well
	}

	/**
	 * @param parent Parent referent or null to use default
	 * @return Frame to use as dialog parent. May be null if program hasn't
	 * started up yet.
	 */
	Frame getDialogFrame(WidgetOwner parent)
	{
		if(parent!=null)
		{
			JComponent c = getPositionReferent(parent);
			Container topLevel = c.getTopLevelAncestor();
			if(topLevel instanceof Frame)
			{
				return (Frame)topLevel;
			}
		}
		return f;
	}

	JComponent getPositionReferent(WidgetOwner parent)
	{
		// Position it relative to specified or main window
		while(parent!=null && (parent instanceof Page))
			parent = ((Page)parent).getOwner();

		if(parent==null)
			return null;
		else if(parent instanceof Window)
			return ((WindowImp.WindowInterface)parent).getPositionReferent();
		else if(parent instanceof Dialog)
			return ((DialogImp.DialogInterface)parent).getPositionReferent();
		else
			throw new BugException("Unexpected WidgetOwner type");
	}

	/**
	 * @param parent Parent component marker (may be null)
	 * @return Component referring to that parent, or the main frame, or null
	 * if the main frame isn't up yet.
	 */
	Component getParentComponent(WidgetOwner parent)
	{
		Component c = getPositionReferent(parent);
		if(c==null)
			return f;
		else
			return c;
	}

	void setDialogPosition(JDialog d, WidgetOwner parent)
	{
		d.setLocationRelativeTo(getDialogFrame(parent));
	}

	@Override
	public File showFileSelect(WidgetOwner parent, String title, boolean saveMode, File folder,
		File file, final String[] extensions, final String filterName)
	{
		if(PlatformUtils.isMac())
		{
			FileDialog fd = new FileDialog(getDialogFrame(parent),
				title, saveMode ? FileDialog.SAVE : FileDialog.LOAD);
			if(folder!=null && file==null)
			{
				fd.setDirectory(folder.getPath());
			}
			if(file!=null)
			{
				fd.setDirectory(file.getParent());
				fd.setFile(file.getName());
			}
			if(extensions!=null)
			{
				fd.setFilenameFilter(new FilenameFilter()
				{
					@Override
					public boolean accept(File dir, String name)
					{
						for(int i = 0; i<extensions.length; i++)
						{
							if(name.toLowerCase().endsWith(extensions[i].toLowerCase()))
								return true;
						}
						return false;
					}
				});
			}

			fd.setVisible(true);
			if(fd.getFile()==null)
				return null;
			else
				return new File(fd.getDirectory(), fd.getFile());
		}
		else // On non-Mac platforms the Swing dialog is better
		{
			JFileChooser fc;
			if(folder==null)
				fc = new JFileChooser();
			else
				fc = new JFileChooser(folder);
			if(saveMode)
			{
				fc.setDialogType(JFileChooser.SAVE_DIALOG);
			}

			fc.setDialogTitle(title);
			if(extensions!=null)
			{
				javax.swing.filechooser.FileFilter[] filters = fc.getChoosableFileFilters();
				for(int i = 0; i<filters.length; i++)
					fc.removeChoosableFileFilter(filters[i]);
				fc.setFileFilter(new javax.swing.filechooser.FileFilter()
				{
					@Override
					public boolean accept(File f)
					{
						for(int i = 0; i<extensions.length; i++)
						{
							if(f.getName().toLowerCase().endsWith(extensions[i].toLowerCase()))
								return true;
						}
						return false;
					}

					@Override
					public String getDescription()
					{
						return filterName==null ? "Appropriate files" : filterName;
					}
				});
			}
			fc.setFileHidingEnabled(true);
			if(file!=null)
			{
				fc.setSelectedFile(file);
			}

			int result;
			if(saveMode)
			{
				result = fc.showSaveDialog(getParentComponent(parent));
			}
			else
			{
				result = fc.showOpenDialog(getParentComponent(parent));
			}
			if(result != JFileChooser.APPROVE_OPTION)
			{
				return null;
			}
			else
			{
				if(saveMode && fc.getSelectedFile().exists())
				{
					int ok = showQuestion(parent, "Confirm overwrite", "The file " +
						fc.getSelectedFile().getName() + " already exists. Are you sure " +
						"you want to overwrite it?", UI.BUTTON_YES|UI.BUTTON_CANCEL,
						"Overwrite", "", "Cancel", UI.BUTTON_CANCEL);
					if(ok != UI.BUTTON_YES)
					{
						return null;
					}
				}
				return fc.getSelectedFile();
			}
		}
	}

	@Override
	public File showFolderSelect(WidgetOwner parent, String title, File folder)
	{
		if(PlatformUtils.isMac())
		{
			FileDialog fd = new FileDialog(getDialogFrame(parent),
				title, FileDialog.LOAD);
			fd.setFile(folder.getName());
			if(folder!=null && folder.getParentFile()!=null)
				fd.setDirectory(folder.getParentFile().getPath());

			System.setProperty("apple.awt.fileDialogForDirectories", "true");
			fd.setVisible(true);
			System.setProperty("apple.awt.fileDialogForDirectories", "false");
			if(fd.getFile()==null)
				return null;
			else
				return new File(fd.getDirectory(), fd.getFile());
		}
		else // On non-Mac platforms the Swing dialog is better
		{
			JFileChooser fc;
			if(folder==null || folder.getParentFile()==null)
				fc = new JFileChooser();
			else
				fc = new JFileChooser(folder.getParentFile());
			fc.setDialogTitle(title);
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			javax.swing.filechooser.FileFilter[] filters = fc.getChoosableFileFilters();
			for(int i = 0; i<filters.length; i++)
				fc.removeChoosableFileFilter(filters[i]);
			fc.setFileFilter(new javax.swing.filechooser.FileFilter()
			{
				@Override
				public boolean accept(File f)
				{
					return f.isDirectory() && !f.isHidden();
				}

				@Override
				public String getDescription()
				{
					return "All folders";
				}
			});
			fc.setFileHidingEnabled(true);
			fc.setSelectedFile(folder);

			if(fc.showOpenDialog(getParentComponent(parent))!=JFileChooser.APPROVE_OPTION)
				return null;
			else
				return fc.getSelectedFile();
		}
	}

	@Override
	public Color showColourSelect(WidgetOwner parent, String title, Color original)
	{
		ColourSelectDialog selector = new ColourSelectDialog(
			getParentComponent(parent), title, original);
		return selector.getChosenColour();
	}

	@Override
	public synchronized int getUIStyle()
	{
		return uiStyle;
	}

	private void focusNowAndLater(WindowImp active)
	{
		if(active!=null)
		{
			final WindowImp activeFinal = active;
			Runnable r = new Runnable()
			{
				@Override
				public void run()
				{
					activeFinal.getHolder().focusFrame();
				}
			};
			r.run();
			SwingUtilities.invokeLater(r);
		}
	}

	@Override
	public synchronized void setUIStyle(int uiStyle)
	{
		if(this.uiStyle==uiStyle) return;
		int styleBefore = this.uiStyle;
		this.uiStyle = uiStyle;
		sb.setUIStyle(uiStyle);
		context.getSingle(Preferences.class).getGroup(context.getPlugin()).set(
			UIPrefs.PREF_UISTYLE,
			uiStyle==UISTYLE_TABBED ? UIPrefs.PREFVALUE_UISTYLE_TABS
				: uiStyle==UISTYLE_MULTIWINDOW ? UIPrefs.PREFVALUE_UISTYLE_SEPARATE
					: UIPrefs.PREFVALUE_UISTYLE_CLASSIC);
		f.setResizable(uiStyle!=UISTYLE_MULTIWINDOW);

		switch(uiStyle)
		{
		case UISTYLE_MULTIWINDOW:
			{
				// Move each window outside
				WindowImp active = null;
				WindowImp[] present = windows.toArray(new WindowImp[windows.size()]);
				for(int i = 0; i<present.length; i++)
				{
					WindowImp wi = present[i];
					FrameHolder fh = wi.getHolder();
					if(fh==null) continue;
					if(fh instanceof FrameInside)
					{
						FrameInside fi = (FrameInside)fh;
						Point
							local = fi.getLocation(), // This is done because it might not be visible
							parent = fi.getParent().getLocationOnScreen();
						Point before = new Point(local.x + parent.x, local.y + parent.y);
						if(PlatformUtils.isMac())
						{
							before.x += MacFixInternalFrame.getStandardInsets().left;
						}
						new FrameOutside(wi, before);
						fh.killSilently();
					}
					else if(fh instanceof FrameTab)
					{
						FrameTab ft = (FrameTab)fh;
						wi.getContents().setSize(ft.getPreviousContentSize());
						new FrameOutside(wi, ft.getPreviousPosition());
						fh.killSilently();
					}
					if(active==null || wi.isActive())
						active = wi;
				}

				if(styleBefore==UISTYLE_SINGLEWINDOW)
				{
					// Now kill the internal desktop
					f.getContentPane().remove(desktop);
					f.getContentPane().remove(sb);
				}
				else if(styleBefore==UISTYLE_TABBED)
				{
					f.getContentPane().remove(tabs.getParent());
					sb.getParent().remove(sb);
				}
				f.pack();

				focusNowAndLater(active);
			}
			break;
		case UISTYLE_SINGLEWINDOW:
			{
				Dimension currentSize = f.getSize();
				if(styleBefore==UISTYLE_TABBED)
				{
					currentSize = tabs.getSize();
					// Remove tab holder
					f.getContentPane().remove(tabs.getParent());
					sb.getParent().remove(sb);
				}

				// Put the internal desktop back
				f.getContentPane().add(desktop, BorderLayout.CENTER);
				f.getContentPane().add(sb, BorderLayout.SOUTH);

				// Make it fit the windows
				Point maxTL=desktop.getLocationOnScreen();
				Point maxBR=new Point(
					maxTL.x + Math.max(600, currentSize.width),
					maxTL.y + Math.max(450, currentSize.height));
				WindowImp active = null;
				for(WindowImp wi : windows)
				{
					FrameHolder fh = wi.getHolder();
					if(fh==null) continue;
					Point tl;
					Dimension d;
					Insets internalInsets = MacFixInternalFrame.getStandardInsets();
					if(fh instanceof FrameOutside)
					{
						FrameOutside fo = (FrameOutside)fh;
						tl = fo.getLocation();
						d = fo.getSize();
					}
					else if(fh instanceof FrameTab)
					{
						FrameTab ft = (FrameTab)fh;
						if(ft.getPreviousPosition()==null) continue; // Don't consider this one's size

						tl = ft.getPreviousPosition();
						d = ft.getPreviousContentSize();
						d.height += internalInsets.top;
					}
					else throw new BugException("Unexpected frame type");
					if(wi.isActive() || active==null)
						active = wi;

					d.width += internalInsets.right + internalInsets.left;
					d.height += internalInsets.bottom;
					Point br = new Point(tl.x + d.width, tl.y + d.height);

					if(tl.x<maxTL.x) maxTL.x = tl.x;
					if(tl.y<maxTL.y) maxTL.y = tl.y;
					if(br.x>maxBR.x) maxBR.x = br.x;
					if(br.y>maxBR.y) maxBR.y = br.y;
				}

				Insets offsets = new Insets(
					f.getInsets().top + toolbar.getHeight(),
					f.getInsets().left,
					f.getInsets().bottom + sb.getPreferredSize().height,
					f.getInsets().right);

				maxTL.x -= offsets.left;
				maxTL.y -= offsets.top;
				maxBR.x += offsets.right;
				maxBR.y += offsets.bottom;
				Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
				if(maxTL.x<0) maxTL.x = 0;
				if(maxTL.y<0) maxTL.y = 0;
				if(maxBR.x>screen.width) maxBR.x = screen.width;
				if(maxBR.y>screen.height) maxBR.y = screen.height;

				f.setLocation(maxTL);
				f.setSize(new Dimension(maxBR.x-maxTL.x, maxBR.y-maxTL.y));

				// The size params in the InternalDesktop haven't updated yet, we
				// need them to
				desktop.setOverrideSize(new Dimension(
					maxBR.x-maxTL.x-offsets.left-offsets.right,
					maxBR.y-maxTL.y-offsets.top-offsets.bottom));

				WindowImp[] windowsArray = windows.toArray(new WindowImp[windows.size()]);
				for(int i = 0; i<windowsArray.length; i++)
				{
					WindowImp wi = windowsArray[i];
					FrameHolder fh = wi.getHolder();
					if(fh==null) continue;
					if(fh instanceof FrameOutside)
					{
						Point before = ((FrameOutside)fh).getLocationOnScreen();
						if(PlatformUtils.isMac())
						{
							before.x -= MacFixInternalFrame.getStandardInsets().left;
						}
						new FrameInside(desktop, wi, true, before, fh.isMinimized());
					}
					else if(fh instanceof FrameTab)
					{
						FrameTab ft = (FrameTab)fh;
						wi.getContents().setSize(ft.getPreviousContentSize());
						new FrameInside(desktop, wi, true,
							ft.getPreviousPosition(), fh.isMinimized());
					}
					fh.killSilently();
				}

				focusNowAndLater(active);

			 desktop.clearOverrideSize();
			}
			break;
		case UISTYLE_TABBED:
			{
				// Get window positions (can't do this after hiding them)
				Map<FrameHolder, Point> positions = new HashMap<FrameHolder, Point>();
				for(WindowImp wi : windows)
				{
					FrameHolder fh = wi.getHolder();
					if(fh==null) continue;
					if(fh instanceof FrameInside)
					{
						FrameInside fi = (FrameInside)fh;
						if(fi.getParent() == null)
						{
							// Dunno why this would happen, but it does (#13)
							continue;
						}
						Point
							local = fi.getLocation(), // This is done because it might not be visible
							parent = fi.getParent().getLocationOnScreen();
						positions.put(fh, new Point(local.x + parent.x, local.y + parent.y));
					}
					else if(fh instanceof FrameOutside)
					{
						FrameOutside fo = (FrameOutside)fh;
						positions.put(fh, new Point(fo.getLocation()));
					}
				}

				if(styleBefore==UISTYLE_SINGLEWINDOW)
				{
					// Remove the internal desktop and add the tabbed pane instead
					f.getContentPane().remove(desktop);
					f.getContentPane().remove(sb);
				}
				JPanel tabsHolder = new JPanel(new BorderLayout());
				tabsHolder.add(sb, BorderLayout.NORTH);
				tabsHolder.add(tabs, BorderLayout.CENTER);
				f.getContentPane().add(tabsHolder, BorderLayout.CENTER);

				WindowImp active = null;
				for(WindowImp wi : windows)
				{
					FrameHolder fh = wi.getHolder();
					if(fh==null) continue;
					if(wi.isActive() || active==null) active = wi;
					new FrameTab(this, wi, positions.get(fh), false);
					fh.killSilently();
				}
				focusNowAndLater(active);

				if(styleBefore==UISTYLE_MULTIWINDOW)
				{
					Preferences p = context.getSingle(Preferences.class);
					PreferencesGroup mainWindow = p.getGroup(context.getPlugin()).getChild(UIPlugin.PREFGROUP_MAINWINDOW);
					Dimension size = new Dimension(
						p.toInt(mainWindow.get(UIPlugin.PREF_WIDTH, UIPlugin.PREFDEFAULT_WIDTH)),
						p.toInt(mainWindow.get(UIPlugin.PREF_HEIGHT, UIPlugin.PREFDEFAULT_HEIGHT)));
					f.setSize(size);
				}

				// On some platforms (tested on Ubuntu 12.04 with Java 7r3), a
				// revalidate is needed here, or else it doesn't update the display.
				if(styleBefore==UISTYLE_SINGLEWINDOW)
				{
					((JComponent)f.getContentPane()).revalidate();
				}
			}
			break;
		}
	}

	synchronized void addWindow(WindowImp wi)
	{
		windows.addLast(wi);
	}

	synchronized void removeWindow(WindowImp wi)
	{
		windows.remove(wi);
	}

	synchronized void informActiveWindow(WindowImp wi)
	{
		windows.remove(wi);
		windows.addLast(wi);
	}

	/**
	 * Adds a tab.
	 * @param tab Tab
	 */
	public void addTab(FrameTab tab)
	{
		tabs.add(tab, tab.getID() + "");
		sb.addFrame(tab);
	}

	/**
	 * Selects a tab.
	 * @param tab Tab
	 */
	public void selectTab(FrameTab tab)
	{
		tabsLayout.show(tabs, tab.getID() + "");
		sb.informActiveFrame(tab);
		Component[] innerTabs = tabs.getComponents();
		for(int i = 0; i<innerTabs.length; i++)
		{
			((FrameTab)innerTabs[i]).informInactive();
		}
		tab.informActive();
	}

	/**
	 * @return Selected tab
	 */
	public FrameTab getSelectedTab()
	{
		return (FrameTab)sb.getActiveFrame();
	}

	/**
	 * Removes a tab.
	 * @param tab Tab
	 */
	public void removeTab(FrameTab tab)
	{
		tabs.remove(tab);
		sb.removeFrame(tab);
		Component[] check = tabs.getComponents();
		for(int i = 0; i<check.length; i++)
		{
			if(check[i].isVisible())
				sb.informActiveFrame((FrameTab)check[i]);
		}
	}

	/**
	 * Adds something to be informed when theme changes. This is stored as
	 * a weak reference so you don't need to remove it.
	 * @param tl Listener
	 */
	synchronized void informThemeListener(ThemeListener tl)
	{
		themeListeners.add(new WeakReference<ThemeListener>(tl));
	}

	// Font settings
	////////////////

	/** @return Stylesheet containing user settings */
	String getSettingsStylesheet()
	{
		StringBuffer sb = new StringBuffer();

		Preferences p = context.getSingle(Preferences.class);
		PreferencesGroup pg = p.getGroup(context.getPlugin());
		if(!p.toBoolean(pg.get(UIPrefs.PREF_SYSTEMFONT, UIPrefs.PREFDEFAULT_SYSTEMFONT)))
		{
			String name = pg.get(UIPrefs.PREF_FONTNAME, UIPrefs.PREFDEFAULT_FONTNAME);
			String family = "SansSerif"; // I haven't found a better way to get this

			sb.append("_root { font-name: \"" + name + "\", " + family + "; " +
				"font-size: " + pg.get(UIPrefs.PREF_FONTSIZE, UIPrefs.PREFDEFAULT_FONTSIZE) + "; }\n");
		}
		PreferencesGroup[] colours = pg.getChild(UIPrefs.PREFGROUP_COLOURS).getAnon();
		for(int i = 0; i<colours.length; i++)
		{
			String keyword = colours[i].get(UIPrefs.PREF_KEYWORD);
			String rgb = colours[i].get(UIPrefs.PREF_RGB);
			Matcher m = RGB.matcher(rgb);
			if(!m.matches()) continue;

			sb.append("@rgb " + keyword + " \"User-set\" rgb(" + rgb + "); \n");
		}

		return sb.toString();
	}

	/** @return Font if user-set, or null if default should be used */
	Font getFont()
	{
		Preferences p = context.getSingle(Preferences.class);
		PreferencesGroup pg = p.getGroup(context.getPlugin());
		if(p.toBoolean(pg.get(UIPrefs.PREF_SYSTEMFONT, UIPrefs.PREFDEFAULT_SYSTEMFONT)))
			return null;

		return new Font(
			pg.get(UIPrefs.PREF_FONTNAME, UIPrefs.PREFDEFAULT_FONTNAME),
			Font.PLAIN,
			Integer.parseInt(pg.get(UIPrefs.PREF_FONTSIZE, UIPrefs.PREFDEFAULT_FONTSIZE)));
	}

	/**
	 * Called on system close.
	 */
	public void close()
	{
		// Before we quit, tell all the windows they're closed
		WindowImp[] windowsArray = windows.toArray(new WindowImp[windows.size()]);
		for(int i = 0; i<windowsArray.length; i++)
		{
			windowsArray[i].informClosed();
		}
		// And the toolbar
		toolbar.informClosed();
	}

	@Override
	public void runInThread(Runnable r)
	{
		SwingUtilities.invokeLater(r);
	}

	@Override
	public boolean isAppActive()
	{
		return KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow()!=null;
	}

	/**
	 * Gets the Mac indent in pixels that corresponds to a specific string
	 * constant as used in the XML files.
	 * @param macIndent Indent constant
	 * @return Indent in pixels (will be 0 if not on a Mac)
	 */
	static int getMacIndent(String macIndent)
	{
		if(macIndent.equals(SupportsMacIndent.TYPE_BUTTON))
		{
			return PlatformUtils.getMacIndentButton();
		}
		else if(macIndent.equals(SupportsMacIndent.TYPE_EDIT)
			|| macIndent.equals(SupportsMacIndent.TYPE_EDIT_LEGACY))
		{
			return PlatformUtils.getMacIndentEdit();
		}
		else if(macIndent.equals(SupportsMacIndent.TYPE_NONE))
		{
			return 0;
		}
		throw new IllegalArgumentException("Unknown MacIndent value: " + macIndent);
	}
}
