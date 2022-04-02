package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

public class QCRoutinesConfiguration extends AbstractQCRoutinesConfiguration {

  /**
   * Main constructor - parses supplied config file and builds all Routine
   * objects.
   *
   * @param configFile
   *          The configuration file
   * @throws QCRoutinesConfigurationException
   *           If the configuration is invalid
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public QCRoutinesConfiguration(SensorsConfiguration sensorsConfig,
    String configFile)
    throws QCRoutinesConfigurationException, MissingParamException {

    super(sensorsConfig, configFile);
  }

  protected Class<? extends AbstractAutoQCRoutine> getRoutineSuperClass() {
    return AutoQCRoutine.class;
  }

  /**
   * Get the name of the package in which all sensor value routine classes will
   * be.
   * 
   * <p>
   * This must be a child of the package returned by
   * {@link #getRoutineClassRoot()}.
   * </p>
   * 
   * @return The name of the package containing QC routines.
   */
  protected String getRoutineClassPackage() {
    return "SensorValues";
  }
}
