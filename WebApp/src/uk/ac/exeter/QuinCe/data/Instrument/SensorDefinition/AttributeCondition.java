package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

public class AttributeCondition {
  private final String attributeName;
  private final String attributeValue;

  protected AttributeCondition(String attributeName, String attributeValue) {
    this.attributeName = attributeName;
    this.attributeValue = attributeValue;
  }

  protected boolean matches(VariableAttributes attributes) {
    return null != attributes
      && attributes.get(attributeName).equals(attributeValue);
  }
}
