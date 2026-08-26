package com.example.llantasac;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InspeccionFormularioActivity extends AppCompatActivity {

    private String unidadId, tipo;
    private FirebaseFirestore db;
    private TextView tvUnidad, tvRuta, tvSeccionTitulo, tvInspectorConfirmacion;
    private LinearLayout llTiresContainer;
    private EditText etObservacionesGenerales;
    private CheckBox chkConformidad;
    private Button btnFinalizar, btnReparacion;
    private boolean tractorYaCompletado = false, cajaYaCompletada = false;
    private boolean tieneTractor = false, tieneCaja = false;

    private List<ItemChecklist> checklistData = new ArrayList<>();
    private Map<String, Map<String, Object>> responses = new HashMap<>();
    private List<EditText> allPsiInInputs = new ArrayList<>();
    private boolean isAveraging = false;

    // Para la cámara
    private String currentCaptureItemId;
    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspeccion_formulario);

        unidadId = getIntent().getStringExtra("unidadId");
        tipo = getIntent().getStringExtra("tipo"); // "tractor" o "caja"
        db = FirebaseFirestore.getInstance();

        tvUnidad = findViewById(R.id.tvFormUnidad);
        tvRuta = findViewById(R.id.tvFormRuta);
        tvSeccionTitulo = findViewById(R.id.tvFormSeccionTitulo);
        tvInspectorConfirmacion = findViewById(R.id.tvInspectorConfirmacion);
        llTiresContainer = findViewById(R.id.llTiresContainer);
        etObservacionesGenerales = findViewById(R.id.etObservacionesGenerales);
        chkConformidad = findViewById(R.id.chkConformidad);
        btnFinalizar = findViewById(R.id.btnFinalizarInspeccion);
        btnReparacion = findViewById(R.id.btnEnviarReparacion);

        if (!BandejaActivity.inspectorSeleccionado.isEmpty()) {
            tvInspectorConfirmacion.setText(BandejaActivity.inspectorSeleccionado);
        } else {
            tvInspectorConfirmacion.setText(R.string.form_inspector_no_seleccionado);
            tvInspectorConfirmacion.setTextColor(Color.RED);
        }

        findViewById(R.id.btnCerrar).setOnClickListener(v -> {
            Intent intent = new Intent(this, BandejaActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
        findViewById(R.id.btnCambiarTipo).setOnClickListener(v -> finish());

        initCameraLaunchers();
        configurarUIInicial();
        cargarChecklist();
        renderChecklist();

        btnFinalizar.setOnClickListener(v -> finalizarInspeccion(false));
        btnReparacion.setOnClickListener(v -> finalizarInspeccion(true));
    }

    private void initCameraLaunchers() {
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        if (imageBitmap != null) {
                            subirFotoAFirebase(imageBitmap);
                        }
                    }
                }
        );

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isChecked) {
                        lanzarCamara();
                    } else {
                        Toast.makeText(this, R.string.form_permiso_camara, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private boolean isChecked; // Variable auxiliar para el lambda de permisos

    private void lanzarCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureLauncher.launch(takePictureIntent);
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void subirFotoAFirebase(Bitmap bitmap) {
        Toast.makeText(this, R.string.form_cargando_evidencia, Toast.LENGTH_SHORT).show();
        
        // Comprimir y convertir a Base64 (máx 300KB como en las reglas)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] byteArray = baos.toByteArray();
        String b64 = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT);

        Map<String, Object> fotoDoc = new HashMap<>();
        fotoDoc.put("b64", b64);
        fotoDoc.put("ts", System.currentTimeMillis());

        db.collection("fotos").add(fotoDoc).addOnSuccessListener(docRef -> {
            String fotoId = docRef.getId();
            vincularFotoARespuesta(fotoId);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, R.string.form_evidencia_error, Toast.LENGTH_SHORT).show();
        });
    }

    private void vincularFotoARespuesta(String fotoId) {
        if (currentCaptureItemId == null) return;
        
        Map<String, Object> resp = responses.get(currentCaptureItemId);
        if (resp != null) {
            List<String> fotos = (List<String>) resp.get("fotos");
            if (fotos == null) fotos = new ArrayList<>();
            fotos.add(fotoId);
            resp.put("fotos", fotos);

            // Actualizar UI - Añadir miniatura
            actualizarMiniaturasUI(currentCaptureItemId, fotoId);
        }
    }

    private void actualizarMiniaturasUI(String itemId, String fotoId) {
        for (int i = 0; i < llTiresContainer.getChildCount(); i++) {
            View card = llTiresContainer.getChildAt(i);
            
            if (itemId.equals(card.getTag(R.id.tvPosicionTitulo))) {
                LinearLayout llPreview = card.findViewById(R.id.llPhotosPreview);
                View btnCamera = card.findViewById(R.id.cardBtnCamera);
                View btnAdd = card.findViewById(R.id.cardBtnAddPhoto);
                
                Map<String, Object> resp = responses.get(itemId);
                List<String> fotos = (resp != null) ? (List<String>) resp.get("fotos") : null;

                if (fotos != null && !fotos.isEmpty()) {
                    // Ocultar cámara inicial y mostrar preview y botón '+'
                    btnCamera.setVisibility(View.GONE);
                    llPreview.setVisibility(View.VISIBLE);
                    btnAdd.setVisibility(View.VISIBLE);
                    
                    llPreview.removeAllViews();
                    for (String id : fotos) {
                        View box = LayoutInflater.from(this).inflate(R.layout.layout_mini_box_check, llPreview, false);
                        llPreview.addView(box);
                    }
                } else {
                    btnCamera.setVisibility(View.VISIBLE);
                    llPreview.setVisibility(View.GONE);
                    btnAdd.setVisibility(View.GONE);
                }
                break;
            }
        }
        Toast.makeText(this, R.string.form_evidencia_exito, Toast.LENGTH_SHORT).show();
    }

    private void configurarUIInicial() {
        tvSeccionTitulo.setText(tipo.equals("tractor") ? getString(R.string.selector_tipo_tractor) : getString(R.string.selector_tipo_caja));

        db.collection("unidades").document(unidadId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String tr = doc.getString("tractor");
                String cj = doc.getString("caja");
                String origen = doc.getString("origen");
                String destino = doc.getString("destino");

                tvUnidad.setText((tr != null ? tr : "S/T") + " / " + (cj != null ? cj : "S/C"));
                tvRuta.setText((origen != null && !origen.trim().isEmpty() ? origen : "?") + " → " + (destino != null && !destino.trim().isEmpty() ? destino : "?"));

                tieneTractor = tr != null && !tr.isEmpty() && !tr.equalsIgnoreCase("S/T") && !tr.equalsIgnoreCase("S/N");
                tieneCaja = cj != null && !cj.isEmpty() && !cj.equalsIgnoreCase("S/C") && !cj.equalsIgnoreCase("S/N");
                tractorYaCompletado = doc.getBoolean("llantas_tractor_completada") != null && doc.getBoolean("llantas_tractor_completada");
                cajaYaCompletada = doc.getBoolean("llantas_caja_completada") != null && doc.getBoolean("llantas_caja_completada");
            }
        });
    }

    private void cargarInspectores() {
        // Método eliminado, se usa el global de BandejaActivity
    }

    private void cargarChecklist() {
        checklistData.clear();
        if (tipo.equals("tractor")) {
            checklistData.add(new ItemChecklist("tr_p1", true, "Direccional izquierda", "Eje 1, lado izquierdo"));
            checklistData.add(new ItemChecklist("tr_p2", true, "Direccional derecha", "Eje 1, lado derecho"));
            checklistData.add(new ItemChecklist("tr_p3", true, "Ext izquierda", "Eje 2, exterior izquierdo"));
            checklistData.add(new ItemChecklist("tr_p4", true, "Int izquierda", "Eje 2, interior izquierdo"));
            checklistData.add(new ItemChecklist("tr_p5", true, "Int derecha", "Eje 2, interior derecho"));
            checklistData.add(new ItemChecklist("tr_p6", true, "Ext derecha", "Eje 2, exterior derecho"));
            checklistData.add(new ItemChecklist("tr_p7", true, "Ext izquierda", "Eje 3, exterior izquierdo"));
            checklistData.add(new ItemChecklist("tr_p8", true, "Int izquierda", "Eje 3, interior izquierdo"));
            checklistData.add(new ItemChecklist("tr_p9", true, "Int derecha", "Eje 3, interior derecho"));
            checklistData.add(new ItemChecklist("tr_p10", true, "Ext derecha", "Eje 3, exterior derecho"));
            checklistData.add(new ItemChecklist("tr_birlos", false, "Birlos", "Completos, apretados, sin daño"));
        } else {
            checklistData.add(new ItemChecklist("cj_p1", true, "Ext izquierda", "Eje 1 caja, exterior izquierdo"));
            checklistData.add(new ItemChecklist("cj_p2", true, "Int izquierda", "Eje 1 caja, interior izquierdo"));
            checklistData.add(new ItemChecklist("cj_p3", true, "Int derecha", "Eje 1 caja, interior derecho"));
            checklistData.add(new ItemChecklist("cj_p4", true, "Ext derecha", "Eje 1 caja, exterior derecho"));
            checklistData.add(new ItemChecklist("cj_p5", true, "Ext izquierda", "Eje 2 caja, exterior izquierdo"));
            checklistData.add(new ItemChecklist("cj_p6", true, "Int izquierda", "Eje 2 caja, interior izquierdo"));
            checklistData.add(new ItemChecklist("cj_p7", true, "Int derecha", "Eje 2 caja, interior derecho"));
            checklistData.add(new ItemChecklist("cj_p8", true, "Ext derecha", "Eje 2 caja, exterior derecho"));
            checklistData.add(new ItemChecklist("cj_birlos", false, "Birlos", "Completos, apretados, sin daño"));
        }
    }

    private void renderChecklist() {
        LayoutInflater inflater = LayoutInflater.from(this);
        for (ItemChecklist item : checklistData) {
            View card = inflater.inflate(R.layout.item_inspeccion_llanta, llTiresContainer, false);
            card.setTag(R.id.tvPosicionTitulo, item.id); // Identificador único de la tarjeta
            
            TextView tvTitle = card.findViewById(R.id.tvPosicionTitulo);
            TextView tvSub = card.findViewById(R.id.tvPosicionSub);
            tvTitle.setText(item.title);
            tvSub.setText(item.subtitle);

            // Estado inicial de respuesta
            Map<String, Object> resp = new HashMap<>();
            resp.put("punto", item.title);
            responses.put(item.id, resp);

            // Lógica de botones Material
            Button btnFierro = card.findViewById(R.id.btnFierro);
            Button btnAluminio = card.findViewById(R.id.btnAluminio);
            
            EditText etPsiIn = card.findViewById(R.id.etPsiIn);
            EditText etPsiOut = card.findViewById(R.id.etPsiOut);

            if (item.isTire) {
                String defaultPsi = tipo.equals("tractor") ? "110" : "105";
                etPsiOut.setText(defaultPsi);
                resp.put("psi_out", defaultPsi);

                etPsiIn.addTextChangedListener(new android.text.TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override public void afterTextChanged(android.text.Editable s) {
                        if (isAveraging) return;
                        String val = s.toString();
                        resp.put("psi_in", val);
                        
                        // Si el usuario escribe o borra, se marca como MANUAL para que el sistema no lo toque más
                        etPsiIn.setTag(R.id.etPsiIn, "manual");
                        etPsiIn.setBackgroundResource(R.drawable.input_bg);
                        
                        recomputePsiInAverage();
                    }
                });
                allPsiInInputs.add(etPsiIn);
                etPsiIn.setTag(R.id.tvPosicionTitulo, item.id); // Guardar ID para el promedio

                etPsiOut.addTextChangedListener(new android.text.TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override public void afterTextChanged(android.text.Editable s) {
                        resp.put("psi_out", s.toString());
                    }
                });
            }
            
            if (!item.isTire) {
                card.findViewById(R.id.llPsiContainer).setVisibility(View.GONE);
                ((View)btnFierro.getParent()).setVisibility(View.GONE);
            }

            View.OnClickListener materialClick = v -> {
                btnFierro.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
                btnAluminio.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
                v.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F59E0B")));
                resp.put("material", v == btnFierro ? "fierro" : "aluminio");
            };
            btnFierro.setOnClickListener(materialClick);
            btnAluminio.setOnClickListener(materialClick);

            // Lógica de botones Estatus
            Button btnBien = card.findViewById(R.id.btnStatusBien);
            Button btnDano = card.findViewById(R.id.btnStatusDano);
            Button btnNA = card.findViewById(R.id.btnStatusNA);
            LinearLayout llDanoPanel = card.findViewById(R.id.llDanoPanel);

            View.OnClickListener statusClick = v -> {
                btnBien.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
                btnDano.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
                btnNA.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
                
                String status = "bien";
                if (v == btnBien) {
                    v.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#BBF7D0")));
                    llDanoPanel.setVisibility(View.GONE);
                } else if (v == btnDano) {
                    v.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FECACA")));
                    llDanoPanel.setVisibility(View.VISIBLE);
                    status = "dano";
                    btnReparacion.setVisibility(View.VISIBLE);
                } else {
                    v.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E2E8F0")));
                    llDanoPanel.setVisibility(View.GONE);
                    status = "na";
                }
                resp.put("status", status);
                actualizarVisibilidadBotonesFinalizar();
            };
            btnBien.setOnClickListener(statusClick);
            btnDano.setOnClickListener(statusClick);
            btnNA.setOnClickListener(statusClick);
            
            // Panel de Daño - Niveles
            Button btnLeve = card.findViewById(R.id.btnNivelLeve);
            Button btnMod = card.findViewById(R.id.btnNivelModerado);
            Button btnAlto = card.findViewById(R.id.btnNivelAlto);
            View.OnClickListener nivelClick = v -> {
                btnLeve.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
                btnMod.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
                btnAlto.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
                v.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
                resp.put("nivel", ((Button)v).getText().toString());
            };
            btnLeve.setOnClickListener(nivelClick);
            btnMod.setOnClickListener(nivelClick);
            btnAlto.setOnClickListener(nivelClick);

            // Tags (ChipGroup)
            ChipGroup cgTags = card.findViewById(R.id.cgDanoTags);
            String[] tagOptions;
            if (item.isTire) {
                tagOptions = new String[]{"Llanta lisa", "Llanta baja", "Ponchada", "Corte lateral", "Desgaste irregular", "Rin dañado", "Válvula dañada", "Recorte"};
            } else {
                tagOptions = new String[]{"Birlo faltante", "Birlo flojo"};
            }

            for (String tag : tagOptions) {
                Chip chip = new Chip(this);
                chip.setText(tag);
                chip.setCheckable(true);
                chip.setOnCheckedChangeListener((v, isChecked) -> {
                    List<String> currentTags = (List<String>) resp.get("tags");
                    if (currentTags == null) currentTags = new ArrayList<>();
                    if (isChecked) currentTags.add(tag);
                    else currentTags.remove(tag);
                    resp.put("tags", currentTags);
                });
                cgTags.addView(chip);
            }

            // Panel de Daño - Fotos
            card.findViewById(R.id.cardBtnCamera).setOnClickListener(v -> {
                currentCaptureItemId = item.id;
                isChecked = true;
                lanzarCamara();
            });
            card.findViewById(R.id.cardBtnAddPhoto).setOnClickListener(v -> {
                currentCaptureItemId = item.id;
                isChecked = true;
                lanzarCamara();
            });

            EditText etDanoNota = card.findViewById(R.id.etDanoNota);
            etDanoNota.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(android.text.Editable s) {
                    resp.put("nota", s.toString());
                }
            });

            llTiresContainer.addView(card);
        }
    }

    private void actualizarVisibilidadBotonesFinalizar() {
        boolean tieneDanos = false;
        for (Map<String, Object> r : responses.values()) {
            if ("dano".equals(r.get("status"))) {
                tieneDanos = true;
                break;
            }
        }
        btnReparacion.setVisibility(tieneDanos ? View.VISIBLE : View.GONE);
    }

    private void recomputePsiInAverage() {
        if (isAveraging) return;
        
        List<Integer> manualValues = new ArrayList<>();
        for (EditText et : allPsiInInputs) {
            String val = et.getText().toString();
            String tagManual = (String) et.getTag(R.id.etPsiIn);
            if (!val.isEmpty() && "manual".equals(tagManual)) {
                try {
                    manualValues.add(Integer.parseInt(val));
                } catch (Exception ignored) {}
            }
        }

        Log.d("AVERAGE_DEBUG", "Valores manuales encontrados: " + manualValues.size());

        if (manualValues.size() == 4) {
            int sum = 0;
            for (int v : manualValues) sum += v;
            int avg = Math.round((float) sum / manualValues.size());
            Log.d("AVERAGE_DEBUG", "Calculando nuevo promedio: " + avg);

            isAveraging = true;
            for (EditText et : allPsiInInputs) {
                String tag = (String) et.getTag(R.id.etPsiIn);
                String itemId = (String) et.getTag(R.id.tvPosicionTitulo);

                // SOLO llenamos si el campo NUNCA ha sido tocado (tag null) 
                // o si ya era un campo automático (auto).
                if (tag == null || "auto".equals(tag)) {
                    et.setText(String.valueOf(avg));
                    et.setTag(R.id.etPsiIn, "auto");
                    et.setBackgroundColor(Color.parseColor("#EFF6FF"));
                    
                    if (itemId != null && responses.containsKey(itemId)) {
                        responses.get(itemId).put("psi_in", String.valueOf(avg));
                    }
                }
            }
            isAveraging = false;
        }
    }

    private void finalizarInspeccion(boolean enviarTaller) {
        if (BandejaActivity.inspectorSeleccionado.isEmpty()) {
            Toast.makeText(this, R.string.form_error_inspector, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!chkConformidad.isChecked()) {
            Toast.makeText(this, R.string.form_error_confirmacion, Toast.LENGTH_SHORT).show();
            return;
        }

        String inspector = BandejaActivity.inspectorSeleccionado;
        String observaciones = etObservacionesGenerales.getText().toString();

        // Lógica de guardado en Firebase
        Toast.makeText(this, R.string.form_guardando, Toast.LENGTH_SHORT).show();

        String prefix = tipo.equals("tractor") ? "llantas_tractor" : "llantas_caja";
        Map<String, Object> updates = new HashMap<>();
        updates.put(prefix + "_completada", true);
        
        // FILTRAR: Solo guardar como "danos" lo que realmente tiene estatus de daño
        Map<String, Map<String, Object>> soloDanos = new HashMap<>();
        boolean tieneDanosActuales = false;
        
        for (Map.Entry<String, Map<String, Object>> entry : responses.entrySet()) {
            Map<String, Object> r = entry.getValue();
            if ("dano".equals(r.get("status"))) {
                soloDanos.put(entry.getKey(), r);
                tieneDanosActuales = true;
            }
        }
        
        updates.put(prefix + "_estado", (enviarTaller || tieneDanosActuales) ? "con_danos" : "aprobada");
        updates.put(prefix + "_fecha", System.currentTimeMillis());
        updates.put(prefix + "_inspector", inspector);
        updates.put(prefix + "_observaciones", observaciones);
        updates.put(prefix + "_danos", soloDanos); // SOLO ENVIAR LOS DAÑOS REALES

        db.collection("unidades").document(unidadId).update(updates);
        
        if (isOnline()) {
            Toast.makeText(this, R.string.form_exito, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Guardado localmente (Modo Offline). Se sincronizará al detectar señal.", Toast.LENGTH_LONG).show();
        }

        // Decidir si ir a Bandeja o volver al Selector
        boolean tractorListo = tractorYaCompletado || tipo.equals("tractor");
        boolean cajaLista = cajaYaCompletada || tipo.equals("caja");

        boolean todoTractorOk = !tieneTractor || tractorListo;
        boolean todoCajaOk = !tieneCaja || cajaLista;

        if (todoTractorOk && todoCajaOk) {
            // Ya no hay nada pendiente en esta unidad, ir directo a la bandeja
            Intent intent = new Intent(this, BandejaActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        
        finish();
    }

    private boolean isOnline() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.Network network = cm.getActiveNetwork();
        if (network == null) return false;
        android.net.NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null && (capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) || 
                                      capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR));
    }

    private static class ItemChecklist {
        String id, title, subtitle;
        boolean isTire;
        ItemChecklist(String id, boolean isTire, String title, String subtitle) {
            this.id = id; this.isTire = isTire; this.title = title; this.subtitle = subtitle;
        }
    }
}
