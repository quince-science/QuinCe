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
	 * Test a completely non-numeric string
	 */
	@Test
	public void testIsNumericNonNumeric() {
		assertFalse(StringUtils.isNumeric("I am not a number"));
	}
}
