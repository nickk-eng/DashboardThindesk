package com.pfc.thindesk.service;

import com.pfc.thindesk.entity.ConfiguracaoItem;
import com.pfc.thindesk.repository.ConfiguracaoItemRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConfiguracaoService {
    private static final Map<String, String> TITULOS = Map.of(
            "tecnicos", "Técnicos", "tipo-chamado", "Tipos de chamado", "recursos", "Recursos");
    private final ConfiguracaoItemRepository repository;
    public ConfiguracaoService(ConfiguracaoItemRepository repository) { this.repository = repository; }
    public String titulo(String categoria) {
        if (!TITULOS.containsKey(categoria)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return TITULOS.get(categoria);
    }
    public List<ConfiguracaoItem> listar(String categoria) {
        titulo(categoria);
        return repository.findByCategoriaOrderByNomeAsc(categoria);
    }
    public ConfiguracaoItem buscar(String categoria, String id) {
        titulo(categoria);
        return repository.findByIdAndCategoria(id, categoria)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cadastro não encontrado"));
    }
    public void salvar(String categoria, ConfiguracaoItem item) {
        titulo(categoria);
        if (item.getNome() == null || item.getNome().isBlank() || item.getNome().length() > 120) {
            throw new IllegalArgumentException("Informe um nome de até 120 caracteres.");
        }
        if (item.getDescricao() != null && item.getDescricao().length() > 1000) {
            throw new IllegalArgumentException("A descrição deve ter até 1000 caracteres.");
        }
        ConfiguracaoItem destino = item.getId() == null || item.getId().isBlank()
                ? new ConfiguracaoItem() : buscar(categoria, item.getId());
        destino.setCategoria(categoria);
        destino.setNome(item.getNome().trim());
        destino.setDescricao(item.getDescricao() == null ? "" : item.getDescricao().trim());
        repository.save(destino);
    }
    public void excluir(String categoria, String id) { repository.delete(buscar(categoria, id)); }
}
