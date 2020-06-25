package uk.ac.exeter.QuinCe.web.datasets.export;

import java.sql.SQLException;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.web.datasets.data.MeasurementDataException;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC.ManualQC2Data;

/**
 * A representation of a dataset's data for export.
 *
 * <p>
 * Based on the data for the {@link ManualQC2Data} for manual QC pages, since
 * that contains everything we need.
 * </p>
 *
 * @author Steve Jones
 *
 */
public class Export2Data extends ManualQC2Data {

  public Export2Data(DataSource dataSource, Instrument instrument,
    DataSet dataset) throws SQLException {
    super(dataSource, instrument, dataset);
  }

  @Override
  public void loadData() {
    try {
      loadDataAction();
      loaded = true;
    } catch (Exception e) {
      error("Error while loading dataset data", e);
    }
  }

  /**
   * Post-process the data before it's finally exported. This instance does no
   * post-processing, but extending classes can override it as needed.
   */
  public void postProcess() throws MeasurementDataException {
    // NOOP
  }
}
