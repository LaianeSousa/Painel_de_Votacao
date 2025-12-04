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
 * <p>
 * Responsabilidades principais:
 * - Exibir os campos de edição (título, opções, rodapé e data de encerramento).
 * - Carregar a configuração atual da enquete a partir do Firestore.
 * - Validar os campos digitados pelo usuário (incluindo data no futuro).
 * - Salvar as novas configurações usando o EnqueteRepository.
 * <p>
 * Toda a parte de acesso ao Firestore fica concentrada no EnqueteRepository,
 * mantendo esta Activity focada apenas em lógica de tela (UI).
 */
public class ConfigurarEnqueteActivity extends AppCompatActivity {

    private static final String TAG = "ConfigEnquete";

    // Formato de data/hora para validação e exibição
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    // --- Componentes de UI ---
    private EditText edtTituloEnquete;
    private EditText edtOpcaoA;
    private EditText edtOpcaoB;
    private EditText edtOpcaoC;
    private EditText edtMensagemRodape; // CORRIGIDO: Nome da variável
    private EditText edtDataHoraEncerramento;
    private Button btnSalvarConfig;

    // Repositório para centralizar a lógica de dados
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
        onBackPressed(); // Volta para a tela anterior
        return true;
    }

    // =====================================================================
    //  Configuração de UI (Toolbar e Insets)
    // =====================================================================

    private void configurarToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarConfig);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle("Configurar enquete");
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

    // =====================================================================
    //  Inicializações
    // =====================================================================

    private void inicializarRepository() {
        enqueteRepository = new EnqueteRepository(this);
    }

    /**
     * Associa as variáveis aos componentes de UI do layout XML.
     *
     * IMPORTANTE: Os IDs aqui (ex: R.id.edtMensagemRodape) devem corresponder
     * exatamente aos IDs definidos no seu arquivo R.layout.activity_configurar_enquete.
     */
    private void inicializarViews() {
        edtTituloEnquete = findViewById(R.id.edtTituloEnquete);
        edtOpcaoA = findViewById(R.id.edtOpcaoA);
        edtOpcaoB = findViewById(R.id.edtOpcaoB);
        edtOpcaoC = findViewById(R.id.edtOpcaoC);
        edtMensagemRodape = findViewById(R.id.edtMensagemRodape); // CORRIGIDO: ID e variável
        edtDataHoraEncerramento = findViewById(R.id.edtDataHoraEncerramento); // CORRIGIDO: ID consistente
        btnSalvarConfig = findViewById(R.id.btnSalvarConfig);
    }

    // =====================================================================
    //  Lógica de Dados
    // =====================================================================

    /**
     * Busca os dados atuais da enquete no Firestore e preenche os EditTexts.
     */
    private void carregarConfiguracoesAtuais() {
        enqueteRepository.carregarConfiguracoes(new EnqueteRepository.ConfiguracaoCarregadaCallback() {
            @Override
            public void onConfiguracaoCarregada(String titulo, String opcaoA, String opcaoB, String opcaoC, String mensagemRodape, String dataHoraEncerramento) {
                if (titulo != null) edtTituloEnquete.setText(titulo);
                if (opcaoA != null) edtOpcaoA.setText(opcaoA);
                if (opcaoB != null) edtOpcaoB.setText(opcaoB);
                if (opcaoC != null) edtOpcaoC.setText(opcaoC);
                
                // --- Q5: Carrega campos de rodapé e encerramento ---
                if (mensagemRodape != null) {
                    edtMensagemRodape.setText(mensagemRodape); // CORRIGIDO: Uso da variável correta
                }
                if (dataHoraEncerramento != null) {
                    edtDataHoraEncerramento.setText(dataHoraEncerramento);
                }
            }

            @Override
            public void onErro(Exception e) {
                Log.e(TAG, "Erro ao carregar configurações atuais: ", e);
                Toast.makeText(ConfigurarEnqueteActivity.this, "Erro ao carregar configurações.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Configura o listener do botão Salvar.
     */
    private void configurarBotaoSalvar() {
        btnSalvarConfig.setOnClickListener(v -> {
            // 1. Coleta os dados da UI
            String titulo = edtTituloEnquete.getText().toString().trim();
            String opcaoA = edtOpcaoA.getText().toString().trim();
            String opcaoB = edtOpcaoB.getText().toString().trim();
            String opcaoC = edtOpcaoC.getText().toString().trim();
            String mensagemRodape = edtMensagemRodape.getText().toString().trim(); // CORRIGIDO: Uso da variável correta
            String dataHoraEncerramento = edtDataHoraEncerramento.getText().toString().trim();

            // 2. Validações
            if (!validarCampos(titulo, opcaoA, opcaoB, opcaoC, dataHoraEncerramento)) {
                return; // Se a validação falhar, interrompe
            }

            // 3. Envia para o repositório salvar
            salvarDados(titulo, opcaoA, opcaoB, opcaoC, mensagemRodape, dataHoraEncerramento);
        });
    }

    /**
     * Valida os campos obrigatórios e o formato da data.
     *
     * @return true se todos os campos forem válidos, false caso contrário.
     */
    private boolean validarCampos(String titulo, String opcaoA, String opcaoB, String opcaoC, String dataHoraEncerramento) {
        if (titulo.isEmpty() || opcaoA.isEmpty() || opcaoB.isEmpty() || opcaoC.isEmpty()) {
            Toast.makeText(this, "Preencha o título e as três opções.", Toast.LENGTH_SHORT).show();
            return false;
        }

        // A validação da data só ocorre se o campo não estiver vazio
        if (!dataHoraEncerramento.isEmpty()) {
            try {
                Date dataEncerramento = DATE_FORMAT.parse(dataHoraEncerramento);
                Date dataAtual = new Date();

                // Verifica se a data de encerramento é anterior à data atual
                if (dataEncerramento != null && dataEncerramento.before(dataAtual)) {
                    Toast.makeText(this, "A data/hora de encerramento não pode ser no passado.", Toast.LENGTH_LONG).show();
                    return false;
                }
            } catch (ParseException e) {
                Toast.makeText(this, "Formato de data/hora inválido. Use AAAA-MM-DD HH:MM", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    /**
     * Chama o EnqueteRepository para salvar os dados no Firestore.
     */
    private void salvarDados(String titulo, String opcaoA, String opcaoB, String opcaoC, String mensagemRodape, String dataHoraEncerramento) {
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
                        Toast.makeText(ConfigurarEnqueteActivity.this, "Configurações salvas com sucesso.", Toast.LENGTH_SHORT).show();
                        finish(); // Fecha a tela e volta para a MainActivity
                    }

                    @Override
                    public void onErro(Exception e) {
                        Log.e(TAG, "Erro ao salvar configurações: ", e);
                        Toast.makeText(ConfigurarEnqueteActivity.this, "Erro ao salvar.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
}