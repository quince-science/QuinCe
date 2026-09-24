package uk.ac.exeter.QuinCe.data.Dataset;

@SuppressWarnings("serial")
public class ColumnHeadingNotFoundException extends Exception {

  /**
   * Sensor name not found
   *
   * @param sensorName
   *          The sensor name
   */
  public ColumnHeadingNotFoundException(String sensorName) {
    super("Item with name '" + sensorName + "' does not exist");
  }

  /**
   * Sensor ID not found
   *
   * @param sensorId
   *          The sensor ID
   */
  public ColumnHeadingNotFoundException(long sensorId) {
    super("Item with ID " + sensorId + " does not exist");
  }

  /**
   * Special case when looking up something based on a {@link ColumnHeading}
   * object.
   *
   * @param columnHeading
   *          The columnHeading we were looking for.
   */
  public ColumnHeadingNotFoundException(ColumnHeading columnHeading) {
    super("The object for " + columnHeading.getId() + ":"
      + columnHeading.getShortName() + " is not valid");
  }

  protected String getItemName() {
    return "Column Heading";
  }

  @Override
  public String getMessage() {
    return getItemName() + ": " + super.getMessage();
  }

}
