package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.AssignmentsTree;

/**
 * Node data for a simple string
 */
public class StringNodeData extends AssignmentsTreeNodeData {

  private String label;

  protected StringNodeData(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }

  @Override
  public String toString() {
    return label;
  }

  @Override
  public String getId() {
    return "STRING_" + label;
  }
}
