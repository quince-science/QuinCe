package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.utils.StringUtils;

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
   * The {@link Variable}'s attributes set during the instrument configuration.
   */
  protected Map<String, Properties> properties;

  /**
   * The Calculation Coefficients for the {@link DataSet} being processed.
   */
  protected CalibrationSet calculationCoefficients;

  /**
   * Simple constructor to initialise the reducer with the {@link Variable}
   * details.
   *
   * @param variable
   *          The {@link Variable}.
   * @param properties
   *          The {@link Variable}'s attributes.
   * @param calculationCoefficients
   *          The calculation coefficients specified for the instrument.
   */
  public DataReducer(Variable variable, Map<String, Properties> properties,
    CalibrationSet calculationCoefficients) {
    this.variable = variable;

    if (null == properties) {
      properties = new HashMap<String, Properties>();
    } else {
      this.properties = properties;
    }

    this.calculationCoefficients = calculationCoefficients;
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
   * @param allSensorValues
   *          The complete set of {@link SensorValue}s for the {@link DataSet}
   *          being processed.
   * @param conn
   *          A database connection.
   * @return The data reduction result.
   * @throws DataReductionException
   *           If an error occurs during the calculations.
   * @see Variable#getCascade(SensorType, Flag, SensorAssignments)
   */
  public DataReductionRecord performDataReduction(Instrument instrument,
    Measurement measurement, DatasetSensorValues allSensorValues,
    Connection conn) throws DataReductionException {

    try {
      DataReductionRecord record = new DataReductionRecord(measurement,
        variable, getCalculationParameterNames());

      doCalculation(instrument, measurement, record, conn);

      Flag cascadeFlag = Flag.GOOD;
      LinkedHashMap<SensorType, List<String>> messages = new LinkedHashMap<SensorType, List<String>>();

      // Apply QC flags to the data reduction records
      for (SensorType sensorType : variable.getAllSensorTypes(true)) {

        MeasurementValue value = measurement.getMeasurementValue(sensorType);

        if (null != value) {
          // Collect all QC messages together. Do not record the same message
          // from multiple sources.
          Flag valueFlag = variable.getCascade(value.getSensorType(),
            value.getQcFlag(allSensorValues),
            instrument.getSensorAssignments());

          if (!valueFlag.isGood()) {
            if (valueFlag.moreSignificantThan(cascadeFlag)) {
              cascadeFlag = valueFlag;
            }

            for (String qcMessage : value.getQcMessages()) {
              if (!messages.containsKey(sensorType)) {
                messages.put(sensorType, new ArrayList<String>());
              }
              if (!messages.get(sensorType).contains(qcMessage)) {
                messages.get(sensorType).add(qcMessage);
              }
            }
          }
        }
      }

      List<String> qcMessages = new ArrayList<String>();

      for (Map.Entry<SensorType, List<String>> entry : messages.entrySet()) {
        StringBuilder builder = new StringBuilder();
        builder.append(entry.getKey().getShortName());
        builder.append(' ');
        builder.append(StringUtils.collectionToDelimited(entry.getValue(), ";"));
        qcMessages.add(builder.toString());
      }

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
      String stringValue = getStringProperty(property);
      if (null != stringValue) {
        result = Float.parseFloat(stringValue);
      }
    } catch (NumberFormatException e) {
      ExceptionUtils.printStackTrace(e);
      // Swallow the exception so that the result is null
    }

    return result;
  }

  /**
   * Get a {@link Variable} property/attribute as a {@link String} value.
   *
   * <p>
   * Returns {@code null} if the property is not present.
   * </p>
   *
   * @param property
   *          The property name.
   * @return The property value.
   */
  public String getStringProperty(String property) {
    String result = null;

    if (properties.containsKey(variable.getName())) {
      result = properties.get(variable.getName()).getProperty(property);
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

  /**
   * Create a unique parameter number for parameter indices specified in
   * {@link #getCalculationParameters()}.
   *
   * <p>
   * The parameter numbers from {@link #getCalculationParameters()} are
   * typically numbered from zero, so parameters from different reducers will
   * clash. This method gives a processed version of a parameter ID that will be
   * unique to the {@link Variable} being processed.
   * </p>
   *
   * @param parameterNumber
   *          The basic parameter number.
   * @return The unique parameter number.
   * @see DataReducerFactory#makeParameterId(Variable, int)
   */
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
