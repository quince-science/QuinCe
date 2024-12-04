package uk.ac.exeter.QuinCe.data.Files;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * An instance of the {@link DataFile} class for testing.
 */
public class TestDataFile extends DataFile {

  public TestDataFile(long id, FileDefinition fileDefinition, String filename,
    LocalDateTime start, LocalDateTime end)
    throws MissingParamException, DataFileException {
    super(id, fileDefinition, filename, start, end, 0, new Properties());
    setTimeOffset(0);
  }

  @Override
  public byte[] getBytes() throws IOException {
    return null;
  }

  @Override
  protected void loadAction() throws DataFileException {
    // noop
  }
}
