package org.aldan3.app;

/** Methods to get access to environment
 * 
 * @author Dmitriy Rogatkin
 *
 */
public class Env {
	private static final boolean android = System.getProperty("java.vm.name") != null
			&& System.getProperty("java.vm.name").startsWith("Dalvik");
	
	private static int ANDROID_JDK_LEVEL = -1;
	
	private static int JAVA_VESRION = 4;

	public static final boolean isAndroid() {
		return android;
	}
	
	public static int getAndroidJDKLevel() {
		return ANDROID_JDK_LEVEL;
	}
	
	public static int getJavaVersion() {
		return JAVA_VESRION;
	}
	
	static {
		try {
			Class<?> andVerCl = Class.forName("android.os.Build.VERSION");
			ANDROID_JDK_LEVEL = andVerCl.getField("SDK_INT").getInt(null);
		} catch(Exception e) {
			
		}
		String jvs = System.getProperty("java.specification.version"); 
		if (jvs.startsWith("1.")) {
			int nd=jvs.indexOf('.', 2);
			if (nd > 1) {
				try {
					JAVA_VESRION =Integer.parseInt(jvs.substring(2, nd));
				} catch(Exception e) {
					
				}
			}
		} else {
			try {
				JAVA_VESRION =Integer.parseInt(jvs);
			} catch(Exception e) {
				
			}
		}
	}
}
