package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;

/**
 * A DataReducer will perform all data reduction calculations for a given
 * {@link Variable}. The output from the data reduction is a
 * {@link DataReductionRecord}.
 *
 * <p>
 * This is the base class for all data reducers providing the common
 * functionality required both by the reducers and their callers.
 * </p>
 */
public abstract class DataReducer {

  /**
   * The {@link Variable} for which the reducer performs calculations.
   */
  protected Variable variable;

  /**
   * The {@link Variable}'s attributes.
   */
  protected Map<String, Properties> properties;

  /**
   * Simple constructor to initialise the reducer with the {@link Variable}
   * details.
   *
   * @param variable
   *          The {@link Variable}.
   * @param properties
   *          The {@link Variable}'s attributes.
   */
  public DataReducer(Variable variable, Map<String, Properties> properties) {
    this.variable = variable;
    this.properties = properties;
  }

  /**
   * Perform the data reduction calculations for a given {@link Measurement} and
   * set the QC flag on the resulting {@link DataReductionRecord}.
   *
   * <p>
   * The QC flag for the {@link DataReductionRecord} is calculated from the QC
   * flags on its source {@link SensorValue}s by applying the {@link Variable}'s
   * flag cascade rules.
   * </p>
   *
   * @param instrument
   *          The {@link Instrument} that took the measurement.
   * @param measurement
   *          The {@link Measurement} being processed.
   * @param conn
   *          A database connection.
   * @return The data reduction result.
   * @see Variable#getCascade(SensorType, Flag, SensorAssignments)
   */
  public DataReductionRecord performDataReduction(Instrument instrument,
    Measurement measurement, Connection conn) throws DataReductionException {

    try {
      DataReductionRecord record = new DataReductionRecord(measurement,
        variable, getCalculationParameterNames());

      doCalculation(instrument, measurement, record, conn);

      Flag cascadeFlag = Flag.GOOD;
      LinkedHashMap<SensorType, String> messages = new LinkedHashMap<SensorType, String>();

      // Apply QC flags to the data reduction records
      for (SensorType sensorType : variable.getAllSensorTypes(true)) {

        MeasurementValue value = measurement.getMeasurementValue(sensorType);

        if (null != value) {
          // Collect all QC messages together. Do not record the same message
          // from
          // multiple sources.
          Flag valueFlag = variable.getCascade(value.getSensorType(),
            value.getQcFlag(), instrument.getSensorAssignments());

          if (!valueFlag.isGood()) {
            if (valueFlag.moreSignificantThan(cascadeFlag)) {
              cascadeFlag = valueFlag;
            }

            for (String qcMessage : value.getQcMessages()) {
              if (!messages.containsValue(qcMessage)) {
                messages.put(sensorType, qcMessage);
              }
            }
          }
        }
      }

      List<String> qcMessages = messages.entrySet().stream()
        .map(e -> e.getKey().getShortName() + " " + e.getValue()).toList();

      record.setQc(cascadeFlag, qcMessages);

      return record;
    } catch (Exception e) {
      if (e instanceof DataReductionException) {
        throw (DataReductionException) e;
      } else {
        throw new DataReductionException(e);
      }
    }
  }

  /**
   * Perform the data reduction calculations for the supplied
   * {@link Measurement} and add the results to the supplied
   * {@link DataReductionRecord}.
   *
   * @param instrument
   *          The {@link Instrument} that took the measurement.
   * @param measurement
   *          The {@link Measurement} being processed.
   * @param record
   *          The record to hold the calculation results.
   * @param conn
   *          A database connection.
   * @throws DataReductionException
   *           If any errors occur during calculation.
   */
  public abstract void doCalculation(Instrument instrument,
    Measurement measurement, DataReductionRecord record, Connection conn)
    throws DataReductionException;

  /**
   * Get the names of the calculation parameters generated by the reducer.
   *
   * <p>
   * The parameters are listed in display order.
   * </p>
   *
   * @return The calculation parameter names.
   */
  public List<String> getCalculationParameterNames() {
    return getCalculationParameters().stream()
      .map(CalculationParameter::getShortName).collect(Collectors.toList());
  }

  /**
   * Get a {@link Variable} property/attribute as a {@link Float} value.
   *
   * <p>
   * Returns {@code null} if the property is not present or non-numeric.
   * </p>
   *
   * @param property
   *          The property name.
   * @return The property value.
   */
  public Float getFloatProperty(String property) {
    Float result = null;

    try {
      if (properties.containsKey(variable.getName())) {
        String stringProp = properties.get(variable.getName())
          .getProperty(property);
        if (null != stringProp) {
          result = Float.parseFloat(stringProp);
        }
      }
    } catch (NumberFormatException e) {
      ExceptionUtils.printStackTrace(e);
      // Swallow the exception so that the result is null
    }

    return result;
  }

  /**
   * Get the calculation parameters generated by the reducer.
   *
   * <p>
   * The parameters are listed in display order.
   * </p>
   *
   * @return The calculation parameters.
   */
  public abstract List<CalculationParameter> getCalculationParameters();

  protected long makeParameterId(int parameterNumber) {
    return DataReducerFactory.makeParameterId(variable, parameterNumber);
  }

  /**
   * Perform any pre-processing actions required on a {@link DataSet} before
   * data reduction can be performed.
   *
   * <p>
   * The default implementation performs no action.
   * </p>
   *
   * @param conn
   *          A database connection.
   * @param instrument
   *          The {@link Instrument} to which the {@link DataSet} belongs.
   * @param dataset
   *          The {@link DataSet} being processed.
   * @param allMeasurements
   *          All the {@link Measurement}s found in the {@link DataSet}.
   * @throws DataReductionException
   *           If the pre-processing fails.
   */
  public void preprocess(Connection conn, Instrument instrument,
    DataSet dataset, List<Measurement> allMeasurements)
    throws DataReductionException {
    // The default is to do nothing
  }
}
