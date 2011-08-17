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

import javax.swing.JComponent;

import com.leafdigital.ui.api.*;

import leafchat.core.api.BugException;

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

	/** Local equivalents of grid constants, just to make code shorter */
	private final static int
		gN=BorderPanel.NORTH,
		gNE=BorderPanel.NORTHEAST,
		gE=BorderPanel.EAST,
		gSE=BorderPanel.SOUTHEAST,
		gS=BorderPanel.SOUTH,
		gSW=BorderPanel.SOUTHWEST,
		gW=BorderPanel.WEST,
		gNW=BorderPanel.NORTHWEST,
		gC=BorderPanel.CENTRAL;

	/** Number of slots in BorderPanel */
	private final static int SLOTS=9;

	/** One of the CORNERS_xxx constants */
	private int iCornerHandling;

	/** Spacing between grid squares */
	private int iSpacing=0;

	/** Border at edge of grid */
	private int iBorder=0;

	/** Keep record of held components */
	private InternalWidget[] aiw=new InternalWidget[SLOTS];

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
		InternalWidget iw=(InternalWidget)bpInterface;
	  int width=iw.getPreferredWidth();
	  return new Dimension(width,iw.getPreferredHeight(width));
	}

	/**
	 * @param iSlot Slot (gN, gNE, etc)
	 * @return Preferred width of component at that slot, or 0 if none
	 */
	private int prefW(int iSlot)
	{
		if(aiw[iSlot]==null || !aiw[iSlot].isVisible()) return 0;
		return aiw[iSlot].getPreferredWidth();
	}

	/**
	 * @param iSlot Slot (gN, gNE, etc)
	 * @param iWidth Available width
	 * @return Preferred height of slot's component at that width, or 0
	 */
	private int prefH(int iSlot,int iWidth)
	{
		if(aiw[iSlot]==null || !aiw[iSlot].isVisible() || iWidth==0) return 0;
		return aiw[iSlot].getPreferredHeight(iWidth);
	}

	/**
	 * @param iSlot Slot (gN, gNE, etc)
	 * @return True if the slot is empty
	 */
	private boolean isEmpty(int iSlot)
	{
		return aiw[iSlot]==null;
	}

	/**
	 * Move a component.
	 * @param iSlot Slot of component to move
	 * @param iX X position
	 * @param iY Y position
	 * @param iWidth Width
	 * @param iHeight Height
	 */
	private void move(int iSlot,int iX,int iY,int iWidth,int iHeight)
	{
		if(aiw[iSlot]==null) return;
		aiw[iSlot].getJComponent().setBounds(iX,iY,iWidth,iHeight);
	}

	/** Move all components to their correct place in new layout */
	private void updateLayout()
	{
		UISingleton.checkSwing();

		// Get desired width and height, and modify to fit actual space available
		GridWidths gw=new GridWidths();
		gw.fitWidth(getWidth()-iBorder*2);
		GridHeights gh=new GridHeights(gw);
		gh.fitHeight(getHeight()-iBorder*2);

		// Calculate positions
		int
			iX1=iBorder,
			iX2=iBorder+gw.iL+gw.iGutterLC,
			iX3=iBorder+gw.iL+gw.iGutterLC+gw.iC+gw.iGutterCR;
		int
		  iY1=iBorder,
		  iY2=iBorder+gh.iT+gh.iGutterTM,
		  iY3=iBorder+gh.iT+gh.iGutterTM+gh.iM+gh.iGutterMB;

		// Corner components always go in the same places
		move(gNW,iX1,iY1,gw.iL,gh.iT);
		move(gNE,iX3,iY1,gw.iR,gh.iT);
		move(gSW,iX1,iY3,gw.iL,gh.iB);
		move(gSE,iX3,iY3,gw.iR,gh.iB);

		switch(iCornerHandling)
		{
			case BorderPanel.CORNERS_LEAVEBLANK :
			{
				// Put all components in their grid squares
				move(gN,iX2,iY1,gw.iC,gh.iT);
				move(gE,iX3,iY2,gw.iR,gh.iM);
				move(gS,iX2,iY3,gw.iC,gh.iB);
				move(gW,iX1,iY2,gw.iL,gh.iM);
				move(gC,iX2,iY2,gw.iC,gh.iM);
				break;
			}
			case BorderPanel.CORNERS_HORIZONTALFILL:
			{
				// E and W don't take up other slots
				move(gE,iX3,iY2,gw.iR,gh.iM);
				move(gW,iX1,iY2,gw.iL,gh.iM);

				int iNStart=iX2,iNWidth=gw.iC;
				if(isEmpty(gNW))
				{
					iNStart=iX1;
					iNWidth+=gw.iL+gw.iGutterLC;
				}
				if(isEmpty(gNE))
				{
					iNWidth+=gw.iGutterCR+gw.iR;
				}
				move(gN,iNStart,iY1,iNWidth,gh.iT);

				int iSStart=iX2,iSWidth=gw.iC;
				if(isEmpty(gSW))
				{
					iSStart=iX1;
					iSWidth+=gw.iL+gw.iGutterLC;
				}
				if(isEmpty(gSE))
				{
					iSWidth+=gw.iGutterCR+gw.iR;
				}
				move(gS,iSStart,iY3,iSWidth,gh.iB);

				int iCStart=iX2,iCWidth=gw.iC;
				if(isEmpty(gW))
				{
					iCStart=iX1;
					iCWidth+=gw.iL+gw.iGutterLC;
				}
				if(isEmpty(gE))
				{
					iCWidth+=gw.iGutterCR+gw.iR;
				}
				move(gC,iCStart,iY2,iCWidth,gh.iM);

				break;
			}
			case BorderPanel.CORNERS_VERTICALFILL:
			{
				// N and S don't take up other slots
				move(gN,iX2,iY1,gw.iC,gh.iT);
				move(gS,iX2,iY3,gw.iC,gh.iB);

				int iWStart=iY2,iWHeight=gh.iM;
				if(isEmpty(gNW))
				{
					iWStart=iY1;
					iWHeight+=gh.iT+gh.iGutterTM;
				}
				if(isEmpty(gSW))
				{
					iWHeight+=gh.iGutterMB+gh.iB;
				}
				move(gW,iX1,iWStart,gw.iL,iWHeight);

				int iEStart=iY2,iEHeight=gh.iM;
				if(isEmpty(gNE))
				{
					iEStart=iY1;
					iEHeight+=gh.iT+gh.iGutterTM;
				}
				if(isEmpty(gSE))
				{
					iEHeight+=gh.iGutterMB+gh.iB;
				}
				move(gE,iX3,iEStart,gw.iR,iEHeight);

				int iCStart=iY2,iCHeight=gh.iM;
				if(isEmpty(gN))
				{
					iCStart=iY1;
					iCHeight+=gh.iT+gh.iGutterTM;
				}
				if(isEmpty(gS))
				{
					iCHeight+=gh.iGutterMB+gh.iB;
				}
				move(gC,iX2,iCStart,gw.iC,iCHeight);

				break;
			}
		}

		repaint();
	}

	/** @return Interface giving limited public access */
	BorderPanel getInterface()
	{
		return bpInterface;
	}

	/**
	 * @param i1 First number
	 * @param i2 Second number
	 * @param i3 Third number
	 * @return Maximum of the three parameters
	 */
	private static int max(int i1,int i2,int i3)
	{
		if(i1 > i2)
			return (i1 > i3) ? i1 : i3;
		else
			return (i2 > i3) ? i2 : i3;
	}

	/** Class manages the three widths in the grid */
	class GridWidths
	{
		int iL,iC,iR;
		int iGutterLC,iGutterCR;

		/** @return Total width this represents */
		int getTotalWidth() { return iL+iGutterLC+iC+iGutterCR+iR; }

		/** Construct with desired widths */
		GridWidths()
		{
			// Calculate L and R; these depend straightforwardly on the things in
			// those corners
			iL=max(prefW(gNW),prefW(gW),prefW(gSW));
			iR=max(prefW(gNE),prefW(gE),prefW(gSE));

			// Calculate gutters, depending on whether there's anything in the places
			iGutterLC=(iL==0) ? 0 : iSpacing;
			iGutterCR=(iR==0) ? 0 : iSpacing;

			// Calculate C, which depends on whether the N component expands or not
			if(iCornerHandling==BorderPanel.CORNERS_HORIZONTALFILL)
			{
				int
					iTPref=prefW(gN)
						- (isEmpty(gNW) ? (iL+iGutterLC) : 0)
						- (isEmpty(gNE) ? (iR+iGutterCR) : 0),
					iMPref=prefW(gC)
						- (isEmpty(gW) ? (iL+iGutterLC) : 0)
						- (isEmpty(gE) ? (iR+iGutterCR) : 0),
					iBPref=prefW(gS)
						- (isEmpty(gSW) ? (iL+iGutterLC) : 0)
						- (isEmpty(gSE) ? (iR+iGutterCR) : 0);

				iC=max(iTPref,iMPref,iBPref);
			}
			else // CORNERS_LEAVEBLANK or .CORNERS_VERTICALFILL
			{
				iC=max(prefW(gN),prefW(gC),prefW(gS));
			}
		}

		/**
		 * Fit to a different available width.
		 * @param iAvailableWidth Required width
		 */
		void fitWidth(int iAvailableWidth)
		{
			int iExtraSpace=iAvailableWidth - getTotalWidth();
			if(iExtraSpace>0)
			{
				iC+=iExtraSpace;
			}
			else if(iExtraSpace<0)
			{
				iC+=iExtraSpace;
				if(iC<0)
				{
					int iOverflow=-iC;
					iC=0;
					iL-=iOverflow/2;
					iR-=(iOverflow+1)/2; // The +1 ensures that we use the complete total
					if(iL<0)
					{
						int iOverflowOverflow=-iL;
						iL=0;

						iR-=iOverflowOverflow;
						if(iR<0) iR=0;
					}
					else if(iR<0)
					{
						int iOverflowOverflow=-iR;
						iR=0;

						iL-=iOverflowOverflow;
						if(iL<0) iL=0;
					}
				}
			}
		}
	}

	/** Manages the three heights in the grid */
	class GridHeights
	{
		int iT,iM,iB;
		int iGutterTM,iGutterMB;

		int getTotalHeight() { return iT+iGutterTM+iM+iGutterMB+iB; }

		/**
		 * Find the desired grid heights for given widths.
		 * @param gw Widths data
		 */
		GridHeights(GridWidths gw)
		{
			if(iCornerHandling==BorderPanel.CORNERS_HORIZONTALFILL)
			{
				iT=max(
					prefH(gNW,gw.iL),
					prefH(gNE,gw.iR),
					prefH(gN,gw.iC
						+(isEmpty(gNW) ? (gw.iL+gw.iGutterLC) : 0)
						+(isEmpty(gNE) ? (gw.iR+gw.iGutterCR) : 0)
						)
					);
				iB=max(
					prefH(gSW,gw.iL),
					prefH(gSE,gw.iR),
					prefH(gS,gw.iC
						+(isEmpty(gSW) ? (gw.iL+gw.iGutterLC) : 0)
						+(isEmpty(gSE) ? (gw.iR+gw.iGutterCR) : 0)
						)
					);
			}
			else // CORNERS_LEAVEBLANK or CORNERS_VERTICALFILL
			{
				iT=max(prefH(gNW,gw.iL),prefH(gN,gw.iC),prefH(gNE,gw.iR));
				iB=max(prefH(gSW,gw.iL),prefH(gS,gw.iC),prefH(gSE,gw.iR));
			}

			// Calculate gutters, depending on whether there's anything in the places
			iGutterTM=(iT>0) ? iSpacing : 0;
			iGutterMB=(iB>0) ? iSpacing : 0;

			if(iCornerHandling==BorderPanel.CORNERS_VERTICALFILL)
			{
				int
					iLPref=prefH(gW,gw.iL)
						- (isEmpty(gNW) ? (iT+iGutterTM) : 0)
						- (isEmpty(gSW) ? (iB+iGutterMB) : 0),
					iCPref=prefH(gC,gw.iC)
						- (isEmpty(gN) ? (iT+iGutterTM) : 0)
						- (isEmpty(gS) ? (iB+iGutterMB) : 0),
					iRPref=prefH(gE,gw.iR)
						- (isEmpty(gNE) ? (iT+iGutterTM) : 0)
						- (isEmpty(gSE) ? (iB+iGutterMB) : 0);

				iM=max(iLPref,iCPref,iRPref);
			}
			else // CORNERS_LEAVEBLANK or CORNERS_HORIZONTALFILL
			{
				iM=max(prefH(gW,gw.iL),prefH(gC,gw.iC),prefH(gE,gw.iR));
			}
		}

		/**
		 * Fit to specified height.
		 * @param iAvailableHeight Height to be used
		 */
		void fitHeight(int iAvailableHeight)
		{
			int iExtraSpace=iAvailableHeight - getTotalHeight();
			if(iExtraSpace>0)
			{
				iM+=iExtraSpace;
			}
			else if(iExtraSpace<0)
			{
				iM+=iExtraSpace;
				if(iM<0)
				{
					int iOverflow=-iM;
					iM=0;
					iT-=iOverflow/2;
					iT-=(iOverflow+1)/2; // The +1 ensures that we use the complete total
					if(iT<0)
					{
						int iOverflowOverflow=-iT;
						iT=0;

						iB-=iOverflowOverflow;
						if(iB<0) iB=0;
					}
					else if(iB<0)
					{
						int iOverflowOverflow=-iB;
						iB=0;

						iT-=iOverflowOverflow;
						if(iT<0) iT=0;
					}
				}
			}
		}
	}

	/** Interface available to public */
	private BorderPanel bpInterface=new BorderPanelInterface();

	/** Interface available to public */
	class BorderPanelInterface extends BasicWidget implements BorderPanel,InternalWidget
	{
		@Override
		public int getContentType() { return CONTENT_NAMEDSLOTS; }

		@Override
		public void set(final int iSlot,Widget w)
		{
			final InternalWidget iw=(InternalWidget)w;
			iw.setParent(this);
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					if(aiw[iSlot]!=null)
					{
						BorderPanelImp.this.remove(aiw[iSlot].getJComponent());
						aiw[iSlot]=null;
					}
					if(iw!=null)
					{
						add(iw.getJComponent());
						iw.getJComponent().revalidate();
						aiw[iSlot]=iw;
					}
					updateLayout();
				}
			});
		}

		@Override
		public Widget get(int slot)
		{
			return aiw[slot];
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
							iCornerHandling=iCorners;
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
			final InternalWidget iw=(InternalWidget)w;
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					for(int i=0;i<aiw.length;i++)
					{
						if(aiw[i]==iw)
						{
							aiw[i]=null;
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
					for(int i=0;i<aiw.length;i++)
					{
						if(aiw[i]!=null)
						{
							InternalWidget iw=aiw[i];
							aiw[i]=null;
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
			GridWidths gw=new GridWidths();
			return gw.getTotalWidth()+2*iBorder;
		}

		@Override
		public int getPreferredHeight(int iWidth)
		{
			if(iWidth==0) return 0;

			GridWidths gw=new GridWidths();
			gw.fitWidth(iWidth-2*iBorder);

			GridHeights gh=new GridHeights(gw);
			int iHeight=gh.getTotalHeight()+2*iBorder;
			return iHeight;
		}

		@Override
		public void setSpacing(final int iSpacing)
		{
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					BorderPanelImp.this.iSpacing=iSpacing;
					updateLayout();
				}
			});
		}

		@Override
		public void setBorder(final int iBorder)
		{
			UISingleton.runInSwing(new Runnable()
			{
				@Override
				public void run()
				{
					BorderPanelImp.this.iBorder=iBorder;
					updateLayout();
				}
			});
		}

		@Override
		public void addXMLChild(String sSlotName, Widget wChild)
		{
			int iSlot;
			if(sSlotName.equals("north")) iSlot=NORTH;
			else if(sSlotName.equals("northeast")) iSlot=NORTHEAST;
			else if(sSlotName.equals("east")) iSlot=EAST;
			else if(sSlotName.equals("southeast")) iSlot=SOUTHEAST;
			else if(sSlotName.equals("south")) iSlot=SOUTH;
			else if(sSlotName.equals("southwest")) iSlot=SOUTHWEST;
			else if(sSlotName.equals("west")) iSlot=WEST;
			else if(sSlotName.equals("northwest")) iSlot=NORTHWEST;
			else if(sSlotName.equals("central")) iSlot=CENTRAL;
			else throw new BugException(
			  "Slot name invalid, expecting 'north', 'northeast', etc.: "+sSlotName);

			set(iSlot,wChild);
		}

		@Override
		public void redoLayout()
		{
			updateLayout();
			super.redoLayout();
		}
	};
}
