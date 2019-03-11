package uk.ac.exeter.QuinCe.web.html;

import java.util.List;

import com.google.gson.Gson;

/**
 * Various HTML-related utilities and constants
 * @author Steve Jones
 *
 */
public class HtmlUtils {

  public static final String CLASS_ERROR = "error";

  public static final String CLASS_INFO = "info";

  public static String makeJSONArray(List<String> lines) {
    Gson json = new Gson();
    return json.toJson(lines);
  }
}
