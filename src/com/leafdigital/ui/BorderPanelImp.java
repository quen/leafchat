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

Copyright 2012 Samuel Marshall.
*/
package com.leafdigital.ui;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.JComponent;

import com.leafdigital.ui.api.*;

import leafchat.core.api.BugException;

import static com.leafdigital.ui.api.BorderPanel.*;

/**
 * Implements BorderPanel
 */
public class BorderPanelImp extends JComponent
{
	// Conceptual grid and notation used
	//
	//    <--L--> <--C--> <--R-->
	//
	//   +-------+-------+-------+
	// | |       |       |       |
	// T |  NW   |  N    |   NE  |
	// | |       |       |       |
	//   +-------+-------+-------+
	// | |       |       |       |
	// M |  W    | CENTR |    E  |
	// | |       |       |       |
	//   +-------+-------+-------+
	// | |       |       |       |
	// B |  SW   |  S    |   SE  |
	// | |       |       |       |
	//   +-------+-------+-------+

	/** Number of slots in BorderPanel */
	private final static int SLOTS = 9;

	/** One of the CORNERS_xxx constants */
	private int cornerHandling;

	/** Spacing between grid squares */
	private int spacing = 0;

	/** Border at edge of grid */
	private int border = 0;

	/** Keep record of held components */
	private InternalWidget[] slots = new InternalWidget[SLOTS];

	/** Constructor */
	BorderPanelImp()
	{
		setLayout(null);
		setOpaque(false);
	}

	@Override
	public void setBounds(int x, int y, int width, int height)
	{
		super.setBounds(x, y, width, height);
		updateLayout();
	}

	@Override
	public void validate()
	{
		super.validate();
		updateLayout();
	}

	@Override
	public Dimension getPreferredSize()
	{
		InternalWidget iw = (InternalWidget)publicInterface;
		int width = iw.getPreferredWidth();
		return new Dimension(width, iw.getPreferredHeight(width));
	}

	/**
	 * @param slot Slot (NORTH, NORTHEAST, etc)
	 * @return Preferred width of component at that slot, or 0 if none
	 */
	private int prefW(int slot)
	{
		if(slots[slot] == null || !slots[slot].isVisible())
		{
			return 0;
		}
		return slots[slot].getPreferredWidth();
	}

	/**
	 * @param slot Slot (NORTH, NORTHEAST, etc)
	 * @param width Available width
	 * @return Preferred height of slot's component at that width, or 0
	 */
	private int prefH(int slot, int width)
	{
		if(slots[slot] == null || !slots[slot].isVisible() || width == 0)
		{
			return 0;
		}
		return slots[slot].getPreferredHeight(width);
	}

	/**
	 * @param slot Slot (NORTH, NORTHEAST, etc)
	 * @return True if the slot is empty
	 */
	private boolean isEmpty(int slot)
	{
		return slots[slot] == null;
	}

	/**
	 * Moves a component.
	 * @param slot Slot of component to move
	 * @param x X position
	 * @param y Y position
	 * @param width Width
	 * @param height Height
	 */
	private void move(int slot, int x, int y, int width, int height)
	{
		if(slots[slot] == null)
		{
			return;
		}
		slots[slot].getJComponent().setBounds(x, y, width, height);
	}

	/** Move all components to their correct place in new layout */
	private void updateLayout()
	{
		UISingleton.checkSwing();

		// Get desired width and height, and modify to fit actual space available
		GridWidths gw = new GridWidths();
		gw.fitWidth(getWidth() - border * 2);
		GridHeights gh = new GridHeights(gw);
		gh.fitHeight(getHeight() - border * 2);

		// Calculate positions
		int
			x1 = border,
			x2 = border + gw.l + gw.gutterLC,
			x3 = border + gw.l + gw.gutterLC + gw.c + gw.gutterCR;
		int
			y1 = border,
			y2 = border + gh.t + gh.gutterTM,
			y3 = border + gh.t + gh.gutterTM + gh.m + gh.gutterMB;

		// Corner components always go in the same places
		move(NORTHWEST, x1, y1, gw.l, gh.t);
		move(NORTHEAST, x3, y1, gw.r, gh.t);
		move(SOUTHWEST, x1, y3, gw.l, gh.b);
		move(SOUTHEAST, x3, y3, gw.r, gh.b);

		switch(cornerHandling)
		{
			case BorderPanel.CORNERS_LEAVEBLANK :
			{
				// Put all components in their grid squares
				move(NORTH, x2, y1, gw.c, gh.t);
				move(EAST, x3, y2, gw.r, gh.m);
				move(SOUTH, x2, y3, gw.c, gh.b);
				move(WEST, x1, y2, gw.l, gh.m);
				move(CENTRAL, x2, y2, gw.c, gh.m);
				break;
			}
			case BorderPanel.CORNERS_HORIZONTALFILL:
			{
				// E and W don't take up other slots
				move(EAST, x3, y2, gw.r, gh.m);
				move(WEST, x1, y2, gw.l, gh.m);

				int nStart = x2, nWidth = gw.c;
				if(isEmpty(NORTHWEST))
				{
					nStart = x1;
					nWidth+= gw.l + gw.gutterLC;
				}
				if(isEmpty(NORTHEAST))
				{
					nWidth += gw.gutterCR + gw.r;
				}
				move(NORTH, nStart, y1, nWidth, gh.t);

				int sStart = x2, sWidth = gw.c;
				if(isEmpty(SOUTHWEST))
				{
					sStart = x1;
					sWidth += gw.l + gw.gutterLC;
				}
				if(isEmpty(SOUTHEAST))
				{
					sWidth += gw.gutterCR + gw.r;
				}
				move(SOUTH, sStart, y3, sWidth, gh.b);

				int cStart = x2, cWidth = gw.c;
				if(isEmpty(WEST))
				{
					cStart = x1;
					cWidth += gw.l + gw.gutterLC;
				}
				if(isEmpty(EAST))
				{
					cWidth += gw.gutterCR + gw.r;
				}
				move(CENTRAL, cStart, y2, cWidth, gh.m);

				break;
			}
			case BorderPanel.CORNERS_VERTICALFILL:
			{
				// N and S don't take up other slots
				move(NORTH, x2, y1, gw.c, gh.t);
				move(SOUTH, x2, y3, gw.c, gh.b);

				int wStart = y2, wHeight = gh.m;
				if(isEmpty(NORTHWEST))
				{
					wStart = y1;
					wHeight += gh.t + gh.gutterTM;
				}
				if(isEmpty(SOUTHWEST))
				{
					wHeight += gh.gutterMB + gh.b;
				}
				move(WEST, x1, wStart, gw.l, wHeight);

				int eStart = y2, eHeight = gh.m;
				if(isEmpty(NORTHEAST))
				{
					eStart = y1;
					eHeight += gh.t + gh.gutterTM;
				}
				if(isEmpty(SOUTHEAST))
				{
					eHeight += gh.gutterMB + gh.b;
				}
				move(EAST, x3, eStart, gw.r, eHeight);

				int cStart = y2, cHeight = gh.m;
				if(isEmpty(NORTH))
				{
					cStart = y1;
					cHeight += gh.t + gh.gutterTM;
				}
				if(isEmpty(SOUTH))
				{
					cHeight += gh.gutterMB + gh.b;
				}
				move(CENTRAL, x2, cStart, gw.c, cHeight);

				break;
			}
		}

		repaint();
	}

	/** @return Interface giving limited public access */
	BorderPanel getInterface()
	{
		return publicInterface;
	}

	/**
	 * @param i1 First number
	 * @param i2 Second number
	 * @param i3 Third number
	 * @return Maximum of the three parameters
	 */
	private static int max(int i1, int i2, int i3)
	{
		if(i1 > i2)
		{
			return (i1 > i3) ? i1 : i3;
		}
		else
		{
			return (i2 > i3) ? i2 : i3;
		}
	}

	/**
	 * Class manages the three widths in the grid.
	 */
	class GridWidths
	{
		int l, c, r;
		int gutterLC, gutterCR;

		/** @return Total width this represents */
		int getTotalWidth()
		{
			return l + gutterLC + c + gutterCR + r;
		}

		/** Construct with desired widths */
		GridWidths()
		{
			// Calculate L and R; these depend straightforwardly on the things in
			// those corners
			l = max(prefW(NORTHWEST), prefW(WEST), prefW(SOUTHWEST));
			r = max(prefW(NORTHEAST), prefW(EAST), prefW(SOUTHEAST));

			// Calculate gutters, depending on whether there's anything in the places
			gutterLC = (l == 0) ? 0 : spacing;
			gutterCR = (r == 0) ? 0 : spacing;

			// Calculate C, which depends on whether the N component expands or not
			if(cornerHandling == BorderPanel.CORNERS_HORIZONTALFILL)
			{
				int
					iTPref = prefW(NORTH) -
					(isEmpty(NORTHWEST) ? (l + gutterLC) : 0) -
						(isEmpty(NORTHEAST) ? (r + gutterCR) : 0),
					iMPref = prefW(CENTRAL) -
						(isEmpty(WEST) ? (l + gutterLC) : 0) -
						(isEmpty(EAST) ? (r + gutterCR) : 0),
					iBPref = prefW(SOUTH) -
						(isEmpty(SOUTHWEST) ? (l + gutterLC) : 0) -
						(isEmpty(SOUTHEAST) ? (r + gutterCR) : 0);

				c = max(iTPref, iMPref, iBPref);
			}
			else // CORNERS_LEAVEBLANK or .CORNERS_VERTICALFILL
			{
				c = max(prefW(NORTH), prefW(CENTRAL), prefW(SOUTH));
			}
		}

		/**
		 * Fit to a different available width.
		 * @param availableWidth Required width
		 */
		void fitWidth(int availableWidth)
		{
			int extraSpace = availableWidth - getTotalWidth();
			if(extraSpace>0)
			{
				c += extraSpace;
			}
			else if(extraSpace < 0)
			{
				c += extraSpace;
				if(c < 0)
				{
					int overflow = -c;
					c = 0;
					l -= overflow / 2;
					r -= (overflow + 1) / 2; // The +1 ensures that we use the complete total
					if(l < 0)
					{
						int overflowOverflow = -l;
						l = 0;

						r -= overflowOverflow;
						if(r < 0)
						{
							r = 0;
						}
					}
					else if(r < 0)
					{
						int overflowOverflow = -r;
						r = 0;

						l -= overflowOverflow;
						if(l < 0)
						{
							l = 0;
						}
					}
				}
			}
		}
	}

	/**
	 * Manages the three heights in the grid.
	 */
	class GridHeights
	{
		int t, m, b;
		int gutterTM, gutterMB;

		int getTotalHeight()
		{
			return t + gutterTM + m + gutterMB + b;
		}

		/**
		 * Find the desired grid heights for given widths.
		 * @param gw Widths data
		 */
		GridHeights(GridWidths gw)
		{
			if(cornerHandling == BorderPanel.CORNERS_HORIZONTALFILL)
			{
				t = max(
					prefH(NORTHWEST, gw.l),
					prefH(NORTHEAST, gw.r),
					prefH(NORTH, gw.c +
						(isEmpty(NORTHWEST) ? (gw.l + gw.gutterLC) : 0) +
						(isEmpty(NORTHEAST) ? (gw.r + gw.gutterCR) : 0)
						)
					);
				b = max(
					prefH(SOUTHWEST, gw.l),
					prefH(SOUTHEAST, gw.r),
					prefH(SOUTH, gw.c +
						(isEmpty(SOUTHWEST) ? (gw.l + gw.gutterLC) : 0) +
						(isEmpty(SOUTHEAST) ? (gw.r + gw.gutterCR) : 0)
						)
					);
			}
			else // CORNERS_LEAVEBLANK or CORNERS_VERTICALFILL
			{
				t = max(prefH(NORTHWEST, gw.l), prefH(NORTH, gw.c), prefH(NORTHEAST, gw.r));
				b = max(prefH(SOUTHWEST, gw.l), prefH(SOUTH, gw.c), prefH(SOUTHEAST, gw.r));
			}

			// Calculate gutters, depending on whether there's anything in the places
			gutterTM = (t > 0) ? spacing : 0;
			gutterMB = (b > 0) ? spacing : 0;

			if(cornerHandling == BorderPanel.CORNERS_VERTICALFILL)
			{
				int
					iLPref = prefH(WEST, gw.l) -
						(isEmpty(NORTHWEST) ? (t + gutterTM) : 0) -
						(isEmpty(SOUTHWEST) ? (b + gutterMB) : 0),
					iCPref = prefH(CENTRAL, gw.c) -
						(isEmpty(NORTH) ? (t + gutterTM) : 0) -
						(isEmpty(SOUTH) ? (b + gutterMB) : 0),
					iRPref = prefH(EAST, gw.r) -
						(isEmpty(NORTHEAST) ? (t + gutterTM) : 0) -
						(isEmpty(SOUTHEAST) ? (b + gutterMB) : 0);

				m = max(iLPref, iCPref, iRPref);
			}
			else // CORNERS_LEAVEBLANK or CORNERS_HORIZONTALFILL
			{
				m = max(prefH(WEST, gw.l), prefH(CENTRAL, gw.c), prefH(EAST, gw.r));
			}
		}

		/**
		 * Fit to specified height.
		 * @param availableHeight Height to be used
		 */
		void fitHeight(int availableHeight)
		{
			int extraSpace = availableHeight - getTotalHeight();
			if(extraSpace>0)
			{
				m += extraSpace;
			}
			else if(extraSpace < 0)
			{
				m += extraSpace;
				if(m < 0)
				{
					int overflow = -m;
					m = 0;
					t -= overflow/2;
					t -= (overflow + 1)/2; // The +1 ensures that we use the complete total
					if(t < 0)
					{
						int overflowOverflow = -t;
						t = 0;

						b -= overflowOverflow;
						if(b < 0)
						{
							b = 0;
						}
					}
					else if(b < 0)
					{
						int overflowOverflow = -b;
						b = 0;

						t -= overflowOverflow;
						if(t < 0)
						{
							t = 0;
						}
					}
				}
			}
		}
	}

	/** Interface available to public */
	private BorderPanel publicInterface = new BorderPanelInterface();

	/**
	 * Interface available to public.
	 */
	class BorderPanelInterface extends BasicWidget implements BorderPanel, InternalWidget
	{
		@Override
		public int getContentType()
		{
			return CONTENT_NAMEDSLOTS;
		}

		@Override
		public void set(final int slot, Widget w)
		{
			final InternalWidget iw = (InternalWidget)w;
			iw.setParent(this);
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					if(slots[slot] != null)
					{
						BorderPanelImp.this.remove(slots[slot].getJComponent());
						slots[slot] = null;
					}
					if(iw != null)
					{
						add(iw.getJComponent());
						iw.getJComponent().revalidate();
						slots[slot] = iw;
					}
					updateLayout();
				}
			});
		}

		@Override
		public Widget get(int slot)
		{
			return slots[slot];
		}

		@Override
		public Widget[] getWidgets()
		{
			List<Widget> all = new LinkedList<Widget>();
			for(InternalWidget w : slots)
			{
				if(w != null)
				{
					all.add(w);
				}
			}
			return all.toArray(new Widget[all.size()]);
		}

		@Override
		public void setCornerHandling(final int iCorners)
		{
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					switch(iCorners)
					{
						case BorderPanel.CORNERS_HORIZONTALFILL:
						case BorderPanel.CORNERS_VERTICALFILL:
						case BorderPanel.CORNERS_LEAVEBLANK:
						{
							cornerHandling = iCorners;
							updateLayout();
							return;
						}

						default:
							throw new IllegalArgumentException("Corner value not supported");
					}
				}
			});
		}

		@Override
		public void remove(Widget w)
		{
			final InternalWidget iw = (InternalWidget)w;
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					for(int i=0; i<slots.length; i++)
					{
						if(slots[i] == iw)
						{
							slots[i] = null;
							BorderPanelImp.this.remove(iw.getJComponent());
						}
					}
					updateLayout();
				}
			});
		}

		@Override
		public void removeAll()
		{
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					for(int i=0; i<slots.length; i++)
					{
						if(slots[i] != null)
						{
							InternalWidget iw = slots[i];
							slots[i] = null;
							BorderPanelImp.this.remove(iw.getJComponent());
						}
					}
					updateLayout();
				}
			});
		}

		@Override
		public JComponent getJComponent()
		{
			return BorderPanelImp.this;
		}

		@Override
		public int getPreferredWidth()
		{
			GridWidths gw = new GridWidths();
			return gw.getTotalWidth() + 2 * border;
		}

		@Override
		public int getPreferredHeight(int width)
		{
			if(width == 0) return 0;

			GridWidths gw = new GridWidths();
			gw.fitWidth(width - 2 * border);

			GridHeights gh = new GridHeights(gw);
			int height = gh.getTotalHeight() + 2 * border;
			return height;
		}

		@Override
		public void setSpacing(final int spacing)
		{
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					BorderPanelImp.this.spacing = spacing;
					updateLayout();
				}
			});
		}

		@Override
		public void setBorder(final int border)
		{
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					BorderPanelImp.this.border = border;
					updateLayout();
				}
			});
		}

		@Override
		public void addXMLChild(String slotName, Widget child)
		{
			int slot;
			if(slotName.equals("north"))
			{
				slot = NORTH;
			}
			else if(slotName.equals("northeast"))
			{
				slot = NORTHEAST;
			}
			else if(slotName.equals("east"))
			{
				slot = EAST;
			}
			else if(slotName.equals("southeast"))
			{
				slot = SOUTHEAST;
			}
			else if(slotName.equals("south"))
			{
				slot = SOUTH;
			}
			else if(slotName.equals("southwest"))
			{
				slot = SOUTHWEST;
			}
			else if(slotName.equals("west"))
			{
				slot = WEST;
			}
			else if(slotName.equals("northwest"))
			{
				slot = NORTHWEST;
			}
			else if(slotName.equals("central"))
			{
				slot = CENTRAL;
			}
			else
			{
				throw new BugException(
					"Slot name invalid, expecting 'north', 'northeast', etc.: " + slotName);
			}

			set(slot, child);
		}

		@Override
		public void redoLayout()
		{
			updateLayout();
			super.redoLayout();
		}
	};
}
