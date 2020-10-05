package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignmentException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorConfigurationException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Holds details of the assigned sensors in tree form for the
 * {@code assign_variables.xhtml} page.
 * 
 * @author stevej
 *
 */
public class AssignmentsTree {

  private TreeNode root;

  private final SensorAssignments assignments;

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

    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();

    for (Variable var : variables) {
      TreeNode varNode = new VariableTreeNode(root, var, assignments);

      for (SensorType sensorType : sensorConfig.getSensorTypes(var.getId(),
        true, true)) {

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

  protected void removeAssignmentNodes(String fileName) {

    for (List<SensorTypeTreeNode> sensorTypeNodes : sensorTypeNodes.values()) {
      for (SensorTypeTreeNode sensorTypeNode : sensorTypeNodes) {

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
}
