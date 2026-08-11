package com.example.llantasac;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BandejaActivity extends AppCompatActivity {

    private TextView textFecha;
    private RecyclerView recyclerBandeja;
    private UnidadAdapter adapter;
    private List<Unidad> listaUnidades;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bandeja);

        // 1. Configurar la Fecha
        textFecha = findViewById(R.id.textFecha);
        SimpleDateFormat formateador = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", new Locale("es", "MX"));
        textFecha.setText(formateador.format(new Date()));

        // 2. Configurar la Cuadrícula (RecyclerView) por defecto
        recyclerBandeja = findViewById(R.id.recyclerBandeja);
        recyclerBandeja.setLayoutManager(new GridLayoutManager(this, 2));

        listaUnidades = new ArrayList<>();
        adapter = new UnidadAdapter(listaUnidades);
        recyclerBandeja.setAdapter(adapter);

        // 3. Lógica de los botones Lista / Cuadrícula
        TextView btnLista = findViewById(R.id.btnLista);
        TextView btnCuadricula = findViewById(R.id.btnCuadricula);

        btnLista.setOnClickListener(v -> {
            recyclerBandeja.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
            btnLista.setBackgroundColor(android.graphics.Color.WHITE);
            btnLista.setTextColor(android.graphics.Color.parseColor("#1F2937"));
            btnCuadricula.setBackgroundResource(android.R.color.transparent);
            btnCuadricula.setTextColor(android.graphics.Color.parseColor("#6B7280"));
        });

        btnCuadricula.setOnClickListener(v -> {
            recyclerBandeja.setLayoutManager(new GridLayoutManager(this, 2));
            btnCuadricula.setBackgroundColor(android.graphics.Color.WHITE);
            btnCuadricula.setTextColor(android.graphics.Color.parseColor("#1F2937"));
            btnLista.setBackgroundResource(android.R.color.transparent);
            btnLista.setTextColor(android.graphics.Color.parseColor("#6B7280"));
        });

        // 4. Conectar a Firestore y descargar las unidades
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("unidades").addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Toast.makeText(BandejaActivity.this, "Bloqueo Firebase: " + error.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("Firebase", "Error al leer unidades", error);
                return;
            }
            if (snapshots != null) {
                listaUnidades.clear();
                for (QueryDocumentSnapshot doc : snapshots) {
                    Unidad u = new Unidad(
                            doc.getId(),
                            doc.getString("tractor"),
                            doc.getString("caja"),
                            doc.getString("llantas_tractor_estado")
                    );
                    listaUnidades.add(u);
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    // =======================================================================
    // CLASES INTERNAS (MODELO Y ADAPTADOR) PARA MANEJAR LA LISTA
    // =======================================================================

    public static class Unidad {
        String id, tractor, caja, estadoLlantas;
        public Unidad(String id, String tractor, String caja, String estadoLlantas) {
            this.id = id; this.tractor = tractor; this.caja = caja; this.estadoLlantas = estadoLlantas;
        }
    }

    public static class UnidadAdapter extends RecyclerView.Adapter<UnidadAdapter.ViewHolder> {
        private List<Unidad> lista;

        public UnidadAdapter(List<Unidad> lista) {
            this.lista = lista;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_unidad_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Unidad unidad = lista.get(position);

            String nomTractor = (unidad.tractor != null && !unidad.tractor.isEmpty()) ? unidad.tractor : "S/N";
            String nomCaja = (unidad.caja != null && !unidad.caja.isEmpty()) ? unidad.caja : "S/N";
            holder.tvNombres.setText(nomTractor + " / " + nomCaja);

            String estado = (unidad.estadoLlantas != null && !unidad.estadoLlantas.isEmpty()) ? unidad.estadoLlantas : "Pendiente";
            holder.tvEstadoLlantas.setText(estado.replace("_", " ").toUpperCase());

        }

        @Override
        public int getItemCount() {
            return lista.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNombres, tvEstadoLlantas, tvRuta;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvNombres = itemView.findViewById(R.id.tvNombres);
                tvEstadoLlantas = itemView.findViewById(R.id.tvEstadoLlantas);
                tvRuta = itemView.findViewById(R.id.tvRuta);
            }
        }
    }
}