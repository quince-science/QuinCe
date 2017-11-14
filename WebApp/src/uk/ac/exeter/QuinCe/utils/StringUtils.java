package uk.ac.exeter.QuinCe.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Miscellaneous string utilities
 * @author Steve Jones
 *
 */
public class StringUtils {

	/**
	 * Converts a list of values to a single string,
	 * with a semi-colon delimiter.
	 * 
	 * <b>Note that this does not handle semi-colons within the values themselves.</b>
	 * 
	 * @param list The list to be converted
	 * @return The converted list
	 */
	public static String listToDelimited(List<?> list) {
		return listToDelimited(list, ";", null);
	}
	
	/**
	 * Converts a list of values to a single string,
	 * with a specified delimiter.
	 * 
	 * <b>Note that this does not handle the case where the delimiter is found within the values themselves.</b>
	 * 
	 * @param list The list to be converted
	 * @param delimiter The delimiter to use
	 * @return The converted list
	 */
	public static String listToDelimited(List<?> list, String delimiter) {
		return listToDelimited(list, delimiter, null);
	}
	
	public static String listToDelimited(List<?> list, String delimiter, String surrounder) {
		
		String result = null;
		
		if (null != list) {
			StringBuilder buildResult = new StringBuilder();
			for (int i = 0; i < list.size(); i++) {
				
				if (null != surrounder) {
					buildResult.append(surrounder);
					buildResult.append(list.get(i).toString().replace(surrounder, "\\" + surrounder));
					buildResult.append(surrounder);
				} else {
					buildResult.append(list.get(i).toString());
				}

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
	 * Convert a delimited list of double into a list of doubles
	 * @param values The list
	 * @return The list as integers
	 */
	public static List<Double> delimitedToDoubleList(String values) {
		
		List<Double> result = null;
		
		if (values != null) {
			List<String> stringList = delimitedToList(values);
			result = new ArrayList<Double>(stringList.size());

			for (String item: stringList) {
				result.add(Double.parseDouble(item));
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
	
	public static boolean isInteger(String value) {
		boolean result = true;
		
		if (null == value) {
			result = false;
		} else {
			try {
				new Integer(value);
			} catch (NumberFormatException e) {
				result = false;
			}
		}
		
		return result;
	}
	
	public static String mapToDelimited(Map<String, String> map) {
		
		StringBuilder result = new StringBuilder();
		
		int counter = 0;
		for (Map.Entry<String, String> entry : map.entrySet()) {
			counter++;
			result.append(entry.getKey());
			result.append('=');
			result.append(entry.getValue());
			
			if (counter < map.size()) {
				result.append(';');
			}
		}
		
		return result.toString();
	}
	
	public static Map<String,String> delimitedToMap(String values) throws StringFormatException {
		
		Map<String, String> result = new HashMap<String, String>();
		
		for (String entry : values.split(";", 0)) {
			
			String[] entrySplit = entry.split("=", 0);
			if (entrySplit.length != 2) {
				throw new StringFormatException("Invalid map format", entry);
			} else {
				result.put(entrySplit[0], entrySplit[1]);
			}
		}
		
		return result;
	}
	
	/**
	 * Convert a case-insensitive Y/N value to a boolean
	 * @param value The value
	 * @return The boolean value
	 * @throws StringFormatException If the supplied value is not Y or N
	 */
	public static boolean parseYNBoolean(String value) throws StringFormatException {
		boolean result;
		
		switch(value.toUpperCase()) {
		case "Y": {
			result = true;
			break;
		}
		case "N": {
			result = false;
			break;
		}
		default: {
			throw new StringFormatException("Invalid boolean value", value);
		}
		}
		
		return result;
	}
	
	/**
	 * Convert a Properties object into a JSON string
	 * @param properties The properties
	 * @return The JSON string
	 */
	public static String getPropertiesAsJson(Properties properties) {
		
		
		StringBuilder result = new StringBuilder();
		if (null == properties) {
			result.append("null");
		} else {
		
			result.append('{');

			int propCount = 0;
			for (String prop : properties.stringPropertyNames()) {
				propCount++;
				result.append('"');
				result.append(prop);
				result.append("\":\"");
				result.append(properties.getProperty(prop));
				result.append('"');
				
				if (propCount < properties.size()) {
					result.append(',');
				}
			}
			
			
			result.append('}');
		}
		
		return result.toString();
	}
	
	/**
	 * Create a {@link Properties} object from a string
	 * @param propsString The properties String
	 * @return The Properties object
	 * @throws IOException If the string cannot be parsed
	 */
	public static Properties propertiesFromString(String propsString) throws IOException {
		Properties result = null;

		if (null != propsString && propsString.length() > 0) {
			StringReader reader = new StringReader(propsString);
			Properties props = new Properties();
			props.load(reader);
			return props;
		}

		return result;
	}
	
	/**
	 * Create a JSON field value
	 * @param fieldNumber The field number
	 * @param value The field value
	 * @return The field string
	 */
	public static String makeJsonField(int fieldNumber, String value) {
		return makeJsonField(fieldNumber, value, true);
	}
	
	/**
	 * Create a JSON field value
	 * @param fieldNumber The field number
	 * @param value The field value
	 * @param asString Indicates whether or not the value should be represented as a String
	 * @return The field string
	 */
	public static String makeJsonField(int fieldNumber, double value, boolean asString) {
		return makeJsonField(fieldNumber, String.valueOf(value), asString);
	}
	
	/**
	 * Create a JSON field value
	 * @param fieldNumber The field number
	 * @param value The field value
	 * @param asString Indicates whether or not the value should be represented as a String
	 * @return The field string
	 */
	public static String makeJsonField(int fieldNumber, boolean value, boolean asString) {
		return makeJsonField(fieldNumber, String.valueOf(value), asString);
	}
	
	/**
	 * Create a JSON field value
	 * @param fieldNumber The field number
	 * @param value The field value
	 * @return The field string
	 */
	public static String makeJsonField(int fieldNumber, double value) {
		return makeJsonField(fieldNumber, String.valueOf(value), false);
	}
	
	/**
	 * Create a JSON field value
	 * @param fieldNumber The field number
	 * @param value The field value
	 * @return The field string
	 */
	public static String makeJsonField(int fieldNumber, boolean value) {
		return makeJsonField(fieldNumber, String.valueOf(value), false);
	}
	
	/**
	 * Create a JSON field value
	 * @param fieldNumber The field number
	 * @param value The field value
	 * @param asString Indicates whether or not the value should be represented as a String
	 * @return The field string
	 */
	public static String makeJsonField(int fieldNumber, long value, boolean asString) {
		return makeJsonField(fieldNumber, String.valueOf(value), asString);
	}
	
	/**
	 * Create a JSON field value
	 * @param fieldNumber The field number
	 * @param value The field value
	 * @return The field string
	 */
	public static String makeJsonField(int fieldNumber, long value) {
		return makeJsonField(fieldNumber, String.valueOf(value), false);
	}
	
	/**
	 * Create a JSON field value
	 * @param fieldNumber The field number
	 * @param value The field value
	 * @param asString Indicates whether or not the value should be represented as a String
	 * @return The field string
	 */
	public static String makeJsonField(int fieldNumber, String value, boolean asString) {
		return makeJsonField(String.valueOf(fieldNumber), value, asString);
	}

	/**
	 * Create a JSON field value
	 * @param fieldName The field name
	 * @param value The field value
	 * @param asString Indicates whether or not the value should be represented as a String
	 * @return The field string
	 */
	public static String makeJsonField(String fieldName, String value, boolean asString) {
		
		StringBuilder field = new StringBuilder();
		
		field.append("\"");
		field.append(fieldName);
		field.append("\":");
		
		if (asString) {
			field.append("\"");
		}
		
		field.append(value);
		
		if (asString) {
			field.append("\"");
		}
		
		return field.toString();
	}
}
