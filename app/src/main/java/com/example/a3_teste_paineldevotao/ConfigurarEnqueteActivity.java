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

/**
 * Tela de configuração da enquete.
 * <p>
 * Responsabilidades principais:
 * - Exibir os campos de edição (título e textos das opções A, B, C).
 * - Carregar a configuração atual da enquete a partir do Firestore.
 * - Validar os campos digitados pelo usuário.
 * - Salvar as novas configurações usando o EnqueteRepository.
 * <p>
 * Toda a parte de acesso ao Firestore fica concentrada no EnqueteRepository,
 * mantendo esta Activity focada apenas em lógica de tela (UI).
 */
public class ConfigurarEnqueteActivity extends AppCompatActivity {

    private static final String TAG = "ConfigEnquete";

    // Campos de entrada
    private EditText edtTituloEnquete;
    private EditText edtOpcaoA;
    private EditText edtOpcaoB;
    private EditText edtOpcaoC;
    private Button btnSalvarConfig;

    // Repositório centraliza toda a lógica de Firestore
    private EnqueteRepository enqueteRepository;

    // =====================================================================
    //  Ciclo de vida
    // =====================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_configurar_enquete);

        configurarToolbar();
        aplicarInsets();
        inicializarRepository();
        inicializarViews();
        carregarConfiguracoesAtuais();
        configurarBotaoSalvar();
    }

    /**
     * Configura o comportamento do botão "voltar" da Toolbar.
     */
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // =====================================================================
    //  Configuração de UI (Toolbar e Insets)
    // =====================================================================

    /**
     * Configura a Toolbar como ActionBar da tela.
     */
    private void configurarToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarConfig);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle("Configurar enquete");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Em algumas versões, setTitleCentered pode não existir; protegemos com try/catch
        try {
            toolbar.setTitleCentered(false);
        } catch (Exception ignored) {
        }
    }

    /**
     * Ajusta os paddings para considerar as barras de sistema (status bar, nav bar)
     * utilizando o modo EdgeToEdge.
     */
    private void aplicarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layoutConfigRoot),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                });
    }

    // =====================================================================
    //  Inicializações principais
    // =====================================================================

    /**
     * Instancia o repositório responsável por lidar com a enquete no Firestore.
     */
    private void inicializarRepository() {
        enqueteRepository = new EnqueteRepository(this);
    }

    /**
     * Faz o findViewById de todos os componentes da tela.
     */
    private void inicializarViews() {
        edtTituloEnquete = findViewById(R.id.edtTituloEnquete);
        edtOpcaoA = findViewById(R.id.edtOpcaoA);
        edtOpcaoB = findViewById(R.id.edtOpcaoB);
        edtOpcaoC = findViewById(R.id.edtOpcaoC);
        btnSalvarConfig = findViewById(R.id.btnSalvarConfig);
    }

    // =====================================================================
    //  Carregamento das configurações atuais
    // =====================================================================

    /**
     * Busca no Firestore os textos atuais da enquete (título e opções) e
     * preenche os campos da tela.
     */
    private void carregarConfiguracoesAtuais() {
        enqueteRepository.carregarConfiguracoes(new EnqueteRepository.ConfiguracaoCarregadaCallback() {
            @Override
            public void onConfiguracaoCarregada(String titulo,
                                                String opcaoA,
                                                String opcaoB,
                                                String opcaoC) {

                if (titulo != null) {
                    edtTituloEnquete.setText(titulo);
                }
                if (opcaoA != null) {
                    edtOpcaoA.setText(opcaoA);
                }
                if (opcaoB != null) {
                    edtOpcaoB.setText(opcaoB);
                }
                if (opcaoC != null) {
                    edtOpcaoC.setText(opcaoC);
                }
            }

            @Override
            public void onErro(Exception e) {
                Log.e(TAG, "Erro ao carregar configurações atuais: ", e);
                Toast.makeText(
                        ConfigurarEnqueteActivity.this,
                        "Erro ao carregar configurações.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    // =====================================================================
    //  Lógica do botão "Salvar"
    // =====================================================================

    /**
     * Define o comportamento do botão de salvar:
     * - Lê os valores digitados
     * - Faz validações básicas
     * - Envia para o EnqueteRepository salvar no Firestore
     */
    private void configurarBotaoSalvar() {
        btnSalvarConfig.setOnClickListener(v -> {
            String titulo = edtTituloEnquete.getText().toString().trim();
            String opcaoA = edtOpcaoA.getText().toString().trim();
            String opcaoB = edtOpcaoB.getText().toString().trim();
            String opcaoC = edtOpcaoC.getText().toString().trim();

            // Validações simples para evitar salvar dados incompletos
            if (titulo.isEmpty()) {
                Toast.makeText(
                        this,
                        "Informe a pergunta da enquete.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (opcaoA.isEmpty() || opcaoB.isEmpty() || opcaoC.isEmpty()) {
                Toast.makeText(
                        this,
                        "Preencha as três opções (A, B e C).",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            // Chama o repositório para salvar no Firestore
            enqueteRepository.salvarConfiguracoes(
                    titulo,
                    opcaoA,
                    opcaoB,
                    opcaoC,
                    new EnqueteRepository.OperacaoCallback() {
                        @Override
                        public void onSucesso() {
                            Toast.makeText(
                                    ConfigurarEnqueteActivity.this,
                                    "Configurações salvas com sucesso.",
                                    Toast.LENGTH_SHORT
                            ).show();
                            // Volta para a tela de votação
                            finish();
                        }

                        @Override
                        public void onErro(Exception e) {
                            Log.e(TAG, "Erro ao salvar configurações: ", e);
                            Toast.makeText(
                                    ConfigurarEnqueteActivity.this,
                                    "Erro ao salvar configurações.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );
        });
    }
}