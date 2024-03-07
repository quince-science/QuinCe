package uk.ac.exeter.QuinCe.utils;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Miscellaneous utility functions for {@link Collection}s.
 */
public class CollectionUtils {

  /**
   * Count the number of non-{@code null} items in the specified
   * {@code Collection}.
   *
   * @param collection
   *          The collection.
   * @return The number of non-{@code null} items.
   */
  public static long getNonNullCount(Collection<?> collection) {
    return collection.stream().filter(x -> null != x).count();
  }

  /**
   * Get the first non-{@code null} item from the specified {@code Collection},
   * or {@code null} if no such item can be found.
   *
   * @param <T>
   * @param collection
   *          The collection.
   * @return The first non-{@code null} value.
   */
  public static <T> T getFirstNonNull(Collection<T> collection) {
    return collection.stream().filter(x -> null != x).findFirst().orElse(null);
  }

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
