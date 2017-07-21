package uk.ac.exeter.QuinCe.utils;

/**
 * Class for a string with a highlighted region
 * @author Steve Jones
 *
 */
public class HighlightedString {
	
	/**
	 * The string
	 */
	private String string;
	
	/**
	 * The index of the first highlighted character
	 */
	private int highlightStart;
	
	/**
	 * The index after the last highlighted character
	 */
	private int highlightEnd;

	/**
	 * Basic constructor
	 * @param string The string
	 * @param highlightStart The first highlighted character
	 * @param highlightEnd The character after the last highlighted character
	 * @throws HighlightedStringException If the string is empty or the highlight indices are invalid
	 */
	public HighlightedString(String string, int highlightStart, int highlightEnd) throws HighlightedStringException {
		if (null == string) {
			this.string = "";
		} else {
			this.string = string.trim();
		}
		this.highlightStart = highlightStart;
		this.highlightEnd = highlightEnd;
		
		if (this.string.length() == 0) {
			highlightStart = -1;
			highlightEnd = -1;
		} else {
			if (highlightStart >= string.length()) {
				throw new HighlightedStringException("Highlight start is outside the string bounds");
			}
			
			if (highlightStart < 0) {
				this.highlightStart = 0;
			}
			
			if (highlightEnd < highlightStart) {
				throw new HighlightedStringException("Highlight end cannot be before highlight start");
			}
			
			if (highlightEnd >= string.length()) {
				this.highlightEnd = string.length();
			}
		}
	}

	/**
	 * Get the highlighted string as a JSON object
	 * @return The string as a JSON object
	 */
	public String getJson() {
		StringBuilder json = new StringBuilder();
		
		json.append("{\"string\":\"");
		json.append(string.replaceAll(" ", "∙").replaceAll("\t", "⇥"));
		json.append("\",\"highlightStart\":");
		json.append(highlightStart);
		json.append(",\"highlightEnd\":");
		json.append(highlightEnd);
		json.append('}');
		
		return json.toString();
	}
	
	/**
	 * Get the highlighted portion of the string
	 * @return The highlighted portion of the string
	 */
	public String getHighlightedPortion(){
		return string.substring(highlightStart, highlightEnd);
	}
}
