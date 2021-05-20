package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;

/**
 * A dummy {@link MeasurementLocator} that returns no measurements.
 *
 * <p>
 * This can be used when multiple variables are processed by a single
 * {@link MeasurementLocator} in one pass, so the other variables don't need
 * processing.
 * </p>
 *
 * @author stevej
 *
 */
public class DummyMeasurementLocator extends MeasurementLocator {

  @Override
  public List<Measurement> locateMeasurements(Connection conn,
    Instrument instrument, DataSet dataset) throws MeasurementLocatorException {

    return new ArrayList<Measurement>();
  }

}
