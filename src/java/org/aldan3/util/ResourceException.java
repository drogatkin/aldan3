/* aldan3 - ResourceException.java
 * Copyright (C) 1999-2009 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: ResourceException.java,v 1.2 2009/08/04 22:35:38 dmitriy Exp $                
 *  Created on Jun 23, 2009
 *  @author Dmitriy
 */
package  org.aldan3.util;
/** Resource manipulation exception. It's using by a resource manager.
 * 
 * @author dmitriy
 *
 */
public class ResourceException extends Exception {
	/** resource not found
	 * 
	 */
	static public final int NOT_FOUND = 1;
	/** IO happened at reaching a resource
	 * 
	 */
	static public final int IO = 2;
	
	protected int type;
	
	/** Parameterless constructor
	 * 
	 *
	 */
	public ResourceException() {
	}
	
	/** Constructor with type of problem
	 * 
	 * @param _type NOT_FOUND, or IO
	 */
	public ResourceException(int _type) {
		type = _type;
	}

	/** Constructor with message
	 * 
	 * @param _message 
	 */
	public ResourceException(String _message) {
		super(_message);
	}		

	/** Constructor with message and type
	 * 
	 * @param _message
	 * @param _type
	 */
	public ResourceException(String _message, int _type) {
		super(_message);
		type = _type;
	}
	
	/** Returns type of exception
	 * 
	 * @return type
	 */
	public int getType() {
		return type;
	}

	/** Constructor with a message and cause exception
	 * 
	 * @param _message
	 * @param t cause
	 */
	public ResourceException(String _message, Throwable t) {
		super(_message);
		initCause(t);
	}		
}	
