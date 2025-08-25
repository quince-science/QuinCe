package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The properties for a variable.
 *
 * <p>
 * Instances of this class are built from JSON strings.
 * </p>
 *
 * @see VariablePropertiesDeserializer
 */
public class VariableProperties {

  private final List<String> coefficients;

  private Map<Long, Boolean> dependsQuestionAnswers;

  private List<PresetRunType> presetRunTypes;

  protected VariableProperties() {
    this.coefficients = new ArrayList<String>();
    this.presetRunTypes = new ArrayList<PresetRunType>();
    this.dependsQuestionAnswers = new HashMap<Long, Boolean>();
  }

  protected VariableProperties(List<String> coefficients,
    Map<Long, Boolean> dependsQuestionAnswers,
    List<PresetRunType> presetRunTypes) {

    this.coefficients = null != coefficients ? coefficients
      : new ArrayList<String>();

    this.dependsQuestionAnswers = null != dependsQuestionAnswers
      ? dependsQuestionAnswers
      : new HashMap<Long, Boolean>();

    this.presetRunTypes = null != presetRunTypes ? presetRunTypes
      : new ArrayList<PresetRunType>();
  }

  public List<String> getCoefficients() {
    return coefficients;
  }

  public String getRunType(long variableId) {

    String runType = null;

    PresetRunType variableRunType = presetRunTypes.stream()
      .filter(prt -> prt.getCategory().getType() == variableId).findFirst()
      .orElse(null);

    if (null != variableRunType) {
      runType = variableRunType.getDefaultRunType();
    }

    return runType;
  }

  public Map<Long, Boolean> getDependsQuestionAnswers() {
    return dependsQuestionAnswers;
  }

  public boolean hasPresetRunTypes() {
    return presetRunTypes.size() > 0;
  }

  protected List<PresetRunType> getPresetRunTypes() {
    return presetRunTypes;
  }
}
