package uk.ac.exeter.QuinCe.data.Dataset;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Files.DataFileException;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Factory class for {@link DataSetRawData} objects
 */
@Deprecated
public class DataSetRawDataFactory {

  /**
   * Construct a {@link DataSetRawData} object suitable for the averaging
   * mode of the supplied instrument
   * @param dataSource A data source
   * @param dataSet The data set whose raw data is to be processed
   * @param instrument The instrument to which the data set belongs
   * @return The {@link DataSetRawData} object
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   * @throws RecordNotFoundException If no data files are found within the data set
   * @throws DataFileException If the data cannot be extracted from the files
   * @throws DataSetException If a suitable object cannot be created
   * @throws InstrumentException
   * @throws CalibrationException
   */
  public static DataSetRawData getDataSetRawData(DataSource dataSource,
    DataSet dataSet, Instrument instrument) throws MissingParamException,
    DatabaseException, RecordNotFoundException, DataFileException,
    DataSetException, CalibrationException, InstrumentException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(dataSet, "dataSet");
    MissingParam.checkMissing(instrument, "Instrument");


    DataSetRawData result = null;

    switch (instrument.getAveragingMode()) {
    case DataSetRawData.AVG_MODE_NONE: {
      result = new NoAverageDataSetRawData(dataSource, dataSet, instrument);
      break;
    }
    case DataSetRawData.AVG_MODE_MINUTE: {
      throw new DataSetException("Minute averager hasn't been written yet!");
    }
    default: {
      throw new DataSetException("Unrecognised averaging mode " + instrument.getAveragingMode());
    }
    }

    return result;
  }

}
