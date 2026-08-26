package com.example.llantasac;

import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.app.Dialog;
import android.widget.ImageView;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReparacionActivity extends AppCompatActivity {

    private String unidadId;
    private FirebaseFirestore db;
    private List<String> listaMecanicos = new ArrayList<>();

    private TextView tvUnidadTitle, tvRutaSub;
    private TextView tvCardUnidad, tvCardRuta, tvCardTipoCarga, tvCardEstado;
    private Spinner spinnerMecanicos;
    private TextView tvMecanicoAsignado;
    private LinearLayout llDanosContainer;
    private Button btnAccionPrincipal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reparacion);

        unidadId = getIntent().getStringExtra("unidadId");
        db = FirebaseFirestore.getInstance();

        // Vincular vistas
        tvUnidadTitle = findViewById(R.id.tvUnidadTitle);
        tvRutaSub = findViewById(R.id.tvRutaSub);
        tvCardUnidad = findViewById(R.id.tvCardUnidad);
        tvCardRuta = findViewById(R.id.tvCardRuta);
        tvCardTipoCarga = findViewById(R.id.tvCardTipoCarga);
        tvCardEstado = findViewById(R.id.tvCardEstado);
        spinnerMecanicos = findViewById(R.id.spinnerMecanicos);
        tvMecanicoAsignado = findViewById(R.id.tvMecanicoAsignado);
        llDanosContainer = findViewById(R.id.llDanosContainer);
        btnAccionPrincipal = findViewById(R.id.btnAccionPrincipal);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        cargarDatosUnidad();
        cargarMecanicos();
    }

    private void cargarDatosUnidad() {
        db.collection("unidades").document(unidadId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String tractor = doc.getString("tractor");
                String caja = doc.getString("caja");
                String origen = doc.getString("origen");
                String destino = doc.getString("destino");
                
                String nomUnidad = (caja != null && !caja.trim().isEmpty() ? caja : "S/C") + " / " + (tractor != null && !tractor.trim().isEmpty() ? tractor : "S/T");
                String nomRuta = (origen != null && !origen.trim().isEmpty() ? origen : "?") + " → " + (destino != null && !destino.trim().isEmpty() ? destino : "?");

                tvUnidadTitle.setText(nomUnidad);
                tvRutaSub.setText(nomRuta);

                tvCardUnidad.setText(nomUnidad);
                tvCardRuta.setText(nomRuta);
                
                String tipoCarga = doc.getString("tipo_carga");
                tvCardTipoCarga.setText(tipoCarga != null && !tipoCarga.trim().isEmpty() ? tipoCarga : "N/A");
                
                String estado = doc.getString("estado");
                tvCardEstado.setText(estado != null && !estado.trim().isEmpty() ? estado : "N/A");

                // Verificar si ya hay mecánico
                String mecTr = doc.getString("mecanico_llantas_tractor");
                String mecCj = doc.getString("mecanico_llantas_caja");
                String mecanicoActual = (mecTr != null) ? mecTr : mecCj;

                if (mecanicoActual != null && !mecanicoActual.isEmpty()) {
                    spinnerMecanicos.setVisibility(View.GONE);
                    tvMecanicoAsignado.setVisibility(View.VISIBLE);
                    tvMecanicoAsignado.setText(mecanicoActual);
                    btnAccionPrincipal.setText("Finalizar Reparación");
                    btnAccionPrincipal.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981")));
                }

                renderListaDanos(doc.get("llantas_tractor_danos"), getString(R.string.reparacion_tractor_label));
                renderListaDanos(doc.get("llantas_caja_danos"), getString(R.string.reparacion_caja_label));
            }
        });
    }

    private void renderListaDanos(Object areaDanos, String tituloSeccion) {
        if (areaDanos == null) return;

        List<Map<String, Object>> listaDanos = new ArrayList<>();
        if (areaDanos instanceof List) {
            for (Object obj : (List<?>) areaDanos) {
                if (obj instanceof Map) listaDanos.add((Map<String, Object>) obj);
            }
        } else if (areaDanos instanceof Map) {
            for (Object obj : ((Map<?, ?>) areaDanos).values()) {
                if (obj instanceof Map) listaDanos.add((Map<String, Object>) obj);
            }
        }

        if (listaDanos.isEmpty()) return;

        // Título de Sección
        TextView tvSeccion = new TextView(this);
        tvSeccion.setText(tituloSeccion);
        tvSeccion.setTextSize(15);
        tvSeccion.setTypeface(null, android.graphics.Typeface.BOLD);
        tvSeccion.setTextColor(Color.parseColor("#0F172A"));
        int paddingVertical = (int) (16 * getResources().getDisplayMetrics().density);
        tvSeccion.setPadding(0, paddingVertical, 0, paddingVertical / 2);
        llDanosContainer.addView(tvSeccion);

        // Ocultar el label estático si estamos inyectando dinámicos
        findViewById(R.id.tvLabelDanosTractor).setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Map<String, Object> dano : listaDanos) {
            View item = inflater.inflate(R.layout.item_dano_reparar, llDanosContainer, false);
            
            CheckBox chk = item.findViewById(R.id.chkDano);
            TextView tvPunto = item.findViewById(R.id.tvDanoPunto);
            TextView tvTags = item.findViewById(R.id.tvDanoTags);
            TextView tvNota = item.findViewById(R.id.tvDanoNota);
            TextView tvStatus = item.findViewById(R.id.tvStatusBadge);
            Button btnFoto = item.findViewById(R.id.btnVerFoto);

            String punto = (String) dano.get("punto");
            String nivel = (String) dano.get("nivel");
            String estatus = (String) dano.get("estatus");
            String nota = (String) dano.get("nota");
            List<String> tags = (List<String>) dano.get("tags");

            tvPunto.setText(punto + (nivel != null ? " (" + nivel + ")" : ""));
            tvTags.setText("Etiquetas: " + (tags != null ? tags.toString().replace("[", "").replace("]", "") : "Sin etiquetas"));
            if (nota != null && !nota.isEmpty()) {
                tvNota.setVisibility(View.VISIBLE);
                tvNota.setText("Nota: " + nota);
            } else {
                tvNota.setVisibility(View.GONE);
            }

            // Lógica para el botón de Foto
            List<String> danoFotos = (List<String>) dano.get("fotos");
            if (danoFotos != null && !danoFotos.isEmpty()) {
                btnFoto.setVisibility(View.VISIBLE);
                btnFoto.setOnClickListener(v -> mostrarVisorImagen(danoFotos.get(0)));
            } else {
                btnFoto.setVisibility(View.GONE);
            }

            if ("reparado".equals(estatus)) {
                tvStatus.setText(R.string.reparacion_reparado);
                tvStatus.setBackgroundResource(R.drawable.badge_bg_green);
                tvStatus.setTextColor(Color.parseColor("#10B981"));
                chk.setChecked(true);
                chk.setEnabled(false);
            } else {
                tvStatus.setText(R.string.reparacion_pendiente);
                tvStatus.setBackgroundResource(R.drawable.badge_bg_red);
                tvStatus.setTextColor(Color.parseColor("#EF4444"));
                chk.setChecked(false);
            }

            llDanosContainer.addView(item);
        }
    }

    private void cargarMecanicos() {
        listaMecanicos.clear();
        listaMecanicos.add("Seleccionar mecánico..."); // Texto estilo Web
        listaMecanicos.add("GARCIA GONZALEZ JORGE GERARDO");
        listaMecanicos.add("GARCIA BERNAL CARLOS EDUARDO");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listaMecanicos) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(Color.parseColor("#475569"));
                    ((TextView) v).setTextSize(14);
                }
                return v;
            }

            @Override
            public boolean isEnabled(int position) {
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
                android.widget.LinearLayout container = new android.widget.LinearLayout(getContext());
                container.setOrientation(android.widget.LinearLayout.VERTICAL);
                container.setBackgroundColor(Color.WHITE);

                View v = super.getDropDownView(position, null, parent);
                if (v instanceof TextView) {
                    TextView tv = (TextView) v;
                    tv.setTextColor(Color.parseColor("#374151"));
                    tv.setTextSize(15);
                    tv.setPadding(48, 40, 48, 40);
                    tv.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
                    container.addView(tv);
                } else {
                    container.addView(v);
                }

                View divider = new View(getContext());
                divider.setBackgroundColor(Color.parseColor("#E5E7EB"));
                android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 2);
                lp.setMargins(48, 0, 48, 0);
                divider.setLayoutParams(lp);
                container.addView(divider);

                return container;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMecanicos.setAdapter(adapter);

        spinnerMecanicos.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                // Aquí se puede guardar el mecánico seleccionado si es necesario
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void mostrarVisorImagen(String fotoId) {
        if (fotoId == null || fotoId.isEmpty()) return;

        Toast.makeText(this, "Cargando imagen...", Toast.LENGTH_SHORT).show();

        db.collection("fotos").document(fotoId).get().addOnSuccessListener(doc -> {
            if (doc.exists() && doc.contains("b64")) {
                String b64Data = doc.getString("b64");
                if (b64Data != null) {
                    if (b64Data.contains(",")) {
                        b64Data = b64Data.split(",")[1];
                    }

                    try {
                        byte[] decodedString = Base64.decode(b64Data, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                        dialog.setContentView(R.layout.layout_visor_foto);

                        ImageView iv = dialog.findViewById(R.id.ivVisor);
                        iv.setImageBitmap(bitmap);

                        dialog.findViewById(R.id.btnCerrarVisor).setOnClickListener(v -> dialog.dismiss());
                        dialog.show();
                    } catch (Exception e) {
                        Log.e("REPARACION", "Error al decodificar imagen", e);
                        Toast.makeText(this, "Error al mostrar la imagen", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(this, "No se encontró la foto en el servidor", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error al conectar con el servidor", Toast.LENGTH_SHORT).show();
        });
    }
}
