package uk.ac.exeter.QuinCe.data.Calculation;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import javax.sql.DataSource;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.RunType;
import uk.ac.exeter.QuinCe.data.Instrument.Standards.GasStandardDB;
import uk.ac.exeter.QuinCe.data.Instrument.Standards.StandardConcentration;
import uk.ac.exeter.QuinCe.data.Instrument.Standards.StandardStub;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Represents a complete set of gas standard runs from a data file
 * @author Steve Jones
 * @see GasStandardMean
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
	 * @param instrument The instrument to which the data file belongs
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
	
	/**
	 * Retrieve the interpolated moisture value at a given time for
	 * a given standard run type
	 * 
	 * <p>
	 *   The interpolated value is calculated as a linear interpolation
	 *   of the xH<sub>2</sub>O values between the standard runs immediately
	 *   preceding and following the specified time. If the specified time
	 *   is before the first standard run, the value for the first run is used.
	 *   If the specified time is after the last standard run, the value for
	 *   the last run is used.
	 * </p>
	 * @param runType The run type
	 * @param time The time to which the values should be interpolated
	 * @return The interpolated moisture value
	 * @see RunType
	 */
	public double getInterpolatedXh2o(String runType, Calendar time) {
		return getInterpolatedValue(runType, time, GasStandardMean.TYPE_XH2O);
	}
	
	/**
	 * Retrieve the interpolated CO<sub>2</sub> value at a given time for
	 * a given standard run type
	 * 
	 * <p>
	 *   The interpolated value is calculated as a linear interpolation
	 *   of the moisture values between the standard runs immediately
	 *   preceding and following the specified time. If the specified time
	 *   is before the first standard run, the value for the first run is used.
	 *   If the specified time is after the last standard run, the value for
	 *   the last run is used.
	 * </p>
	 * @param runType The run type
	 * @param time The time to which the values should be interpolated
	 * @return The interpolated CO<sub>2</sub> value
	 * @see RunType
	 */
	public double getInterpolatedCo2(String runType, Calendar time) {
		return getInterpolatedValue(runType, time, GasStandardMean.TYPE_CO2);
	}
	
	/**
	 * Calculate an interpolated moisture or CO<sub>2</sub> value for a given
	 * gas standard type. This performs the calculations for
	 * {@link #getInterpolatedCo2(String, Calendar)} and {@link #getInterpolatedMoisture(String, Calendar)}.
	 * 
	 * @param runType The gas standard type
	 * @param time The time to which the values should be interpolated
	 * @param valueType The type of value to interpolate. Either {@link GasStandardMean#TYPE_CO2} or {@link GasStandardMean#TYPE_MOISTURE}.
	 * @return The interpolated value
	 * @see RunType
	 */
	private double getInterpolatedValue(String runType, Calendar time, int valueType) {
		GasStandardMean previous = getStandardBefore(runType, time);
		GasStandardMean next = getStandardAfter(runType, time);
		
		double result;
		
		if (null == previous && null == next) {
			result = RawDataDB.MISSING_VALUE;
		} else if (null == previous) {
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
	
	/**
	 * Find the standard run immediately preceding the specified time for a given gas standard run type.
	 * If there is no standard before that time {@code null} is returned.
	 * 
	 * @param runType The gas standard type
	 * @param time The target time
	 * @return The standard before the specified time, or {@code null} if there is no standard
	 */
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
	
	/**
	 * Find the standard run immediately following the specified time for a given gas standard run type.
	 * If there is no standard after that time {@code null} is returned.
	 * 
	 * @param runType The gas standard type
	 * @param time The target time
	 * @return The standard after the specified time, or {@code null} if there is no standard
	 */
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
	
	/**
	 * Retrieve the set of gas standard runs to be searched for a calculation.
	 * 
	 * <p>
	 *   If {@code runType} is null, all standard runs are returned. Otherwise
	 *   only the specified runs are returned.
	 * </p>
	 * 
	 * @param runType The run type
	 * @return The set of standards to be searched
	 * @see RunType
	 */
	private TreeSet<GasStandardMean> getSearchSet(String runType) {
		return (null == runType ? allStandardMeans : groupedStandardMeans.get(runType));
	}
	
	/**
	 * Retrieve the linear regression to be used to calibrate a measurement against the gas standard
	 * runs.
	 * 
	 * <p>
	 *   Calibrating a measurement against gas standards is a two dimensional process.
	 *   First, the gas standards of each type are examined individually, and the standard value for each
	 *   type is calculated as a linear interpolation of the standard runs immediately preceding and
	 *   following the measurement. This gives the value of the standards that would be measured at that time.
	 *   
     *   Plotting the measured values against the known true concentrations of the gas standards gives what
     *   is close to a linear relationship between a measured value and its true value. This can be expressed
     *   as a linear regression, which can then be used to calculate the true value of a measurement.
	 * </p>
	 * @param dataSource A data source
	 * @param instrumentId The database ID of the instrument
	 * @param time The time of the measurement being calibrated
	 * @return The regression that can be used to calculate the true value of a measurement
	 * @throws MissingParamException If any parameters are missing
	 * @throws DatabaseException If a database error occurs
	 * @throws RecordNotFoundException If any required database records are missing
	 */
	public SimpleRegression getStandardsRegression(DataSource dataSource, long instrumentId, Calendar time) throws MissingParamException, DatabaseException, RecordNotFoundException {
		
		SimpleRegression result = new SimpleRegression(true);
		
		StandardStub actualStandard = GasStandardDB.getStandardBefore(dataSource, instrumentId, time);
		Map<String, StandardConcentration> actualConcentrations = GasStandardDB.getConcentrationsMap(dataSource, actualStandard);
		
		for (String runType : groupedStandardMeans.keySet()) {
			
			// The gas standard as measured either side of the 'real' CO2 measurement
			double measuredStandard = getInterpolatedCo2(runType, time);
			
			if (measuredStandard != RawDataDB.MISSING_VALUE) {
				// The actual concentration of the gas standard
				double actualConcentration = actualConcentrations.get(runType).getConcentration();
				
				result.addData(measuredStandard, actualConcentration);
			}
		}
		
		return result;
	}
}
