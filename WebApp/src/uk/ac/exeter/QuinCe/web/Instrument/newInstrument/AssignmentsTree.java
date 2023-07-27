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
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.VariableAttributes;
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

  protected static final String SENSOR_TYPE_ASSIGNED = "ASSIGNED_SENSOR_TYPE";

  protected static final String SENSOR_TYPE_UNASSIGNED = "UNASSIGNED_SENSOR_TYPE";

  protected static final String ASSIGNMENT = "SENSOR_TYPE_ASSIGNMENT";

  private final SensorAssignments assignments;

  private final boolean needsPosition;

  private DefaultTreeNode<AssignmentsTreeNodeData> root;

  private DefaultTreeNode<AssignmentsTreeNodeData> dateTimeNode;

  private Map<String, DefaultTreeNode<AssignmentsTreeNodeData>> dateTimeNodes;

  private DefaultTreeNode<AssignmentsTreeNodeData> positionNode;

  private List<DefaultTreeNode<AssignmentsTreeNodeData>> variableNodes;

  private Map<SensorType, List<DefaultTreeNode<AssignmentsTreeNodeData>>> sensorTypeNodes;

  private TreeSet<FileDefinitionBuilder> files;

  private final List<Variable> variables;

  private Map<Long, VariableAttributes> varAttributes = null;

  protected AssignmentsTree(List<Variable> variables,
    SensorAssignments assignments, boolean needsPosition)
    throws SensorConfigurationException, SensorAssignmentException,
    SensorTypeNotFoundException {

    root = new DefaultTreeNode<AssignmentsTreeNodeData>(
      new StringNodeData("Root"), null);
    this.variables = variables;
    this.assignments = assignments;
    this.needsPosition = needsPosition;
    sensorTypeNodes = new HashMap<SensorType, List<DefaultTreeNode<AssignmentsTreeNodeData>>>();
    files = new TreeSet<FileDefinitionBuilder>();

    buildTree(variables);
  }

  private void buildTree(List<Variable> variables)
    throws SensorConfigurationException, SensorAssignmentException,
    SensorTypeNotFoundException {

    dateTimeNode = new DefaultTreeNode<AssignmentsTreeNodeData>(VAR_UNFINISHED,
      new StringNodeData("Date/Time"), root);
    dateTimeNode.setExpanded(true);
    dateTimeNodes = new TreeMap<String, DefaultTreeNode<AssignmentsTreeNodeData>>();

    if (needsPosition) {
      positionNode = new DefaultTreeNode<AssignmentsTreeNodeData>(
        VAR_UNFINISHED, new StringNodeData("Position"), root);
      positionNode.setExpanded(true);
      updatePositionNodes(null);
    }

    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();

    variableNodes = new ArrayList<DefaultTreeNode<AssignmentsTreeNodeData>>(
      variables.size());

    for (Variable var : variables) {
      DefaultTreeNode<AssignmentsTreeNodeData> varNode = new DefaultTreeNode<AssignmentsTreeNodeData>(
        new VariableNodeData(var), root);

      varNode.setType(
        assignments.isVariableComplete(var) ? VAR_FINISHED : VAR_UNFINISHED);
      varNode.setExpanded(true);

      variableNodes.add(varNode);

      for (SensorType sensorType : sensorConfig.getSensorTypes(var.getId(),
        true, true, true)) {

        DefaultTreeNode<AssignmentsTreeNodeData> node = new DefaultTreeNode<AssignmentsTreeNodeData>(
          new SensorTypeNodeData(sensorType), varNode);

        node.setType(assignments.isAssigned(sensorType) ? SENSOR_TYPE_ASSIGNED
          : SENSOR_TYPE_UNASSIGNED);

        addSensorTypeNode(sensorType, node);
      }
    }

    DefaultTreeNode<AssignmentsTreeNodeData> diagnosticsNode = new DefaultTreeNode<AssignmentsTreeNodeData>(
      VAR_FINISHED, new StringNodeData("Diagnostics"), root);

    for (SensorType diagnosticType : sensorConfig.getDiagnosticSensorTypes()) {
      addSensorTypeNode(diagnosticType,
        new DefaultTreeNode<AssignmentsTreeNodeData>(SENSOR_TYPE_ASSIGNED,
          new SensorTypeNodeData(diagnosticType), diagnosticsNode));
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

  protected void addAssignment(SensorAssignment assignment)
    throws SensorAssignmentException, SensorConfigurationException,
    SensorTypeNotFoundException {

    for (DefaultTreeNode<AssignmentsTreeNodeData> sensorTypeNode : sensorTypeNodes
      .get(assignment.getSensorType())) {

      new DefaultTreeNode<AssignmentsTreeNodeData>(ASSIGNMENT,
        new SensorAssignmentNodeData(assignment), sensorTypeNode);

      sensorTypeNode.setExpanded(true);
    }

    updateVariableNodes();
  }

  protected void removeFile(String fileName) throws SensorAssignmentException,
    SensorConfigurationException, SensorTypeNotFoundException {

    dateTimeNodes.remove(fileName);

    Iterator<TreeNode<AssignmentsTreeNodeData>> nodeSearch = dateTimeNode
      .getChildren().iterator();
    while (nodeSearch.hasNext()) {
      TreeNode<AssignmentsTreeNodeData> node = nodeSearch.next();
      FileNodeData nodeData = (FileNodeData) node.getData();

      if (nodeData.getFileDescription().equals(fileName)) {
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

          SensorAssignmentNodeData nodeData = (SensorAssignmentNodeData) node
            .getData();
          if (nodeData.getAssignment().getDataFile().equals(fileName)) {
            keptChildren.add(node);
          }
        }

        sensorTypeNode.setChildren(keptChildren);
        updateVariableNodes();
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

  protected void removeAssignment(SensorAssignment assignment)
    throws SensorAssignmentException, SensorConfigurationException,
    SensorTypeNotFoundException {
    List<DefaultTreeNode<AssignmentsTreeNodeData>> typeNodes = sensorTypeNodes
      .get(assignment.getSensorType());

    for (DefaultTreeNode<AssignmentsTreeNodeData> node : typeNodes) {

      Iterator<TreeNode<AssignmentsTreeNodeData>> nodeSearch = node
        .getChildren().iterator();

      while (nodeSearch.hasNext()) {
        TreeNode<AssignmentsTreeNodeData> assignmentNode = nodeSearch.next();
        SensorAssignmentNodeData nodeData = (SensorAssignmentNodeData) assignmentNode
          .getData();
        if (nodeData.getAssignment().equals(assignment)) {
          nodeSearch.remove();
        }
      }
    }

    updateVariableNodes();
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
      DATETIME_UNFINISHED, new FileNodeData(file), dateTimeNode);
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
          new StringNodeData(entry.getKey()), parent);
      } else {
        DefaultTreeNode<AssignmentsTreeNodeData> child = new DefaultTreeNode<AssignmentsTreeNodeData>(
          DATETIME_ASSIGNED, new StringNodeData(entry.getKey()), parent);
        child.setExpanded(true);

        DateTimeColumnAssignment assignment = dateTimeSpec.getAssignment(
          DateTimeSpecification.getAssignmentIndex(entry.getKey()));

        new DefaultTreeNode<AssignmentsTreeNodeData>(DATETIME_ASSIGNMENT,
          new DateTimeAssignmentNodeData(file, assignment), child);
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
    node.setData(new FileNodeData(renamedFile));
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
          new StringNodeData(positionType), positionNode);
        if (expand) {
          mainNode.setExpanded(true);
        }
      }

      if (posSpec.getValueColumn() > -1) {

        new DefaultTreeNode<AssignmentsTreeNodeData>(assignmentNodeType,
          new PositionSpecNodeData(file, posSpec, PositionSpecNodeData.VALUE),
          mainNode);
        mainNode.setType(assignedType);

        if (posSpec.hemisphereRequired()
          && posSpec.getHemisphereColumn() == -1) {
          new DefaultTreeNode<AssignmentsTreeNodeData>(HEMISPHERE_UNASSIGNED,
            new StringNodeData(hemisphereNodeName), positionNode);
        } else if (posSpec.getHemisphereColumn() > -1) {
          DefaultTreeNode<AssignmentsTreeNodeData> hemisphereNode = new DefaultTreeNode<AssignmentsTreeNodeData>(
            HEMISPHERE_ASSIGNED, new StringNodeData(hemisphereNodeName),
            positionNode);
          if (expand) {
            hemisphereNode.setExpanded(true);
          }

          new DefaultTreeNode<AssignmentsTreeNodeData>(HEMISPHERE_ASSIGNMENT,
            new PositionSpecNodeData(file, posSpec,
              PositionSpecNodeData.HEMISPHERE),
            hemisphereNode);
        }
      }
    }

    return mainNode;
  }

  /**
   * Set the ASSIGNED/UNASSIGNED type on all non-diagnostic SensorType nodes.
   * 
   * @throws SensorConfigurationException
   * @throws SensorAssignmentException
   * @throws SensorTypeNotFoundException
   */
  private void updateVariableNodes() throws SensorAssignmentException,
    SensorConfigurationException, SensorTypeNotFoundException {

    for (DefaultTreeNode<AssignmentsTreeNodeData> varNode : variableNodes) {
      VariableNodeData data = (VariableNodeData) varNode.getData();
      varNode.setType(
        assignments.isVariableComplete(data.getVariable()) ? VAR_FINISHED
          : VAR_UNFINISHED);
    }

    for (Map.Entry<SensorType, List<DefaultTreeNode<AssignmentsTreeNodeData>>> entry : sensorTypeNodes
      .entrySet()) {

      SensorType sensorType = entry.getKey();

      if (!sensorType.isDiagnostic()) {
        String type = assignments.isAssignmentRequired(sensorType,
          getVarAttributesMap()) ? SENSOR_TYPE_UNASSIGNED
            : SENSOR_TYPE_ASSIGNED;
        entry.getValue().stream().forEach(n -> n.setType(type));
      }
    }
  }

  private Map<Long, VariableAttributes> getVarAttributesMap() {
    if (null == varAttributes) {
      varAttributes = new HashMap<Long, VariableAttributes>();
      variables.forEach(v -> varAttributes.put(v.getId(), v.getAttributes()));
    }

    return varAttributes;
  }
}
