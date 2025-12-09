package io.github.jongminchung.odata.spring;

import io.github.jongminchung.odata.query.OrderBy;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;

import java.util.Collections;
import java.util.Set;

public final class StringToOrderByConverter implements GenericConverter {
    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(String.class, OrderBy.class));
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) return null;
        if (!(source instanceof String)) return null;
        return OrderBy.parse((String) source);
    }
}
