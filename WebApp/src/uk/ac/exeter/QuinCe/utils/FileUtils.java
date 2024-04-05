package uk.ac.exeter.QuinCe.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Miscellaneous file utils
 */
public class FileUtils {

  /**
   * Check that a file can be accessed
   *
   * @param file
   *          The file to be checked
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

  public static boolean isDirectoryEmpty(File directory) throws IOException {

    Path dir = directory.toPath();

    if (!Files.isDirectory(dir)) {
      throw new IOException("Path is not a directory");
    }

    try (Stream<Path> entries = Files.list(dir)) {
      return !entries.findFirst().isPresent();
    }
  }
}
