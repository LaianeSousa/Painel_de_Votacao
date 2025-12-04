package com.example.a3_teste_paineldevotao.adapter;

/*
* Adapter é onde a lógica visual acontece.
*  Ele pega os dados "frios" do banco e os transforma em informação útil para o usuário.
* */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.a3_teste_paineldevotao.R;
import com.example.a3_teste_paineldevotao.model.VotoModel;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class VotantesAdapter extends BaseAdapter {
    private Context context;
    private List<VotoModel> listaVotos;

    // construtor
    public VotantesAdapter(Context context, List<VotoModel> listaVotos){
        this.context = context;
        this.listaVotos = listaVotos;
    }
    // 1- Mostra quantas  linhas vai ter na lista
    @Override
    public int getCount(){
        return listaVotos.size();
    }

    @Override
    public Object getItem(int posicicao){
        return listaVotos.get(posicicao);
    }
    @Override
    public long getItemId(int posicao){
        return posicao;
    }

    @Override
    public View getView(int posicao, View convertView, ViewGroup parent){
        if (convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.item_voto, parent, false);
        }
        
        VotoModel votoAtual = listaVotos.get(posicao);
        TextView txtNome = convertView.findViewById(R.id.textViewNome);
        TextView txtData = convertView.findViewById(R.id.textViewData);
        TextView txtOpcao = convertView.findViewById(R.id.textViewVoto);
        
        // posicao do votante
        int numVotante = posicao+1;
        txtNome.setText("Votante "+numVotante);
        
        // Opção escolhida
        txtOpcao.setText("Voto: " + votoAtual.getOpcaoEscolhida());

        // Formatar a data
        if (votoAtual.getTimestamp() != null){
            SimpleDateFormat formatoDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("pt", "BR"));
            String dataFormatada = formatoDate.format(votoAtual.getTimestamp());
            txtData.setText(dataFormatada);
        }
        else {
            txtData.setText("--/--/----");
        }
        return convertView;
        
    }


}
