package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

public class QCRoutinesConfiguration extends AbstractQCRoutinesConfiguration {

  public QCRoutinesConfiguration() {
    super();
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
