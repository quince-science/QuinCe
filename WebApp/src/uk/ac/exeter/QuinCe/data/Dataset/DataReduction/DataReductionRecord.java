package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.MathUtils;
import uk.ac.exeter.QuinCe.utils.NoEmptyStringSet;

public class DataReductionRecord implements Comparable<DataReductionRecord> {

  /**
   * The database ID of the measurement
   */
  private final long measurementId;

  /**
   * The variable calculated in this record
   */
  private final long variableId;

  /**
   * The parameter names for this record.
   */
  private final List<String> parameterNames;

  /**
   * Holds variables calculated for this record.
   */
  private Map<String, Double> calculationValues;

  /**
   * The record's QC Flag.
   */
  private Flag qcFlag;

  /**
   * The QC message for the record.
   */
  private NoEmptyStringSet qcMessages;

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
    this.qcMessages = new NoEmptyStringSet();
  }

  protected DataReductionRecord(long measurementId, long variableId,
    List<String> parameterNames, Map<String, Double> calculationValues,
    Flag qcFlag, NoEmptyStringSet qcMessages) {

    this.measurementId = measurementId;
    this.variableId = variableId;
    this.parameterNames = Collections.unmodifiableList(parameterNames);

    this.calculationValues = calculationValues;
    this.qcFlag = qcFlag;
    this.qcMessages = qcMessages;

  }

  /**
   * Set a simple QC flag and single message.
   *
   * @param flag
   *          The QC flag.
   * @param message
   *          The QC message.
   * @throws DataReductionException
   *           If the QC message is empty.
   */
  public void setQc(Flag flag, String message) throws DataReductionException {
    setQc(flag, Arrays.asList(new String[] { message }));
  }

  /**
   * Set the QC details for the record.
   *
   * @param flag
   *          The QC flag.
   * @param message
   *          The QC messages.
   * @throws DataReductionException
   *           If there are not valid QC messages.
   */
  public void setQc(Flag flag, List<String> messages)
    throws DataReductionException {
    if (flag.equalSignificance(qcFlag)) {
      qcMessages.addAll(messages);
    } else if (flag.moreSignificantThan(qcFlag)) {
      qcFlag = flag;

      NoEmptyStringSet messageList = new NoEmptyStringSet(messages);
      if (!flag.equals(Flag.NO_QC) && messageList.size() == 0) {
        throw new DataReductionException("Empty QC message not allowed");
      }

      qcMessages = new NoEmptyStringSet(messages);
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
  public void put(String parameter, Double value)
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
  public Set<String> getQCMessages() {
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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (measurementId ^ (measurementId >>> 32));
    result = prime * result + (int) (variableId ^ (variableId >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    DataReductionRecord other = (DataReductionRecord) obj;
    if (measurementId != other.measurementId)
      return false;
    if (variableId != other.variableId)
      return false;
    return true;
  }

  @Override
  public int compareTo(DataReductionRecord o) {
    int result = ((Long) measurementId).compareTo((Long) o.measurementId);
    if (result == 0) {
      result = ((Long) variableId).compareTo((Long) o.variableId);
    }
    return result;
  }
}
