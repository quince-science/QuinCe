package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.data.Calculation.DataReductionDB;
import uk.ac.exeter.QuinCe.data.Calculation.RawDataDB;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Files.FileInfo;
import uk.ac.exeter.QuinCe.data.Files.RawDataFile;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorCode;
import uk.ac.exeter.QuinCe.data.QC.QCDB;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.JobThread;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * This job extracts the data from uploaded files and
 * adds it into the database ready for processing.
 * 
 * <p>
 *   Gas standards, and measurements are extracted from the file
 *   and loaded into the appropriate database tables. Empty QC database
 *   records are created ready for the data reduction/auto QC jobs
 *   to fill them in.
 * </p>
 * <p>  
 *   Sensor calibrations are applied to the measurements as they are loaded.
 * </p>
 * <p>
 *   Once the job is complete, the {@link TrimFlushingJob} is queued to run
 *   on the file's data.
 * </p>
 * @author Steve Jones
 *
 */
@Deprecated
public class ExtractRawDataJob extends FileJob {

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
	public ExtractRawDataJob(ResourceManager resourceManager, Properties config, long jobId, Map<String, String> parameters) throws MissingParamException, InvalidJobParametersException, DatabaseException, RecordNotFoundException {
		super(resourceManager, config, jobId, parameters);
	}

	/**
	 * Performs the raw data extraction.
	 */
	@Override
	protected void executeFileJob(JobThread thread) throws JobFailedException {
		
		/*
		
		reset();
		Connection conn = null;
		int lineNumber = 0;
		
		try {
			RawDataFile inData = DataFileDB.getRawDataFile(dataSource, config, fileId);
			List<List<String>> data = inData.getContents();

			Instrument instrument = InstrumentDB.getInstrumentByFileId(dataSource, fileId);
			
			List<Calendar> fileDates = inData.getDates(null);
			List<CalibrationStub> fileCalibrations = CalibrationDB.getCalibrationsForFile(dataSource, instrument.getDatabaseId(), fileDates.get(0), fileDates.get(fileDates.size() - 1));

			int currentCalibration = 0;
			List<CalibrationCoefficients> currentCoefficients = CalibrationDB.getCalibrationCoefficients(dataSource, fileCalibrations.get(currentCalibration));
			
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			lineNumber = 0;
			for (List<String> line : data) {
				
				if (thread.isInterrupted()) {
					break;
				}
				
				lineNumber++;
				
				String runType = line.get(instrument.getColumnAssignment(Instrument.COL_RUN_TYPE));
				if (instrument.isMeasurementRunType(runType) || instrument.isStandardRunType(runType)) {
				
					// Check that we're still using the right calibration
					if (currentCalibration + 1 < fileCalibrations.size()) {
						Calendar lineDate = instrument.getDateFromLine(line);
						Date nextCalibrationDate = fileCalibrations.get(currentCalibration + 1).getDate();
						if (nextCalibrationDate.getTime() < lineDate.getTimeInMillis()) {
							currentCalibration++;
							currentCoefficients = CalibrationDB.getCalibrationCoefficients(dataSource, fileCalibrations.get(currentCalibration));
						}
					}
					
					applyCalibrations(line, instrument, currentCoefficients);
					RawDataDB.storeRawData(conn, instrument, fileId, lineNumber, line);
					
					if (instrument.isMeasurementRunType(runType)) {
						QCDB.createQCRecord(conn, fileId, lineNumber, instrument);
						
						// Store empty data reduction values (unless other values have previously been stored)
						DataReductionDB.storeRow(conn, fileId, lineNumber, false, instrument.getRunTypeCode(runType), RawDataDB.MISSING_VALUE,
								RawDataDB.MISSING_VALUE, RawDataDB.MISSING_VALUE, RawDataDB.MISSING_VALUE, RawDataDB.MISSING_VALUE, 
								RawDataDB.MISSING_VALUE, RawDataDB.MISSING_VALUE, RawDataDB.MISSING_VALUE, RawDataDB.MISSING_VALUE,
								RawDataDB.MISSING_VALUE, RawDataDB.MISSING_VALUE, RawDataDB.MISSING_VALUE, RawDataDB.MISSING_VALUE);
					}
				}
				
				if (lineNumber % 100 == 0) {
					setProgress((double) lineNumber / (double) data.size() * 100.0);
				}
			}
			
			// If the thread was interrupted, reset everything an requeue the job.
			// Otherwise commit all changes and queue the Trim Flushing job
			if (thread.isInterrupted()) {
				conn.rollback();
				reset();
			} else {
				// Save the created records
				conn.commit();
				
				// Queue up the Initial Check job
				Map<String, String> nextJobParams = AutoQCJob.getJobParameters(FileInfo.JOB_CODE_INITIAL_CHECK, fileId);
				User owner = JobManager.getJobOwner(dataSource, id);
				JobManager.addJob(conn, owner, FileInfo.getJobClass(FileInfo.JOB_CODE_INITIAL_CHECK), nextJobParams);
				DataFileDB.setCurrentJob(conn, fileId, FileInfo.JOB_CODE_INITIAL_CHECK);
				conn.commit();
			}
		} catch (Exception e) {
			DatabaseUtils.rollBack(conn);
			throw new JobFailedException(id, lineNumber, e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
		
		*/
	}
	
	/**
	 * Apply all the calibrations to the sensor values present in a given line from
	 * the raw data file
	 * @param line The line from the file
	 * @param instrument The instrument with which the data is associated
	 * @param coefficients The calibration coefficients to be applied
	 */
	/*
	private void applyCalibrations(List<String> line, Instrument instrument, List<CalibrationCoefficients> coefficients) {
		if (instrument.hasIntakeTemp1()) {
			SensorCode code = new SensorCode(SensorCode.TYPE_INTAKE_TEMP, 1, instrument);
			applyCoefficients(line, instrument.getColumnAssignment(Instrument.COL_INTAKE_TEMP_1), code, coefficients);
		}
		
		if (instrument.hasIntakeTemp2()) {
			SensorCode code = new SensorCode(SensorCode.TYPE_INTAKE_TEMP, 2, instrument);
			applyCoefficients(line, instrument.getColumnAssignment(Instrument.COL_INTAKE_TEMP_2), code, coefficients);
		}
		
		if (instrument.hasIntakeTemp3()) {
			SensorCode code = new SensorCode(SensorCode.TYPE_INTAKE_TEMP, 3, instrument);
			applyCoefficients(line, instrument.getColumnAssignment(Instrument.COL_INTAKE_TEMP_3), code, coefficients);
		}
		
		if (instrument.hasSalinity1()) {
			SensorCode code = new SensorCode(SensorCode.TYPE_SALINITY, 1, instrument);
			applyCoefficients(line, instrument.getColumnAssignment(Instrument.COL_SALINITY_1), code, coefficients);
		}
		
		if (instrument.hasSalinity2()) {
			SensorCode code = new SensorCode(SensorCode.TYPE_SALINITY, 2, instrument);
			applyCoefficients(line, instrument.getColumnAssignment(Instrument.COL_SALINITY_2), code, coefficients);
		}
		
		if (instrument.hasSalinity3()) {
			SensorCode code = new SensorCode(SensorCode.TYPE_SALINITY, 3, instrument);
			applyCoefficients(line, instrument.getColumnAssignment(Instrument.COL_SALINITY_3), code, coefficients);
		}
		
		if (instrument.hasEqt1()) {
			SensorCode code = new SensorCode(SensorCode.TYPE_EQT, 1, instrument);
			applyCoefficients(line, instrument.getColumnAssignment(Instrument.COL_EQT_1), code, coefficients);
		}
		
		if (instrument.hasEqt2()) {
			SensorCode code = new SensorCode(SensorCode.TYPE_EQT, 2, instrument);
			applyCoefficients(line, instrument.getColumnAssignment(Instrument.COL_EQT_2), code, coefficients);
		}
		
		if (instrument.hasEqt3()) {
			SensorCode code = new SensorCode(SensorCode.TYPE_EQT, 3, instrument);
			applyCoefficients(line, instrument.getColumnAssignment(Instrument.COL_EQT_3), code, coefficients);
		}
		
		if (instrument.hasEqp1()) {
			SensorCode code = new SensorCode(SensorCode.TYPE_EQP, 1, instrument);
			applyCoefficients(line, instrument.getColumnAssignment(Instrument.COL_EQP_1), code, coefficients);
		}
		
		if (instrument.hasEqp2()) {
			SensorCode code = new SensorCode(SensorCode.TYPE_EQP, 2, instrument);
			applyCoefficients(line, instrument.getColumnAssignment(Instrument.COL_EQP_2), code, coefficients);
		}
		
		if (instrument.hasEqp3()) {
			SensorCode code = new SensorCode(SensorCode.TYPE_EQP, 3, instrument);
			applyCoefficients(line, instrument.getColumnAssignment(Instrument.COL_EQP_3), code, coefficients);
		}
	}
	*/
	
	/**
	 * Apply a set of calibration coefficients to a specific sensor
	 * that has a measurement on the current data line
	 * @param line The data line
	 * @param sensorColumn The column in the line where the sensor's value can be found
	 * @param sensorCode The sensor code for the sensor
	 * @param coefficients The calibration coefficients
	 */
	/*
	private void applyCoefficients(List<String> line, int sensorColumn, SensorCode sensorCode, List<CalibrationCoefficients> coefficients) {
		
		CalibrationCoefficients calibration = CalibrationCoefficients.findSensorCoefficients(coefficients, sensorCode);
		
		double value = Double.parseDouble(line.get(sensorColumn));
		double calibratedValue = calibration.getIntercept() +
									value * calibration.getX() +
									value * Math.pow(calibration.getX2(), 2) +
									value * Math.pow(calibration.getX3(), 3) +
									value * Math.pow(calibration.getX4(), 4) +
									value * Math.pow(calibration.getX5(), 5);
							
		line.set(sensorColumn, String.valueOf(calibratedValue));
	}
	*/
	
	/**
	 * Reset the data for the data file configured for this job.
	 * 
	 * <p>
	 *   This deletes all data relating to the data file from the database,
	 *   including the raw data, data reduction calculations, and QC details.
	 * </p>
	 * <p>
	 *   Once data removal is complete, a new instance of this job
	 *   is queued to restart the data extraction process.
	 * </p>
	 * @throws JobFailedException If any of the data clearance operations fail
	 */
	protected void reset() throws JobFailedException {
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			QCDB.clearQCData(conn, fileId);
			DataReductionDB.clearDataReductionData(conn, fileId);
			RawDataDB.clearRawData(conn, fileId);
			
			// If the data file isn't marked for deletion, re-queue the job
			if (!DataFileDB.getDeleteFlag(dataSource, fileId)) {
				DataFileDB.setCurrentJob(conn, fileId, FileInfo.JOB_CODE_EXTRACT);
			}
			
			conn.commit();
		} catch(MissingParamException|SQLException|DatabaseException|RecordNotFoundException e) {
			throw new JobFailedException(id, e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
	}
}
