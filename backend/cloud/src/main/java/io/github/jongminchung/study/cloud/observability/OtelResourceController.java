package io.github.jongminchung.study.cloud.observability;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint to verify OpenTelemetry Resource attributes including host.ip.
 * Uses reflection to access OpenTelemetry SDK classes injected by the Java Agent.
 */
@RestController
@RequestMapping("/actuator/otel")
public class OtelResourceController {

    private static final String SHADED_GLOBAL_OTEL_CLASS =
            "io.opentelemetry.javaagent.shaded.io.opentelemetry.api.GlobalOpenTelemetry";
    private static final String GLOBAL_OTEL_CLASS = "io.opentelemetry.api.GlobalOpenTelemetry";

    @GetMapping("/resources")
    public Map<String, Object> getResources() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Access GlobalOpenTelemetry via reflection (injected by Java Agent)
            Class<?> globalOtelClass = loadGlobalOtelClass();
            Object globalOtel = invokeMethod(null, globalOtelClass, "get", new Class<?>[0], new Object[0]);
            Object resolvedOtel = unwrapOpenTelemetry(globalOtel);

            Object resource = resolveResource(resolvedOtel);

            // Get Attributes from Resource
            Object attributes =
                    invokeMethod(resource, resource.getClass(), "getAttributes", new Class<?>[0], new Object[0]);

            // Convert attributes to Map
            Map<String, String> attributeMap = new HashMap<>();
            BiConsumer<Object, Object> consumer = (key, value) -> {
                try {
                    String keyStr = (String) key.getClass().getMethod("getKey").invoke(key);
                    attributeMap.put(keyStr, String.valueOf(value));
                } catch (Exception e) {
                    // Ignore
                }
            };

            invokeMethod(attributes, attributes.getClass(), "forEach", new Class<?>[] {BiConsumer.class}, new Object[] {
                consumer
            });

            response.put("status", "success");
            response.put("resourceAttributes", attributeMap);
            response.put("hostIp", attributeMap.get("host.ip"));
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            response.put("exceptionClass", e.getClass().getName());
        }

        return response;
    }

    private Class<?> loadGlobalOtelClass() throws ClassNotFoundException {
        try {
            return Class.forName(SHADED_GLOBAL_OTEL_CLASS);
        } catch (ClassNotFoundException e) {
            return Class.forName(GLOBAL_OTEL_CLASS);
        }
    }

    private Object unwrapOpenTelemetry(Object openTelemetry) {
        try {
            var delegateField = openTelemetry.getClass().getDeclaredField("delegate");
            delegateField.setAccessible(true);
            Object delegate = delegateField.get(openTelemetry);
            return delegate != null ? delegate : openTelemetry;
        } catch (Exception e) {
            return openTelemetry;
        }
    }

    private Object resolveResource(Object openTelemetry) throws Exception {
        Class<?> openTelemetrySdkClass = tryLoadClass(
                "io.opentelemetry.sdk.OpenTelemetrySdk",
                openTelemetry.getClass().getClassLoader());
        if (openTelemetrySdkClass != null && openTelemetrySdkClass.isInstance(openTelemetry)) {
            Object sdkTracerProvider = invokeMethod(
                    openTelemetry, openTelemetrySdkClass, "getSdkTracerProvider", new Class<?>[0], new Object[0]);
            return resolveResourceFromTracerProvider(sdkTracerProvider);
        }

        Object tracerProvider = invokeMethod(
                openTelemetry, openTelemetry.getClass(), "getTracerProvider", new Class<?>[0], new Object[0]);

        return resolveResourceFromTracerProvider(tracerProvider);
    }

    private Object resolveResourceFromTracerProvider(Object tracerProvider) throws Exception {
        try {
            return invokeMethod(
                    tracerProvider, tracerProvider.getClass(), "getResource", new Class<?>[0], new Object[0]);
        } catch (NoSuchMethodException e) {
            var sharedStateField = tracerProvider.getClass().getDeclaredField("sharedState");
            sharedStateField.setAccessible(true);
            Object sharedState = sharedStateField.get(tracerProvider);
            return invokeMethod(sharedState, sharedState.getClass(), "getResource", new Class<?>[0], new Object[0]);
        }
    }

    private Class<?> tryLoadClass(String name, ClassLoader classLoader) {
        try {
            return Class.forName(name, false, classLoader);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private Object invokeMethod(Object target, Class<?> type, String name, Class<?>[] parameterTypes, Object[] args)
            throws Exception {
        try {
            var method = type.getMethod(name, parameterTypes);
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (NoSuchMethodException e) {
            var method = type.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method.invoke(target, args);
        }
    }
}
