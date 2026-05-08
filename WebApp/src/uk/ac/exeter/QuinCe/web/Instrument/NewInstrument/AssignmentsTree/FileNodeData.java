package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.AssignmentsTree;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.FileDefinitionBuilder;

public class FileNodeData extends AssignmentsTreeNodeData {

  protected final FileDefinitionBuilder file;

  protected FileNodeData(FileDefinitionBuilder file) {
    this.file = file;
  }

  public FileDefinition getFile() {
    return file;
  }

  protected String getFileDescription() {
    return file.getFileDescription();
  }

  @Override
  public String getLabel() {
    return file.getFileDescription();
  }

  @Override
  public String getId() {
    return "FILE_" + file.getFileDescription();
  }
}
