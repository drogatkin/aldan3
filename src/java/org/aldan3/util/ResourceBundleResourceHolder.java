/* aldan3 - ResourceBundleResourceHolder.java
 * Copyright (C) 1999-2009 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: ResourceBundleResourceHolder.java,v 1.2 2009/08/04 22:35:38 dmitriy Exp $                
 *  Created on Jun 23, 2009
 *  @author Dmitriy
 */
package  org.aldan3.util;
import java.util.PropertyResourceBundle;
import java.io.InputStream;
import java.io.IOException;

/** Resource bundle holder
 * 
 * @author dmitriy
 *
 */
public class ResourceBundleResourceHolder extends ResourceHolder {
	protected Object getResourceFromStream(InputStream is, String _encoding) throws IOException {
		return (Object) new PropertyResourceBundle(is);
	}
}