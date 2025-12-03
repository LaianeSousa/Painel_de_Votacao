package com.example.a3_teste_paineldevotao.model;

import com.google.firebase.firestore.Exclude;

import java.util.Date;
/**
 * Modelo que representa um VOTO individual na subcoleção 'votos'.
 * * Caminho no Firestore: enquetes/enquete_geral/votos/{document_id}
 */
public class VotoModel {

    private String opcaoEscolhida;
    private Date timestamp;
    @Exclude
    private String uid;

    // construtor

    public VotoModel(){

    }

    public VotoModel(String opcaoEscolhida, Date timestamp) {
        this.opcaoEscolhida = opcaoEscolhida;
        this.timestamp = timestamp;
    }

    public String getOpcaoEscolhida() {
        return opcaoEscolhida;
    }

    public void setOpcaoEscolhida(String opcaoEscolhida) {
        this.opcaoEscolhida = opcaoEscolhida;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
