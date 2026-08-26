package com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.repository;

import com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.entity.ClientEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaClientRepository extends CrudRepository<ClientEntity, String> {

}
