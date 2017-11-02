package uk.ac.exeter.QuinCe.jobs.files;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetRawData;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetRawDataFactory;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetRawDataRecord;
import uk.ac.exeter.QuinCe.data.Dataset.InvalidDataSetStatusException;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.jobs.JobThread;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Job to extract the data for a data set from the uploaded data files
 * @author Steve Jones
 *
 */
public class ExtractDataSetJob extends Job {

	/**
	 * The parameter name for the data set id
	 */
	public static final String ID_PARAM = "id";
	
	/**
	 * The data set being processed by the job
	 */
	private DataSet dataSet = null;
	
	/**
	 * The instrument to which the data set belongs
	 */
	private Instrument instrument = null;
	
	/**
	 * Initialise the job object so it is ready to run
	 * 
	 * @param resourceManager The system resource manager
	 * @param config The application configuration
	 * @param jobId The id of the job in the database
	 * @param parameters The job parameters, containing the file ID
	 * @throws InvalidJobParametersException If the parameters are not valid for the job
	 * @throws MissingParamException If any of the parameters are invalid
	 * @throws RecordNotFoundException If the job record cannot be found in the database
	 * @throws DatabaseException If a database error occurs
	 */
	public ExtractDataSetJob(ResourceManager resourceManager, Properties config, long jobId, Map<String, String> parameters) throws MissingParamException, InvalidJobParametersException, DatabaseException, RecordNotFoundException {
		super(resourceManager, config, jobId, parameters);
	}

	@Override
	protected void execute(JobThread thread) throws JobFailedException {
		try {
			// Get the data set from the database
			dataSet = DataSetDB.getDataSet(dataSource, Long.parseLong(parameters.get(ID_PARAM)));
			
			// Reset the data set and all associated data
			reset();
			
			// Set processing status
			DataSetDB.setDatasetStatus(dataSource, dataSet, DataSet.STATUS_DATA_EXTRACTION);

			// Get related data
			instrument = InstrumentDB.getInstrument(dataSource, dataSet.getInstrumentId(), resourceManager.getSensorsConfiguration(), resourceManager.getRunTypeCategoryConfiguration());
			
			DataSetRawData rawData = DataSetRawDataFactory.getDataSetRawData(dataSource, dataSet, instrument);
			
			DataSetRawDataRecord record = rawData.getNextRecord();
			while (null != record) {

				for (Map.Entry<SensorType, Set<SensorAssignment>> entry : instrument.getSensorAssignments().entrySet()) {
					
					SensorType sensorType = entry.getKey();
					Set<SensorAssignment> assignments = entry.getValue();
					
					if (sensorType.isUsedInCalculation()) {
						
						double primarySensorTotal = 0.0;
						int primarySensorCount = 0;
						
						double fallbackSensorTotal = 0.0;
						int fallbackSensorCount = 0;
						
						for (SensorAssignment assignment : assignments) {
							Double sensorValue = rawData.getSensorValue(assignment);
							if (null != sensorValue) {
								if (assignment.isPrimary()) {
									primarySensorTotal += sensorValue;
									primarySensorCount++;
								} else {
									fallbackSensorTotal+= sensorValue;
									fallbackSensorCount++;
								}
							}
						}
						
						Double finalSensorValue = null;
						
						if (primarySensorCount > 0) {
							finalSensorValue = new Double(primarySensorTotal / primarySensorCount);
						} else if (fallbackSensorCount > 0) {
							finalSensorValue = new Double(fallbackSensorTotal / fallbackSensorCount);
						}

						record.setValue(sensorType.getName(), finalSensorValue);
					} else {
						for (SensorAssignment assignment : assignments) {
							record.setDiagnosticValue(assignment.getSensorName(), rawData.getSensorValue(assignment));
						}
					}
					
				}
				
				
				
				// Read the next record
				record = rawData.getNextRecord();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new JobFailedException(id, e);
		}
	}

	@Override
	protected void validateParameters() throws InvalidJobParametersException {
		// TODO Auto-generated method stub
	}

	/**
	 * Reset the data set processing.
	 * 
	 * Delete all related records and reset the status
	 * @throws MissingParamException If any of the parameters are invalid
	 * @throws InvalidDataSetStatusException If the method sets an invalid data set status
	 * @throws DatabaseException If a database error occurs
	 */
	private void reset() throws MissingParamException, InvalidDataSetStatusException, DatabaseException {
		DataSetDB.setDatasetStatus(dataSource, dataSet, DataSet.STATUS_WAITING);
	}
}
