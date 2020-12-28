package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.MathUtils;
import uk.ac.exeter.QuinCe.utils.NoEmptyStringList;

public class DataReductionRecord {

  /**
   * The database ID of the measurement
   */
  private final long measurementId;

  /**
   * The variable calculated in this record
   */
  private final long variableId;

  private final List<String> parameterNames;

  /**
   * Intermediate calculation values
   */
  private Map<String, Double> calculationValues;

  /**
   * QC Flag
   */
  private Flag qcFlag;

  /**
   * QC Message
   */
  private NoEmptyStringList qcMessages;

  /**
   * Create an empty record for a given measurement
   *
   * @param measurement
   *          The measurement
   */
  public DataReductionRecord(Measurement measurement, Variable variable,
    List<String> parameterNames) {
    this.measurementId = measurement.getId();
    this.variableId = variable.getId();
    this.parameterNames = Collections.unmodifiableList(parameterNames);

    this.calculationValues = new HashMap<String, Double>();
    this.qcFlag = Flag.ASSUMED_GOOD;
    this.qcMessages = new NoEmptyStringList();
  }

  protected DataReductionRecord(long measurementId, long variableId,
    List<String> parameterNames, Map<String, Double> calculationValues,
    Flag qcFlag, NoEmptyStringList qcMessages) {

    this.measurementId = measurementId;
    this.variableId = variableId;
    this.parameterNames = Collections.unmodifiableList(parameterNames);

    this.calculationValues = calculationValues;
    this.qcFlag = qcFlag;
    this.qcMessages = qcMessages;

  }

  public void setQc(Flag flag, String message) {
    setQc(flag, Arrays.asList(new String[] { message }));
  }

  /**
   * Set the QC details for the record
   *
   * @param flag
   *          The QC flag
   * @param message
   *          The QC messages
   */
  public void setQc(Flag flag, List<String> messages) {
    if (flag.equals(qcFlag)) {
      qcMessages.addAll(messages);
    } else if (flag.moreSignificantThan(qcFlag)) {
      qcFlag = flag;
      qcMessages = new NoEmptyStringList(messages);
    }
  }

  /**
   * Store a calculation value
   *
   * @param parameter
   *          The parameter
   * @param value
   *          The value
   * @throws DataReductionException
   */
  protected void put(String parameter, Double value)
    throws DataReductionException {
    if (!parameterNames.contains(parameter)) {
      throw new DataReductionException(
        "Unrecognised calculation parameter '" + parameter + "'");
    }
    calculationValues.put(parameter, value);
  }

  /**
   * Get the measurement ID
   *
   * @return The measurement ID
   */
  public long getMeasurementId() {
    return measurementId;
  }

  public long getVariableId() {
    return variableId;
  }

  /**
   * Get the QC flag
   *
   * @return The QC flag
   */
  public Flag getQCFlag() {
    return qcFlag;
  }

  /**
   * Get the QC messages
   *
   * @return The QC messages
   */
  public List<String> getQCMessages() {
    return qcMessages;
  }

  /**
   * Get the calculation values as a JSON string
   *
   * @return The calculation JSON
   */
  public String getCalculationJson() {
    Gson gson = new Gson();
    return gson.toJson(MathUtils.nanToNull(calculationValues));
  }

  public Double getCalculationValue(String param) {
    return calculationValues.get(param);
  }
}
