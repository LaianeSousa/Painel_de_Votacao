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
import com.example.a3_teste_paineldevotao.HistoricoLogsActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.ParseException; // Importação necessária
import java.text.SimpleDateFormat; // Importação necessária
import java.util.Date; // Importação necessária
import java.util.Locale; // Importação necessária

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
    // Formato de data e hora que deve ser usado para parsear a string do Firestore
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    // Componentes de interface
    private TextView txtSubtitulo, txtPergunta, txtTituloResultados;


    private TextView txtTotalA;
    private TextView txtTotalB;
    private TextView txtTotalC;
    private TextView txtTotalGeral;
    private TextView txtSeuVoto;

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
        carregarVotoUsuario();
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
            // Se tiver a tela de config, descomente abaixo:
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
        // ... (views de resultados omitidas para economizar espaço, mantenha as suas) ...
        txtTituloResultados = findViewById(R.id.txtTituloResultados);
        txtTotalA = findViewById(R.id.txtTotalA);
        txtTotalB = findViewById(R.id.txtTotalB);
        txtTotalC = findViewById(R.id.txtTotalC);
        txtTotalGeral = findViewById(R.id.txtTotalGeral);
        txtSeuVoto = findViewById(R.id.txtSeuVoto);

//-------------------INICIALIZAÇÃO DOS NOVOS TEXTVIEWS ---Q2-------------------------------------
        txtModeloDispositivo = findViewById(R.id.txtModeloDispositivo);
        txtVersaoAndroid = findViewById(R.id.txtVersaoAndroid);
//---------------------------------------------------------------------------------------------
        // Q5: Inicialização dos novos TextViews de Rodapé e Encerramento.
        // O ID é txtDataEncerramento, não txtEncerramento, para evitar NullPointerException.
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
        auth.signInAnonymously().addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                configurarPosLogin();
            } else {
                Toast.makeText(MainActivity.this, "Erro ao conectar.", Toast.LENGTH_LONG).show();
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
                enqueteAtual = enquete; // Armazena a enquete atual para checagem de tempo
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
     * - Rodapé e status de encerramento
     */
    private void atualizarUIComEnquete(Enquete enquete) {
        if (enquete == null) return;

        // 1. Textos dinâmicos
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

        // 2. Contadores e Totais
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

        // 3. Q5: ATUALIZAÇÃO DO RODAPÉ E ENCERRAMENTO

        // Mensagem de Rodapé
        String rodape = enquete.getMensagemRodape();
        if (rodape != null && !rodape.trim().isEmpty()) {
            txtRodape.setText(rodape);
            txtRodape.setVisibility(View.VISIBLE);
        } else {
            txtRodape.setVisibility(View.GONE);
        }

        // Data/Hora de Encerramento e Verificação
        String encerramentoStr = enquete.getDataHoraEncerramento();
        boolean votacaoEncerrada = false;

        if (encerramentoStr != null && !encerramentoStr.trim().isEmpty()) {
            txtDataEncerramento.setText(String.format("Encerramento: %s", encerramentoStr));
            txtDataEncerramento.setVisibility(View.VISIBLE);

            try {
                Date dataEncerramento = DATE_FORMAT.parse(encerramentoStr);
                Date dataAtual = new Date();

                if (dataEncerramento != null && dataAtual.after(dataEncerramento)) {
                    votacaoEncerrada = true;
                }
            } catch (ParseException e) {
                Log.e(TAG, "Erro ao parsear data de encerramento.", e);
                txtDataEncerramento.setText(String.format("Encerramento: Formato Inválido (%s)", encerramentoStr));
            }
        } else {
            txtDataEncerramento.setVisibility(View.GONE);
        }

        // 4. Aplicar o estado de Votação (Encerrada ou Voto Próprio)
        if (votacaoEncerrada) {
            setBotoesVotoEnabled(false);
            if (votoDoUsuario == null) {
                txtSeuVoto.setText("Votação encerrada.");
            } else {
                txtSeuVoto.setText("Votação encerrada (Seu voto: opção " + votoDoUsuario + ")");
            }
        } else if (votoDoUsuario != null) {
            setBotoesVotoEnabled(false);
            txtSeuVoto.setText("Seu voto: opção " + votoDoUsuario);
        } else {
            setBotoesVotoEnabled(true);
            txtSeuVoto.setText("Seu voto: ainda não votou");
        }
    }

    /**
     * Habilita ou desabilita os botões de votação (A, B, C).
     * @param enabled TRUE para habilitar, FALSE para desabilitar.
     */
    private void setBotoesVotoEnabled(boolean enabled) {
        btnVotarA.setEnabled(enabled);
        btnVotarB.setEnabled(enabled);
        btnVotarC.setEnabled(enabled);
    }

    /**
     * Carrega do Firestore qual opção o usuário já votou (se houver)
     * e atualiza o texto "Seu voto".
     */
    //-----------------------------Q2 Alterado----------------------------------------------------------------------------------
    private void carregarVotoUsuario() {
        enqueteRepository.carregarVotoUsuario(new EnqueteRepository.VotoUsuarioCallback() {
            @Override
            public void onVotoCarregado(String opcao, String modelo, String versaoAndroid) {
                votoDoUsuario = opcao; // Armazena localmente o voto do usuário

                // Variável para exibir o traço (—) quando não houver voto ou metadado
                final String traco = "—";

                if (opcao != null) {
                    txtSeuVoto.setText("Seu voto: opção " + opcao);

                    // Exibe metadados do dispositivo se o voto foi encontrado
                    txtModeloDispositivo.setText("Modelo: " + (modelo != null ? modelo : traco));
                    txtVersaoAndroid.setText("Android: " + (versaoAndroid != null ? versaoAndroid : traco));

                    // Desabilita os botões se já votou (a menos que a votação esteja encerrada, o que será tratado em atualizarUIComEnquete)
                    setBotoesVotoEnabled(false);
                } else {
                    txtSeuVoto.setText("Seu voto: ainda não votou");

                    // Exibe metadados do dispositivo atual quando não há voto
                    txtModeloDispositivo.setText("Modelo: " + android.os.Build.MODEL);
                    txtVersaoAndroid.setText("Android: " + android.os.Build.VERSION.RELEASE);

                    // Habilita os botões se ainda não votou
                    setBotoesVotoEnabled(true);
                }

                // Garante que o estado final dos botões considere o encerramento da votação
                if (enqueteAtual != null) {
                    atualizarUIComEnquete(enqueteAtual);
                }
            }
        });
    }

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
     * Envia a opção selecionada para o EnqueteRepository registrar o voto,
     * APÓS verificar se a votação foi encerrada.
     */
    private void registrarVoto(String opcao) {
        if (enqueteAtual == null) {
            Toast.makeText(this, "Aguarde o carregamento da enquete.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Q5: VERIFICAÇÃO DE ENCERRAMENTO DA VOTAÇÃO
        String encerramentoStr = enqueteAtual.getDataHoraEncerramento();

        if (encerramentoStr != null && !encerramentoStr.trim().isEmpty()) {
            try {
                Date dataEncerramento = DATE_FORMAT.parse(encerramentoStr);
                Date dataAtual = new Date();

                if (dataEncerramento != null && dataAtual.after(dataEncerramento)) {
                    Toast.makeText(this, "Votação encerrada pelo professor.", Toast.LENGTH_LONG).show();
                    txtSeuVoto.setText("Votação encerrada.");
                    setBotoesVotoEnabled(false);
                    return; // Sai do método sem registrar o voto
                }
            } catch (ParseException e) {
                Log.e(TAG, "Erro ao parsear data de encerramento. Votando mesmo assim.", e);
            }
        }
        // FIM Q5: VERIFICAÇÃO DE ENCERRAMENTO

        enqueteRepository.registrarVoto(opcao, new EnqueteRepository.RegistrarVotoCallback() {
            @Override
            public void onVotoRegistrado(String opcaoRegistrada) {
                txtSeuVoto.setText("Seu voto: opção " + opcaoRegistrada);
                setBotoesVotoEnabled(false); // Desabilita após votar
                Toast.makeText(
                        MainActivity.this,
                        "Voto registrado.",
                        Toast.LENGTH_SHORT
                ).show();
            }

            @Override
            public void onJaVotou(String opcaoExistente) {
                votoDoUsuario = opcaoExistente; // Atualiza o voto local
                txtSeuVoto.setText("Seu voto: opção " + opcaoExistente);
                setBotoesVotoEnabled(false); // Desabilita se já votou
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
     * Exibe um diálogo pedindo a senha e o motivo para zerar a votação.
     */
    private void mostrarDialogoReset() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Zerar Votação");
        builder.setMessage("Digite a senha e o motivo do reset:");

        // Layout vertical para os campos de Senha e Motivo
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Campo de texto para a senha
        final EditText inputSenha = new EditText(this);
        inputSenha.setHint("Senha do Professor");
        inputSenha.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(inputSenha);

        // Campo de texto para o motivo
        final EditText inputMotivo = new EditText(this);
        inputMotivo.setHint("Motivo (ex: Nova turma)");
        layout.addView(inputMotivo);

        builder.setView(layout);

        // --- LÓGICA CORRIGIDA ---

        // Ação do botão "Zerar"
        builder.setPositiveButton("Zerar", (dialog, which) -> {
            String senha = inputSenha.getText().toString();
            String motivo = inputMotivo.getText().toString();

            // 1. Verifica a senha
            if (senha.equals("1234")) { // Sua senha

                // Se o motivo estiver vazio, usa um texto padrão.
                if (motivo.isEmpty()) {
                    motivo = "Sem motivo informado";
                }

                // 2. Chama o repositório para resetar a enquete, passando o motivo
                enqueteRepository.resetarEnquete(motivo, new EnqueteRepository.OperacaoCallback() {
                    @Override
                    public void onSucesso() {
                        // Limpa o voto local na tela
                        votoDoUsuario = null;

                        // Atualiza a interface
                        txtSeuVoto.setText("Seu voto: ainda não votou");
                        Toast.makeText(MainActivity.this, "Enquete zerada e log gravado!", Toast.LENGTH_SHORT).show();

                        // Garante que os botões de voto sejam reativados
                        setBotoesVotoEnabled(true);
                    }

                    @Override
                    public void onErro(Exception e) {
                        Toast.makeText(MainActivity.this, "Erro ao zerar enquete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

            } else {
                // Se a senha estiver incorreta
                Toast.makeText(MainActivity.this, "Senha incorreta!", Toast.LENGTH_SHORT).show();
            }
        });

        // Ação do botão "Cancelar"
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        // Exibe o diálogo
        builder.show();
    }

            // O método antigo 'resetarEnquete()' foi REMOVIDO daqui porque estava errado/duplicado.
        }