package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.AssignmentsTree;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public class VariableNodeData extends AssignmentsTreeNodeData {

  private final Variable variable;

  protected VariableNodeData(Variable variable) {
    this.variable = variable;
  }

  protected Variable getVariable() {
    return variable;
  }

  @Override
  public String getLabel() {
    return variable.getName();
  }

  @Override
  public String toString() {
    return getLabel();
  }

  @Override
  public String getId() {
    return "VARIABLE" + variable.getName();
  }
}
