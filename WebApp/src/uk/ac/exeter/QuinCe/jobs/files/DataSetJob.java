package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public abstract class DataSetJob extends Job {

  /**
   * The parameter name for the data set id
   */
  public static final String ID_PARAM = "id";

  private DataSet dataSet = null;

  private Instrument instrument = null;

  /**
   * Initialise the job object so it is ready to run
   *
   * @param resourceManager
   *          The system resource manager
   * @param config
   *          The application configuration
   * @param jobId
   *          The id of the job in the database
   * @param parameters
   *          The job parameters, containing the file ID
   * @throws InvalidJobParametersException
   *           If the parameters are not valid for the job
   * @throws MissingParamException
   *           If any of the parameters are invalid
   * @throws RecordNotFoundException
   *           If the job record cannot be found in the database
   * @throws DatabaseException
   *           If a database error occurs
   */
  public DataSetJob(ResourceManager resourceManager, Properties config,
    long jobId, Properties properties) throws MissingParamException,
    InvalidJobParametersException, DatabaseException, RecordNotFoundException {
    super(resourceManager, config, jobId, properties);
  }

  protected long getDatsetId() {
    return Long.parseLong(properties.getProperty(ID_PARAM));
  }

  protected DataSet getDataset(Connection conn) throws JobFailedException {
    if (null == dataSet) {
      try {
        dataSet = DataSetDB.getDataSet(conn, getDatsetId());
      } catch (Exception e) {
        throw new JobFailedException(id, "Error getting job dataset", e);
      }
    }

    return dataSet;
  }

  protected Instrument getInstrument(Connection conn)
    throws JobFailedException {

    if (null == instrument) {
      try {
        instrument = InstrumentDB.getInstrument(conn,
          getDataset(conn).getInstrumentId(),
          ResourceManager.getInstance().getSensorsConfiguration(),
          ResourceManager.getInstance().getRunTypeCategoryConfiguration());
      } catch (Exception e) {
        throw new JobFailedException(id, "Error getting instrument", e);
      }
    }

    return instrument;
  }

  @Override
  protected void validateParameters() throws InvalidJobParametersException {

    String datasetIdString = properties.getProperty(ID_PARAM);
    if (null == datasetIdString) {
      throw new InvalidJobParametersException(ID_PARAM + "is missing");
    }

    try {
      Long.parseLong(datasetIdString);
    } catch (NumberFormatException e) {
      throw new InvalidJobParametersException(ID_PARAM + "is not numeric");
    }
  }
}
