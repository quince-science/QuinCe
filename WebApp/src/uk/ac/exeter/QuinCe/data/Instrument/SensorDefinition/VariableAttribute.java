package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.util.List;

public class VariableAttribute {

  private final String id;

  private final String type;

  private final String description;

  private List<String> enumEntries;

  /**
   * The user's chosen value for this attribute.
   */
  private String value;

  protected VariableAttribute(String id, String type, String description,
    List<String> enumEntries) throws InvalidVariableAttributeException {
    if (!VariableAttributes.isValidType(type)) {
      throw new InvalidVariableAttributeException(
        "Invalid attribute type " + type);
    }

    this.id = id;
    this.type = type;
    this.description = description;

    if (type.equals(VariableAttributes.ENUM_TYPE)) {
      if (enumEntries.size() < 2) {
        throw new InvalidVariableAttributeException(
          "Must have 2 or more enum values");
      }

      this.enumEntries = enumEntries;
    } else {
      this.enumEntries = null;
    }

    setDefaultValue();
  }

  protected void setDefaultValue() {
    switch (type) {
    case VariableAttributes.NUMBER_TYPE: {
      value = "0.00";
      break;
    }
    case VariableAttributes.ENUM_TYPE: {
      value = enumEntries.get(0);
      break;
    }
    case VariableAttributes.BOOL_TYPE: {
      value = "false";
      break;
    }
    }
  }

  public String getDescription() {
    return description;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getType() {
    return type;
  }

  public List<String> getEnumEntries() {
    return enumEntries;
  }

  public String getId() {
    return id;
  }
}
