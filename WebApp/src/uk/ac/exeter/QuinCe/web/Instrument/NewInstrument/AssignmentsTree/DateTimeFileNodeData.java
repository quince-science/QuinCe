package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.AssignmentsTree;

import uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.FileDefinitionBuilder;

public class DateTimeFileNodeData extends FileNodeData {

  protected DateTimeFileNodeData(FileDefinitionBuilder file) {
    super(file);
  }

  @Override
  public String getId() {
    return "DATETIME_FILE_" + getFileDescription();
  }
}
