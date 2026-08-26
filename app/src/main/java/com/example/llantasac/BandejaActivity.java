package com.example.llantasac;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

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

    public static String inspectorSeleccionado = "";
    private TextView textFecha, textEstadoConexion;
    private Spinner spinnerInspectoresGlobal;
    private RecyclerView recyclerBandeja;
    private UnidadAdapter adapter;
    private List<Unidad> listaUnidades;
    private String currentTab = "inspeccion"; // "inspeccion" o "taller"

    private LinearLayout tabInspeccion, tabTaller;
    private TextView textTabInspeccion, textTabTaller;
    private View indicatorInspeccion, indicatorTaller;
    private TextView textSeccionTag, textSeccionTitulo, textSeccionDesc;
    private View cardInspectorGlobal;

    private ListenerRegistration firestoreListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bandeja);

        textFecha = findViewById(R.id.textFecha);
        textEstadoConexion = findViewById(R.id.textEstadoConexion);
        cardInspectorGlobal = findViewById(R.id.cardInspectorGlobal);
        SimpleDateFormat formateador = new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", new Locale("es", "MX"));
        textFecha.setText(formateador.format(new Date()));

        spinnerInspectoresGlobal = findViewById(R.id.spinnerInspectoresGlobal);
        cargarInspectoresGlobal();

        recyclerBandeja = findViewById(R.id.recyclerBandeja);
        recyclerBandeja.setLayoutManager(new GridLayoutManager(this, 2));

        // Vincular elementos de pestañas
        tabInspeccion = findViewById(R.id.tabInspeccion);
        tabTaller = findViewById(R.id.tabTaller);
        textTabInspeccion = findViewById(R.id.textTabInspeccion);
        textTabTaller = findViewById(R.id.textTabTaller);
        indicatorInspeccion = findViewById(R.id.indicatorInspeccion);
        indicatorTaller = findViewById(R.id.indicatorTaller);
        textSeccionTag = findViewById(R.id.textSeccionTag);
        textSeccionTitulo = findViewById(R.id.textSeccionTitulo);
        textSeccionDesc = findViewById(R.id.textSeccionDesc);

        tabInspeccion.setOnClickListener(v -> switchTab("inspeccion"));
        tabTaller.setOnClickListener(v -> switchTab("taller"));

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

        cargarUnidadesDesdeFirebase();
        observarEstadoConexion();
    }

    private void observarEstadoConexion() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            cm.registerDefaultNetworkCallback(new android.net.ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull android.net.Network network) {
                    runOnUiThread(() -> {
                        textEstadoConexion.setText(R.string.estado_en_linea);
                        textEstadoConexion.setTextColor(Color.parseColor("#10B981"));
                    });
                }

                @Override
                public void onLost(@NonNull android.net.Network network) {
                    runOnUiThread(() -> {
                        textEstadoConexion.setText(R.string.estado_sin_conexion);
                        textEstadoConexion.setTextColor(Color.parseColor("#EF4444"));
                    });
                }
            });
        }
    }

    private void switchTab(String tab) {
        currentTab = tab;
        if (tab.equals("inspeccion")) {
            textTabInspeccion.setTextColor(Color.parseColor("#06B6D4"));
            textTabInspeccion.setTypeface(null, android.graphics.Typeface.BOLD);
            indicatorInspeccion.setBackgroundColor(Color.parseColor("#06B6D4"));

            textTabTaller.setTextColor(Color.parseColor("#64748B"));
            textTabTaller.setTypeface(null, android.graphics.Typeface.NORMAL);
            indicatorTaller.setBackgroundColor(Color.TRANSPARENT);

            textSeccionTag.setText(R.string.bandeja_seccion_unidades);
            textSeccionTitulo.setText(R.string.bandeja_titulo_pendientes);
            textSeccionDesc.setText(R.string.bandeja_instrucciones);
            cardInspectorGlobal.setVisibility(View.VISIBLE);
        } else {
            textTabTaller.setTextColor(Color.parseColor("#06B6D4"));
            textTabTaller.setTypeface(null, android.graphics.Typeface.BOLD);
            indicatorTaller.setBackgroundColor(Color.parseColor("#06B6D4"));

            textTabInspeccion.setTextColor(Color.parseColor("#64748B"));
            textTabInspeccion.setTypeface(null, android.graphics.Typeface.NORMAL);
            indicatorInspeccion.setBackgroundColor(Color.TRANSPARENT);

            textSeccionTag.setText(R.string.bandeja_seccion_taller);
            textSeccionTitulo.setText(R.string.bandeja_titulo_taller);
            textSeccionDesc.setText(R.string.bandeja_subtitulo_taller);
            cardInspectorGlobal.setVisibility(View.GONE);
        }
        cargarUnidadesDesdeFirebase();
    }

    private void cargarInspectoresGlobal() {
        List<String> inspectores = new ArrayList<>();
        inspectores.add(getString(R.string.bandeja_seleccionar_inspector));
        inspectores.add("GARCIA BERNAL CARLOS EDUARDO");
        inspectores.add("GARCIA GONZALEZ JORGE GERARDO");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, inspectores) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                ((TextView) v).setTextColor(Color.WHITE);
                ((TextView) v).setTextSize(12);
                return v;
            }

            @Override
            public boolean isEnabled(int position) {
                // Deshabilitar la primera opción (Seleccionar...)
                return position != 0;
            }

            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                if (position == 0) {
                    View v = new View(getContext());
                    v.setLayoutParams(new android.widget.AbsListView.LayoutParams(0, 0));
                    v.setVisibility(View.GONE);
                    return v;
                }

                // Crear un contenedor con un divisor al fondo para que se vea profesional
                android.widget.LinearLayout container = new android.widget.LinearLayout(getContext());
                container.setOrientation(android.widget.LinearLayout.VERTICAL);
                container.setBackgroundColor(Color.WHITE);

                // Obtener la vista del texto
                View v = super.getDropDownView(position, null, parent);
                if (v instanceof TextView) {
                    TextView tv = (TextView) v;
                    tv.setTextColor(Color.parseColor("#374151")); // Gris elegante
                    tv.setTextSize(15);
                    tv.setPadding(48, 40, 48, 40); // Más espacio a los lados
                    tv.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
                    container.addView(tv);
                } else {
                    container.addView(v);
                }

                // Añadir una línea divisoria muy sutil
                View divider = new View(getContext());
                divider.setBackgroundColor(Color.parseColor("#E5E7EB")); // Gris muy clarito
                android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 2);
                lp.setMargins(48, 0, 48, 0); // Que la línea no toque los bordes
                divider.setLayoutParams(lp);
                container.addView(divider);

                return container;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerInspectoresGlobal.setAdapter(adapter);

        spinnerInspectoresGlobal.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    inspectorSeleccionado = inspectores.get(position);
                } else {
                    inspectorSeleccionado = "";
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void cargarUnidadesDesdeFirebase() {
        if (firestoreListener != null) {
            firestoreListener.remove();
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        firestoreListener = db.collection("unidades").addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Log.e("FIRESTORE", "Error al leer unidades: " + error.getMessage());
                Toast.makeText(this, "Error de Firestore: " + error.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            if (snapshots != null) {
                Log.d("FIRESTORE", "Documentos recibidos: " + snapshots.size());
                listaUnidades.clear();
                for (QueryDocumentSnapshot doc : snapshots) {
                    String docId = doc.getId();
                    Boolean activo = doc.getBoolean("activo");
                    if (activo != null && !activo) {
                        Log.d("FIRESTORE_DEBUG", "Ignorada " + docId + ": Inactiva");
                        continue;
                    }

                    if (currentTab.equals("inspeccion")) {
                        String tractor = doc.getString("tractor");
                        String caja = doc.getString("caja");

                        boolean tieneTr = tractor != null && !tractor.isEmpty() && !tractor.equalsIgnoreCase("S/T") && !tractor.equalsIgnoreCase("S/N");
                        boolean tieneCj = caja != null && !caja.isEmpty() && !caja.equalsIgnoreCase("S/C") && !caja.equalsIgnoreCase("S/N");

                        boolean tractorComp = doc.getBoolean("llantas_tractor_completada") != null && doc.getBoolean("llantas_tractor_completada");
                        boolean cajaComp = doc.getBoolean("llantas_caja_completada") != null && doc.getBoolean("llantas_caja_completada");
                        
                        Log.d("FIRESTORE_DEBUG", "Unidad " + docId + " -> TrComp: " + tractorComp + ", CjComp: " + cajaComp);
                        
                        // Una unidad está completada si (no tiene tractor o ya se hizo) Y (no tiene caja o ya se hizo)
                        boolean todoTractorListo = !tieneTr || tractorComp;
                        boolean todoCajaListo = !tieneCj || cajaComp;

                        if (todoTractorListo && todoCajaListo) {
                            Log.d("FIRESTORE_DEBUG", "Ignorada " + docId + ": Todo el trabajo realizado");
                            continue;
                        }
                    } else {
                        String estadoT = doc.getString("llantas_tractor_estado");
                        String estadoC = doc.getString("llantas_caja_estado");
                        boolean conDanos = "con_danos".equals(estadoT) || "con_danos".equals(estadoC);
                        if (!conDanos) {
                            Log.d("FIRESTORE_DEBUG", "Ignorada " + docId + ": Sin daños (Taller)");
                            continue;
                        }
                    }

                    Log.d("FIRESTORE_DEBUG", "Añadida a lista: " + docId);
                    
                    int numDanos = 0;
                    Object trDanos = doc.get("llantas_tractor_danos");
                    if (trDanos != null) {
                        numDanos += contarDanosMap(trDanos);
                    }
                    Object cjDanos = doc.get("llantas_caja_danos");
                    if (cjDanos != null) {
                        numDanos += contarDanosMap(cjDanos);
                    }

                    listaUnidades.add(new Unidad(
                            doc.getId(),
                            doc.getString("tractor"),
                            doc.getString("caja"),
                            doc.getString("origen"),
                            doc.getString("destino"),
                            doc.getString("tipo_carga"),
                            doc.getString("estado"),
                            doc.getString("producto"),
                            numDanos
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
                    for (Unidad u : listaUnidades) {
                        String tracNorm = normalizarCodigo(u.tractor);
                        String cajaNorm = normalizarCodigo(u.caja);

                        JSONObject bestMatch = null;

                        for (int i = 0; i < posiciones.length(); i++) {
                            try {
                                JSONObject pos = posiciones.getJSONObject(i);
                                String nombreGps = normalizarCodigo(pos.getString("nombre"));

                                if (nombreGps.isEmpty()) continue;

                                boolean matchTracto = !tracNorm.isEmpty() && (nombreGps.contains(tracNorm) || tracNorm.contains(nombreGps));
                                boolean matchCaja = !cajaNorm.isEmpty() && (nombreGps.contains(cajaNorm) || cajaNorm.contains(nombreGps));

                                if (matchTracto || matchCaja) {
                                    if (bestMatch == null) {
                                        bestMatch = pos;
                                    } else {
                                        // Prioridad: Si el nuevo match está en patio y el anterior no, ganar patio.
                                        boolean nuevoEnPatio = pos.optBoolean("en_patio", false);
                                        boolean anteriorEnPatio = bestMatch.optBoolean("en_patio", false);
                                        if (nuevoEnPatio && !anteriorEnPatio) {
                                            bestMatch = pos;
                                        }
                                    }
                                }
                            } catch (Exception ignored) {}
                        }

                        if (bestMatch != null) {
                            try {
                                u.tieneGps = true;
                                u.enPatio = bestMatch.getBoolean("en_patio");
                                u.lugar = bestMatch.has("lugar") && !bestMatch.isNull("lugar") ? bestMatch.getString("lugar") : "";
                                
                                int etaMin = 0;
                                if (bestMatch.has("eta_real_min") && !bestMatch.isNull("eta_real_min")) {
                                    etaMin = bestMatch.getInt("eta_real_min");
                                } else if (bestMatch.has("eta_patio_min") && !bestMatch.isNull("eta_patio_min")) {
                                    etaMin = bestMatch.getInt("eta_patio_min");
                                }
                                u.etaMinutos = etaMin;
                            } catch (Exception ignored) {}
                        }
                        // Al terminar la búsqueda, marcamos como finalizado
                        u.buscandoGps = false;
                    }
                    adapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                Log.e("API_RASTREO", "Error al conectar con Railway", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    for (Unidad u : listaUnidades) u.buscandoGps = false;
                    adapter.notifyDataSetChanged();
                });
            }
        });
    }

    private int contarDanosMap(Object areaDanos) {
        if (areaDanos == null) return 0;
        int count = 0;

        if (areaDanos instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) areaDanos;
            for (Object val : list) {
                if (val instanceof java.util.Map) {
                    java.util.Map<?, ?> dano = (java.util.Map<?, ?>) val;
                    if (!"reparado".equals(dano.get("estatus"))) {
                        count++;
                    }
                }
            }
        } else if (areaDanos instanceof java.util.Map) {
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) areaDanos;
            for (Object val : map.values()) {
                if (val instanceof java.util.Map) {
                    java.util.Map<?, ?> dano = (java.util.Map<?, ?>) val;
                    // Solo contar si el estado es específicamente "dano"
                    if ("dano".equals(dano.get("status"))) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private String normalizarCodigo(String str) {
        if (str == null) return "";
        String s = str.trim().toUpperCase();
        if (s.isEmpty() || s.equals("S/N") || s.equals("S/T") || s.equals("S/C") || s.equals("?") || s.equals("N/A")) {
            return "";
        }
        // Quitar prefijos comunes estilo Web
        if (s.startsWith("AL ") || s.startsWith("AL-") || s.startsWith("AL_")) {
            s = s.substring(3);
        } else if (s.startsWith("AL") && s.length() > 2) {
            s = s.substring(2);
        }
        // Quitar todo lo que no sea letras o números
        return s.replaceAll("[^A-Z0-9]", "");
    }

    // ==========================================
    // CLASES INTERNAS (MODELO Y ADAPTADOR)
    // ==========================================
    public static class Unidad {
        String id, tractor, caja, origen, destino, tipoCarga, estadoCarga, producto;
        int numDanos;
        boolean tieneGps = false;
        boolean buscandoGps = true; // Nuevo: Estado de búsqueda
        boolean enPatio = false;
        String lugar = "";
        int etaMinutos = 0;

        public Unidad(String id, String tractor, String caja, String origen, String destino, String tipoCarga, String estadoCarga, String producto, int numDanos) {
            this.id = id; this.tractor = tractor; this.caja = caja;
            this.origen = origen; this.destino = destino;
            this.tipoCarga = tipoCarga; this.estadoCarga = estadoCarga; this.producto = producto;
            this.numDanos = numDanos;
        }
    }

    public class UnidadAdapter extends RecyclerView.Adapter<UnidadAdapter.ViewHolder> {
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
            Log.d("ADAPTER_DEBUG", "Dibujando unidad en posición " + position + ": " + unidad.id);

            // Efecto de opacidad para unidades fuera de patio (estilo web)
            if (unidad.tieneGps && !unidad.enPatio) {
                holder.itemView.setAlpha(0.7f);
            } else {
                holder.itemView.setAlpha(1.0f);
            }

            String nomTractor = (unidad.tractor != null && !unidad.tractor.isEmpty()) ? unidad.tractor : "S/N";
            String nomCaja = (unidad.caja != null && !unidad.caja.isEmpty()) ? unidad.caja : "S/N";
            holder.tvNombres.setText(nomTractor + " / " + nomCaja);

            holder.tvRuta.setText((unidad.origen != null && !unidad.origen.trim().isEmpty() ? unidad.origen : "?") + " → " + (unidad.destino != null && !unidad.destino.trim().isEmpty() ? unidad.destino : "?"));

            // 🔥 SOLUCIÓN A LOS N/A VACÍOS 🔥
                holder.tvTipoCarga.setText(unidad.tipoCarga != null && !unidad.tipoCarga.trim().isEmpty() ? unidad.tipoCarga : "N/A");
                holder.tvEstadoCarga.setText(unidad.estadoCarga != null && !unidad.estadoCarga.trim().isEmpty() ? unidad.estadoCarga : "N/A");
                
                if (currentTab.equals("taller")) {
                    holder.tvLabelProducto.setText(R.string.unidad_label_reparaciones);
                    holder.tvProducto.setText(unidad.numDanos + " " + getString(R.string.reparacion_danos_count));
                    holder.tvProducto.setTextColor(Color.parseColor("#DC2626")); // Rojo
                } else {
                    holder.tvLabelProducto.setText(R.string.unidad_label_producto);
                    holder.tvProducto.setText(unidad.producto != null && !unidad.producto.trim().isEmpty() ? unidad.producto : "N/A");
                    holder.tvProducto.setTextColor(Color.parseColor("#1F2937"));
                }

                if (unidad.tieneGps) {
                    holder.tvUbicacion.setText(unidad.lugar != null && !unidad.lugar.isEmpty() ? unidad.lugar : getString(R.string.unidad_ubicacion_desconocida));

                    if (unidad.enPatio) {
                        // Diseño "En Patio" (Verde)
                        holder.tvBadgeGps.setText(R.string.unidad_en_patio);
                        holder.tvBadgeGps.setTextColor(Color.parseColor("#15803D"));
                        holder.cardBadgeGps.setCardBackgroundColor(Color.parseColor("#DCFCE7"));

                        if (currentTab.equals("taller")) {
                            holder.btnAccion.setText(R.string.unidad_boton_reparar);
                        } else {
                            holder.btnAccion.setText(R.string.bandeja_boton_inspeccion);
                        }

                    holder.btnAccion.setTextColor(Color.WHITE);
                    holder.cardBtnAccion.setCardBackgroundColor(Color.parseColor("#F59E0B")); // Naranja
                    holder.btnAccion.setOnClickListener(v -> {
                        if (currentTab.equals("taller")) {
                            android.content.Intent intent = new android.content.Intent(BandejaActivity.this, ReparacionActivity.class);
                            intent.putExtra("unidadId", unidad.id);
                            startActivity(intent);
                        } else {
                            if (inspectorSeleccionado == null || inspectorSeleccionado.isEmpty()) {
                                Toast.makeText(BandejaActivity.this, R.string.form_error_inspector, Toast.LENGTH_LONG).show();
                                return;
                            }
                            android.content.Intent intent = new android.content.Intent(BandejaActivity.this, InspeccionSelectorActivity.class);
                            intent.putExtra("unidadId", unidad.id);
                            startActivity(intent);
                        }
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
                    holder.tvBadgeGps.setText(R.string.unidad_en_camino + etaTxt);
                    holder.tvBadgeGps.setTextColor(Color.parseColor("#0369A1"));
                    holder.cardBadgeGps.setCardBackgroundColor(Color.parseColor("#E0F2FE"));

                    holder.btnAccion.setText(R.string.unidad_fuera_patio);
                    holder.btnAccion.setTextColor(Color.parseColor("#94A3B8"));
                    holder.cardBtnAccion.setCardBackgroundColor(Color.parseColor("#F8FAFC"));
                    holder.btnAccion.setOnClickListener(null);
                }
            } else {
                // Estado de búsqueda o Sin señal
                if (unidad.buscandoGps) {
                    holder.tvBadgeGps.setText(R.string.unidad_buscando_gps);
                    holder.tvBadgeGps.setTextColor(Color.parseColor("#4B5563")); // Gris oscuro
                    holder.cardBadgeGps.setCardBackgroundColor(Color.parseColor("#F3F4F6")); // Gris claro
                } else {
                    holder.tvBadgeGps.setText(R.string.unidad_sin_senal);
                    holder.tvBadgeGps.setTextColor(Color.parseColor("#991B1B"));
                    holder.cardBadgeGps.setCardBackgroundColor(Color.parseColor("#FEE2E2"));
                }
                
                holder.tvUbicacion.setText(R.string.unidad_ubicacion_desconocida);

                if (currentTab.equals("taller")) {
                    holder.btnAccion.setText(R.string.unidad_boton_reparar);
                } else {
                    holder.btnAccion.setText(R.string.bandeja_boton_inspeccion);
                }

                holder.btnAccion.setTextColor(Color.WHITE);
                holder.cardBtnAccion.setCardBackgroundColor(Color.parseColor("#F59E0B")); // Naranja
                
                holder.btnAccion.setOnClickListener(v -> {
                    if (currentTab.equals("taller")) {
                        android.content.Intent intent = new android.content.Intent(BandejaActivity.this, ReparacionActivity.class);
                        intent.putExtra("unidadId", unidad.id);
                        startActivity(intent);
                    } else {
                        if (inspectorSeleccionado == null || inspectorSeleccionado.isEmpty()) {
                            Toast.makeText(BandejaActivity.this, R.string.form_error_inspector, Toast.LENGTH_LONG).show();
                            return;
                        }
                        android.content.Intent intent = new android.content.Intent(BandejaActivity.this, InspeccionSelectorActivity.class);
                        intent.putExtra("unidadId", unidad.id);
                        startActivity(intent);
                    }
                });
            }
            // Configurar botón eliminar
            holder.btnEliminar.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(BandejaActivity.this)
                        .setTitle("Eliminar Unidad")
                        .setMessage("¿Estás seguro de que deseas eliminar esta unidad? Esta acción no se puede deshacer.")
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            FirebaseFirestore.getInstance().collection("unidades")
                                    .document(unidad.id)
                                    .update("activo", false)
                                    .addOnSuccessListener(aVoid -> Toast.makeText(BandejaActivity.this, "Unidad eliminada", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(BandejaActivity.this, "Error al eliminar", Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return lista.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNombres, tvRuta, tvTipoCarga, tvEstadoCarga, tvProducto, tvLabelProducto;
            TextView tvBadgeGps, tvUbicacion, btnAccion;
            CardView cardBadgeGps, cardBtnAccion;
            android.widget.ImageButton btnEliminar;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvNombres = itemView.findViewById(R.id.tvNombres);
                tvRuta = itemView.findViewById(R.id.tvRuta);
                tvTipoCarga = itemView.findViewById(R.id.tvTipoCarga);
                tvEstadoCarga = itemView.findViewById(R.id.tvEstadoCarga);
                tvProducto = itemView.findViewById(R.id.tvProducto);
                tvLabelProducto = itemView.findViewById(R.id.tvLabelProducto);

                tvBadgeGps = itemView.findViewById(R.id.tvBadgeGps);
                tvUbicacion = itemView.findViewById(R.id.tvUbicacion);
                cardBadgeGps = itemView.findViewById(R.id.cardBadgeGps);

                btnAccion = itemView.findViewById(R.id.btnAccion);
                cardBtnAccion = itemView.findViewById(R.id.cardBtnAccion);
                btnEliminar = itemView.findViewById(R.id.btnEliminarUnidad);
            }
        }
    }
}
