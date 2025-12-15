package io.github.jongminchung.odata.spring;

import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers OData converters to Spring's ConversionService. */
public class ODataWebMvcConfigurer implements WebMvcConfigurer {
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToFilterConverter());
        registry.addConverter(new StringToOrderByConverter());
    }
}
