package uk.ac.exeter.QuinCe.data.Instrument;

@SuppressWarnings("serial")
public class InvalidInstrumentBasisException extends InstrumentException {

  public InvalidInstrumentBasisException(int basis) {
    super("Unrecognised instrument basis value " + basis);
  }

}
