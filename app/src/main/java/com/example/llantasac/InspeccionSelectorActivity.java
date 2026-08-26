package com.example.llantasac;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.firestore.FirebaseFirestore;

public class InspeccionSelectorActivity extends AppCompatActivity {

    private String unidadId;
    private FirebaseFirestore db;
    private TextView tvUnidadTitle, tvStatusTractor, tvStatusCaja;
    private CardView cardTractor, cardCaja;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspeccion_selector);

        unidadId = getIntent().getStringExtra("unidadId");
        db = FirebaseFirestore.getInstance();

        tvUnidadTitle = findViewById(R.id.tvSelectorUnidad);
        tvStatusTractor = findViewById(R.id.tvStatusTractor);
        tvStatusCaja = findViewById(R.id.tvStatusCaja);
        cardTractor = findViewById(R.id.cardTipoTractor);
        cardCaja = findViewById(R.id.cardTipoCaja);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refrescar la información cada vez que volvemos a esta pantalla
        cargarInfoUnidad();
    }

    private void cargarInfoUnidad() {
        db.collection("unidades").document(unidadId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String tractor = doc.getString("tractor");
                String caja = doc.getString("caja");
                tvUnidadTitle.setText((caja != null ? caja : "S/C") + " / " + (tractor != null ? tractor : "S/T"));

                boolean trComp = doc.getBoolean("llantas_tractor_completada") != null && doc.getBoolean("llantas_tractor_completada");
                boolean cjComp = doc.getBoolean("llantas_caja_completada") != null && doc.getBoolean("llantas_caja_completada");

                boolean tieneTr = tractor != null && !tractor.isEmpty() && !tractor.equalsIgnoreCase("S/T") && !tractor.equalsIgnoreCase("S/N");
                boolean tieneCj = caja != null && !caja.isEmpty() && !caja.equalsIgnoreCase("S/C") && !caja.equalsIgnoreCase("S/N");

                // Configurar Tractor
                if (!tieneTr) {
                    configurarDeshabilitado(cardTractor, tvStatusTractor, "(Sin Tractor - N/A)");
                } else if (trComp) {
                    configurarDeshabilitado(cardTractor, tvStatusTractor, "(Ya completada)");
                } else {
                    cardTractor.setOnClickListener(v -> abrirFormulario("tractor"));
                }

                // Configurar Caja
                if (!tieneCj) {
                    configurarDeshabilitado(cardCaja, tvStatusCaja, "(Sin Caja - N/A)");
                } else if (cjComp) {
                    configurarDeshabilitado(cardCaja, tvStatusCaja, "(Ya completada)");
                } else {
                    cardCaja.setOnClickListener(v -> abrirFormulario("caja"));
                }
            }
        });
    }

    private void configurarDeshabilitado(CardView card, TextView tvStatus, String mensaje) {
        card.setAlpha(0.5f);
        card.setEnabled(false); // CRÍTICO: Deshabilitar el toque
        card.setOnClickListener(null); // Asegurar que no responda a clics
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(mensaje);
    }

    private void abrirFormulario(String tipo) {
        Intent intent = new Intent(this, InspeccionFormularioActivity.class);
        intent.putExtra("unidadId", unidadId);
        intent.putExtra("tipo", tipo);
        startActivity(intent);
    }
}
