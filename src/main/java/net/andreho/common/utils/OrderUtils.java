package net.andreho.common.utils;

import net.andreho.common.anno.Order;

import java.util.Comparator;
import java.util.Objects;

/**
 * <br/>Created by a.hofmann on 23.03.2017 at 15:34.
 */
public class OrderUtils
  implements Comparator<Object> {

  private static final Class<Order> ORDER_ANNOTATION_CLASS = Order.class;
  static volatile OrderUtils COMPARATOR = new OrderUtils();

  /**
   * @return the defined comparator instance
   */
  public static Comparator<Object> comparator() {
    return COMPARATOR;
  }

  /**
   * Overrides the default behavior of this utility class
   *
   * @param instance is the new instance to use as comparator
   */
  public static void overrideDefaults(OrderUtils instance) {
    COMPARATOR = Objects.requireNonNull(instance, "Given instance can't be null.");
  }

  /**
   * Compares two given objects using chosen {@link #comparator()}
   *
   * @param a instance to compare
   * @param b instance to compare
   * @return an integer as defined by {@link Comparator#compare(Object, Object)}
   */
  public static int order(Object a,
                          Object b) {
    return COMPARATOR.compare(a, b);
  }

  private static boolean isOrderAnnotationPresent(final Object a) {
    return a.getClass().isAnnotationPresent(ORDER_ANNOTATION_CLASS);
  }

  @Override
  public int compare(final Object a,
                     final Object b) {

    if (isOrderAnnotationPresent(a) || isOrderAnnotationPresent(b)) {
      final Order ao = a.getClass().getAnnotation(ORDER_ANNOTATION_CLASS);
      final Order bo = b.getClass().getAnnotation(ORDER_ANNOTATION_CLASS);

      if (ao != null && bo != null) {
        return Integer.compare(ao.value(), bo.value());
      } else if (ao != null) {
        return -1;
      } else if (bo != null) {
        return 1;
      }
    }

    return fallback(a, b);
  }

  protected int fallback(final Object a,
                         final Object b) {
    return 0;
  }
}
