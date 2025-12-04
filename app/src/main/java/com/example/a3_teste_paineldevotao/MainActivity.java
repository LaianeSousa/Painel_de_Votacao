package com.example.a3_teste_paineldevotao;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
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

import java.text.SimpleDateFormat;
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
    /// Questão 01 ///
    //03 Define o formato padrão para converter o Timestamp do Firestore em uma string legível.
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
    /// FIM Questão 01 ///

    private static final String TAG = "PainelVotacao";

    // =====================================================================
    //  Componentes de interface
    // =====================================================================

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

    // =====================================================================
    //  Firebase / Repositório
    // =====================================================================

    private FirebaseManager firebaseManager;
    private FirebaseAuth auth;
    private EnqueteRepository enqueteRepository;
    private ListenerRegistration resultadosListener;

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
        if (item.getItemId() == R.id.menu_configurar_enquete) {
            Intent intent = new Intent(MainActivity.this, ConfigurarEnqueteActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
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
                                "Erro ao conectar.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    /**
     * Configura o estado da tela após garantir que o usuário está logado:
     * - Garante documento inicial da enquete.
     * - Inicia listener em tempo real para os resultados.
     * - Carrega o voto atual do usuário.
     */
    private void configurarPosLogin() {
        // Garante que o documento da enquete exista
        enqueteRepository.inicializarSeNecessario();

        // Começa a ouvir as mudanças em tempo real da enquete
        configurarListenerResultados();

        // Atualiza a informação "Seu voto" na tela
        carregarVotoUsuario();
    }

    /**
     * Configura um listener em tempo real para acompanhar mudanças
     * nos resultados da enquete.
     */
    private void configurarListenerResultados() {
        resultadosListener = enqueteRepository.observarEnquete(new EnqueteRepository.EnqueteListener() {
            @Override
            public void onEnqueteAtualizada(Enquete enquete) {
                atualizarUIComEnquete(enquete);
            }

            @Override
            public void onErro(Exception e) {
                if (e != null) {
                    Log.e(TAG, "Erro no listener da enquete: ", e);
                }
            }
        });
    }

    // =====================================================================
    //  Atualização da UI com os dados da enquete
    // =====================================================================

    /**
     * Atualiza a interface com os dados da enquete:
     * - Pergunta
     * - Texto dos botões de voto
     * - Contadores de votos e porcentagens
     */
    private void atualizarUIComEnquete(Enquete enquete) {
        if (enquete == null) return;

        // Textos dinâmicos
        if (enquete.getTituloEnquete() != null) {
            txtPergunta.setText(enquete.getTituloEnquete());
        }
        if (enquete.getTextoOpcaoA() != null) {
            btnVotarA.setText(enquete.getTextoOpcaoA());
        }
        if (enquete.getTextoOpcaoB() != null) {
            btnVotarB.setText(enquete.getTextoOpcaoB());
        }
        if (enquete.getTextoOpcaoC() != null) {
            btnVotarC.setText(enquete.getTextoOpcaoC());
        }

        long votosA = enquete.getOpcaoA();
        long votosB = enquete.getOpcaoB();
        long votosC = enquete.getOpcaoC();
        long total = votosA + votosB + votosC;

        long percA = (total > 0) ? (votosA * 100 / total) : 0;
        long percB = (total > 0) ? (votosB * 100 / total) : 0;
        long percC = (total > 0) ? (votosC * 100 / total) : 0;

        txtTotalA.setText("Opção A: " + votosA + " votos (" + percA + "%)");
        txtTotalB.setText("Opção B: " + votosB + " votos (" + percB + "%)");
        txtTotalC.setText("Opção C: " + votosC + " votos (" + percC + "%)");
        txtTotalGeral.setText("Total de votos: " + total);
    }

    /**
     * Carrega do Firestore qual opção o usuário já votou (se houver)
     * e atualiza o texto "Seu voto".
     */
/// FOI TOLTALMENTE ALTERADO PARA QUESTÃO01/////////////////////////////////////////////////////
    private void carregarVotoUsuario() {
        // Comentário (Requisito 13): O carregamento ocorre em onResume() e após o voto
        // para garantir que os metadados (Data/Hora, UID) estejam sempre atualizados.

        // Chamamos o novo método do repositório para obter todos os metadados
        enqueteRepository.carregarMetadadosVotoUsuario(new EnqueteRepository.VotoMetadadosCallback() {

//         onMetadadosCarregados Converte o objeto Timestamp do Firestore para o formato
//         de String definido
            @Override
            public void onMetadadosCarregados(String opcao, Timestamp timestamp, String uid) {
                // Voto existe: exibe as informações

                //12 Formata o timestamp para exibição na UI
                String dataFormatada = DATE_FORMAT.format(timestamp.toDate());

                // 4/6: Exibe as informações adicionais
                txtSeuVoto.setText("Seu voto: opção " + opcao);
                txtDataVoto.setText("Data do voto: " + dataFormatada);
                txtSeuUid.setText("Seu UID: " + uid);
            }

            @Override
            public void onNaoVotou() {
                //9 Se ainda não votou, exibe os textos padrão
                txtSeuVoto.setText("Seu voto: ainda não votou");
                txtDataVoto.setText("Data do voto: -");

                //11 Exibe o UID anônimo, mesmo sem voto
                if (auth.getCurrentUser() != null) {
                    txtSeuUid.setText("Seu UID: " + auth.getCurrentUser().getUid());
                } else {
                    txtSeuUid.setText("Seu UID: não conectado");
                }
            }

            // ATENÇÃO: Método onErro é OBRIGATÓRIO pelo callback
            @Override
            public void onErro(Exception e) {
                Log.e(TAG, "Erro ao carregar metadados do voto: ", e);
                Toast.makeText(MainActivity.this,
                        "Falha ao carregar dados do voto.",
                        Toast.LENGTH_SHORT).show();
            }
        }); // Fim do callback e do método carregarVotoUsuario()
    } // Fim do método carregarVotoUsuario()


                // =====================================================================
                //  Ações da interface (votar e resetar enquete)
                // =====================================================================

                /**
                 * Configura os listeners dos botões da tela.
                 */
        private void configurarBotoes() {
            btnVotarA.setOnClickListener(v -> registrarVoto("A"));
            btnVotarB.setOnClickListener(v -> registrarVoto("B"));
            btnVotarC.setOnClickListener(v -> registrarVoto("C"));
            btnReset.setOnClickListener(v -> mostrarDialogoReset());
        }

        /**
         * Envia a opção selecionada para o EnqueteRepository registrar o voto.
         */
        ////////QUESTÃO01//////////////////
        private void registrarVoto(String opcao) {
            enqueteRepository.registrarVoto(opcao, new EnqueteRepository.RegistrarVotoCallback() {
                ///onVotoRegistrado foi alterado para Garantir que a data/hora e o UID sejam recarregados e exibidos.
                @Override
                public void onVotoRegistrado(String opcaoRegistrada) {
                   // txtSeuVoto.setText("Seu voto: opção " + opcaoRegistrada);
                    //Chamamos carregarVotoUsuario() para recarregar o voto e exibir o novo
                    // Timestamp e UID na tela imediatamente.
                    carregarVotoUsuario();
                    /////////////FIM DA QUESTÃO01////////////
                    Toast.makeText(
                            MainActivity.this,
                            "Voto registrado.",
                            Toast.LENGTH_SHORT
                    ).show();
                }

                @Override
                public void onJaVotou(String opcaoExistente) {
                    txtSeuVoto.setText("Seu voto: opção " + opcaoExistente);
                    Toast.makeText(
                            MainActivity.this,
                            "Você já votou.",
                            Toast.LENGTH_SHORT
                    ).show();
                }

                @Override
                public void onErro(Exception e) {
                    Log.e(TAG, "Erro ao registrar voto: ", e);
                    Toast.makeText(
                            MainActivity.this,
                            "Erro ao registrar voto.",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        }

        /**
         * Exibe um diálogo pedindo o "código do professor" para autorizar o reset.
         */
        private void mostrarDialogoReset() {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Zerar votos");
            builder.setMessage("Digite o código do professor:");

            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            builder.setView(input);

            builder.setPositiveButton("Confirmar", (dialog, which) -> {
                if ("1234".equals(input.getText().toString().trim())) {
                    resetarEnquete();
                } else {
                    Toast.makeText(this, "Código incorreto.", Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
            builder.show();
        }

        /**
         * Chama o repositório para zerar contadores e votos no Firestore.
         */

        private void resetarEnquete() {
            enqueteRepository.resetarEnquete(new EnqueteRepository.OperacaoCallback() {

                @Override
                public void onSucesso() {
                    //txtSeuVoto.setText("Seu voto: ainda não votou");


                  //////QUESTÃO01/////////////////
                    //Chamamos carregarVotoUsuario() para atualizar todos os campos
                    // e garantir que o estado "ainda não votou" seja exibido.
                    carregarVotoUsuario();
   /////////////////////// FIM QUESTÃO01///////////////////////////////////
                    Toast.makeText(
                            MainActivity.this,
                            "Enquete zerada.",
                            Toast.LENGTH_SHORT
                    ).show();
                }

                @Override
                public void onErro(Exception e) {
                    Log.e(TAG, "Erro ao resetar enquete: ", e);
                    Toast.makeText(
                            MainActivity.this,
                            "Erro ao zerar enquete.",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        }
    };




