package uk.ac.exeter.QuinCe.web.html;

import java.util.List;

/**
 * Various HTML-related utilities and constants
 * @author Steve Jones
 *
 */
public class HtmlUtils {

	public static final String CLASS_ERROR = "error";
	
	public static final String CLASS_INFO = "info";
	
	public static String makeJSONArray(List<String> lines) {
		StringBuilder output = new StringBuilder();
		
		output.append('[');
		
		for (int i = 0; i < lines.size(); i++) {
			output.append('"');
			output.append(lines.get(i)
					.replace("\r", "")
					.replace("\n", "")
					.replace("\\", "\\\\")
					.replace("\"", "\\\""));
			
			output.append('"');
			
			if (i < (lines.size() - 1)) {
				output.append(',');
			}
		}
		
		output.append(']');
		
		return output.toString();
	}
}
