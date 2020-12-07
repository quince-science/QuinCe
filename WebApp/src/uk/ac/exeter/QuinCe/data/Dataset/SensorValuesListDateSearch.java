package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.HashMap;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * A stateful date-based search for a {@link SearchableSensorValuesList}.
 *
 * @author Steve Jones
 *
 */
@Deprecated
class SensorValuesListDateSearch {

  /**
   * The list being searched.
   */
  private final SearchableSensorValuesList list;

  /**
   * The current search position.
   */
  private int currentIndex = -1;

  /**
   * Indicates whether we want to match only GOOD flags, or also include
   * QUESTIONABLE or BAD flags.
   */
  private final boolean goodFlagsOnly;

  /**
   * Store of the last position that different flag types were found
   */
  private HashMap<Flag, Integer> lastFlagValues;

  /**
   * Set up the search. See {@link #search(LocalDateTime)} for a detailed
   * description of how the flags work.
   *
   * @param list
   *          The list to be searched.
   * @param preferredFlags
   *          The QC flags we would prefer to find.
   * @param forbiddenFlags
   *          The QC flags we must not find.
   * @param preferredFlagsOnly
   *          Indicates whether only preferred flags can be matched.
   * @throws MissingParamException
   */
  protected SensorValuesListDateSearch(SearchableSensorValuesList list,
    boolean goodFlagsOnly) throws MissingParamException {

    MissingParam.checkMissing(list, "list", false);

    this.list = list;
    this.goodFlagsOnly = goodFlagsOnly;

    lastFlagValues = new HashMap<Flag, Integer>();
    lastFlagValues.put(Flag.GOOD, -1);
    lastFlagValues.put(Flag.QUESTIONABLE, -1);
    lastFlagValues.put(Flag.BAD, -1);

    // TODO Add a time limit option to find values within a specified time
    // period of the target time
  }

  /**
   * Perform an incremental search for the specified time from the current
   * search state. Returns the latest SensorValue that is before or equal to the
   * specified time.
   *
   * <p>
   * If {@link #preferredFlags} is populated, the search will initially only
   * match records with a user QC flag that is in that list. If no such records
   * are found the behaviour depends on {@link #preferredFlagsOnly}:
   * </p>
   *
   * <ul>
   * <li>If {@link #preferredFlagsOnly} is {@code true}, the method returns
   * {@code null}.</li>
   * <li>If {@link #preferredFlagsOnly} is {@code false}, the method will return
   * any record whose user QC flag is not in {@link #forbiddenFlags}. If all
   * records have a forbidden flag, {@code null} is returned.</li>
   * </ul>
   *
   * <p>
   * <b>Note:</b> If the same flag is present in both {@link #preferredFlags}
   * and {@link #forbiddenFlags}, the behaviour of this method is undefined.
   * </p>
   *
   * @param time
   *          The time to search for
   * @return The closest value to the search time that matches the flag
   *         requirements
   * @throws CloneNotSupportedException
   */
  protected SensorValue search(LocalDateTime time)
    throws CloneNotSupportedException {

    SensorValue result = null;

    // Loop through the values until either the next entry is after the search
    // time, or we get to the end of the list
    while (currentIndex < list.size() - 1
      && !list.get(currentIndex + 1).getTime().isAfter(time)) {

      currentIndex++;

      Flag qcFlag = getQCFlag(list.get(currentIndex));

      switch (qcFlag.getFlagValue()) {
      case Flag.VALUE_GOOD:
      case Flag.VALUE_ASSUMED_GOOD: {
        lastFlagValues.put(Flag.GOOD, currentIndex);
        break;
      }
      case Flag.VALUE_QUESTIONABLE:
      case Flag.VALUE_BAD: {
        lastFlagValues.put(qcFlag, currentIndex);
        break;
      }
      default: {
        // We ignore all other flags
      }
      }
    }

    // Now we've either hit the requested time, or the next record is after the
    // requested time, or we fell off the list.

    // If we hit the exact time, and it's a FLUSHING record, then we signal this
    // by returning a special NO_VALUE SensorValue.
    if (list.get(currentIndex).getTime().equals(time)
      && list.get(currentIndex).getUserQCFlag().equals(Flag.FLUSHING)) {

      result = SensorValue.getNoValueSensorValue(list.get(currentIndex));
    } else {

      // Get the index of the value we want to return
      int returnIndex;

      if (goodFlagsOnly) {
        returnIndex = lastFlagValues.get(Flag.GOOD);
      } else {
        returnIndex = getLastFlagValue();
      }

      if (returnIndex > -1) {
        result = list.get(returnIndex);
      }
    }

    return result;
  }

  /**
   * Get the latest value that we've found with a GOOD, QUESTIONABLE or BAD
   * flag.
   *
   * <p>
   * We always return the last GOOD flag if one has been found. If not, we
   * return the found QUESTIONABLE flag, and finally fall back to BAD.
   * </p>
   *
   * @return
   */
  private int getLastFlagValue() {
    int result = lastFlagValues.get(Flag.GOOD);

    // TODO For now we return the lasted value found regardless of flag
    if (lastFlagValues.get(Flag.QUESTIONABLE) > result) {
      result = lastFlagValues.get(Flag.QUESTIONABLE);
    }

    if (lastFlagValues.get(Flag.BAD) > result) {
      result = lastFlagValues.get(Flag.BAD);
    }

    /*
     * TODO This is the algorithm we'll use for full interpolation.
     *
     *
     * if (result == -1) { result = lastFlagValues.get(Flag.QUESTIONABLE); }
     *
     * if (result == -1) { result = lastFlagValues.get(Flag.BAD); }
     */
    return result;
  }

  protected int nextIndex() {
    return currentIndex + 1;
  }

  private Flag getQCFlag(SensorValue value) {
    return value.getUserQCFlag().equals(Flag.NEEDED) ? value.getAutoQcFlag()
      : value.getUserQCFlag();
  }

  /**
   * Look ahead in the search list from the current position for the next valid
   * value. The search state is not changed.
   *
   * <p>
   * We find the first available GOOD value, or if there isn't one (and
   * {@code goodFlagsOnly == false}) we look for the first QUESTIONABLE value,
   * then finally the first BAD value.
   * </p>
   *
   * @return The found value.
   */
  protected SensorValue findNextValue() {

    int goodPos = -1;
    int questionablePos = -1;
    int badPos = -1;

    int nextSearchPos = currentIndex + 1;
    searchLoop: while (nextSearchPos < list.size()) {
      Flag qcFlag = getQCFlag(list.get(nextSearchPos));

      switch (qcFlag.getFlagValue()) {
      case Flag.VALUE_GOOD:
      case Flag.VALUE_ASSUMED_GOOD: {
        goodPos = nextSearchPos;

        // We found a GOOD value, so we can stop searching immediately.
        break searchLoop;
      }
      case Flag.VALUE_QUESTIONABLE: {
        questionablePos = nextSearchPos;
        break;
      }
      case Flag.VALUE_BAD: {
        badPos = nextSearchPos;
        break;
      }
      default: {
        // We ignore all other flags
      }
      }

      nextSearchPos++;
    }

    SensorValue result = null;

    if (goodPos > -1) {
      result = list.get(goodPos);
    } else if (!goodFlagsOnly) {
      if (questionablePos > -1) {
        result = list.get(questionablePos);
      } else if (badPos > -1) {
        result = list.get(badPos);
      }
    }

    return result;
  }
}
