package uk.ac.exeter.QuinCe.data.Dataset.QC;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Represents a quality control flag placed on a value.
 * 
 * <p>
 * A flag has multiple representations: </p?
 * 
 * <ul>
 * <li>Numeric (used in exported data)</li>
 * <li>Name (Human-readable representation)</li>
 * <li>Character (single-character representation, mostly used internally by
 * QuinCe)</li>
 * </ul>
 * 
 * <p>
 * Each Flag is also assigned a numeric significance, which allows resolution of
 * conflicts when a value has multiple possible flags. For example, if a value
 * may be assigned flags representing Good and Bad, it is likely that the Bad
 * flag would take precedence and thus will have a higher significance value.
 * </p>
 */
public class Flag implements Comparable<Flag> {

  /**
   * The Flag's numeric value.
   */
  private final int value;

  /**
   * The value to use when exporting flag values.
   */
  private final int exportValue;

  /**
   * The human-readable name.
   */
  private final String name;

  /**
   * The character representation.
   */
  private final char character;

  /**
   * The Flag's significance.
   */
  private final int significance;

  /**
   * Indicates whether or not a user can assign this flag to a value.
   */
  private final boolean userAssignable;

  /**
   * Indicates whether or not setting this QC flag must be accompanied by an
   * explanatory comment.
   */
  private final boolean commentRequired;

  /**
   * Base constructor.
   * 
   * @param value
   *          The Flag's numeric value.
   * @param name
   *          The human-readable Flag name.
   * @param character
   *          The Flag's character representation.
   * @param significance
   *          The Flag's significance order.
   * @param userAssignable
   *          Whether or not a user can assign the Flag to a value.
   * @param commentRequired
   *          Whether or not this Flag requires an explanatory comment.
   */
  protected Flag(int value, String name, char character, int significance,
    boolean userAssignable, boolean commentRequired, int exportValue) {
    this.value = value;
    this.name = name;
    this.character = character;
    this.significance = significance;
    this.userAssignable = userAssignable;
    this.commentRequired = commentRequired;
    this.exportValue = exportValue;
  }

  /**
   * Copy constructor.
   * 
   * @source The source Flag object.
   */
  protected Flag(Flag source) {
    this.value = source.value;
    this.name = source.name;
    this.character = source.character;
    this.significance = source.significance;
    this.userAssignable = source.userAssignable;
    this.commentRequired = source.commentRequired;
    this.exportValue = source.exportValue;
  }

  /**
   * Get the Flag`s numeric value.
   * 
   * @return The numeric value.
   */
  public int getValue() {
    return value;
  }

  /**
   * Get the human-readable name for the Flag.
   * 
   * @return The name.
   */
  public String getName() {
    return name;
  }

  /**
   * Get the Flag's character representation.
   * 
   * @return The character representation.
   */
  public char getCharacter() {
    return character;
  }

  /**
   * Get the Flag's significance score.
   * 
   * @return The significance.
   */
  protected int getSignificance() {
    return significance;
  }

  /**
   * Determine whether a user is allowed to assign this Flag to a value.
   * 
   * @return {@code true} if the Flag is user-assignable; {@code false} if it is
   *         not.
   */
  public boolean isUserAssignable() {
    return userAssignable;
  }

  /**
   * Determine whether setting this Flag on a value requires an accompanying
   * explanatory comment.
   * 
   * @return {@code true} if a comment is required; {@code false} if not.
   */
  public boolean isCommentRequired() {
    return commentRequired;
  }

  @Override
  public int compareTo(Flag arg0) {

    // Flags are sorted by descending significance, then by name.
    int result = arg0.significance - this.significance;
    if (result == 0) {
      result = name.compareTo(arg0.name);
    }
    return result;
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
   * Determine whether or not this {@link Flag} has a greater significance than
   * another {@link Flag}.
   * 
   * @param other
   *          The {@link Flag} whose significance is to be compared.
   * @return {@code true} if this {@link Flag} is more significant than the
   *         passed in {@link Flag}; {@code false} if not.
   */
  public boolean moreSignificantThan(Flag other) {
    return significance > other.significance;
  }

  /**
   * Determine whether or not this {@link Flag} has a lesser significance than
   * another {@link Flag}.
   * 
   * @param other
   *          The {@link Flag} whose significance is to be compared.
   * @return {@code true} if this {@link Flag} is less significant than the
   *         passed in {@link Flag}; {@code false} if not.
   */
  public boolean lessSignificantThan(Flag other) {
    return significance < other.significance;
  }

  /**
   * Determine whether or not this {@link Flag} has equal significance to
   * another {@link Flag}.
   * 
   * @param other
   *          The {@link Flag} whose significance is to be compared.
   * @return {@code true} if this {@link Flag} has the same significance than
   *         the passed in {@link Flag}; {@code false} if not.
   */
  public boolean equalSignificance(Flag other) {
    return significance == other.significance;
  }

  public int getExportValue() {
    return exportValue;
  }

  /**
   * Examine a {@link Collection} of {@link Flag}s and return the {@link Flag}
   * with the highest significance score.
   */
  public static Flag mostSignificant(Collection<Flag> flags) {
    return flags.stream().filter(f -> null != f).sorted().findFirst()
      .orElse(null);
  }

  /**
   * Examine a number of {@link Flag}s and return the {@link Flag} with the
   * lowest significance score.
   */
  public static Flag leastSignificant(Flag... flags) {
    return leastSignificant(Arrays.asList(flags));
  }

  /**
   * Examine a {@link Collection} of {@link Flag}s and return the {@link Flag}
   * with the lowest significance score.
   */
  public static Flag leastSignificant(Collection<Flag> flags) {

    Flag result = null;

    List<Flag> outList = flags.size() == 0 ? null
      : flags.stream().filter(f -> null != f).sorted().toList();

    if (null != outList) {
      result = outList.get(outList.size() - 1);
    }

    return result;
  }

  /**
   * Examine a number of {@link Flag}s and return the {@link Flag} with the
   * highest significance score.
   */
  public static Flag mostSignificant(Flag... flags) {
    return mostSignificant(Arrays.asList(flags));
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Flag other = (Flag) obj;
    return value == other.value;
  }
}
