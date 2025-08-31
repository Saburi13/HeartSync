package com.example.hridayasuraksha;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private ImageButton btnTogglePassword;
    private MaterialButton btnSignIn;
    private ImageButton btnGoogle, btnApple, btnFacebook;
    private TextView tvRegister, tvForgot;

    private boolean passwordVisible = false;

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleClient;

    private final String SUPABASE_URL = "https://iwjhyzlvjlaafdtuuxuj.supabase.co";
    private final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Iml3amh5emx2amxhYWZkdHV1eHVqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTY1NDU3MDcsImV4cCI6MjA3MjEyMTcwN30.XoUmmRFZGc3sZb-d7UvpbOYoueE7Rfm-UJj0m8leNtw"; // Replace with your actual key
    private final String REDIRECT_URI = "com.example.hridayasuraksha://callback";

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() == null) {
                    Toast.makeText(this, "Google Sign-In canceled", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    GoogleSignInAccount account =
                            GoogleSignIn.getSignedInAccountFromIntent(result.getData()).getResult(ApiException.class);
                    if (account != null) {
                        String idToken = account.getIdToken();
                        // Exchange Google token for Supabase session
                        exchangeTokenWithSupabase(idToken);
                        // Optional: still use Firebase auth
                        firebaseAuthWithGoogle(idToken);
                    } else {
                        Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();
                    }
                } catch (ApiException e) {
                    Toast.makeText(this, "Sign-In error: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Bind views
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnApple = findViewById(R.id.btnApple);
        btnFacebook = findViewById(R.id.btnFacebook);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgot = findViewById(R.id.tvForgot);

        // Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Google Sign-In options
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleClient = GoogleSignIn.getClient(this, gso);

        // Email/Password Sign In
        btnSignIn.setOnClickListener(v -> signInWithEmail());

        // Google Sign-In (Firebase + Supabase via token exchange)
        btnGoogle.setOnClickListener(v -> {
            Intent signInIntent = googleClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        // Example: Use Supabase OAuth directly
        btnApple.setOnClickListener(v -> startSupabaseOAuth());

        // Password toggle
        btnTogglePassword.setOnClickListener(v -> togglePassword());

        // Navigate to Register
        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        // Forgot password
        tvForgot.setOnClickListener(v -> {
            String email = etUsername.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Enter your email first", Toast.LENGTH_SHORT).show();
                return;
            }
            mAuth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        // Handle deep link (Supabase OAuth redirect)
        handleOAuthRedirect(getIntent());
    }

    // Direct Supabase OAuth using Custom Tabs
    private void startSupabaseOAuth() {
        String oauthUrl = SUPABASE_URL + "/auth/v1/authorize"
                + "?provider=google"
                + "&redirect_to=" + Uri.encode(REDIRECT_URI);

        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
        customTabsIntent.launchUrl(this, Uri.parse(oauthUrl));
    }

    // Handle OAuth redirect when app resumes
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleOAuthRedirect(intent);
    }

    private void handleOAuthRedirect(Intent intent) {
        Uri data = intent.getData();
        if (data != null && data.toString().startsWith(REDIRECT_URI)) {
            String accessToken = data.getQueryParameter("access_token");
            String refreshToken = data.getQueryParameter("refresh_token");
            if (accessToken != null) {
                // Save with SupabaseSession
                SupabaseSession.saveSession(this, accessToken, "unknown_user"); // Will update when parsed properly
                Toast.makeText(this, "Supabase OAuth success!", Toast.LENGTH_SHORT).show();
                Log.d("SUPABASE", "AccessToken: " + accessToken);
                goToDashboard();
            }
        }
    }

    private void signInWithEmail() {
        String email = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etUsername.setError("Enter a valid email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password required");
            return;
        }

        btnSignIn.setEnabled(false);
        btnSignIn.setAlpha(0.6f);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> goToDashboard())
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSignIn.setEnabled(true);
                    btnSignIn.setAlpha(1f);
                });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        btnGoogle.setEnabled(false);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> goToDashboard())
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Google auth failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnGoogle.setEnabled(true);
                });
    }

    private void togglePassword() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_eye_off_24);
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_eye_24);
        }
        etPassword.setSelection(etPassword.getText().length());
    }

    private void goToDashboard() {
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check Firebase
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // Check Supabase session
        if (currentUser != null || SupabaseSession.isLoggedIn(this)) {
            goToDashboard();
        }
    }

    // Supabase token exchange (if using Google ID token instead of OAuth)
    private void exchangeTokenWithSupabase(String idToken) {
        OkHttpClient client = new OkHttpClient();

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("id_token", idToken);
            jsonBody.put("provider", "google");
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/token?grant_type=id_token")
                .post(body)
                .addHeader("apikey",SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(LoginActivity.this, "Supabase auth failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(LoginActivity.this, "Supabase response error: " + response.code(), Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                String respBody = response.body().string();
                try {
                    JSONObject json = new JSONObject(respBody);
                    String accessToken = json.getString("access_token");
                    String userId = json.getJSONObject("user").getString("id");

                    // Save via SupabaseSession
                    SupabaseSession.saveSession(LoginActivity.this, accessToken, userId);

                    Log.d("SUPABASE", "UserID: " + userId);
                    Log.d("SUPABASE", "AccessToken: " + accessToken);

                    runOnUiThread(() ->
                            Toast.makeText(LoginActivity.this, "Supabase login success!", Toast.LENGTH_SHORT).show()
                    );

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
