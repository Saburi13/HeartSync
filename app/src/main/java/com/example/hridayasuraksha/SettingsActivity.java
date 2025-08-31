package com.example.hridayasuraksha;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;

import com.example.hridayasuraksha.BuildConfig;


import com.google.firebase.auth.FirebaseAuth; // Safe even if you didn't add Firebase; remove if not using

public class SettingsActivity extends AppCompatActivity {

    // ---- UI ----
    private SwitchMaterial switchBiometric, switchNotifications;
    private LinearLayout rowExport, rowDeleteData, rowBiometric, rowTheme, rowAbout, rowReports, rowRecordings, rowGoogleFit;
    private TextView tvVersion, tvFitStatus, tvName, tvEmail;
    private BottomNavigationView bottomNav;
    private MaterialButton btnConnectFit;
    private Button btnLogout;

    // ---- Prefs ----
    private SharedPreferences prefs;

    // ---- Google Fit ----
    private static final int REQ_GOOGLE_FIT_PERMS = 9101;
    private FitnessOptions fitnessOptions;

    // ActivityResult launcher for Settings-style permission prompts if needed in future
    private final ActivityResultLauncher<Intent> fitPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // After permission flow, just refresh UI
                updateFitUI();
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // ---- Init views ----
        switchBiometric = findViewById(R.id.switchBiometric);
        switchNotifications = findViewById(R.id.switchNotifications);
        rowExport = findViewById(R.id.rowExport);
        rowDeleteData = findViewById(R.id.rowDeleteData);
        rowBiometric = findViewById(R.id.rowBiometric);
        rowTheme = findViewById(R.id.rowTheme);
        rowAbout = findViewById(R.id.rowAbout);
        rowReports = findViewById(R.id.rowReports);
        rowRecordings = findViewById(R.id.rowRecordings);
        rowGoogleFit = findViewById(R.id.rowGoogleFit);
        tvVersion = findViewById(R.id.tvVersion);
        tvFitStatus = findViewById(R.id.tvFitStatus);
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        bottomNav = findViewById(R.id.bottomNav);
        btnConnectFit = findViewById(R.id.btnConnectFit);
        btnLogout = findViewById(R.id.btnLogout);

        // ---- Version text from BuildConfig ----
        tvVersion.setText("v" + BuildConfig.VERSION_NAME);

        // ---- Prefs ----
        prefs = getSharedPreferences("app_settings", MODE_PRIVATE);

        // ---- BottomNav default ----
        bottomNav.setSelectedItemId(R.id.nav_settings);

        // ---- Google account display (name/email) ----
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            if (account.getDisplayName() != null) tvName.setText(account.getDisplayName());
            if (account.getEmail() != null) tvEmail.setText(account.getEmail());
        } else {
            tvName.setText("Guest");
            tvEmail.setText("Not signed in");
        }

        // ---- Fitness options ----
        fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                .build();

        // ---- Restore switches ----
        switchBiometric.setChecked(prefs.getBoolean("biometric_enabled", false));
        switchNotifications.setChecked(prefs.getBoolean("notifications_enabled", true));

        // ---- Biometric toggle ----
        switchBiometric.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            prefs.edit().putBoolean("biometric_enabled", isChecked).apply();
            Toast.makeText(this,
                    isChecked ? "Biometric lock enabled" : "Biometric lock disabled",
                    Toast.LENGTH_SHORT).show();
            // TODO: Start your biometric setup/verify flow here if you actually lock the app.
        });

        // ---- Notifications toggle ----
        switchNotifications.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply();
            Toast.makeText(this,
                    isChecked ? "Notifications enabled" : "Notifications disabled",
                    Toast.LENGTH_SHORT).show();

            if (isChecked && Build.VERSION.SDK_INT >= 33) {
                // (Optional) request POST_NOTIFICATIONS here if you actually post notifications
                // registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { ... })
                //     .launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        });

        // ---- Export ----
        rowExport.setOnClickListener(v ->
                Toast.makeText(this, "Export feature coming soon", Toast.LENGTH_SHORT).show()
        );

        // ---- Delete data ----
        rowDeleteData.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Delete my data")
                .setMessage("Are you sure you want to delete your local data? This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    // TODO: Also delete Firestore data if needed
                    Toast.makeText(this, "Data deleted (mock)", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show()
        );

        // ---- Theme ----
        rowTheme.setOnClickListener(v -> {
            int current = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            int next = (current == AppCompatDelegate.MODE_NIGHT_YES)
                    ? AppCompatDelegate.MODE_NIGHT_NO
                    : AppCompatDelegate.MODE_NIGHT_YES;
            prefs.edit().putInt("theme_mode", next).apply();
            AppCompatDelegate.setDefaultNightMode(next);
            Toast.makeText(this,
                    next == AppCompatDelegate.MODE_NIGHT_YES ? "Switched to Dark Mode" : "Switched to Light Mode",
                    Toast.LENGTH_SHORT).show();
        });

        // ---- About ----
        rowAbout.setOnClickListener(v -> {
            Toast.makeText(this, "Hridaya Suraksha " + tvVersion.getText(), Toast.LENGTH_LONG).show();
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://your-website-or-docs-link.com")));
        });

        // ---- Reports / Recordings ----
        rowReports.setOnClickListener(v -> Toast.makeText(this, "Reports feature coming soon", Toast.LENGTH_SHORT).show());
        rowRecordings.setOnClickListener(v -> Toast.makeText(this, "Opening Past Recordings...", Toast.LENGTH_SHORT).show());

        // ---- Google Fit Connect/Disconnect ----
        btnConnectFit.setOnClickListener(v -> handleFitConnectToggle());
        rowGoogleFit.setOnClickListener(v -> handleFitConnectToggle());

        // ---- Edit profile (header button) ----
        findViewById(R.id.btnEditProfile).setOnClickListener(v ->
                Toast.makeText(this, "Edit profile coming soon", Toast.LENGTH_SHORT).show()
        );

        // ---- Logout ----
        btnLogout.setOnClickListener(v -> doLogout());

        // ---- Bottom Navigation ----
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(getApplicationContext(), DashboardActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_reports) {
                startActivity(new Intent(getApplicationContext(), ResultActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_settings) {
                return true; // already here
            }
            return false;
        });

        // Initial Fit UI
        updateFitUI();

        // Apply persisted theme on start (if user toggled earlier)
        AppCompatDelegate.setDefaultNightMode(prefs.getInt(
                "theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateFitUI();
    }

    // ---------------- Google Fit helpers ----------------

    private void updateFitUI() {
        GoogleSignInAccount acc = GoogleSignIn.getAccountForExtension(this, fitnessOptions);
        boolean hasPerms = GoogleSignIn.hasPermissions(acc, fitnessOptions);
        if (hasPerms) {
            tvFitStatus.setText("Connected");
            btnConnectFit.setText("Disconnect");
        } else {
            tvFitStatus.setText("Not connected");
            btnConnectFit.setText("Connect");
        }
    }

    private void handleFitConnectToggle() {
        GoogleSignInAccount acc = GoogleSignIn.getAccountForExtension(this, fitnessOptions);
        boolean hasPerms = GoogleSignIn.hasPermissions(acc, fitnessOptions);

        if (!hasPerms) {
            // Request Google Fit permissions
            GoogleSignIn.requestPermissions(
                    this,
                    REQ_GOOGLE_FIT_PERMS,
                    acc,
                    fitnessOptions
            );
        } else {
            // Disconnect (revoke Fit permissions)
            GoogleSignInClient client = GoogleSignIn.getClient(
                    this,
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .addExtension(fitnessOptions)
                            .build()
            );
            client.revokeAccess().addOnCompleteListener(t -> {
                Toast.makeText(this, "Google Fit disconnected", Toast.LENGTH_SHORT).show();
                updateFitUI();
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_GOOGLE_FIT_PERMS) {
            // Whether granted or not, refresh label/button
            updateFitUI();
        }
    }

    // ---------------- Logout ----------------

    private void doLogout() {
        // 1) Firebase sign out (if you're using FirebaseAuth)
        try {
            FirebaseAuth.getInstance().signOut();
        } catch (Throwable ignore) { /* safe if Firebase isn't set up */ }

        // 2) Google sign out + revoke (clears cached account)
        GoogleSignInClient googleClient =
                GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);

        // signOut then revokeAccess to fully clear
        googleClient.signOut().addOnCompleteListener(task ->
                googleClient.revokeAccess().addOnCompleteListener(task2 -> {
                    Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
                    // 3) Relaunch app to the launcher activity with a clean task
                    Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
                    if (launch != null) {
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(launch);
                    } else {
                        // Fallback: go to Dashboard
                        Intent i = new Intent(this, DashboardActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                    }
                    // 4) Finish this activity
                    finish();
                })
        );
    }
}
