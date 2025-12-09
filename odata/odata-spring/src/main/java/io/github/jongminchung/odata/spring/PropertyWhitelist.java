package io.github.jongminchung.odata.spring;

/**
 * Optional contract to validate allowed properties for a domain type.
 * v1 default is allow-all. Implementations can restrict fields.
 */
public interface PropertyWhitelist {
    default boolean isAllowed(Class<?> domainType, String property) { return true; }
}
