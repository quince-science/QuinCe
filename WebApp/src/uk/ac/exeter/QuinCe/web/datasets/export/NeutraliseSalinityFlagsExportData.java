package uk.ac.exeter.QuinCe.web.datasets.export;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Export.ExportOption;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;

public class NeutraliseSalinityFlagsExportData extends ExportData {

  public NeutraliseSalinityFlagsExportData(DataSource dataSource,
    Instrument instrument, DataSet dataSet, ExportOption exportOption)
    throws Exception {
    super(dataSource, instrument, dataSet, exportOption);
  }

  @Override
  public void postProcess() {
    System.out.println("DEATH TO SALINITY FLAGS!");
  }

}
