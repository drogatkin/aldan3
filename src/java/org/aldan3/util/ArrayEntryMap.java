/* aldan3 - ArrayEntryMap.java
 * Copyright (C) 1999-2007 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: ArrayEntryMap.java,v 1.4 2012/11/10 04:10:43 cvs Exp $                
 *  Created on Feb 4, 2007
 *  @author Dmitriy
 */
package org.aldan3.util;

import java.util.HashMap;

public class ArrayEntryMap<K> extends HashMap<K, Object> {
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("ArrayEntryMap entries=\n");
		for (K k : keySet()) {
			result.append(k).append('=');
			Object v = get(k);
			if (v instanceof Object[]) {
				result.append('[');
				for (Object o : (Object[]) v)
					result.append(o).append(',');
				result.append(']');
			} else
				result.append(v);
			result.append(';');
		}
		return result.toString();
	}

	public Object[] put(K key, Object value) {
		Object result = get(key);
		Object[] na = null;
		if (result == null) {
			na = new Object[] { value };
		} else if (result instanceof Object[]) {
			na = new Object[((Object[]) result).length + 1];
			System.arraycopy((Object[]) result, 0, na, 0, ((Object[]) result).length);
			na[((Object[]) result).length] = value;
		} else {
			na = new Object[] { result, value };
		}
		return (Object[]) super.put(key, na);
	}
}
