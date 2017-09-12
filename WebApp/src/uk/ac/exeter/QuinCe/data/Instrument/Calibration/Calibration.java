package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Base class for a calibration.
 * 
 * <p>
 *   {@code Calibration} classes can be used for gas standards,
 *   sensor calibrations or external calibrations.
 * </p>
 * 
 * <p>
 *   All calibrations will be held in the same table in the database,
 *   distinguished by a {@code type} field
 * </p>
 * 
 * <p>
 *   Comparison operations on this class compare the instrument ID,
 *   type and target in that order.
 * </p>
 * 
 * @author Steve Jones
 *
 */
public abstract class Calibration implements Comparable<Calibration> {

	/**
	 * The instrument to which this calibration belongs 
	 */
	private long instrumentId;
	
	/**
	 * The calibration type
	 */
	protected String type = null;
	
	/**
	 * The date and time of the deployment. Some calibrations do not have a time,
	 * in which case the time portion will be set to midnight.
	 * @see #hasTime
	 */
	private ZonedDateTime deploymentDate = null;
	
	/**
	 * The part of the instrument to which this calibration applies.
	 * Examples are the name of a gas standard, sensor etc.
	 */
	private String target = null;

	/**
	 * The values for the calibration. The list must contain
	 * the same number of entries as the list of value names
	 * returned by {@link #getCoefficientNames()}.
	 * @see #getCoefficientNames()
	 */
	protected List<Double> coefficients = null;
	
	/**
	 * The formatter for dates
	 */
	private static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	/**
	 * The formatter for date/times
	 */
	private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	
	/**
	 * Create an empty calibration for an instrument 
	 * @param instrumentId The instrument's database ID
	 */
	protected Calibration(long instrumentId, String type) {
		this.instrumentId = instrumentId;
		this.type = type;
	}
	
	/**
	 * Create an empty calibration for a specified target
	 * @param instrumentId The instrument ID
	 * @param type The calibration type
	 * @param target The target
	 */
	protected Calibration(long instrumentId, String type, String target) {
		this.instrumentId = instrumentId;
		this.type = type;
		this.target = target;
	}
	
	/**
	 * Get the human-readable names of the values to be stored for the calibration
	 * @return The value names
	 */
	public abstract List<String> getCoefficientNames();
	
	/**
	 * Get the type of the calibration. This is provided
	 * by each of the concrete implementations of the class
	 * @return The calibration type
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * Indicates whether or not the {@code deploymentDate} should
	 * have a time component. Most calibrations do not have a time,
	 * so the default method returns {@code false}.
	 * @see #deploymentDate
	 */
	protected boolean hasTime() {
		return false;
	}
	
	/**
	 * Get the calibration values as a human-readable string.
	 * 
	 * <p>
	 *   If either the deployment date or coefficients are
	 *   {@code null}, the method assumes that the coefficients
	 *   are not set and returns a default {@code "Not set"} value.
	 * </p>
	 * @return The calibration values string
	 */
	public String getHumanReadableCoefficients() {
		String result;
		
		if (null == deploymentDate) {
			result = "Not set";
		} else {
			if (null == coefficients) {
				result = "Not set";
			} else {
				result = buildHumanReadableCoefficients();
			}
		}
		
		return result;
	}
	
	/**
	 * Build the human-readable coefficients string for
	 * {@link #getHumanReadableCoefficients()}.
	 * @return The human-readable coefficients
	 */
	protected abstract String buildHumanReadableCoefficients();
	
	/**
	 * Get the calibration target
	 * @return The target
	 */
	public String getTarget() {
		return target;
	}
	
	/**
	 * Set the calibration target
	 * @param target The target
	 */
	public void setTarget(String target) {
		this.target = target;
	}
	
	/**
	 * Get the deployment date as a {@link Date} object
	 * @return The deployment date
	 */
	public Date getDeploymentDateAsDate() {
		Date result = null;

		if (null != deploymentDate) {
			result = new Date(deploymentDate.toInstant().toEpochMilli());
		}
		
		return result;
	}
	
	/**
	 * Set the deployment date using a {@link Date} object
	 * @param deploymentDate The deployment date
	 */
	public void setDeploymentDateAsDate(Date deploymentDate) {
		if (null == deploymentDate) {
			this.deploymentDate = null;
		} else {
			this.deploymentDate = ZonedDateTime.ofInstant(deploymentDate.toInstant(), ZoneOffset.UTC);
			if (!hasTime()) {
				this.deploymentDate = this.deploymentDate.with(LocalTime.MIDNIGHT);
			}
		}
		
		if (null == coefficients) {
			initialiseCoefficients();
		}
	}
	
	/**
	 * Get the database ID of the instrument to which this calibration applies
	 * @return The instrument ID
	 */
	public long getInstrumentId() {
		return instrumentId;
	}
	
	/**
	 * Get the calibration values as a semicolon-delimited list
	 * @return The calibration values
	 */
	public String getCoefficientsAsDelimitedList() {
		return StringUtils.listToDelimited(coefficients, ";");
	}
	
	/**
	 * Get the deployment date as milliseconds since the epoch
	 * @return The deployment date in milliseconds
	 */
	public long getDeploymentDateAsMillis() {
		long result = Long.MIN_VALUE;
		
		if (null != deploymentDate) {
			result = deploymentDate.toInstant().toEpochMilli();
		}
		
		return result;
	}
	
	/**
	 * Get the deployment date as a formatted string
	 * @return The deployment date string
	 */
	public String getDeploymentDateAsString() {
		String result;
		
		if (null == deploymentDate || null == coefficients) {
			result = "Not set";
		} else {
			if (hasTime()) {
				result = deploymentDate.format(dateTimeFormatter);
			} else {
				result = deploymentDate.format(dateFormatter);
			}
		}
		
		return result;
	}
	
	/**
	 * Initialise the coefficients for this calibration with zero values
	 */
	protected void initialiseCoefficients() {
		coefficients = new ArrayList<Double>(getCoefficientNames().size());
		for (int i = 0; i < getCoefficientNames().size(); i++) {
			coefficients.add(0.0);
		}
	}
	
	/**
	 * Set the coefficients for this calibration
	 * @param coefficients The coefficients
	 * @throws CalibrationException If an incorrect number of coefficients is supplied
	 */
	public void setCoefficients(List<Double> coefficients) throws CalibrationException {
		if (coefficients.size() != getCoefficientNames().size()) {
			throw new CalibrationException("Incorrect number of coefficients: expected " + getCoefficientNames().size() + ", got " + coefficients.size());
		}
		
		this.coefficients = coefficients;
	}
	
	/**
	 * Check to ensure that this calibration is valid.
	 * 
	 * <p>
	 *   To pass validation, both a {@link #deploymentDate} and {@link #coefficients} must be
	 *   present, and the coefficients must be valid.
	 * </p>
	 * 
	 * @return {@code true} if the calibration is valid; {@code false} if it is not
	 * @see #coefficientsValid()
	 */
	public boolean validate() {
		boolean valid = true;
		
		if (null == deploymentDate || null == coefficients) {
			valid = false;
		} else {
			valid = coefficientsValid();
		}
		
		return valid;
	}
	
	/**
	 * Determine whether the calibration coefficients are valid
	 * @return {@code true} if the coefficients are valid; {@code false} if they are not
	 */
	public abstract boolean coefficientsValid();
	
	@Override
	public int compareTo(Calibration o) {
		int result = (int) (this.instrumentId - o.instrumentId);
		
		if (result == 0) {
			result = this.type.compareTo(o.type);
		}
		
		if (result == 0) {
			result = this.target.compareTo(o.target);
		}
		
		return result;
	}
}
