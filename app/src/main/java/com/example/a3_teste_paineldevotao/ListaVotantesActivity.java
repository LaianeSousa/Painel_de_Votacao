package com.example.a3_teste_paineldevotao;

import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.a3_teste_paineldevotao.adapter.VotantesAdapter;
import com.example.a3_teste_paineldevotao.data.FirebaseManager;
import com.example.a3_teste_paineldevotao.model.VotoModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

/*
* Esta é a Activity que vai gerenciar a tela.
* Ela tem três responsabilidades principais:
- Proteger o acesso (Senha).
- Buscar os dados no Firestore.
- Atualizar a lista visualmente.
*
* */
public class ListaVotantesActivity extends AppCompatActivity {

    // componentes da interface
    private ListView listaVotos;
    private Button btnAtualizar;
    private ProgressBar progressBar;

    // Firebase e dados
    private FirebaseManager firebaseManager;
    private ArrayList<VotoModel> listaDeVotos;
    private VotantesAdapter adapter;

    // Senha para acessar a tela.
    private final String SENHA_PROFESSOR = "admin123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_votantes);
        firebaseManager = FirebaseManager.getInstance(this);

        listaVotos = findViewById(R.id.listaViewVotantes);
        btnAtualizar = findViewById(R.id.btnAtualizar);

        listaDeVotos = new ArrayList<>();
        adapter = new VotantesAdapter(this, listaDeVotos);
        listaVotos.setAdapter(adapter);

        // -- configurar o clique do botão
        btnAtualizar.setOnClickListener(v -> {
            Toast.makeText(this, "Atualizando a lista de votos...", Toast.LENGTH_SHORT).show();
            carregarDadosDosVotos();
        });

        exibirDialogoDeSenha();
    }
    /**
     * Exibe um AlertDialog para solicitar a senha de acesso.
     * Se a senha estiver correta, chama o método para carregar os dados.
     * Se estiver incorreta ou for cancelado, fecha a Activity.
     */

    private void exibirDialogoDeSenha(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Acesso restrito");
        builder.setMessage("Digite a senha para visualizar os votos");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Confirmar", (dialog, which) -> {
            String senhaDigitada = input.getText().toString();
            if (senhaDigitada.equals(SENHA_PROFESSOR)){
                Toast.makeText(ListaVotantesActivity.this,"Acesso Liberado!", Toast.LENGTH_SHORT).show();
                carregarDadosDosVotos();
            } else {
                // Senha incorreta, exibe erro e fecha a tela
                Toast.makeText(ListaVotantesActivity.this, "Senha incorreta. Acesso negado.", Toast.LENGTH_LONG).show();
                finish(); // Fecha a Activity
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            dialog.cancel();
            finish();
        });
        builder.setCancelable(false);
        builder.show();
    }
    public void carregarDadosDosVotos() {


        // Usando a referência do EnqueteRepository/FirebaseManager para ser consistente
        firebaseManager.getEnqueteRef().collection("votos")
                .orderBy("timestamp", Query.Direction.DESCENDING) // Correção: 'orderBy' com 'O' maiúsculo e campo 'timestamp'
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // C. Sucesso
                    listaDeVotos.clear(); // Limpa a lista antes de adicionar novos itens

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // Converte o documento para seu objeto VotoModel
                        VotoModel voto = document.toObject(VotoModel.class);
                        voto.setUid(document.getId()); // Guarda o UID do votante
                        listaDeVotos.add(voto);
                    }

                    adapter.notifyDataSetChanged(); // Avise o Adapter que os dados mudaram
                })
                .addOnFailureListener(e -> {
                    // C. Falha
                    Toast.makeText(this, "Erro ao buscar dados: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }


}