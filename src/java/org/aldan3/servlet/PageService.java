/* aldan3 - PageService.java
 * Copyright (C) 1999-2009 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: PageService.java,v 1.5 2015/02/26 21:08:07 cvs Exp $                
 *  Created on Jun 23, 2009
 *  @author Dmitriy
 */
package  org.aldan3.servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.aldan3.model.ServiceProvider;

public interface PageService extends ServiceProvider {
	/** Main entry point in service of page requests
	 * 
	 * @param req request
	 * @param resp response
	 * @param frotnController front controller servlet
	 * @throws java.io.IOException
	 * @throws ServletException
	 */
	public void serve(HttpServletRequest req, HttpServletResponse resp, FrontController frotnController)
		throws java.io.IOException, ServletException;
	
	/** tells to a container that it can be cached, used a shared copy, or instantiate each time
	 * @return true if an instance can be cached, but not shared 
	 */
	public boolean isThreadSafe();
	
	/** tells to a container that can be cached, used a shared copy, or instantiate each time
	 * 
	 * @return true if a copy can be shared
	 */ 
	public boolean isThreadFriendly();
	
	public void reset();
}