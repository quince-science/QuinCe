package uk.ac.exeter.QuinCe.web.datasets.export;

import java.sql.SQLException;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.MissingRunTypeException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC.ArgoManualQCData;

public class ArgoExportData extends ArgoManualQCData {

  public ArgoExportData(DataSource dataSource, Instrument instrument,
    DataSet dataset)
    throws SQLException, MissingRunTypeException, SensorTypeNotFoundException {
    super(dataSource, instrument, dataset);
  }

}
