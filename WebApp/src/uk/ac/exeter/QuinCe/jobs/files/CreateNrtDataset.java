package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.TimeDataSet;
import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Files.TimeDataFile;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.JobThread;
import uk.ac.exeter.QuinCe.jobs.NextJobInfo;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
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
    long jobId, User owner, Properties properties) throws MissingParamException,
    InvalidJobParametersException, DatabaseException, RecordNotFoundException {
    super(resourceManager, config, jobId, owner, properties);
  }

  @Override
  protected NextJobInfo execute(JobThread thread) throws JobFailedException {

    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      conn.setAutoCommit(false);
      long instrumentId = Long.parseLong(properties.getProperty(ID_PARAM));

      Instrument instrument = InstrumentDB.getInstrument(conn, instrumentId);

      // Delete the existing NRT dataset
      DataSetDB.deleteNrtDataSet(conn, instrumentId);

      // Now create the new dataset

      // The NRT dataset will start immediately after the last 'real' dataset.
      // If there isn't one, it will start at the beginning of the first
      // available data file.
      LocalDateTime nrtStartDate = null;
      TimeDataSet lastDataset = (TimeDataSet) DataSetDB.getLastDataSet(conn,
        instrument.getId(), false);

      TreeSet<DataFile> instrumentFiles = DataFileDB.getFiles(conn, instrument);

      if (null != lastDataset) {
        nrtStartDate = lastDataset.getEndTime().plusSeconds(1);
      } else {
        // We can only continue if there's at least one file
        if (instrumentFiles.size() > 0) {
          nrtStartDate = ((TimeDataFile) instrumentFiles.first())
            .getOffsetStartTime();
        }
      }

      if (null != nrtStartDate) {
        TreeSet<DataFile> files = DataFileDB.getFiles(conn, instrument);

        LocalDateTime endDate = ((TimeDataFile) files.last()).getRawEndTime();

        boolean canCreateNrt = true;

        if (!endDate.isAfter(nrtStartDate)) {
          canCreateNrt = false;
        } else if (!TimeDataFile.hasConcurrentFiles(instrument, instrumentFiles,
          nrtStartDate, endDate)) {
          canCreateNrt = false;
        }

        // Only create the NRT dataset if there are records available
        if (canCreateNrt) {
          String nrtDatasetName = buildNrtDatasetName(instrument);

          DataSet newDataset = new TimeDataSet(instrument, nrtDatasetName,
            nrtStartDate, endDate, true);
          DataSetDB.addDataSet(conn, newDataset);

          // TODO This is a copy of the code in DataSetsBean.addDataSet. Does
          // it need collapsing?
          Properties jobProperties = new Properties();
          jobProperties.setProperty(DataSetJob.ID_PARAM,
            String.valueOf(newDataset.getId()));

          JobManager.addJob(conn, instrument.getOwner(),
            ExtractDataSetJob.class.getCanonicalName(), jobProperties);
        }
      }

      conn.commit();
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
      DatabaseUtils.rollBack(conn);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return null;
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
