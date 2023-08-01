package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentFileSet;

/**
 * Class to define a set of InstrumentFiles when creating an instrument
 *
 * @author Steve Jones
 *
 */
@SuppressWarnings("serial")
public class NewInstrumentFileSet extends InstrumentFileSet {

  /**
   * Simple constructor to create an empty set
   */
  protected NewInstrumentFileSet() {
    super();
  }

  @Override
  public boolean add(FileDefinition file) {
    boolean result = false;

    if (file instanceof FileDefinitionBuilder) {
      result = super.add(file);
    }

    return result;
  }

  @Override
  public FileDefinitionBuilder get(int index) {
    return (FileDefinitionBuilder) super.get(index);
  }

  protected FileDefinitionBuilder getByDescription(String fileDescription) {
    FileDefinitionBuilder result = null;

    for (FileDefinition file : this) {
      if (file.getFileDescription().equals(fileDescription)) {
        result = (FileDefinitionBuilder) file;
      }
    }

    return result;
  }
}
