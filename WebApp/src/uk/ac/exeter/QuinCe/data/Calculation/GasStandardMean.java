package uk.ac.exeter.QuinCe.data.Calculation;

import uk.ac.exeter.QuinCe.data.StandardConcentration;

/**
 * Contains the mean gas standards data from a given gas standard run
 * @author Steve Jones
 *
 */
public class GasStandardMean implements Comparable<GasStandardMean> {

	/**
	 * The actual concentration of the standard
	 */
	private StandardConcentration standardConcentration;
	
	/**
	 * The start time of the gas standard run
	 */
	private int startRow;
	
	/**
	 * The end time of the gas standard run
	 */
	private int endRow;
	
	/**
	 * The mean concentration measured during the run
	 */
	private double meanConcentration;
	
	/**
	 * The mean moisture measured during the run
	 */
	private double meanMoisture;
	
	/**
	 * Simple constructor to set all fields
	 * @param standardConcentration The true gas standard concentration
	 * @param startTime The start time of the gas standard run
	 * @param endTime The end time of the gas standard run
	 * @param meanConcentration The mean concentration measured during the run
	 * @param meanMoisture The mean moisture measured during the run
	 */
	public GasStandardMean(StandardConcentration standardConcentration, int startRow, int endRow, double meanConcentration, double meanMoisture) {
		this.standardConcentration = standardConcentration;
		this.startRow = startRow;
		this.endRow = endRow;
		this.meanConcentration = meanConcentration;
		this.meanMoisture = meanMoisture;
	}

	public StandardConcentration getStandardConcentration() {
		return standardConcentration;
	}

	public int getStartRow() {
		return startRow;
	}

	public int getEndRow() {
		return endRow;
	}

	public double getMeanConcentration() {
		return meanConcentration;
	}

	public double getMeanMoisture() {
		return meanMoisture;
	}

	@Override
	public int compareTo(GasStandardMean o) {
		return startRow - o.startRow;
	}
	
	public String getRunName() {
		return standardConcentration.getStandardName();
	}
}
