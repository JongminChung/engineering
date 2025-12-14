package io.github.jongminchung.study.apicommunication.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
@ConfigurationProperties(prefix = "app.security")
public class ApiSecurityProperties {

    private final List<Client> clients = new ArrayList<>();

    public Optional<Client> findClient(String tenantId, String clientId) {
        return clients.stream()
                .filter(client -> Objects.equals(client.getTenantId(), tenantId) && Objects.equals(client.getClientId(), clientId))
                .findFirst();
    }

    @Setter
    @Getter
    public static class Client {
        private String tenantId;
        private String clientId;
        private String hashedApiKey;
    }
}
