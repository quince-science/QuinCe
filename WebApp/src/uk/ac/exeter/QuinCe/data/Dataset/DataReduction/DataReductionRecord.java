package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.utils.MathUtils;
import uk.ac.exeter.QuinCe.utils.NoEmptyStringList;

public class DataReductionRecord {

  /**
   * The database ID of the measurement
   */
  private final long measurementId;

  /**
   * The database ID of the variable being processed
   */
  private final long variableId;

  /**
   * Intermediate calculation values
   */
  private HashMap<String, Double> calculationValues;

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
  public DataReductionRecord(Measurement measurement) {
    this.measurementId = measurement.getId();
    this.variableId = measurement.getVariable().getId();

    this.calculationValues = new HashMap<String, Double>();
    this.qcFlag = Flag.ASSUMED_GOOD;
    this.qcMessages = new NoEmptyStringList();
  }

  /**
   * Set the QC details for the record
   * 
   * @param flag
   *          The QC flag
   * @param message
   *          The QC messages
   */
  protected void setQc(Flag flag, List<String> messages) {
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
   */
  protected void put(String parameter, Double value) {
    calculationValues.put(parameter, value);
  }

  protected void putAll(Map<String, Double> values) {
    calculationValues.putAll(values);
  }

  /**
   * Get the measurement ID
   * 
   * @return The measurement ID
   */
  public long getMeasurementId() {
    return measurementId;
  }

  /**
   * Get the variable ID
   * 
   * @return The variable ID
   */
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
}
