/* aldan3 - TreeViewHelper.java
 * Copyright (C) 1999-2007 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: TreeViewHelper.java,v 1.2 2007/04/17 07:57:22 rogatkin Exp $                
 *  Created on Feb 8, 2007
 *  @author dmitriy
 */
package org.aldan3.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aldan3.model.TreeModel;

/**
 * This class helps to build tree controls in pure HTML. The class generates 
 * <class>Map</class>s sequence for the following corresponding template fragment:
 * <pre>
 *   &lt;head&gt;
 *    ...
 *      &lt;STYLE TYPE='text/css'&gt; a:hover {color: rgb(255,33,33);text-decoration:
 *        underline;} td {font-size: 10px; font-family: Verdana,Arial; }
 *      &lt;/style&gt;
 *       ...
 *   &lt;/head&gt;
 *    ...
 * </pre>
 * <p>More in <b>body</b>
 * <pre>
 *   &#064;treeview[treeId]( &lt;table cellspacing=0 cellpadding=0 border=0&gt;
 *              &lt;tr&gt;
 *     &#064;treeline(
 *         &#064;rootnode( &lt;td> &lt;a
 *                    href='@page@?nodeid@reeId@=@:nodeid@@state(&state@treeid@=@:state@)@[&tree=@treeId@]&@parameters@'>
 *                    &lt;img src='@treeimages@/@mark@.gif' border=0 width=16
 *                    height=16>&lt;/a>&lt;/td>
 *                    &lt;td> <img src='@treeimages@/space.gif' border=0 width=3
 *                    height=10>&lt;/td>
 *                    &lt;td> &lt;a href='@.link@'> &lt;img src='@treeimages@/space.gif'
 *                    border=0 width=3 height=10>@name@</a> <img
 *                    src='@treeimages@/space.gif' border=0 width=3 height=10>&lt;/td>
 *         )@
 *         &#064;opennode( &lt;td> &lt;a
 *                    href='@page@?nodeid@treeId@=@:nodeid@@state(&state@treeId@=@:state@)@[&tree=@treeId@]&@parameters@'>
 *                    &lt;img src='@treeimages@/minus@vpos@.gif' border=0 width=16
 *                    height=16>&lt;/a>&lt;/td>
 *                    &lt;!-- _first, _notlast, _last -->
 *                    &lt;td> &lt;a
 *                    href='@page@?nodeid@treeId@=@:nodeid@@state(&state@treeId@=@:state@)@[&tree=@treeId@&@parameters@'>
 *                    &lt;img src='@treeimages@/folder_o.gif' border=0 width=16
 *                    height=16>&lt;/a>&lt;/td>
 *                    &lt;td> &lt;img src='@treeimages@/space.gif' border=0 width=3
 *                    height=10>&lt;/td>
 *                    &lt;td> &lt;a href='@.link@'> &lt;img src='@treeimages@/space.gif'
 *                    border=0 width=3 height=10>@name@&lt;/a> <img
 *                    src='@treeimages@/space.gif' border=0 width=3 height=10>&lt;/td>
 *         )@
 *         &#064;shiftlevel(
 *                    &lt;td>&lt;img src='@treeimages@/@vpos@.gif' border=0 width=16
 *                    height=16>&lt;/td>
 *         )@
 *         &#064;element(
 *                    &lt;td><img src='@treeimages@/line@vpos@.gif' border=0
 *                    width=16 height=16>&lt;/td>
 *                    &lt;!-- _first, _notlast, _last -->
 *                    &lt;td>&lt;a
 *                    href='@page@?nodeid@treeId@=@:nodeid@@state(&state@treeId@=@:state@)@&tree=@treeId@&@parameters@'>
 *                    &lt;img src='@treeimages@/site@spec@@mark@.gif' border=0
 *                    width=16 height=16>&lt;/a>&lt;/td>
 *                    &lt;td><img src='@treeimages@/space.gif' border=0 width=3
 *                    height=10>&lt;/td>
 *                    &lt;td> <!--1--> &lt;a
 *                    href='@page@?nodeid@treeId@=@:nodeid@@state(&state@treeId@=@:state@)@&tree=@treeId@&@parameters@'>
 *                    &lt;!-- or 2-->&lt;a
 *                    href='@.link@&nodeid@treeId@=@:nodeid@@state(&state@treeId@=@:state@)@&tree=@treeId@&@parameters@'>
 *                    &lt;img src='@treeimages@/space.gif' border=0 width=3
 *                    height=10>@name@&lt;/a> &lt;img src='@treeimages@/space.gif'
 *                    border=0 width=3 height=10>&lt;/td>
 *         )@
 *         &#064;closenode(
 *                    &lt;td> &lt;a
 *                    href='@page@?nodeid@treeId@=@:nodeid@@state(&state@treeId@=@:state@)@&tree=@treeId@&@parameters@'>
 *                    &lt;img src='@treeimages@/plus@vpos@.gif' border=0 width=16
 *                    height=16>&lt;/a>&lt;/td>
 *                    &lt;!-- _first, _notlast, _last -->
 *                    &lt;td> &lt;a
 *                    href='@page@?nodeid@treeId@=@:nodeid@@state(&state@treeId@=@:state@)@&tree=@treeId@&@parameters@'>
 *                    &lt;img src='@treeimages@/folder.gif' border=0 width=16
 *                    height=16>&lt;/a>&lt;/td>
 *                    &lt;td> &lt;img src='@treeimages@/space.gif' border=0 width=3
 *                    height=10></td>
 *                    &lt;td> &lt;a href='@.link@'> <img src='@treeimages@/space.gif'
 *                    border=0 width=3 height=10>@name@&lt;/a> &lt;img
 *                    src='@treeimages@/space.gif' border=0 width=3 height=10>&lt;/td>
 *         )@ 
 *      )@
 *                    &lt;/tr>
 *               &lt;/table> 
 *   )@
 *   </pre>
 */

public class TreeViewHelper implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7561659510085645195L;
	
	public static final String HV_NODEID = "nodeid";

	public static final String HV_IMAGEVPOS = "vpos";

	public static final String HV_TREEIMAGES = "treeimages";

	public static final String HV_NAME = "name";

	public static final String HV_LINK = "link";

	public static final String HV_PAGE = "page";

	public static final String HV_PARAMS = "parameters";

	public static final String HV_STATE = "state";

	public static final String HV_TREEID = "treeId";

	public static final String HV_MARK = "mark";

	public static final String HV_SPECIALITY = "spec";

	public static final String HV_TOOLTIP = "tooltip";

	public static final String HVS_ROOT = "rootnode";

	public static final String HVS_TREE = "treeview";

	public static final String HVS_ONODE = "opennode";

	public static final String HVS_SHIFT = "shiftlevel";

	public static final String HVS_ELEM = "element";

	public static final String HVS_CNODE = "closenode";

	public static final String HVS_TLINE = "treeline";

	// nodeid=@nodeid@treeId@@&tree=@treeId@@state(&state@treeId@=@:state@)@&@parameters@

	public static final char ST_NODE_CLOSED = '0';

	public static final char ST_NODE_OPEN = '1';

	public static final char POS_FIRST = '0';

	public static final char POS_NOTLAST = '1';

	public static final char POS_LAST = '2';

	public static final String POS_NAME_FIRST = "_first";

	public static final String POS_NAME_NOTLAST = "_notlast";

	public static final String POS_NAME_LAST = "_last";

	public static final String MARK = "_mark";

	public static final String[] POS_NAMES = { POS_NAME_FIRST, POS_NAME_NOTLAST, POS_NAME_LAST };

	static final int CAPACITY = 20;

	private static final HashMap LINE_FILLER = new HashMap();

	private static final HashMap SPACE_FILLER = new HashMap();

	private static final HashMap FIRST_FILLER = new HashMap();

	private static final HashMap LAST_FILLER = new HashMap();

	private static final HashMap FILLER = new HashMap();
	
	static {
		FIRST_FILLER.put(HV_IMAGEVPOS, POS_NAME_FIRST);
		FILLER.put(HV_IMAGEVPOS, POS_NAME_NOTLAST);
		LAST_FILLER.put(HV_IMAGEVPOS, POS_NAME_LAST);
		LINE_FILLER.put(HV_IMAGEVPOS, "line");
		SPACE_FILLER.put(HV_IMAGEVPOS, "space");
	}

	public static class TreeState {
		protected String id;
		protected String[] nodeStates;
		protected String selection;
		
		public TreeState(String id, String[] nodeStates, String selection) {
			this.id = id;
			this.nodeStates = nodeStates;
			this.selection = selection;
		}
	}
	
	protected String treeId;

	// PROBLEM: where to keep state of a tree? in URL, or in internal storage
	// accessible by session and tree id, or somewhere else?
	public TreeViewHelper(String _treeId) {
		if (_treeId == null)
			treeId = "";
		else
			treeId = _treeId;
	}

	public String getId() {
		return treeId;
	}

	public void apply(Map _result, TreeModel _provider, TreeState ts,  String _appendparam, TreeViewHelper[] _others) {
			apply(_result, _provider, ts, null, _appendparam, _others);
	}
	
	/**
	 * 
	 * @param _result map to insert tree map into
	 * @param _provider Traversable provider for a particular tree
	 * @param _bfp
	 * @param _markObjectId object id if should be marked not in HV_NODEID  
	 * @param _appendparam extra parameters to urls
	 * @param _others if more than one tree on page
	 */
	public void apply(Map _result, TreeModel _provider, TreeState ts, String _markObjectId, String _appendparam,
			TreeViewHelper[] _others) {
		// null parameters except _append and _other are not allowed here
		ArrayList levels = new ArrayList(CAPACITY);
		ArrayList newState = new ArrayList(CAPACITY);
		String ltid = ts.id; //pageService.getStringParameterValue(HV_TREEID, "", 0);
		if (ltid.equals(treeId))
			ltid = "";
		String[] prevState = ts.nodeStates; //pageService.getStringParameterValues(HV_STATE + ltid);
		String aid = _markObjectId==null?ts.selection/*pageService.getStringParameterValue(HV_NODEID + ltid, (String) null, 0)*/:_markObjectId;
		applyLevel(levels, newState, prevState, _provider, null, "" + POS_LAST, aid, _appendparam);
		_result.put(HVS_TREE + treeId, levels);
		_result.put(HV_STATE + treeId, newState); // populate also in entire
													// environment
	}

	protected void applyLevel(ArrayList _levels, ArrayList _newState, String[] _prevState, TreeModel _provider,
			Object _parent, String _poss, /*
											 * it can be two parameters,
											 * previous and current
											 */
			String _aid, String _appendparam) {
		ArrayList level = new ArrayList(4); // treeline
		List children = _provider.getChildren(_parent);
		if (children == null)
			return; // way to hide node?
		HashMap line = new HashMap(6);
		int plen = _poss.length();
		ArrayList tline = new ArrayList(plen);
		for (int i = 0; i < plen - 1; i++)
			tline.add(_poss.charAt(i) == POS_LAST ? SPACE_FILLER : LINE_FILLER);

		line.put(HVS_SHIFT, tline);
		level.add(line);
		int cl = children.size();
		char state = ST_NODE_CLOSED;
		HashMap elnode = new HashMap(1);
		char pos = _poss.charAt(plen - 1);
		if (cl == 0) { // element
			if (pos == POS_FIRST && _levels.size() > 1)
				pos = POS_NOTLAST;
		} else { // node
			if (plen <= 1)
				state = ST_NODE_OPEN;
			else {
				if (_prevState != null)
					state = isNodeStated(_prevState, _provider, _parent) ? ST_NODE_OPEN : ST_NODE_CLOSED;
				if (_aid != null && _provider.isId(_parent, _aid))
					if (state == ST_NODE_CLOSED)
						state = ST_NODE_OPEN;
					else
						state = ST_NODE_CLOSED;
			}
		}
		elnode.put(HV_NAME, _provider.getLabel(_parent));
		elnode.put(HV_LINK, _provider.getAssociatedReference(_parent));
		elnode.put(HV_PAGE, _provider.getSwitchReference(_parent));
		elnode.put(HV_NODEID, _provider.getId(_parent));
		if (state == ST_NODE_OPEN && _parent != null)
			setNodeStated(elnode, _newState, (String) elnode.get(HV_NODEID), _provider, _parent);
		elnode.put(HV_STATE, _newState);
		elnode.put(HV_PARAMS, _appendparam);
		elnode.put(HV_IMAGEVPOS, POS_NAMES[pos - '0']);
		elnode.put(HV_SPECIALITY, _provider.getImageModifier(_parent));
		elnode.put(HV_TREEID, ""+getId());
		elnode.put(HV_TOOLTIP, _provider.getToolTip(_parent));
		line = new HashMap(1);
		if (_parent == null)
			line.put(HVS_ROOT, elnode);
		else if (cl == 0) // element
			line.put(HVS_ELEM, elnode);
		else
			line.put(state == ST_NODE_CLOSED ? HVS_CNODE : HVS_ONODE, elnode);
		level.add(line);
		line = new HashMap(2);
		// marking possible for node and element
		// Should we unmark on next click?
		if (_aid != null && _provider.isId(_parent, _aid) && _provider.canMark(_parent, state == ST_NODE_OPEN)) {
			elnode.put(HV_MARK, MARK);
			line.put(HV_MARK, MARK);
		}
		line.put(HVS_TLINE, level);
		// log(CLASS_ID+ ": Added level "+level +" to hashmap "+HVS_TLINE);
		_levels.add(line);
		for (int i = 0; state == ST_NODE_OPEN && i < cl; i++)
			applyLevel(_levels, _newState, _prevState, _provider, children.get(i), _poss
					+ (i == cl - 1 ? POS_LAST : (i == 0 ? POS_FIRST : POS_NOTLAST)), _aid, _appendparam);
	}

	/**
	 * Gives possibility to override and save new state in external storage
	 */
	protected void setNodeStated(HashMap _modeMap, ArrayList _newState, String _nid, TreeModel _provider, Object _node) {
		// Note: _newState stored under 'state' name in a caller
		// so 'state' can't be used because will be overriden
		_modeMap.put(":" + HV_STATE, _nid);
		_newState.add(_modeMap);
	}

	// ! poor performance
	// ! as a solution, can be removing node id from state
	// and cache it in hash map in case if the node exists more then ones
	// ! consider copying the rest to new state to at reopening all nodes
	// keep their state

	protected boolean isNodeStated(String[] _prevState, TreeModel _provider, Object _node) {
		// TODO consider to traverse tree down to find any lower node open, so open this too
		// it should reduce number of opened state just to end visibe nodes
		for (int i = 0; i < _prevState.length; i++)
			if (_provider.isId(_node, _prevState[i]))
				return true;
		return false;
	}

}
