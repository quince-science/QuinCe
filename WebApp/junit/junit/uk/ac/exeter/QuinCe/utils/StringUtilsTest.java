package junit.uk.ac.exeter.QuinCe.utils;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Properties;

import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

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

  @Test
  public void testGetPropertiesAsJsonNull() throws JSONException {
    assertEquals("null", StringUtils.getPropertiesAsJson(null));
  }

  @Test
  public void testGetPropertiesAsJsonStrings() throws JSONException {
    Properties props = new Properties();
    String expectedJson = "{\"anykey\": \"anyvalue\", \"anykey.dot\": \"anothervalue\"}";
    props.put("anykey", "anyvalue");
    props.put("anykey.dot", "anothervalue");
    JSONAssert.assertEquals(
        expectedJson,
        StringUtils.getPropertiesAsJson(props),
        JSONCompareMode.LENIENT
    );
  }

  @Test
  public void testIntListToJsonArray() throws JSONException {
    ArrayList<Integer> list = new ArrayList<Integer>();
    list.add(new Integer(1));
    list.add(new Integer(2));
    JSONAssert.assertEquals("[1,2]", StringUtils.intListToJsonArray(list),
        JSONCompareMode.LENIENT);
  }

  @Test
  public void testJsonArrayToIntList() throws JSONException {
    ArrayList<Integer> list = new ArrayList<Integer>();
    list.add(new Integer(1));
    list.add(new Integer(2));
    assertEquals(list, StringUtils.jsonArrayToIntList("[1,2]"));
  }

  @Test
  public void testJsonArrayToIntListEmpty() throws JSONException {
    ArrayList<Integer> list = new ArrayList<Integer>();
    assertEquals(list, StringUtils.jsonArrayToIntList("[]"));
  }

}
