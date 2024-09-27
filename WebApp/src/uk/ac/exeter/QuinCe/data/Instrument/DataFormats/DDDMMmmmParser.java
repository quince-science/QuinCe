package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DDDMMmmmParser extends HDMParser {

  private static final Pattern HAS_HEMISPHERE_PATTERN = Pattern
    .compile("([\\d]*)(\\d\\d\\.[\\d]*)");

  private static final Pattern NO_HEMISPHERE_PATTERN = Pattern
    .compile("([-\\d]*)(\\d\\d\\.[\\d]*)");

  private static final int DEGREES = 1;

  private static final int MINUTES = 2;

  private boolean hasHemisphere;

  protected DDDMMmmmParser(boolean hasHemisphere) {
    super();
    hasHemisphere = false;
  }

  @Override
  protected void parseAction(String value) throws PositionParseException {

    Pattern p = hasHemisphere ? HAS_HEMISPHERE_PATTERN : NO_HEMISPHERE_PATTERN;
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
