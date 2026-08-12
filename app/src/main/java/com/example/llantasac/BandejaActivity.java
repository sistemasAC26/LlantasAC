package com.example.llantasac;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class BandejaActivity extends AppCompatActivity {

    private TextView textFecha;
    private RecyclerView recyclerBandeja;
    private UnidadAdapter adapter;
    private List<Unidad> listaUnidades;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bandeja);

        textFecha = findViewById(R.id.textFecha);
        SimpleDateFormat formateador = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", new Locale("es", "MX"));
        textFecha.setText(formateador.format(new Date()));

        recyclerBandeja = findViewById(R.id.recyclerBandeja);
        recyclerBandeja.setLayoutManager(new GridLayoutManager(this, 2));

        listaUnidades = new ArrayList<>();
        adapter = new UnidadAdapter(listaUnidades);
        recyclerBandeja.setAdapter(adapter);

        TextView btnLista = findViewById(R.id.btnLista);
        TextView btnCuadricula = findViewById(R.id.btnCuadricula);

        btnLista.setOnClickListener(v -> {
            recyclerBandeja.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
            btnLista.setBackgroundColor(Color.WHITE);
            btnLista.setTextColor(Color.parseColor("#1F2937"));
            btnCuadricula.setBackgroundResource(android.R.color.transparent);
            btnCuadricula.setTextColor(Color.parseColor("#6B7280"));
        });

        btnCuadricula.setOnClickListener(v -> {
            recyclerBandeja.setLayoutManager(new GridLayoutManager(this, 2));
            btnCuadricula.setBackgroundColor(Color.WHITE);
            btnCuadricula.setTextColor(Color.parseColor("#1F2937"));
            btnLista.setBackgroundResource(android.R.color.transparent);
            btnLista.setTextColor(Color.parseColor("#6B7280"));
        });

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("unidades").addSnapshotListener((snapshots, error) -> {
            if (error != null) return;
            if (snapshots != null) {
                listaUnidades.clear();
                for (QueryDocumentSnapshot doc : snapshots) {

                    Boolean activo = doc.getBoolean("activo");
                    if (activo != null && !activo) continue;

                    Boolean completada = doc.getBoolean("llantas_tractor_completada");
                    if (completada != null && completada) continue;

                    listaUnidades.add(new Unidad(
                            doc.getId(),
                            doc.getString("tractor"),
                            doc.getString("caja"),
                            doc.getString("origen"),
                            doc.getString("destino"),
                            doc.getString("tipo_carga"),
                            doc.getString("estado"),
                            doc.getString("producto")
                    ));
                }
                adapter.notifyDataSetChanged();
                consultarApiRastreoRailway();
            }
        });
    }

    private void consultarApiRastreoRailway() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL("https://web-production-99ffd.up.railway.app/api/posiciones");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) result.append(line);
                reader.close();

                JSONObject json = new JSONObject(result.toString());
                JSONArray posiciones = json.getJSONArray("unidades");

                new Handler(Looper.getMainLooper()).post(() -> {
                    for (int i = 0; i < posiciones.length(); i++) {
                        try {
                            JSONObject pos = posiciones.getJSONObject(i);
                            String nombreGps = pos.getString("nombre").toUpperCase().trim();
                            boolean enPatio = pos.getBoolean("en_patio");

                            // Extraer lugar y tiempo estimado
                            String lugar = pos.has("lugar") && !pos.isNull("lugar") ? pos.getString("lugar") : "";
                            int etaMin = 0;
                            if (pos.has("eta_real_min") && !pos.isNull("eta_real_min")) {
                                etaMin = pos.getInt("eta_real_min");
                            } else if (pos.has("eta_patio_min") && !pos.isNull("eta_patio_min")) {
                                etaMin = pos.getInt("eta_patio_min");
                            }

                            for (Unidad u : listaUnidades) {
                                String trac = u.tractor != null ? u.tractor.toUpperCase() : "S/N";
                                String caj = u.caja != null ? u.caja.toUpperCase() : "S/N";

                                if (nombreGps.contains(trac) || nombreGps.contains(caj) || trac.contains(nombreGps)) {
                                    u.tieneGps = true;
                                    u.enPatio = enPatio;
                                    u.lugar = lugar;
                                    u.etaMinutos = etaMin;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    adapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                Log.e("API_RASTREO", "Error al conectar con Railway", e);
            }
        });
    }

    // ==========================================
    // CLASES INTERNAS (MODELO Y ADAPTADOR)
    // ==========================================
    public static class Unidad {
        String id, tractor, caja, origen, destino, tipoCarga, estadoCarga, producto;
        boolean tieneGps = false;
        boolean enPatio = false;
        String lugar = "";
        int etaMinutos = 0;

        public Unidad(String id, String tractor, String caja, String origen, String destino, String tipoCarga, String estadoCarga, String producto) {
            this.id = id; this.tractor = tractor; this.caja = caja;
            this.origen = origen; this.destino = destino;
            this.tipoCarga = tipoCarga; this.estadoCarga = estadoCarga; this.producto = producto;
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

            holder.tvRuta.setText((unidad.origen != null ? unidad.origen : "?") + " -> " + (unidad.destino != null ? unidad.destino : "?"));

            // 🔥 SOLUCIÓN A LOS N/A VACÍOS 🔥
            holder.tvTipoCarga.setText(unidad.tipoCarga != null && !unidad.tipoCarga.trim().isEmpty() ? unidad.tipoCarga : "N/A");
            holder.tvEstadoCarga.setText(unidad.estadoCarga != null && !unidad.estadoCarga.trim().isEmpty() ? unidad.estadoCarga : "N/A");
            holder.tvProducto.setText(unidad.producto != null && !unidad.producto.trim().isEmpty() ? unidad.producto : "N/A");

            if (unidad.tieneGps) {
                holder.tvUbicacion.setText(unidad.lugar != null && !unidad.lugar.isEmpty() ? unidad.lugar : "Ubicación desconocida");

                if (unidad.enPatio) {
                    // Diseño "En Patio" (Verde)
                    holder.tvBadgeGps.setText("En Patio");
                    holder.tvBadgeGps.setTextColor(Color.parseColor("#15803D"));
                    holder.cardBadgeGps.setCardBackgroundColor(Color.parseColor("#DCFCE7"));

                    holder.btnAccion.setText("Iniciar Inspección de Llantas");
                    holder.btnAccion.setTextColor(Color.WHITE);
                    holder.cardBtnAccion.setCardBackgroundColor(Color.parseColor("#F59E0B")); // Naranja
                    holder.btnAccion.setOnClickListener(v -> {
                        // AQUÍ LUEGO ABRIREMOS LA PANTALLA
                    });
                } else {
                    // Diseño "En Camino" (Azul)
                    String etaTxt = "";
                    if (unidad.etaMinutos > 0) {
                        if (unidad.etaMinutos > 60) {
                            etaTxt = " (~" + (unidad.etaMinutos / 60) + "h " + (unidad.etaMinutos % 60) + "m)";
                        } else {
                            etaTxt = " (~" + unidad.etaMinutos + " min)";
                        }
                    }
                    holder.tvBadgeGps.setText("En Camino" + etaTxt);
                    holder.tvBadgeGps.setTextColor(Color.parseColor("#0369A1"));
                    holder.cardBadgeGps.setCardBackgroundColor(Color.parseColor("#E0F2FE"));

                    holder.btnAccion.setText("Fuera de Patio (Inactivo)");
                    holder.btnAccion.setTextColor(Color.parseColor("#94A3B8"));
                    holder.cardBtnAccion.setCardBackgroundColor(Color.parseColor("#F8FAFC"));
                    holder.btnAccion.setOnClickListener(null);
                }
            } else {
                // Sin señal GPS
                holder.tvBadgeGps.setText("Sin Señal");
                holder.tvBadgeGps.setTextColor(Color.parseColor("#991B1B"));
                holder.cardBadgeGps.setCardBackgroundColor(Color.parseColor("#FEE2E2"));
                holder.tvUbicacion.setText("-");

                holder.btnAccion.setText("Cargando...");
                holder.btnAccion.setTextColor(Color.parseColor("#94A3B8"));
                holder.cardBtnAccion.setCardBackgroundColor(Color.parseColor("#F1F5F9"));
            }
        }

        @Override
        public int getItemCount() {
            return lista.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNombres, tvRuta, tvTipoCarga, tvEstadoCarga, tvProducto;
            TextView tvBadgeGps, tvUbicacion, btnAccion;
            CardView cardBadgeGps, cardBtnAccion;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvNombres = itemView.findViewById(R.id.tvNombres);
                tvRuta = itemView.findViewById(R.id.tvRuta);
                tvTipoCarga = itemView.findViewById(R.id.tvTipoCarga);
                tvEstadoCarga = itemView.findViewById(R.id.tvEstadoCarga);
                tvProducto = itemView.findViewById(R.id.tvProducto);

                tvBadgeGps = itemView.findViewById(R.id.tvBadgeGps);
                tvUbicacion = itemView.findViewById(R.id.tvUbicacion);
                cardBadgeGps = itemView.findViewById(R.id.cardBadgeGps);

                btnAccion = itemView.findViewById(R.id.btnAccion);
                cardBtnAccion = itemView.findViewById(R.id.cardBtnAccion);
            }
        }
    }
}