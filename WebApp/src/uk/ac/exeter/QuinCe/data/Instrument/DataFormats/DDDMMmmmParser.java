package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DDDMMmmmParser extends HDMParser {

  private static final int DEGREES = 1;

  private static final int MINUTES = 2;

  private boolean hasHemisphere;

  protected DDDMMmmmParser() {
    super();
    hasHemisphere = false;
  }

  protected DDDMMmmmParser(HemisphereMultiplier hemisphereMultiplier) {
    super(hemisphereMultiplier);
    hasHemisphere = true;
  }

  @Override
  protected void parseAction(String value) throws PositionParseException {

    String pattern;

    if (hasHemisphere) {
      pattern = "([\\d]*)(\\d\\d\\.[\\d]*)";
    } else {
      pattern = "([-\\d]*)(\\d\\d\\.[\\d]*)";
    }

    Pattern p = Pattern.compile(pattern);
    Matcher matcher = p.matcher(value);

    if (!matcher.matches()) {
      throw new PositionParseException(value);
    }

    degrees = Integer.parseInt(matcher.group(DEGREES));
    minutes = Double.parseDouble(matcher.group(MINUTES));

    if (hasHemisphere && degrees < 0) {
      throw new PositionParseException(value);
    }
  }
}
