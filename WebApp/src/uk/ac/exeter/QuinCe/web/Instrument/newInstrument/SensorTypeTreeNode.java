package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

@SuppressWarnings("serial")
public class SensorTypeTreeNode extends DefaultTreeNode {

  private static final String SENSOR_UNASSIGNED = "UNASSIGNED_SENSOR_TYPE";

  private static final String SENSOR_ASSIGNED = "ASSIGNED_SENSOR_TYPE";

  private final SensorType sensorType;

  private final SensorAssignments sensorAssignments;

  protected SensorTypeTreeNode(TreeNode parent, SensorType sensorType,
    SensorAssignments sensorAssignments) {

    super(sensorType.getName(), parent);
    this.sensorType = sensorType;
    this.sensorAssignments = sensorAssignments;
  }

  @Override
  public String getType() {

    String result = "";

    try {
      return sensorAssignments.isAssignmentRequired(sensorType)
        ? SENSOR_UNASSIGNED
        : SENSOR_ASSIGNED;
    } catch (Exception e) {
      // Print the stack trace and continue
      e.printStackTrace();
    }

    return result;
  }

}
