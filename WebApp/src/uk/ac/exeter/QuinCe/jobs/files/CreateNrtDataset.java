package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

import uk.ac.exeter.QuinCe.User.UserDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Files.DataFile;
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
   * Constructor that allows the {@link JobManager} to create an instance of
   * this job.
   *
   * @param resourceManager
   *          The application's resource manager
   * @param config
   *          The application configuration
   * @param jobId
   *          The database ID of the job
   * @param parameters
   *          The job parameters
   * @throws MissingParamException
   *           If any parameters are missing
   * @throws InvalidJobParametersException
   *           If any of the job parameters are invalid
   * @throws DatabaseException
   *           If a database occurs
   * @throws RecordNotFoundException
   *           If any required database records are missing
   * @see JobManager#getNextJob(ResourceManager, Properties)
   */
  public CreateNrtDataset(ResourceManager resourceManager, Properties config,
    long jobId, Properties properties) throws MissingParamException,
    InvalidJobParametersException, DatabaseException, RecordNotFoundException {
    super(resourceManager, config, jobId, properties);
  }

  @Override
  protected void execute(JobThread thread) throws JobFailedException {

    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      long instrumentId = Long.parseLong(properties.getProperty(ID_PARAM));

      Instrument instrument = InstrumentDB.getInstrument(conn, instrumentId,
        ResourceManager.getInstance().getSensorsConfiguration(),
        ResourceManager.getInstance().getRunTypeCategoryConfiguration());

      // Delete the existing NRT dataset
      DataSetDB.deleteNrtDataSet(conn, instrumentId);

      // Now create the new dataset

      // The NRT dataset will start immediately after the last 'real' dataset.
      // If there isn't one, it will start at the beginning of the first
      // available
      // data file.
      LocalDateTime nrtStartDate = null;
      DataSet lastDataset = DataSetDB.getLastDataSet(conn,
        instrument.getDatabaseId(), false);
      if (null != lastDataset) {
        nrtStartDate = lastDataset.getEnd().plusSeconds(1);
      } else {
        List<DataFile> instrumentFiles = DataFileDB.getFiles(conn,
          ResourceManager.getInstance().getConfig(),
          instrument.getDatabaseId());

        // We can only continue if there's at least one file
        if (instrumentFiles.size() > 0) {
          nrtStartDate = instrumentFiles.get(0).getStartDate();
        }
      }

      if (null != nrtStartDate) {
        LocalDateTime endDate = DataFileDB.getLastFileDate(conn,
          instrument.getDatabaseId());

        // Only create the NRT dataset if there are records available
        if (endDate.isAfter(nrtStartDate)) {
          String nrtDatasetName = buildNrtDatasetName(instrument);

          DataSet newDataset = new DataSet(instrument, nrtDatasetName,
            nrtStartDate, endDate, true);
          DataSetDB.addDataSet(conn, newDataset);

          // TODO This is a copy of the code in DataSetsBean.addDataSet. Does
          // it need collapsing?
          Properties jobProperties = new Properties();
          jobProperties.setProperty(ExtractDataSetJob.ID_PARAM,
            String.valueOf(newDataset.getId()));

          JobManager.addJob(conn, UserDB.getUser(conn, instrument.getOwnerId()),
            ExtractDataSetJob.class.getCanonicalName(), jobProperties);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      DatabaseUtils.rollBack(conn);
    } finally {
      DatabaseUtils.closeConnection(conn);
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
