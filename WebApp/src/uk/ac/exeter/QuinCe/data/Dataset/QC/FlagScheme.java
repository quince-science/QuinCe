package uk.ac.exeter.QuinCe.data.Dataset.QC;

import java.util.Collection;
import java.util.List;

/**
 * Defines a flagging scheme for a particular type of data.
 * 
 * <p>
 * Different types of data use different flagging schemes with different numbers
 * of flags and different meanings. For example, ICOS flags are based on SOCAT
 * flags (which are in turn based on WOCE flags). Argo data has its own set of
 * flags.
 * </p>
 * 
 * <p>
 * Each {@link Instrument} will be assigned a single flagging scheme. At the
 * time of writing, this is anticipated to be linked to the {@link Instrument}'s
 * measurement basis (see {@link Instrument#getBasis}.
 * </p>
 * 
 * <p>
 * A {@code FlagScheme} defines the available QC flags and their hierarchy of
 * significance (e.g. for ICOS flags, BAD &gt; QUESTIONABLE &gt; GOOD).
 * </p>
 * 
 * <p>
 * There is the special concept of a <i>Good</i> flag, which is a flag
 * indicating that the data is fully accepted with no questions over its
 * quality. The <i>Good</i> flag has an <i>Assumed Good</i> counterpart which is
 * automatically created, and indicates that any automatic QC checks have no
 * found any reason to think that a value has any issues. This distinguishes
 * untouched values from those which have been explicitly flagged as <i>Good</i>
 * by a user. The <i>Good</i> flag must be registered by calling
 * {@link #registerGoodFlag(Flag)} instead of {@link #registerFlag(Flag)}. There
 * must be one and only one <i>Good</i> flag registered for each scheme.
 * </p>
 * 
 * <p>
 * There is also the concept of a <i>Bad</i> flag, indicating a value that
 * should not be used because it is invalid in some way.
 * </p>
 * 
 * <p>
 * All implementations of this interface <b>must</b> contain a set of universal
 * {@link Flag}s that correspond to processing actions within QuinCe. These will
 * include:
 * </p>
 * <ul>
 * <li>{@link #NO_QC_FLAG}: No QC of any type has been performed.</li>
 * <li>{@link #NEEDED_FLAG}: The user must confirm the result of automatic
 * QC.</li>
 * <li>{@link #FLUSHING_FLAG}: indicates that the flag for a value is defined by
 * the flag on another value. The QC message will specify the location of the
 * other flag, but the exact details are not defined here.</li>
 * <li>{@link #FLUSHING_FLAG}: The instrument is flushing after a change in
 * operation mode.</li>
 * </ul>
 * 
 * <p>
 * Each concrete implementation of this class is expected to be a singleton.
 * </p>
 */
public interface FlagScheme {

  /**
   * A fixed {@link Flag} to indicate that the instrument is in a flushing
   * period after switching measurement modes.
   */
  static final Flag FLUSHING_FLAG = new Flag(-100, "Flushing", 'F', 130, false,
    false, -1);

  /**
   * A fixed {@link Flag} to indicate no QC has been performed on a value.
   */
  static final Flag NO_QC_FLAG = new Flag(0, "No QC", 'X', 100, false, false,
    0);

  /**
   * A fixed {@link Flag} to indicate a user must confirm an automatic QC flag.
   */
  static final Flag NEEDED_FLAG = new Flag(-10, "Needed", 'N', 110, false,
    false, -1);

  /**
   * A fixed {@link Flag} to indicate that the flag for a value is defined as
   * the QC Flag from another value.
   * 
   * <p>
   * The QC message should be used to determine the other {@link Flag}, but the
   * details are not defined here as there may be multiple approaches depending
   * on the situation.
   * </p>
   */
  static final Flag LOOKUP_FLAG = new Flag(-200, "Lookup", 'L', 120, false,
    false, -1);

  /**
   * Get the {@link Flag} object for a specified numeric flag value.
   * 
   * @param value
   *          The flag value.
   * @return The Flag object.
   * @throws FlagException
   *           If the flag value is not in this scheme.
   */
  Flag getFlag(int value);

  /**
   * Get the {@link Flag} object for a specified numeric flag value.
   * 
   * @param value
   *          The flag value.
   * @return The Flag object.
   * @throws FlagException
   *           If the flag value is not in this scheme.
   */
  Flag getFlag(char character);

  /**
   * Get the user-assignable {@link Flag}s for this flag scheme.
   * 
   * <p>
   * Flags are listed in ascending significance order, since the most desirable
   * flags (i.e. Good data) are generally those with lowest significance but
   * should should be displayed first.
   * </p>
   * 
   * @return The user-assignable {@link Flag}s.
   * @throws FlagException
   */
  List<Flag> getUserAssignableFlags();

  /**
   * Determine whether the specified {@link Flag} is a <i>Good</i> flag.
   * 
   * <p>
   * A {@link #NO_QC_FLAG} is assumed to be <i>Good</i>.
   * </p>
   * 
   * @param flag
   *          The {@link Flag} to test.
   * @param includeAssumedGood
   *          Specify whether the <i>Assumed Good</i> flag should be treated as
   *          a positive result.
   * @return {@code true} if the {@link Flag} is a <i>Good</i> flag;
   *         {@code false} otherwise.
   */
  default boolean isGood(Flag flag, boolean includeAssumedGood) {

    if (flag.equals(NO_QC_FLAG)) {
      return true; // No QC is assumed to be Good.
    } else if (includeAssumedGood) {
      return flag.equals(getGoodFlag()) || flag.equals(getAssumedGoodFlag());
    } else {
      return flag.equals(getGoodFlag());
    }
  }

  /**
   * Get the instrument basis for this scheme.
   * 
   * @return
   */
  int getBasis();

  /**
   * Get the name of this Flag Scheme.
   * 
   * @return The scheme name.
   */
  String getName();

  /**
   * Get the <i>Good</i> {@link Flag} for this flag scheme.
   * 
   * @return The <i>Good</i> {@link Flag}.
   */
  Flag getGoodFlag();

  /**
   * Get the <i>Assumed Good</i> {@link Flag} for this flag scheme.
   * 
   * @return The <i>Assumed Good</i> {@link Flag}.
   */
  Flag getAssumedGoodFlag();

  /**
   * Get the <i>Bad</i> {@link Flag} for this flag scheme.
   * 
   * @return
   */
  Flag getBadFlag();

  /**
   * Determine whether the specified {@link Flag} is a <i>Bad</i< flag.
   * 
   * @param flag
   *          The {@link Flag} to test.
   * @return {@code true} if the {@link Flag} is a <i>Bad</i> flag;
   *         {@code false} otherwise.
   */
  default boolean isBad(Flag flag) {
    return flag.equals(getBadFlag());
  }

  /**
   * Get the {@link Flag}s that should be shown as highlights on QC plots.
   * 
   * @return The highlight {@link Flag}s.
   */
  default List<Flag> getPlotHighlightFlags() {
    return getUserAssignableFlags();
  }

  /**
   * Determine whether a {@link Collection} of {@link Flag}s contains a
   * {@link Flag} with greater significance than the specified reference
   * {@link Flag}.
   * 
   * @param flags
   *          The {@link Flag}s to check.
   * @param referenceFlag
   *          The reference {@link Flag}.
   * @return {@code true} if the Collection contains at least one {@link Flag}
   *         with greater significance than the reference {@link Flag};
   *         {@code false} otherwise.
   */
  static boolean containsMoreSignificantFlag(Collection<Flag> flags,
    Flag referenceFlag) {
    return flags.stream()
      .filter(f -> f.getSignificance() > referenceFlag.getSignificance())
      .findAny().isPresent();
  }
}
