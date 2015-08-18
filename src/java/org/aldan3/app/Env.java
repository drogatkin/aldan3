package org.aldan3.app;

/** Methods to get access to environment
 * 
 * @author Dmitriy Rogatkin
 *
 */
public class Env {
	private static final boolean android = System.getProperty("java.vm.name") != null
			&& System.getProperty("java.vm.name").startsWith("Dalvik");

	public static final boolean isAndroid() {
		return android;
	}
}
