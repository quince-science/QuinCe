package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
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
 */
public class AssignmentsTree {

  /**
   * Node type indicating that the date/time has not been fully assigned.
   */
  private static final String DATETIME_UNFINISHED = "UNFINISHED_DATETIME";

  /**
   * Node type indicating that the date/time has been fully assigned.
   */
  private static final String DATETIME_FINISHED = "FINISHED_DATETIME";

  /**
   * Node type indicating an unassigned date/time node.
   */
  private static final String DATETIME_UNASSIGNED = "UNASSIGNED_DATETIME";

  /**
   * Node type indicating an assigned date/time node.
   */
  private static final String DATETIME_ASSIGNED = "ASSIGNED_DATETIME";

  /**
   * Node type for an assigned date/time column.
   */
  private static final String DATETIME_ASSIGNMENT = "DATETIME_ASSIGNMENT";

  /**
   * Node type indicating that the longitude has not been fully assigned.
   */
  private static final String LONGITUDE_UNASSIGNED = "UNASSIGNED_LONGITUDE";

  /**
   * Node type indicating that the longitude has been fully assigned.
   */
  private static final String LONGITUDE_ASSIGNED = "ASSIGNED_LONGITUDE";

  /**
   * Node type for an assigned longitude column.
   */
  private static final String LONGITUDE_ASSIGNMENT = "LONGITUDE_ASSIGNMENT";

  /**
   * Node type indicating that the latitude has not been fully assigned.
   */
  private static final String LATITUDE_UNASSIGNED = "UNASSIGNED_LATITUDE";

  /**
   * Node type indicating that the latitude has been fully assigned.
   */
  private static final String LATITUDE_ASSIGNED = "ASSIGNED_LATITUDE";

  /**
   * Node type for an assigned latitude column.
   */
  private static final String LATITUDE_ASSIGNMENT = "LATITUDE_ASSIGNMENT";

  /**
   * Node type indicating that a hemisphere entry has not been fully assigned.
   */
  private static final String HEMISPHERE_UNASSIGNED = "UNASSIGNED_HEMISPHERE";

  /**
   * Node type indicating that a hemisphere entry has been fully assigned.
   */
  private static final String HEMISPHERE_ASSIGNED = "ASSIGNED_HEMISPHERE";

  /**
   * Node type for an assigned hemisphere column.
   */
  private static final String HEMISPHERE_ASSIGNMENT = "HEMISPHERE_ASSIGNMENT";

  /**
   * Node type for a variable that has not been fully assigned (including the
   * position).
   */
  protected static final String VAR_UNFINISHED = "UNFINISHED_VARIABLE";

  /**
   * Node type for a variable that has been fully assigned (including the
   * position).
   */
  protected static final String VAR_FINISHED = "FINISHED_VARIABLE";

  /**
   * Node type indicating that a {@link SensorType} has not been fully assigned.
   */
  protected static final String SENSOR_TYPE_UNASSIGNED = "UNASSIGNED_SENSOR_TYPE";

  /**
   * Node type indicating that a {@link SensorType} has been fully assigned.
   */
  protected static final String SENSOR_TYPE_ASSIGNED = "ASSIGNED_SENSOR_TYPE";

  /**
   * Node type for an assigned {@link SensorType} column.
   */
  protected static final String ASSIGNMENT = "SENSOR_TYPE_ASSIGNMENT";

  /**
   * The set of sensor assignments being built.
   */
  private final SensorAssignments assignments;

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

  /**
   * The root node of the whole assignments tree.
   */
  private DefaultTreeNode<AssignmentsTreeNodeData> root;

  /**
   * The 'root' node of the Date/Time portion of the tree.
   */
  private DefaultTreeNode<AssignmentsTreeNodeData> dateTimeNode;

  /**
   * The parent Date/Time nodes for each of the {@link FileDefinition}s
   * specified for the {@link uk.ac.exeter.QuinCe.data.Instrument.Instrument}.
   */
  private Map<String, DefaultTreeNode<AssignmentsTreeNodeData>> dateTimeNodes;

  /**
   * The 'root' node of the Position section of the tree.
   */
  private DefaultTreeNode<AssignmentsTreeNodeData> positionNode;

  /**
   * The 'root' node for each of the {@link Variable}s measured by the
   * {@link uk.ac.exeter.QuinCe.data.Instrument.Instrument}.
   */
  private List<DefaultTreeNode<AssignmentsTreeNodeData>> variableNodes;

  /**
   * The nodes representing the columns assigned to each of the
   * {@link SensorType}s.
   *
   * <p>
   * These nodes may appear multiple times in the tree if the same
   * {@link SensorType} is used by multiple {@link Variable}s.
   * </p>
   */
  private Map<SensorType, List<DefaultTreeNode<AssignmentsTreeNodeData>>> sensorTypeNodes;

  /**
   * The set of {@link FileDefinition}s specified for the
   * {@link uk.ac.exeter.QuinCe.data.Instrument.Instrument}.
   */
  private TreeSet<FileDefinitionBuilder> files;

  /**
   * The {@link Variable}s measured by the
   * {@link uk.ac.exeter.QuinCe.data.Instrument.Instrument}.
   */
  private final List<Variable> variables;

  /**
   * The attributes assigned for each variable.
   *
   * <p>
   * The attributes can determine which {@link SensorType}s are required in the
   * {@link uk.ac.exeter.QuinCe.data.Instrument.Instrument}'s configuration.
   * </p>
   *
   * <p>
   * The key for the {@link Map} is the database ID of each {@link Variable}.
   * </p>
   */
  private Map<Long, VariableAttributes> varAttributes = null;

  /**
   * Initialise and construct the assignments tree.
   *
   * @param variables
   *          The {@link Variable}s measured by the
   *          {@link uk.ac.exeter.QuinCe.data.Instrument.Instrument}.
   * @param assignments
   *          The object holding the assignments columns columns to times,
   *          positions, {@link SensorType}s etc.
   * @param needsPosition
   *          Indicates whether or not position columns are needed.
   * @throws SensorConfigurationException
   *           If the system's sensor configuration is invalid.
   * @throws SensorTypeNotFoundException
   *           If any referenced {@link SensorType} does not exist.
   */
  protected AssignmentsTree(List<Variable> variables,
    SensorAssignments assignments, boolean needsPosition)
    throws SensorConfigurationException, SensorTypeNotFoundException {

    root = new DefaultTreeNode<AssignmentsTreeNodeData>(
      new StringNodeData("Root"), null);
    this.variables = variables;
    this.assignments = assignments;
    this.needsPosition = needsPosition;
    sensorTypeNodes = new HashMap<SensorType, List<DefaultTreeNode<AssignmentsTreeNodeData>>>();
    files = new TreeSet<FileDefinitionBuilder>();

    buildTree(variables);
  }

  /**
   * Construct the base structure of the tree ready to be populated.
   *
   * @param variables
   *          The {@link Variable}s measured by the
   *          {@link uk.ac.exeter.QuinCe.data.Instrument.Instrument}.
   * @throws SensorConfigurationException
   *           If the system's sensor configuration is invalid.
   * @throws SensorTypeNotFoundException
   *           If any referenced {@link SensorType} does not exist.
   */
  private void buildTree(List<Variable> variables)
    throws SensorConfigurationException, SensorTypeNotFoundException {

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

  /**
   * Add a node for a specific {@link SensorType} to the structure tracking
   * these nodes.
   *
   * @param sensorType
   *          The {@link SensorType}
   * @param node
   *          The node for the {@link SensorType}.
   * @see #sensorTypeNodes
   */
  private void addSensorTypeNode(SensorType sensorType,
    DefaultTreeNode<AssignmentsTreeNodeData> node) {

    if (!sensorTypeNodes.containsKey(sensorType)) {
      sensorTypeNodes.put(sensorType,
        new ArrayList<DefaultTreeNode<AssignmentsTreeNodeData>>());
    }

    sensorTypeNodes.get(sensorType).add(node);
  }

  /**
   * Get the overall root node of the tree.
   *
   * @return The root node.
   */
  protected DefaultTreeNode<AssignmentsTreeNodeData> getRoot() {
    return root;
  }

  /**
   * Update the tree with the details of a new assignment.
   *
   * @param assignment
   *          The assignment.
   * @throws SensorConfigurationException
   *           If the system's sensor configuration is invalid.
   * @throws SensorTypeNotFoundException
   *           If any referenced {@link SensorType} does not exist.
   * @throws SensorAssignmentException
   *           If the assignment is invalid.
   */
  protected void addAssignment(SensorAssignment assignment)
    throws SensorConfigurationException, SensorTypeNotFoundException,
    SensorAssignmentException {

    if (sensorTypeNodes.containsKey(assignment.getSensorType())) {
      for (DefaultTreeNode<AssignmentsTreeNodeData> sensorTypeNode : sensorTypeNodes
        .get(assignment.getSensorType())) {

        new DefaultTreeNode<AssignmentsTreeNodeData>(ASSIGNMENT,
          new SensorAssignmentNodeData(assignment), sensorTypeNode);

        sensorTypeNode.setExpanded(true);
      }
    }

    updateVariableNodes();
  }

  /**
   * Update the tree to reflect the removal of a {@link FileDefinition} from the
   * {@link uk.ac.exeter.QuinCe.data.Instrument.Instrument}'s configuration.
   *
   * <p>
   * The date/time nodes for the {@link FileDefinition} will be removed, along
   * with any nodes related to {@link SensorAssignment}s from that file.
   * </p>
   *
   * @param fileDescription
   *          The file description.
   * @throws SensorConfigurationException
   *           If the system's sensor configuration is invalid.
   * @throws SensorTypeNotFoundException
   *           If any referenced {@link SensorType} does not exist.
   * @throws SensorAssignmentException
   *           If any assignments in the adjusted tree are invalid.
   */
  protected void removeFile(String fileDescription)
    throws SensorConfigurationException, SensorTypeNotFoundException,
    SensorAssignmentException {

    dateTimeNodes.remove(fileDescription);

    Iterator<TreeNode<AssignmentsTreeNodeData>> nodeSearch = dateTimeNode
      .getChildren().iterator();
    while (nodeSearch.hasNext()) {
      TreeNode<AssignmentsTreeNodeData> node = nodeSearch.next();
      FileNodeData nodeData = (FileNodeData) node.getData();

      if (nodeData.getFileDescription().equals(fileDescription)) {
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
          if (!nodeData.getAssignment().getDataFile().equals(fileDescription)) {
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
      if (file.getFileDescription().equals(fileDescription)) {
        fileSearch.remove();
        break;
      }
    }

    updatePositionNodes(null);
  }

  /**
   * Remove the details of a {@link SensorAssignment} from the tree.
   *
   * @param assignment
   *          The {@link SensorAssignment} to be removed.
   * @throws SensorConfigurationException
   *           If the system's sensor configuration is invalid.
   * @throws SensorTypeNotFoundException
   *           If any referenced {@link SensorType} does not exist.
   * @throws SensorAssignmentException
   *           If any assignments in the adjusted tree are invalid.
   */
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

  /**
   * Update the tree to reflect the addition of a new {@link FileDefinition} to
   * the {@link uk.ac.exeter.QuinCe.data.Instrument.Instrument}.
   *
   * <p>
   * Date/Time nodes are added for the file, and the tree is refreshed.
   * </p>
   *
   * @param file
   *          The file being added.
   * @throws DateTimeSpecificationException
   *           If an error occurs while setting up the date/time nodes.
   */
  protected void addFile(FileDefinitionBuilder file)
    throws DateTimeSpecificationException {

    makeDateTimeNode(file, true);
    files.add(file);
    updatePositionNodes(null);
  }

  /**
   * Create the date/time nodes for the specified {@link FileDefinition}.
   *
   * @param file
   *          The file.
   * @param expanded
   *          Indicates whether the date/time nodes for the file should be
   *          expanded in the display.
   * @throws DateTimeSpecificationException
   *           If an error occurs while setting up the date/time nodes.
   */
  private void makeDateTimeNode(FileDefinitionBuilder file, boolean expanded)
    throws DateTimeSpecificationException {

    DefaultTreeNode<AssignmentsTreeNodeData> fileDateTimeNode = new DefaultTreeNode<AssignmentsTreeNodeData>(
      DATETIME_UNFINISHED, new FileNodeData(file), dateTimeNode);
    fileDateTimeNode.setExpanded(true);

    dateTimeNodes.put(file.getFileDescription(), fileDateTimeNode);
    setDateTimeAssignment(file);
  }

  /**
   * Rebuild the date/time nodes for a {@link FileDefinition} after an update to
   * its date/time specification.
   *
   * @param file
   *          The file that has been updated.
   * @throws DateTimeSpecificationException
   *           If the file's date/time specification is invalid.
   */
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

  /**
   * Update the tree to reflect the renaming of a {@link FileDefinition}.
   *
   * <p>
   * All nodes with the file's name in their text are updated.
   * </p>
   *
   * @param oldName
   *          The file's previous name.
   * @param renamedFile
   *          The file's new name.
   * @throws DateTimeSpecificationException
   *           If the date/time specification for the file is invalid.
   */
  protected void renameFile(String oldName, FileDefinitionBuilder renamedFile)
    throws DateTimeSpecificationException {

    // We only need to update the date/time assignments;
    // sensor assignments are updated in the SensorAssignment objects
    DefaultTreeNode<AssignmentsTreeNodeData> node = dateTimeNodes
      .remove(oldName);
    node.setData(new FileNodeData(renamedFile));
    dateTimeNodes.put(renamedFile.getFileDescription(), node);
  }

  /**
   * Update the position nodes for the
   * {@link uk.ac.exeter.QuinCe.data.Instrument.Instrument}.
   *
   * @param expand
   *          Indicates whether the {@code Longitude} or {@code Latitude} nodes
   *          should be expanded (if it equals either of those values), or
   *          neither (if any other value is set).
   */
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

  /**
   * Build the position nodes for the
   * {@link uk.ac.exeter.QuinCe.data.Instrument.Instrument}.
   *
   * <p>
   * This builds either the Longitude or Latitude node (according to
   * {@code positionType}. If the node does not exist ({@code mainNode} is
   * {@code null}) it is created; otherwise it is rebuilt.
   * </p>
   *
   * @param positionType
   *          Indicates whether the {@code Longitude} or {@code Latitude} nodes
   *          are to be created.
   * @param mainNode
   *          The 'root' node for the position nodes.
   * @param expand
   *          Indicates whether or not the node should be expanded.
   * @return The position node, either created or an updated {@code mainNode}.
   */
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
   * Update all non-diagnostic {@link SensorType} nodes, setting their
   * Assigned/Unassigned status.
   *
   * @throws SensorConfigurationException
   *           If the system's sensor configuration is invalid.
   * @throws SensorTypeNotFoundException
   *           If any referenced {@link SensorType} does not exist.
   * @throws SensorAssignmentException
   *           If any assignments in the adjusted tree are invalid.
   */
  private void updateVariableNodes() throws SensorConfigurationException,
    SensorTypeNotFoundException, SensorAssignmentException {

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

  /**
   * Build a cached {@link Map} of the attributes for each of the
   * {@link uk.ac.exeter.QuinCe.data.Instrument.Instrument}'s {@link Variable}s.
   *
   * @return The constructed Map.
   */
  private Map<Long, VariableAttributes> getVarAttributesMap() {
    if (null == varAttributes) {
      varAttributes = new HashMap<Long, VariableAttributes>();
      variables.forEach(v -> varAttributes.put(v.getId(), v.getAttributes()));
    }

    return varAttributes;
  }

  /**
   * Get all the {@link SensorType}s that need to be assigned. Includes both
   * assigned and unassigned types.
   *
   * @return The {@link SensorType}s in the tree.
   */
  protected Set<SensorType> getSensorTypes() {
    return sensorTypeNodes.keySet();
  }
}
