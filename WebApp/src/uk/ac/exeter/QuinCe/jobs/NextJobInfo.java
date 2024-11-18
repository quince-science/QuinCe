package uk.ac.exeter.QuinCe.jobs;

import java.util.HashMap;
import java.util.Properties;

/**
 * Class to contain information about a {@link Job} that should be executed as
 * an immediate follow-on from a previous {@link Job}.
 */
public class NextJobInfo {

  protected final String jobClass;

  protected final Properties properties;

  protected HashMap<String, Object> transferData = new HashMap<String, Object>();

  public NextJobInfo(String jobClass, Properties properties)
    throws InvalidJobClassTypeException {
    int classCheckResult = JobManager.checkJobClass(jobClass);
    if (classCheckResult != JobManager.CLASS_CHECK_OK) {
      throw new InvalidJobClassTypeException(jobClass);
    }

    this.jobClass = jobClass;
    this.properties = properties;
  }

  public void putTransferData(String key, Object data) {
    transferData.put(key, data);
  }

  public HashMap<String, Object> getTransferData() {
    return transferData;
  }
}
