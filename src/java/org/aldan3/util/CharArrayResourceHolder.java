/* aldan3 - CharArrayResourceHolder.java
 * Copyright (C) 1999-2009 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: CharArrayResourceHolder.java,v 1.2 2009/08/04 22:35:38 dmitriy Exp $                
 *  Created on Jun 23, 2009
 *  @author Dmitriy
 */
package  org.aldan3.util;
import java.io.InputStream;
import java.io.IOException;

/** particular type resource holder
 * 
 * @author dmitriy
 *
 */
public class CharArrayResourceHolder extends StringResourceHolder {
	protected Object getResourceFromStream(InputStream is, String _encoding) throws IOException {
		return ((String)super.getResourceFromStream(is, _encoding)).toCharArray();
	}
}