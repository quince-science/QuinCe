package uk.ac.exeter.QuinCe.data.Calculation;

import java.util.Calendar;

import uk.ac.exeter.QuinCe.data.Instrument.RunType;
import uk.ac.exeter.QuinCe.data.Instrument.Standards.StandardConcentration;

/**
 * Represents the mean of a gas standard during an instrument's
 * gas standard run. The instrument will measure a single standard
 * for a period, and the mean measured value during that period will
 * be used to calibrate the actual ocean/atmosphere measurements.
 * 
 * <p>
 *   {@code GasStandardMean} objects implement the {@link Comparable}
 *   interface. Comparisons are based on the {@link #startTime}, allowing
 *   them to be sorted by time order.
 * </p>
 *
 * @author Steve Jones
 * @see GasStandardRuns
 */
public class GasStandardMean implements Comparable<GasStandardMean> {

	/**
	 * Code indicating that xH<sub>2</sub>O values should be retrieved from a gas standard run 
	 */
	protected static final int TYPE_XH2O = 0;
	
	/**
	 * Code indicating that CO<sub>2</sub> values should be retrieved from a gas standard run 
	 */
	protected static final int TYPE_CO2 = 1;
	
	/**
	 * The true concentration of the gas standard bottle
	 */
	private StandardConcentration standardConcentration;
	
	/**
	 * The start time of the gas standard run
	 */
	private Calendar startTime;
	
	/**
	 * The end time of the gas standard run
	 */
	private Calendar endTime;
	
	/**
	 * The mean concentration measured during the run
	 */
	private double meanConcentration;
	
	/**
	 * The mean xH<sub>2</sub>O measured during the run
	 */
	private double meanXh2o;
	
	/**
	 * Simple constructor to set all fields
	 * @param standardConcentration The true gas standard concentration
	 * @param startTime The start time of the gas standard run
	 * @param endTime The end time of the gas standard run
	 * @param meanConcentration The mean concentration measured during the run
	 * @param meanXh2o The mean xH2O measured during the run
	 */
	public GasStandardMean(StandardConcentration standardConcentration, Calendar startTime, Calendar endTime, double meanConcentration, double meanXh2o) {
		this.standardConcentration = standardConcentration;
		this.startTime = startTime;
		this.endTime = endTime;
		this.meanConcentration = meanConcentration;
		this.meanXh2o = meanXh2o;
	}

	/**
	 * Get the standard concentration record related to this gas standard run
	 * @return The standard concentration
	 */
	public StandardConcentration getStandardConcentration() {
		return standardConcentration;
	}

	/**
	 * Get the start time of the gas standard run
	 * @return The start time
	 */
	public Calendar getStartTime() {
		return startTime;
	}

	/**
	 * Get the end time of the gas standard run
	 * @return The end time
	 */
	public Calendar getEndTime() {
		return endTime;
	}

	/**
	 * Get the mean CO<sub>2</sub> concentration for the gas standard run
	 * @return The mean CO<sub>2</sub> concentration
	 */
	public double getMeanConcentration() {
		return meanConcentration;
	}

	/**
	 * Get the mean xH<sub>2</sub>O value for the gas standard run
	 * @return The mean xH<sub>2</sub>O value
	 */
	public double getMeanXh2o() {
		return meanXh2o;
	}
	
	/**
	 * Get the time at the mid point of the gas standard run
	 * @return The mid point time
	 */
	public Calendar getMidTime() {
		int midLength = (int) (endTime.getTimeInMillis() - startTime.getTimeInMillis()) / 2;
		Calendar result = (Calendar) startTime.clone();
		result.add(Calendar.MILLISECOND, midLength);
		return result;
	}

	@Override
	public int compareTo(GasStandardMean o) {
		return startTime.compareTo(o.startTime);
	}
	
	/**
	 * Get the name of this gas standard run.
	 * This is the Run Type recorded in the data file
	 * @return The gas standard name
	 * @see RunType
	 */
	public String getRunName() {
		return standardConcentration.getStandardName();
	}
	
	/**
	 * Get either the mean CO<sub>2</sub> concentration or mean
	 * moisture of the gas standard run.
	 * 
	 * <p>
	 *   The {@code valueType} must be one of {@link #TYPE_CO2} or {@link #TYPE_XH2O}.
	 *   If any other value is passed, the method's behaviour is undefined.
	 * </p>
	 * 
	 * @param valueType Either {@link #TYPE_CO2} or {@link #TYPE_XH2O}
	 * @return The mean value
	 */
	public double getMeanValue(int valueType) {
		double result;
		
		if (valueType == TYPE_XH2O) {
			result = meanXh2o;
		} else {
			result = meanConcentration;
		}
		
		return result;
	}
}
