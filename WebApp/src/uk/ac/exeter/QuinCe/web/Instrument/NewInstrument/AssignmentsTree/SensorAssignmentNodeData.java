package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.AssignmentsTree;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;

public class SensorAssignmentNodeData extends AssignmentsTreeNodeData {

  private final SensorAssignment assignment;

  protected SensorAssignmentNodeData(SensorAssignment assignment) {
    this.assignment = assignment;
  }

  protected SensorAssignment getAssignment() {
    return assignment;
  }

  public long getSensorTypeId() {
    return assignment.getSensorType().getId();
  }

  public String getFile() {
    return assignment.getDataFile();
  }

  public int getColumn() {
    return assignment.getColumn();
  }

  @Override
  public String getLabel() {
    return assignment.getDataFile() + ": "
      + assignment.getColumnHeading().getShortName();
  }

  @Override
  public String getId() {
    return assignment.getSensorType() + "_" + assignment.getDataFile() + "_"
      + assignment.getColumnHeading().getShortName();
  }
}
