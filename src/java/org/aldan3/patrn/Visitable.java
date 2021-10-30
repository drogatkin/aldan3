/* aldan3 - Visitable.java
 * Copyright (C) 1999-2009 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: Visitable.java,v 1.2 2009/08/04 22:35:39 dmitriy Exp $                
 *  Created on Jun 23, 2009
 *  @author Dmitriy
 */
package org.aldan3.patrn;

/**
 * This interface used for implementation Visitor design pattern
 * @author Dmitriy
 *
 * @param <T>
 */
public interface Visitable<T> {
	void accept(Visitor<T> visitor);
}
