package io.github.jongminchung.otel;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;

/**
 * Custom ResourceProvider that automatically detects and adds the host.ip attribute.
 * This provider will be loaded via Java SPI mechanism.
 */
public final class HostIpResourceProvider implements ResourceProvider {

    private static final String HOST_IP_KEY = "host.ip";
    private static final String SHADED_ATTRIBUTES_CLASS =
            "io.opentelemetry.javaagent.shaded.io.opentelemetry.api.common.Attributes";
    private static final String SHADED_ATTRIBUTES_BUILDER_CLASS =
            "io.opentelemetry.javaagent.shaded.io.opentelemetry.api.common.AttributesBuilder";

    @Override
    public Resource createResource(ConfigProperties config) {
        String hostIp = detectHostIp();
        return buildResource(hostIp);
    }

    private Resource buildResource(String hostIp) {
        try {
            Class<?> attributesClass = Class.forName(SHADED_ATTRIBUTES_CLASS);
            Class<?> attributesBuilderClass = Class.forName(SHADED_ATTRIBUTES_BUILDER_CLASS);
            Object builder = attributesClass.getMethod("builder").invoke(null);

            attributesBuilderClass.getMethod("put", String.class, String.class).invoke(builder, HOST_IP_KEY, hostIp);

            Object attributes = attributesBuilderClass.getMethod("build").invoke(builder);

            return (Resource)
                    Resource.class.getMethod("create", attributesClass).invoke(null, attributes);
        } catch (Exception e) {
            return Resource.empty();
        }
    }

    /**
     * Detects the host IP address by looking for non-loopback network interfaces.
     * Prefers IPv4 addresses over IPv6.
     */
    private String detectHostIp() {
        try {
            // First, try to get the first non-loopback address
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                // Skip loopback and inactive interfaces
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    var address = addresses.nextElement();

                    // Skip loopback addresses
                    if (address.isLoopbackAddress()) {
                        continue;
                    }

                    // Prefer IPv4 addresses
                    if (address.getAddress().length == 4) {
                        return address.getHostAddress();
                    }
                }
            }

            var localHost = InetAddress.getLocalHost();
            return localHost.getHostAddress();

        } catch (Exception e) {
            return "unknown";
        }
    }

    @Override
    public int order() {
        // Higher order to override default values if any
        return 100;
    }
}
