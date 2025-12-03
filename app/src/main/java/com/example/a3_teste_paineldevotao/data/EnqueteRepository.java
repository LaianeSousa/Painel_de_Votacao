package com.example.a3_teste_paineldevotao.data;

import android.content.Context;

import androidx.annotation.Nullable;

import com.example.a3_teste_paineldevotao.model.Enquete;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Repositório responsável por TODA a comunicação com o Firestore
 * relacionada à enquete:
 *
 * - Inicializar enquete padrão
 * - Observar mudanças em tempo real
 * - Carregar dados pontuais (uma vez)
 * - Salvar configurações (título e opções)
 * - Registrar votos do usuário
 * - Resetar votação
 *
 * A ideia é deixar a Activity “limpa”, chamando apenas métodos
 * deste repositório.
 */
public class EnqueteRepository {

    private final FirebaseManager firebaseManager;
    private final DocumentReference enqueteRef;

    /**
     * Construtor recebe o Context apenas para inicializar o FirebaseManager.
     */
    public EnqueteRepository(Context context) {
        this.firebaseManager = FirebaseManager.getInstance(context);
        this.enqueteRef = firebaseManager.getEnqueteRef();
    }

    // =====================================================================
    //  Inicialização da enquete padrão
    // =====================================================================

    /**
     * Cria uma enquete padrão no Firestore CASO ainda não exista.
     * Deve ser chamado, por exemplo, na tela principal ao iniciar o app,
     * apenas para garantir que o documento base esteja criado.
     */
    public void inicializarSeNecessario() {
        enqueteRef.get().addOnSuccessListener(snapshot -> {
            // Se o documento não existe, criamos a enquete padrão
            if (snapshot == null || !snapshot.exists()) {
                Enquete enquetePadrao = new Enquete(
                        "Em qual opção você deseja votar?",
                        "Opção A",
                        "Opção B",
                        "Opção C",
                        0, 0, 0
                );
                // Salvamos o mapa da enquete no Firestore
                enqueteRef.set(enquetePadrao.toMap());
            }
        });
    }

    // =====================================================================
    //  Listener em tempo real da enquete
    // =====================================================================

    /**
     * Adiciona um listener em tempo real para a enquete.
     *
     * Sempre que o documento for alterado no Firestore, o listener será
     * notificado e a Activity (ou Fragment) receberá o objeto Enquete
     * atualizado pelo callback {@link EnqueteListener#onEnqueteAtualizada(Enquete)}.
     *
     * @param listener interface de callback para notificar a UI
     * @return ListenerRegistration para permitir remover o listener no onStop/onDestroy
     */
    public ListenerRegistration observarEnquete(EnqueteListener listener) {
        return enqueteRef.addSnapshotListener((snapshot, error) -> {

            // Se houve erro ou o documento não existe, avisamos a UI
            if (error != null || snapshot == null || !snapshot.exists()) {
                listener.onErro(error);
                return;
            }

            // Monta o objeto Enquete a partir do documento do Firestore
            Enquete enquete = new Enquete();
            enquete.setTituloEnquete(snapshot.getString("tituloEnquete"));
            enquete.setTextoOpcaoA(snapshot.getString("textoOpcaoA"));
            enquete.setTextoOpcaoB(snapshot.getString("textoOpcaoB"));
            enquete.setTextoOpcaoC(snapshot.getString("textoOpcaoC"));

            // Contadores podem ser nulos, então tratamos para evitar NullPointerException
            Long a = snapshot.getLong("opcaoA");
            Long b = snapshot.getLong("opcaoB");
            Long c = snapshot.getLong("opcaoC");

            enquete.setOpcaoA(a != null ? a : 0);
            enquete.setOpcaoB(b != null ? b : 0);
            enquete.setOpcaoC(c != null ? c : 0);

            // Notifica a UI com a enquete atualizada
            listener.onEnqueteAtualizada(enquete);
        });
    }

    // =====================================================================
    //  Leitura pontual da enquete (ex.: onResume)
    // =====================================================================

    /**
     * Carrega a enquete apenas UMA vez (sem ficar ouvindo em tempo real).
     *
     * Útil, por exemplo, no onResume ou quando só precisamos do estado
     * atual sem manter listener aberto.
     *
     * @param callback callback chamado com sucesso ou erro
     */
    public void carregarEnquete(final EnqueteCarregadaCallback callback) {
        enqueteRef.get()
                .addOnSuccessListener(snapshot -> {
                    // Se não existe, avisamos erro genérico (poderia ser tratado melhor)
                    if (snapshot == null || !snapshot.exists()) {
                        callback.onErro(null);
                        return;
                    }

                    // Monta o objeto Enquete a partir do documento
                    Enquete enquete = new Enquete();
                    enquete.setTituloEnquete(snapshot.getString("tituloEnquete"));
                    enquete.setTextoOpcaoA(snapshot.getString("textoOpcaoA"));
                    enquete.setTextoOpcaoB(snapshot.getString("textoOpcaoB"));
                    enquete.setTextoOpcaoC(snapshot.getString("textoOpcaoC"));

                    Long a = snapshot.getLong("opcaoA");
                    Long b = snapshot.getLong("opcaoB");
                    Long c = snapshot.getLong("opcaoC");

                    enquete.setOpcaoA(a != null ? a : 0);
                    enquete.setOpcaoB(b != null ? b : 0);
                    enquete.setOpcaoC(c != null ? c : 0);

                    callback.onEnqueteCarregada(enquete);
                })
                .addOnFailureListener(callback::onErro);
    }

    // =====================================================================
    //  Configuração da enquete (tela 2)
    // =====================================================================

    /**
     * Salva as configurações da enquete (título e textos das opções).
     * Não mexe nos contadores de votos, apenas no “texto” exibido na tela.
     *
     * @param titulo  texto do título da enquete
     * @param opcaoA  texto da opção A
     * @param opcaoB  texto da opção B
     * @param opcaoC  texto da opção C
     * @param callback chamado em sucesso ou erro
     */
    public void salvarConfiguracoes(String titulo,
                                    String opcaoA,
                                    String opcaoB,
                                    String opcaoC,
                                    OperacaoCallback callback) {

        Map<String, Object> dados = new HashMap<>();
        dados.put("tituloEnquete", titulo);
        dados.put("textoOpcaoA", opcaoA);
        dados.put("textoOpcaoB", opcaoB);
        dados.put("textoOpcaoC", opcaoC);

        // merge() apenas atualiza estes campos, mantendo os demais (contadores, etc.)
        enqueteRef.set(dados, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSucesso())
                .addOnFailureListener(callback::onErro);
    }

    /**
     * Carrega apenas as configurações (título e textos das opções) da enquete.
     * Não retorna os contadores de votos, apenas os textos.
     *
     * @param callback callback com os textos carregados ou erro
     */
    public void carregarConfiguracoes(ConfiguracaoCarregadaCallback callback) {
        enqueteRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) {
                        callback.onErro(null);
                        return;
                    }

                    String titulo = snapshot.getString("tituloEnquete");
                    String opcaoA = snapshot.getString("textoOpcaoA");
                    String opcaoB = snapshot.getString("textoOpcaoB");
                    String opcaoC = snapshot.getString("textoOpcaoC");

                    callback.onConfiguracaoCarregada(titulo, opcaoA, opcaoB, opcaoC);
                })
                .addOnFailureListener(callback::onErro);
    }

    // =====================================================================
    //  Voto do usuário
    // =====================================================================

    /**
     * Verifica, no Firestore, qual opção o usuário já votou (se é que já votou).
     * Se não houver voto registrado, o callback recebe null.
     *
     * @param callback callback com a opção (“A”, “B”, “C”) ou null
     */
    public void carregarVotoUsuario(VotoUsuarioCallback callback) {
        DocumentReference votoRef = firebaseManager.getUserVoteRef();

        // Se não há usuário logado, não há como buscar voto
        if (votoRef == null) {
            callback.onVotoCarregado(null);
            return;
        }

        votoRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && snapshot.exists()) {
                        String opcao = snapshot.getString("opcaoEscolhida");
                        callback.onVotoCarregado(opcao);
                    } else {
                        callback.onVotoCarregado(null);
                    }
                })
                .addOnFailureListener(e -> callback.onVotoCarregado(null));
    }

    /**
     * Registra o voto do usuário em uma das opções (A/B/C), garantindo:
     * - O usuário só pode votar uma vez (se já houver documento, chama onJaVotou).
     * - Incrementa o contador da opção escolhida no documento da enquete.
     * - Cria/atualiza o documento de voto do usuário com a opção e timestamp.
     *
     * @param opcao    "A", "B" ou "C"
     * @param callback callback com sucesso, já votou ou erro
     */
    public void registrarVoto(String opcao, RegistrarVotoCallback callback) {

        DocumentReference votoRef = firebaseManager.getUserVoteRef();
        if (votoRef == null) {
            callback.onErro(new IllegalStateException("Usuário não logado."));
            return;
        }

        // Primeiro verificamos se o usuário já votou
        votoRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && snapshot.exists()) {
                        String jaVotouOpcao = snapshot.getString("opcaoEscolhida");
                        callback.onJaVotou(jaVotouOpcao);
                        return;
                    }

                    // Mapeia "A", "B" ou "C" para o campo correspondente no Firestore
                    String campo =
                            opcao.equals("A") ? "opcaoA" :
                                    opcao.equals("B") ? "opcaoB" : "opcaoC";

                    // Incrementa o contador da opção no documento da enquete
                    enqueteRef.update(campo, FieldValue.increment(1))
                            .addOnSuccessListener(unused -> {
                                // Depois de atualizar o contador, salvamos o voto do usuário
                                Map<String, Object> voto = new HashMap<>();
                                voto.put("opcaoEscolhida", opcao);
                                voto.put("timestamp", FieldValue.serverTimestamp());

                                votoRef.set(voto)
                                        .addOnSuccessListener(unused2 -> callback.onVotoRegistrado(opcao))
                                        .addOnFailureListener(callback::onErro);
                            })
                            .addOnFailureListener(callback::onErro);
                })
                .addOnFailureListener(callback::onErro);
    }

    // =====================================================================
    //  Reset da enquete
    // =====================================================================

    /**
     * Reseta a enquete:
     * - Zera os contadores das três opções.
     * - Remove todos os documentos da subcoleção "votos" (votos por usuário).
     *
     * Útil na tela de administração para começar uma votação “do zero”.
     *
     * @param callback callback de sucesso ou erro
     */
    public void resetarEnquete(OperacaoCallback callback) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("opcaoA", 0L);
        dados.put("opcaoB", 0L);
        dados.put("opcaoC", 0L);

        // Primeiro zera os contadores
        enqueteRef.set(dados, SetOptions.merge())
                .addOnSuccessListener(unused -> {

                    // Depois remove todos os documentos da subcoleção "votos"
                    enqueteRef.collection("votos")
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                querySnapshot.getDocuments()
                                        .forEach(doc -> doc.getReference().delete());
                                callback.onSucesso();
                            })
                            .addOnFailureListener(callback::onErro);

                })
                .addOnFailureListener(callback::onErro);
    }

    // =====================================================================
    //  Interfaces de callback
    // =====================================================================

    /**
     * Listener para receber atualizações em tempo real da enquete.
     */
    public interface EnqueteListener {
        void onEnqueteAtualizada(Enquete enquete);

        void onErro(@Nullable Exception e);
    }

    /**
     * Callback para carregamento pontual da enquete.
     */
    public interface EnqueteCarregadaCallback {
        void onEnqueteCarregada(Enquete enquete);

        void onErro(@Nullable Exception e);
    }

    /**
     * Callback para carregamento apenas das configurações (título e textos).
     */
    public interface ConfiguracaoCarregadaCallback {
        void onConfiguracaoCarregada(String titulo,
                                     String opcaoA,
                                     String opcaoB,
                                     String opcaoC);

        void onErro(@Nullable Exception e);
    }

    /**
     * Callback genérico para operações simples (sucesso/erro).
     */
    public interface OperacaoCallback {
        void onSucesso();

        void onErro(@Nullable Exception e);
    }

    /**
     * Callback para informar qual opção o usuário já votou (ou null).
     */
    public interface VotoUsuarioCallback {
        void onVotoCarregado(@Nullable String opcao);
    }

    /**
     * Callback completo para registrar voto:
     * - onVotoRegistrado: voto salvo com sucesso
     * - onJaVotou: usuário já tinha votado antes
     * - onErro: falha geral
     */
    public interface RegistrarVotoCallback {
        void onVotoRegistrado(String opcao);

        void onJaVotou(@Nullable String opcaoExistente);

        void onErro(@Nullable Exception e);
    }
}