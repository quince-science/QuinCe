package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import javax.sql.DataSource;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import uk.ac.exeter.QCRoutines.data.NoSuchColumnException;
import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QCRoutines.messages.Message;
import uk.ac.exeter.QCRoutines.messages.MessageException;
import uk.ac.exeter.QCRoutines.messages.MissingValueMessage;
import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.data.FileInfo;
import uk.ac.exeter.QuinCe.data.RawDataValues;
import uk.ac.exeter.QuinCe.data.RunType;
import uk.ac.exeter.QuinCe.data.Calculation.GasStandardRuns;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.QC.NoDataQCRecord;
import uk.ac.exeter.QuinCe.data.QC.QCDB;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.database.Calculation.DataReductionDB;
import uk.ac.exeter.QuinCe.database.Calculation.RawDataDB;
import uk.ac.exeter.QuinCe.database.files.DataFileDB;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.JobThread;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * The background job to perform data reduction on a data file.
 * 
 * <p>
 *   The calculations follow the procedure in Pierrot et al. 2009
 *   (doi:10.1016/j.dsr2.2008.12.005), with direct input from Denis Pierrot.
 * 
 * @author Steve Jones
 * @see <a href="http://www.sciencedirect.com/science/article/pii/S0967064508004268">Recommendations for autonomous underway pCO<sub>2</sub> measuring systems and data-reduction routines</a> 
 */
public class DataReductionJob extends FileJob {

	/**
	 * The conversion factor from Pascals to Atmospheres
	 */
	private static final double PASCALS_TO_ATMOSPHERES = 0.00000986923266716013;

	/**
	 * Constructor for a data reduction job to be run on a specific data file.
	 * The job record must already have been created in the database.
	 * 
	 * @param resourceManager The QuinCe resource manager
	 * @param config The application configuration
	 * @param jobId The job's database ID
	 * @param parameters The job parameters. These will be ignored.
	 * @throws MissingParamException If any constructor parameters are missing
	 * @throws InvalidJobParametersException If any of the parameters are invalid. Because parameters are ignored for this job, this exception will not be thrown.
	 * @throws DatabaseException If a database error occurs
	 * @throws RecordNotFoundException If the job cannot be found in the database
	 */
	public DataReductionJob(ResourceManager resourceManager, Properties config, long jobId, Map<String, String> parameters) throws MissingParamException, InvalidJobParametersException, DatabaseException, RecordNotFoundException {
		super(resourceManager, config, jobId, parameters);
	}
	
	@Override
	protected void executeFileJob(JobThread thread) throws JobFailedException {
		Connection conn = null;
		
		try {
			Instrument instrument = InstrumentDB.getInstrumentByFileId(dataSource, fileId);
			
			// Load the gas standard runs
			GasStandardRuns standardRuns = RawDataDB.getGasStandardRuns(dataSource, fileId, instrument);
			
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			List<RawDataValues> rawData = RawDataDB.getRawData(conn, fileId, instrument);
			Map<Integer, NoDataQCRecord> qcRecords = QCDB.getNoDataQCRecords(conn, resourceManager.getColumnConfig(), fileId, instrument);
			
			int lineNumber = 0;
			for (RawDataValues record : rawData) {
				
				if (thread.isInterrupted()) {
					break;
				}
				
				lineNumber++;
				NoDataQCRecord qcRecord = qcRecords.get(record.getRow());
				
				// If the record has been marked bad, we skip it
				if (qcRecord.getWoceFlag().equals(Flag.FATAL) || qcRecord.getWoceFlag().equals(Flag.BAD) || qcRecord.getWoceFlag().equals(Flag.IGNORED)) {
				
					// Store empty data reduction values (unless other values have previously been stored)
					DataReductionDB.storeRow(conn, fileId, record.getRow(), false, record.getCo2Type(), RawDataDB.MISSING_VALUE,
							RawDataDB.MISSING_VALUE, RawDataDB.MISSING_VALUE, RawDataDB.MISSING_VALUE, RawDataDB.MISSING_VALUE, 
							RawDataDB.MISSING_VALUE, RawDataDB.MISSING_VALUE, RawDataDB.MISSING_VALUE, RawDataDB.MISSING_VALUE,
							RawDataDB.MISSING_VALUE, RawDataDB.MISSING_VALUE, RawDataDB.MISSING_VALUE, RawDataDB.MISSING_VALUE);
					
				} else {
					if (record.getCo2Type() == RunType.RUN_TYPE_WATER) {
						
						boolean canCalculateCO2 = true;
						
						// Check that the date/time is present
						if (null == record.getTime()) {
							canCalculateCO2 = false;
							qcRecord.addMessage(new MissingValueMessage(record.getRow(), qcRecord.getDateTimeColumns(), qcRecord.getDateTimeColumnNames(), Flag.BAD));
							qcRecord.setWoceFlag(Flag.BAD);
							qcRecord.appendWoceComment("Missing Date/Time");
						}
						
						if (RawDataDB.MISSING_VALUE == record.getLongitude()) {
							canCalculateCO2 = false;
							qcRecord.addMessage(new MissingValueMessage(record.getRow(), qcRecord.getLongitudeColumn(), qcRecord.getLongitudeColumnName(), Flag.BAD));
							qcRecord.setWoceFlag(Flag.BAD);
							qcRecord.appendWoceComment("Missing Longitude");
						}
						
						if (RawDataDB.MISSING_VALUE == record.getLatitude()) {
							canCalculateCO2 = false;
							qcRecord.addMessage(new MissingValueMessage(record.getRow(), qcRecord.getLatitudeColumn(), qcRecord.getLatitudeColumnName(), Flag.BAD));
							qcRecord.setWoceFlag(Flag.BAD);
							qcRecord.appendWoceComment("Missing Latitude");
						}
						
						
						// Sensor means
						double meanIntakeTemp = calcMeanIntakeTemp(record, instrument, qcRecord);
						if (meanIntakeTemp == RawDataDB.MISSING_VALUE) {
							canCalculateCO2 = false;
							qcRecord.addMessage(new MissingValueMessage(qcRecord.getLineNumber(), qcRecord.getColumnIndex("Intake Temp 1"), instrument.getIntakeTempName1(), Flag.FATAL));
							qcRecord.setWoceFlag(Flag.BAD);
							qcRecord.appendWoceComment("Missing intake temperature");
						}
						
						double meanSalinity = calcMeanSalinity(record, instrument, qcRecord);
						if (meanSalinity == RawDataDB.MISSING_VALUE) {
							canCalculateCO2 = false;
							qcRecord.addMessage(new MissingValueMessage(qcRecord.getLineNumber(), qcRecord.getColumnIndex("Salinity 1"), instrument.getSalinityName1(), Flag.FATAL));
							qcRecord.setWoceFlag(Flag.FATAL);
							qcRecord.appendWoceComment("Missing salinity");
						}
						
						double meanEqt = calcMeanEqt(record, instrument, qcRecord);
						if (meanEqt == RawDataDB.MISSING_VALUE) {
							canCalculateCO2 = false;
							qcRecord.addMessage(new MissingValueMessage(qcRecord.getLineNumber(), qcRecord.getColumnIndex("Equilibrator Temperature 1"), instrument.getEqtName1(), Flag.FATAL));
							qcRecord.setWoceFlag(Flag.BAD);
							qcRecord.appendWoceComment("Missing equilibrator temperature");
						}
						
						double deltaTemperature = RawDataDB.MISSING_VALUE;
						if (meanIntakeTemp != RawDataDB.MISSING_VALUE && meanEqt != RawDataDB.MISSING_VALUE) {
							deltaTemperature = meanEqt - meanIntakeTemp;
						}
						
						double meanEqp = calcMeanEqp(record, instrument, qcRecord);
						if (meanEqp == RawDataDB.MISSING_VALUE) {
							canCalculateCO2 = false;
							qcRecord.addMessage(new MissingValueMessage(qcRecord.getLineNumber(), qcRecord.getColumnIndex("Equilibrator Pressure 1"), instrument.getEqpName1(), Flag.FATAL));
							qcRecord.setWoceFlag(Flag.BAD);
							qcRecord.appendWoceComment("Missing equilibrator pressure");
						}
						
						// Get the xH2O and mathematically dry the CO2 if required
						double driedCo2 = record.getCo2();
						if (driedCo2 == RawDataDB.MISSING_VALUE) {
							canCalculateCO2 = false;
							qcRecord.addMessage(new MissingValueMessage(record.getRow(), NoDataQCRecord.FIELD_CO2, qcRecord.getColumnName(NoDataQCRecord.FIELD_CO2), Flag.BAD));
							qcRecord.setWoceFlag(Flag.BAD);
							qcRecord.appendWoceComment("Missing CO2");
						}
						
						double trueXh2o = 0;
						if (canCalculateCO2 && !instrument.getSamplesDried()) {
							double measuredXh2o = record.getXh2o();
							if (measuredXh2o == RawDataDB.MISSING_VALUE) {
								canCalculateCO2 = false;
								qcRecord.addMessage(new MissingValueMessage(record.getRow(), NoDataQCRecord.FIELD_XH2O, qcRecord.getColumnName(NoDataQCRecord.FIELD_XH2O), Flag.BAD));
								qcRecord.setWoceFlag(Flag.BAD);
								qcRecord.appendWoceComment("Missing xH2O");
							}

							trueXh2o = measuredXh2o - standardRuns.getInterpolatedXh2o(null, record.getTime());
							driedCo2 = record.getCo2() / (1.0 - (trueXh2o / 1000));
						}

						double calibratedCo2 = RawDataDB.MISSING_VALUE;
						double pCo2TEDry = RawDataDB.MISSING_VALUE;
						double pH2O = RawDataDB.MISSING_VALUE;
						double pCo2TEWet = RawDataDB.MISSING_VALUE;
						double fco2TE = RawDataDB.MISSING_VALUE;
						double fco2 = RawDataDB.MISSING_VALUE;

						if (canCalculateCO2) {
							calibratedCo2 = calcCalibratedCo2(driedCo2, record.getTime(), standardRuns, instrument);
							pCo2TEDry = calcPco2TEDry(calibratedCo2, meanEqp);
							pH2O = calcPH2O(meanSalinity, meanEqt);
							pCo2TEWet = calcPco2TEWet(pCo2TEDry, meanEqp, pH2O);
							fco2TE = calcFco2TE(pCo2TEWet, meanEqp, meanEqt);
							fco2 = calcFco2(fco2TE, meanEqt, meanIntakeTemp);
						}
												
						DataReductionDB.storeRow(conn, fileId, record.getRow(), true, record.getCo2Type(), meanIntakeTemp,
								meanSalinity, meanEqt, deltaTemperature, meanEqp, trueXh2o, driedCo2, calibratedCo2,
								pCo2TEDry, pH2O, pCo2TEWet, fco2TE, fco2);
						
						QCDB.setQC(conn, fileId, qcRecord);
					} else {
						
						// For the time being we're ignoring atmospheric records,
						// so clear all the QC flags
						qcRecord.clearAllFlags();
						QCDB.setQC(conn, fileId, qcRecord);
					}
				}
				
				if (Math.floorMod(lineNumber, 100) == 0) {
					setProgress((double) lineNumber / (double) rawData.size() * 100.0);
				}
			}
			

			// If the thread was interrupted, undo everything
			if (thread.isInterrupted()) {
				conn.rollback();

				// Requeue the data reduction job
				try {
					User owner = JobManager.getJobOwner(dataSource, id);
					JobManager.addJob(conn, owner, FileInfo.getJobClass(FileInfo.JOB_CODE_REDUCTION), parameters);
					DataFileDB.setCurrentJob(conn, fileId, FileInfo.JOB_CODE_REDUCTION);
					conn.commit();
				} catch (RecordNotFoundException e) {
					// This means the file has been marked for deletion. No action is required.
				}
			} else {
				// Queue up the automatic QC job
				Map<String, String> nextJobParameters = AutoQCJob.getJobParameters(FileInfo.JOB_CODE_AUTO_QC, fileId);
				
				User owner = JobManager.getJobOwner(conn, id);
				JobManager.addJob(conn, owner, FileInfo.getJobClass(FileInfo.JOB_CODE_AUTO_QC), nextJobParameters);
				DataFileDB.setCurrentJob(conn, fileId, FileInfo.JOB_CODE_AUTO_QC);
				conn.commit();
			}

			
		} catch (Exception e) {
			throw new JobFailedException(id, e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
	}
	
	/**
	 * Removes any previously calculated data reduction results from the database
	 * @throws JobFailedException If an error occurs
	 */
	protected void reset() throws JobFailedException {
		try {
			DataReductionDB.clearDataReductionData(dataSource, fileId);
		} catch(DatabaseException e) {
			throw new JobFailedException(id, e);
		}
	}
	
	/**
	 * <p>Calculates the mean intake temperature for a single measurement from all intake temperature sensors.</p>
	 * 
	 * <p>Only sensors defined for the instrument are used. If any sensor values are missing
	 * (identified by {@link RawDataDB#MISSING_VALUE}), the sensor is not included in the calculation,
	 * and the appropriate flag is set on the measurment's QC record.</p>
	 * @param values The raw data for the measurement
	 * @param instrument The instrument from which the measurement was taken
	 * @param qcRecord The QC record for the measurement
	 * @return The mean intake temperature
	 * @throws NoSuchColumnException If an intake temperature column is missing.
	 * @throws MessageException If an error occurs while creating a QC message
	 */
	private double calcMeanIntakeTemp(RawDataValues values, Instrument instrument, NoDataQCRecord qcRecord) throws NoSuchColumnException, MessageException {
		
		double total = 0;
		int count = 0;
		List<Message> qcMessages = new ArrayList<Message>();
		TreeSet<Integer> missingColumnIndices = new TreeSet<Integer>();
		
		if (instrument.hasIntakeTemp1()) {
			double value = values.getIntakeTemp1();

			if (RawDataDB.MISSING_VALUE == value) {
				int colIndex = NoDataQCRecord.FIELD_INTAKE_TEMP_1;
				qcMessages.add(new MissingValueMessage(values.getRow(), colIndex, qcRecord.getColumnName(colIndex), Flag.QUESTIONABLE));
				qcRecord.setIntakeTemp1Used(false);
			}
			
			total = total + value;
			count++;
		}
		
		if (instrument.hasIntakeTemp2()) {
			double value = values.getIntakeTemp2();

			if (RawDataDB.MISSING_VALUE == value) {
				int colIndex = NoDataQCRecord.FIELD_INTAKE_TEMP_2;
				qcMessages.add(new MissingValueMessage(values.getRow(), colIndex, qcRecord.getColumnName(colIndex), Flag.QUESTIONABLE));
				qcRecord.setIntakeTemp2Used(false);
			}
			
			total = total + value;
			count++;
		}
		
		if (instrument.hasIntakeTemp3()) {
			double value = values.getIntakeTemp3();

			if (RawDataDB.MISSING_VALUE == value) {
				int colIndex = NoDataQCRecord.FIELD_INTAKE_TEMP_3;
				qcMessages.add(new MissingValueMessage(values.getRow(), colIndex, qcRecord.getColumnName(colIndex), Flag.QUESTIONABLE));
				qcRecord.setIntakeTemp3Used(false);
			}
			
			total = total + value;
			count++;
		}
		
		double result;
		if (count == 0) {
			result = RawDataDB.MISSING_VALUE;
			qcMessages.clear();
			qcMessages.add(new MissingValueMessage(values.getRow(), missingColumnIndices, qcRecord.getColumnNames(missingColumnIndices), Flag.BAD));
		} else {
			result= total / (double) count;
		}
		
		return result;
	}

	/**
	 * <p>Calculates the mean salinity for a single measurement from all salinity sensors.</p>
	 * 
	 * <p>Only sensors defined for the instrument are used. If any sensor values are missing
	 * (identified by {@link RawDataDB#MISSING_VALUE}), the sensor is not included in the calculation,
	 * and the appropriate flag is set on the measurment's QC record.</p>
	 * @param values The raw data for the measurement
	 * @param instrument The instrument from which that the measurement was taken
	 * @param qcRecord The QC record for the measurement
	 * @return The mean salinity
	 * @throws NoSuchColumnException If a salinity column is missing.
	 * @throws MessageException If an error occurs while creating a QC message
	 */
	private double calcMeanSalinity(RawDataValues values, Instrument instrument, NoDataQCRecord qcRecord) throws NoSuchColumnException, MessageException {
		
		double total = 0;
		int count = 0;
		List<Message> qcMessages = new ArrayList<Message>();
		TreeSet<Integer> missingColumnIndices = new TreeSet<Integer>();
		
		if (instrument.hasSalinity1()) {
			double value = values.getSalinity1();

			if (RawDataDB.MISSING_VALUE == value) {
				int colIndex = NoDataQCRecord.FIELD_SALINITY_1;
				qcMessages.add(new MissingValueMessage(values.getRow(), colIndex, qcRecord.getColumnName(colIndex), Flag.QUESTIONABLE));
				qcRecord.setSalinity1Used(false);
			}
			
			total = total + value;
			count++;
		}
		
		if (instrument.hasSalinity2()) {
			double value = values.getSalinity2();

			if (RawDataDB.MISSING_VALUE == value) {
				int colIndex = NoDataQCRecord.FIELD_SALINITY_2;
				qcMessages.add(new MissingValueMessage(values.getRow(), colIndex, qcRecord.getColumnName(colIndex), Flag.QUESTIONABLE));
				qcRecord.setSalinity2Used(false);
			}
			
			total = total + value;
			count++;
		}
		
		if (instrument.hasSalinity3()) {
			double value = values.getSalinity3();

			if (RawDataDB.MISSING_VALUE == value) {
				int colIndex = NoDataQCRecord.FIELD_SALINITY_3;
				qcMessages.add(new MissingValueMessage(values.getRow(), colIndex, qcRecord.getColumnName(colIndex), Flag.QUESTIONABLE));
				qcRecord.setSalinity3Used(false);
			}
			
			total = total + value;
			count++;
		}
		
		double result;
		if (count == 0) {
			result = RawDataDB.MISSING_VALUE;
			qcMessages.clear();
			qcMessages.add(new MissingValueMessage(values.getRow(), missingColumnIndices, qcRecord.getColumnNames(missingColumnIndices), Flag.BAD));
		} else {
			result= total / (double) count;
		}
		
		return result;
	}

	/**
	 * <p>Calculates the mean equilibrator temperature for a single measurement from all equilibrator temperature sensors.</p>
	 * 
	 * <p>Only sensors defined for the instrument are used. If any sensor values are missing
	 * (identified by {@link RawDataDB#MISSING_VALUE}), the sensor is not included in the calculation,
	 * and the appropriate flag is set on the measurment's QC record.</p>
	 * @param values The raw data for the measurement
	 * @param instrument The instrument from which the measurement was taken
	 * @param qcRecord The QC record for the measurement
	 * @return The mean equilibrator temperature
	 * @throws NoSuchColumnException If an equilibrator temperature column is missing.
	 * @throws MessageException If an error occurs while creating a QC message
	 */
	private double calcMeanEqt(RawDataValues values, Instrument instrument, NoDataQCRecord qcRecord) throws NoSuchColumnException, MessageException {
		
		double total = 0;
		int count = 0;
		List<Message> qcMessages = new ArrayList<Message>();
		TreeSet<Integer> missingColumnIndices = new TreeSet<Integer>();
		
		if (instrument.hasEqt1()) {
			double value = values.getEqt1();

			if (RawDataDB.MISSING_VALUE == value) {
				int colIndex = NoDataQCRecord.FIELD_EQT_1;
				qcMessages.add(new MissingValueMessage(values.getRow(), colIndex, qcRecord.getColumnName(colIndex), Flag.QUESTIONABLE));
				qcRecord.setEqt1Used(false);
			}
			
			total = total + value;
			count++;
		}
		
		if (instrument.hasEqt2()) {
			double value = values.getEqt2();

			if (RawDataDB.MISSING_VALUE == value) {
				int colIndex = NoDataQCRecord.FIELD_EQT_2;
				qcMessages.add(new MissingValueMessage(values.getRow(), colIndex, qcRecord.getColumnName(colIndex), Flag.QUESTIONABLE));
				qcRecord.setEqt2Used(false);
			}
			
			total = total + value;
			count++;
		}
		
		if (instrument.hasEqt3()) {
			double value = values.getEqt3();

			if (RawDataDB.MISSING_VALUE == value) {
				int colIndex = NoDataQCRecord.FIELD_EQT_3;
				qcMessages.add(new MissingValueMessage(values.getRow(), colIndex, qcRecord.getColumnName(colIndex), Flag.QUESTIONABLE));
				qcRecord.setEqt3Used(false);
			}
			
			total = total + value;
			count++;
		}
		
		double result;
		if (count == 0) {
			result = RawDataDB.MISSING_VALUE;
			qcMessages.clear();
			qcMessages.add(new MissingValueMessage(values.getRow(), missingColumnIndices, qcRecord.getColumnNames(missingColumnIndices), Flag.BAD));
		} else {
			result= total / (double) count;
		}
		
		return result;
	}

	/**
	 * <p>Calculates the mean equilibrator pressure for a single measurement from all equilibrator pressure sensors.</p>
	 * 
	 * <p>Only sensors defined for the instrument are used. If any sensor values are missing
	 * (identified by {@link RawDataDB#MISSING_VALUE}), the sensor is not included in the calculation,
	 * and the appropriate flag is set on the measurment's QC record.</p>
	 * @param values The raw data for the measurement
	 * @param instrument The instrument from which the measurement was taken
	 * @param qcRecord The QC record for the measurement
	 * @return The mean equilibrator pressure
	 * @throws NoSuchColumnException If an equilibrator pressure column is missing.
	 * @throws MessageException If an error occurs while creating a QC message
	 */
	private double calcMeanEqp(RawDataValues values, Instrument instrument, NoDataQCRecord qcRecord) throws NoSuchColumnException, MessageException {
		
		double total = 0;
		int count = 0;
		List<Message> qcMessages = new ArrayList<Message>();
		TreeSet<Integer> missingColumnIndices = new TreeSet<Integer>();
		
		if (instrument.hasEqp1()) {
			double value = values.getEqp1();

			if (RawDataDB.MISSING_VALUE == value) {
				int colIndex = NoDataQCRecord.FIELD_EQP_1;
				qcMessages.add(new MissingValueMessage(values.getRow(), colIndex, qcRecord.getColumnName(colIndex), Flag.QUESTIONABLE));
				qcRecord.setEqp1Used(false);
			}
			
			total = total + value;
			count++;
		}
		
		if (instrument.hasEqp2()) {
			double value = values.getEqp2();

			if (RawDataDB.MISSING_VALUE == value) {
				int colIndex = NoDataQCRecord.FIELD_EQP_2;
				qcMessages.add(new MissingValueMessage(values.getRow(), colIndex, qcRecord.getColumnName(colIndex), Flag.QUESTIONABLE));
				qcRecord.setEqp2Used(false);
			}
			
			total = total + value;
			count++;
		}
		
		if (instrument.hasEqp3()) {
			double value = values.getEqp3();

			if (RawDataDB.MISSING_VALUE == value) {
				int colIndex = NoDataQCRecord.FIELD_EQP_3;
				qcMessages.add(new MissingValueMessage(values.getRow(), colIndex, qcRecord.getColumnName(colIndex), Flag.QUESTIONABLE));
				qcRecord.setEqp3Used(false);
			}
			
			total = total + value;
			count++;
		}
		
		double result;
		if (count == 0) {
			result = RawDataDB.MISSING_VALUE;
			qcMessages.clear();
			qcMessages.add(new MissingValueMessage(values.getRow(), missingColumnIndices, qcRecord.getColumnNames(missingColumnIndices), Flag.BAD));
		} else {
			result= total / (double) count;
		}
		
		return result;
	}

	/**
	 * <p>Calibrate a CO<sub>2</sub> measurement to the gas standards.</p>
	 * 
	 * <p>The calibration is performed by retrieving the gas standard runs immediately before
	 * and after the measurement, which implicitly include the adjustment of the measured CO<sub>2</sub>
	 * in the gas standard run to the true value of the standard
	 * (see {@link GasStandardRuns#getStandardsRegression(DataSource, long, Calendar)}).
	 * If only one standard is available (e.g. at the beginning or end of a data file), the single
	 * available standard is used.
	 * 
	 * The CO<sub>2</sub> value measured for the current record is adjusted to the linear progression between
	 * the two gas standard runs.
	 * 
	 * @param driedCo2 The CO<sub>2</sub> value after drying
	 * @param time The time that the measurement was taken
	 * @param standardRuns The set of gas standard runs for the data file
	 * @param instrument The instrument from which the measurement was taken
	 * @return The calibrated CO<sub>2</sub> value
	 * @throws MissingParamException If any parameters are missing
	 * @throws DatabaseException If a database error occurs
	 * @throws RecordNotFoundException If any required database records are missing
	 * @see GasStandardRuns
	 */
	private double calcCalibratedCo2(double driedCo2, Calendar time, GasStandardRuns standardRuns, Instrument instrument) throws MissingParamException, DatabaseException, RecordNotFoundException {
		SimpleRegression regression = standardRuns.getStandardsRegression(dataSource, instrument.getDatabaseId(), time);
		return regression.predict(driedCo2);
	}
	
	/**
	 * Calculates dry pCO<sub>2</sub> at the equilibrator temperature.
	 * Assumes that the CO<sub>2</sub> value has already been calibrated.
	 *  
	 * @param co2 The calibrated CO<sub>2</sub> value 
	 * @param eqp The equilibrator pressure
	 * @return The dry pCO<sub>2</sub> at the equilibrator temperature
	 */
	private double calcPco2TEDry(double co2, double eqp) {
		
		// Calibrated CO2 to Pascals (adjusted for equilibrator pressure)
		double pressureAdjusted = (co2 * 1.0e-6) * (eqp * 100);
		
		// Convert back to microatmospheres
		return pressureAdjusted * PASCALS_TO_ATMOSPHERES * 1.0e6;
	}
	
	/**
	 * Calculates the water vapour pressure (pH<sub>2</sub>O).
	 * From Weiss and Price (1980)
	 * @param salinity Salinity
	 * @param eqt Equilibrator temperature (in celcius)
	 * @return The calculated pH2O value
	 */
	private double calcPH2O(double salinity, double eqt) {
		double kelvin = eqt + 273.15;
		return Math.exp(24.4543 - 67.4509 * (100 / kelvin) - 4.8489 * Math.log(kelvin / 100) - 0.000544 * salinity);
	}
	
	/**
	 * Calculates pCO<sub>2</sub> in water at equlibrator temperature
	 * @param co2TEDry Dry pCO<sub>2</sub> at equilibrator temperature 
	 * @param eqp The equilibrator pressure
	 * @param pH2O The water vapour pressure
	 * @return pCO<sub>2</sub> in water at equlibrator temperature
	 */
	private double calcPco2TEWet(double co2TEDry, double eqp, double pH2O) {
		double eqp_atm = eqp * PASCALS_TO_ATMOSPHERES * 100;
		return co2TEDry * (eqp_atm - pH2O);
	}
	
	/**
	 * Calculates fCO<sub>2</sub> at equilibrator temperature
	 * @param pco2TEWet pCO<sub>2</sub> at equilibrator temperature
	 * @param eqp The equilibrator pressure
	 * @param eqt The equilibrator temperature
	 * @return fCO<sub>2</sub> at equilibrator temperature
	 */
	private double calcFco2TE(double pco2TEWet, double eqp, double eqt) {
		double kelvin = eqt + 273.15;
		double B = -1636.75 + 12.0408 * kelvin -0.0327957 * Math.pow(kelvin, 2) + (3.16528 * 1e-5) * Math.pow(kelvin, 3);
		double delta = 57.7 - 0.118 * kelvin;
				
		return pco2TEWet * Math.exp(((B + 2 * (delta * 1e-6)) * (eqp * 1e-6)) / (8.314472 * kelvin));		
	}
	
	/**
	 * Calculates fCO<sub>2</sub> at the sea surface temperature
	 * @param fco2TE fCO<sub>2</sub> at equilibrator temperature
	 * @param eqt The equilibrator temperature
	 * @param sst The sea surface temperature
	 * @return fCO<sub>2</sub> at the sea surface temperature
	 */
	private double calcFco2(double fco2TE, double eqt, double sst) {
		double sst_kelvin = sst + 273.15;
		double eqt_kelvin = eqt + 273.15;
		return fco2TE * Math.exp(0.0423 * (sst_kelvin - eqt_kelvin));
	}
}

