package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Class representing a set of calibrations of a given type for a given instrument.
 * 
 * <p>
 *   Calibrations can only be added to the set if they are for the correct instrument
 *   and of the correct type.
 * </p>
 *   
 * @author Steve Jones
 *
 */
public class CalibrationSet extends TreeSet<Calibration> {
	
	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = 1647218597328709319L;

	/**
	 * The ID of the instrument for to which this calibration set belongs
	 */
	private long instrumentId;
	
	/**
	 * The calibration type that is allowed in this set
	 */
	private String type;
	
	/**
	 * The set of targets that can be contained in this set
	 */
	private Collection<String> targets;
	
	/**
	 * Initialise an empty calibration set
	 * @param instrumentId The ID of the instrument to which the calibrations will belong
	 * @param type The calibration type
	 * @param targets The set of targets for the calibration set
	 * @throws MissingParamException If any required paramters are missing
	 */
	protected CalibrationSet(long instrumentId, String type, Collection<String> targets) throws MissingParamException {
		super();
		MissingParam.checkZeroPositive(instrumentId, "instrumentId");
		MissingParam.checkMissing(type, "type");
		MissingParam.checkMissing(targets, "targets");
		
		this.instrumentId = instrumentId;
		this.type = type;
		this.targets = targets;
		
		for (String target : targets) {
			add(new EmptyCalibration(instrumentId, type, target));
		}
	}
	
	@Override
	public boolean add(Calibration calibration) {
		if (calibration.getInstrumentId() != instrumentId) {
			throw new CalibrationException("Instrument ID does not match");
		}

		if (!type.equals(calibration.getType())) {
			throw new CalibrationException("Incorrect calibraiton type");
		}
		
		if (!targets.contains(calibration.getTarget())) {
			throw new CalibrationException("Calibration with target '" + calibration.getTarget() + "' is not allowed in this set");
		}

		if (contains(calibration)) {
			super.remove(calibration);
		}
		
		super.add(calibration);
		
		return true;
	}
	
	@Override
	public boolean addAll(Collection<? extends Calibration> c) {
		for (Calibration calibration : c) {
			add(calibration);
		}
		
		return true;
	}
	
	/**
	 * Determines whether or not a {@code Calibration} for the
	 * specified target has been added to the set. The method 
	 * does not check whether or not the target is in the list
	 * of allowed targets.
	 * 
	 * Empty calibrations are not detected by this method.
	 * 
	 * @param target The target to find
	 * @return {@code true} if a calibration for the target is found; {@code false} otherwise
	 */
	public boolean containsTarget(String target) {
		boolean result = false;
		
		for (Calibration calibration : this) {
			if (!(calibration instanceof EmptyCalibration) && calibration.getTarget().equals(target)) {
				result = true;
				break;
			}
		}
		
		return result;
	}
	
	/**
	 * Get the contents of the calibration set as a {@link List}.
	 * 
	 * Required for JSF.
	 * @return The calibration set as a {@link List}.
	 */
	public List<Calibration> asList() {
		return new ArrayList<Calibration>(this);
	}
	
	/**
	 * Determines whether or not the calibration set contains a {@link Calibration}
	 * for all the targets specified for the set.
	 * @return {@code true} if a Calibration has been added for each target; {@code false} otherwise
	 * @see #targets
	 */
	public boolean isComplete() {
		boolean result = false;
		
		List<String> addedTargets = new ArrayList<String>();
		for (Calibration calibration : this) {
			if (!(calibration instanceof EmptyCalibration)) {
				addedTargets.add(calibration.getTarget());
			}
		}
		
		/*
		 * Since we can only add calibrations for targets in the original
		 * targets list, then by definition the list of added targets will
		 * be the same size as the original targets list if, and only if,
		 * all targets have been added.
		 */
		if (addedTargets.size() == targets.size()) {
			result = true;
		}
		
		return result;
	}
}
