package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeColumnAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecificationException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignmentException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorConfigurationException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;
import uk.ac.exeter.QuinCe.web.ui.EditableTreeNode;

/**
 * Holds details of the assigned sensors in tree form for the
 * {@code assign_variables.xhtml} page.
 *
 * @author stevej
 *
 */
public class AssignmentsTree {

  private static final String DATETIME_UNFINISHED = "UNFINISHED_DATETIME";

  private static final String DATETIME_FINISHED = "FINISHED_DATETIME";

  private static final String DATETIME_UNASSIGNED = "UNASSIGNED_DATETIME";

  private static final String DATETIME_ASSIGNED = "ASSIGNED_DATETIME";

  private static final String DATETIME_ASSIGNMENT = "DATETIME_ASSIGNMENT";

  private final SensorAssignments assignments;

  private DefaultTreeNode root;

  private DefaultTreeNode dateTimeNode;

  private Map<String, EditableTreeNode> dateTimeNodes;

  private Map<SensorType, List<SensorTypeTreeNode>> sensorTypeNodes;

  protected AssignmentsTree(List<Variable> variables,
    SensorAssignments assignments) throws SensorConfigurationException,
    SensorTypeNotFoundException, SensorAssignmentException {

    root = new DefaultTreeNode("Root", null);
    this.assignments = assignments;
    sensorTypeNodes = new HashMap<SensorType, List<SensorTypeTreeNode>>();

    buildTree(variables);
  }

  private void buildTree(List<Variable> variables)
    throws SensorConfigurationException, SensorTypeNotFoundException,
    SensorAssignmentException {

    dateTimeNode = new DefaultTreeNode(VariableTreeNode.VAR_UNFINISHED,
      "Date/Time", root);
    dateTimeNode.setExpanded(true);
    dateTimeNodes = new TreeMap<String, EditableTreeNode>();

    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();

    for (Variable var : variables) {
      TreeNode varNode = new VariableTreeNode(root, var, assignments);

      for (SensorType sensorType : sensorConfig.getSensorTypes(var.getId(),
        true, true, true)) {

        addSensorTypeNode(sensorType,
          new SensorTypeTreeNode(varNode, sensorType, assignments));
      }
    }

    TreeNode diagnosticsNode = new DefaultTreeNode(
      VariableTreeNode.VAR_FINISHED, "Diagnostics", root);

    for (SensorType diagnosticType : sensorConfig.getDiagnosticSensorTypes()) {

      addSensorTypeNode(diagnosticType,
        new SensorTypeTreeNode(diagnosticsNode, diagnosticType, assignments));
    }

  }

  private void addSensorTypeNode(SensorType sensorType,
    SensorTypeTreeNode node) {

    if (!sensorTypeNodes.containsKey(sensorType)) {
      sensorTypeNodes.put(sensorType, new ArrayList<SensorTypeTreeNode>());
    }

    sensorTypeNodes.get(sensorType).add(node);

  }

  protected TreeNode getRoot() {
    return root;
  }

  protected void addAssignment(SensorAssignment assignment) {

    for (SensorTypeTreeNode sensorTypeNode : sensorTypeNodes
      .get(assignment.getSensorType())) {

      new SensorAssignmentTreeNode(sensorTypeNode, assignment);
      sensorTypeNode.setExpanded(true);
    }
  }

  protected void removeFileAssignmentNodes(String fileName) {

    // Remove the date/time nodes for the file
    List<TreeNode> filteredDateTimeFileNodes = new ArrayList<TreeNode>(
      dateTimeNode.getChildCount() - 1);

    for (TreeNode dateTimeFileNode : dateTimeNode.getChildren()) {
      if (!dateTimeFileNode.getData().equals(fileName)) {
        filteredDateTimeFileNodes.add(dateTimeFileNode);
      }
    }

    dateTimeNode.setChildren(filteredDateTimeFileNodes);
    dateTimeNodes.remove(fileName);

    // Remove any sensor type nodes assigned from the file
    for (List<SensorTypeTreeNode> sensorTypeNodes : sensorTypeNodes.values()) {
      for (SensorTypeTreeNode sensorTypeNode : sensorTypeNodes) {

        @SuppressWarnings("unchecked")
        List<SensorAssignmentTreeNode> assignmentNodes = (List<SensorAssignmentTreeNode>) (Object) sensorTypeNode
          .getChildren();

        List<TreeNode> filteredChildren = new ArrayList<TreeNode>(
          assignmentNodes.size());

        for (SensorAssignmentTreeNode node : assignmentNodes) {
          if (!node.getTargetFile().equals(fileName)) {
            filteredChildren.add(node);
          }
        }

        sensorTypeNode.setChildren(filteredChildren);
      }
    }
  }

  protected void removeAssignment(SensorAssignment assignment) {
    List<SensorTypeTreeNode> typeNodes = sensorTypeNodes
      .get(assignment.getSensorType());
    for (SensorTypeTreeNode node : typeNodes) {
      node.removeAssignment(assignment);
    }
  }

  protected void addFile(FileDefinitionBuilder file)
    throws DateTimeSpecificationException {

    makeDateTimeNode(file, true);
  }

  private void makeDateTimeNode(FileDefinitionBuilder file, boolean expanded)
    throws DateTimeSpecificationException {
    EditableTreeNode fileDateTimeNode = new EditableTreeNode(
      DATETIME_UNFINISHED, file.getFileDescription(), dateTimeNode);
    fileDateTimeNode.setExpanded(true);

    dateTimeNodes.put(file.getFileDescription(), fileDateTimeNode);
    setDateTimeAssignment(file);
  }

  protected void setDateTimeAssignment(FileDefinitionBuilder file)
    throws DateTimeSpecificationException {

    DateTimeSpecification dateTimeSpec = file.getDateTimeSpecification();

    DefaultTreeNode parent = (DefaultTreeNode) dateTimeNodes
      .get(file.getFileDescription());

    // Remove all existing nodes
    parent.setChildren(new ArrayList<TreeNode>());

    // Build set of nodes for date/time
    if (dateTimeSpec.assignmentComplete()) {
      parent.setType(DATETIME_FINISHED);
    }

    for (Map.Entry<String, Boolean> entry : dateTimeSpec
      .getAssignedAndRequiredEntries().entrySet()) {

      if (entry.getValue()) {
        new DefaultTreeNode(DATETIME_UNASSIGNED, entry.getKey(), parent);
      } else {
        TreeNode child = new DefaultTreeNode(DATETIME_ASSIGNED, entry.getKey(),
          parent);
        child.setExpanded(true);

        DateTimeColumnAssignment assignment = dateTimeSpec.getAssignment(
          DateTimeSpecification.getAssignmentIndex(entry.getKey()));

        new DefaultTreeNode(DATETIME_ASSIGNMENT,
          file.getFileColumns().get(assignment.getColumn()).getName(), child);

      }
    }

    updateDateTimeNode();
  }

  private void updateDateTimeNode() {
    boolean dateTimeFinished = true;

    for (TreeNode node : dateTimeNodes.values()) {
      if (node.getType().equals(DATETIME_UNFINISHED)) {
        dateTimeFinished = false;
        break;
      }
    }

    dateTimeNode.setType(dateTimeFinished ? VariableTreeNode.VAR_FINISHED
      : VariableTreeNode.VAR_UNFINISHED);
  }

  protected void renameFile(String oldName, FileDefinitionBuilder renamedFile)
    throws DateTimeSpecificationException {

    // We only need to update the date/time assignments;
    // sensor assignments are updated in the SensorAssignment objects

    // Get expanded status from old file
    EditableTreeNode node = dateTimeNodes.remove(oldName);
    node.setData(renamedFile.getFileDescription());
    dateTimeNodes.put(renamedFile.getFileDescription(), node);
  }
}
