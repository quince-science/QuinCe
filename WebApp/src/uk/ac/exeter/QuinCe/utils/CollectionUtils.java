package uk.ac.exeter.QuinCe.utils;

import java.util.Map;
import java.util.stream.Collectors;

public class CollectionUtils {

  /**
   * Remove {@link Double#NaN} values from a {@link Map} whose values are of
   * type {@link Double}. The type of the keys does not matter.
   *
   * <p>
   * The method returns a copy of the supplied {@link Map}; the original is
   * unchanged.
   * </p>
   *
   * @param <T>
   * @param map
   *          The {@link Map} to be edited.
   * @return A copy of the {@link Map} with any {@link Double#NaN} values
   *         removed.
   */
  @SuppressWarnings("unchecked")
  public static <T> Map<T, Double> removeNans(Map<?, Double> map) {
    return map.entrySet().stream().filter(e -> !e.getValue().isNaN())
      .collect(Collectors.toMap(e -> (T) e.getKey(), e -> e.getValue()));
  }

}
