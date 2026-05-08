package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.AssignmentsTree;

import java.util.List;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecificationException;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.LatitudeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.LongitudeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignmentException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorConfigurationException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.NewInstrumentFileSet;

public class ArgoAssignmentsTree extends AssignmentsTree {

  protected ArgoAssignmentsTree(NewInstrumentFileSet files,
    List<Variable> variables, SensorAssignments assignments)
    throws SensorConfigurationException, SensorTypeNotFoundException {

    super(files, variables, assignments);
  }

  @Override
  public TreeNode<AssignmentsTreeNodeData> getRoot()
    throws AssignmentsTreeException {

    DefaultTreeNode<AssignmentsTreeNodeData> root = new DefaultTreeNode<AssignmentsTreeNodeData>(
      new StringNodeData("Root"), null);

    try {
      buildCoordinateNode(root);
      buildSensorTypeNodes(root);
    } catch (Exception e) {
      throw new AssignmentsTreeException(e);
    }

    return root;
  }

  private void buildCoordinateNode(
    DefaultTreeNode<AssignmentsTreeNodeData> parent)
    throws SensorTypeNotFoundException, SensorAssignmentException,
    SensorConfigurationException, DateTimeSpecificationException {

    AssignmentsTreeNode<AssignmentsTreeNodeData> coordinateNode = new AssignmentsTreeNode<AssignmentsTreeNodeData>(
      this, coordinateAssigned() ? VAR_FINISHED : VAR_UNFINISHED,
      new StringNodeData("Profile Info"), parent);

    makeSensorTypeNode("Cycle Number", coordinateNode);
    makeSensorTypeNode("Profile", coordinateNode);
    makeSensorTypeNode("Direction", coordinateNode);
    makeSensorTypeNode("Level", coordinateNode);
    makeSensorTypeNode("Pressure (Depth)", coordinateNode);

    makeSingleDateTimeNode(files.get(0), coordinateNode,
      DateTimeSpecification.UNIX);

    makePositionNodes("Longitude", coordinateNode,
      LongitudeSpecification.FORMAT_MINUS180_180);
    makePositionNodes("Latitude", coordinateNode,
      LatitudeSpecification.FORMAT_MINUS90_90);

    makeSensorTypeNode("Source File", coordinateNode);
  }

  private boolean coordinateAssigned() throws SensorTypeNotFoundException {
    boolean assigned = true;

    if (!files.get(0).getDateTimeSpecification().assignmentComplete()) {
      assigned = false;
    } else if (!files.get(0).getLatitudeSpecification()
      .specificationComplete()) {
      assigned = false;
    } else if (!files.get(0).getLongitudeSpecification()
      .specificationComplete()) {
      assigned = false;
    } else if (!assignments.isAssigned("Cycle Number", "Profile", "Direction",
      "Level", "Pressure (Depth)", "Source File")) {
      assigned = false;
    }

    return assigned;
  }
}
