package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class RunTypePeriods extends ArrayList<RunTypePeriod> {

  boolean finished = false;

  public RunTypePeriods() {
    super();
  }

  public void add(String runType, LocalDateTime time) throws DataSetException {

    if (finished) {
      throw new DataSetException("RunTypePeriods is finished");
    }

    if (size() == 0) {
      add(new RunTypePeriod(runType, time));
    } else {
      if (time.isBefore(getLastTime()) || time.equals(getLastTime())) {
        throw new DataSetException(
          "Added time must be after last period end time");
      }

      RunTypePeriod currentPeriod = get(size() - 1);
      if (!currentPeriod.getRunType().equals(runType)) {
        add(new RunTypePeriod(runType, time));
      } else {
        currentPeriod.setEnd(time);
      }
    }
  }

  /**
   * Signal that the last run time has been registered
   */
  public void finish() {
    if (size() > 0) {
      get(size() - 1).setEnd(LocalDateTime.MAX);
    }

    finished = true;
  }

  public boolean contains(LocalDateTime time) {
    boolean result = false;

    for (RunTypePeriod period : this) {
      if (period.encompasses(time)) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * Get the Run Type name that is current at the specified time.
   *
   * <p>
   * If there is no active Run Type at the specified time and {@code fallback}
   * is {@code true}, the last Run Type before the time will be returned.
   * Otherwise the method will return {@code null}.
   *
   * @param time
   *          The time.
   * @param fallback
   *          Whether the method should fall back to using the previous Run Type
   *          if there is no concurrent Run Type.
   * @return The found Run Type name.
   */
  public String getRunType(LocalDateTime time, boolean fallback) {

    String result = null;

    Optional<RunTypePeriod> period = stream().filter(p -> p.encompasses(time))
      .findAny();

    if (!period.isEmpty()) {
      result = period.get().getRunType();
    } else {
      if (fallback) {
        List<RunTypePeriod> priorPeriods = stream()
          .filter(p -> p.getEnd().isBefore(time)).toList();
        if (priorPeriods.size() > 0) {
          result = priorPeriods.get(priorPeriods.size() - 1).getRunType();
        }
      }
    }

    return result;
  }

  /**
   * Get the unique set of run type names from all periods. The order is not
   * guaranteed.
   *
   * @return The run type names.
   */
  public Set<String> getRunTypeNames() {
    return stream().map(p -> p.getRunType()).distinct()
      .collect(Collectors.toSet());
  }

  private LocalDateTime getLastTime() {
    LocalDateTime result = null;

    if (size() > 0) {
      result = get(size() - 1).getEnd();
    }

    return result;
  }
}
