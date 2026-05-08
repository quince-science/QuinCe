package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.AssignmentsTree;

import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeColumnAssignment;
import uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.FileDefinitionBuilder;

public class DateTimeAssignmentNodeData extends FileNodeData {

  private final DateTimeColumnAssignment assignment;

  protected DateTimeAssignmentNodeData(FileDefinitionBuilder file,
    DateTimeColumnAssignment assignment) {
    super(file);
    this.assignment = assignment;
  }

  public int getColumn() {
    return assignment.getColumn();
  }

  @Override
  public String getLabel() {
    return getFileDescription() + ": "
      + file.getColumnName(assignment.getColumn());
  }

  @Override
  public String getId() {
    return "DATETIME_" + file.getFileDescription() + "_" + getLabel();
  }
}
