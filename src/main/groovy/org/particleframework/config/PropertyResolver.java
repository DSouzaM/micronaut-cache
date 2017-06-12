package org.particleframework.config;

import java.util.Optional;

/**
 * A property resolver is capable of resolving properties from an underlying property source
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public interface PropertyResolver {

    /**
     * Resolve the given property for the given name
     *
     * @param name The name
     * @param requiredType The required type
     * @param <T> The concrete type
     * @return An optional containing the property value if it exists
     */
    <T> Optional<T> getProperty(String name, Class<T> requiredType);

    /**
     * Resolve the given property for the given name
     *
     * @param name The name
     * @param requiredType The required type
     * @param defaultValue The default value
     * @param <T> The concrete type
     * @return An optional containing the property value if it exists
     */
    default <T> T getProperty(String name, Class<T> requiredType, T defaultValue) {
        return getProperty(name, requiredType).orElse(defaultValue);
    }

    /**
     * Resolve the given property for the given name
     *
     * @param name The name of the property
     * @param requiredType The required type
     * @param <T> The concrete type
     * @return The value of the property
     */
    default <T> T getRequiredProperty(String name, Class<T> requiredType) throws PropertyNotFoundException {
        return getProperty(name, requiredType).orElseThrow(() ->
            new PropertyNotFoundException(name, requiredType)
        );
    }
}
