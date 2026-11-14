package com.pfc.thindesk.service;

import com.pfc.thindesk.entity.HorarioAtendimento;
import com.pfc.thindesk.repository.HorarioAtendimentoRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HorarioAtendimentoService {

    @Autowired
    private HorarioAtendimentoRepository horarioAtendimentoRepository;

    public List<HorarioAtendimento> listarTodos() {
        return horarioAtendimentoRepository.findAll();
    }

    public List<HorarioAtendimento> listarPorSetor(String setor) {
        return horarioAtendimentoRepository.findBySetor(setor);
    }

    public HorarioAtendimento salvar(HorarioAtendimento horarioAtendimento) {
        if (horarioAtendimento.getSetor() == null || horarioAtendimento.getSetor().isBlank()
                || horarioAtendimento.getSetor().length() > 120
                || horarioAtendimento.getDiaSemana() == null || horarioAtendimento.getDiaSemana().isBlank()
                || horarioAtendimento.getDiaSemana().length() > 40) {
            throw new IllegalArgumentException("Informe o setor e o dia da semana.");
        }
        try {
            java.time.LocalTime inicio = java.time.LocalTime.parse(horarioAtendimento.getHorarioInicio());
            java.time.LocalTime fim = java.time.LocalTime.parse(horarioAtendimento.getHorarioFim());
            if (!inicio.isBefore(fim)) throw new IllegalArgumentException("O horário final deve ser posterior ao inicial.");
        } catch (java.time.format.DateTimeParseException | NullPointerException exception) {
            throw new IllegalArgumentException("Informe horários válidos no formato HH:mm.");
        }
        horarioAtendimento.setSetor(horarioAtendimento.getSetor().trim());
        horarioAtendimento.setDiaSemana(horarioAtendimento.getDiaSemana().trim());
        return horarioAtendimentoRepository.save(horarioAtendimento);
    }

    public void deletar(String id) {
        horarioAtendimentoRepository.deleteById(id);
    }
}
