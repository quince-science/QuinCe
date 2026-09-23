package uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC;

import java.sql.SQLException;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.MissingRunTypeException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;

/**
 * Factory class to automatically retrieve a {@link ManualQCData} object
 * suitable for a given {@link DataSet}.
 */
public class ManualQCDataFactory {

  public static ManualQCData getManualQCData(DataSource dataSource,
    Instrument instrument, DataSet dataset)
    throws SensorTypeNotFoundException, MissingRunTypeException, SQLException {

    switch (instrument.getBasis()) {
    case Instrument.BASIS_ARGO: {
      return new ArgoManualQCData(dataSource, instrument, dataset);
    }
    default:
      return new ManualQCData(dataSource, instrument, dataset);
    }
  }
}
