package com.example.a3_teste_paineldevotao.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Modelo que representa a enquete armazenada no Firestore.
 *
 * Esta classe centraliza TODOS os campos da enquete:
 * - Textos de título e opções
 * - Contadores de votos
 *
 * Ela serve como “ponte” entre o Firestore e as Activities:
 * - Cada documento do Firestore é convertido para Enquete
 * - Cada Enquete pode ser convertida para Map ao salvar
 *
 * Mantemos simples de propósito, pois é justamente um modelo (dados puros).
 */
public class Enquete {

    // =====================================================================
    //  Campos de configuração (textos exibidos na UI)
    // =====================================================================

    private String tituloEnquete;
    private String textoOpcaoA;
    private String textoOpcaoB;
    private String textoOpcaoC;

    // =====================================================================
    //  Contadores de votos
    // =====================================================================

    private long opcaoA;
    private long opcaoB;
    private long opcaoC;

    // =====================================================================
    //  Construtores
    // =====================================================================

    /**
     * Construtor vazio.
     * Necessário para o Firestore caso futuramente usemos snapshot.toObject(Enquete.class).
     */
    public Enquete() {
    }

    /**
     * Construtor completo, usado para criar a enquete padrão ou inicializar dados carregados.
     */
    public Enquete(String tituloEnquete,
                   String textoOpcaoA,
                   String textoOpcaoB,
                   String textoOpcaoC,
                   long opcaoA,
                   long opcaoB,
                   long opcaoC) {

        this.tituloEnquete = tituloEnquete;
        this.textoOpcaoA = textoOpcaoA;
        this.textoOpcaoB = textoOpcaoB;
        this.textoOpcaoC = textoOpcaoC;
        this.opcaoA = opcaoA;
        this.opcaoB = opcaoB;
        this.opcaoC = opcaoC;
    }

    // =====================================================================
    //  Getters e Setters
    // =====================================================================

    // Mantidos simples para que o Repository manipule os valores quando carregar ou salvar.

    public String getTituloEnquete() {
        return tituloEnquete;
    }

    public void setTituloEnquete(String tituloEnquete) {
        this.tituloEnquete = tituloEnquete;
    }

    public String getTextoOpcaoA() {
        return textoOpcaoA;
    }

    public void setTextoOpcaoA(String textoOpcaoA) {
        this.textoOpcaoA = textoOpcaoA;
    }

    public String getTextoOpcaoB() {
        return textoOpcaoB;
    }

    public void setTextoOpcaoB(String textoOpcaoB) {
        this.textoOpcaoB = textoOpcaoB;
    }

    public String getTextoOpcaoC() {
        return textoOpcaoC;
    }

    public void setTextoOpcaoC(String textoOpcaoC) {
        this.textoOpcaoC = textoOpcaoC;
    }

    public long getOpcaoA() {
        return opcaoA;
    }

    public void setOpcaoA(long opcaoA) {
        this.opcaoA = opcaoA;
    }

    public long getOpcaoB() {
        return opcaoB;
    }

    public void setOpcaoB(long opcaoB) {
        this.opcaoB = opcaoB;
    }

    public long getOpcaoC() {
        return opcaoC;
    }

    public void setOpcaoC(long opcaoC) {
        this.opcaoC = opcaoC;
    }

    // =====================================================================
    //  Conversão para Map (útil para Firestore)
    // =====================================================================

    /**
     * Converte a enquete para um Map<String, Object>.
     *
     * Este formato é aceito diretamente pelo Firestore em:
     * - set()
     * - update()
     *
     * Deixamos aqui para evitar repetir código no Repository.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> dados = new HashMap<>();

        dados.put("tituloEnquete", tituloEnquete);
        dados.put("textoOpcaoA", textoOpcaoA);
        dados.put("textoOpcaoB", textoOpcaoB);
        dados.put("textoOpcaoC", textoOpcaoC);

        dados.put("opcaoA", opcaoA);
        dados.put("opcaoB", opcaoB);
        dados.put("opcaoC", opcaoC);

        return dados;
    }
}