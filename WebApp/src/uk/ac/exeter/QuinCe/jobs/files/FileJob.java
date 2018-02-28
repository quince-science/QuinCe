package uk.ac.exeter.QuinCe.jobs.files;

import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.jobs.JobThread;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * A version of the Job class specifically for jobs operating on data files.
 *
 * <p>
 *   Before the job is run, the file is checked to see if it has been marked for deletion.
 *   It is up to the concrete implementation of a {@code FileJob} to decide if it wants to
 *   check the file again during processing.
 * </p>
 *
 * @author Steve Jones
 * @see Job
 */
public abstract class FileJob extends Job {

  /**
   * The parameter key that holds the data file's database ID
   */
  public static final String FILE_ID_KEY = "FILE_ID";

  /**
   * The ID of the data file
   */
  protected long fileId;

  /**
   * The instrument to which the data file being processed belongs
   */
  protected Instrument instrument = null;

  /**
   * Construct a Job to run on a data file.
   *
   * <p>
   *   The first entry in the {@code parameters} list must be the
   *   database ID of the file to be processed.
   * </p>
   *
   * @param resourceManager The system resource manager
   * @param config The application configuration
   * @param jobId The job's database ID
   * @param parameters The job parameters
   * @throws MissingParamException If any constructor parameters are missing
   * @throws InvalidJobParametersException If the supplied job parameters are invalid
   * @throws DatabaseException If a database error occurs
   * @throws RecordNotFoundException If any referenced database records are missing
   */
  public FileJob(ResourceManager resourceManager, Properties config, long jobId, Map<String, String> parameters) throws MissingParamException, InvalidJobParametersException, DatabaseException, RecordNotFoundException {
    super(resourceManager, config, jobId, parameters);

    // The file ID and instrument are extracted in the validateParameters method
  }

  /**
   * Check that the supplied file ID exists.
   * It's not strictly a parameter, but it's a handy place to do it.
   */
  protected void validateParameters() throws InvalidJobParametersException {
    if (!parameters.containsKey(FILE_ID_KEY)) {
      throw new InvalidJobParametersException("Data file not specified");
    }

    try {
      // TODO Reinstate
      /*
      fileId = Long.parseLong(parameters.get(FILE_ID_KEY));
      instrument = InstrumentDB.getInstrumentByFileId(dataSource, fileId);

      if (!DataFileDB.fileExists(dataSource, fileId)) {
        throw new InvalidJobParametersException("The data file " + fileId + " does not exist");
      }
      */
    } catch (Exception e) {
      throw new InvalidJobParametersException("An unexpected error occurred: " + e.getMessage());
    }
  }

  @Override
  protected final void execute(JobThread thread) throws JobFailedException {
    try {
      executeFileJob(thread);
    } catch (JobFailedException e) {
      throw e;
    } catch (Exception e) {
      throw new JobFailedException(id, "Error while performing checks for file job", e);
    }
  }

  /**
   * Perform the file job tasks
   * @param thread The thread that will be running the job
   * @throws JobFailedException If an error occurs during the job
   */
  protected abstract void executeFileJob(JobThread thread) throws JobFailedException;
}
