package uk.ac.exeter.QuinCe.data.Files;

import java.io.IOException;
import java.util.List;

/**
 * Class to hold the contents of a {@link DataFile}.
 *
 * <p>
 * Only one instance of this class can have its contents loaded into memory at
 * any given time. Calling {@link #get()} on an instance whose contents are not
 * loaded will cause that instance's contents to be loaded, and the previously
 * loaded contents will be discarded.
 * </p>
 */
public abstract class FileContents {

  /**
   * The instance whose contents are currently loaded.
   */
  private static FileContents loadedContents = null;

  /**
   * The file contents split into lines.
   */
  protected List<String> contents = null;

  /**
   * Get the file contents.
   *
   * <p>
   * If the contents aren't yet loaded, they will be.
   * </p>
   */
  protected List<String> get() throws DataFileException {
    load();
    return contents;
  }

  protected void load() throws DataFileException {
    boolean doLoad = false;

    if (null == loadedContents) {
      doLoad = true;
    } else if (loadedContents != this) {
      loadedContents.contents = null;
      loadedContents = null;
      doLoad = true;
    }

    if (doLoad) {
      loadAction();
      loadedContents = this;
    }
  }

  public int size() throws DataFileException {
    load();
    return contents.size();
  }

  public String get(int row) throws DataFileException {
    load();
    return contents.get(row);
  }

  /**
   * Load the contents of the file.
   *
   * @throws DataFileException
   *           If the file cannot be loaded.
   */
  protected abstract void loadAction() throws DataFileException;

  /**
   * Return the raw bytes of the file.
   *
   * @return The file bytes.
   * @throws IOException
   *           If the file cannot be loaded.
   */
  public abstract byte[] getBytes() throws IOException;
}
