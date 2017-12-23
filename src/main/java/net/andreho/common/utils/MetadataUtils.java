package net.andreho.common.utils;

import net.andreho.common.anno.Metadata;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

/**
 * <br/>Created by a.hofmann on 22.12.2017 at 23:58.
 */
public class MetadataUtils {
  private static final ClassValue<Descriptor> METADATA_CACHE = new ClassValue<>() {
    @Override
    protected Descriptor computeValue(final Class<?> type) {
      return computeDescriptor((Class<? extends Annotation>) type);
    }
  };

  /**
   * @param annotationType
   * @return
   */
  public static Optional<Descriptor> ofAnnotation(Class<? extends Annotation> annotationType) {
    if (!annotationType.isAnnotation()) {
      return Optional.empty();
    }
    return Optional.of(ofAnnotationType(annotationType));
  }

  static Descriptor ofAnnotationType(Class<? extends Annotation> annotationType) {
    return METADATA_CACHE.get(annotationType);
  }

  static Descriptor computeDescriptor(final Class<? extends Annotation> type) {
    final Deque<Annotation> transitiveAnnotations = collectMetaAnnotations(type);
    final Map<Class<? extends Annotation>, Descriptor> descriptorMap =
      new IdentityHashMap<>(transitiveAnnotations.size() - 1);
    final Map<Class<? extends Annotation>, Annotation> annotationMap =
      new IdentityHashMap<>(transitiveAnnotations.size() - 1);

    Annotation currentAnnotation;
    while ((currentAnnotation = transitiveAnnotations.pop()) != null) {
      final Class<? extends Annotation> annotationType = currentAnnotation.annotationType();
      if (annotationType == type) {
        break;
      }
      Descriptor descriptor = ofAnnotationType(annotationType);
      descriptorMap.putIfAbsent(annotationType, descriptor);
      annotationMap.putIfAbsent(annotationType, currentAnnotation);
    }

    return new Descriptor(type, computeProperties(type, descriptorMap), descriptorMap, annotationMap);
  }

  static Deque<Annotation> collectMetaAnnotations(final Class<? extends Annotation> annotationType) {
    return collectMetaAnnotations(annotationType,
                                  new ArrayDeque<>(),
                                  Collections.newSetFromMap(new IdentityHashMap<>())
    );
  }

  static Deque<Annotation> collectMetaAnnotations(final Class<? extends Annotation> annotationType,
                                                  final Deque<Annotation> classes,
                                                  final Set<Class<? extends Annotation>> visited) {
    if (visited.add(annotationType)) {

      for (Annotation metaAnnotation : annotationType.getDeclaredAnnotations()) {
        final Class<? extends Annotation> metaAnnotationType = metaAnnotation.annotationType();
        if (metaAnnotationType.isAnnotationPresent(Metadata.class)) {
          classes.push(metaAnnotation);
          collectMetaAnnotations(metaAnnotationType, classes, visited);
        }
      }
    }
    return classes;
  }

  private static Map<String, Property> computeProperties(final Class<? extends Annotation> annotationType,
                                                         final Map<Class<? extends Annotation>, Descriptor> visible) {
    final Map<String, Property> properties = new LinkedHashMap<>();
    try {
      for (Method method : annotationType.getDeclaredMethods()) {
        final String methodName = method.getName();
        if (method.getParameterCount() != 0 ||
            //           "equals".equals(method.getName()) ||
            "hashCode".equals(methodName) ||
            "toString".equals(methodName) ||
            "annotationType".equals(methodName)) {
          continue;
        }

        Property reference = null;
        if (method.isAnnotationPresent(Metadata.Property.class)) {
          reference = resolveReference(annotationType, visible, method);
        }

        Property property =
          new Property(annotationType,
                       methodName,
                       method.getDefaultValue(),
                       reference,
                       MethodHandles.lookup().unreflect(method));

        properties.put(methodName, property);
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return properties;
  }

  private static Property resolveReference(final Class<? extends Annotation> annotationType,
                                           final Map<Class<? extends Annotation>, Descriptor> visible,
                                           final Method method) {
    final Metadata.Property referencedProperty = method.getAnnotation(Metadata.Property.class);
    final Class<? extends Annotation> referencedOwner = referencedProperty.owner();
    final String referencedName = referencedProperty.name();

    Descriptor descriptor = visible.get(referencedOwner);
    if (descriptor == null) {
      throw new IllegalStateException(
        "Reference to the meta-property '" + referencedOwner.getName() + "#" + referencedName + "' " +
        " is invalid, because the annotation '" + annotationType.getName()+"' wasn't (transitively) annotated with: "
        + referencedOwner.getName());
    }
    Property resolvedReference = descriptor.getProperties().get(referencedName);
    if(resolvedReference == null) {
      throw new IllegalStateException(
        "Reference to the meta-property '" + referencedOwner.getName() + "#" + referencedName + "' " +
        " cant't be resolved. Please correct the reference defined in: "+annotationType.getName());
    }
    return resolvedReference;
  }

  protected static class Descriptor {

    private final Class<? extends Annotation> annotationType;
    private final Map<String, Property> properties;
    private final Map<String, Property> references;
    private final Map<Class<? extends Annotation>, Descriptor> metaDescriptors;
    private final Map<Class<? extends Annotation>, Annotation> metaAnnotations;

    public Descriptor(final Class<? extends Annotation> annotationType,
                      final Map<String, Property> properties,
                      final Map<Class<? extends Annotation>, Descriptor> metaDescriptors,
                      final Map<Class<? extends Annotation>, Annotation> metaAnnotations) {
      this.annotationType = annotationType;
      this.metaDescriptors = metaDescriptors.isEmpty() ? emptyMap() : unmodifiableMap(metaDescriptors);
      this.metaAnnotations = metaAnnotations.isEmpty() ? emptyMap() : unmodifiableMap(metaAnnotations);
      this.properties = properties.isEmpty() ? emptyMap() : unmodifiableMap(properties);

      final Map<String, Property> references = new HashMap<>();
      for (Map.Entry<String, Property> entry : properties.entrySet()) {
        Property property = entry.getValue();
        if (property.isReference()) {
          Optional<Property> referenced = property.getReferenced();
          if(!referenced.isPresent()) {
            throw new IllegalStateException("Something went wrong.");
          }
          final Property referencedProperty = referenced.get();
          references.putIfAbsent(referencedProperty.getName(), property);
        }
      }
      this.references = references.isEmpty() ? emptyMap() : unmodifiableMap(references);
    }

    public Class<? extends Annotation> getAnnotationType() {
      return annotationType;
    }

    public Map<String, Property> getProperties() {
      return properties;
    }

    public Map<String, Property> getReferences() {
      return references;
    }

    public Map<Class<? extends Annotation>, Descriptor> getMetaDescriptors() {
      return metaDescriptors;
    }

    public Map<Class<? extends Annotation>, Annotation> getMetaAnnotations() {
      return metaAnnotations;
    }

    /**
     * @param owner
     * @param name
     * @return
     */
    public Optional<Property> property(final Class<? extends Annotation> owner,
                                       final String name) {
      Descriptor descriptor = metaDescriptors.get(owner);
      if (descriptor != null) {
        return descriptor.property(name);
      }
      return Optional.empty();
    }

    /**
     * @param name
     * @return
     */
    public Optional<Property> property(final String name) {
      return Optional.ofNullable(properties.get(name));
    }

    /**
     * @param owner
     * @param name
     * @return
     */
    public Optional<Object> fetchProperty(final Class<? extends Annotation> owner,
                                          final String name) {
      if (Objects.equals(getAnnotationType(), owner)) {
        Optional<Property> property = property(name);
        if (property.isPresent()) {
          return Optional.ofNullable(property.get().getDefaultValue());
        }
      } else {
        final Annotation annotation = getMetaAnnotations().get(owner);
        if (annotation != null) {
          return property(owner, name).map((Property prop) ->
                                             Optional.ofNullable(prop.readFrom(annotation))
          );
        }
      }
      return Optional.empty();
    }
  }

  protected static class Property {

    private final Class<? extends Annotation> owner;
    private final String name;
    private final Object defaultValue;
    private final MethodHandle handle;
    private final Property reference;

    public Property(
      final Class<? extends Annotation> owner,
      final String name,
      final Object defaultValue,
      final Property reference,
      final MethodHandle handle) {
      this.owner = owner;
      this.name = name;
      this.defaultValue = defaultValue;
      this.reference = reference;
      this.handle = handle;
    }

    protected Method getReferencedMethod() {
      try {
        return owner.getMethod(getName());
      } catch (NoSuchMethodException e) {
        throw new IllegalStateException(e);
      }
    }

    public Class<? extends Annotation> getOwner() {
      return owner;
    }

    public String getName() {
      return name;
    }

    public Class<?> getType() {
      return handle.type().returnType();
    }

    public boolean hasDefaultValue() {
      return defaultValue != null;
    }

    public Object getDefaultValue() {
      return defaultValue;
    }

    public boolean isReference() {
      return reference != null;
    }

    public Optional<Property> getReferenced() {
      return Optional.ofNullable(reference);
    }

    public boolean isCompatibleWith(Annotation annotation) {
      return owner.equals(annotation.annotationType());
    }

    public Object readFrom(Annotation annotation) {
      try {
        return handle.invoke(annotation);
      } catch (Throwable throwable) {
        throw new IllegalStateException(throwable);
      }
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      final Property property = (Property) o;
      return Objects.equals(owner, property.owner) &&
             Objects.equals(name, property.name);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(owner) * 31 +
             Objects.hashCode(name);
    }
  }
}
