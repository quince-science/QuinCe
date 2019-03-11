package junit.uk.ac.exeter.QuinCe.web.html;

import uk.ac.exeter.QuinCe.web.html.HtmlUtils;

import java.util.ArrayList;

import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Tests for the HtmlUtils class
 *
 * @author Jonas F. Henriksen
 *
 */
public class HtmlUtilsTest {

  @Test
  public void testMakeJSONArray() throws JSONException {
    ArrayList<String> list = new ArrayList<String>();
    list.add("line 1");
    list.add("line 2");
    JSONAssert.assertEquals("[\"line 1\",\"line 2\"]",
        HtmlUtils.makeJSONArray(list), JSONCompareMode.LENIENT);
  }
}
