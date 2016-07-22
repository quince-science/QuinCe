package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QuinCe.data.FileInfo;
import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.data.RawDataValues;
import uk.ac.exeter.QuinCe.data.RunType;
import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.data.Calculation.GasStandardRuns;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.database.Calculation.DataReductionDB;
import uk.ac.exeter.QuinCe.database.Calculation.RawDataDB;
import uk.ac.exeter.QuinCe.database.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.database.QC.QCDB;
import uk.ac.exeter.QuinCe.database.files.DataFileDB;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class DataReductionJob extends FileJob {


	private static final double PASCALS_TO_ATMOSPHERES = 0.00000986923266716013;

	public DataReductionJob(ResourceManager resourceManager, Properties config, long jobId, List<String> parameters) throws MissingParamException, InvalidJobParametersException, DatabaseException, RecordNotFoundException {
		super(resourceManager, config, jobId, parameters);
	}
	
	@Override
	protected void execute() throws JobFailedException {
		reset();
		
		Connection conn = null;
		
		try {
			Instrument instrument = InstrumentDB.getInstrumentByFileId(dataSource, fileId);
			
			// Load the gas standard runs
			GasStandardRuns standardRuns = RawDataDB.getGasStandardRuns(dataSource, fileId, instrument);
			
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			List<RawDataValues> rawData = RawDataDB.getRawData(dataSource, fileId, instrument);
			int lineNumber = 0;
			for (RawDataValues record : rawData) {
				lineNumber++;
				
				// If the record has been marked bad, we skip it
				if (!QCDB.getWoceFlag(conn, fileId, record.getRow()).equals(Flag.BAD)) {
				
					
					if (record.getCo2Type() == RunType.RUN_TYPE_WATER) {
						
						// Sensor means
						double meanIntakeTemp = calcMeanIntakeTemp(record, instrument);
						double meanSalinity = calcMeanSalinity(record, instrument);
						double meanEqt = calcMeanEqt(record, instrument);
						double meanEqp = calcMeanEqp(record, instrument);
						
						// Get the moisture and mathematically dry the CO2 if required
						double trueMoisture = 0;
						double driedCo2 = record.getCo2();
						
						if (!instrument.getSamplesDried()) {
							trueMoisture = record.getMoisture() - standardRuns.getInterpolatedMoisture(null, record.getTime());
							driedCo2 = record.getCo2() / (1.0 - (trueMoisture / 1000));
						}
						
						double calibratedCo2 = calcCalibratedCo2(driedCo2, record.getTime(), standardRuns, instrument);
						double pCo2TEDry = calcPco2TEDry(calibratedCo2, meanEqp);
						double pH2O = calcPH2O(meanSalinity, meanEqt);
						double pCo2TEWet = calcPco2TEWet(pCo2TEDry, meanEqp, pH2O);
						double fco2TE = calcFco2TE(pCo2TEWet, meanEqp, meanEqt);
						double fco2 = calcFco2(fco2TE, meanEqt, meanIntakeTemp);
											
						DataReductionDB.storeRow(conn, fileId, record.getRow(), record.getCo2Type(), meanIntakeTemp,
								meanSalinity, meanEqt, meanEqp, trueMoisture, driedCo2, calibratedCo2,
								pCo2TEDry, pH2O, pCo2TEWet, fco2TE, fco2);
						
						QCDB.resetQCFlagsByRow(conn, fileId, record.getRow());
					}
				}
				
				if (Math.floorMod(lineNumber, 100) == 0) {
					setProgress((double) lineNumber / (double) rawData.size() * 100.0);
				}
			}
			
			// Queue up the automatic QC job
			User owner = JobManager.getJobOwner(conn, id);
			JobManager.addJob(conn, owner, FileInfo.JOB_CLASS_AUTO_QC, parameters);
			DataFileDB.setCurrentJob(conn, fileId, FileInfo.JOB_CODE_AUTO_QC);

			conn.commit();
		} catch (Exception e) {
			throw new JobFailedException(id, e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
	}
	
	protected void reset() throws JobFailedException {
		try {
			DataReductionDB.clearDataReductionData(dataSource, fileId);
		} catch(DatabaseException e) {
			throw new JobFailedException(id, e);
		}
	}
	
	private double calcMeanIntakeTemp(RawDataValues values, Instrument instrument) {
		
		double total = 0;
		int count = 0;
		
		if (instrument.hasIntakeTemp1()) {
			total = total + values.getIntakeTemp1();
			count++;
		}
		
		if (instrument.hasIntakeTemp2()) {
			total = total + values.getIntakeTemp2();
			count++;
		}
		
		if (instrument.hasIntakeTemp3()) {
			total = total + values.getIntakeTemp3();
			count++;
		}
		
		return total / (double) count;
	}

	private double calcMeanSalinity(RawDataValues values, Instrument instrument) {
		
		double total = 0;
		int count = 0;
		
		if (instrument.hasSalinity1()) {
			total = total + values.getSalinity1();
			count++;
		}
		
		if (instrument.hasSalinity2()) {
			total = total + values.getSalinity2();
			count++;
		}
		
		if (instrument.hasSalinity3()) {
			total = total + values.getSalinity3();
			count++;
		}
		
		return total / (double) count;
	}

	private double calcMeanEqt(RawDataValues values, Instrument instrument) {
		
		double total = 0;
		int count = 0;
		
		if (instrument.hasEqt1()) {
			total = total + values.getEqt1();
			count++;
		}
		
		if (instrument.hasEqt2()) {
			total = total + values.getEqt2();
			count++;
		}
		
		if (instrument.hasEqt3()) {
			total = total + values.getEqt3();
			count++;
		}
		
		return total / (double) count;
	}

	private double calcMeanEqp(RawDataValues values, Instrument instrument) {
		
		double total = 0;
		int count = 0;
		
		if (instrument.hasEqp1()) {
			total = total + values.getEqp1();
			count++;
		}
		
		if (instrument.hasEqp2()) {
			total = total + values.getEqp2();
			count++;
		}
		
		if (instrument.hasEqp3()) {
			total = total + values.getEqp3();
			count++;
		}
		
		return total / (double) count;
	}
	
	private double calcCalibratedCo2(double driedCo2, Calendar time, GasStandardRuns standardRuns, Instrument instrument) throws MissingParamException, DatabaseException, RecordNotFoundException {
		SimpleRegression regression = standardRuns.getStandardsRegression(dataSource, instrument.getDatabaseId(), time);
		return regression.predict(driedCo2);
	}
	
	private double calcPco2TEDry(double co2, double eqp) {
		
		// Calibrated CO2 to Pascals (adjusted for equilibrator pressure)
		double pressureAdjusted = (co2 * 1.0e-6) * (eqp * 100);
		
		// Convert back to microatmospheres
		return pressureAdjusted * PASCALS_TO_ATMOSPHERES * 1.0e6;
	}
	
	/**
	 * Calculate pH2O. From Weiss and Price (1980)
	 * @param salinity Salinity
	 * @param eqt Equilibrator temperature (in celcius)
	 * @return
	 */
	private double calcPH2O(double salinity, double eqt) {
		double kelvin = eqt + 273.15;
		return Math.exp(24.4543 - 67.4509 * (100 / kelvin) - 4.8489 * Math.log(kelvin / 100) - 0.000544 * salinity);
	}
	
	private double calcPco2TEWet(double co2TEDry, double eqp, double pH2O) {
		double eqp_atm = eqp * PASCALS_TO_ATMOSPHERES * 100;
		return co2TEDry * (eqp_atm - pH2O);
	}
	
	private double calcFco2TE(double pco2TEWet, double eqp, double eqt) {
		double kelvin = eqt + 273.15;
		double B = -1636.75 + 12.0408 * kelvin -0.0327957 * Math.pow(kelvin, 2) + (3.16528 * 1e-5) * Math.pow(kelvin, 3);
		double delta = 57.7 - 0.118 * kelvin;
				
		return pco2TEWet * Math.exp(((B + 2 * (delta * 1e-6)) * (eqp * 1e-6)) / (8.314472 * kelvin));		
	}
	
	private double calcFco2(double fco2TE, double eqt, double sst) {
		double sst_kelvin = sst + 273.15;
		double eqt_kelvin = eqt + 273.15;
		return fco2TE * Math.exp(0.0423 * (sst_kelvin - eqt_kelvin));
	}
}

