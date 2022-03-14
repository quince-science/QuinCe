package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;

@SuppressWarnings("serial")
public class VariableTreeNode extends DefaultTreeNode {

  protected static final String VAR_UNFINISHED = "UNFINISHED_VARIABLE";

  protected static final String VAR_FINISHED = "FINISHED_VARIABLE";

  private final SensorAssignments sensorAssignments;

  private final Variable variable;

  protected VariableTreeNode(TreeNode parent, Variable variable,
    SensorAssignments assignments) {

    super(variable.getName(), parent);
    this.variable = variable;
    this.sensorAssignments = assignments;
    setExpanded(true);
  }

  @Override
  public String getType() {

    String result = "";

    try {
      result = sensorAssignments.isVariableComplete(variable) ? VAR_FINISHED
        : VAR_UNFINISHED;
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
    }

    return result;
  }
}
