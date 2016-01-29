package uk.ac.exeter.QuinCe.data.Calculation;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import javax.sql.DataSource;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.data.RunType;
import uk.ac.exeter.QuinCe.data.StandardConcentration;
import uk.ac.exeter.QuinCe.data.StandardStub;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.database.Instrument.GasStandardDB;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Represents a set of gas standard runs from a data file
 * @author Steve Jones
 *
 */
public class GasStandardRuns {

	/**
	 * The database ID of the data file to which these gas standard runs belong
	 */
	private long fileId;
	
	/**
	 * The set of gas standard means for the data file, grouped
	 * by run type
	 */
	private Map<String,TreeSet<GasStandardMean>> groupedStandardMeans;
	
	/**
	 * The set of all gas standard means for the data file
	 */
	private TreeSet<GasStandardMean> allStandardMeans;
	
	/**
	 * Creates an empty set of gas standard runs for a given data file
	 * @param fileId The database ID of the file
	 */
	public GasStandardRuns(long fileId, Instrument instrument) {
		this.fileId = fileId;
		groupedStandardMeans = new HashMap<String,TreeSet<GasStandardMean>>();
		for (RunType runType : instrument.getRunTypes()) {
			if (instrument.isStandardRunType(runType.getName())) {
				groupedStandardMeans.put(runType.getName(), new TreeSet<GasStandardMean>());
			}
		}
		
		allStandardMeans = new TreeSet<GasStandardMean>();
	}
	
	/**
	 * Returns the database ID of the data file to which these gas standard runs belong
	 * @return The data file ID
	 */
	public long getFileId() {
		return fileId;
	}

	/**
	 * Add the mean results of a specific gas standard run to the object
	 * @param standardMean The gas standard results
	 */
	public void addStandardMean(GasStandardMean standardMean) {
		TreeSet<GasStandardMean> runMeans = groupedStandardMeans.get(standardMean.getRunName());		
		runMeans.add(standardMean);
		allStandardMeans.add(standardMean);
	}
	
	public double getInterpolatedMoisture(String runType, Calendar time) {
		return getInterpolatedValue(runType, time, GasStandardMean.TYPE_MOISTURE);
	}
	
	public double getInterpolatedCo2(String runType, Calendar time) {
		return getInterpolatedValue(runType, time, GasStandardMean.TYPE_CO2);
	}
	
	private double getInterpolatedValue(String runType, Calendar time, int valueType) {
		GasStandardMean previous = getStandardBefore(runType, time);
		GasStandardMean next = getStandardAfter(runType, time);
		
		double result;
		
		if (null == previous) {
			result = next.getMeanValue(valueType);
		} else if (null == next) {
			result = previous.getMeanValue(valueType);
		} else {
			double previousValue = previous.getMeanValue(valueType);
			double nextValue = next.getMeanValue(valueType);
			double valueRange = nextValue - previousValue;
			long secondsBetweenStandards = DateTimeUtils.getSecondsBetween(previous.getMidTime(), next.getMidTime());
			long timeAfterStart = DateTimeUtils.getSecondsBetween(previous.getMidTime(), time);
			double timeFraction = (double) timeAfterStart / (double) secondsBetweenStandards;
			result = previousValue + (valueRange * timeFraction);
		}
		
		return result;
	}
	
	private GasStandardMean getStandardBefore(String runType, Calendar time) {
		
		TreeSet<GasStandardMean> searchSet = getSearchSet(runType);
		
		GasStandardMean result = null;
		
		for (GasStandardMean standard : searchSet) {
			if (standard.getEndTime().before(time)) {
				result = standard;
			} else {
				break;
			}
		}
		
		return result;
		
	}
	
	private GasStandardMean getStandardAfter(String runType, Calendar time) {
		
		TreeSet<GasStandardMean> searchSet = getSearchSet(runType);
		
		GasStandardMean result = null;
		
		for (GasStandardMean standard : searchSet) {
			if (standard.getStartTime().after(time)) {
				result = standard;
				break;
			}
		}
		
		return result;
		
	}
	
	private TreeSet<GasStandardMean> getSearchSet(String runType) {
		return (null == runType ? allStandardMeans : groupedStandardMeans.get(runType));
	}
	
	public SimpleRegression getStandardsRegression(DataSource dataSource, long instrumentId, Calendar time) throws MissingParamException, DatabaseException, RecordNotFoundException {
		
		SimpleRegression result = new SimpleRegression(true);
		
		StandardStub actualStandard = GasStandardDB.getStandardBefore(dataSource, instrumentId, time);
		Map<String, StandardConcentration> actualConcentrations = GasStandardDB.getConcentrationsMap(dataSource, actualStandard);
		
		for (String runType : groupedStandardMeans.keySet()) {
			
			// The gas standard as measured either side of the 'real' CO2 measurement
			double measuredStandard = getInterpolatedCo2(runType, time);
			
			// The actual concentration of the gas standard
			double actualConcentration = actualConcentrations.get(runType).getConcentration();
			
			result.addData(measuredStandard, actualConcentration);
		}
		
		return result;
	}
}
