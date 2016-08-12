package uk.ac.exeter.QuinCe.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
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
	 * Convert a delimited list of integers into a list of integers
	 * @param values The list
	 * @return The list as integers
	 */
	public static List<Integer> delimitedToIntegerList(String values) {
		
		List<Integer> result = null;
		
		if (values != null) {
			List<String> stringList = delimitedToList(values);
			result = new ArrayList<Integer>(stringList.size());

			for (String item: stringList) {
				result.add(Integer.parseInt(item));
			}
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
	
	/**
	 * Determines whether or not a line is a comment, signified by it starting with {@code #} or {@code !} or {@code //}
	 * @param line The line to be checked
	 * @return {@code true} if the line is a comment; {@code false} otherwise.
	 */
	public static boolean isComment(String line) {
		String trimmedLine = line.trim();
		return trimmedLine.length() == 0 || trimmedLine.charAt(0) == '#' || trimmedLine.charAt(0) == '!' || trimmedLine.startsWith("//", 0);
	}
	
	/**
	 * Trims all items in a list of strings. A string that starts with a
	 * single backslash has that backslash removed.
	 * @param source The strings to be converted 
	 * @return The converted strings
	 */
	public static List<String> trimList(List<String> source) {
		
		List<String> result = new ArrayList<String>(source.size());
		
		for (int i = 0; i < source.size(); i++) {
			String trimmedValue = source.get(i).trim();
			if (trimmedValue.startsWith("\\")) {
				trimmedValue = trimmedValue.substring(1);
			}
			
			result.add(trimmedValue);
		}
		
		return result;
	}
	
	public static boolean isNumeric(String value) {
		boolean result = true;
		
		if (null == value) {
			result = false;
		} else {
			try {
				Double doubleValue = new Double(value);
				if (doubleValue.isNaN()) {
					result = false;
				}
			} catch (NumberFormatException e) {
				result = false;
			}
		}
		
		return result;
	}
}
