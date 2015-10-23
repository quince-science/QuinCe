package uk.ac.exeter.QuinCe.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

/**
 * Various miscellaneous string utilities
 * @author Steve Jones
 *
 */
public class StringUtils {

	/**
	 * Converts a list of String values to a single string,
	 * with a semi-colon delimiter.
	 * 
	 * <b>Note that this does not handle semi-colons within the values themselves.</b>
	 * 
	 * @param list The list to be converted
	 * @return The converted list
	 */
	public static String listToDelimited(List<String> list) {
		String delimiter = ";";
		
		String result = null;
		
		if (null != list) {
			StringBuffer buildResult = new StringBuffer();
			for (int i = 0; i < list.size(); i++) {
				buildResult.append(list.get(i));
				if (i < (list.size() - 1)) {
					buildResult.append(delimiter);
				}
			}
			result = buildResult.toString();
		}
		
		return result;
	}
	
	/**
	 * Converts a String containing values separated by semi-colon delimiters
	 * into a list of String values
	 * 
	 * <b>Note that this does not handle semi-colons within the values themselves.</b>
	 * 
	 * @param values The String to be converted
	 * @return A list of String values
	 */
	public static List<String> delimitedToList(String values) {
		
		List<String> result = null;
		
		if (null != values) {
			String delimiter = ";";
			result = Arrays.asList(values.split(delimiter, 0));
		}
		
		return result;
	}
	
	/**
	 * Extract the stack trace from an Exception (or other
	 * Throwable) as a String.
	 * @param e The error
	 * @return The stack trace
	 */
	public static String stackTraceToString(Throwable e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}
}
