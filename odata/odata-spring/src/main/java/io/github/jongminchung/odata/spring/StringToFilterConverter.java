package io.github.jongminchung.odata.spring;

import io.github.jongminchung.odata.query.Filter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;

import java.util.Collections;
import java.util.Set;

/**
 * GenericConverter that parses a query-string into Filter<T>.
 */
public final class StringToFilterConverter implements GenericConverter {

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(String.class, Filter.class));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) return null;
        if (!(source instanceof String)) return null;
        String s = (String) source;
        return Filter.parse(s); // returns Filter<?> or null
    }
}
