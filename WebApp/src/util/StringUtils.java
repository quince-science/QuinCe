package util;

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
		
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < list.size(); i++) {
			result.append(list.get(i));
			if (i < (list.size() - 1)) {
				result.append(delimiter);
			}
		}
		
		return result.toString();
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
		String delimiter = ";";
		return Arrays.asList(values.split(delimiter, 0));
	}
}
