/* aldan3 - ResourceHolder.java
 * Copyright (C) 1999-2009 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: ResourceHolder.java,v 1.10 2010/09/11 04:33:51 dmitriy Exp $                
 *  Created on Jun 23, 2009
 *  @author Dmitriy
 */
package  org.aldan3.util;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;

import javax.servlet.ServletContext;

/** resource holder used by resource manager.
 * 
 * @author dmitriy
 *
 */
public abstract class ResourceHolder {
	protected Object data; // resource data
	
	protected int usageCount; // how many times resource was used
	
	protected long lastModified; // resource source last modified
	protected long lastAccessed; // resource last accessed
	
	protected URL address;
	protected String encoding;
	
	protected String name;
	
	public static ResourceHolder createResource(ResourceManager.ResourceType resourceType,
												ResourceManager.Strategy strategy,
												ResourceManager.LocalizedRequester lrequester,
												String resPath,
												File[] searchPaths) {
		ResourceHolder result = null;
		if (searchPaths == null)
			searchPaths = new File[]{null};
		InputStream is = null;
		try {
			Locale locale = lrequester==null?null:lrequester.getLocale();
			String encoding = lrequester==null?null:lrequester.getEncoding();
			String extension = resourceType.getExtension();
			File resFile = null;
			if (searchPaths.length == 0)
				ResourceManager.log("createResource(File) no paths specified");			
scanPaths: 
			for (int pi=0; pi<searchPaths.length; pi++) {
				if (strategy != null) {
					Iterator<String> fi = strategy.localizeName(ResourceManager.Strategy.PATH, locale, resPath, extension);
					while(fi.hasNext()) {
						if (searchPaths[pi] == null)
							resFile = new File((String)fi.next());
						else
							resFile = new File(searchPaths[pi], bracketPath((String)fi.next()));
						if (checkFile(resFile))
							break scanPaths;
					}
				} else {
					if (searchPaths[pi] == null)
						resFile = new File(resPath+extension);
					else
						resFile = new File(searchPaths[pi], resPath+extension);
					if (checkFile(resFile))
						break;
				}
				resFile = null;
			}
			if (resFile != null) {
				result = (ResourceHolder)Class.forName(resourceType.getResourceHolderClassName()).newInstance();
				result.lastModified = resFile.lastModified();
				result.updateData(result.getResourceFromStream(is = new FileInputStream(resFile), encoding));
				result.address = resFile.toURL();
				result.encoding = encoding;
			}
		} catch(Exception ex) {
			result = null;
			ResourceManager.log("createResource(File)="+resPath, ex);
		} finally {
			if (is != null)
				try {
					is.close();
				} catch(IOException ioe) {ResourceManager.log( "close()", ioe);}
		}
		return result;
	}
	
	public static ResourceHolder createResource(ResourceManager.ResourceType resourceType,
												ResourceManager.Strategy strategy,
												ResourceManager.LocalizedRequester lrequester,
												String resPath,
												ClassLoader classLoader,
												String searchPaths[]) {
		ResourceHolder result = null;
		InputStream is = null;
		try {
			Locale locale = lrequester==null?null:lrequester.getLocale();
			String encoding = lrequester==null?null:lrequester.getEncoding();
			String extension = resourceType.getExtension();
			URL resUrl = null;
			if (searchPaths == null)
				searchPaths = new String[]{""};
scanPackages:
			for (int pi=0; pi<searchPaths.length; pi++) {
				if (strategy != null) {
					Iterator fi = strategy.localizeName(ResourceManager.Strategy.PATH, locale, resPath, extension);
					while (fi.hasNext()) {
						resUrl = getResource(classLoader, searchPaths[pi]+fi.next());
						if (resUrl != null)
							break scanPackages;
					}
				} else {
					resUrl = getResource(classLoader, searchPaths[pi]+resPath+extension);
					if (resUrl != null)
						break;
				}
				resUrl = null;
			}
			if (resUrl != null) {
				result = (ResourceHolder)Class.forName(resourceType.getResourceHolderClassName()).newInstance();
				URLConnection uc = resUrl.openConnection();
				result.lastModified = uc.getLastModified();
				result.updateData(result.getResourceFromStream(is = uc.getInputStream(), encoding));
				result.address = resUrl;
				result.encoding = encoding;
			}
		} catch(Exception ex) {
			result = null;
			ResourceManager.log ("createResource(ClassLoader)="+resPath, ex);
		} finally {
			if (is != null)
				try {
					is.close();
				} catch(IOException ioe) {ResourceManager.log( "close()", ioe);}
		}
		return result;
	}
	
	public static ResourceHolder createResource(ResourceManager.ResourceType resourceType,
												ResourceManager.Strategy strategy,
												ResourceManager.LocalizedRequester lrequester,
												String resPath,
												URL[] urls) {
		ResourceHolder result = null;
		if (urls == null) // resource name is absolute URL
			urls = new URL[]{null};

		InputStream is = null;
		try {
			Locale locale = lrequester==null?null:lrequester.getLocale();
			String encoding = lrequester==null?null:lrequester.getEncoding();
			String extension = resourceType.getExtension();
			URLConnection resConn = null;
scanUrls: 
			for (int pi=0; pi<urls.length; pi++) {
				if (strategy != null) {
					Iterator fi = strategy.localizeName(ResourceManager.Strategy.PATH, locale, resPath, extension);
														
					while(fi.hasNext()) {
						resConn = checkUrl(urls[pi], (String)fi.next());
						if (resConn != null)
							break scanUrls;
					}
				} else {
					resConn = checkUrl(urls[pi], resPath+extension);
					if (resConn != null)
						break;
				}
				resConn = null;
			}
			if (resConn != null) {
				result = (ResourceHolder)Class.forName(resourceType.getResourceHolderClassName()).newInstance();
				//resConn.getIfModifiedSince();
				result.lastModified = resConn.getLastModified();
				result.updateData(result.getResourceFromStream(is = resConn.getInputStream(), encoding));
				result.address = resConn.getURL();
				result.encoding = encoding;
			}
		} catch(Exception ex) {
			result = null;
			ResourceManager.log ("createResource(URL)="+resPath, ex);
		} finally {
			if (is != null)
			try {
				is.close();
			} catch(IOException ioe) {ResourceManager.log( "close()", ioe);}
		}
		return result;
	}
	
	public static ResourceHolder createResource(ResourceManager.ResourceType resourceType,
												ResourceManager.Strategy strategy,
												ResourceManager.LocalizedRequester lrequester,
												String resPath,
												ServletContext sctx,
												String[] prefixes) {
		ResourceHolder result = null;
		if (sctx == null)
			return null;
		InputStream is = null;
		try {
			Locale locale = lrequester==null?null:lrequester.getLocale();
			String encoding = lrequester==null?null:lrequester.getEncoding();
			String extension = resourceType.getExtension();
			URL resUrl = null;
            URLConnection uc = null;
			if (prefixes == null) 
				prefixes = new String[]{""};
scanPaths:			
			for (int pi=0; pi<prefixes.length; pi++) {
				if (strategy != null) {
					Iterator fi = strategy.localizeName(ResourceManager.Strategy.PATH, locale, resPath, extension);
					while(fi.hasNext()) {
						try {
							resUrl = sctx.getResource(prefixes[pi] + fi.next());
							if (resUrl != null)
								break scanPaths;
						} catch (java.net.MalformedURLException mue) {
							continue scanPaths;
						}
					}
				} else {
					resUrl = sctx.getResource(prefixes[pi] + resPath + extension);
					if (resUrl != null)
						break;
				}
				resUrl = null;
			}
			uc = checkUrl(resUrl, "");
			if (uc != null) {
				result = (ResourceHolder)Class.forName(resourceType.getResourceHolderClassName()).newInstance();
				result.lastModified = uc.getLastModified();
				if (encoding == null)
					encoding = uc.getContentEncoding();
				result.updateData(result.getResourceFromStream(is = uc.getInputStream(), encoding));
				result.address = resUrl;
				result.encoding = encoding;
			}
		} catch(Exception ex) {
			result = null;
			ResourceManager.log ("createResource(ServletContext)="+resPath, ex);
		} finally {
			if (is != null)
				try {
					is.close();
				} catch(IOException ioe) {ResourceManager.log( "close()", ioe);}
		}
		return result;
	}
	
	public static String bracketPath(String path) {
		String[] elements = path.split("[/|\\\\]");
		//ResourceManager.log(String.format(">>>> processing %s len %d%n", path, elements.length));
		if (elements.length == 1)
			return path;
		StringBuffer normalized = new StringBuffer(path.length());
		int level = 0;
		for (String element:elements) {
			if ("..".equals(element)) {
				if (level > 0) {
					level --;					
				} else
					continue;
			} else
				level ++;
			normalized.append('/').append(element);
		}
		return normalized.toString();
	}
	
	protected static URLConnection checkUrl(URL url, String name) {
		URLConnection uc = null;
		try {
			url = url == null?new URL(name):new URL(url, name);
			ResourceManager.log("Checking URL:"+url);
			uc = url.openConnection();
			if (uc instanceof HttpURLConnection) {
				HttpURLConnection huc = (HttpURLConnection)uc;
				if (huc.getResponseCode() == 200)
					return uc;
				else
					huc.disconnect();
			} else if (uc instanceof JarURLConnection && ((JarURLConnection) uc).getJarEntry() != null) {
				uc.connect();
				return uc; // for jar and others
			} else {
				uc.connect();
				return uc;
			}
		} catch(MalformedURLException mfue) {
			ResourceManager.log("Invalid URL:"+url+'&'+name);			
		} catch(IOException ioe) {
			//ResourceManager.log("Can't reach "+url, ioe);
		}
		return null;
	}

	/*protected static InputStream getResourceAsStream(URLClassLoader classLoader, String path) {
		InputStream result = null;
		try {
			if (classLoader != null)
				result =  classLoader.getResourceAsStream(path);
			if (result == null)
				result = ResourceManager.class.getClassLoader().getResourceAsStream(path);
			if (result == null)
				result = ClassLoader.getSystemResourceAsStream(path);
		} catch(Exception e) {
			ResourceManager.log("getResourceAsStream()", e);
		}
		return result;
	}*/

	protected static URL getResource(ClassLoader classLoader, String path) {
		URL result = null;
		try {
			if (classLoader != null)
				if (classLoader instanceof URLClassLoader)
					result =  ((URLClassLoader)classLoader).findResource(path);
			     else
					 result = classLoader.getResource(path);
			// add a loop to look from parents
			if (result == null)
				result = ResourceManager.class.getClassLoader().getResource(path);
			if (result == null)
				result = ClassLoader.getSystemResource(path);
		} catch(Exception e) {
			ResourceManager.log("getResource(ClassLoader,String)", e);
		}
		ResourceManager.debug("Checking classloader "+path+"\n"+result);
		return result;
	}
	
	protected static boolean checkFile(File file) {
		ResourceManager.debug("Checking file "+file);
		if (file.exists() && file.isFile())
			try {
				file.getCanonicalPath();
				return true;
			} catch(IOException ioe) {
			}
		return false;
	}
		
	public ResourceHolder() {
	}

	public ResourceHolder(Object data, URL location, String name) {
		this.data = data;
		this.name = name;
		address = location;
	}
	
	public Object getData() {
		usageCount++;
		lastAccessed = System.currentTimeMillis();
		return data;
	}
	
	public void updateData(Object data) {
		this.data = data;
	}
	
	public synchronized boolean updateIfNeeded() throws ResourceException {
		if (address == null)
			throw new NullPointerException("Resource "+name+" wasn't initialized properly.");
		boolean result = true;
		long fileLastModified = lastModified;
		InputStream is = null;
		URLConnection resConn = null;
		try {
			resConn = address.openConnection();
			fileLastModified = resConn.getLastModified();
			result = fileLastModified == 0 || fileLastModified > lastModified;
			if (result) {
				updateData(getResourceFromStream(is=resConn.getInputStream(), encoding));
				lastModified = fileLastModified;
				ResourceManager.log("Res "+toString()+" reloaded on "+lastModified);
			}									   
		} catch(IOException ioe) {
			throw new ResourceException("I/O exception reading resource, can be moved, provide rescan locations.", ioe);
		} finally {
			if (is != null)
				try {
					is.close();
				} catch(IOException ioe) {ResourceManager.log( "close()", ioe);}
			/*if (resConn != null)
				try {
					ResourceManager.log("Disconnecting "+resConn.getClass().getName());
					resConn.getClass().getMethod("disconnect", EMPTY_PARAM_DEF).invoke(resConn, EMPTY_PARAM);
				} catch(Exception e) {
				}*/
		}
		return result;
	}
	
	/** Can be overriden for another algorithm of name calc
	 */
	void setName(String name) {
		this.name = name; 
	}
	
	String getName() {
		return name;
	}
	
	public String toString() {
		return "Resource "+name+" url: "+address;
	}
	
	protected abstract Object getResourceFromStream(InputStream is, String _encoding) throws IOException;

	static class UsageComparator implements Comparator {
		public int compare(Object _o1, Object _o2) throws ClassCastException {
			if (_o1 instanceof ResourceHolder && _o2 instanceof ResourceHolder) {
				return ((ResourceHolder)_o1).usageCount - ((ResourceHolder)_o2).usageCount;
			} else
				throw new ClassCastException();
		}
	}

	static class AccessComparator implements Comparator {
		public int compare(Object _o1, Object _o2) throws ClassCastException {
			if (_o1 instanceof ResourceHolder && _o2 instanceof ResourceHolder) {
				return (int)(((ResourceHolder)_o1).lastAccessed - ((ResourceHolder)_o2).lastAccessed);
			} else
				throw new ClassCastException();
		}
	}
//	protected static final Class[] EMPTY_PARAM_DEF = {};
//	protected static final Object[] EMPTY_PARAM = {};
}