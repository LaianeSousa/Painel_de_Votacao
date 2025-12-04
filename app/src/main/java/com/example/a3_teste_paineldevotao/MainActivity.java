package com.example.a3_teste_paineldevotao;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.a3_teste_paineldevotao.data.EnqueteRepository;
import com.example.a3_teste_paineldevotao.data.FirebaseManager;
import com.example.a3_teste_paineldevotao.model.Enquete;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Tela principal do aplicativo (Painel de Votação).
 *
 * Responsabilidades:
 * - Exibir a pergunta da enquete e as opções de voto.
 * - Mostrar o total de votos por opção e o total geral.
 * - Permitir ao usuário votar (uma única vez).
 * - Exibir qual foi o voto do usuário.
 * - Permitir reset da enquete (com código de professor).
 * - Permitir acesso à tela de configuração da enquete (menu).
 *
 * Toda a lógica de Firestore está encapsulada em EnqueteRepository e FirebaseManager.
 * Aqui focamos na parte de UI e fluxo de tela.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PainelVotacao";
    // Formato de data e hora que deve ser usado para parsear a string do Firestore e formatar o Timestamp
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

    // Componentes de interface
    private TextView txtSubtitulo;
    private TextView txtPergunta;
    private TextView txtTituloResultados;
    private TextView txtTotalA;
    private TextView txtTotalB;
    private TextView txtTotalC;
    private TextView txtTotalGeral;
    private TextView txtSeuVoto;

    //////QUESTÃO01/////
    /// 01 São necessários para exibir os novos metadados do voto (Data/Hora e UID)
    /// na interface do usuário.
    private TextView txtDataVoto;
    private TextView txtSeuUid;
    //////FIM QUESTÃO01//////

    private Button btnVotarA;
    private Button btnVotarB;
    private Button btnVotarC;
    private Button btnReset;

    //----------------------------------Q2----------------------------------------------------------------
    private TextView txtModeloDispositivo;
    private TextView txtVersaoAndroid;
    //----------------------------------------------------------------------------------------------------
    // Q5: NOVOS CAMPOS DE RODAPÉ E ENCERRAMENTO
    private TextView txtRodape;
    private TextView txtDataEncerramento;

    // =====================================================================
    //  Firebase / Repositório
    // =====================================================================

    private FirebaseManager firebaseManager;
    private FirebaseAuth auth;
    private EnqueteRepository enqueteRepository;
    private ListenerRegistration resultadosListener;
    private Enquete enqueteAtual; // Armazena o último estado da enquete para checagem de tempo
    private String votoDoUsuario = null; // Armazena o voto do usuário localmente

    // =====================================================================
    //  Ciclo de vida
    // =====================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        configurarToolbar();
        aplicarInsets();

        inicializarFirebase();
        inicializarViews();
        fazerLoginAnonimo();
        configurarBotoes();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reforço: carrega estado atual da enquete ao voltar para a tela
        enqueteRepository.carregarEnquete(new EnqueteRepository.EnqueteCarregadaCallback() {
            @Override
            public void onEnqueteCarregada(Enquete enquete) {
                enqueteAtual = enquete; // Garante que a enqueteAtual não seja nula
                atualizarUIComEnquete(enquete);
            }

            @Override
            public void onErro(Exception e) {
                Log.e(TAG, "Erro ao carregar enquete (onResume): ", e);
            }
        });
        ///////////////////////////QUESTÃO01/////////////////////////////////
        //O carregamento do voto é feito em onResume() para garantir que os
        // metadados (Data/Hora e UID) estejam atualizados ao iniciar ou retornar
        // à tela.

        carregarVotoUsuario();
        /////////////////////// FIM QUESTÃO01///////////////////////////////////
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove listener em tempo real para evitar leaks
        if (resultadosListener != null) {
            resultadosListener.remove();
            resultadosListener = null;
        }
    }

    // =====================================================================
    //  Toolbar e Insets (UI)
    // =====================================================================

    /**
     * Configura a Toolbar como ActionBar da tela principal.
     */
    private void configurarToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle("Painel de Votação");
        }

        // Em algumas versões, setTitleCentered pode não existir; protegemos com try/catch
        try {
            toolbar.setTitleCentered(false);
        } catch (Exception ignored) {
        }
    }

    /**
     * Ajusta os paddings para considerar as barras de sistema
     * (status bar, nav bar) com EdgeToEdge.
     */
    private void aplicarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layoutMain),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                });
    }

    // =====================================================================
    //  Menu (acesso à tela de configuração)
    // =====================================================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Menu superior da tela principal.
     * Aqui só temos a opção de ir para "Configurar enquete".
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.menu_configurar_enquete) {
            startActivity(new Intent(this, ConfigurarEnqueteActivity.class));
            Toast.makeText(this, "Configurações...", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.menu_lista_votantes) {
            startActivity(new Intent(this, ListaVotantesActivity.class));
            return true;
        } else if (itemId == R.id.action_historico) {
            // --- AQUI ESTÁ A CORREÇÃO: Chama a tela de Histórico ---
            Intent intent = new Intent(this, HistoricoLogsActivity.class);
            startActivity(intent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    // =====================================================================
    //  Inicialização (Firebase, Views, Login)
    // =====================================================================

    /**
     * Inicializa FirebaseManager, FirebaseAuth e EnqueteRepository.
     */
    private void inicializarFirebase() {
        firebaseManager = FirebaseManager.getInstance(this);
        auth = firebaseManager.getAuth();
        enqueteRepository = new EnqueteRepository(this);
    }

    /**
     * Faz o findViewById de todos os componentes da interface.
     */
    private void inicializarViews() {
        txtSubtitulo = findViewById(R.id.txtSubtitulo);
        txtPergunta = findViewById(R.id.txtPergunta);
        txtTituloResultados = findViewById(R.id.txtTituloResultados);
        txtTotalA = findViewById(R.id.txtTotalA);
        txtTotalB = findViewById(R.id.txtTotalB);
        txtTotalC = findViewById(R.id.txtTotalC);
        txtTotalGeral = findViewById(R.id.txtTotalGeral);
        txtSeuVoto = findViewById(R.id.txtSeuVoto);

        ///////QUESTÃO01/////
        ///02 Conecta as variáveis Java aos componentes visuais (criados no XML)
        ///através de seus IDs
        txtDataVoto = findViewById(R.id.txtDataVoto);
        txtSeuUid = findViewById(R.id.txtSeuUid);
        //////FIM QUESTÃO01//////

        //-------------------INICIALIZAÇÃO DOS NOVOS TEXTVIEWS ---Q2-------------------------------------
        txtModeloDispositivo = findViewById(R.id.txtModeloDispositivo);
        txtVersaoAndroid = findViewById(R.id.txtVersaoAndroid);
        //---------------------------------------------------------------------------------------------

        // Q5: Inicialização dos novos TextViews de Rodapé e Encerramento.
        // O ID de layout usado é 'txtEncerramento', então o findViewById deve corresponder.
        txtRodape = findViewById(R.id.txtRodape);
        txtDataEncerramento = findViewById(R.id.txtEncerramento);

        btnVotarA = findViewById(R.id.btnVotarA);
        btnVotarB = findViewById(R.id.btnVotarB);
        btnVotarC = findViewById(R.id.btnVotarC);
        btnReset = findViewById(R.id.btnReset);
    }

    /**
     * Faz login anônimo no Firebase Auth.
     * Isso permite identificar o usuário unicamente (UID) sem exigir cadastro.
     */
    private void fazerLoginAnonimo() {
        // Se já está logado, não precisa fazer login novamente
        if (auth.getCurrentUser() != null) {
            configurarPosLogin();
            return;
        }

        auth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(
                                MainActivity.this,
                                "Conectado (anônimo).",
                                Toast.LENGTH_SHORT
                        ).show();
                        configurarPosLogin();
                    } else {
                        Toast.makeText(
                                MainActivity.this,
                                "Erro ao conectar. Verifique a conexão e as regras do Firebase.",
                                Toast.LENGTH_LONG
                        ).show();
                        Log.w(TAG, "signInAnonymously:failure", task.getException());
                    }
                });
    }

    /**
     * Carrega a enquete e o voto do usuário após o login.
     * Este método é chamado somente depois que o usuário está autenticado.
     */
    private void configurarPosLogin() {
        if (auth.getCurrentUser() == null) return;

        enqueteRepository.carregarEnquete(new EnqueteRepository.EnqueteCarregadaCallback() {
            @Override
            public void onEnqueteCarregada(Enquete enquete) {
                enqueteAtual = enquete; // Armazena o estado inicial
                atualizarUIComEnquete(enquete);
                configurarListenerResultados();
            }

            @Override
            public void onErro(Exception e) {
                Toast.makeText(MainActivity.this, "Erro ao carregar enquete.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Erro ao carregar enquete inicial: ", e);
            }
        });
        carregarVotoUsuario();
    }


    /**
     * Configura os listeners de clique para todos os botões da tela.
     */
    private void configurarBotoes() {
        btnVotarA.setOnClickListener(v -> registrarVoto("A"));
        btnVotarB.setOnClickListener(v -> registrarVoto("B"));
        btnVotarC.setOnClickListener(v -> registrarVoto("C"));
        btnReset.setOnClickListener(v -> solicitarResetEnquete());
    }

    // =====================================================================
    //  Lógica de Votação
    // =====================================================================

    /**
     * Registra o voto do usuário no Firestore.
     *
     * @param opcao A opção escolhida pelo usuário ("A", "B", ou "C").
     */
    private void registrarVoto(String opcao) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Aguarde, conectando...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validação Q5: Checa se a enquete já foi encerrada
        if (enqueteAtual != null && enqueteAtual.isEncerrada()) {
            Toast.makeText(this, "Votação encerrada.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Trava os botões para evitar votos múltiplos enquanto a operação ocorre
        travarBotoesVotacao();

        enqueteRepository.registrarVoto(opcao, new EnqueteRepository.VotoCallback() {
            @Override
            public void onSucesso() {
                Toast.makeText(MainActivity.this, "Voto '" + opcao + "' registrado!", Toast.LENGTH_SHORT).show();
                votoDoUsuario = opcao; // Armazena localmente
                atualizarSeuVotoUI(opcao, new Timestamp(new Date())); // Atualiza a UI imediatamente com a data atual
            }

            @Override
            public void onFalha(Exception e) {
                Toast.makeText(MainActivity.this, "Erro ao votar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                destravarBotoesVotacao(); // Destrava se deu erro
            }

            @Override
            public void onVotoJaExistente() {
                Toast.makeText(MainActivity.this, "Você já votou nesta enquete.", Toast.LENGTH_SHORT).show();
                // Não precisa destravar, pois o estado de "já votou" deve ser mantido
            }
        });
    }

    /**
     * Carrega e exibe o voto anterior do usuário, se houver.
     */
    private void carregarVotoUsuario() {
        if (auth.getCurrentUser() == null) {
            return; // Ainda não logado
        }

        enqueteRepository.getVotoDoUsuario(new EnqueteRepository.VotoUsuarioCallback() {
            @Override
            public void onVotoCarregado(String opcao, Timestamp dataVoto) {
                if (opcao != null) {
                    votoDoUsuario = opcao;
                    atualizarSeuVotoUI(opcao, dataVoto);
                    travarBotoesVotacao();
                } else {
                    votoDoUsuario = null;
                    atualizarSeuVotoUI(null, null);
                    destravarBotoesVotacao();
                }
            }

            @Override
            public void onErro(Exception e) {
                Log.e(TAG, "Erro ao buscar voto do usuário: ", e);
            }
        });
    }

    /**
     * Exibe um diálogo para solicitar o código do professor e resetar a enquete.
     */
    private void solicitarResetEnquete() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Resetar Enquete");
        builder.setMessage("Digite o código do professor para limpar todos os votos:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Resetar", (dialog, which) -> {
            String codigo = input.getText().toString();
            enqueteRepository.resetarEnquete(codigo, new EnqueteRepository.ResetCallback() {
                @Override
                public void onSucesso() {
                    Toast.makeText(MainActivity.this, "Enquete resetada!", Toast.LENGTH_SHORT).show();
                    // UI será atualizada pelo listener em tempo real
                }

                @Override
                public void onFalha(String mensagem) {
                    Toast.makeText(MainActivity.this, mensagem, Toast.LENGTH_LONG).show();
                }
            });
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // =====================================================================
    //  Listeners e Atualização de UI
    // =====================================================================

    /**
     * Configura um listener em tempo real para os resultados da enquete.
     * A UI é atualizada sempre que houver uma mudança no Firestore.
     */
    private void configurarListenerResultados() {
        if (resultadosListener == null) {
            resultadosListener = enqueteRepository.adicionarListenerResultados(
                    (enquete, e) -> {
                        if (e != null) {
                            Log.e(TAG, "Erro no listener de resultados: ", e);
                            return;
                        }
                        if (enquete != null) {
                            enqueteAtual = enquete;
                            atualizarUIComEnquete(enquete);
                        }
                    }
            );
        }
    }

    /**
     * Atualiza todos os componentes da UI com os dados da enquete.
     *
     * @param enquete O objeto Enquete com os dados mais recentes.
     */
    private void atualizarUIComEnquete(Enquete enquete) {
        if (enquete == null) return;

        // Atualiza pergunta e subtitulo
        txtPergunta.setText(enquete.getPergunta());
        txtSubtitulo.setText(enquete.getSubtitulo());

        // Atualiza totais de votos
        txtTotalA.setText("Opção A: " + enquete.getTotalVotosA());
        txtTotalB.setText("Opção B: " + enquete.getTotalVotosB());
        txtTotalC.setText("Opção C: " + enquete.getTotalVotosC());
        txtTotalGeral.setText("Total de Votos: " + enquete.getTotalGeral());

        // Atualiza rodapé e data de encerramento (Q5)
        txtRodape.setText(enquete.getRodape());
        if (enquete.getDataEncerramento() != null) {
            try {
                // Tenta parsear a string de data/hora
                Date dataFim = DATE_FORMAT.parse(enquete.getDataEncerramento());
                // Formata de volta para um padrão amigável
                String dataFormatada = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault()).format(dataFim);
                txtDataEncerramento.setText("Encerra em: " + dataFormatada);
                txtDataEncerramento.setVisibility(View.VISIBLE);
            } catch (ParseException e) {
                // Se o formato for inesperado, apenas mostra a string original
                txtDataEncerramento.setText("Encerra em: " + enquete.getDataEncerramento());
                txtDataEncerramento.setVisibility(View.VISIBLE);
                Log.e(TAG, "Erro ao parsear data de encerramento: ", e);
            }
        } else {
            txtDataEncerramento.setVisibility(View.GONE);
        }

        // Se o usuário já votou, mantém os botões travados.
        // Senão, verifica se a enquete está encerrada para travar/destravar.
        if (votoDoUsuario == null) {
            if (enquete.isEncerrada()) {
                travarBotoesVotacao();
                Toast.makeText(this, "Votação encerrada.", Toast.LENGTH_SHORT).show();
            } else {
                destravarBotoesVotacao();
            }
        }
    }


    /**
     * Atualiza a seção "Seu Voto" na UI.
     *
     * @param opcao    A opção votada ("A", "B", "C") ou null se não houver voto.
     * @param dataVoto O Timestamp do voto.
     */
    private void atualizarSeuVotoUI(String opcao, Timestamp dataVoto) {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        if (opcao != null) {
            txtSeuVoto.setText("Sua escolha: " + opcao);
            txtSeuUid.setText("Seu ID: " + uid);
            if (dataVoto != null) {
                txtDataVoto.setText("Data do voto: " + DATE_FORMAT.format(dataVoto.toDate()));
            } else {
                txtDataVoto.setText("Data do voto: N/D");
            }
            txtSeuVoto.setVisibility(View.VISIBLE);
            txtSeuUid.setVisibility(View.VISIBLE);
            txtDataVoto.setVisibility(View.VISIBLE);
        } else {
            txtSeuVoto.setVisibility(View.GONE);
            txtSeuUid.setVisibility(View.GONE);
            txtDataVoto.setVisibility(View.GONE);
        }
    }

    /**
     * Desabilita os botões de votação para impedir novos votos.
     */
    private void travarBotoesVotacao() {
        btnVotarA.setEnabled(false);
        btnVotarB.setEnabled(false);
        btnVotarC.setEnabled(false);
        btnVotarA.setAlpha(0.5f);
        btnVotarB.setAlpha(0.5f);
        btnVotarC.setAlpha(0.5f);
    }

    /**
     * Habilita os botões de votação.
     */
    private void destravarBotoesVotacao() {
        btnVotarA.setEnabled(true);
        btnVotarB.setEnabled(true);
        btnVotarC.setEnabled(true);
        btnVotarA.setAlpha(1.0f);
        btnVotarB.setAlpha(1.0f);
        btnVotarC.setAlpha(1.0f);
    }
}
