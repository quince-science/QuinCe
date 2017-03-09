package uk.ac.exeter.QuinCe.data.Calculation;

import java.util.Calendar;

import uk.ac.exeter.QuinCe.data.StandardConcentration;

/**
 * Contains the mean gas standards data from a given gas standard run
 * @author Steve Jones
 *
 */
public class GasStandardMean implements Comparable<GasStandardMean> {

	protected static final int TYPE_XH2O = 0;
	
	protected static final int TYPE_CO2 = 1;
	

	/**
	 * The actual concentration of the standard
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
	 * The mean xH2O measured during the run
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

	public StandardConcentration getStandardConcentration() {
		return standardConcentration;
	}

	public Calendar getStartTime() {
		return startTime;
	}

	public Calendar getEndTime() {
		return endTime;
	}

	public double getMeanConcentration() {
		return meanConcentration;
	}

	public double getMeanXh2o() {
		return meanXh2o;
	}
	
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
	
	public String getRunName() {
		return standardConcentration.getStandardName();
	}
	
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
