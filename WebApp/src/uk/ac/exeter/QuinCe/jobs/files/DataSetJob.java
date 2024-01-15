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

/**
 * Abstract {@link Job} instance for jobs that operate on a {@link DataSet}.
 *
 * <p>
 * Most jobs perform work processing a {@link DataSet}, so this class provides
 * some convenience methods in addition to those from the base {@link Job}
 * class.
 * </p>
 */
public abstract class DataSetJob extends Job {

  /**
   * The parameter name for the {@link DataSet}'s database ID. All
   * {@link DataSetJob}s must contain a parameter with this ID in the properties
   * provided in the constructor.
   */
  public static final String ID_PARAM = "id";

  /**
   * The {@link DataSet} being processed.
   */
  private DataSet dataSet = null;

  /**
   * The {@link #dataSet}'s parent {@link Instrument}.
   */
  private Instrument instrument = null;

  /**
   * Initialise the job object so it is ready to run.
   *
   * @param resourceManager
   *          The system resource manager.
   * @param config
   *          The application configuration.
   * @param jobId
   *          The id of the job in the database.
   * @param properties
   *          The job properties (must contain at least the {@link DataSet}'s
   *          database ID).
   * @throws InvalidJobParametersException
   *           If the parameters are not valid for the job.
   * @throws MissingParamException
   *           If any of the parameters are invalid.
   * @throws RecordNotFoundException
   *           If the job record cannot be found in the database.
   * @throws DatabaseException
   *           If a database error occurs.
   */
  public DataSetJob(ResourceManager resourceManager, Properties config,
    long jobId, Properties properties) throws MissingParamException,
    InvalidJobParametersException, DatabaseException, RecordNotFoundException {
    super(resourceManager, config, jobId, properties);
    if (!properties.containsKey(ID_PARAM)) {
      throw new InvalidJobParametersException(
        "Missing dataset ID parameter from job properties");
    }
  }

  /**
   * Get the database ID of the {@link DataSet} this job is processing.
   *
   * @return The {@link DataSet}'s database ID.
   */
  protected long getDatsetId() {
    return Long.parseLong(properties.getProperty(ID_PARAM));
  }

  /**
   * Retrieve the {@link DataSet} that this job is processing from the database.
   *
   * @param conn
   *          A database connection.
   * @return The {@link DataSet}.
   * @throws JobFailedException
   *           If the {@link DataSet} cannot be retrieved.
   */
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

  /**
   * Get the parent {@link Instrument} of the {@link DataSet} being processed by
   * the job.
   *
   * @param conn
   *          A database connection.
   * @return The {@link Instrument}.
   * @throws JobFailedException
   *           If the {@link Instrument} cannot be retrieved.
   */
  protected Instrument getInstrument(Connection conn)
    throws JobFailedException {

    if (null == instrument) {
      try {
        instrument = InstrumentDB.getInstrument(conn,
          getDataset(conn).getInstrumentId());
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
