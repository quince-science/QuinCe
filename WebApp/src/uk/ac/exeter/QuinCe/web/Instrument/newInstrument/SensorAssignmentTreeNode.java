package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import org.primefaces.model.DefaultTreeNode;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;

@SuppressWarnings("serial")
public class SensorAssignmentTreeNode extends DefaultTreeNode {

  private static final String ASSIGNMENT = "ASSIGNMENT";

  protected SensorAssignmentTreeNode(SensorTypeTreeNode parent,
    SensorAssignment assignment) {

    super(ASSIGNMENT, assignment, parent);
  }

  protected String getTargetFile() {
    return ((SensorAssignment) getData()).getDataFile();
  }
}
