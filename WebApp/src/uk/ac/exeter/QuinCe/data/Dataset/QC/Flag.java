package uk.ac.exeter.QuinCe.data.Dataset.QC;

import java.util.Collection;
import java.util.HashMap;

/**
 * Represents a Flag placed on a data record.
 *
 * <p>
 * Flags are based on the <a href=
 * "https://exchange-format.readthedocs.io/en/latest/quality.html#woce-ctd-quality-codes">WOCE
 * CTD Quality Codes</a>, with primary values for Good (2), Questionable (3) and
 * Bad (4). This convention is used in the global
 * <a href="https://www.socat.info">SOCAT</a> database. All records exported
 * from a system should ultimately have one of these three flags assigned to it.
 * However, during processing a number of other flag values can be useful. The
 * complete list of flag values used in QuinCe is in the table below.
 * </p>
 *
 * <table>
 * <caption>All flag values used in QuinCe.</caption>
 * <tr>
 * <th>Flag</th>
 * <th>Value</th>
 * <th>Meaning</th>
 * </tr>
 * <tr>
 * <td>Not Calibrated</td>
 * <td style="text-align: right">{@code 1}</td>
 * <td>The value has not been calibrated.</td>
 * </tr>
 * <tr>
 * <td>Good</td>
 * <td style="text-align: right">{@code 2}</td>
 * <td>The value is good and can be used.</td>
 * </tr>
 * <tr>
 * <td>Questionable</td>
 * <td style="text-align: right">{@code 3}</td>
 * <td>The value is of questionable quality.</td>
 * </tr>
 * <tr>
 * <td>Bad</td>
 * <td style="text-align: right">{@code 4}</td>
 * <td>The value is bad and should not be used.</td>
 * </tr>
 * <tr>
 * <td>No QC</td>
 * <td style="text-align: right">{@code 0}</td>
 * <td>The value has not yet been quality controlled.</td>
 * </tr>
 * <tr>
 * <td>Assumed Good</td>
 * <td style="text-align: right">{@code -2}</td>
 * <td>The value is assumed to be good. This is set by automatic QC routines
 * that do not find anything wrong with the checked values.</td>
 * </tr>
 * <tr>
 * <td>Flushing</td>
 * <td style="text-align: right">{@code -100}</td>
 * <td>The instrument is in Flushing mode, so values should be ignored.</td>
 * </tr>
 * <tr>
 * <td>Needed</td>
 * <td style="text-align: right">{@code -10}</td>
 * <td>A flag must be assigned manually by a human.</td>
 * </tr>
 * <tr>
 * <td>Lookup</td>
 * <td style="text-align: right">{@code -200}</td>
 * <td>The flag for this value is inherited from another value. The value ID(s)
 * are stored in the QC comment. These flags cannot be overridden by the
 * user.</td>
 * </tr>
 * </table>
 *
 * <p>
 * Each flag value has a corresponding text value for display to users.
 * </p>
 *
 * <p>
 * In many cases a single value could have multiple possible Flags, e.g. a
 * calculated value will have its Flag set according to the Flags of the values
 * used in that calculation. Flags are therefore given relative significance
 * values so the correct final Flag for those values is set. The significance
 * order of Flags is as follows:
 * </p>
 *
 * *
 * <ul>
 * <li>{@link #FLUSHING}</li>
 * <li>{@link #LOOKUP}</li>
 * <li>{@link #NEEDED}</li>
 * <li>{@link #NO_QC}</li>
 * <li>{@link #BAD}</li>
 * <li>{@link #QUESTIONABLE}</li>
 * <li>{@link #NOT_CALIBRATED}</li>
 * <li>{@link #GOOD}/{@link #ASSUMED_GOOD}</li>
 * </ul>
 *
 * <p>
 * This class provides instances of each type of flag to remove the need to
 * repeatedly construct them. These instances can be used repeatedly without
 * issues in most cases.
 * </p>
 */
public class Flag {

  /**
   * Numeric value for a {@link #NO_QC} flag.
   */
  public static final int VALUE_NO_QC = 0;

  /**
   * Text value for a {@link #NO_QC} flag.
   */
  protected static final String TEXT_NO_QC = "No QC";

  /**
   * Numeric value for a {@link #NOT_CALIBRATED} flag.
   */
  public static final int VALUE_NOT_CALIBRATED = 1;

  /**
   * Text value for a {@link #NOT_CALIBRATED} flag.
   */
  protected static final String TEXT_NOT_CALIBRATED = "Not calibrated";

  /**
   * Numeric value for a {@link #GOOD} flag.
   */
  public static final int VALUE_GOOD = 2;

  /**
   * Text value for a {@link #GOOD} flag.
   */
  protected static final String TEXT_GOOD = "Good";

  /**
   * Numeric value for an {@link #ASSUMED_GOOD} flag.
   */
  public static final int VALUE_ASSUMED_GOOD = -2;

  /**
   * Text value for an {@link #ASSUMED_GOOD} flag.
   */
  protected static final String TEXT_ASSUMED_GOOD = "Assumed Good";

  /**
   * Numeric value for a {@link #QUESTIONABLE} flag.
   */
  public static final int VALUE_QUESTIONABLE = 3;

  /**
   * Text value for a {@link #QUESTIONABLE} flag.
   */
  protected static final String TEXT_QUESTIONABLE = "Questionable";

  /**
   * Numeric value for a {@link #BAD} flag.
   */
  public static final int VALUE_BAD = 4;

  /**
   * Text value for a {@link #BAD} flag.
   */
  protected static final String TEXT_BAD = "Bad";

  /**
   * Numeric value for a {@link #NEEDED} flag.
   */
  public static final int VALUE_NEEDED = -10;

  /**
   * Text value for a {@link #NEEDED} flag.
   */
  protected static final String TEXT_NEEDED = "Needed";

  /**
   * Numeric value for a {@link #FLUSHING} flag.
   */
  public static final int VALUE_FLUSHING = -100;

  /**
   * Text value for a {@link #FLUSHING} flag.
   */
  protected static final String TEXT_FLUSHING = "In flushing time";

  /**
   * Numeric value for a {@link #LOOKUP} flag.
   */
  public static final int VALUE_LOOKUP = -200;

  /**
   * Text value for a {@link #LOOKUP} flag.
   */
  protected static final String TEXT_LOOKUP = "Lookup";

  /**
   * Reusable instance of a flag indicating that no QC has been performed.
   */
  public static final Flag NO_QC = makeNoQCFlag();

  /**
   * Reusable instance of a flag indicating that a value has not been
   * calibrated.
   */
  public static final Flag NOT_CALIBRATED = makeNotCalibratedFlag();

  /**
   * Reusable instance of a flag indicating that a value is good and can be
   * used.
   */
  public static final Flag GOOD = makeGoodFlag();

  /**
   * Reusable instance of a flag indicating that automatic QC has not found any
   * fault with a value.
   */
  public static final Flag ASSUMED_GOOD = makeAssumedGoodFlag();

  /**
   * Reusable instance of a flag indicating that a value's quality is
   * questionable.
   */
  public static final Flag QUESTIONABLE = makeQuestionableFlag();

  /**
   * Reusable instance of a flag indicating that a value is bad and should not
   * be used.
   */
  public static final Flag BAD = makeBadFlag();

  /**
   * Reusable instance of a flag indicating that a human must provide a QC flag.
   */
  public static final Flag NEEDED = makeNeededFlag();

  /**
   * Reusable instance of a flag indicating that the instrument is in flushing
   * mode and values should be ignored.
   */
  public static final Flag FLUSHING = makeFlushingFlag();

  /**
   * Reusable instance of a flag indicating that a value's QC flag is linked to
   * the QC flag of another value.
   */
  public static final Flag LOOKUP = makeLookupQCFlag();

  /**
   * Stores the relative significance of all Flag types.
   *
   * Larger numbers in the Map indicate more significant Flags.
   *
   * @see #moreSignificantThan(Flag)
   * @see #lessSignificantThan(Flag)
   */
  private static HashMap<Flag, Integer> FLAG_SIGNIFICANCE;

  /**
   * The numeric value for this flag.
   */
  private int flagValue;

  static {
    FLAG_SIGNIFICANCE = new HashMap<Flag, Integer>();

    FLAG_SIGNIFICANCE.put(Flag.FLUSHING, 70);
    FLAG_SIGNIFICANCE.put(Flag.LOOKUP, 60);
    FLAG_SIGNIFICANCE.put(Flag.NEEDED, 50);
    FLAG_SIGNIFICANCE.put(Flag.NO_QC, 40);
    FLAG_SIGNIFICANCE.put(Flag.BAD, 30);
    FLAG_SIGNIFICANCE.put(Flag.QUESTIONABLE, 20);
    FLAG_SIGNIFICANCE.put(Flag.NOT_CALIBRATED, 10);
    FLAG_SIGNIFICANCE.put(Flag.GOOD, 00);
    FLAG_SIGNIFICANCE.put(Flag.ASSUMED_GOOD, 00);
  }

  /**
   * Creates a flag instance with the specified numeric value.
   *
   * @param flagValue
   *          The flag's numeric value.
   * @throws InvalidFlagException
   *           If the flag value is invalid
   */
  public Flag(int flagValue) throws InvalidFlagException {
    if (!isValidFlagValue(flagValue)) {
      throw new InvalidFlagException(flagValue);
    }

    this.flagValue = flagValue;
  }

  /**
   * <b>Use only for unit testing.</b>
   *
   * Create a flag instance from either a numeric value or a single character
   * indicating the flag type.
   *
   * @param flagLetter
   *          The flag character
   * @throws InvalidFlagException
   *           If the character is not recognised.
   */
  public Flag(char flagLetter) throws InvalidFlagException {
    switch (Character.toUpperCase(flagLetter)) {
    case 'C':
    case '1': {
      this.flagValue = VALUE_NOT_CALIBRATED;
      break;
    }
    case 'G':
    case '2': {
      this.flagValue = VALUE_GOOD;
      break;
    }
    case 'A': {
      this.flagValue = VALUE_ASSUMED_GOOD;
      break;
    }
    case 'Q':
    case '3': {
      this.flagValue = VALUE_QUESTIONABLE;
      break;
    }
    case 'B':
    case '4': {
      this.flagValue = VALUE_BAD;
      break;
    }
    case 'N': {
      this.flagValue = VALUE_NEEDED;
      break;
    }
    case 'F': {
      this.flagValue = VALUE_FLUSHING;
      break;
    }
    case 'X': {
      this.flagValue = VALUE_NO_QC;
      break;
    }
    case 'L': {
      this.flagValue = VALUE_LOOKUP;
      break;
    }
    default: {
      throw new InvalidFlagException(flagLetter);
    }
    }
  }

  /**
   * Create a new basic flag based on an existing flag (usually an extending
   * class). Used by internal classes only.
   *
   * @param sourceFlag
   *          The source flag
   */
  protected Flag(Flag sourceFlag) {
    this.flagValue = sourceFlag.flagValue;
  }

  /**
   * Get the flag's numeric value
   *
   * @return The flag's numeric value
   */
  public int getFlagValue() {
    return flagValue;
  }

  /**
   * Get the flag's text value.
   */
  @Override
  public String toString() {
    String result;

    switch (flagValue) {
    case VALUE_NO_QC: {
      result = TEXT_NO_QC;
      break;
    }
    case VALUE_NOT_CALIBRATED: {
      result = TEXT_NOT_CALIBRATED;
      break;
    }
    case VALUE_GOOD: {
      result = TEXT_GOOD;
      break;
    }
    case VALUE_ASSUMED_GOOD: {
      result = TEXT_ASSUMED_GOOD;
      break;
    }
    case VALUE_QUESTIONABLE: {
      result = TEXT_QUESTIONABLE;
      break;
    }
    case VALUE_BAD: {
      result = TEXT_BAD;
      break;
    }
    case VALUE_NEEDED: {
      result = TEXT_NEEDED;
      break;
    }
    case VALUE_FLUSHING: {
      result = TEXT_FLUSHING;
      break;
    }
    case VALUE_LOOKUP: {
      result = TEXT_LOOKUP;
      break;
    }
    default: {
      // This should never happen!
      result = "***INVALID FLAG VALUE***";
    }
    }

    return result;
  }

  /**
   * Checks to ensure that a numeric flag value is valid.
   *
   * @param value
   *          The flag value
   * @return {@code true} if the flag value is valid; {@code false} if it is not
   */
  public static boolean isValidFlagValue(int value) {
    return (value == VALUE_NO_QC || value == VALUE_NOT_CALIBRATED
      || value == VALUE_GOOD || value == VALUE_ASSUMED_GOOD
      || value == VALUE_QUESTIONABLE || value == VALUE_BAD
      || value == VALUE_NEEDED || value == VALUE_FLUSHING
      || value == VALUE_LOOKUP);
  }

  /**
   * Create an instance of a No QC flag.
   *
   * @return A No QC flag.
   */
  private static Flag makeNoQCFlag() {
    Flag flag = null;
    try {
      flag = new Flag(VALUE_NO_QC);
    } catch (InvalidFlagException e) {
      // This won't be thrown; do nothing
    }

    return flag;
  }

  /**
   * Create an instance of a Not Calibrated flag.
   *
   * @return A Good flag.
   */
  private static Flag makeNotCalibratedFlag() {
    Flag flag = null;
    try {
      flag = new Flag(VALUE_NOT_CALIBRATED);
    } catch (InvalidFlagException e) {
      // This won't be thrown; do nothing
    }

    return flag;
  }

  /**
   * Create an instance of a Good flag.
   *
   * @return A Good flag.
   */
  private static Flag makeGoodFlag() {
    Flag flag = null;
    try {
      flag = new Flag(VALUE_GOOD);
    } catch (InvalidFlagException e) {
      // This won't be thrown; do nothing
    }

    return flag;
  }

  /**
   * Create an instance of an Assumed Good flag.
   *
   * @return An Assumed Good flag.
   */
  private static Flag makeAssumedGoodFlag() {
    Flag flag = null;
    try {
      flag = new Flag(VALUE_ASSUMED_GOOD);
    } catch (InvalidFlagException e) {
      // This won't be thrown; do nothing
    }

    return flag;
  }

  /**
   * Create an instance of a Questionable flag.
   *
   * @return A Questionable flag.
   */
  private static Flag makeQuestionableFlag() {
    Flag flag = null;
    try {
      flag = new Flag(VALUE_QUESTIONABLE);
    } catch (InvalidFlagException e) {
      // This won't be thrown; do nothing
    }

    return flag;
  }

  /**
   * Create an instance of a Bad flag.
   *
   * @return A Bad flag.
   */
  private static Flag makeBadFlag() {
    Flag flag = null;
    try {
      flag = new Flag(VALUE_BAD);
    } catch (InvalidFlagException e) {
      // This won't be thrown; do nothing
    }

    return flag;
  }

  /**
   * Create an instance of a Needed flag.
   *
   * @return A Needed flag.
   */
  private static Flag makeNeededFlag() {
    Flag flag = null;
    try {
      flag = new Flag(VALUE_NEEDED);
    } catch (InvalidFlagException e) {
      // This won't be thrown; do nothing
    }

    return flag;
  }

  /**
   * Create an instance of a Flushing flag.
   *
   * @return A Flushing flag.
   */
  private static Flag makeFlushingFlag() {
    Flag flag = null;
    try {
      flag = new Flag(VALUE_FLUSHING);
    } catch (InvalidFlagException e) {
      // This won't be thrown; do nothing
    }

    return flag;
  }

  /**
   * Create an instance of a Lookup flag.
   *
   * @return A Lookup flag.
   */
  private static Flag makeLookupQCFlag() {
    Flag flag = null;
    try {
      flag = new Flag(VALUE_LOOKUP);
    } catch (InvalidFlagException e) {
      // This won't be thrown; do nothing
    }

    return flag;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + flagValue;
    return result;
  }

  /**
   * Establishes equality using the numeric flag value.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof Flag))
      return false;
    Flag other = (Flag) obj;
    if (flagValue != other.flagValue)
      return false;
    return true;
  }

  /**
   * Determines whether or not this Flag is more significant than the specified
   * Flag.
   *
   * @param otherFlag
   *          The flag to be compared
   * @return {@code true} if this flag is more significant than the supplied
   *         flag; {@code false} if it is not.
   * @see #FLAG_SIGNIFICANCE
   */
  public boolean moreSignificantThan(Flag otherFlag) {
    return FLAG_SIGNIFICANCE.get(this) > FLAG_SIGNIFICANCE.get(otherFlag);
  }

  /**
   * Determines whether or not this Flag is less significant than the specified
   * Flag.
   *
   * @param otherFlag
   *          The flag to be compared.
   * @return {@code true} if this flag is less significant than the supplied
   *         flag; {@code false} if it is not.
   */
  public boolean lessSignificantThan(Flag otherFlag) {
    return FLAG_SIGNIFICANCE.get(this) < FLAG_SIGNIFICANCE.get(otherFlag);
  }

  /**
   * Determines whether or not this flag represents a Good value. Both Good and
   * Assumed Good flags pass the check.
   *
   * @return {@code true} if this flag is Good; {@code false} if it is not.
   */
  public boolean isGood() {
    return Math.abs(flagValue) == VALUE_GOOD;
  }

  /**
   * Return the WOCE value for a flag.
   *
   * @return The WOCE value for the flag
   * @see #getWoceValue(int)
   */
  public int getWoceValue() {
    return getWoceValue(flagValue);
  }

  /**
   * Return the WOCE value for a given flag value
   * <ul>
   * <li>Good and Assumed Good will return 2</li>
   * <li>Questionable will return 3</li>
   * <li>Bad and Fatal will return 4</li>
   * <li>All other flag types will return -1, because there is no corresponding
   * WOCE value.</li>
   * </ul>
   *
   * @param flagValue
   *          The numeric flag value
   * @return The WOCE value for the flag
   */
  public static int getWoceValue(int flagValue) {
    int result;

    switch (flagValue) {
    case VALUE_NOT_CALIBRATED: {
      result = 1;
      break;
    }
    case VALUE_GOOD:
    case VALUE_ASSUMED_GOOD: {
      result = 2;
      break;
    }
    case VALUE_QUESTIONABLE: {
      result = 3;
      break;
    }
    case VALUE_BAD: {
      result = 4;
      break;
    }
    default: {
      result = -1;
    }
    }

    return result;
  }

  /**
   * Check whether the supplied flag is of equal significance to this flag.
   *
   * @param otherFlag
   *          The flag to be compared.
   * @return {@code true} if the supplied flag is of equal significance to this
   *         flag; {@code false} otherwise.
   */
  public boolean equalSignificance(Flag otherFlag) {
    return FLAG_SIGNIFICANCE.get(this) == FLAG_SIGNIFICANCE.get(otherFlag);
  }

  /**
   * Get the fully simplified version of the Flag.
   *
   * <p>
   * Returns the basic {@link Flag} object on which a subclassed {@link Flag} is
   * based.
   * </p>
   *
   * @return The raw Flag object.
   */
  public Flag getSimpleFlag() {
    return new Flag(this);
  }

  /**
   * Get the flag with the highest significance from a the supplied {@link Flag}
   * objects.
   *
   * @param flags
   *          The flags to check.
   * @return The most significant flag.
   */
  public static Flag getMostSignificantFlag(Flag... flags) {
    Flag result = null;

    for (Flag flag : flags) {
      if (null != flag) {
        if (null == result || flag.moreSignificantThan(result)) {
          result = flag;
        }
      }
    }

    return result;
  }

  public boolean commentRequired() {
    return flagValue == 3 || flagValue == 4 || flagValue == -200;
  }

  public static boolean containsWorseFlag(Collection<Flag> flags,
    Flag reference) {

    return flags.stream().anyMatch(f -> f.moreSignificantThan(reference));
  }
}
