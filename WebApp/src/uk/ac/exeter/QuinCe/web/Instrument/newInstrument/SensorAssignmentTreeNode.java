package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import org.primefaces.model.DefaultTreeNode;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;

@SuppressWarnings("serial")
public class SensorAssignmentTreeNode extends DefaultTreeNode {

  private static final String ASSIGNMENT = "ASSIGNMENT";

  private SensorAssignment assignment;

  protected SensorAssignmentTreeNode(SensorTypeTreeNode parent,
    SensorAssignment assignment) {

    super(ASSIGNMENT, assignment.getTarget(), parent);
    this.assignment = assignment;
  }

  @Override
  public Object getData() {
    return assignment.getTarget();
  }

  protected String getTargetFile() {
    return assignment.getDataFile();
  }
}
