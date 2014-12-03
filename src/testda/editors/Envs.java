/**
 * 
 */
package testda.editors;

import java.io.File;

/**
 * @author koyasukiichi
 *
 */
public class Envs {
	
	public final static String UTF_8 = "UTF-8";
	public final static String SJIS = "SJIS";
	public final static String EUC_JP = "EUC-JP";
	
	public static String[] getClasspath() {
		String property = System.getProperty("java.class.path", ".");
		return property.split(File.separator);
	}
	
	public static String[] getSourcepath() {
		return new String[] {"."};
	}
	
	public static String getEncoding() {
		return UTF_8;
	}
	
	public static String getLineSeparator() {
		return System.getProperty("line.separator", "\n");
	}
}
