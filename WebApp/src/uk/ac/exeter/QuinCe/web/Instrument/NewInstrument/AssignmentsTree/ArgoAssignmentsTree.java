package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.AssignmentsTree;

import java.util.List;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import uk.ac.exeter.QuinCe.data.Dataset.Coordinate;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.LatitudeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.LongitudeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorConfigurationException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.NewInstrumentFileSet;

/**
 * {@link AssignmentsTree} for Argo-based {@link Instrument}s (i.e. those where
 * {@link Instrument#basis} {@code ==} {@link Instrument#BASIS_ARGO}).
 */
public class ArgoAssignmentsTree extends AssignmentsTree {

  /**
   * Initialise and construct the tree.
   *
   * @param files
   *          The uploaded sample files.
   * @param variables
   *          The {@link Variable}s measured by the
   *          {@link uk.ac.exeter.QuinCe.data.Instrument.Instrument}.
   * @param assignments
   *          The object holding the assignments columns columns to times,
   *          positions, {@link SensorType}s etc.
   * @throws SensorConfigurationException
   *           If the system's sensor configuration is invalid.
   * @throws SensorTypeNotFoundException
   *           If any referenced {@link SensorType} does not exist.
   */
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

  /**
   * Build the {@code Coordinate} node for the tree.
   *
   * <p>
   * For Argo data, a value or measurement's {@link Coordinate} is based on a
   * combination of multiple values:
   * </p>
   * <ul>
   * <li>Cycle Number</li>
   * <li>Profile</li>
   * <li>Direction</li>
   * <li>Level</li>
   * <li>Pressure (Depth)</li>
   * </ul>
   *
   * <p>
   * All these values must be assigned for valid Argo data.
   * </p>
   *
   * <p>
   * Other coordinate-style data provided in Argo data, but not required for
   * value identification, are also assignable in the {@code Coordinate} node.
   * These are usually present once per profile, and not with every data point.
   * They includes:
   * </p>
   * <ul>
   * <li>Date/Time</li>
   * <li>Longitude/Latitude</li>
   * </ul>
   *
   * <p>
   * We also capture the name of the source Argo file for data prevenance and to
   * help with integration of QuinCe's output back into the Argo data flow.
   * </p>
   *
   * @param parent
   *          The parent node to which the {@code Coordinate} node is to be
   *          added.
   * @throws Exception
   *           If the system configuration prevents the node being created. This
   *           should not happen under any circumstances.
   */
  private void buildCoordinateNode(
    DefaultTreeNode<AssignmentsTreeNodeData> parent) throws Exception {

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

  /**
   * Determine whether or not the required {@link Coordinate} entries have been
   * assigned.
   *
   * @return {@code true} if the required values have been assigned;
   *         {@code false} if not.
   * @throws SensorTypeNotFoundException
   *           If the expected {@link SensorType}s are not defined.
   * @see #buildCoordinateNode(DefaultTreeNode)
   */
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
