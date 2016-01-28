package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.data.RawDataValues;
import uk.ac.exeter.QuinCe.data.RunType;
import uk.ac.exeter.QuinCe.data.Calculation.GasStandardRuns;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.database.Calculation.DataReductionDB;
import uk.ac.exeter.QuinCe.database.Calculation.RawDataDB;
import uk.ac.exeter.QuinCe.database.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.jobs.InvalidJobParametersException;
import uk.ac.exeter.QuinCe.jobs.JobFailedException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

public class DataReductionJob extends FileJob {

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
				
					double meanIntakeTemp = calcMeanIntakeTemp(record, instrument);
					double meanSalinity = calcMeanSalinity(record, instrument);
					double meanEqt = calcMeanEqt(record, instrument);
					double meanEqp = calcMeanEqp(record, instrument);
					
					double trueMoisture = 0;
					double driedCo2 = record.getCo2();
					
					if (!instrument.getSamplesDried()) {
						trueMoisture = record.getMoisture() - standardRuns.getStandardMean(record.getRow()).getMeanMoisture();
						driedCo2 = record.getCo2() / (1.0 - (trueMoisture / 1000));
					}
										
					DataReductionDB.storeRow(conn, fileId, record.getRow(), record.getCo2Type(), meanIntakeTemp,
							meanSalinity, meanEqt, meanEqp, trueMoisture, driedCo2);
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
			total = total + values.getEqt1();
			count++;
		}
		
		if (instrument.hasEqp2()) {
			total = total + values.getEqt2();
			count++;
		}
		
		if (instrument.hasEqp3()) {
			total = total + values.getEqt3();
			count++;
		}
		
		return total / (double) count;
	}
}

