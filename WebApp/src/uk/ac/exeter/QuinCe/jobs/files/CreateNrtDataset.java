package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.User.UserDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.JobThread;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class CreateNrtDataset extends Job {

  /**
   * The parameter name for the data set id
   */
  public static final String ID_PARAM = "id";

  /**
   * Constructor that allows the {@link JobManager} to create an instance of this job.
   * @param resourceManager The application's resource manager
   * @param config The application configuration
   * @param jobId The database ID of the job
   * @param parameters The job parameters
   * @throws MissingParamException If any parameters are missing
   * @throws InvalidJobParametersException If any of the job parameters are invalid
   * @throws DatabaseException If a database occurs
   * @throws RecordNotFoundException If any required database records are missing
   * @see JobManager#getNextJob(ResourceManager, Properties)
   */
  public CreateNrtDataset(ResourceManager resourceManager, Properties config, long jobId, Map<String, String> parameters) throws MissingParamException, InvalidJobParametersException, DatabaseException, RecordNotFoundException {
    super(resourceManager, config, jobId, parameters);
  }

  @Override
  protected void execute(JobThread thread) throws JobFailedException {

    Connection conn = null;

    try {

      conn = dataSource.getConnection();

      long instrumentId = Long.parseLong(parameters.get(ID_PARAM));
      Instrument instrument = InstrumentDB.getInstrument(conn, instrumentId,
        ResourceManager.getInstance().getSensorsConfiguration(),
        ResourceManager.getInstance().getRunTypeCategoryConfiguration());

      // Delete the existing NRT dataset
      DataSetDB.deleteNrtDataSet(conn, instrumentId);

      // Now create the new dataset

      // Default to 1st Jan 1900, or immediately after the last dataset.
      // The real dataset date will be adjusted when the records are extracted
      LocalDateTime nrtStartDate = LocalDateTime.of(1900, 1, 1, 0, 0, 0);
      DataSet lastDataset = DataSetDB.getLastDataSet(conn, instrument.getDatabaseId());
      if (null != lastDataset) {
        nrtStartDate = lastDataset.getEnd().plusSeconds(1);
      }

      LocalDateTime endDate = DataFileDB.getLastFileDate(conn, instrument.getDatabaseId());
      String nrtDatasetName = buildNrtDatasetName(instrument);
      DataSet newDataset = new DataSet(instrument.getDatabaseId(), nrtDatasetName, nrtStartDate, endDate, true);
      DataSetDB.addDataSet(conn, newDataset);

      // TODO This is a copy of the code in DataSetsBean.addDataSet. Does it need collapsing?
      Map<String, String> params = new HashMap<String, String>();
      params.put(ExtractDataSetJob.ID_PARAM, String.valueOf(newDataset.getId()));

      JobManager.addJob(conn, UserDB.getUser(conn, instrument.getOwnerId()),
          ExtractDataSetJob.class.getCanonicalName(), params);

    } catch (Exception e) {
      e.printStackTrace();
      DatabaseUtils.rollBack(conn);
    }

  }

  private String buildNrtDatasetName(Instrument instrument) {
    StringBuilder result = new StringBuilder("NRT");
    result.append(instrument.getPlatformCode());
    result.append(System.currentTimeMillis());
    return result.toString();
  }

  @Override
  protected void validateParameters() throws InvalidJobParametersException {
    // TODO Auto-generated method stub
  }

  @Override
  public String getJobName() {
    return "Create NRT Dataset";
  }
}
