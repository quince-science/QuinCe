package uk.ac.exeter.QuinCe.data.Files;

/**
 * Class to hold a message related to a data file
 * @author Steve Jones
 *
 */
public class DataFileMessage implements Comparable<DataFileMessage> {

	/**
	 * The row to which the message applies.
	 * {@code -1} indicates that the message is not related
	 * to any particular row.
	 */
	private int line;

	/**
	 * The message text
	 */
	private String message;

	/**
	 * Constructor for a message that is not related
	 * to a specific row in the file
	 * @param message The message text
	 */
	public DataFileMessage(String message) {
		this.line = -1;
		this.message = message;
	}

	/**
	 * Constructor for a message related to a
	 * specific row in the file
	 * @param line The row number
	 * @param message The message text
	 */
	public DataFileMessage(int line, String message) {
		this.line = line + 1; // Human-readable line numbers in messages!
		this.message = message;
	}

	@Override
	public String toString() {
		StringBuilder string = new StringBuilder();
		if (line > -1) {
			string.append("Line ");
			string.append(line);
			string.append(": ");
		}

		string.append(message);

		return string.toString();
	}

	@Override
	public int compareTo(DataFileMessage o) {
		int result = (line == -1 ? 0 : line - o.line);

		if (result == 0) {
			result = message.compareTo(o.message);
		}

		return result;
	}
}
