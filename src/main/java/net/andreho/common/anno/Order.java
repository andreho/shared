package net.andreho.common.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <br/>Created by a.hofmann on 23.03.2017 at 15:29.
 */
@Metadata
@Target({ElementType.PACKAGE,
         ElementType.TYPE,
         ElementType.FIELD,
         ElementType.METHOD,
         ElementType.CONSTRUCTOR,
         ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Order {
   /**
    * The order index of the marked element. A <b>smaller value</b> leads to greater ordering priority.
    * @return the order index
    * @implSpec default value is the lowest possible value,
    * so any other value would lead to greater priority
    */
   int value() default Integer.MAX_VALUE;
}
