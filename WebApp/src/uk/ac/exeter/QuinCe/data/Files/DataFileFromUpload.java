package uk.ac.exeter.QuinCe.data.Files;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.lang3.NotImplementedException;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.files.UploadedDataFile;

public class DataFileFromUpload extends DataFile {

  private UploadedDataFile source;

  public DataFileFromUpload(FileDefinition fileDefinition, String filename,
    UploadedDataFile source) throws MissingParamException, DataFileException {

    super(fileDefinition, filename);
    this.source = source;
    validate();
  }

  @Override
  public byte[] getBytes() throws IOException {
    // This is not implemented (nor needed) for files from uploads.
    throw new NotImplementedException();
  }

  @Override
  protected void loadAction() throws DataFileException {
    contents = Arrays.asList(source.getLines());
  }

}
