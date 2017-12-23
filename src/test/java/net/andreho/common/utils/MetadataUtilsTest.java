package net.andreho.common.utils;

import net.andreho.common.anno.Metadata;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <br/>Created by a.hofmann on 23.12.2017 at 02:04.
 */
class MetadataUtilsTest {

  @Test
  void ofAnnotation() {
    MetadataUtils.Descriptor descriptor =
      MetadataUtils.ofAnnotation(NormalAnnotation.class).orElse(null);
  }

  @Metadata
  @Retention(RetentionPolicy.RUNTIME)
  @interface FirstMetaAnnotation {
    String name() default "";
  }

  @Metadata
  @Retention(RetentionPolicy.RUNTIME)
  @interface SecondMetaAnnotation {
    int index() default 0;
  }

  @Metadata
  @SecondMetaAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  @interface ThirdMetaAnnotation {
    ElementType type() default ElementType.PACKAGE;
  }

  @Metadata
  @FirstMetaAnnotation
  @ThirdMetaAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  @interface FourthMetaAnnotation {
    boolean check() default false;
  }

  @Metadata
  @FourthMetaAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  @interface FifthMetaAnnotation {
    int[] args() default {};
  }

  @FifthMetaAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  @interface NormalAnnotation {
    @Metadata.Property(owner = FirstMetaAnnotation.class)
    String name();
  }
}