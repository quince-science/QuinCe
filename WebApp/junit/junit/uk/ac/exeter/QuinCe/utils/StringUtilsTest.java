package junit.uk.ac.exeter.QuinCe.utils;

import static org.junit.Assert.*;

import org.junit.Test;

import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Tests for the StringUtils class
 * @author Steve Jones
 *
 */
public class StringUtilsTest {

	/**
	 * Test an integer
	 */
	@Test
	public void testIsNumericInteger() {
		assertTrue(StringUtils.isNumeric("7"));
	}

	/**
	 * Test a decimal number
	 */
	@Test
	public void testIsNumericFloat() {
		assertTrue(StringUtils.isNumeric("67.5"));
	}

	/**
	 * Test a completely non-numeric string
	 */
	@Test
	public void testIsNumericNonNumeric() {
		assertFalse(StringUtils.isNumeric("I am not a number"));
	}

	/**
	 * Test a completely non-numeric string
	 */
	@Test
	public void testIsNumericNull() {
		assertFalse(StringUtils.isNumeric(null));
	}

	/**
	 * Test a completely non-numeric string
	 */
	@Test
	public void testIsNumericNan() {
		assertFalse(StringUtils.isNumeric("NaN"));
	}
}
