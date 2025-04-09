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

  private final String runType;

  private Map<Long, Boolean> dependsQuestionAnswers;

  protected VariableProperties() {
    this.coefficients = new ArrayList<String>();
    this.runType = null;
    this.dependsQuestionAnswers = new HashMap<Long, Boolean>();
  }

  protected VariableProperties(String runType, List<String> coefficients,
    Map<Long, Boolean> dependsQuestionAnswers) {
    this.runType = runType;
    this.coefficients = coefficients;

    if (null == dependsQuestionAnswers) {
      this.dependsQuestionAnswers = new HashMap<Long, Boolean>();
    } else {
      this.dependsQuestionAnswers = dependsQuestionAnswers;
    }
  }

  public List<String> getCoefficients() {
    return coefficients;
  }

  public String getRunType() {
    return runType;
  }

  public Map<Long, Boolean> getDependsQuestionAnswers() {
    return dependsQuestionAnswers;
  }
}
