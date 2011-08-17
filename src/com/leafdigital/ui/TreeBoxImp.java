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
import java.awt.dnd.DragSource;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import com.leafdigital.ui.api.*;
import com.leafdigital.ui.api.TreeBox.*;

import leafchat.core.api.BugException;

/** Combo box */
public class TreeBoxImp extends JScrollPane implements TreeSelectionListener
{
	private JTree t;
	private TreeBox.Handler th=null;
	private int disableSelectionEvents;

	private class TreeBoxModel implements TreeModel
	{
		private java.util.List<TreeModelListener> listeners =
			new LinkedList<TreeModelListener>();

		@Override
		public Object getRoot()
		{
			if(th==null)
				return null;
			else
				return th.getRoot();
		}

		@Override
		public Object getChild(Object oParent,int iIndex)
		{
			return ((TreeBox.Item)oParent).getChildren()[iIndex];
		}

		@Override
		public int getChildCount(Object oParent)
		{
			if(oParent==null)
				return 0;
			else
				return ((TreeBox.Item)oParent).getChildren().length;
		}

		@Override
		public boolean isLeaf(Object oParent)
		{
			return ((TreeBox.Item)oParent).isLeaf();
		}

		@Override
		public void valueForPathChanged(TreePath arg0,Object arg1)
		{
			// Ignore, not editable
			throw new Error("Can't edit trees");
		}

		@Override
		public int getIndexOfChild(Object oParent,Object oChild)
		{
			if(oParent==null || oChild==null) return -1;

			TreeBox.Item[] aiSearch=((TreeBox.Item)oParent).getChildren();

			for(int i=0;i<aiSearch.length;i++)
			{
				if(aiSearch[i]==oChild) return i;
			}
			return -1;
		}

		@Override
		public void addTreeModelListener(TreeModelListener tml)
		{
			synchronized(listeners)
			{
				listeners.add(tml);
			}
		}

		@Override
		public void removeTreeModelListener(TreeModelListener tml)
		{
			synchronized(listeners)
			{
				listeners.remove(tml);
			}
		}

		private void update()
		{
			try
			{
				disableSelectionEvents++;
				synchronized(listeners)
				{
					for(TreeModelListener tml : listeners)
					{
						tml.treeStructureChanged(new TreeModelEvent(
							TreeBoxImp.this,new Object[] {getRoot()}
							));
					}
				}
			}
			finally
			{
				disableSelectionEvents--;
			}

			// Selection might have changed
			valueChanged(null);
		}

		TreePath find(Item i)
		{
			if(th==null) return null;
			Item iRoot=th.getRoot();
			TreePath tp=new TreePath(iRoot);
			return recursiveFind(i,iRoot,tp);
		}

		private TreePath recursiveFind(Item iFind,Item iCurrent,TreePath tpCurrent)
		{
			if(iFind==iCurrent) return tpCurrent;
			Item[] ai=iCurrent.getChildren();
			for(int i=0;i<ai.length;i++)
			{
				TreePath tpResult=recursiveFind(iFind,ai[i],tpCurrent.pathByAddingChild(ai[i]));
				if(tpResult!=null) return tpResult;
			}
			return null;
		}
	}

	class TreeBoxRenderer extends DefaultTreeCellRenderer
	{
		@Override
		public Component getTreeCellRendererComponent(JTree t,Object o,boolean bSelected,boolean bExpanded,
			boolean bLeaf,int iRow,boolean bHasFocus)
		{
			boolean dragging=((DragPaintTree)t).isDragging(iRow);
			super.getTreeCellRendererComponent(t,o,!dragging && bSelected,bExpanded,bLeaf,iRow,bHasFocus);
			if(dragging)
			{
				Color fg=getForeground();
				setForeground(new Color(fg.getRed(),fg.getGreen(),fg.getBlue(),128));
			}

			if(o==t.getModel())
			{
				setText("[Root]");
			}
			else
			{
				TreeBox.Item ti=(TreeBox.Item)o;
				setText(ti.getText());
				if(ti.getIcon()!=null)
					setIcon(new ImageIcon(ti.getIcon()));
			}

			return this;
		}
	}

	TreeBoxImp()
	{
		setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
		setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
		DragHandler dh=new DragHandler();
		t=new DragPaintTree(dh);
		t.setModel(new TreeBoxModel());
		t.setCellRenderer(new TreeBoxRenderer());
		t.addTreeSelectionListener(this);
		t.addMouseListener(dh);
		t.addMouseMotionListener(dh);
		setViewportView(t);
	}

	private final static int POSITION_ABOVE=1,POSITION_BELOW=2,POSITION_ON=3;

	private static class DragPaintTree extends JTree
	{
		private DragHandler dh;
		DragPaintTree(DragHandler dh)
		{
			this.dh=dh;
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			dh.paintHighlight(g);
		}

		boolean isDragging(int row)
		{
			return dh.dragging!=null && dh.dragging.row==row;
		}
	}

	private class DragHandler implements MouseListener,MouseMotionListener
	{
		PositionDetails dragging,highlight;

		private Point getPoint(MouseEvent e)
		{
			Point
				scrollPanePos=TreeBoxImp.this.getLocationOnScreen(),
				treePos=t.getLocationOnScreen();
			return new Point(e.getX()+scrollPanePos.x-treePos.x,
				e.getY()+scrollPanePos.y-treePos.y);
		}

		private void setCursor(Cursor c)
		{
			t.	setCursor(c);
		}

		private void paintHighlight(Graphics g)
		{
			if(dragging==null) return;

			Color foreground=t.getForeground();
			if(dragging.row!=-1)
			{
				Rectangle start=t.getRowBounds(dragging.row);
				g.setColor(new Color(
					foreground.getRed(),foreground.getGreen(),foreground.getBlue(),128));
				g.drawRect(start.x,start.y,start.width,start.height);
			}
			if(highlight!=null && highlight.row!=-1)
			{
				Rectangle target=t.getRowBounds(highlight.row);
				g.setColor(foreground);
				switch(highlight.position)
				{
				case POSITION_ON:
					g.drawRect(target.x,target.y,t.getWidth()-target.x,target.height);
					break;
				case POSITION_ABOVE:
					g.drawLine(target.x,target.y-1,t.getWidth(),target.y-1);
					break;
				case POSITION_BELOW:
					g.drawLine(target.x,target.y+target.height-1,t.getWidth(),target.y+target.height-1);
					break;
				}
			}

		}

		private class PositionDetails
		{
			Item i;
			int row;
			int position;

			Item parent;
			int parentPos;
		}

		private PositionDetails getPositionDetails(MouseEvent e,boolean loose)
		{
			Point p=getPoint(e);
			int row=loose ? t.getClosestRowForLocation(p.x,p.y) : t.getRowForLocation(p.x,p.y);
			if(row==-1) return null;

			PositionDetails pd=new PositionDetails();

			TreePath tp=t.getPathForRow(row);
			pd.i=(Item)tp.getLastPathComponent();
			pd.row=row;
			if(pd.i.getParent()==null) return null;

			Rectangle bounds=t.getRowBounds(row);
			if(!pd.i.isLeaf() &&
				p.y>bounds.y+3 && p.y<bounds.y+bounds.height-3)
			{
				pd.position=POSITION_ON;
				pd.parent=pd.i;
				pd.parentPos=pd.parent.getChildren().length;
			}
			else if(p.y<(bounds.y+bounds.height/2))
			{
				pd.position=POSITION_ABOVE;
				pd.parent=pd.i.getParent();
				Item[] siblings=pd.parent.getChildren();
				for(int i=0;i<siblings.length;i++)
				{
					if(siblings[i]==pd.i)
					{
						pd.parentPos=i;
					}
				}
			}
			else
			{
				pd.position=POSITION_BELOW;
				pd.parent=pd.i.getParent();
				Item[] siblings=pd.parent.getChildren();
				for(int i=0;i<siblings.length;i++)
				{
					if(siblings[i]==pd.i)
					{
						pd.parentPos=i+1;
					}
				}
			}

			return pd;
		}

		@Override
		public void mousePressed(MouseEvent e)
		{
			PositionDetails pd=getPositionDetails(e,false);
			if(pd!=null && (th instanceof DragSingleSelectionHandler) &&
				((DragSingleSelectionHandler)th).canDrag(pd.i))
			{
				setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
				dragging=pd;
				t.repaint();
			}
		}

		private boolean sameItem(PositionDetails pd)
		{
			if(pd.i==dragging.i) return true; // Same item
			if(pd.parent!=dragging.parent) return false; // Different parent
			// Same parent, but are we looking at same pos?
			for(int pos=0;pos<dragging.i.getParent().getChildren().length;pos++)
			{
				if(dragging.i.getParent().getChildren()[pos]==dragging.i)
				{
					return pos==pd.parentPos;
				}
			}
			return false; // wtf?
		}

		@Override
		public void mouseDragged(MouseEvent e)
		{
			if(dragging==null) return;
			highlight=getPositionDetails(e,false);
			if(highlight!=null && !sameItem(highlight) &&
				((DragSingleSelectionHandler)th).canDragTo(
				dragging.i,highlight.parent,getTargetPos()))
			{
				t.repaint();
				setCursor(DragSource.DefaultMoveDrop);
			}
			else
			{
				highlight=null;
				t.repaint();
				setCursor(DragSource.DefaultMoveNoDrop);
			}
		}

		private int getTargetPos()
		{
			// If the items are in the same container, we adjust the index to assume
			// that the original item had been removed
			int targetPos=highlight.parentPos;
			if(dragging.i.getParent()==highlight.parent)
			{
				for(int i=0;i<targetPos;i++)
				{
					if(dragging.i.getParent().getChildren()[i]==dragging.i)
					{
						targetPos--;
						break;
					}
				}
			}
			return targetPos;
		}

		@Override
		public void mouseReleased(MouseEvent e)
		{
			if(dragging==null) return;

			if(highlight!=null)
			{
				((DragSingleSelectionHandler)th).dragTo(
					dragging.i,highlight.parent,getTargetPos());
			}

			dragging=null;
			highlight=null;
			t.repaint();
			setCursor(
				Cursor.getDefaultCursor());
		}

		@Override
		public void mouseMoved(MouseEvent e) 	{}
		@Override
		public void mouseClicked(MouseEvent e) 	{}
		@Override
		public void mouseEntered(MouseEvent e) 	{}
		@Override
		public void mouseExited(MouseEvent e) 	{}
	}

	/** @return Tree interface */
	public TreeBox getInterface() { return tbInterface; }

	/** Tree interface */
	TreeBox tbInterface=new TreeBoxInterface();

	/** Class implementing tree box interface */
	class TreeBoxInterface extends BasicWidget implements TreeBox, InternalWidget
	{
		private int iWidth=-1,height=-1;

		@Override
		public int getContentType() { return CONTENT_NONE; }

		@Override
		public void addXMLChild(String sSlotName, Widget wChild)
		{
			throw new BugException("Trees cannot contain children");
		}

		@Override
		public JComponent getJComponent()
		{
			return TreeBoxImp.this;
		}

		@Override
		public int getPreferredWidth()
		{
			if(iWidth!=-1)
				return iWidth;
			else
				return getPreferredSize().width;
		}

		@Override
		public int getPreferredHeight(int iWidth)
		{
			if(height!=-1)
				return height;
			else
				return getPreferredSize().height;
		}

		@Override
		public void setHandler(Handler th)
		{
			TreeBoxImp.this.th=th;
			t.setRootVisible(th.isRootDisplayed());

			if(th instanceof MultiSelectionHandler)
			{
				t.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
			}
			else
			{
				t.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			}

			try
			{
				disableSelectionEvents++;
				update();
			}
			finally
			{
				disableSelectionEvents--;
			}
		}

		@Override
		public void update()
		{
			((TreeBoxModel)t.getModel()).update();
		}

		@Override
		public void setWidth(int iWidth)
		{
			this.iWidth=iWidth;
		}

		@Override
		public void setHeight(int height)
		{
			this.height=height;
		}

		@Override
		public void select(Item i)
		{
			try
			{
				disableSelectionEvents++;
				t.setSelectionPath(((TreeBoxModel)t.getModel()).find(i));
			}
			finally
			{
				disableSelectionEvents--;
			}
		}

		@Override
		public void select(Item[] ai)
		{
			try
			{
				disableSelectionEvents++;
				TreePath[] atp=new TreePath[ai.length];
				for(int i=0;i<atp.length;i++)
				{
					atp[i]=((TreeBoxModel)t.getModel()).find(ai[i]);
				}
				t.setSelectionPaths(atp);
			}
			finally
			{
				disableSelectionEvents--;
			}
		}
	}

	private TreeBox.Item lastSingleSelection;
	private TreeBox.Item[] lastMultiSelection;

	@Override
	public void valueChanged(TreeSelectionEvent e)
	{
		if(disableSelectionEvents>0) return;
		if(th==null) return;
		if(th instanceof SingleSelectionHandler)
		{
			SingleSelectionHandler ssh=(SingleSelectionHandler)th;
			TreePath tp=t.getSelectionPath();
			TreeBox.Item newSelection;
			if(tp==null)
				newSelection=null;
			else
				newSelection=(TreeBox.Item)tp.getLastPathComponent();
			if(newSelection!=lastSingleSelection)
			{
				lastSingleSelection=newSelection;
				ssh.selected(newSelection);
			}
		}
		else if(th instanceof MultiSelectionHandler)
		{
			TreePath[] atp=t.getSelectionPaths();
			if(atp==null) atp=new TreePath[0];
			Item[] ai=new Item[atp.length];
			for(int i=0;i<ai.length;i++)
			{
				ai[i]=(TreeBox.Item)atp[i].getLastPathComponent();
			}
			if(lastMultiSelection==null || !Arrays.equals(ai,lastMultiSelection))
			{
				lastMultiSelection=ai;
				((MultiSelectionHandler)th).selected(ai);
			}
		}
	}
}
