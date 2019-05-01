package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementsWithSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * A DataReducer will perform all data reduction calculations
 * for a given variable. The output from the data reduction is
 * an instance of the DataReductionRecord class
 * 
 * @author Steve Jones
 *
 */
public abstract class DataReducer {

  /**
   * Perform the data reduction and set up the QC flags
   * @param instrument The instrument that took the measurement
   * @param measurement The measurement
   * @param sensorValues The measurement's sensor values
   * @param allMeasurements All measurements for the data set
   * @return The data reduction result
   */
  public DataReductionRecord performDataReduction(Instrument instrument,
      Measurement measurement, HashMap<SensorType, TreeSet<SensorValue>> sensorValues,
      MeasurementsWithSensorValues allMeasurements) throws Exception {
    
    DataReductionRecord record = new DataReductionRecord(measurement);
    doCalculation(instrument, measurement, sensorValues, allMeasurements, record);
    
    return record;
  }
  
  /**
   * Perform the data reduction calculations
   * @param instrument The instrument that took the measurement
   * @param measurement The measurement
   * @param sensorValues The measurement's sensor values
   * @param allMeasurements All measurements for the data set
   * @param record The data reduction result
   */
  protected abstract void doCalculation(Instrument instrument,
      Measurement measurement, HashMap<SensorType, TreeSet<SensorValue>> sensorValues,
      MeasurementsWithSensorValues allMeasurements, DataReductionRecord record)
      throws Exception;
  
  /**
   * Gather all the calculation values to be used in data reduction
   * from the sensor values. All fallbacks/averaging/interpolation are
   * applied, and the QC flag to be applied to the final calculated value
   * @param sensorValues
   * @param sensorTypes
   * @return
   * @throws SensorTypeNotFoundException
   */
  protected CalculationInputValues getCalculationInputValues(Instrument instrument,
      HashMap<SensorType, TreeSet<SensorValue>> sensorValues,
      String... sensorTypes) throws SensorTypeNotFoundException {
    
    SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();
    SensorAssignments sensorAssignments = instrument.getSensorAssignments();
    

    // Work out all sensor types to get, accounting for dependencies and parents
    List<SensorType> actualSensorTypes = new ArrayList<SensorType>();
    
    for (String sensorTypeName : sensorTypes) {
      SensorType sensorType = sensorConfig.getSensorType(sensorTypeName);
      if (sensorConfig.isParent(sensorType)) {
        for (SensorType child : sensorConfig.getChildren(sensorType)) {
        
          if (sensorAssignments.isAssigned(child)) {
            actualSensorTypes.add(child);
            if (child.dependsOnOtherType()) {
              SensorType dependentOtherType = sensorConfig.getSensorType(child.getDependsOn());
              if (sensorAssignments.isAssigned(dependentOtherType)) {
                actualSensorTypes.add(dependentOtherType);
              }
            }
          }
        }
      } else {
        actualSensorTypes.add(sensorType);
        if (sensorType.dependsOnOtherType()) {
          SensorType dependentOtherType = sensorConfig.getSensorType(sensorType.getDependsOn());
          if (sensorAssignments.isAssigned(dependentOtherType)) {
            actualSensorTypes.add(dependentOtherType);
          }
        }
      }
    }

    // Now get the values for all the sensor types we've chosen
    CalculationInputValues result = new CalculationInputValues();
    
    for (SensorType sensorType : actualSensorTypes) {
      Set<SensorValue> values = sensorValues.get(sensorType);
      if (null == values) {
        result.put(sensorType.getName(), Double.NaN, Flag.BAD, "Missing " + sensorType.getName());
      } else {
        
        // Calculate the final value for the sensor type
        // along with the QC flag

        // TODO For the moment we just average all values and use the
        // worst flag. This needs to be made more sophisticated - skipping
        // bad values and using fallbacks, or interpolating, or whatever.
        
        Double valueTotal = 0.0;
        int count = 0;
        Flag qcFlag = Flag.ASSUMED_GOOD;
        List<String> qcMessages = null;
        
        for (SensorValue value : values) {
          if (!value.isNaN()) {
            valueTotal += value.getDoubleValue();
            count++;

            // Update the QC flag to be applied to the overall value
            if (value.getUserQCFlag().equals(Flag.NEEDED)) {
              
              if (!qcFlag.equals(Flag.NEEDED)) {
                qcFlag = Flag.NEEDED;
                qcMessages = new ArrayList<String>();
                qcMessages.add("AUTO QC: " + sensorType.getName() + " " + value.getUserQCMessage());
              } else if (value.getUserQCFlag().moreSignificantThan(qcFlag)) {
                qcFlag = value.getUserQCFlag();
                qcMessages = new ArrayList<String>();
                qcMessages.add(value.getUserQCMessage());
              } else if (value.getUserQCFlag().equals(qcFlag)) {
                qcMessages.add(value.getUserQCMessage());
              }
            }
          }
        }
        
        if (count == 0) {
          result.put(sensorType.getName(), Double.NaN, Flag.BAD,
            "Missing " + sensorType.getName());
        } else {
          result.put(sensorType.getName(), valueTotal / count, qcFlag, qcMessages);
        }
      }
    }

    return result;    
  }
  
  /**
   * Set a data reduction record's state for a missing required parameter
   * @param record The record
   * @param missingParameterName The name of the missing parameter
   */
  protected void makeMissingParameterRecord(
      DataReductionRecord record, String missingParameterName) {
    
    for (String parameter : getCalculationParameters()) {
      record.put(parameter, Double.NaN);
    }
    record.setQc(Flag.BAD, "Missing " + missingParameterName);
  }
  
  /**
   * Get the calculation parameters generated by the reducer, in
   * display order
   * @return The calculation parameters
   */
  protected abstract List<String> getCalculationParameters();
}
