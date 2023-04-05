package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeColumnAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecificationException;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.PositionSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignmentException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorConfigurationException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
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

  private static final String DATETIME_UNFINISHED = "UNFINISHED_DATETIME";

  private static final String DATETIME_FINISHED = "FINISHED_DATETIME";

  private static final String DATETIME_UNASSIGNED = "UNASSIGNED_DATETIME";

  private static final String DATETIME_ASSIGNED = "ASSIGNED_DATETIME";

  private static final String DATETIME_ASSIGNMENT = "DATETIME_ASSIGNMENT";

  private static final String LONGITUDE_UNASSIGNED = "UNASSIGNED_LONGITUDE";

  private static final String LONGITUDE_ASSIGNED = "ASSIGNED_LONGITUDE";

  private static final String LONGITUDE_ASSIGNMENT = "LONGITUDE_ASSIGNMENT";

  private static final String LATITUDE_UNASSIGNED = "UNASSIGNED_LATITUDE";

  private static final String LATITUDE_ASSIGNED = "ASSIGNED_LATITUDE";

  private static final String LATITUDE_ASSIGNMENT = "LATITUDE_ASSIGNMENT";

  private static final String HEMISPHERE_UNASSIGNED = "UNASSIGNED_HEMISPHERE";

  private static final String HEMISPHERE_ASSIGNED = "ASSIGNED_HEMISPHERE";

  private static final String HEMISPHERE_ASSIGNMENT = "HEMISPHERE_ASSIGNMENT";

  protected static final String VAR_UNFINISHED = "UNFINISHED_VARIABLE";

  protected static final String VAR_FINISHED = "FINISHED_VARIABLE";

  private final SensorAssignments assignments;

  private final boolean needsPosition;

  private DefaultTreeNode<AssignmentsTreeNodeData> root;

  private DefaultTreeNode<AssignmentsTreeNodeData> dateTimeNode;

  private Map<String, DefaultTreeNode<AssignmentsTreeNodeData>> dateTimeNodes;

  private DefaultTreeNode<AssignmentsTreeNodeData> positionNode;

  private Map<SensorType, List<DefaultTreeNode<AssignmentsTreeNodeData>>> sensorTypeNodes;

  private TreeSet<FileDefinitionBuilder> files;

  protected AssignmentsTree(List<Variable> variables,
    SensorAssignments assignments, boolean needsPosition)
    throws SensorConfigurationException, SensorAssignmentException {

    root = new DefaultTreeNode<AssignmentsTreeNodeData>(
      new AssignmentsTreeNodeData("Root"), null);
    this.assignments = assignments;
    this.needsPosition = needsPosition;
    sensorTypeNodes = new HashMap<SensorType, List<DefaultTreeNode<AssignmentsTreeNodeData>>>();
    files = new TreeSet<FileDefinitionBuilder>();

    buildTree(variables);
  }

  private void buildTree(List<Variable> variables)
    throws SensorConfigurationException, SensorAssignmentException {

    dateTimeNode = new DefaultTreeNode<AssignmentsTreeNodeData>(VAR_UNFINISHED,
      new AssignmentsTreeNodeData("Date/Time"), root);
    dateTimeNode.setExpanded(true);
    dateTimeNodes = new TreeMap<String, DefaultTreeNode<AssignmentsTreeNodeData>>();

    if (needsPosition) {
      positionNode = new DefaultTreeNode<AssignmentsTreeNodeData>(
        VAR_UNFINISHED, new AssignmentsTreeNodeData("Position"), root);
      positionNode.setExpanded(true);
      updatePositionNodes(null);
    }

    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();

    for (Variable var : variables) {
      DefaultTreeNode<AssignmentsTreeNodeData> varNode = new DefaultTreeNode<AssignmentsTreeNodeData>(
        new AssignmentsTreeNodeData(var, assignments), root);

      for (SensorType sensorType : sensorConfig.getSensorTypes(var.getId(),
        true, true, true)) {

        addSensorTypeNode(sensorType,
          new DefaultTreeNode<AssignmentsTreeNodeData>(
            new AssignmentsTreeNodeData(sensorType, assignments), varNode));
      }
    }

    DefaultTreeNode<AssignmentsTreeNodeData> diagnosticsNode = new DefaultTreeNode<AssignmentsTreeNodeData>(
      VAR_FINISHED, new AssignmentsTreeNodeData("Diagnostics"), root);

    for (SensorType diagnosticType : sensorConfig.getDiagnosticSensorTypes()) {
      addSensorTypeNode(diagnosticType,
        new DefaultTreeNode<AssignmentsTreeNodeData>(
          new AssignmentsTreeNodeData(diagnosticType, assignments),
          diagnosticsNode));
    }
  }

  private void addSensorTypeNode(SensorType sensorType,
    DefaultTreeNode<AssignmentsTreeNodeData> node) {

    if (!sensorTypeNodes.containsKey(sensorType)) {
      sensorTypeNodes.put(sensorType,
        new ArrayList<DefaultTreeNode<AssignmentsTreeNodeData>>());
    }

    sensorTypeNodes.get(sensorType).add(node);

  }

  protected DefaultTreeNode<AssignmentsTreeNodeData> getRoot() {
    return root;
  }

  protected void addAssignment(SensorAssignment assignment) {

    for (DefaultTreeNode<AssignmentsTreeNodeData> sensorTypeNode : sensorTypeNodes
      .get(assignment.getSensorType())) {

      new DefaultTreeNode<AssignmentsTreeNodeData>(
        new AssignmentsTreeNodeData(assignment), sensorTypeNode);
    }
  }

  protected void removeFile(String fileName) {

    dateTimeNodes.remove(fileName);

    Iterator<TreeNode<AssignmentsTreeNodeData>> nodeSearch = dateTimeNode
      .getChildren().iterator();
    while (nodeSearch.hasNext()) {
      TreeNode<AssignmentsTreeNodeData> node = nodeSearch.next();
      if (node.getData().getAssignment().getDataFile().equals(fileName)) {
        nodeSearch.remove();
      }
    }

    // Remove any sensor type nodes assigned from the file

    // Loop through all nodes for each SensorType
    for (List<DefaultTreeNode<AssignmentsTreeNodeData>> sensorTypeNodes : sensorTypeNodes
      .values()) {
      for (DefaultTreeNode<AssignmentsTreeNodeData> sensorTypeNode : sensorTypeNodes) {

        // Get the children of the SensorType node
        List<TreeNode<AssignmentsTreeNodeData>> assignmentNodes = sensorTypeNode
          .getChildren();

        List<TreeNode<AssignmentsTreeNodeData>> keptChildren = new ArrayList<TreeNode<AssignmentsTreeNodeData>>(
          assignmentNodes.size());

        for (TreeNode<AssignmentsTreeNodeData> node : assignmentNodes) {

          // Quietly ignore nodes of the wrong type. They shouldn't be there
          // anyway.
          if (node.getData()
            .getDataType() == AssignmentsTreeNodeData.SENSORASSIGNMENT_TYPE) {
            if (node.getData().getAssignment().getDataFile().equals(fileName)) {
              keptChildren.add(node);
            }
          }
        }

        sensorTypeNode.setChildren(keptChildren);
      }
    }

    Iterator<FileDefinitionBuilder> fileSearch = files.iterator();
    while (fileSearch.hasNext()) {
      FileDefinitionBuilder file = fileSearch.next();
      if (file.getFileDescription().equals(fileName)) {
        fileSearch.remove();
        break;
      }
    }

    updatePositionNodes(null);
  }

  protected void removeAssignment(SensorAssignment assignment) {
    List<DefaultTreeNode<AssignmentsTreeNodeData>> typeNodes = sensorTypeNodes
      .get(assignment.getSensorType());

    for (DefaultTreeNode<AssignmentsTreeNodeData> node : typeNodes) {

      Iterator<TreeNode<AssignmentsTreeNodeData>> nodeSearch = node
        .getChildren().iterator();

      while (nodeSearch.hasNext()) {
        TreeNode<AssignmentsTreeNodeData> assignmentNode = nodeSearch.next();
        if (assignmentNode.getData()
          .getDataType() == AssignmentsTreeNodeData.SENSORASSIGNMENT_TYPE) {
          if (assignmentNode.getData().getAssignment().equals(assignment)) {
            nodeSearch.remove();
          }
        }
      }
    }
  }

  protected void addFile(FileDefinitionBuilder file)
    throws DateTimeSpecificationException {

    makeDateTimeNode(file, true);
    files.add(file);
    updatePositionNodes(null);
  }

  private void makeDateTimeNode(FileDefinitionBuilder file, boolean expanded)
    throws DateTimeSpecificationException {

    DefaultTreeNode<AssignmentsTreeNodeData> fileDateTimeNode = new DefaultTreeNode<AssignmentsTreeNodeData>(
      DATETIME_UNFINISHED, new AssignmentsTreeNodeData(file), dateTimeNode);
    fileDateTimeNode.setExpanded(true);

    dateTimeNodes.put(file.getFileDescription(), fileDateTimeNode);
    setDateTimeAssignment(file);
  }

  protected void setDateTimeAssignment(FileDefinitionBuilder file)
    throws DateTimeSpecificationException {

    DateTimeSpecification dateTimeSpec = file.getDateTimeSpecification();

    DefaultTreeNode<AssignmentsTreeNodeData> parent = dateTimeNodes
      .get(file.getFileDescription());

    // Remove all existing nodes
    parent.setChildren(new ArrayList<TreeNode<AssignmentsTreeNodeData>>());

    // Build set of nodes for date/time
    parent.setType(dateTimeSpec.assignmentComplete() ? DATETIME_FINISHED
      : DATETIME_UNFINISHED);

    for (Map.Entry<String, Boolean> entry : dateTimeSpec
      .getAssignedAndRequiredEntries().entrySet()) {

      if (entry.getValue()) {
        new DefaultTreeNode<AssignmentsTreeNodeData>(DATETIME_UNASSIGNED,
          new AssignmentsTreeNodeData(entry.getKey()), parent);
      } else {
        DefaultTreeNode<AssignmentsTreeNodeData> child = new DefaultTreeNode<AssignmentsTreeNodeData>(
          DATETIME_ASSIGNED, new AssignmentsTreeNodeData(entry.getKey()),
          parent);
        child.setExpanded(true);

        DateTimeColumnAssignment assignment = dateTimeSpec.getAssignment(
          DateTimeSpecification.getAssignmentIndex(entry.getKey()));

        new DefaultTreeNode<AssignmentsTreeNodeData>(DATETIME_ASSIGNMENT,
          new AssignmentsTreeNodeData(file, assignment), child);
      }
    }

    boolean dateTimeFinished = true;
    for (DefaultTreeNode<AssignmentsTreeNodeData> node : dateTimeNodes
      .values()) {
      if (node.getType().equals(DATETIME_UNFINISHED)) {
        dateTimeFinished = false;
        break;
      }
    }

    dateTimeNode.setType(dateTimeFinished ? VAR_FINISHED : VAR_UNFINISHED);
  }

  protected void renameFile(String oldName, FileDefinitionBuilder renamedFile)
    throws DateTimeSpecificationException {

    // We only need to update the date/time assignments;
    // sensor assignments are updated in the SensorAssignment objects
    DefaultTreeNode<AssignmentsTreeNodeData> node = dateTimeNodes
      .remove(oldName);
    node.setData(new AssignmentsTreeNodeData(renamedFile));
    dateTimeNodes.put(renamedFile.getFileDescription(), node);
  }

  protected void updatePositionNodes(String expand) {
    if (null != positionNode) {
      positionNode
        .setChildren(new ArrayList<TreeNode<AssignmentsTreeNodeData>>());

      DefaultTreeNode<AssignmentsTreeNodeData> longitudeNode = null;
      DefaultTreeNode<AssignmentsTreeNodeData> latitudeNode = null;

      longitudeNode = makePositionNodes("Longitude", longitudeNode,
        null != expand && expand.equals("Longitude"));
      latitudeNode = makePositionNodes("Latitude", latitudeNode,
        null != expand && expand.equals("Latitude"));

      boolean allAssigned = true;

      for (TreeNode<AssignmentsTreeNodeData> child : positionNode
        .getChildren()) {
        if (child.getType().equals(LONGITUDE_UNASSIGNED)
          || child.getType().equals(LATITUDE_UNASSIGNED)
          || child.getType().equals(HEMISPHERE_UNASSIGNED)) {
          allAssigned = false;
          break;
        }
      }

      positionNode.setType(allAssigned ? VAR_FINISHED : VAR_UNFINISHED);
    }
  }

  private DefaultTreeNode<AssignmentsTreeNodeData> makePositionNodes(
    String positionType, DefaultTreeNode<AssignmentsTreeNodeData> mainNode,
    boolean expand) {

    String hemisphereNodeName = positionType + " Hemisphere";
    String unassignedType;
    String assignedType;
    String assignmentNodeType;

    for (FileDefinitionBuilder file : files) {
      PositionSpecification posSpec;

      if (positionType.equals("Longitude")) {
        posSpec = file.getLongitudeSpecification();
        unassignedType = LONGITUDE_UNASSIGNED;
        assignedType = LONGITUDE_ASSIGNED;
        assignmentNodeType = LONGITUDE_ASSIGNMENT;
      } else {
        posSpec = file.getLatitudeSpecification();
        unassignedType = LATITUDE_UNASSIGNED;
        assignedType = LATITUDE_ASSIGNED;
        assignmentNodeType = LATITUDE_ASSIGNMENT;
      }

      if (null == mainNode) {
        mainNode = new DefaultTreeNode<AssignmentsTreeNodeData>(unassignedType,
          new AssignmentsTreeNodeData(positionType), positionNode);
        if (expand) {
          mainNode.setExpanded(true);
        }
      }

      if (posSpec.getValueColumn() > -1) {

        new DefaultTreeNode<AssignmentsTreeNodeData>(assignmentNodeType,
          new AssignmentsTreeNodeData(file, posSpec), mainNode);
        mainNode.setType(assignedType);

        if (posSpec.hemisphereRequired()
          && posSpec.getHemisphereColumn() == -1) {
          new DefaultTreeNode<AssignmentsTreeNodeData>(HEMISPHERE_UNASSIGNED,
            new AssignmentsTreeNodeData(hemisphereNodeName), positionNode);
        } else if (posSpec.getHemisphereColumn() > -1) {
          DefaultTreeNode<AssignmentsTreeNodeData> hemisphereNode = new DefaultTreeNode<AssignmentsTreeNodeData>(
            HEMISPHERE_ASSIGNED,
            new AssignmentsTreeNodeData(hemisphereNodeName), positionNode);
          if (expand) {
            hemisphereNode.setExpanded(true);
          }

          new DefaultTreeNode<AssignmentsTreeNodeData>(HEMISPHERE_ASSIGNMENT,
            new AssignmentsTreeNodeData(file, posSpec), hemisphereNode);
        }
      }
    }

    return mainNode;
  }
}
