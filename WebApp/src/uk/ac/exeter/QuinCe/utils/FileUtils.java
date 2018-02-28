package uk.ac.exeter.QuinCe.utils;

import java.io.File;

/**
 * Miscellaneous file utils
 * @author Steve Jones
 *
 */
public class FileUtils {

	/**
	 * Check that a file can be accessed
	 * @param file The file to be checked
	 * @return {@code true} if the file can be accessed; {@code if it cannot}
	 */
	public static boolean canAccessFile(File file) {
		boolean ok = true;

		if (!file.exists()) {
			ok = false;
		} else if (!file.isFile()) {
			ok = false;
		} else if (!file.canRead()) {
			ok = false;
		}

		return ok;
	}

}
