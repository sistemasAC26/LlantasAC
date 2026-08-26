package com.example.llantasac;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText emailInput, passwordInput;
    private CheckBox checkRecordarme;
    private Button loginButton;
    private android.content.SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Esto le genera un Pase VIP a tu tablet (App Check Debug Provider)
        com.google.firebase.FirebaseApp.initializeApp(this);
        com.google.firebase.appcheck.FirebaseAppCheck firebaseAppCheck = com.google.firebase.appcheck.FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
        );

        // Inicializa la base de datos para autenticación
        mAuth = FirebaseAuth.getInstance();

        // CONFIGURACIÓN DE MODO OFFLINE (PERSISTENCIA LOCAL)
        // Esto permite que la app guarde datos sin internet y los sincronice después
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        com.google.firebase.firestore.PersistentCacheSettings cacheSettings = 
                com.google.firebase.firestore.PersistentCacheSettings.newBuilder()
                .build(); // Por defecto usa un tamaño razonable, pero se puede configurar

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(cacheSettings)
                .build();
        db.setFirestoreSettings(settings);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        checkRecordarme = findViewById(R.id.checkRecordarme);
        loginButton = findViewById(R.id.loginButton);

        // Inicializar SharedPreferences
        prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);

        // Cargar credenciales guardadas si existen
        String savedEmail = prefs.getString("email", "");
        String savedPass = prefs.getString("password", "");
        boolean isRemembered = prefs.getBoolean("remember", false);

        if (isRemembered) {
            emailInput.setText(savedEmail);
            passwordInput.setText(savedPass);
            checkRecordarme.setChecked(true);
        }

        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString();
            String password = passwordInput.getText().toString();

            if (!email.isEmpty() && !password.isEmpty()) {
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                // Guardar o borrar credenciales según el checkbox
                                android.content.SharedPreferences.Editor editor = prefs.edit();
                                if (checkRecordarme.isChecked()) {
                                    editor.putString("email", email);
                                    editor.putString("password", password);
                                    editor.putBoolean("remember", true);
                                } else {
                                    editor.clear();
                                }
                                editor.apply();

                                Intent intent = new Intent(MainActivity.this, BandejaActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                String errorMsg = task.getException() != null ? task.getException().getMessage() : "Desconocido";
                                Toast.makeText(MainActivity.this, getString(R.string.login_error_autenticacion, errorMsg), Toast.LENGTH_LONG).show();
                            }
                        });
            } else {
                Toast.makeText(this, R.string.login_error_campos, Toast.LENGTH_SHORT).show();
            }
        });
    }
}