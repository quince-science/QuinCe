package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.CalibrationCoefficients;
import uk.ac.exeter.QuinCe.data.CalibrationStub;
import uk.ac.exeter.QuinCe.data.FileInfo;
import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.data.RawDataFile;
import uk.ac.exeter.QuinCe.data.SensorCode;
import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.database.Calculation.DataReductionDB;
import uk.ac.exeter.QuinCe.database.Calculation.RawDataDB;
import uk.ac.exeter.QuinCe.database.Instrument.CalibrationDB;
import uk.ac.exeter.QuinCe.database.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.database.QC.QCDB;
import uk.ac.exeter.QuinCe.database.files.DataFileDB;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class ExtractRawDataJob extends FileJob {

	/**
	 * Creates the job
	 * @param dataSource A datasource
	 * @param config The application configuration
	 * @param jobId The job's database ID
	 * @param fileId The data file ID
	 * @throws MissingParamException If any parameters are missing
	 * @throws InvalidJobParametersException If the parameters are invalid
	 * @throws RecordNotFoundException 
	 * @throws DatabaseException 
	 */
	public ExtractRawDataJob(ResourceManager resourceManager, Properties config, long jobId, List<String> parameters) throws MissingParamException, InvalidJobParametersException, DatabaseException, RecordNotFoundException {
		super(resourceManager, config, jobId, parameters);
	}

	@Override
	protected void execute() throws JobFailedException {
		
		reset();
		Connection conn = null;
		
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
			
			int lineNumber = 0;
			for (List<String> line : data) {
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
					}
				}
				
				if (lineNumber % 100 == 0) {
					setProgress((double) lineNumber / (double) data.size() * 100.0);
				}
			}
			
			// Queue up the Trim Flushing job
			User owner = JobManager.getJobOwner(dataSource, id);
			JobManager.addJob(conn, owner, FileInfo.JOB_CLASS_TRIM_FLUSHING, parameters);
			DataFileDB.setCurrentJob(conn, fileId, FileInfo.JOB_CODE_TRIM_FLUSHING);

			conn.commit();
		} catch (Exception e) {
			DatabaseUtils.rollBack(conn);
			throw new JobFailedException(id, e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
	}
	
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
	
	protected void reset() throws JobFailedException {
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			QCDB.clearQCData(conn, fileId);
			DataReductionDB.clearDataReductionData(conn, fileId);
			RawDataDB.clearRawData(conn, fileId);
			DataFileDB.setCurrentJob(conn, fileId, FileInfo.JOB_CODE_EXTRACT);
			
			conn.commit();
		} catch(MissingParamException|SQLException|DatabaseException e) {
			throw new JobFailedException(id, e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
	}
}
