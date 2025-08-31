package com.example.hridayasuraksha;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    private static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1001;

    BottomNavigationView bottomNav;
    FloatingActionButton fab;

    // Dashboard TextViews
    private TextView tvSteps, tvCalories, tvHeartRate, walkingMiles;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Firebase init
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Bottom Nav + FAB
        bottomNav = findViewById(R.id.bottomNav);
        fab = findViewById(R.id.fabCentral);

        // Dashboard TextViews
        tvSteps = findViewById(R.id.tvSteps);
        tvCalories = findViewById(R.id.tvCalories);
        tvHeartRate = findViewById(R.id.tvHeartRate);
        walkingMiles = findViewById(R.id.walkingMiles);

        bottomNav.setSelectedItemId(R.id.nav_dashboard);

        fab.setOnClickListener(v -> {
            Toast.makeText(this, "FAB Clicked!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(DashboardActivity.this, RecordingActivity.class));
        });

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                return true;
            } else if (id == R.id.nav_reports) {
                startActivity(new Intent(getApplicationContext(), ResultActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        // -------- Google Fit Integration --------
        FitnessOptions fitnessOptions =
                FitnessOptions.builder()
                        .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                        .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
                        .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                        .build();

        GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(this, fitnessOptions);

        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this,
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    account,
                    fitnessOptions);
        } else {
            accessGoogleFitData(account);
        }
    }

    private void accessGoogleFitData(GoogleSignInAccount account) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "guest";
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        DocumentReference dailyReportRef = db.collection("users").document(userId)
                .collection("dailyReports")
                .document(today); // One document per day

        // Steps
        Fitness.getHistoryClient(this, account)
                .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                .addOnSuccessListener(result -> {
                    long steps = result.isEmpty() ? 0 :
                            result.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                    tvSteps.setText(String.valueOf(steps));

                    Map<String, Object> update = new HashMap<>();
                    update.put("steps", steps);
                    update.put("timestamp", System.currentTimeMillis());
                    dailyReportRef.set(update, com.google.firebase.firestore.SetOptions.merge());
                })
                .addOnFailureListener(e -> Log.e("GoogleFit", "Error reading steps", e));

        // Heart Rate
        Fitness.getHistoryClient(this, account)
                .readDailyTotal(DataType.TYPE_HEART_RATE_BPM)
                .addOnSuccessListener(result -> {
                    float bpm = result.isEmpty() ? 0 :
                            result.getDataPoints().get(0).getValue(Field.FIELD_BPM).asFloat();
                    tvHeartRate.setText(bpm + " bpm");

                    Map<String, Object> update = new HashMap<>();
                    update.put("heartRate", bpm);
                    update.put("timestamp", System.currentTimeMillis());
                    dailyReportRef.set(update, com.google.firebase.firestore.SetOptions.merge());
                })
                .addOnFailureListener(e -> Log.e("GoogleFit", "Error reading heart rate", e));

        // Calories
        Fitness.getHistoryClient(this, account)
                .readDailyTotal(DataType.TYPE_CALORIES_EXPENDED)
                .addOnSuccessListener(result -> {
                    float calories = result.isEmpty() ? 0 :
                            result.getDataPoints().get(0).getValue(Field.FIELD_CALORIES).asFloat();
                    tvCalories.setText(String.format("%.1f kcal", calories));

                    Map<String, Object> update = new HashMap<>();
                    update.put("calories", calories);
                    update.put("timestamp", System.currentTimeMillis());
                    dailyReportRef.set(update, com.google.firebase.firestore.SetOptions.merge());
                })
                .addOnFailureListener(e -> Log.e("GoogleFit", "Error reading calories", e));

        // Distance
        Fitness.getHistoryClient(this, account)
                .readDailyTotal(DataType.TYPE_DISTANCE_DELTA)
                .addOnSuccessListener(result -> {
                    float distance = result.isEmpty() ? 0 :
                            result.getDataPoints().get(0).getValue(Field.FIELD_DISTANCE).asFloat();
                    walkingMiles.setText(String.format("%.2f km today", distance / 1000));

                    Map<String, Object> update = new HashMap<>();
                    update.put("distance", distance);
                    update.put("timestamp", System.currentTimeMillis());
                    dailyReportRef.set(update, com.google.firebase.firestore.SetOptions.merge());
                })
                .addOnFailureListener(e -> Log.e("GoogleFit", "Error reading distance", e));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                GoogleSignInAccount account =
                        GoogleSignIn.getAccountForExtension(this,
                                FitnessOptions.builder()
                                        .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                                        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                                        .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
                                        .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                                        .build());
                accessGoogleFitData(account);
            } else {
                Log.e("GoogleFit", "Permission denied.");
            }
        }
    }
}
