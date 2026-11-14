package com.pfc.thindesk.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "chamados")
public class Chamado{
    private java.time.Instant dataAbertura;
    private java.time.Instant dataConclusao;
    private String clienteId;
    private String consideracoesTecnicas;

    public java.time.Instant getDataAbertura() { return dataAbertura; }
    public void setDataAbertura(java.time.Instant dataAbertura) { this.dataAbertura = dataAbertura; }
    public java.time.Instant getDataConclusao() { return dataConclusao; }
    public void setDataConclusao(java.time.Instant dataConclusao) { this.dataConclusao = dataConclusao; }
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
    public String getConsideracoesTecnicas() { return consideracoesTecnicas; }
    public void setConsideracoesTecnicas(String consideracoesTecnicas) { this.consideracoesTecnicas = consideracoesTecnicas; }
    
    @Id
    private String id;
    private String descricao;
    private String status;
    private String tipo;
    private String tecnico;
    private String usuario;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTecnico() {
        return tecnico;
    }

    public void setTecnico(String tecnico) {
        this.tecnico = tecnico;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }
    
    
    
}
