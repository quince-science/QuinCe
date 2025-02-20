package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument;

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
}
