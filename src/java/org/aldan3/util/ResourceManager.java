/* aldan3 - ResourceManager.java
 * Copyright (C) 1999-2009 Dmitriy Rogatkin.  All rights reserved.
 *  $Id: ResourceManager.java,v 1.10 2009/08/04 22:35:38 dmitriy Exp $                
 *  Created on Jun 23, 2009
 *  @author Dmitriy
 */
package  org.aldan3.util;
import java.io.File;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Locale;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Comparator;
import java.util.TimeZone;
import java.util.Properties;
import java.util.Map;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.net.URL;

import javax.servlet.ServletContext;

import org.aldan3.model.AccessControl;
import org.aldan3.model.Log;

/** This class encapsulate resource managing, providing access to local and
 * remote resources. 
 */
public class ResourceManager {
    // TODO: add periodically running task for flushing obsolete resources
    // it's alternative approach to check resource freshness just upon request
    /** This class define strategy of searching for a resource
     */
    public static interface Strategy {
        public static final int PATH = 1;

        public static final int URL = 2;

        public static final int SERVLET = 3;

        public static final int CLASSLOADER = 4;

        public static final int[] LOCAL_FILES = { PATH, URL, CLASSLOADER, SERVLET };

        public static final int[] WEB_SERVER = { SERVLET, URL, CLASSLOADER, PATH };

        int[] get();

        Iterator<String> localizeName(int type, Locale locale, String name, String extension/*[]*/);
    }

    /** Holder for current localization parameters of a resource requester
     * 
     * @author dmitriy
     *
     */
    public static interface LocalizedRequester {
    	/** Returns preferred locale
    	 * 
    	 * @return preferred locale or null for default
    	 */
        Locale getLocale();

        /** Returns preferred encoding
         * 
         * @return preferred encoding or null for default
         */
        String getEncoding();

        /** Returns preferred time zone
         * 
         * @return preferred time zone, null for default
         */
        TimeZone getTimeZone();
    }

    /** This class providing type of stored resources improving access time
     */
    static public class ResourceType {
        //Class typeClass;
        public String resourceHolderClass;

        public String resourceFileExtension;

        /** Constructs resource type
         * 
         * @param className class of resource holder
         * @param extension storing file extension if any
         */
        public ResourceType(String className, String extension) {
            if (className == null)
                throw new NullPointerException();
            resourceHolderClass = className;
            if (extension != null)
                resourceFileExtension = extension;
            else
                resourceFileExtension = "";
        }

        /** Returns extension
         * 
         * @return comma separated extensions in order of priority
         */
        public String getExtension() {
            return resourceFileExtension;
        }

        /** Returns resource class name
         * 
         * @return class name
         */
        public String getResourceHolderClassName() {
            return resourceHolderClass;
        }

        /** Constructs resource file name based on locale
         * 
         * @param path plain resource path
         * @param locale 
         * @return resource path with locale information
         */
        public String buildName(String path, Locale locale) {
            return locale == null ? path : path + '@' + locale;
        }

        /** hash code since equals redefined
         * @return hash code
         */
        public int hashCode() {
            return resourceHolderClass.hashCode() ^ resourceFileExtension.hashCode();
        }

        /** Check if resource types are equal
         * @param other resource type
         * @return true when equals
         */
        public boolean equals(Object o) {
            return o == this
                    || (o instanceof ResourceType
                            && ((ResourceType) o).resourceFileExtension.equals(this.resourceFileExtension) && ((ResourceType) o).resourceHolderClass
                            .equals(this.resourceHolderClass));
        }
    }

    /** An implementation of resource searching strategy
     * 
     * @author dmitriy
     *
     */
    public static class SimpleStrategy implements Strategy {
        int[] scanOrder;

        /** Constructor
         * 
         *
         */
        public SimpleStrategy() {
            scanOrder = Strategy.LOCAL_FILES;
        }

        /** Constructor with search order
         * 
         * @param anOrder resource will be searched in order of elements of
         * this array. Value of element specifies type of search.
         */
        public SimpleStrategy(int[] anOrder) {
            scanOrder = anOrder;
        }

        
        /** gets resource search orders
         * @return array in order of search types
         */
        public int[] get() {
            return scanOrder;
        }

        /** provides combination of names and type orders upon used strategy
         * 
         */
        public Iterator localizeName(int type, final Locale plocale, final String pname, final String pextension) {
            return new Iterator() {
                int variant;

                String extension, name, cntry, lang, parent;

                {
                    if (pname == null)
                        throw new NullPointerException("Name of resources 'null'");
                    if (pextension == null || pextension.length() == 0) {
                        int dotPos = pname.lastIndexOf('.');
                        if (dotPos > 0) {
                            this.extension = pname.substring(dotPos);
                            this.name = pname.substring(0, dotPos);
                        } else {
                            this.extension = "";
                            this.name = pname;
                        }
                    } else {
                        this.name = pname;
                        this.extension = pextension;
                    }
                    if (plocale != null) {
                        this.cntry = plocale.getCountry();
                        this.lang = plocale.getLanguage();
                    }
                    separateParent();
                    if (lang == null)
                        variant = 4;
                    else if (cntry == null)
                    	variant = 2;
                }

                public boolean hasNext() {
                    return variant < 5;
                }

                public Object next() {
                    switch (variant++) {
                    case 0:
                        return parent + lang + '_' +cntry + '/' + name + extension;
                    case 1:
                        return parent + name + '_' + lang + '_' +cntry + extension;
                    case 2:
                        return parent + lang + '/' + name + extension;
                    case 3:
                        return parent + name + '_' + lang + extension;
                    case 4:
                        return parent + name + extension;
                    }
                    throw new java.util.NoSuchElementException();
                }

                public void remove() {
                }

                protected void separateParent() {
                    int bs_p = name.lastIndexOf('\\');
                    int s_p = name.lastIndexOf('/');
                    if (bs_p > s_p)
                        s_p = bs_p;
                    if (s_p >= 0) {
                        parent = name.substring(0, s_p + 1);
                        name = name.substring(s_p + 1);
                    } else
                        parent = "";
                }
            };
        }
    }

    /** Default string resource type
     * 
     */
    public static final ResourceType STRING_RES = new ResourceType(ResourceManager.class.getPackage().getName()+".StringResourceHolder", "");

    /** Default char array resource type
     * 
     */
    public static final ResourceType CHARARRAY_RES = new ResourceType(ResourceManager.class.getPackage().getName()+".CharArrayResourceHolder", "");

    /** Default resource bundle holder type
     * 
     */
    public static final ResourceType RESOURCE_RES = new ResourceType(ResourceManager.class.getPackage().getName()+".ResourceBundleResourceHolder",
            ".properties");

    private static final String ID = "Res Man";

    // static singleton like
    protected static Log logger;

    protected static File defaultPaths[];

    protected static URLClassLoader defaultResClassLoader;

    protected static ServletContext defaultServletContext;

    protected static URL[] defaultSearchURLs;

    protected static Map<ResourceType, ResourceManager> resourceManagers = new HashMap<ResourceType,ResourceManager>();

    // dynamic per instance
    protected boolean cached = true;

    protected boolean dynamic = true;
    
    protected int checkInterval = 3;
    
	protected UpdatesWatcher threadUpdater = null;

    protected Map storage;

    protected int maxSize = 50;

    protected int growSize = 5;

    protected AccessControl sm;

    protected Strategy strategy;

    protected ResourceType resType;

    protected File[] searchPaths;

    protected ClassLoader resClassLoader;

    protected ServletContext servletContext;

    protected URL[] searchURLs;

    protected String[] contextRoots;

    protected String[] classesRoots;

    /** Sets default search paths for resource types of
     * @param resource type
     * @param search strategy 
     */
    public static void setDefaultFileSearchPaths(ResourceType type, String paths) {
        defaultPaths = parseSearchPathsString(paths);
    }

    /** Creates a resource manager of requested type and search strategy
     * 
     * @param type
     * @param strategy
     * @return resource manager
     */
    public static ResourceManager createResourceManager(ResourceType type, Strategy strategy) {
        ResourceManager resourceManager = new ResourceManager();
        if (type != null)
            resourceManager.setType(type);
        resourceManager.setStrategy(strategy == null ? new SimpleStrategy() : strategy);
        return resourceManager;
    }

    /** Creates a resource manager of given type and default search strategy
     * 
     * @param type
     * @return resource manager
     */
    public static ResourceManager getResourceManager(ResourceType type) {
        return getResourceManager(type, null, null, null, null, null);
    }

    /** Returns a resource manager of given type with specified file system root path, URLs and servlet contexts
     * <p>
     * If the resource manager doesn't exist already, then creates a new one and set
     * resources paths, otherwise returns existing without paths redefiniton 
     * 
     * @param type
     * @param paths
     * @param urls
     * @param sctx
     * @param contextLocations paths
     * @param resPackages Java package names
     * @return resource manager
     */
    public static ResourceManager getResourceManager(ResourceType type, String paths, String urls, ServletContext sctx,
            String contextLocations, String resPackages) {
        if (type == null)
            return createResourceManager(null, null);
        else {
            ResourceManager result = (ResourceManager) resourceManagers.get(type);
            synchronized (resourceManagers) {
                if (result == null) {
                    result = createResourceManager(type, null);
                    result.setFileSearchPaths(paths);
                    result.setFileSearchUrls(urls);
                    result.setServletContext(sctx);
                    result.setResourcePackages(resPackages);
                    result.setServletContextLocations(contextLocations);
                    resourceManagers.put(type, result);
                }
            }
            return result;
        }
    }

    /** Creates a resource manager reading configuration from properties. properties name specified.
     * 
     * @param type
     * @param props
     * @param pathProp
     * @param urlProp
     * @param sctx
     * @return resource manager
     */
    public static ResourceManager getResourceManager(ResourceType type, Properties props, String pathProp,
            String urlProp, ServletContext sctx) {
        return getResourceManager(type, props.getProperty(pathProp), props.getProperty(urlProp), sctx, props
                .getProperty(urlProp), props.getProperty(pathProp));
    }

    /** Creates a resource manager reading configuration from properties with predefined names, as
     * RESOURCE_ROOT, URL_RESOURCE_ROOT, CONTEXT_RESOURCE_ROOT, CLASS_RESOURCE_ROOT
     * @param type
     * @param props
     * @param sctx
     * @return
     */ 
    public static ResourceManager getResourceManager(ResourceType type, Properties props, ServletContext sctx) {
        return getResourceManager(type, props.getProperty("RESOURCE_ROOT"), props.getProperty("URL_RESOURCE_ROOT"),
                sctx, props.getProperty("CONTEXT_RESOURCE_ROOT"), props.getProperty("CLASS_RESOURCE_ROOT"));
    }

    /** Creates a resource manager reading configuration from properties and not using in servlets.
     * 
     * @param type
     * @param props
     * @return a resource manager
     */
    public static ResourceManager getResourceManager(ResourceType type, Properties props) {
        return getResourceManager(type, props, (ServletContext) null);
    }

    protected ResourceManager() {
        searchPaths = defaultPaths;
        if (defaultResClassLoader != null)
            resClassLoader = defaultResClassLoader;
        else
            resClassLoader = getClass().getClassLoader();
        servletContext = defaultServletContext;
        searchURLs = defaultSearchURLs;
        if (cached)
            storage = new HashMap(maxSize);
    }

    static void log(String message) {
        if (logger != null)
            logger.info(message);
        else
            System.out.println(formatMessage(Log.INFO, message));
    }

    static void debug(String message) {
        if (logger != null)
            logger.debug(message);
        else
            System.out.println(formatMessage(Log.DEBUG, message));
    }

    static void log(String message, Throwable t) {
        StringWriter stackTrace = null;
        if (t != null) {
            stackTrace = new StringWriter(100);
            t.printStackTrace(new PrintWriter(stackTrace));
            message = message + '\n' + stackTrace;
        }
        if (logger != null)
            logger.error( message, t );
        else
            System.err.println(formatMessage( Log.ERROR, message ));
    }

    protected static String formatMessage(String level, String message) {
        return String.format("(%d) %s [%s] %s", System.currentTimeMillis(), ID, level, message);
    }

    /** Requests a resource from a manager
     * 
     * @param path resource name
     * @param requester object
     * @return resource object
     * @throws ResourceException if resource can't be found, security problem, or IO
     */
    public final Object getResource(String path, Object requester) throws ResourceException {
        if (path == null)
            throw new ResourceException("No resource can be found for 'null'");
        if (sm != null)
            sm.check(path, requester);
        String cacheName = null;
        ResourceHolder resource = null;
        if (cached) {
            if (resType != null)
                cacheName = resType.buildName(path, requester == null
                        || requester instanceof LocalizedRequester == false ? null : ((LocalizedRequester) requester)
                        .getLocale());
            else
                cacheName = path;
            SoftReference ref = (SoftReference) storage.get(cacheName);
            if (ref != null)
                resource = (ResourceHolder) ref.get();
            // check if need reload
            debug("Res " + cacheName + "(" + ref + " = " + resource);
        }
        if (resource != null) {
            if (dynamic)
                resource.updateIfNeeded();
            return resource.getData();
        }
		if (cached) {
			SoftReference ref = null;
			synchronized (storage) {
				ref = (SoftReference) storage.get(cacheName);
			}
			if (ref != null)
				resource = (ResourceHolder) ref.get();
			if (resource == null) { // race condition, so it can be already there
				resource = createResource(path, requester);
				if (resource != null) {
					resource.setName(cacheName);
					log("Put in cache under " + cacheName);
					synchronized (storage) {
						if (storage.size() >= maxSize)
							resizeTo(maxSize);
						storage.put(cacheName, new SoftReference(resource));
					}
				}
			}
		} else
			resource = createResource(path, requester);

        if (resource == null)
            throw new ResourceException("Resource " + path + " can't be found", ResourceException.NOT_FOUND);
        return resource.getData();
    }

    /** Sets file search root paths
     * 
     * @param paths
     */
    public void setFileSearchPaths(String paths) {
        searchPaths = parseSearchPathsString(paths);
    }

    /** Sets URLs 
     * 
     * @param urls
     */ 
    public void setFileSearchUrls(String urls) {
        searchURLs = parseSearchURLsString(urls);
    }

    /** Sets servlet context for searching
     * 
     * @param sctx
     */
    public void setServletContext(ServletContext sctx) {
        servletContext = sctx;
    }

    /** Sets packages for resources when a class loader used for searching
     * 
     * @param packages
     */
    public void setResourcePackages(String packages) {
    	if (packages != null)
    		classesRoots = parsePathPrefixes(packages.replace('.', '/'), false);
    }

    /** Sets servlet context locations
     * 
     * @param locations
     */
    public void setServletContextLocations(String locations) {
        contextRoots = parsePathPrefixes(locations, true);
    }

    /** Sets if check last modified to figure out using cached value
     * 
     * @param set true if check last modified and compare with cached to make
     * decision about reloading a resource
     */
    public void setCheckLastModified(boolean set) {
        dynamic = set;
    }

	public void setUseUpdatesWatcher(boolean set) {
		if (set) {
			if (threadUpdater == null || threadUpdater.isAlive() == false) {
				threadUpdater = new UpdatesWatcher();
				threadUpdater.setDaemon(true);
				threadUpdater.setPriority(Thread.MIN_PRIORITY);
				threadUpdater.setName("ResMan "+toString());
				threadUpdater.start();
			}
		} else {
			if (threadUpdater != null) {
				threadUpdater.interrupt();
				threadUpdater = null;
			}
		}
	}
	
	public void setUpdatesCheckInterval(int intInSec) {
		checkInterval = intInSec;
	}

    /** Sets a class loader for resource search
     * 
     * @param classLoader
     */
    public void setClassLoader(ClassLoader classLoader) {
        resClassLoader = classLoader;
    }

    /** Checks if resources cached
     * 
     * @return true if cached, false otherwise
     */
    public boolean isCached() {
        return cached;
    }

    /** Sets resource search strategy
     * 
     * @param strategy
     */
    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    /** Sets to use resource cache
     * 
     * @param set true to use cache, false disable cache
     */
    public void setCached(boolean set) {
        cached = set;
    }

    /** Sets resource type
     * 
     * @param type
     */
    public void setType(ResourceType type) {
        resType = type;
    }

    /** Sets resource cache size
     * 
     * @param size max size
     * @param grow how much when need more
     */
    public void setCacheSize(int size, int grow) {
        if (size > 10)
            maxSize = size;
        if (grow > 0 && grow < maxSize)
            growSize = grow;
    }

    /** Sets an access control provider
     * 
     * @param sm access control provider
     */
    public void setAccessControl(AccessControl acp) {
        this.sm = acp;
    }

    /** Sets custom logger
     * 
     * @param logger
     */
    public static void setLogger(Log logger) {
        ResourceManager.logger = logger;
    }

    protected ResourceHolder createResource(String path, Object requester) {
        ResourceHolder resource = null;
        // follow strategy 
        int[] searchOrder = null;
        if (strategy != null)
            searchOrder = strategy.get();
        else
            searchOrder = Strategy.LOCAL_FILES;

        for (int si = 0; si < searchOrder.length; si++) {
            switch (searchOrder[si]) {
            case Strategy.PATH:
                resource = ResourceHolder.createResource(resType, strategy,
                        requester instanceof LocalizedRequester ? (LocalizedRequester) requester : null, path,
                        searchPaths);
                log(String.format("====>Path %s%n", resource));
                break;
            case Strategy.CLASSLOADER:
                resource = ResourceHolder.createResource(resType, strategy,
                        requester instanceof LocalizedRequester ? (LocalizedRequester) requester : null, path,
                        resClassLoader, classesRoots);
                log(String.format("====>ClassLoader %s%n", resource));
                break;
            case Strategy.URL:
                resource = ResourceHolder.createResource(resType, strategy,
                        requester instanceof LocalizedRequester ? (LocalizedRequester) requester : null, path,
                        searchURLs);
                log(String.format("====>URL %s%n", resource));
                break;
            case Strategy.SERVLET:
                resource = ResourceHolder.createResource(resType, strategy,
                        requester instanceof LocalizedRequester ? (LocalizedRequester) requester : null, path,
                        servletContext, contextRoots);
                log(String.format("====>Context %s%n", resource));
                break;
            default:
                log("", new Exception("Invalid strategy code:" + searchOrder[si]));

            }
            if (resource != null)
                break;
        }
        return resource;
    }

    protected void resizeTo(int _size) {
        if (storage.size() < _size)
            return;
        TreeSet ts = new TreeSet((Comparator) new ResourceHolder.UsageComparator());
        synchronized (storage) {
            // reordering current map
            Iterator i = storage.values().iterator();
            while (i.hasNext()) {
                SoftReference ref = (SoftReference) i.next();
                if (ref != null) {
                    Object o = ref.get();
                    if (o != null)
                        ts.add(o);
                }
            }
            int l = storage.size() - (maxSize - growSize);
            i = ts.iterator();
            for (int k = 0; k < l && i.hasNext(); k++) {
                storage.remove(((ResourceHolder) i.next()).getName());
            }
        }
    }

    /**This method used internally to notify resource managers that 
     * some system settings changed
     */
    protected void settingsChanged() {
    }

    protected static String[] parsePathPrefixes(String prefixes, boolean leadSlash) {
        if (prefixes == null)
            return new String[] { "" };
        String[] result = prefixes.split(File.pathSeparator);
        for (int i = 0; i < result.length; i++) {
            if (result[i].length() == 0)
                continue;

            char ec = result[i].charAt(result[i].length() - 1);

            if (ec == '\\')
                result[i] = result[i].substring(0, result[i].length() - 1) + '/';
            else if (ec != '/')
                result[i] += '/';
            char lc = result[i].charAt(0);
            if (lc == '\\') {
                result[i] = result[i].substring(1);
                if (result[i].length() == 0)
                    continue;
                lc = result[i].charAt(0);
            }
            if (leadSlash) {
                if (lc != '/')
                    result[i] = "/" + result[i];
            } else {
                if (lc == '/')
                    result[i] = result[i].substring(1);
            }
        }
        return result;
    }

    protected static File[] parseSearchPathsString(String paths) {
        if (paths == null)
            return new File[] { null };
        List files = new ArrayList();
        String[] arpaths = paths.split(File.pathSeparator);
        for (int i = 0; i < arpaths.length; i++) {
			File f;
			try {
				f = arpaths[i].startsWith("file:")?new File(new URL(arpaths[i]).getFile()):new File(arpaths[i]);
				if (f.exists() && f.isDirectory())
					files.add(f);
				else
					log("File resource directory '"+f+"' not found");
			} catch (MalformedURLException e) {
			}
        }
        return (File[]) files.toArray(new File[files.size()]);
    }

    protected static URL[] parseSearchURLsString(String strurls) {
        if (strurls == null)
            return new URL[] { null };
        List urls = new ArrayList();
        String[] arurls = strurls.split("[," + File.pathSeparator + ']');
        for (int i = 0; i < arurls.length; i++) {
            try {
                URL u = new URL(arurls[i]);
                urls.add(u);
            } catch (java.net.MalformedURLException mfe) {
            }
        }
        return (URL[]) urls.toArray(new URL[urls.size()]);
    }

    protected class UpdatesWatcher extends Thread {
		
		public void run() {
			int sleepInt = 1000 * checkInterval;
			while(true) {
				if (cached == false || dynamic)
					break;
				//List content = new LinkedList();
				Collection items = null;
				synchronized(storage) {
					items = storage.values();
				}
				Iterator i = items.iterator();
				while(i.hasNext()) {
					SoftReference ref = (SoftReference) i.next();
					if (ref != null) {
						ResourceHolder resource = (ResourceHolder) ref.get();
						try {
							resource.updateIfNeeded();
						} catch (ResourceException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				try {
					sleep(sleepInt);
				} catch(InterruptedException ie) {
					break;
				}
			}
		}
	}
}