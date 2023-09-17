package uk.ac.exeter.QuinCe.data.Dataset;

@SuppressWarnings("serial")
public class IncorrectValueTypeException extends RuntimeException {

  private int wrongType;

  public IncorrectValueTypeException(int wrongType) {
    super();
    this.wrongType = wrongType;

  }

  @Override
  public String getMessage() {
    if (wrongType == SensorValuesListValue.STRING_TYPE) {
      return "Not a String value";
    } else {
      return "Not a Double value";
    }
  }
}
