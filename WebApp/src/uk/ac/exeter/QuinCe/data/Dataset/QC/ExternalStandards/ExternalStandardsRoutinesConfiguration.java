package uk.ac.exeter.QuinCe.data.Dataset.QC.ExternalStandards;

import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.AbstractAutoQCRoutine;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.AbstractQCRoutinesConfiguration;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.QCRoutinesConfigurationException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Configuration for the Auto QC routines run on values in External Standard
 * runs.
 * 
 * <p>
 * These routines are similar in structure to the normal Auto QC routines,
 * except that they are only run on {@link SensorValue}s that have internal
 * calibrations and taken during calibration runs as opposed to measurement
 * runs.
 * </p>
 */
public class ExternalStandardsRoutinesConfiguration
  extends AbstractQCRoutinesConfiguration {

  public ExternalStandardsRoutinesConfiguration(
    SensorsConfiguration sensorsConfig, String configFile)
    throws QCRoutinesConfigurationException, MissingParamException {

    super(sensorsConfig, configFile);
  }

  @Override
  protected String getRoutineClassPackage() {
    return "ExternalStandards";
  }

  @Override
  protected Class<? extends AbstractAutoQCRoutine> getRoutineSuperClass() {
    return ExternalStandardsQCRoutine.class;
  }
}
