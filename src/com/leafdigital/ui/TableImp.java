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
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import org.w3c.dom.Element;

import util.*;
import util.xml.*;

import com.leafdigital.ui.api.*;

import leafchat.core.api.BugException;

/** Combo box */
public class TableImp extends JScrollPane
{
	private JComponent wrapper;
	private JTable t;
	private OurTableModel otm;

	private boolean inSetSelect = false;

	/** Callbacks */
	private String callbackEditing = null, callbackChange = null,
		callbackSelect = null, callbackAction = null;

	/** Margin on right side of table header text at min size */
	private final static int HEADERRIGHTMARGIN = 2;

	/** Extra space required (insets) around header columns in addition to text width */
	private static int headerExtraWidth = -1;

	/** Extra left indent on this table, used to make things line up on Mac */
	private int macIndent;

	/** Interface */
	private Table publicInterface;

	/** Map of flags for cells (TableLocation -> Flag) */
	private Map<TableLocation, Flag> flags = new HashMap<TableLocation, Flag>();

	/** Standard colours */
	private static Color
		cNormal = UIManager.getColor("Table.foreground"),
		cNormalSelected = UIManager.getColor("Table.selectionForeground"),
		cDim = dim(cNormal),
		cDimSelected = dim(cNormalSelected),
		cNormalBG = UIManager.getColor("Table.background"),
		cSelectedBG = UIManager.getColor("Table.selectionBackground");

	// TODO Is there a better way to get this colour? Shared TableImp, EditBoxImp
	private static Color cErrorBG = new Color(255, 200, 200);

	TableImp()
	{
		if(headerExtraWidth==-1)
		{
			try
			{
				JWindow w = new JWindow();
				JTable t = new JTable();
				JScrollPane sp = new JScrollPane(t);
				w.getContentPane().setLayout(new BorderLayout());
				w.getContentPane().add(sp, BorderLayout.CENTER);
				OurTableModel model = new OurTableModel(new Element[] {
					XML.parse("<what name='test' type='string' width='0'/>").getDocumentElement()
				});
				t.setModel(model);
				t.getColumnModel().getColumn(0).setMaxWidth(25);
				w.setSize(0, 0);
				w.setVisible(true);
				JLabel headerLabel = (JLabel)t.getTableHeader().getDefaultRenderer();
				headerExtraWidth = headerLabel.getInsets().left+headerLabel.getInsets().right;
				w.dispose();
			}
			catch(XMLException e)
			{
				// Not possible!
				throw new Error("Impossible error checking table widths");
			}
		}

		setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
		setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
		t = new JTable();
		t.setDefaultRenderer(String.class, new FlaggedCellRenderer());
		t.setDefaultEditor(String.class, new CheckedCellEditor());

		// Make headers left-aligned (they are in other Mac apps, plus it
		// looks much better anyhow) - note this is default on Mac now
		JLabel headerRenderer = ((JLabel)t.getTableHeader().getDefaultRenderer());
		headerRenderer.setHorizontalAlignment(JLabel.LEADING);

		// Selection listener
		t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		t.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if(callbackSelect!=null && !inSetSelect)
					getInterface().getOwner().getCallbackHandler().callHandleErrors(callbackSelect);
			}
		});

		// Double-clicks
		t.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if(e.getClickCount()==2 && callbackAction!=null)
				{
					getInterface().getOwner().getCallbackHandler().callHandleErrors(callbackAction);
				}
			}
		});
		t.getActionMap().put("pressedreturn", new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if(callbackAction!=null)
				{
					getInterface().getOwner().getCallbackHandler().callHandleErrors(callbackAction);
				}
			}
		});
		t.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "pressedreturn");

		setViewportView(t);

		publicInterface = new TableInterface();

		wrapper = new JPanel(new BorderLayout());
		wrapper.setOpaque(false);
		wrapper.add(this, BorderLayout.CENTER);
	}

	/** @return List interface */
	public Table getInterface() { return publicInterface; }

	private class OurTableModel extends AbstractTableModel
	{
		private class Col
		{
			Class<?> type;
			String name;
			boolean editable;
			int preferredWidth = -1;
		}
		private Col[] cols;

		private LinkedList<Object[]> data = new LinkedList<Object[]>();

		OurTableModel(Element[] elements)
		{
			cols = new Col[elements.length];
			for(int i=0; i<elements.length; i++)
			{
				cols[i] = new Col();
				String type = elements[i].getAttribute("type");
				if(type.equals("string"))
				{
					cols[i].type = String.class;
				}
				else if(type.equals("boolean"))
				{
					cols[i].type = Boolean.class;
				}
				else
				{
					throw new BugException("Unknown type=: "+type);
				}

				cols[i].name = elements[i].getAttribute("name");
				cols[i].editable = "y".equals(elements[i].getAttribute("editable"));
				if(elements[i].hasAttribute("width"))
				{
					String width = elements[i].getAttribute("width");
					try
					{
						cols[i].preferredWidth = Integer.parseInt(width);
					}
					catch(NumberFormatException nfe)
					{
						throw new BugException("Invalid width=: "+width);
					}
				}
			}
		}

		void setColumnWidths(JTable t)
		{
			Font f = t.getTableHeader().getFont();
			for(int i=0; i<cols.length; i++)
			{
				TableColumn col = t.getColumnModel().getColumn(i);

				int width = cols[i].preferredWidth;
				if(width >= 0)
				{
					int headerWidth = Math.round((float)f.getStringBounds(
						t.getColumnName(i), GraphicsUtils.getFontRenderContext()).getWidth());
					width = Math.max(width, headerWidth + headerExtraWidth + HEADERRIGHTMARGIN);
					col.setPreferredWidth(width);
					col.setMinWidth(width);
					col.setMaxWidth(width);
				}
			}
		}

		@Override
		public int getRowCount()
		{
			return data.size();
		}

		@Override
		public int getColumnCount()
		{
			return cols.length;
		}

		@Override
		public Object getValueAt(int row, int col)
		{
			Object[] rowValues = data.get(row);
			return rowValues[col];
		}

		@Override
		public void setValueAt(Object value, int row, int col)
		{
			internalSetValueAt(value, row, col, true);
		}

		private void internalSetValueAt(Object value, int row, int col, boolean user)
		{
			Object[] rowData = data.get(row);
			if(rowData[col].equals(value))
			{
				return; // Do nothing if no change
			}
			Object before = rowData[col];
			rowData[col] = value;

			if(!user)
			{
				fireTableDataChanged();
			}

			if(user && callbackChange!=null)
			{
				getInterface().getOwner().getCallbackHandler().callHandleErrors(
					callbackChange, row, col, before);
			}
		}

		@Override
		public Class<?> getColumnClass(int col)
		{
			return cols[col].type;
		}

		@Override
		public String getColumnName(int col)
		{
			return cols[col].name;
		}

		@Override
		public boolean isCellEditable(int row, int col)
		{
			if(!cols[col].editable) return false;
			Flag f = flags.get(new TableLocation(row, col));
			return f==null || f.editable;
		}

		int add()
		{
			Object[] newRow = new Object[getColumnCount()];
			for(int i=0; i<cols.length; i++)
			{
				if(cols[i].type==String.class)
				{
					newRow[i] = "";
				}
				else if(cols[i].type==Boolean.class)
				{
					newRow[i] = Boolean.FALSE;
				}
				else
				{
					throw new Error("ar?");
				}
			}
			data.add(newRow);
			int newIndex = data.size()-1;
			fireTableDataChanged();
			return newIndex;
		}

		void remove(int index)
		{
			// Get rid of it from linkedlist
			data.remove(index);
			// Shuffle up the flags
			Map<TableLocation, Flag> newFlags = new TreeMap<TableLocation, Flag>();
			for(Iterator<Map.Entry<TableLocation, Flag>> i=flags.entrySet().iterator();
				i.hasNext(); )
			{
				Map.Entry<TableLocation, Flag> me = i.next();
				TableLocation tl = me.getKey();
				if(tl.index >= index)
				{
					i.remove(); // Remove items on same row
					if(tl.index != index)
					{ // Other ones are shuffled up via a new map (can't modify this one
						// while iterating on it)
						newFlags.put(new TableLocation(tl.index-1, tl.column), me.getValue());
					}
				}
			}
			flags.putAll(newFlags);
			// Fire change
			fireTableDataChanged();
		}

		void setString(int index, int column, String value)
		{
			checkExists(index, column);
			checkType(column, String.class, "string");
			internalSetValueAt(value, index, column, false);
		}

		String getString(int index, int column)
		{
			checkExists(index, column);
			checkType(column, String.class, "string");
			return (String)getValueAt(index, column);
		}

		void setBoolean(int index, int column, boolean value)
		{
			checkExists(index, column);
			checkType(column, Boolean.class, "boolean");
			internalSetValueAt(Boolean.valueOf(value), index, column, false);
		}

		boolean getBoolean(int index, int column)
		{
			checkExists(index, column);
			checkType(column, Boolean.class, "Boolean");
			return ((Boolean)getValueAt(index, column)).booleanValue();
		}

		/**
		 * Checks whether a given index and column are valid.
		 * @param index
		 * @param column
		 * @throws BugException
		 */
		private void checkExists(int index, int column)
		{
			if(index >= getRowCount() || column >= getColumnCount() ||
				index<0 || column<0)
			{
				throw new BugException("Row or column out of range");
			}
		}

		/**
		 * Checks whether a given column is required type
		 * @param column Column number
		 * @param type Type class
		 * @param typeName Type name for use in error
		 * @throws BugException
		 */
		private void checkType(int column, Class<?> type, String typeName)
		{
			if(cols[column].type!=type)
			{
				throw new BugException("Column is not " + typeName + " type");
			}
		}

		public void setEditable(int index, int column, boolean editable)
		{
			checkExists(index, column);
			if(!cols[column].editable)
			{
				throw new BugException("Cannot change editable state of non-editable columns");
			}

			TableLocation tl = new TableLocation(index, column);

			// Get existing flag, if any
			Flag f = flags.get(tl);
			if(f==null)
			{
				if(editable)
				{
					return;
				}
				f = new Flag();
				flags.put(tl, f);
			}

			// Set new flag
			f.editable = editable;
			if(f.canDiscard())
			{
				flags.remove(tl);
			}

			// Repaint
			t.repaint();
		}

		public boolean isEditable(int index, int column)
		{
			checkExists(index, column);
			if(!cols[column].editable)
			{
				return false;
			}
			TableLocation tl = new TableLocation(index, column);
			Flag f = flags.get(tl);
			return f==null || f.editable;
		}

		public void setOverwrite(int index, int column, boolean overwrite)
		{
			checkExists(index, column);
			checkType(column, String.class, "string");

			TableLocation tl = new TableLocation(index, column);

			// Get existing flag, if any
			Flag f = flags.get(tl);
			if(f==null)
			{
				if(!overwrite)
				{
					return;
				}
				f = new Flag();
				flags.put(tl, f);
			}

			// Set new flag
			f.overwrite = overwrite;
			if(f.canDiscard())
				flags.remove(tl);

			// Repaint
			t.repaint();
		}

		public boolean isOverwrite(int index, int column)
		{
			checkExists(index, column);
			if(!cols[column].editable)
			{
				return false;
			}
			TableLocation tl = new TableLocation(index, column);
			Flag f = flags.get(tl);
			return f!=null && f.overwrite;
		}

		public void setDim(int index, int column, boolean dim)
		{
			checkExists(index, column);
			checkType(column, String.class, "string");

			TableLocation tl = new TableLocation(index, column);

			// Get existing flag, if any
			Flag f = flags.get(tl);
			if(f==null)
			{
				if(!dim)
				{
					return;
				}
				f = new Flag();
				flags.put(tl, f);
			}

			// Set new flag
			f.dim = dim;
			if(f.canDiscard())
				flags.remove(tl);

			// Repaint
			t.repaint();
		}

		public boolean isDim(int index, int column)
		{
			checkExists(index, column);
			TableLocation tl = new TableLocation(index, column);
			Flag f = flags.get(tl);
			return f!=null && f.dim;
		}
	}

	private static class Flag
	{
		boolean editable = true;
		boolean overwrite = false;
		boolean dim = false;

		/**
		 * Checks if the flag has default values, meaning it can be discarded.
		 * @return True if flag can be discarded
		 */
		boolean canDiscard() { return editable && !overwrite && !dim; }
	}

	private static Color dim(Color original)
	{
		return new Color(
			original.getRed(), original.getGreen(), original.getBlue(), 128);
	}

	private class FlaggedCellRenderer extends DefaultTableCellRenderer
	{
		@Override
		public Component getTableCellRendererComponent(JTable t, Object o,
			boolean selected, boolean focus, int index, int column)
		{
			Component c = super.getTableCellRendererComponent(
				t, o, selected, focus, index, column);
			TableLocation tl = new TableLocation(index, column);
			Flag f = flags.get(tl);
			if(f==null || (f.editable && !f.dim))
			{
				c.setForeground(selected ? cNormalSelected : cNormal);
			}
			else
			{
				c.setForeground(selected ? cDimSelected : cDim);
			}
			c.setBackground(selected ? cSelectedBG : cNormalBG);
			return c;
		}
	}

	/** Represents a location in table. Suitable for use as key in map */
	private class TableLocation implements Comparable<TableLocation>
	{
		private int index, column;

		TableLocation(int index, int column)
		{
			this.index = index;
			this.column = column;
		}

		@Override
		public boolean equals(Object o)
		{
			if(!(o instanceof TableLocation))
			{
				return false;
			}
			TableLocation tl = (TableLocation)o;
			return tl.index==index && tl.column==column;
		}

		@Override
		public int hashCode()
		{
			return (index + ":" + column).hashCode();
		}

		@Override
		public int compareTo(TableLocation tl)
		{
			if(tl.index < index)
			{
				return 1;
			}
			if(tl.index > index)
			{
				return -1;
			}
			if(tl.column < column)
			{
				return 1;
			}
			if(tl.column > column)
			{
				return -1;
			}
			return 0;
		}
	}

	class CheckedCellEditor extends JTextField implements TableCellEditor, ActionListener
	{
		/** Current and initial values */
		private String value, originalValue;

		/** True when programmatically setting value */
		private boolean inSet = false;

		/** Current index/col editing */
		private int index, col;

		/** True if item is currently invalid (red) */
		private boolean invalid = false;

		/** True if item is currently dim (grey) */
		private boolean dim = false;

		/** Listeners */
		private LinkedList<CellEditorListener> listeners =
			new LinkedList<CellEditorListener>();

		CheckedCellEditor()
		{
			addActionListener(this);
			getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
			getActionMap().put("cancel", new AbstractAction()
			{
				@Override
				public void actionPerformed(ActionEvent ae)
				{
					cancelCellEditing();
				}
			});
			getDocument().addDocumentListener(new DocumentListener()
			{
				@Override
				public void insertUpdate(DocumentEvent arg0)
				{
					changed();
				}

				@Override
				public void removeUpdate(DocumentEvent arg0)
				{
					changed();
				}

				@Override
				public void changedUpdate(DocumentEvent arg0)
				{
					changed();
				}
			});
			addFocusListener(new FocusAdapter()
			{
				@Override
				public void focusLost(FocusEvent e)
				{
					stopCellEditing();
				}
			});
			setBorder(BorderFactory.createEmptyBorder(
				0, t.getColumnModel().getColumnMargin(),
				0, 0));
			setFont(t.getFont());
		}

		@Override
		public void actionPerformed(ActionEvent ae)
		{
			stopCellEditing();
		}

		private void changed()
		{
			value = getText();
			if(callbackEditing!=null && !inSet)
			{
				Table.EditingControl ec = new Table.EditingControl();
				getInterface().getOwner().getCallbackHandler().callHandleErrors(
					callbackEditing, index, col, value, ec);
				invalid = ec.isError();
				dim = ec.isDim();
				updateColours();
			}
		}

		@Override
		public Component getTableCellEditorComponent(JTable t, Object o,
			boolean selected, int index, int col)
		{
			value = originalValue = (String)o;
			invalid = false;
			dim = false;
			this.index = index;
			this.col = col;
			try
			{
				inSet = true;
				setText(value);
			}
			finally
			{
				inSet = false;
			}

			TableLocation tl = new TableLocation(index, col);
			Flag f = flags.get(tl);
			if(f!=null && f.overwrite)
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						selectAll();
					}
				});
			}

			updateColours();

			return this;
		}

		private void updateColours()
		{
			if(invalid)
			{
				setForeground(cNormal);
				setBackground(cErrorBG);
			}
			else if(dim)
			{
		 		setForeground(cDim);
		 		setBackground(cNormalBG);
			}
			else
			{
		 		setForeground(cNormal);
		 		setBackground(cNormalBG);
			}
		}

		@Override
		public Object getCellEditorValue()
		{
			if(invalid)
			{
				return originalValue;
			}
			else
			{
				return value;
			}
		}

		@Override
		public boolean isCellEditable(EventObject e)
		{
			return true;
		}

		@Override
		public boolean shouldSelectCell(EventObject e)
		{
			return true;
		}

		@Override
		public boolean stopCellEditing()
		{
			// Send to listeners
			CellEditorListener[] listenersArray =
				listeners.toArray(new CellEditorListener[listeners.size()]);
			ChangeEvent ce = new ChangeEvent(this);
			for(int i=0; i<listenersArray.length; i++)
			{
				listenersArray[i].editingStopped(ce);
			}
			return true;
		}

		@Override
		public void cancelCellEditing()
		{
			value = originalValue;
			// Send to listeners
			CellEditorListener[] listenersArray =
				listeners.toArray(new CellEditorListener[listeners.size()]);
			ChangeEvent ce = new ChangeEvent(this);
			for(int i=0; i<listenersArray.length; i++)
			{
				listenersArray[i].editingCanceled(ce);
			}
		}

		@Override
		public void addCellEditorListener(CellEditorListener listener)
		{
			listeners.add(listener);
		}

		@Override
		public void removeCellEditorListener(CellEditorListener listener)
		{
			listeners.remove(listener);
		}
	}

	/** Class implementing combo interface */
	class TableInterface extends BasicWidget implements Table, InternalWidget
	{
		int width = -1;
		private boolean multiSelect = false;

		@Override
		public int getContentType()
		{
			return CONTENT_NONE;
		}

		public TableInterface()
		{
			setRows(4);
		}

		@Override
		public void addXMLChild(String slotName, Widget child)
		{
			throw new BugException("Tables cannot contain children");
		}

		@Override
		public String[] getReservedChildren()
		{
			return new String[] { "column" };
		}

		@Override
		public void setReservedData(Element[] elements)
		{
			otm = new OurTableModel(elements);
			t.setModel(otm);
			otm.setColumnWidths(t);
		}

		@Override
		public JComponent getJComponent()
		{
			return wrapper;
		}

		@Override
		public int getPreferredWidth()
		{
			if(width!=-1)
			{
				return width;
			}
			else
			{
				return getPreferredSize().width;
			}
		}

		@Override
		public int getPreferredHeight(int width)
		{
			return getPreferredSize().height;
		}

		@Override
		public void setWidth(int width)
		{
			this.width = width;
		}

		@Override
		public int addItem()
		{
			stopEditing();
			return otm.add();
		}

		void stopEditing()
		{
			if(t.isEditing())
			{
				t.getCellEditor().stopCellEditing();
			}
		}

		@Override
		public void removeItem(int index)
		{
			stopEditing();
			otm.remove(index);
		}

		@Override
		public void clear()
		{
			stopEditing();
			while(otm.getRowCount()>0)
				otm.remove(0);
		}

		@Override
		public void setString(int index, int column, String value)
		{
			otm.setString(index, column, value);
		}

		@Override
		public String getString(int index, int column)
		{
			return otm.getString(index, column);
		}

		@Override
		public void setBoolean(int index, int column, boolean value)
		{
			otm.setBoolean(index, column, value);
		}

		@Override
		public boolean getBoolean(int index, int column)
		{
			return otm.getBoolean(index, column);
		}

		@Override
		public void setOnChange(String callback)
		{
			getInterface().getOwner().getCallbackHandler().check(callback,
				new Class[] {int.class, int.class, Object.class});
			TableImp.this.callbackChange = callback;
		}

		@Override
		public void setOnEditing(String callback)
		{
			getInterface().getOwner().getCallbackHandler().check(callback,
				new Class[] {int.class, int.class, String.class, Table.EditingControl.class});
			TableImp.this.callbackEditing = callback;
		}

		@Override
		public void setOnSelect(String callback)
		{
			getInterface().getOwner().getCallbackHandler().check(callback);
			TableImp.this.callbackSelect = callback;
		}

		@Override
		public void setEditable(int index, int column, boolean editable)
		{
			otm.setEditable(index, column, editable);
		}

		@Override
		public boolean isEditable(int index, int column)
		{
			return otm.isEditable(index, column);
		}

		@Override
		public void setDim(int index, int column, boolean dim)
		{
			otm.setDim(index, column, dim);
		}

		@Override
		public boolean isDim(int index, int column)
		{
			return otm.isDim(index, column);
		}

		@Override
		public void setOverwrite(int index, int column, boolean overwrite)
		{
			otm.setOverwrite(index, column, overwrite);
		}

		@Override
		public boolean isOverwrite(int index, int column)
		{
			return otm.isOverwrite(index, column);
		}

		@Override
		public int getNumItems()
		{
			return otm.getRowCount();
		}

		@Override
		public void setRows(int rows)
		{
			t.setPreferredScrollableViewportSize(new Dimension(
				getPreferredWidth(), rows*(t.getRowHeight()+t.getRowMargin())
				));
		}

		@Override
		public int getSelectedIndex()
		{
			if(multiSelect)
			{
				throw new BugException("Can't call getSelectedIndex on a MultiSelect table");
			}
			return t.getSelectedRow();
		}

		@Override
		public int[] getSelectedIndices()
		{
			return t.getSelectedRows();
		}

		@Override
		public void setSelectedIndex(int selected)
		{
			try
			{
				inSetSelect = true;
				t.getSelectionModel().clearSelection();
				t.getSelectionModel().addSelectionInterval(selected, selected);
			}
			finally
			{
				inSetSelect = false;
			}
		}

		@Override
		public void setSelectedIndices(int[] selected)
		{
			try
			{
				t.getSelectionModel().clearSelection();
				for(int i=0; i<selected.length; i++)
				{
					t.getSelectionModel().addSelectionInterval(selected[i], selected[i]+1);
				}
			}
			finally
			{
				inSetSelect = false;
			}
		}

		@Override
		public void setMultiSelect(boolean multiSelect)
		{
			this.multiSelect = multiSelect;
			t.setSelectionMode(multiSelect
				? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
				: ListSelectionModel.SINGLE_SELECTION);
		}

		@Override
		public void setOnAction(String callback)
		{
			callbackAction = callback;
		}

		@Override
		public void setMacIndent(boolean macIndent)
		{
			setMacIndent(macIndent ? SupportsMacIndent.TYPE_EDIT_LEGACY
				: SupportsMacIndent.TYPE_NONE);
		}

		@Override
		public void setMacIndent(String macIndent)
		{
			int newValue = UISingleton.getMacIndent(macIndent);
			if(newValue != TableImp.this.macIndent)
			{
				TableImp.this.macIndent = newValue;
				UISingleton.runInSwing(new Runnable()
				{
					@Override
					public void run()
					{
						wrapper.setBorder(TableImp.this.macIndent == 0 ? null :
							BorderFactory.createEmptyBorder(0, TableImp.this.macIndent, 0, 0));
					}
				});
			}
		}
	}
}
