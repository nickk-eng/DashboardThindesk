package com.pfc.thindesk.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "horariosAtendimento")
public class HorarioAtendimento {

    @Id
    private String id;
    private String setor;
    private String diaSemana; // Exemplo: "Segunda-feira"
    private String horarioInicio; // Exemplo: "08:00"
    private String horarioFim; // Exemplo: "18:00"

    public HorarioAtendimento() { }
    public HorarioAtendimento(String id, String setor, String diaSemana, String horarioInicio, String horarioFim) {
        this.id = id;
        this.setor = setor;
        this.diaSemana = diaSemana;
        this.horarioInicio = horarioInicio;
        this.horarioFim = horarioFim;
    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSetor() { return setor; }
    public void setSetor(String setor) { this.setor = setor; }
    public String getDiaSemana() { return diaSemana; }
    public void setDiaSemana(String diaSemana) { this.diaSemana = diaSemana; }
    public String getHorarioInicio() { return horarioInicio; }
    public void setHorarioInicio(String horarioInicio) { this.horarioInicio = horarioInicio; }
    public String getHorarioFim() { return horarioFim; }
    public void setHorarioFim(String horarioFim) { this.horarioFim = horarioFim; }
}
