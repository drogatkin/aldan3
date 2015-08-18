/* aldan3 - FixedGridLayout.java
 * Copyright (C) 1999-2007 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: FixedGridLayout.java,v 1.2 2007/07/27 02:53:28 rogatkin Exp $                
 *  Created on Feb 8, 2007
 *  @author Dmitriy
 */
package org.aldan3.app.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

public class FixedGridLayout implements LayoutManager {

	static final int DEF_H = 24; // should be in font size

	/**
	 * Coonstructs layout with predefined dimensions.
	 * 
	 * @param grwidth
	 *            number fo cols
	 * @param grheight
	 *            number of rows
	 * @param maxheight
	 *            max height of a row
	 * @param maxvertgap
	 *            max gap between rows
	 */
	public FixedGridLayout(int grwidth, int grheight, int maxheight, int maxvertgap) {
		this(grwidth, grheight, maxheight, maxvertgap, 4, 2, 0, 4);
	}

	/**
	 * Constructs a layout with specified dimension and grow parameters.
	 * 
	 * @param grwidth
	 *            number of horizontal cells
	 * @param grheight
	 *            number of vertical cells
	 * @param maxheight
	 *            max height of layout
	 * @param maxvertgap
	 *            max vertical gap between rows
	 * @param horizinset
	 *            horizontal size
	 * @param horizgap
	 *            horizontal gap between cells
	 * @param proportion
	 *            cell proportion / aspect ration number of heights in a width
	 */
	public FixedGridLayout(int grwidth, int grheight, int maxheight, int maxvertgap, int horizinset, int horizgap,
			int proportion) {
		this(grwidth, grheight, maxheight, maxvertgap, horizinset, horizgap, 0, proportion);
	}

	/**
	 * Constructs a layout with specified dimension and grow parameters.
	 * 
	 * @param grwidth
	 *            number of horizontal cells
	 * @param grheight
	 *            number of vertical cells
	 * @param maxheight
	 *            max height of layout
	 * @param maxvertgap
	 *            max vertical gap between rows
	 * @param horizinset
	 *            horizontal size
	 * @param horizgap
	 *            horizontal gap between cells
	 */
	public FixedGridLayout(int grwidth, int grheight, int maxheight, int maxvertgap, int horizinset, int horizgap) {
		this(grwidth, grheight, maxheight, maxvertgap, horizinset, horizgap, 0, 4);
	}

	/**
	 * Constructs a layout with specified dimension and grow parameters.
	 * 
	 * @param grwidth
	 *            number of horizontal cells
	 * @param grheight
	 *            number of rows
	 * @param maxheight
	 *            max height of layout
	 * @param maxvertgap
	 *            max vertical gap between rows
	 * @param horizinset
	 *            cell width
	 * @param horizgap
	 *            max horizontal gap
	 * @param vertgap
	 *            max vertical gap
	 * @param proportion
	 *            number of heights in width
	 */
	public FixedGridLayout(int grwidth, int grheight, int maxheight, int maxvertgap, int horizinset, int horizgap,
			int vertgap, int proportion) {
		layouters = new Hashtable();
		if (grwidth <= 0)
			grwidth = 1;
		widthingranul = grwidth;
		if (grheight <= 0)
			grheight = 1;
		heightingranul = grheight;
		if (maxheight < 0)
			maxheight = DEF_H;
		maxgranulvertsize = maxheight;
		if (maxvertgap <= 0)
			maxvertgap = 4;
		this.maxvertgap = maxvertgap;
		if (proportion < 1 || proportion > 8)
			proportion = 4;
		this.proportion = proportion;
		if (horizgap < 0 || horizgap > proportion * 4)
			this.horizgap = 0;
		else
			this.horizgap = horizgap;
		if (horizinset < 0 || horizinset > proportion * 4)
			this.horizinset = 0;
		else
			this.horizinset = horizinset;

		if (vertgap < 0 || vertgap > maxvertgap)
			this.vertgap = 0;
		else
			this.vertgap = vertgap;
	}

	/**
	 * Adds a component to layout
	 * 
	 * @param component
	 *            name, constraints, it build like a string with numbers comma separated,
	 *            <ol>
	 *            <li> x coordinate in cells
	 *            <li> y coordinate in rows
	 *            <li> width in cells, 0 means preferred component size used, default 0
	 *            <li> height in rows, 0 means preferred component size used, -1 for last component to get all space, default 0
	 *            <li> extra horizontal margin in pixels, 0 default
	 *            <li> extra vertical margin in pixels, 0 default
	 *            </ol>
	 *            Examples: "0,0", "1,2,4,1"
	 * @param component
	 *            added
	 */
	public void addLayoutComponent(String name, Component comp) {
		layouters.put(comp, name);
	}

	/**
	 * Removes a component from layout.
	 * 
	 * @param component
	 *            to be removed
	 */
	public void removeLayoutComponent(Component comp) {
		layouters.remove(comp);
	}

	/**
	 * Calculates preferred layout size
	 * 
	 * @param parent
	 *            container
	 * @return preferred dimension
	 */
	public Dimension preferredLayoutSize(Container parent) {
		return new Dimension(maxgranulvertsize * proportion * (widthingranul), maxgranulvertsize * (heightingranul));
	}

	/**
	 * Calculates minimal layout size
	 * 
	 * @param parent
	 *            container
	 * @return preferred dimension
	 */
	public Dimension minimumLayoutSize(Container parent) {
		return new Dimension(1, 1);
	}

	/**
	 * makes layout.
	 * 
	 * @param parent
	 *            container
	 */
	public void layoutContainer(Container parent) {
		Dimension d = parent.getSize();
		int horzgransize = (d.width - 2 * horizinset) / widthingranul;
		int vertgransize = d.height / heightingranul;
		if (vertgransize > (maxgranulvertsize + maxvertgap))
			vertgransize = maxgranulvertsize;
		int x, y, w, h, hg, vg;
		Dimension ps;
		Enumeration ce = layouters.keys();
		while (ce.hasMoreElements()) {
			Component comp = (Component) ce.nextElement();
			try {
				StringTokenizer st = new StringTokenizer((String) layouters.get(comp), ", ");
				if (st.hasMoreTokens()) {
					x = Integer.parseInt(st.nextToken());
					if (st.hasMoreTokens()) {
						y = Integer.parseInt(st.nextToken());
						if (st.hasMoreTokens()) {
							w = Integer.parseInt(st.nextToken());
							if (st.hasMoreTokens()) {
								h = Integer.parseInt(st.nextToken());
								if (st.hasMoreTokens()) {
									hg = Integer.parseInt(st.nextToken());
									if (st.hasMoreTokens())
										vg = Integer.parseInt(st.nextToken());
									else
										vg = vertgap;
								} else {
									vg = vertgap;
									hg = horizgap;
								}
							} else {
								h = 1;
								hg = horizgap;
								vg = vertgap;
							}
						} else {
							w = 1;
							h = 1;
							hg = horizgap;
							vg = vertgap;
						}
					} else {
						y = 0;
						w = 1;
						h = 1;
						hg = horizgap;
						vg = vertgap;
					}
				} else {
					y = 0;
					x = 0;
					w = 1;
					h = 1;
					hg = horizgap;
					vg = vertgap;
				}
				if (y < 0 || x < 0)
					throw new IllegalArgumentException("Negative coordinate constraints.");
				ps = comp.getPreferredSize();
				comp.setBounds(x * horzgransize + hg / 2 + horizinset, y * vertgransize + vg / 2, w > 0 ? w
						* horzgransize - (hg>0?hg:0) : (w == 0 ? ps.width : d.width - (x * horzgransize + horzgransize / 4)),
						(h == 0) ? ps.height : (h < 0 ? d.height - (y * vertgransize + vertgransize / 3) : (h
								* vertgransize - vg)));
			} catch (Exception e) {
				if (debug)
					e.printStackTrace();
			}
		}
	}

	/** increases layout height
	 * 
	 * @param hd steps to increase, can be negative
	 */
	public void inflateHeight(int hd) {
		if (hd > 0 || -hd < heightingranul)
			heightingranul += hd;
	}

	/** increases layout width
	 * 
	 * @param wd steps to increase, can be negative
	 */
	public void inflateWidth(int wd) {
		if (wd > 0 || -wd < widthingranul)
			widthingranul += wd;
	}

	/** Gives current layout height
	 * 
	 * @return height in cells
	 */
	public int getHeight() {
		return heightingranul;
	}

	/** Gives layout width
	 * 
	 * @return width in cells
	 */
	public int getWidth() {
		return widthingranul;
	}

	private Hashtable layouters;

	private int widthingranul;

	private int heightingranul;

	private int maxgranulvertsize;

	private int maxvertgap;

	private int horizinset, horizgap;

	private int proportion;

	protected int vertgap;

	private final static boolean debug = false;

}
