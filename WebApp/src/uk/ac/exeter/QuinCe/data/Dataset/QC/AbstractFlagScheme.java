package uk.ac.exeter.QuinCe.data.Dataset.QC;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

/**
 * Base implementation of the {@link FlagScheme} interface.
 *
 *
 */
public abstract class AbstractFlagScheme implements FlagScheme {

  /**
   * Lookup table of {@link Flag}s indexed by their numeric value.
   */
  private HashMap<Integer, Flag> flagsByValue = new HashMap<Integer, Flag>();

  /**
   * Lookup table of {@link Flag}s indexed by their character value.
   */
  private HashMap<Character, Flag> flagsByChar = new HashMap<Character, Flag>();

  /**
   * The {@link Flag}s in significance order (most significant first);
   */
  private TreeSet<Flag> orderedFlags = new TreeSet<Flag>();

  /**
   * The <i>Good</i> {@link Flag}.
   *
   * @see #registerGoodFlag(Flag)
   */
  private Flag goodFlag = null;

  /**
   * The <i>Assumed Good</i> {@link Flag}.
   *
   * @see #registerGoodFlag(Flag)
   */
  private Flag assumedGoodFlag = null;

  /**
   * Base constructor.
   *
   * @throws FlagException
   *           If the default {@link Flag}s cannot be created.
   */
  protected AbstractFlagScheme() {
    registerFlag(NO_QC_FLAG);
    registerFlag(NEEDED_FLAG);
    registerFlag(LOOKUP_FLAG);
    registerFlag(FLUSHING_FLAG);
  }

  /**
   * Register a {@link Flag} with this scheme.
   *
   * @param flag
   *          The Flag.
   * @throws FlagException
   *           If conflicting Flag has already been registered.
   */
  protected void registerFlag(Flag flag) {
    if (flagsByValue.containsKey(flag.getValue())) {
      throw new FlagException(
        "Flag with value " + flag.getValue() + " already registered");
    }

    if (flagsByChar.containsKey(flag.getCharacter())) {
      throw new FlagException(
        "Flag with character " + flag.getCharacter() + " already registered");
    }

    flagsByValue.put(flag.getValue(), flag);
    flagsByChar.put(flag.getCharacter(), flag);
    orderedFlags.add(flag);
  }

  /**
   * Register the <i>Good</i> flag for this scheme.
   *
   * <p>
   * The corresponding <i>Assumed Good</i> flag is automatically created.
   * </p>
   *
   * @param goodFlag
   * @throws FlagException
   */
  protected void registerGoodFlag(Flag flag) {

    if (null != goodFlag) {
      throw new FlagException("Good Flag already registered");
    }

    goodFlag = flag;
    registerFlag(flag);

    assumedGoodFlag = new Flag(flag.getValue() * -1,
      "Assumed " + flag.getName(), Character.toLowerCase(flag.getCharacter()),
      flag.getSignificance(), false, false, goodFlag.getExportValue());

    registerFlag(assumedGoodFlag);
  }

  @Override
  public Flag getFlag(int value) {
    Flag result = flagsByValue.get(value);
    if (null == result) {
      throw new FlagException("Unknown flag value " + value);
    }
    return result;
  }

  @Override
  public Flag getFlag(char character) {
    Flag result = flagsByChar.get(character);
    if (null == result) {
      throw new FlagException("Unknown flag character " + character);
    }
    return result;
  }

  @Override
  public List<Flag> getUserAssignableFlags() {
    List<Flag> list = orderedFlags.stream().filter(f -> f.isUserAssignable())
      .toList();
    Collections.reverse(list);
    return list;
  }

  @Override
  public Flag getGoodFlag() {
    return goodFlag;
  }

  @Override
  public Flag getAssumedGoodFlag() {
    return assumedGoodFlag;
  }
}
