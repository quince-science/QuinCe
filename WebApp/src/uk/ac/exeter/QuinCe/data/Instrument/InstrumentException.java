package uk.ac.exeter.QuinCe.data.Instrument;

@SuppressWarnings("serial")
public class InstrumentException extends Exception {

  public InstrumentException(String message) {
    super(message);
  }

  public InstrumentException(String message, Throwable cause) {
    super(message);
  }
}
