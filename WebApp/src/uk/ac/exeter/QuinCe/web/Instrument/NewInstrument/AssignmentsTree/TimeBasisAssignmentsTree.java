package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.AssignmentsTree;

import java.util.List;

import org.primefaces.model.DefaultTreeNode;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecificationException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorConfigurationException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.FileDefinitionBuilder;
import uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.NewInstrumentFileSet;

/**
 * {@link SensorAssignments} tree for instruments with a Time basis.
 */
public class TimeBasisAssignmentsTree extends AssignmentsTree {

  /**
   * Indicates whether or not position information needs to be assigned for the
   * {@link uk.ac.exeter.QuinCe.data.Instrument.Instrument}.
   *
   * <p>
   * Note that this indicates whether positions are needed <i>at all</i>, and
   * not whether required position details have been assigned.
   * </p>
   */
  private final boolean needsPosition;

  protected TimeBasisAssignmentsTree(NewInstrumentFileSet files,
    List<Variable> variables, SensorAssignments assignments,
    boolean needsPosition)
    throws SensorConfigurationException, SensorTypeNotFoundException {

    super(files, variables, assignments);
    this.needsPosition = needsPosition;
  }

  @Override
  public DefaultTreeNode<AssignmentsTreeNodeData> getRoot()
    throws AssignmentsTreeException {

    DefaultTreeNode<AssignmentsTreeNodeData> root = new DefaultTreeNode<AssignmentsTreeNodeData>(
      new StringNodeData("Root"), null);

    try {

      buildDateTimeNode(root);

      if (needsPosition) {
        buildPositionNodes(root);
      }

      buildSensorTypeNodes(root);
    } catch (Exception e) {
      throw new AssignmentsTreeException(e);
    }

    return root;
  }

  private void buildDateTimeNode(
    DefaultTreeNode<AssignmentsTreeNodeData> parent)
    throws DateTimeSpecificationException {

    // See if any date/time specs still need assignment
    String nodeType = files.stream().anyMatch(
      f -> !f.getDateTimeSpecification().assignmentComplete()) ? VAR_UNFINISHED
        : VAR_FINISHED;

    AssignmentsTreeNode<AssignmentsTreeNodeData> main = new AssignmentsTreeNode<AssignmentsTreeNodeData>(
      this, nodeType, new StringNodeData("Date/Time"), parent);

    if (files.size() == 1) {
      buildDateTimeNodes((FileDefinitionBuilder) files.get(0), main);
    } else {
      for (FileDefinition f : files) {
        FileDefinitionBuilder file = (FileDefinitionBuilder) f;
        String fileState = file.getDateTimeSpecification().assignmentComplete()
          ? DATETIME_FINISHED
          : DATETIME_UNFINISHED;

        AssignmentsTreeNode<AssignmentsTreeNodeData> fileNode = new AssignmentsTreeNode<AssignmentsTreeNodeData>(
          this, fileState, new DateTimeFileNodeData(file), main);

        buildDateTimeNodes((FileDefinitionBuilder) file, fileNode);
      }
    }
  }
}
