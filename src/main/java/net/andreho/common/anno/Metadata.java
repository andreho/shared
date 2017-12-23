package net.andreho.common.anno;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to create composable meta-annotations using the composition of
 * available annotations on top of a custom one.
 * You may create your own annotations that suit one or more use-cases.
 * <p>
 * <br/>Created by a.hofmann on 29.11.2016 at 06:56.
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Metadata {

  /**
   * This annotation allows to mark properties of annotations that represents a property of a meta annotation.
   * So you can define own annotation-properties, that would override the configured value of the referenced property. <br/>
   * <b>Attention:</b> the matching is made against the property-name and also against property-type
   * <br/>Created by a.hofmann on 29.11.2016 at 06:56.
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Property {
    /**
     * Defines the defining meta-annotation of the referenced property
     * @return the owner meta-annotation of the referenced property
     * @implNote the referenced annotation must be marked with the {@link Metadata} -annotation
     * @see Metadata
     * @see Property
     */
    Class<? extends Annotation> owner();

    /**
     * Defines the name of the referenced property,
     * that is defined by the referenced {@link #owner() owner} meta-annotation
     * @return the name of the property
     * @see Property
     */
    String name() default "";
  }
}
