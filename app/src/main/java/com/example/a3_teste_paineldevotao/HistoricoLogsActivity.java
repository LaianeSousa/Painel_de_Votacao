package com.example.a3_teste_paineldevotao; // Ajuste seu pacote

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a3_teste_paineldevotao.data.EnqueteRepository;
import com.google.firebase.Timestamp;

import java.util.List;
import java.util.Map;

public class HistoricoLogsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EnqueteRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historico_logs);

        // Configurar Toolbar/Título se necessário
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Logs de Auditoria");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Botão voltar
        }

        recyclerView = findViewById(R.id.recyclerViewLogs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        repository = new EnqueteRepository(this);

        carregarDados();
    }

    // Em HistoricoLogsActivity.java
    private void carregarDados() {
        repository.carregarHistoricoLogs(new EnqueteRepository.HistoricoLogsCallback() { // <--- Correto (com 'H' maiúsculo)
            @Override
            public void onHistoricoCarregado(List<Map<String, Object>> logs) {
                LogsAdapter adapter = new LogsAdapter(logs);
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onErro(Exception e) {
                Toast.makeText(HistoricoLogsActivity.this, "Erro ao carregar logs", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // Ao clicar no botão voltar da Toolbar
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // =================================================================
    // ADAPTER INTERNO (Para facilitar)
    // =================================================================
    private class LogsAdapter extends RecyclerView.Adapter<LogsAdapter.LogViewHolder> {

        private final List<Map<String, Object>> listaLogs;

        public LogsAdapter(List<Map<String, Object>> listaLogs) {
            this.listaLogs = listaLogs;
        }

        @NonNull
        @Override
        public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_log, parent, false);
            return new LogViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
            Map<String, Object> log = listaLogs.get(position);

            // 1. Pegar Observação
            String motivo = (String) log.get("observacao");
            if (motivo == null) motivo = "Sem descrição";
            holder.tvMotivo.setText(motivo);

            // 2. Pegar e Formatar Data
            // O Firestore retorna um objeto Timestamp, precisamos converter
            Object timestampObj = log.get("timestamp");
            if (timestampObj instanceof Timestamp) {
                Timestamp ts = (Timestamp) timestampObj;
                // Formata para "dd/MM/yyyy HH:mm" usando classe utilitária do Android
                String dataFormatada = DateFormat.format("dd/MM/yyyy HH:mm", ts.toDate()).toString();
                holder.tvData.setText(dataFormatada);
            } else {
                holder.tvData.setText("Data desconhecida");
            }
        }

        @Override
        public int getItemCount() {
            return listaLogs.size();
        }

        class LogViewHolder extends RecyclerView.ViewHolder {
            TextView tvData, tvMotivo;

            public LogViewHolder(@NonNull View itemView) {
                super(itemView);
                tvData = itemView.findViewById(R.id.tvDataLog);
                tvMotivo = itemView.findViewById(R.id.tvMotivoLog);
            }
        }
    }
}