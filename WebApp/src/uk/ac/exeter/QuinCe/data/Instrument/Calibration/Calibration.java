package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
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
 * @author Steve Jones
 *
 */
public abstract class Calibration {

	/**
	 * The instrument to which this calibration belongs 
	 */
	private long instrumentId;
	
	/**
	 * The date and time of the deployment. Some calibrations do not have a time,
	 * in which case the time portion will be set to midnight.
	 * @see #hasTime
	 */
	private LocalDateTime deploymentDate = null;
	
	/**
	 * The part of the instrument to which this calibration applies.
	 * Examples are the name of a gas standard, sensor etc.
	 */
	private String target = null;

	/**
	 * The values for the calibration. The list must contain
	 * the same number of entries as the list of value names
	 * returned by {@link #getValueNames()}.
	 * @see #getValueNames()
	 */
	protected List<Double> values = null;
	
	/**
	 * Create an empty calibration for an instrument 
	 * @param instrumentId The instrument's database ID
	 */
	public Calibration(long instrumentId) {
		this.instrumentId = instrumentId;
		
		// Initialise the list of values
		values = new ArrayList<Double>(getValueNames().size());
		for (int i = 0; i < getValueNames().size(); i++) {
			values.add(0.0);
		}
	}
	
	/**
	 * Get the human-readable names of the values to be stored for the calibration
	 * @return The value names
	 */
	public abstract List<String> getValueNames();
	
	/**
	 * Get the type of the calibration. This is provided
	 * by each of the concrete implementations of the class
	 * @return The calibration type
	 */
	protected abstract String getType();
	
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
	 * Get the calibration values as a human-readable string
	 * @return The calibration values string
	 */
	public abstract String getHumanReadableValues();
	
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
			ZonedDateTime zoned = deploymentDate.atZone(ZoneOffset.UTC);
			result = new Date(zoned.toInstant().toEpochMilli());
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
			this.deploymentDate = LocalDateTime.ofInstant(deploymentDate.toInstant(), ZoneOffset.UTC);
			if (!hasTime()) {
				this.deploymentDate = this.deploymentDate.with(LocalTime.MIDNIGHT);
			}
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
	public String getValuesAsDelimitedList() {
		return StringUtils.listToDelimited(values, ";");
	}
	
	/**
	 * Get the deployment date as milliseconds since the epoch
	 * @return The deployment date in milliseconds
	 */
	public long getDeploymentDateAsMillis() {
		long result = Long.MIN_VALUE;
		
		if (null != deploymentDate) {
			result = deploymentDate.atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
		}
		
		return result;
	}
}
