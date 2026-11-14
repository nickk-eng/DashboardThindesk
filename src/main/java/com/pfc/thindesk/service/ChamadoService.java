package com.pfc.thindesk.service;

import com.pfc.thindesk.entity.Chamado;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import com.pfc.thindesk.repository.ChamadoRepository;

@Service
public class ChamadoService {

    @Autowired
    private ChamadoRepository chamadoRepository;

    public Chamado criarChamado(Chamado chamado) {
        if (chamado.getDataAbertura() == null) chamado.setDataAbertura(java.time.Instant.now());
        return chamadoRepository.save(chamado);
    }

    public List<Chamado> listarChamados() {
        return chamadoRepository.findAll();
    }

    public Chamado atualizarChamado(String id, Chamado chamadoAtualizado) {
        return chamadoRepository.findById(id)
                .map(chamado -> {
                    chamado.setDescricao(chamadoAtualizado.getDescricao());
                    chamado.setStatus(chamadoAtualizado.getStatus());
                    chamado.setTipo(chamadoAtualizado.getTipo());
                    chamado.setTecnico(chamadoAtualizado.getTecnico());
                    chamado.setUsuario(chamadoAtualizado.getUsuario());
                    // Os formulários legados não enviam os novos campos opcionais.
                    if (chamadoAtualizado.getClienteId() != null) chamado.setClienteId(chamadoAtualizado.getClienteId());
                    if (chamadoAtualizado.getConsideracoesTecnicas() != null) chamado.setConsideracoesTecnicas(chamadoAtualizado.getConsideracoesTecnicas());
                    if (chamadoAtualizado.getDataConclusao() != null) chamado.setDataConclusao(chamadoAtualizado.getDataConclusao());
                    return chamadoRepository.save(chamado);
                })
                .orElseThrow(() -> new RuntimeException("Chamado não encontrado com id: " + id));
    }

}
