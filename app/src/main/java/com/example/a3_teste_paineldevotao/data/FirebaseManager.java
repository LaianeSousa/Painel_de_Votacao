package com.example.a3_teste_paineldevotao.data;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Centraliza toda a inicialização e acesso ao Firebase.
 *
 * Responsabilidades:
 * - Garantir que o Firebase seja inicializado apenas uma vez.
 * - Fornecer instâncias de FirebaseAuth e FirebaseFirestore.
 * - Expor a referência principal da enquete.
 * - Expor a referência do documento de voto do usuário atual.
 *
 * A ideia é que as Activities/Repositories não precisem lidar diretamente
 * com inicialização do Firebase, apenas chamem este manager.
 */
public class FirebaseManager {

    private static FirebaseManager instance;

    // =====================================================================
    //  Atributos principais do Firebase
    // =====================================================================

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final DocumentReference enqueteRef;

    /**
     * Construtor privado para forçar o uso do getInstance().
     * Aqui fazemos a configuração inicial do Firebase.
     */
    private FirebaseManager(Context context) {
        // Garante que o Firebase esteja inicializado com o contexto da aplicação
        FirebaseApp.initializeApp(context.getApplicationContext());

        // Instâncias principais
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Documento principal da enquete (único para todo o app)
        enqueteRef = db.collection("enquetes").document("enquete_geral");
    }

    /**
     * Retorna a instância única do FirebaseManager (padrão Singleton).
     *
     * @param context Context usado apenas na primeira inicialização.
     */
    public static FirebaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseManager(context);
        }
        return instance;
    }

    // =====================================================================
    //  Getters de acesso aos serviços
    // =====================================================================

    /**
     * Retorna a instância do FirebaseAuth usada no app.
     */
    public FirebaseAuth getAuth() {
        return auth;
    }

    /**
     * Retorna a instância do FirebaseFirestore usada no app.
     */
    public FirebaseFirestore getDb() {
        return db;
    }

    /**
     * Retorna a referência do documento principal da enquete.
     *
     * Exemplo de caminho:
     *  enquetes/enquete_geral
     */
    public DocumentReference getEnqueteRef() {
        return enqueteRef;
    }

    // =====================================================================
    //  Referência do voto do usuário
    // =====================================================================

    /**
     * Retorna a referência do documento de voto do usuário atual,
     * ou null se ainda não estiver logado.
     *
     * Estrutura esperada no Firestore:
     *  enquetes/enquete_geral/votos/{uid_do_usuario}
     */
    public DocumentReference getUserVoteRef() {
        if (auth.getCurrentUser() == null) {
            return null;
        }

        String uid = auth.getCurrentUser().getUid();
        return enqueteRef.collection("votos").document(uid);
    }
}
