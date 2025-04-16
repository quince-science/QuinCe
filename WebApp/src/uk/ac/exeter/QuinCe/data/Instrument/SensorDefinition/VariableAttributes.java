package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("serial")
public class VariableAttributes extends ArrayList<VariableAttribute> {

  protected static final String NUMBER_TYPE = "NUMBER";

  protected static final String ENUM_TYPE = "ENUM";

  protected static final String BOOL_TYPE = "BOOL";

  private static List<String> TYPES_ENUM = null;

  static {
    TYPES_ENUM = new ArrayList<String>();
    TYPES_ENUM.add(BOOL_TYPE);
    TYPES_ENUM.add(NUMBER_TYPE);
    TYPES_ENUM.add(ENUM_TYPE);
  }

  protected static boolean isValidType(String type) {
    return TYPES_ENUM.contains(type);
  }

  public void reset() {
    stream().forEach(a -> a.setDefaultValue());
  }

  public String get(String id) {
    String result = null;

    Optional<VariableAttribute> attribute = stream()
      .filter(a -> a.getId().equals(id)).findAny();

    if (attribute.isPresent()) {
      result = attribute.get().getValue();
    }

    return result;
  }
}
