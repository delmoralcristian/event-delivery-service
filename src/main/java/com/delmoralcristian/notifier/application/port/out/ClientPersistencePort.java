package com.delmoralcristian.notifier.application.port.out;

import com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.entity.ClientEntity;
import java.util.Optional;

public interface ClientPersistencePort {

    Optional<ClientEntity> findById(String id);

    boolean existsByIdAndApiKey(String clientId, String apiKey);

    boolean existsByApiKey(String apiKey);
}
