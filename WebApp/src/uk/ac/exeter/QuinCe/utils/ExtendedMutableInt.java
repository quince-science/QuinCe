package uk.ac.exeter.QuinCe.utils;

import org.apache.commons.lang3.mutable.MutableInt;

/**
 * An extension of the Apache Commons MutableInt class
 * with some useful functions
 * @author Steve Jones
 * @see MutableInt
 */
public class ExtendedMutableInt extends MutableInt {

	/**
	 * The serial version UID
	 */
	private static final long serialVersionUID = -376200292110822097L;

	/**
	 * Simple constructor for an {@code int} value
	 * @param value The initial value
	 */
	public ExtendedMutableInt(int value) {
		super(value);
	}
	
	/**
	 * See if the value of this object is greater than the specified value
	 * @param otherValue The value to compare
	 * @return {@code this > otherValue}
	 */
	public boolean greaterThan(int otherValue) {
		return (getValue() > otherValue);
	}
	
	/**
	 * Create a new {@code ExtendedMutableInt} object with
	 * a value one higher than this object's value.
	 * @return The new object
	 */
	public ExtendedMutableInt incrementedClone() {
		return new ExtendedMutableInt(getValue() + 1);
	}
}
