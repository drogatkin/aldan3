/* aldan3 - DBField.java
 * Copyright (C) 1999-2010 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: Behavior.java,v 1.2 2011/04/20 04:08:48 dmitriy Exp $                
 *  Created on Mar 1, 2010
 *  @author Dmitriy
 */
package org.aldan3.annot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.TYPE, ElementType.METHOD })
public @interface Behavior {
	//String[] templateExtension() default {};
	String templatePattern() default "";
}
