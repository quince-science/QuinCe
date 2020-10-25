package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

@SuppressWarnings("serial")
public class SensorTypeTreeNode extends DefaultTreeNode {

  protected static final String SENSOR_UNASSIGNED = "UNASSIGNED_SENSOR_TYPE";

  protected static final String SENSOR_ASSIGNED = "ASSIGNED_SENSOR_TYPE";

  private final Set<FileDefinitionBuilder> files;

  private final SensorType sensorType;

  private final SensorAssignments sensorAssignments;

  protected SensorTypeTreeNode(TreeNode parent,
    Set<FileDefinitionBuilder> files, SensorType sensorType,
    SensorAssignments sensorAssignments) {

    super(sensorType.getName(), parent);
    this.files = files;
    this.sensorType = sensorType;
    this.sensorAssignments = sensorAssignments;
  }

  @Override
  public String getType() {

    String result = "";

    try {
      if (sensorType.equals(SensorType.RUN_TYPE_SENSOR_TYPE)) {
        boolean required = false;

        for (FileDefinitionBuilder file : files) {
          if (sensorAssignments.runTypeRequired(file.getFileDescription())
            && file.getRunTypeColumn() == -1) {
            required = true;
          }
        }

        result = required ? SENSOR_UNASSIGNED : SENSOR_ASSIGNED;
      } else {
        result = sensorAssignments.isAssignmentRequired(sensorType)
          ? SENSOR_UNASSIGNED
          : SENSOR_ASSIGNED;
      }
    } catch (Exception e) {
      // Print the stack trace and continue
      e.printStackTrace();
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
