package com.example.a3_teste_paineldevotao;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
import com.example.a3_teste_paineldevotao.HistoricoLogsActivity; // Import da nova Activity
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PainelVotacao";

    // Componentes de interface
    private TextView txtSubtitulo, txtPergunta, txtTituloResultados;
    private TextView txtTotalA, txtTotalB, txtTotalC, txtTotalGeral, txtSeuVoto;
    private Button btnVotarA, btnVotarB, btnVotarC, btnReset;

    // Firebase / Repositório
    private FirebaseManager firebaseManager;
    private FirebaseAuth auth;
    private EnqueteRepository enqueteRepository;
    private ListenerRegistration resultadosListener;

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
        if (resultadosListener != null) {
            resultadosListener.remove();
            resultadosListener = null;
        }
    }

    private void configurarToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Painel de Votação");
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
    //  MENU (CORRIGIDO)
    // =====================================================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

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
    //  Inicialização
    // =====================================================================

    private void inicializarFirebase() {
        firebaseManager = FirebaseManager.getInstance(this);
        auth = firebaseManager.getAuth();
        enqueteRepository = new EnqueteRepository(this);
    }

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
            } else {
                Toast.makeText(MainActivity.this, "Erro ao conectar.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void configurarPosLogin() {
        enqueteRepository.inicializarSeNecessario();
        configurarListenerResultados();
        carregarVotoUsuario();
    }

    private void configurarListenerResultados() {
        resultadosListener = enqueteRepository.observarEnquete(new EnqueteRepository.EnqueteListener() {
            @Override
            public void onEnqueteAtualizada(Enquete enquete) {
                atualizarUIComEnquete(enquete);
            }

            @Override
            public void onErro(Exception e) {
                // Log erro
            }
        });
    }

    private void atualizarUIComEnquete(Enquete enquete) {
        if (enquete == null) return;

        // Atualiza textos
        if (enquete.getTituloEnquete() != null) txtPergunta.setText(enquete.getTituloEnquete());
        if (enquete.getTextoOpcaoA() != null) btnVotarA.setText(enquete.getTextoOpcaoA());
        if (enquete.getTextoOpcaoB() != null) btnVotarB.setText(enquete.getTextoOpcaoB());
        if (enquete.getTextoOpcaoC() != null) btnVotarC.setText(enquete.getTextoOpcaoC());

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

    private void carregarVotoUsuario() {
        enqueteRepository.carregarVotoUsuario(opcao -> {
            if (opcao != null) txtSeuVoto.setText("Seu voto: opção " + opcao);
            else txtSeuVoto.setText("Seu voto: ainda não votou");
        });
    }

    // =====================================================================
    //  Ações
    // =====================================================================

    private void configurarBotoes() {
        btnVotarA.setOnClickListener(v -> registrarVoto("A"));
        btnVotarB.setOnClickListener(v -> registrarVoto("B"));
        btnVotarC.setOnClickListener(v -> registrarVoto("C"));
        btnReset.setOnClickListener(v -> mostrarDialogoReset());
    }

    private void registrarVoto(String opcao) {
        enqueteRepository.registrarVoto(opcao, new EnqueteRepository.RegistrarVotoCallback() {
            @Override
            public void onVotoRegistrado(String opcaoRegistrada) {
                txtSeuVoto.setText("Seu voto: opção " + opcaoRegistrada);
                Toast.makeText(MainActivity.this, "Voto registrado.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onJaVotou(String opcaoExistente) {
                txtSeuVoto.setText("Seu voto: opção " + opcaoExistente);
                Toast.makeText(MainActivity.this, "Você já votou.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onErro(Exception e) {
                Toast.makeText(MainActivity.this, "Erro ao registrar voto.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Exibe um diálogo pedindo a senha e o motivo.
     */
    private void mostrarDialogoReset() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Zerar Votação");
        builder.setMessage("Digite a senha e o motivo do reset:");

        // Layout vertical para Senha e Motivo
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

            if (senha.equals("1234")) { // Sua senha
                if (motivo.isEmpty()) {
                    motivo = "Sem motivo informado";
                }

                // CHAMA O REPOSITÓRIO PASSANDO O MOTIVO
                enqueteRepository.resetarEnquete(motivo, new EnqueteRepository.OperacaoCallback() {
                    @Override
                    public void onSucesso() {
                        txtSeuVoto.setText("Seu voto: ainda não votou");
                        Toast.makeText(getApplicationContext(), "Enquete zerada e log gravado!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onErro(Exception e) {
                        Toast.makeText(getApplicationContext(), "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(getApplicationContext(), "Senha incorreta!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // O método antigo 'resetarEnquete()' foi REMOVIDO daqui porque estava errado/duplicado.
}