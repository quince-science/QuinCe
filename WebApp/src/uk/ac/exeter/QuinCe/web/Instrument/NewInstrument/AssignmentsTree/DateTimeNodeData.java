package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.AssignmentsTree;

import uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.FileDefinitionBuilder;

public class DateTimeNodeData extends DateTimeFileNodeData {

  private String name;

  public DateTimeNodeData(FileDefinitionBuilder file, String name) {
    super(file);
    this.name = name;
  }

  @Override
  public String getLabel() {
    return name;
  }

  @Override
  public String getId() {
    return "DATETIME_" + getFileDescription() + "_" + name;
  }

}
