package com.pfc.thindesk.repository;

import com.pfc.thindesk.entity.ConfiguracaoItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ConfiguracaoItemRepository extends MongoRepository<ConfiguracaoItem, String> {
    List<ConfiguracaoItem> findByCategoriaOrderByNomeAsc(String categoria);
    Optional<ConfiguracaoItem> findByIdAndCategoria(String id, String categoria);
}
