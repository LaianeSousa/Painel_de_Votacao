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
// Certifique-se de que esta Activity existe no seu projeto ou remova o import se não usar
import com.example.a3_teste_paineldevotao.HistoricoLogsActivity;

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
 * - Permitir ao usuário votar (uma única vez) com verificação de encerramento.
 * - Exibir qual foi o voto do usuário e metadados do dispositivo (Q2).
 * - Permitir reset da enquete com senha e motivo.
 * - Navegação para histórico e configurações.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PainelVotacao";

    // Formato de data usado para verificar o encerramento (yyyy-MM-dd HH:mm)
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    // --- Componentes de Interface ---
    private TextView txtSubtitulo;
    private TextView txtPergunta;
    private TextView txtTituloResultados;

    // Totais
    private TextView txtTotalA;
    private TextView txtTotalB;
    private TextView txtTotalC;
    private TextView txtTotalGeral;

    // Status do usuário
    private TextView txtSeuVoto;

    // Botões
    private Button btnVotarA;
    private Button btnVotarB;
    private Button btnVotarC;
    private Button btnReset;

    // --- Q1: Campos Legados (Data/UID) ---
    // Mantidos para compatibilidade caso o layout exija, mas a lógica principal foca na Q2
    private TextView txtDataVoto;
    private TextView txtSeuUid;


    // --- Q2: Novos Campos (Dispositivo) ---
    private TextView txtModeloDispositivo;
    private TextView txtVersaoAndroid;

    // --- Q5: Rodapé e Encerramento ---
    private TextView txtRodape;
    private TextView txtDataEncerramento;

    // --- Firebase / Repositório ---
    private FirebaseManager firebaseManager;
    private FirebaseAuth auth;
    private EnqueteRepository enqueteRepository;
    private ListenerRegistration resultadosListener;

    // Estado
    private Enquete enqueteAtual; // Armazena o último estado da enquete
    private String votoDoUsuario = null; // Armazena o voto do usuário localmente

    // =====================================================================
    //  Ciclo de Vida
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

        // Recarrega a enquete ao voltar para a tela
        enqueteRepository.carregarEnquete(new EnqueteRepository.EnqueteCarregadaCallback() {
            @Override
            public void onEnqueteCarregada(Enquete enquete) {
                enqueteAtual = enquete;
                atualizarUIComEnquete(enquete);
            }

            @Override
            public void onErro(Exception e) {
                Log.e(TAG, "Erro ao carregar enquete (onResume): ", e);
            }
        });

        // Carrega o voto do usuário (incluindo metadados Q2)
        carregarVotoUsuario();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove listener em tempo real para evitar memory leaks
        if (resultadosListener != null) {
            resultadosListener.remove();
            resultadosListener = null;
        }
    }

    // =====================================================================
    //  Configurações de UI (Toolbar, Insets, Menu)
    // =====================================================================

    private void configurarToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle("Painel de Votação");
        }
        try {
            toolbar.setTitleCentered(false);
        } catch (Exception ignored) { }
    }

    private void aplicarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layoutMain),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.menu_configurar_enquete) {
            startActivity(new Intent(this, ConfigurarEnqueteActivity.class));
            return true;

        } else if (itemId == R.id.menu_lista_votantes) {
            startActivity(new Intent(this, ListaVotantesActivity.class));
            return true;

        } else if (itemId == R.id.action_historico) {
            // Ação corrigida: Abre o histórico de logs
            Intent intent = new Intent(this, HistoricoLogsActivity.class);
            startActivity(intent);
            return true;

        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    // =====================================================================
    //  Inicialização
    // =====================================================================

    private void inicializarFirebase() {
        firebaseManager = FirebaseManager.getInstance(this);
        auth = firebaseManager.getAuth();
        enqueteRepository = new EnqueteRepository(this);
    }

    private void inicializarViews() {
        // Textos Gerais
        txtSubtitulo = findViewById(R.id.txtSubtitulo);
        txtPergunta = findViewById(R.id.txtPergunta);
        txtTituloResultados = findViewById(R.id.txtTituloResultados);

        // Contadores
        txtTotalA = findViewById(R.id.txtTotalA);
        txtTotalB = findViewById(R.id.txtTotalB);
        txtTotalC = findViewById(R.id.txtTotalC);
        txtTotalGeral = findViewById(R.id.txtTotalGeral);

        // Status Voto
        txtSeuVoto = findViewById(R.id.txtSeuVoto);

        // Q1 (Legado - tenta encontrar no layout, se não existir, ignora)
        txtDataVoto = findViewById(R.id.txtDataVoto);
        txtSeuUid = findViewById(R.id.txtSeuUid);

        // Q2: Metadados do Dispositivo
        txtModeloDispositivo = findViewById(R.id.txtModeloDispositivo);
        txtVersaoAndroid = findViewById(R.id.txtVersaoAndroid);

        // Q5: Rodapé e Encerramento
        txtRodape = findViewById(R.id.txtRodape);
        txtDataEncerramento = findViewById(R.id.txtEncerramento);

        // Botões
        btnVotarA = findViewById(R.id.btnVotarA);
        btnVotarB = findViewById(R.id.btnVotarB);
        btnVotarC = findViewById(R.id.btnVotarC);
        btnReset = findViewById(R.id.btnReset);
    }

    private void fazerLoginAnonimo() {
        if (auth.getCurrentUser() != null) {
            configurarPosLogin();
            return;
        }
        auth.signInAnonymously().addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                configurarPosLogin();
                Toast.makeText(MainActivity.this, "Conectado (Anônimo)", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Erro ao conectar.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void configurarPosLogin() {
        // Garante documento inicial
        enqueteRepository.inicializarSeNecessario();
        // Listener em tempo real
        configurarListenerResultados();
        // Carrega voto do usuário
        carregarVotoUsuario();

    }

    private void configurarListenerResultados() {
        resultadosListener = enqueteRepository.observarEnquete(new EnqueteRepository.EnqueteListener() {
            @Override
            public void onEnqueteAtualizada(Enquete enquete) {
                enqueteAtual = enquete;
                atualizarUIComEnquete(enquete);
            }

            @Override
            public void onErro(Exception e) {
                if (e != null) Log.e(TAG, "Erro no listener da enquete: ", e);
            }
        });
    }

    private void configurarBotoes() {
        btnVotarA.setOnClickListener(v -> registrarVoto("A"));
        btnVotarB.setOnClickListener(v -> registrarVoto("B"));
        btnVotarC.setOnClickListener(v -> registrarVoto("C"));
        // Chama o diálogo com senha e motivo (Versão atualizada)
        btnReset.setOnClickListener(v -> mostrarDialogoReset());
    }

    // =====================================================================
    //  Lógica de UI e Votação
    // =====================================================================

    private void atualizarUIComEnquete(Enquete enquete) {
        if (enquete == null) return;

        // 1. Textos da Pergunta e Botões
        if (enquete.getTituloEnquete() != null) txtPergunta.setText(enquete.getTituloEnquete());
        if (enquete.getSubTitulo() != null && txtSubtitulo != null) txtSubtitulo.setText(enquete.getSubTitulo());

        if (enquete.getTextoOpcaoA() != null) btnVotarA.setText(enquete.getTextoOpcaoA());
        if (enquete.getTextoOpcaoB() != null) btnVotarB.setText(enquete.getTextoOpcaoB());
        if (enquete.getTextoOpcaoC() != null) btnVotarC.setText(enquete.getTextoOpcaoC());

        // 2. Cálculos e Exibição de Totais
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

        // 3. Rodapé e Encerramento (Q5)
        String rodape = enquete.getMensagemRodape();
        if (rodape != null && !rodape.trim().isEmpty()) {
            txtRodape.setText(rodape);
            txtRodape.setVisibility(View.VISIBLE);
        } else {
            txtRodape.setVisibility(View.GONE);
        }

        // Verificação de Data de Encerramento
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
                txtDataEncerramento.setText("Encerramento: Data Inválida");
            }
        } else {
            txtDataEncerramento.setVisibility(View.GONE);
        }

        // 4. Controle dos Botões (Encerrado ou Já Votou)
        if (votacaoEncerrada) {
            setBotoesVotoEnabled(false);
            if (votoDoUsuario == null) {
                txtSeuVoto.setText("Votação encerrada.");
            } else {
                txtSeuVoto.setText("Votação encerrada (Seu voto: " + votoDoUsuario + ")");
            }
        } else if (votoDoUsuario != null) {
            setBotoesVotoEnabled(false);
            txtSeuVoto.setText("Seu voto: opção " + votoDoUsuario);
        } else {
            setBotoesVotoEnabled(true);
            txtSeuVoto.setText("Seu voto: ainda não votou");
        }
    }

    private void setBotoesVotoEnabled(boolean enabled) {
        btnVotarA.setEnabled(enabled);
        btnVotarB.setEnabled(enabled);
        btnVotarC.setEnabled(enabled);
        float alpha = enabled ? 1.0f : 0.5f;
        btnVotarA.setAlpha(alpha);
        btnVotarB.setAlpha(alpha);
        btnVotarC.setAlpha(alpha);
    }

    /**
     * Carrega voto e preenche os metadados do dispositivo (Q2).
     */
    private void carregarVotoUsuario() {
        // Caso sua interface VotoUsuarioCallback exija o método de erro:
// @Override public void onErro(Exception e) { ... }
        enqueteRepository.carregarVotoUsuario((opcao, modelo, versaoAndroid) -> {
            votoDoUsuario = opcao;
            final String traco = "—";
            onMetadadosCarregados();

            if (opcao != null) {
                txtSeuVoto.setText("Seu voto: opção " + opcao);

                // Q2: Exibe metadados salvos
                if(txtModeloDispositivo != null)
                    txtModeloDispositivo.setText("Modelo: " + (modelo != null ? modelo : traco));
                if(txtVersaoAndroid != null)
                    txtVersaoAndroid.setText("Android: " + (versaoAndroid != null ? versaoAndroid : traco));

                // Se já votou, bloqueia botões (a validação de encerramento pode sobrescrever isso depois)
                setBotoesVotoEnabled(false);
            } else {
                txtSeuVoto.setText("Seu voto: ainda não votou");

                // Q2: Exibe metadados atuais do dispositivo
                if(txtModeloDispositivo != null)
                    txtModeloDispositivo.setText("Modelo: " + android.os.Build.MODEL);
                if(txtVersaoAndroid != null)
                    txtVersaoAndroid.setText("Android: " + android.os.Build.VERSION.RELEASE);

                setBotoesVotoEnabled(true);
            }

            // Atualiza UI geral para checar se a votação encerrou
            if (enqueteAtual != null) {
                atualizarUIComEnquete(enqueteAtual);
            }


        });
    }
    public void onMetadadosCarregados(){
        enqueteRepository.carregarMetadadosVotoUsuario(new EnqueteRepository.VotoMetadadosCallback() {
            @Override
            public void onMetadadosCarregados(String opcaoVoto, Timestamp timestamp, String uid) {
                String dataFormatada = DATE_FORMAT.format(timestamp.toDate());
                txtSeuVoto.setText("Seu voto: opção " + opcaoVoto);
                txtDataVoto.setText("Data: " + dataFormatada);
                txtSeuUid.setText("ID: " + uid);
            }

            @Override
            public void onNaoVotou() {
                txtSeuVoto.setText("Seu voto: ainda não votou");
                txtDataVoto.setText("Data: -");

                // exibe o UID anônimo mesmo sem voto
                if (auth.getCurrentUser() != null){
                    txtSeuUid.setText("Seu UID: " + auth.getCurrentUser().getUid());
                } else{
                    txtSeuUid.setText("Seu UID: não conectado");
                }

            }


            @Override
            public void onErro(Exception e) {
                Log.e(TAG, "Erro ao carregar metadados do voto: ", e);

            }

        });
    }

    /**
     * Registra voto com validação de encerramento (Q5).
     */
    private void registrarVoto(String opcao) {
        if (enqueteAtual == null) {
            Toast.makeText(this, "Aguarde o carregamento...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Q5: Verifica se está encerrada antes de enviar
        String encerramentoStr = enqueteAtual.getDataHoraEncerramento();
        if (encerramentoStr != null && !encerramentoStr.trim().isEmpty()) {
            try {
                Date dataEncerramento = DATE_FORMAT.parse(encerramentoStr);
                Date dataAtual = new Date();
                if (dataEncerramento != null && dataAtual.after(dataEncerramento)) {
                    Toast.makeText(this, "Votação encerrada pelo professor.", Toast.LENGTH_LONG).show();
                    txtSeuVoto.setText("Votação encerrada.");
                    setBotoesVotoEnabled(false);
                    return;
                }
            } catch (ParseException e) {
                Log.e(TAG, "Erro data encerramento, prosseguindo com voto.", e);
            }
        }

        enqueteRepository.registrarVoto(opcao, new EnqueteRepository.RegistrarVotoCallback() {
            @Override
            public void onVotoRegistrado(String opcaoRegistrada) {
                votoDoUsuario = opcaoRegistrada;
                txtSeuVoto.setText("Seu voto: opção " + opcaoRegistrada);
                setBotoesVotoEnabled(false);
                Toast.makeText(MainActivity.this, "Voto registrado!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onJaVotou(String opcaoExistente) {
                votoDoUsuario = opcaoExistente;
                txtSeuVoto.setText("Seu voto: opção " + opcaoExistente);
                setBotoesVotoEnabled(false);
                Toast.makeText(MainActivity.this, "Você já votou.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onErro(Exception e) {
                Log.e(TAG, "Erro ao votar: ", e);
                Toast.makeText(MainActivity.this, "Erro ao registrar voto.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Diálogo de Reset atualizado com Senha e Motivo.
     */
    private void mostrarDialogoReset() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Zerar Votação");
        builder.setMessage("Digite a senha e o motivo do reset:");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText inputSenha = new EditText(this);
        inputSenha.setHint("Senha do Professor");
        inputSenha.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(inputSenha);

        final EditText inputMotivo = new EditText(this);
        inputMotivo.setHint("Motivo (ex: Nova turma)");
        layout.addView(inputMotivo);

        builder.setView(layout);

        builder.setPositiveButton("Zerar", (dialog, which) -> {
            String senha = inputSenha.getText().toString();
            String motivo = inputMotivo.getText().toString();

            if (senha.equals("1234")) { // Senha hardcoded conforme requisito
                if (motivo.isEmpty()) motivo = "Sem motivo informado";

                enqueteRepository.resetarEnquete(motivo, new EnqueteRepository.OperacaoCallback() {
                    @Override
                    public void onSucesso() {
                        votoDoUsuario = null;
                        txtSeuVoto.setText("Seu voto: ainda não votou");
                        Toast.makeText(MainActivity.this, "Enquete zerada e log gravado!", Toast.LENGTH_SHORT).show();
                        setBotoesVotoEnabled(true);
                    }

                    @Override
                    public void onErro(Exception e) {
                        Toast.makeText(MainActivity.this, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(MainActivity.this, "Senha incorreta!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}