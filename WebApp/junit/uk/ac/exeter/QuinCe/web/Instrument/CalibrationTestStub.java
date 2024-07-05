package uk.ac.exeter.QuinCe.web.Instrument;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.Calibration;

public class CalibrationTestStub extends Calibration {

  protected CalibrationTestStub(Instrument instrument, LocalDateTime date) {
    super(instrument, "TEST_TYPE", date);
  }

  @Override
  public List<String> getCoefficientNames() {
    ArrayList<String> names = new ArrayList<String>(1);
    names.add("Calibration Test Stub Coefficient");
    return names;
  }

  @Override
  protected String buildHumanReadableCoefficients() {
    return "Calibration Test Stub Human Readable Coefficients";
  }

  @Override
  public boolean coefficientsValid() {
    return true;
  }

  @Override
  public Double calibrateValue(Double rawValue) {
    return 0.0;
  }
}
