package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.data.RawDataValues;
import uk.ac.exeter.QuinCe.data.RunType;
import uk.ac.exeter.QuinCe.data.Calculation.GasStandardRuns;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.database.Calculation.DataReductionDB;
import uk.ac.exeter.QuinCe.database.Calculation.RawDataDB;
import uk.ac.exeter.QuinCe.database.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

public class DataReductionJob extends FileJob {

	private static final double PASCALS_TO_ATMOSPHERES = 0.00000986923266716013;
	
	public DataReductionJob(DataSource dataSource, Properties config, long jobId, List<String> parameters) throws MissingParamException, InvalidJobParametersException {
		super(dataSource, config, jobId, parameters);
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
					
					
										
					DataReductionDB.storeRow(conn, fileId, record.getRow(), record.getCo2Type(), meanIntakeTemp,
							meanSalinity, meanEqt, meanEqp, trueMoisture, driedCo2, calibratedCo2,
							pCo2TEDry);
				}
				
				if (Math.floorMod(lineNumber, 100) == 0) {
					setProgress((double) lineNumber / (double) rawData.size() * 100.0);
				}
			}
			
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
}

