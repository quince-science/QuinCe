package uk.ac.exeter.QuinCe.data.Files;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import uk.ac.exeter.QuinCe.utils.StringUtils;

public class FileStoreFileContents extends FileContents {

  private String fileStore;

  private long fileDefinitionId;

  private long fileId;

  public FileStoreFileContents(String fileStore, long fileDefinitionId,
    long fileId) {
    this.fileStore = fileStore;
    this.fileDefinitionId = fileDefinitionId;
    this.fileId = fileId;
  }

  @Override
  protected void loadAction() throws DataFileException {

    try {
      String fileContent = new String(getBytes());
      contents = new ArrayList<String>(
        Arrays.asList(fileContent.split("[\\r\\n]+")));

      StringUtils.removeBlankTailLines(contents);
    } catch (IOException e) {
      throw new DataFileException(fileId, -1, e);
    }
  }

  @Override
  public byte[] getBytes() throws IOException {
    return FileStore.getBytes(fileStore, fileDefinitionId, fileId);
  }
}
