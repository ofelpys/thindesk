package com.pfc.thindesk.entity;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "chamados")
public class Chamado {

    @Id
    private String id;
    private String descricao;
    private String status;
    private String tipo;
    private String tecnico;
    private String usuario;
    private Instant abertoEm;
    private Instant concluidoEm;
    private String consideracoesTecnicas;

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

    public Instant getAbertoEm() {
        return abertoEm;
    }

    public void setAbertoEm(Instant abertoEm) {
        this.abertoEm = abertoEm;
    }

    public Instant getConcluidoEm() {
        return concluidoEm;
    }

    public void setConcluidoEm(Instant concluidoEm) {
        this.concluidoEm = concluidoEm;
    }

    public String getConsideracoesTecnicas() {
        return consideracoesTecnicas;
    }

    public void setConsideracoesTecnicas(String consideracoesTecnicas) {
        this.consideracoesTecnicas = consideracoesTecnicas;
    }
}