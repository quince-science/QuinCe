package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import java.util.ArrayList;
import java.util.List;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;

@SuppressWarnings("serial")
public class SensorTypeTreeNode extends DefaultTreeNode {

  protected static final String SENSOR_UNASSIGNED = "UNASSIGNED_SENSOR_TYPE";

  protected static final String SENSOR_ASSIGNED = "ASSIGNED_SENSOR_TYPE";

  private final SensorType sensorType;

  private final SensorAssignments sensorAssignments;

  protected SensorTypeTreeNode(TreeNode parent, SensorType sensorType,
    SensorAssignments sensorAssignments) {

    super(sensorType.getShortName(), parent);
    this.sensorType = sensorType;
    this.sensorAssignments = sensorAssignments;
  }

  @Override
  public String getType() {

    String result = "";

    try {
      result = sensorAssignments.isAssignmentRequired(sensorType)
        ? SENSOR_UNASSIGNED
        : SENSOR_ASSIGNED;
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
    }

    return result;
  }

  protected void removeAssignment(SensorAssignment assignment) {

    List<TreeNode> updatedChildren = new ArrayList<TreeNode>(
      getChildren().size());

    for (TreeNode child : getChildren()) {
      if (!assignment.equals(child.getData())) {
        updatedChildren.add(child);
      }
    }

    setChildren(updatedChildren);
  }
}
