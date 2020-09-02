package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

public class VariableProperty {

  private final String id;

  private final String label;

  private Float value = 0F;

  protected VariableProperty(String id, String label) {
    this.id = id;
    this.label = label;
  }

  public Float getValue() {
    return value;
  }

  public void setValue(Float value) {
    this.value = value;
  }

  public String getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }

}
