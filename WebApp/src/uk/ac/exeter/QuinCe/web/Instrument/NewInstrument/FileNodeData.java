package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument;

public class FileNodeData extends AssignmentsTreeNodeData {

  private final FileDefinitionBuilder file;

  protected FileNodeData(FileDefinitionBuilder file) {
    this.file = file;
  }

  protected String getFileDescription() {
    return file.getFileDescription();
  }

  @Override
  public String getLabel() {
    return file.getFileDescription();
  }
}
