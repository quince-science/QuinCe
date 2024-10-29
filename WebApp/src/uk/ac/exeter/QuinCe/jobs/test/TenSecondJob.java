package uk.ac.exeter.QuinCe.jobs.test;

import java.util.Properties;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.jobs.JobThread;
import uk.ac.exeter.QuinCe.jobs.NextJobInfo;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * A test job that runs in chunks of 10 seconds. It doesn't actually do
 * anything, but simply updates its progress after each 10 second chunk.
 */
public class TenSecondJob extends Job {

  /**
   * The parameter key for the number of chunks
   */
  public static final String CHUNK_KEY = "chunks";

  /**
   * The number of 10 second chunks in the job
   */
  private int chunkCount = 1;

  /**
   * Name of the job, used for reporting
   */
  private final String jobName = "Ten second test";

  /**
   * Constructs a job object, and validates the parameters passed to it
   *
   * @param resourceManager
   *          The system resource manager
   * @param config
   *          The application properties
   * @param id
   *          The id of the job in the database
   * @param parameters
   *          The parameters for the job
   * @throws InvalidJobParametersException
   *           If the parameters are not valid for the job
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public TenSecondJob(ResourceManager resourceManager, Properties config,
    long id, User owner, Properties properties)
    throws MissingParamException, InvalidJobParametersException {
    super(resourceManager, config, id, owner, properties);
    chunkCount = Integer.parseInt(properties.getProperty(CHUNK_KEY));
  }

  @Override
  protected NextJobInfo execute(JobThread thread) throws JobFailedException {
    for (int i = 0; i < chunkCount && !thread.isInterrupted(); i++) {
      System.out.println("Job " + id + ": Chunk " + i + " of " + chunkCount);
      try {
        Thread.sleep(10000);
        setProgress(((double) i / (double) chunkCount) * 100);
      } catch (InterruptedException e) {
        // Don't care
      } catch (Exception e) {
        throw new JobFailedException(id, e);
      }
    }

    return null;
  }

  @Override
  protected void validateParameters() throws InvalidJobParametersException {
    // For the test, we expect exactly one string, which is an integer
    if (null == properties) {
      throw new InvalidJobParametersException("parameters are null");
    } else if (properties.size() != 1) {
      throw new InvalidJobParametersException("Wrong number of parameters");
    } else {
      try {
        chunkCount = Integer.parseInt(properties.getProperty(CHUNK_KEY));
      } catch (NumberFormatException e) {
        throw new InvalidJobParametersException("It's not a number!");
      }
    }
  }

  @Override
  public String getJobName() {
    return jobName;
  }
}
