package org.particleframework.application.env;

import org.particleframework.config.MapPropertyResolver;
import org.particleframework.config.PropertyResolver;
import org.particleframework.core.convert.ConversionService;
import org.particleframework.core.convert.DefaultConversionService;
import org.particleframework.core.convert.TypeConverter;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * <p>The default implementation of the {@link Environment} interface. Configures a named environment.</p>
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class DefaultEnvironment implements Environment {
    private final String name;
    private final ConversionService conversionService;
    // properties are stored in an array of maps organized by character in the alphabet
    // this allows optimization of searches by prefix
    private final Map<String,Object>[] catalog = new Map[57];
    private final Collection<PropertySource> propertySources = new ConcurrentLinkedQueue<>();

    public DefaultEnvironment(String name) {
        this(name, new DefaultConversionService());
    }

    public DefaultEnvironment(String name, ConversionService conversionService) {
        this.name = name;
        this.conversionService = conversionService;
        addPropertySource(new MapPropertySource(System.getProperties()));
        addPropertySource(new MapPropertySource(System.getenv()) {
            @Override
            public boolean hasUpperCaseKeys() {
                return true;
            }
        });

        ServiceLoader<PropertySourceLoader> propertySources = ServiceLoader.load(PropertySourceLoader.class);
        for (PropertySourceLoader loader : propertySources) {
            addPropertySource(loader.load(this));
        }
    }

    @Override
    public Environment addPropertySource(PropertySource propertySource) {
        propertySources.add(propertySource);
        return this;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public <T> Optional<T> getProperty(String name, Class<T> requiredType, Map<String, Class> typeArguments) {
        if(name.length() == 0) {
            return Optional.empty();
        }
        else {
            Map<String,Object> entries = resolveEntriesForKey(name, false);
            if(entries != null) {
                Object value = entries.get(name);
                if(value != null) {
                    return conversionService.convert(value, requiredType, typeArguments);
                }
                else if(Map.class.isAssignableFrom(requiredType)) {
                    Map<String, Object> subMap = resolveSubMap(name, entries);
                    return Optional.of((T) subMap);
                }
                else if(PropertyResolver.class.isAssignableFrom(requiredType)) {
                    Map<String, Object> subMap = resolveSubMap(name, entries);
                    return Optional.of((T) new MapPropertyResolver(subMap, conversionService));
                }
            }
        }
        return Optional.empty();
    }

    protected Map<String, Object> resolveSubMap(String name, Map<String, Object> entries) {
        // special handling for maps for resolving sub keys
        String prefix = name + '.';
        return entries.entrySet().stream()
                .filter(map -> map.getKey().startsWith(prefix))
                .collect(Collectors.toMap(entry -> entry.getKey().substring(prefix.length()), Map.Entry::getValue));
    }

    @Override
    public Environment start() {
        for (PropertySource propertySource : propertySources) {
            processPropertySource(propertySource, propertySource.hasUpperCaseKeys());
        }
        return this;
    }

    protected void processPropertySource(PropertySource properties, boolean upperCaseUnderscoreSeperated) {
        synchronized (catalog) {
            for (String property : properties) {
                Object value = properties.get(property);
                if(upperCaseUnderscoreSeperated) {
                    property = property.toLowerCase(Locale.ENGLISH).replace('_', '.');
                }

                Map entries = resolveEntriesForKey(property, true);
                if(entries != null) {
                    entries.put(property, value);
                }
            }
        }
    }

    protected Map<String,Object> resolveEntriesForKey(String name, boolean allowCreate) {
        Map<String,Object> entries = null;
        if(name.length() == 0) {
            return null;
        }
        char firstChar = name.charAt(0);
        if(Character.isLetter(firstChar)) {
            int index = ((int)firstChar) - 65;
            if(index < catalog.length && index > 0) {
                entries = catalog[index];
                if(allowCreate && entries == null) {
                    entries = new LinkedHashMap<>(5);
                    catalog[index] = entries;
                }
            }
        }
        return entries;
    }

    @Override
    public Environment stop() {
        return this;
    }

    @Override
    public <T> Optional<T> convert(Object object, Class<T> targetType, Map<String, Class> typeArguments) {
        return conversionService.convert(object, targetType, typeArguments);
    }

    @Override
    public <S, T> Environment addConverter(Class<S> sourceType, Class<T> targetType, TypeConverter<S, T> typeConverter) {
        conversionService.addConverter(sourceType, targetType, typeConverter);
        return this;
    }
}
