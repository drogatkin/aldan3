/* aldan3 - Registry.java
 * Copyright (C) 1999-2007 Dmitriy Rogatkin.  All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  
 *  Visit http://aldan3.sourceforge.net to get the latest infromation
 *  about Rogatkin's products.                                                        
 *  $Id: Registry.java,v 1.7 2013/03/04 07:59:41 cvs Exp $                
 *  Created on Feb 6, 2007
 *  @author Dmitriy
 */
package org.aldan3.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.aldan3.model.ServiceProvider;

public class Registry {
	HashMap<String, ServiceProvider> services = new HashMap<String, ServiceProvider>();

	ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

	public void register(ServiceProvider sp) {
		try {
			rwl.writeLock().lock();
			if (services.get(sp.getPreferredServiceName()) == null)
				services.put(sp.getPreferredServiceName(), sp);
			//else
				//throw new RegistryException("Service is already registered");
		} finally {
			rwl.writeLock().unlock();
		}
	}

	public ServiceProvider unregister(ServiceProvider sp) {
		try {
			rwl.writeLock().lock();
			if (services.get(sp.getPreferredServiceName()) != null)
				return services.remove(sp.getPreferredServiceName());
		} finally {
			rwl.writeLock().unlock();
		}
		return null;
	}
	
	public ServiceProvider unregister(String serviceName) {
		try {
			rwl.writeLock().lock();
			if (services.get(serviceName) != null)
				return services.remove(serviceName);
		} finally {
			rwl.writeLock().unlock();
		}
		return null;
	}

	public ServiceProvider getService(String serviceName) {
		rwl.readLock().lock();
		try {
			return services.get(serviceName);
		} finally {
			rwl.readLock().unlock();
		}
	}

	public Iterator<ServiceProvider> iterator() {
		rwl.readLock().lock();
		Collection<ServiceProvider> services_clone = new ArrayList<ServiceProvider>(services.size());// LinkedList<>();
		try {
			for (ServiceProvider service : services.values())
				services_clone.add(service);
		} finally {
			rwl.readLock().unlock();
		}
		return services_clone.iterator();
	}
}
