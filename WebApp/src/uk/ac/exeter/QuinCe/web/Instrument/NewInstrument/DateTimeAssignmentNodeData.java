package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument;

import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeColumnAssignment;

public class DateTimeAssignmentNodeData extends AssignmentsTreeNodeData {

  private FileDefinitionBuilder file;

  private final DateTimeColumnAssignment assignment;

  protected DateTimeAssignmentNodeData(FileDefinitionBuilder file,
    DateTimeColumnAssignment assignment) {
    this.file = file;
    this.assignment = assignment;
  }

  public String getFile() {
    return file.getFileDescription();
  }

  public int getColumn() {
    return assignment.getColumn();
  }

  @Override
  public String getLabel() {
    return file.getFileDescription() + ": "
      + file.getColumnName(assignment.getColumn());
  }
}
