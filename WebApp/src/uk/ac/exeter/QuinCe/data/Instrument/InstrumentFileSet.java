package uk.ac.exeter.QuinCe.data.Instrument;

import java.util.ArrayList;
import java.util.List;

/**
 * Methods to handle and process a set of file definitions for an instrument
 *
 * @author Steve Jones
 *
 */
@SuppressWarnings("serial")
public class InstrumentFileSet extends ArrayList<FileDefinition> {

  /**
   * Simple constructor to create an empty set
   */
  protected InstrumentFileSet() {
    super();
  }

  @Override
  public boolean add(FileDefinition file) {
    boolean result = false;

    int currentPosition = indexOf(file);

    if (currentPosition == -1) {
      result = super.add(file);
    } else {
      set(currentPosition, file);
      result = true;
    }

    return result;
  }

  /**
   * Determine whether or not the file set contains a file definition with the
   * specified description
   *
   * @param description
   *          The file description
   * @return {@code true} if a file with the specified description is present;
   *         {@code false} otherwise
   */
  public boolean containsFileDescription(String description) {
    boolean found = false;

    for (FileDefinition file : this) {
      if (file.getFileDescription().equalsIgnoreCase(description)) {
        found = true;
        break;
      }
    }

    return found;
  }

  /**
   * Retrieve the file definition with the specified description. If no
   * definition is found, the method returns {@code null}
   *
   * @param description
   *          The file description
   * @return The file definition
   */
  public FileDefinition get(String description) {
    FileDefinition result = null;

    for (FileDefinition file : this) {
      if (file.getFileDescription().equalsIgnoreCase(description)) {
        result = file;
        break;
      }
    }

    return result;
  }

  /**
   * Retrieve the file definition with the specified database ID. If no
   * definition is found, the method returns {@code null}
   *
   * @param definitionId
   *          The database ID
   * @return The file definition
   */
  public FileDefinition get(long definitionId) {
    FileDefinition result = null;

    for (FileDefinition file : this) {
      if (file.getDatabaseId() == definitionId) {
        result = file;
        break;
      }
    }

    return result;
  }

  public boolean remove(String name) {
    // Allow removal by file description
    int fileToRemove = -1;

    for (int i = 0; i < size(); i++) {
      if (get(i).getFileDescription().equalsIgnoreCase(name)) {
        fileToRemove = i;
        break;
      }
    }

    boolean result = false;
    if (fileToRemove > -1) {
      remove(fileToRemove);
      result = true;
    }

    return result;
  }

  /**
   * Find file definitions that match the layout of the supplied file
   * definition.
   *
   * @param fileToMatch
   *          The file definition to be matched
   * @return The matching file definitions
   */
  public List<FileDefinition> getMatchingFileDefinition(
    FileDefinition fileToMatch) {
    List<FileDefinition> result = new ArrayList<FileDefinition>();

    for (FileDefinition file : this) {
      if (file.matchesLayout(fileToMatch)) {
        result.add(file);
        break;
      }
    }

    return result;
  }
}
