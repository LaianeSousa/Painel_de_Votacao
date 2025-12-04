package com.example.a3_teste_paineldevotao;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.a3_teste_paineldevotao.data.EnqueteRepository;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Tela de configuração da enquete.
 */
public class ConfigurarEnqueteActivity extends AppCompatActivity {

    private static final String TAG = "ConfigEnquete";


    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("pt", "BR"));

    // --- Componentes de UI ---
    private EditText edtTituloEnquete;
    private EditText edtOpcaoA;
    private EditText edtOpcaoB;
    private EditText edtOpcaoC;
    // ----- Campos Extras -----
    private EditText edtMensagemRodape;
    private EditText edtDataHoraEncerramento;
    // -------------------------
    private Button btnSalvarConfig;

    private EnqueteRepository enqueteRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_configurar_enquete);

        configurarToolbar();
        aplicarInsets();
        inicializarRepository();
        inicializarViews(); // Vincula todos os IDs

        // Carrega dados APÓS as views estarem prontas
        carregarConfiguracoesAtuais();
        configurarBotaoSalvar();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void configurarToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarConfig);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle("Configurar Enquete");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void aplicarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layoutConfigRoot),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                });
    }

    private void inicializarRepository() {
        enqueteRepository = new EnqueteRepository(this);
    }

    private void inicializarViews() {
        edtTituloEnquete = findViewById(R.id.edtTituloEnquete);
        edtOpcaoA = findViewById(R.id.edtOpcaoA);
        edtOpcaoB = findViewById(R.id.edtOpcaoB);
        edtOpcaoC = findViewById(R.id.edtOpcaoC);
        edtMensagemRodape = findViewById(R.id.edtMensagemRodape);
        edtDataHoraEncerramento = findViewById(R.id.edtDataHoraEncerramento);
        btnSalvarConfig = findViewById(R.id.btnSalvarConfig);
    }

    // =====================================================================
    //  Lógica de Dados
    // =====================================================================

    private void carregarConfiguracoesAtuais() {
        // Nota: Certifique-se que seu EnqueteRepository.ConfiguracaoCarregadaCallback
        // foi atualizado para receber os 6 parâmetros.
        enqueteRepository.carregarConfiguracoes(new EnqueteRepository.ConfiguracaoCarregadaCallback() {
            @Override
            public void onConfiguracaoCarregada(String titulo, String opcaoA, String opcaoB, String opcaoC, String mensagemRodape, String dataHoraEncerramento) {
                // Preenche campos básicos
                if (titulo != null) edtTituloEnquete.setText(titulo);
                if (opcaoA != null) edtOpcaoA.setText(opcaoA);
                if (opcaoB != null) edtOpcaoB.setText(opcaoB);
                if (opcaoC != null) edtOpcaoC.setText(opcaoC);

                // LÓGICA NOVA: Preenche os campos extras se existirem
                if (mensagemRodape != null) {
                    edtMensagemRodape.setText(mensagemRodape);
                }
                if (dataHoraEncerramento != null) {
                    edtDataHoraEncerramento.setText(dataHoraEncerramento);
                }
            }

            @Override
            public void onErro(Exception e) {
                Log.e(TAG, "Erro ao carregar configurações: ", e);
                Toast.makeText(ConfigurarEnqueteActivity.this, "Erro ao carregar dados.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configurarBotaoSalvar() {
        // CORREÇÃO IMPORTANTE: As Strings devem ser lidas DENTRO do listener.
        // Se ficarem fora, elas pegam o texto vazio na hora que a tela abre.

        btnSalvarConfig.setOnClickListener(v -> {
            // 1. Coleta os dados da UI no momento do clique
            String titulo = edtTituloEnquete.getText().toString().trim();
            String opcaoA = edtOpcaoA.getText().toString().trim();
            String opcaoB = edtOpcaoB.getText().toString().trim();
            String opcaoC = edtOpcaoC.getText().toString().trim();

            // Novos campos
            String mensagemRodape = edtMensagemRodape.getText().toString().trim();
            String dataHoraEncerramento = edtDataHoraEncerramento.getText().toString().trim();

            // 2. Validações
            if (!validarCampos(titulo, opcaoA, opcaoB, opcaoC, dataHoraEncerramento)) {
                return; // Se falhar, para aqui.
            }

            // 3. Envia para o repositório
            salvarDados(titulo, opcaoA, opcaoB, opcaoC, mensagemRodape, dataHoraEncerramento);
        });
    }

    private boolean validarCampos(String titulo, String opcaoA, String opcaoB, String opcaoC, String dataHoraEncerramento) {
        if (titulo.isEmpty() || opcaoA.isEmpty() || opcaoB.isEmpty() || opcaoC.isEmpty()) {
            Toast.makeText(this, "Preencha o título e as três opções.", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validação da Data (Apenas se o campo não estiver vazio)
        if (!dataHoraEncerramento.isEmpty()) {
            DATE_FORMAT.setLenient(false); // Impede datas inválidas como 30/02

            try {
                Date dataEncerramento = DATE_FORMAT.parse(dataHoraEncerramento);
                Date dataAtual = new Date();

                // Verifica se é passado
                if (dataEncerramento != null && dataEncerramento.before(dataAtual)) {
                    Toast.makeText(this, "A data de encerramento deve ser no futuro!", Toast.LENGTH_LONG).show();
                    return false;
                }
            } catch (ParseException e) {
                Toast.makeText(this, "Data inválida. Use o formato: dd/MM/yyyy HH:mm", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    private void salvarDados(String titulo, String opcaoA, String opcaoB, String opcaoC, String mensagemRodape, String dataHoraEncerramento) {
        // Certifique-se que o método salvarConfiguracoes no Repository aceita esses argumentos
        enqueteRepository.salvarConfiguracoes(
                titulo,
                opcaoA,
                opcaoB,
                opcaoC,
                mensagemRodape,
                dataHoraEncerramento,
                new EnqueteRepository.OperacaoCallback() {
                    @Override
                    public void onSucesso() {
                        Toast.makeText(ConfigurarEnqueteActivity.this, "Configurações atualizadas!", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onErro(Exception e) {
                        Log.e(TAG, "Erro ao salvar: ", e);
                        Toast.makeText(ConfigurarEnqueteActivity.this, "Erro ao salvar alterações.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
}