package uk.ac.exeter.QuinCe.data.Files;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;

public class FileStoreDataFile extends DataFile {

  private String fileStore;

  public FileStoreDataFile(String fileStore, long id, Instrument instrument,
    FileDefinition fileDefinition, String filename, LocalDateTime startDate,
    LocalDateTime endDate, int recordCount, Properties properties) {

    super(id, instrument, fileDefinition, filename, startDate, endDate,
      recordCount, properties);
    this.fileStore = fileStore;
  }

  @Override
  protected void loadAction() throws DataFileException {

    try {
      FileStore.loadFileContents(fileStore, this);
    } catch (IOException e) {
      throw new DataFileException(getDatabaseId(), -1, e);
    }
  }

  @Override
  public byte[] getBytes() throws IOException {
    return FileStore.getBytes(fileStore, this);
  }

}
